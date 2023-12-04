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
package com.romanpierson.vertx.elasticsearch.indexer.authentication;

import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpRequest;

public interface Authentication {

	/**
	 * 
	 * Allows the authentication implementation to modify the request if needed
	 * 
	 * @param request
	 */
	void modifyRequest(HttpRequest<Buffer> request);
	
}
