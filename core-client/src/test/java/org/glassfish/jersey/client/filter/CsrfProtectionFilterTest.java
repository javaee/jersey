/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.client.filter;

import java.util.concurrent.Future;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientRequest;
import org.glassfish.jersey.client.ClientResponse;
import org.glassfish.jersey.client.spi.AsyncConnectorCallback;
import org.glassfish.jersey.client.spi.Connector;
import org.glassfish.jersey.client.spi.ConnectorProvider;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Cross-site request forgery client filter test.
 *
 * @author Martin Matula
 */
public class CsrfProtectionFilterTest {
    private Invocation.Builder invBuilder;

    @Before
    public void setUp() {
        Client client = ClientBuilder.newClient(new ClientConfig(CsrfProtectionFilter.class)
                .connectorProvider(new TestConnector()));
        invBuilder = client.target(UriBuilder.fromUri("/").build()).request();
    }

    @Test
    public void testGet() {
        Response r = invBuilder.get();
        assertNull(r.getHeaderString(CsrfProtectionFilter.HEADER_NAME));
    }

    @Test
    public void testPut() {
        Response r = invBuilder.put(Entity.entity("put", MediaType.TEXT_PLAIN_TYPE));
        assertNotNull(r.getHeaderString(CsrfProtectionFilter.HEADER_NAME));
    }

    private static class TestConnector implements Connector, ConnectorProvider {

        @Override
        public Connector getConnector(Client client, Configuration runtimeConfig) {
            return this;
        }

        @Override
        public ClientResponse apply(ClientRequest requestContext) {
            final ClientResponse responseContext = new ClientResponse(
                    Response.Status.OK, requestContext);

            final String headerValue = requestContext.getHeaderString(CsrfProtectionFilter.HEADER_NAME);
            if (headerValue != null) {
                responseContext.header(CsrfProtectionFilter.HEADER_NAME, headerValue);
            }
            return responseContext;
        }

        @Override
        public Future<?> apply(ClientRequest request, AsyncConnectorCallback callback) {
            throw new UnsupportedOperationException("Asynchronous execution not supported.");
        }

        @Override
        public void close() {
            // do nothing
        }

        @Override
        public String getName() {
            return null;
        }
    }
}
