/*
 * Copyright (c) 2016-2019 Roman Pierson
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
package com.mdac.vertx.web.accesslogger;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import com.mdac.vertx.elasticsearch.indexer.ElasticSearchIndexerConstants;
import com.mdac.vertx.elasticsearch.indexer.verticle.ElasticSearchIndexerVerticle;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;


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
	
	private final static List<String> identifiers = Arrays.asList("accesslog_ssl"/*, "accesslog", "applicationlog"*/);
	private final static Random random = new Random();
	
	public static void main(String[] args) throws InterruptedException {
		
		System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory");
		
		final Vertx vertx = Vertx.vertx();
		
		vertx.exceptionHandler(throwable -> {
			throwable.printStackTrace();
		});
		
		ConfigStoreOptions store = new ConfigStoreOptions().setType("file").setFormat("yaml")
										.setConfig(new JsonObject().put("path", "indexer-config.yaml"));

		ConfigRetriever retriever = ConfigRetriever.create(vertx,  new ConfigRetrieverOptions().addStore(store));
				
		retriever.getConfig(result -> {
			if(result.succeeded()) {
				vertx.deployVerticle(ElasticSearchIndexerVerticle.class.getName(), new DeploymentOptions().setConfig(result.result()));
			
				vertx.setPeriodic(1000, handler -> {
					
					final String randomIdentifier = identifiers.get(random.nextInt(identifiers.size()));
					
					JsonObject meta = new JsonObject().put("instance_identifier", randomIdentifier).put("timestamp", System.currentTimeMillis());
					JsonObject message = new JsonObject().put("message", "A message for identifier [" + randomIdentifier + "]....").put("level", "INFO");
					
					vertx.eventBus().send(ElasticSearchIndexerConstants.EVENTBUS_EVENT_NAME, new JsonObject().put("meta", meta).put("message", message));
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
