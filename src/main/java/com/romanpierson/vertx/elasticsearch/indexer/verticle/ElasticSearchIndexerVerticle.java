/*
 * Copyright (c) 2016-2024 Roman Pierson
 * ------------------------------------------------------
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0 
 * which accompanies this distribution.
 *
 *     The Apache License v2.0 is available at
 *     http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */
package com.romanpierson.vertx.elasticsearch.indexer.verticle;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import com.romanpierson.vertx.elasticsearch.indexer.ElasticSearchIndexerConfiguration;
import com.romanpierson.vertx.elasticsearch.indexer.ElasticSearchIndexerConfiguration.IndexMode;
import com.romanpierson.vertx.elasticsearch.indexer.ElasticSearchIndexerConstants;
import com.romanpierson.vertx.elasticsearch.indexer.ElasticSearchIndexerConstants.Configuration;
import com.romanpierson.vertx.elasticsearch.indexer.authentication.impl.BasicAuthentication;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

/**
 * 
 * This verticle basically receives {@link JsonObject} data instances from the
 * eventbus and indexes it to the corresponding ES instance
 * 
 * The message structure is
 * 
 * meta timestamp instance_identifier message value...1 value...n
 * 
 * 
 * Configuration of the verticle itself is done via standard vertx config json.
 * 
 * indexScheduleInterval instances [ { identifier host port indexMode
 * {@link IndexMode} indexNameOrPattern ssl (true/false) sslTrustAll
 * (true/false) authentication { type (basic/aws) configuration { (basic
 * example) user password } } } ]
 * 
 * @author Roman Pierson
 *
 */
public class ElasticSearchIndexerVerticle extends AbstractVerticle {

	private static final String TIMEZONE_UTC = "UTC";

	private final Logger LOG = LoggerFactory.getLogger(this.getClass().getName());

	private BlockingQueue<JsonObject> queue = new LinkedBlockingQueue<>();

	private Map<String, ElasticSearchIndexerConfiguration> configurations = new HashMap<>();
	private Long indexScheduleInterval = 5000L;

	private Map<String, WebClient> webClients = new HashMap<>();

	private final DateFormat indexDateModePattern;
	private final DateFormat indexTimeStampPattern;

	private final String newLine = "\n";
	private String cachedStaticIndexPrefix;
	private Map<String, String> cachedDynamicIndexPrefix = new HashMap<>();

	public ElasticSearchIndexerVerticle() {

		super();

		indexDateModePattern = new SimpleDateFormat("yyyyMMdd");
		indexDateModePattern.setTimeZone(TimeZone.getTimeZone(TIMEZONE_UTC));

		indexTimeStampPattern = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		indexTimeStampPattern.setTimeZone(TimeZone.getTimeZone(TIMEZONE_UTC));

	}

	private void readConfig() {

		if (this.config() == null || this.config().getJsonArray(Configuration.INSTANCES, null) == null) {
			throw new RuntimeException("Invalid configuration");
		}
		;

		for (Object xInstance : this.config().getJsonArray(Configuration.INSTANCES).getList()) {
			ElasticSearchIndexerConfiguration instanceConfig = readInstanceConfig(xInstance);
			this.configurations.put(instanceConfig.getIdentifier(), instanceConfig);
		}

		this.indexScheduleInterval = this.config().getLong(Configuration.INDEX_SCHEDULE_INTERVAL,
				Configuration.Defaults.INDEX_SCHEDULE_INTERVAL);

	}

	private ElasticSearchIndexerConfiguration readInstanceConfig(final Object xInstance) {

		if (!(xInstance instanceof JsonObject)) {
			throw new RuntimeException("Invalid instance configuration");
		}

		final JsonObject jsonInstance = (JsonObject) xInstance;

		String identifier = jsonInstance.getString(Configuration.IDENTIFIER);
		String host = jsonInstance.getString(Configuration.HOST);
		Long port = jsonInstance.getLong(Configuration.PORT, null);
		String indexModeCode = jsonInstance.getString(Configuration.INDEX_MODE);
		String indexNameOrPattern = jsonInstance.getString(Configuration.INDEX_NAME_OR_PATTERN);
		boolean isSSL = jsonInstance.getBoolean(Configuration.SSL, false);
		boolean isSSLtrustAll = jsonInstance.getBoolean(Configuration.SSL_TRUST_ALL, false);
		JsonObject authentication = jsonInstance.getJsonObject(Configuration.AUTHENTICATION, null);

		IndexMode indexMode = IndexMode.valueOf(indexModeCode);

		ElasticSearchIndexerConfiguration config = new ElasticSearchIndexerConfiguration().setIdentifier(identifier)
				.setHost(host).setIndexMode(indexMode).setIndexNameOrPattern(indexNameOrPattern)
				.setPort(port.intValue());

		if (isSSL) {
			config.setSSL(isSSLtrustAll);
		}

		if (authentication != null && "basic".equals(authentication.getString("type", null))) {
			// For now we only support basic so we can create the basic authentication
			// implementation directly
			config.setAuthentication(new BasicAuthentication(authentication.getJsonObject("config", new JsonObject())));
		}

		return config;
	}

