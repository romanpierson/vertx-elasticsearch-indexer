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
package com.romanpierson.vertx.web.accesslogger;

import java.util.Arrays;
import java.util.List;

import com.romanpierson.vertx.elasticsearch.indexer.ElasticSearchIndexerConstants;
import com.romanpierson.vertx.elasticsearch.indexer.verticle.ElasticSearchIndexerVerticle;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 
 * Just a test verticle that shows the usage
 * 
 * @author Roman Pierson
 *
 */
public class TestVerticle extends AbstractVerticle {

	@SuppressWarnings("unused")
	private Logger LOG = LoggerFactory.getLogger(TestVerticle.class.getName());
	
	private final static List<String> targetIdentifiers = Arrays.asList("axiom-accesslog", "es-accesslog");
	
	public static void main(String[] args) throws InterruptedException {
		
		System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory");
		
		final Vertx vertx = Vertx.vertx();
		
		vertx.exceptionHandler(throwable -> {
			throwable.printStackTrace();
		});
		
		ConfigStoreOptions store = new ConfigStoreOptions().setType("file").setFormat("yaml")
										.setConfig(new JsonObject().put("path", "indexer-config.yaml"));

		ConfigRetriever retriever = ConfigRetriever.create(vertx,  new ConfigRetrieverOptions().addStore(store));
		
		retriever
			.getConfig()
			.onComplete(result -> {
				
				if(result.succeeded()) {
					vertx.deployVerticle(ElasticSearchIndexerVerticle.class.getName(),
							new DeploymentOptions().setConfig(result.result())).onComplete(deploymentId -> {
								vertx.setPeriodic(1000, handler -> {

									for (String targetIdentifier : targetIdentifiers) {

										final long ts = System.currentTimeMillis();
										
										JsonObject meta = new JsonObject().put("instance_identifier", targetIdentifier)
												.put("timestamp", ts);
										JsonObject message = new JsonObject()
												.put("message", String.format("A message for identifier [%s] sent at [%d]....", targetIdentifier, ts))
												.put("level", "INFO");

										vertx.eventBus().send(ElasticSearchIndexerConstants.EVENTBUS_EVENT_NAME,
												new JsonObject().put("meta", meta).put("message", message));
									}

								});
							}, throwable -> {
								throw new RuntimeException("Error when deploying ElasticSearchIndexerVerticle", throwable);
							});
				} else {
					result.cause().printStackTrace();
				}
				
			});
		
	}
	
	
	@Override
	public void start() throws Exception {
		
		super.start();
		
	}

}
