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
package com.romanpierson.vertx.elasticsearch.indexer;

public interface ElasticSearchIndexerConstants {

	static final String EVENTBUS_EVENT_NAME = "es.indexer.event";
	
	interface Configuration {
		
		static final String INSTANCES = "instances";
		static final String IDENTIFIER = "identifier";
		static final String FLAVOUR = "flavour";
		static final String HOST = "host";
		static final String PORT = "port";
		static final String INDEX_MODE = "indexMode";
		static final String INDEX_TIMESTAMP_FIELD_NAME = "indexTimestampFieldName";
		static final String INDEX_NAME_OR_PATTERN = "indexNameOrPattern";
		static final String SSL = "ssl";
		static final String SSL_TRUST_ALL = "sslTrustAll";
		static final String AUTHENTICATION = "authentication";
		
		static final String INDEX_SCHEDULE_INTERVAL = "indexScheduleInterval";
		
		
		interface Defaults {
			
			static final Long INDEX_SCHEDULE_INTERVAL = 5000L;
			
		}
	}
	
	interface Message{
	
		interface Structure{
			
			static enum Field{
				
				META("meta"),
				MESSAGE("message"),
				TIMESTAMP("timestamp"),
				INSTANCE_IDENTIFIER("instance_identifier");
				
				private final String fieldName;
				
				private Field(String fieldName) {
					this.fieldName = fieldName;
				}
				
				public String getFieldName() {
					return this.fieldName;
				}
			}
			
			
		}
	}
	
}