	@Override
	public void start() throws Exception {

		super.start();

		readConfig();

		LOG.info("Started successfully ElasticSearchIndexerVerticle");
		LOG.info("Index Scheduler Interval is [{}] ms", this.indexScheduleInterval);

		LOG.info("[{}] Global ES instance(s) defined", this.configurations.size());

		this.configurations.values().forEach(config -> {
			LOG.info(
					"identifier [{}], host [{}], port[{}], indexMode[{}], indexNameOrPattern[{}], isSSL[{}], isSSLtrustAll[{}] ",
					config.getIdentifier(), config.getHost(), config.getPort(), config.getIndexMode(),
					config.getIndexNameOrPattern(), config.isSSL(), config.isSSLTrustAll());
		});

		vertx.eventBus().<JsonObject>consumer(ElasticSearchIndexerConstants.EVENTBUS_EVENT_NAME, event -> {

			try {
				this.queue.put(event.body());
			} catch (Exception ex) {
				LOG.error("Error when trying to add event to queue", ex);
			}

		});

		initializeClient();

		vertx.setPeriodic(this.indexScheduleInterval, handler -> {

			if (!this.queue.isEmpty()) {

				indexCurrentData();

			}
		});

	}

	private void indexCurrentData() {

		final int currentSize = this.queue.size();

		final Collection<JsonObject> drainedValues = new ArrayList<>(currentSize);

		this.queue.drainTo(drainedValues, currentSize);

		Map<String, List<JsonObject>> valuesByIdentifier = drainedValues.parallelStream()
				.collect(Collectors.groupingBy(value -> value
						.getJsonObject(ElasticSearchIndexerConstants.Message.Structure.Field.META.getFieldName())
						.getString(ElasticSearchIndexerConstants.Message.Structure.Field.INSTANCE_IDENTIFIER
								.getFieldName())));

		for (final String identifier : valuesByIdentifier.keySet()) {

			final Collection<JsonObject> values = valuesByIdentifier.get(identifier);

			if (!this.configurations.containsKey(identifier)) {
				LOG.warn("Cannot index [{}] values for unknown instanceIdentifer [{}]", values.size(), identifier);
				continue;
			}

			final ElasticSearchIndexerConfiguration indexerConfiguration = this.configurations.get(identifier);

			final HttpRequest<Buffer> request = getRequestFor(indexerConfiguration);

			final String indexString = getIndexString(indexerConfiguration, values);

			request.sendBuffer(Buffer.buffer(indexString), ar -> {
				if (ar.succeeded()) {

					JsonObject response = ar.result().bodyAsJsonObject();

					if (response == null || response.getBoolean("errors", true)) {
						handleError(values, null);
						LOG.error("Error response received from ES \n{}", response.encodePrettily());
					}

				} else {

					handleError(values, ar.cause());

				}
			});

		}

	}

	private void handleError(Collection<JsonObject> events, Throwable throwable) {

		if (throwable != null) {
			LOG.warn("Failed to index [{}] values", events.size(), throwable);
		} else {
			LOG.warn("Failed to index [{}] values", events.size());
		}

	}

	private void initializeClient() {

		for (ElasticSearchIndexerConfiguration indexerConfig : this.configurations.values()) {

			WebClientOptions options = new WebClientOptions();
			options.setKeepAlive(true);
			options.setTrustAll(indexerConfig.isSSLTrustAll());

			webClients.put(indexerConfig.getIdentifier(), WebClient.create(vertx, options));

			LOG.info("Initialized WebClient for identifier[{}] at [{}:{}] using SSL[{}] and trustAll[{}]",
					indexerConfig.getIdentifier(), indexerConfig.getHost(), indexerConfig.getPort(),
					indexerConfig.isSSL(), indexerConfig.isSSLTrustAll());

		}

	}

