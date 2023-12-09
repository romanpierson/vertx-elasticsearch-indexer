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
package com.romanpierson.vertx.elasticsearch.indexer.authentication.impl;

import com.romanpierson.vertx.elasticsearch.indexer.authentication.Authentication;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;

public class BearerAuthentication implements Authentication {

	private final String token;
	
	public BearerAuthentication(final JsonObject jsonConfig) {
		
		// Check if we have a valid token configured
		final String tokenValue = jsonConfig.getString("token");
		
		if(tokenValue == null || tokenValue.trim().length() == 0) {
			throw new IllegalArgumentException("Invalid authentication configuration of type BEARER - missing valid token value");
		}
		
		this.token = tokenValue;
	}

	@Override
	public void modifyRequest(final HttpRequest<Buffer> request) {
		
		request.putHeader("authorization", "Bearer " + token);
		
	}
	
}
