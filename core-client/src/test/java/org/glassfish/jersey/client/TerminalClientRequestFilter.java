/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * http://glassfish.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package org.glassfish.jersey.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

/**
 * Client request filter, which doesn't perform actual requests.
 *
 * Sets the response based on various request properties:
 * <ul>
 *     <li>Response code is value of request header {@code "Response-Status"} or {@code 200}, if not present.</li>
 *     <li>Response entity is request entity or {@code "NO-ENTITY"}, if request entity is not present.</li>
 *     <li>Response headers do contain all request headers, names are prefixed by {@code "Test-Header-"}.</li>
 *     <li>Response headers do contain all request properties, names are prefixed by {@code "Test-Property-"}.</li>
 * </ul>
 *
 * Additionally, response headers contain {@code "Test-Thread"} (current thread name), {@code "Test-Uri"} (request Uri)
 * and {@code "Test-Method"} (request Http method).
 *
 * @author Michal Gajdos
 */
class TerminalClientRequestFilter implements ClientRequestFilter {

    @Override
    public void filter(final ClientRequestContext requestContext) throws IOException {
        // Obtain entity - from request or create new.
        final ByteArrayInputStream entity = new ByteArrayInputStream(
                requestContext.hasEntity() ? requestContext.getEntity().toString().getBytes() : "NO-ENTITY".getBytes()
        );

        final int responseStatus = Optional.ofNullable(requestContext.getHeaders().getFirst("Response-Status"))
                                           .map(Integer.class::cast)
                                           .orElse(200);

        Response.ResponseBuilder response = Response.status(responseStatus)
                                                    .entity(entity)
                                                    .type("text/plain")
                                                    // Test properties.
                                                    .header("Test-Thread", Thread.currentThread().getName())
                                                    .header("Test-Uri", requestContext.getUri().toString())
                                                    .header("Test-Method", requestContext.getMethod());

        // Request headers -> Response headers (<header> -> Test-Header-<header>)
        for (final MultivaluedMap.Entry<String, List<String>> entry : requestContext.getStringHeaders().entrySet()) {
            response = response.header("Test-Header-" + entry.getKey(), entry.getValue());
        }

        // Request properties -> Response headers (<header> -> Test-Property-<header>)
        for (final String property : requestContext.getPropertyNames()) {
            response = response.header("Test-Property-" + property, requestContext.getProperty(property));
        }

        requestContext.abortWith(response.build());
    }
}
