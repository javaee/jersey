/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2016 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.api;

import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test setting headers that are restricted by {@link java.net.HttpURLConnection}.
 *
 * @author Miroslav Fuksa
 */
public class RestrictedHeaderTest extends JerseyTest {

    private static final Logger LOGGER = Logger.getLogger(RestrictedHeaderTest.class.getName());

    @Path("/")
    public static class MyResource {

        @GET
        public Response getOptions(@Context HttpHeaders headers) {
            MultivaluedMap<String, String> requestHeaders = headers.getRequestHeaders();
            System.out.println("Headers: " + requestHeaders);
            if (requestHeaders.containsKey("Origin") || requestHeaders.containsKey("Access-Control-Request-Method")) {
                LOGGER.info("CORS headers found.");
                return Response.ok().build();
            }
            LOGGER.info("CORS headers are missing. ");
            return Response.serverError().entity("CORS headers are missing").build();
        }
    }

    @Override
    protected ResourceConfig configure() {
        enable(TestProperties.LOG_TRAFFIC);
        return new ResourceConfig(MyResource.class, LoggingFeature.class);
    }

    @Ignore("The setting of allowRestrictedHeaders system property is global and cached. Only "
            + "one of both testForbiddenHeadersNotAllowed() and testForbiddenHeadersAllowed() can be run during one test.")
    @Test
    public void testForbiddenHeadersNotAllowed() {
        Client client = ClientBuilder.newClient();
        Response response = testHeaders(client);
        Assert.assertEquals(500, response.getStatus());
    }

    /**
     * Tests sending of restricted headers (Origin and Access-Control-Request-Method) which are
     * used for CORS. These headers are by default skipped by the {@link java.net.HttpURLConnection}.
     * The system property {@code sun.net.http.allowRestrictedHeaders} must be defined in order to
     * allow these headers.
     */
    @Test
    public void testForbiddenHeadersAllowed() {
        Client client = ClientBuilder.newClient();
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");

        Response response = testHeaders(client);
        System.out.println(response.readEntity(String.class));
        Assert.assertEquals(200, response.getStatus());
    }

    /**
     * Same as {@link #testForbiddenHeadersAllowed()} ()} but uses {@link org.glassfish.jersey.apache.connector
     * .ApacheConnector} connector
     * which allows modification of these headers.
     */
    @Test
    public void testForbiddenHeadersWithApacheConnector() {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.connectorProvider(new ApacheConnectorProvider());
        Client client = ClientBuilder.newClient(clientConfig);
        testHeaders(client);
    }

    private Response testHeaders(Client client) {
        client.register(LoggingFeature.class);
        Invocation.Builder builder = client.target(getBaseUri()).path("/").request()
                .header("Origin", "http://example.com")
                .header("Access-Control-Request-Method", "POST")
                .header("Testus", "Hello");
        return builder.get();
    }
}
