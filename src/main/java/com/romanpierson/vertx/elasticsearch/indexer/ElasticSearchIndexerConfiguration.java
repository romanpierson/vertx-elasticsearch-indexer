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

import com.romanpierson.vertx.elasticsearch.indexer.authentication.Authentication;
import com.romanpierson.vertx.elasticsearch.indexer.verticle.ElasticSearchIndexerVerticle.IndexFlavour;

public class ElasticSearchIndexerConfiguration {

	private String identifier;
	private IndexFlavour indexFlavour;
	
	private String host;
	private int port;
	private IndexMode indexMode;
	private String indexNameOrPattern;
	
	private boolean isSSL = false;
	private boolean isSSLTrustAll = false;
	
	private Authentication authentication;
	
	public enum IndexMode{
		
		STATIC_NAME,
		DATE_PATTERN_EVENT_TIMESTAMP,
		DATE_PATTERN_INDEX_TIMESTAMP
		
	}

	public ElasticSearchIndexerConfiguration setAuthentication(final Authentication authentication) {
		
		this.authentication = authentication;
		
		return this;
		
	}
	
	public ElasticSearchIndexerConfiguration setIdentifier(final String identifier) {
		
		this.identifier = identifier;
		
		return this;
		
	}
	
	public ElasticSearchIndexerConfiguration setIndexFlavour(final IndexFlavour indexFlavour) {
		
		this.indexFlavour = indexFlavour;
		
		return this;
		
	}

	public ElasticSearchIndexerConfiguration setHost(final String host) {
		
		this.host = host;
		
		return this;
		
	}
	
	public ElasticSearchIndexerConfiguration setPort(final int port) {
		
		this.port = port;
		
		return this;
		
	}
	
	public ElasticSearchIndexerConfiguration setSSL(final boolean isSSLTrustAll) {
		
		this.isSSL = true;
		this.isSSLTrustAll = isSSLTrustAll;
		
		return this;
		
	}
	
	public ElasticSearchIndexerConfiguration setIndexNameOrPattern(final String indexNameOrPattern) {
		
		this.indexNameOrPattern = indexNameOrPattern;
		
		return this;
		
	}
	
	public ElasticSearchIndexerConfiguration setIndexMode(final IndexMode indexMode) {
		
		this.indexMode = indexMode;
		
		return this;
	}
	
	

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public IndexMode getIndexMode() {
		return indexMode;
	}

	public String getIndexNameOrPattern() {
		return indexNameOrPattern;
	}

	public boolean isSSL() {
		return isSSL;
	}

	public boolean isSSLTrustAll() {
		return isSSLTrustAll;
	}


	public String getIdentifier() {
		return identifier;
	}
	
	public IndexFlavour getIndexFlavour() {
		return indexFlavour;
	}
	

	public Authentication getAuthentication() {
		return authentication;
	}
	
}