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

import java.util.Base64;

import com.romanpierson.vertx.elasticsearch.indexer.authentication.Authentication;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;

public class BasicAuthentication implements Authentication {

	private final JsonObject jsonConfig;
	private final String base64key;
	
	public BasicAuthentication(final JsonObject jsonConfig) {
		
		this.jsonConfig = jsonConfig;
		base64key = Base64.getEncoder().encodeToString(new StringBuilder(this.jsonConfig.getString("user")).append(":").append(this.jsonConfig.getString("password")).toString().getBytes());
		
	}

	@Override
	public void modifyRequest(final HttpRequest<Buffer> request) {
		
		request.putHeader("authorization", "Basic " + base64key);
		
	}
	
}