	private HttpRequest<Buffer> getRequestFor(final ElasticSearchIndexerConfiguration indexerConfiguration) {

		final WebClient webClient = this.webClients.get(indexerConfiguration.getIdentifier());

		HttpRequest<Buffer> request = webClient.post(indexerConfiguration.getPort(), indexerConfiguration.getHost(),
				"/_bulk");
		request.putHeader("content-type", "application/json");
		request.ssl(indexerConfiguration.isSSL());

		if (indexerConfiguration.getAuthentication() != null) {
			indexerConfiguration.getAuthentication().modifyRequest(request);
		}

		return request;
	}

	private String getIndexPrefixString(final ElasticSearchIndexerConfiguration indexerConfiguration,
			final long eventTimestamp) {

		if (IndexMode.DATE_PATTERN_EVENT_TIMESTAMP.equals(indexerConfiguration.getIndexMode())
				|| IndexMode.DATE_PATTERN_INDEX_TIMESTAMP.equals(indexerConfiguration.getIndexMode())) {

			long timestamp = IndexMode.DATE_PATTERN_EVENT_TIMESTAMP.equals(indexerConfiguration.getIndexMode())
					? eventTimestamp
					: System.currentTimeMillis();

			final String cacheKey = indexerConfiguration.getIdentifier() + indexDateModePattern.format(timestamp);

			if (!this.cachedDynamicIndexPrefix.containsKey(cacheKey)) {

				ZonedDateTime tsDateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp),
						ZoneId.of(TIMEZONE_UTC));
				// Explicitly not using a DateTimeFormatter as this would require escaping the
				// whole pattern
				String formattedIndexPattern = indexerConfiguration.getIndexNameOrPattern()
						.replaceAll("yyyy", String.format("%04d", tsDateTime.getYear()))
						.replaceAll("MM", String.format("%02d", tsDateTime.getMonthValue()))
						.replaceAll("dd", String.format("%02d", tsDateTime.getDayOfMonth()));

				String formattedIndexPrefix = String.format(
						"{ \"index\" : { \"_index\" : \"%s\", \"_type\" : \"%s\" } }%s", formattedIndexPattern, "_doc",
						this.newLine);

				this.cachedDynamicIndexPrefix.put(cacheKey, formattedIndexPrefix);

			}

			return this.cachedDynamicIndexPrefix.get(cacheKey);

		} else {

			if (this.cachedStaticIndexPrefix == null) {

				this.cachedStaticIndexPrefix = String.format(
						"{ \"index\" : { \"_index\" : \"%s\", \"_type\" : \"%s\" } }%s",
						indexerConfiguration.getIndexNameOrPattern(), indexerConfiguration.getType(), this.newLine);

			}

			return this.cachedStaticIndexPrefix;
		}

	}

	private String getIndexString(final ElasticSearchIndexerConfiguration indexerConfiguration,
			final Collection<JsonObject> values) {

		StringBuilder sb = new StringBuilder();

		for (JsonObject value : values) {

			sb.append(getIndexPrefixString(indexerConfiguration,
					value.getJsonObject(ElasticSearchIndexerConstants.Message.Structure.Field.META.getFieldName())
							.getLong(ElasticSearchIndexerConstants.Message.Structure.Field.TIMESTAMP.getFieldName())));

			JsonObject jsonValue = JsonObject.mapFrom(
					value.getJsonObject(ElasticSearchIndexerConstants.Message.Structure.Field.MESSAGE.getFieldName()));

			jsonValue.put(ElasticSearchIndexerConstants.Message.Structure.Field.TIMESTAMP.getFieldName(),
					indexTimeStampPattern.format(value
							.getJsonObject(ElasticSearchIndexerConstants.Message.Structure.Field.META.getFieldName())
							.getLong(ElasticSearchIndexerConstants.Message.Structure.Field.TIMESTAMP.getFieldName())));

			sb.append(jsonValue.encode()).append(newLine);
		}

		return sb.toString();
	}

	@Override
	public void stop() throws Exception {

		LOG.info("Stopping ElasticSearchAppender Verticle");

		if (!this.queue.isEmpty()) {

			LOG.info("Starting to drain queue with [{}] items left to ElasticSearch", this.queue.size());

			indexCurrentData();

			LOG.info("Finished queue draining");

		} else {

			LOG.info("No items left in queue");

		}

		LOG.info("Stopping Web Client(s)");
		this.webClients.values().forEach(webClient -> {
			webClient.close();
		});

		super.stop();

	}

}