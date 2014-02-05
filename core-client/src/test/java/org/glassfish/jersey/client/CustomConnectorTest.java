/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2014 Oracle and/or its affiliates. All rights reserved.
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
import java.net.URI;
import java.util.concurrent.Future;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.client.spi.AsyncConnectorCallback;
import org.glassfish.jersey.client.spi.Connector;
import org.glassfish.jersey.client.spi.ConnectorProvider;

import org.junit.Test;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class CustomConnectorTest {

    public static class NullConnector implements Connector, ConnectorProvider {

        @Override
        public ClientResponse apply(ClientRequest request) {
            throw new ProcessingException("test");
        }

        @Override
        public Future<?> apply(ClientRequest request, AsyncConnectorCallback callback) {
            throw new ProcessingException("test-async");
        }

        @Override
        public void close() {
            // do nothing
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public Connector getConnector(Client client, Configuration runtimeConfig) {
            return this;
        }
    }

    @Test
    public void testNullConnector() {
        Client client = ClientBuilder.newClient(new ClientConfig().connectorProvider(new NullConnector()).getConfiguration());
        try {
            client.target(UriBuilder.fromUri("/").build()).request().get();
        } catch (ProcessingException ce) {
            assertEquals("test", ce.getMessage());
        }
        try {
            client.target(UriBuilder.fromUri("/").build()).request().async().get();
        } catch (ProcessingException ce) {
            assertEquals("test-async", ce.getMessage());
        }
    }

    /**
     * Loop-back connector provider.
     */
    public static class TestConnectorProvider implements ConnectorProvider {

        @Override
        public Connector getConnector(Client client, Configuration runtimeConfig) {
            return new TestConnector();
        }

    }

    /**
     * Loop-back connector.
     */
    public static class TestConnector implements Connector {
        /**
         * Test loop-back status code.
         */
        public static final int TEST_LOOPBACK_CODE = 600;
        /**
         * Test loop-back status type.
         */
        public final Response.StatusType LOOPBACK_STATUS = new Response.StatusType() {
            @Override
            public int getStatusCode() {
                return TEST_LOOPBACK_CODE;
            }

            @Override
            public Response.Status.Family getFamily() {
                return Response.Status.Family.OTHER;
            }

            @Override
            public String getReasonPhrase() {
                return "Test connector loop-back";
            }
        };

        private volatile boolean closed = false;

        @Override
        public ClientResponse apply(ClientRequest request) {
            checkNotClosed();
            final ClientResponse response = new ClientResponse(LOOPBACK_STATUS, request);

            response.setEntityStream(new ByteArrayInputStream(request.getUri().toString().getBytes()));
            return response;
        }

        @Override
        public Future<?> apply(ClientRequest request, AsyncConnectorCallback callback) {
            checkNotClosed();
            throw new UnsupportedOperationException("Async invocation not supported by the test connector.");
        }

        @Override
        public String getName() {
            return "test-loop-back-connector";
        }

        @Override
        public void close() {
            closed = true;
        }

        private void checkNotClosed() {
            if (closed) {
                throw new IllegalStateException("Connector closed.");
            }
        }
    }

    /**
     * Test client request filter that creates new client based on the current runtime configuration
     * and uses the new client to produce a response.
     */
    public static class TestClientFilter implements ClientRequestFilter {

        private static final String INVOKED_BY_TEST_FILTER = "invoked-by-test-filter";

        @Override
        public void filter(ClientRequestContext requestContext) {
            final Configuration config = requestContext.getConfiguration();
            final JerseyClient client = new JerseyClientBuilder().withConfig(config).build();

            try {
                if (requestContext.getPropertyNames().contains(INVOKED_BY_TEST_FILTER)) {
                    return; // prevent the infinite recursion...
                }

                final URI filteredUri = UriBuilder.fromUri(requestContext.getUri()).path("filtered").build();
                requestContext.abortWith(client.target(filteredUri).request().property(INVOKED_BY_TEST_FILTER, true).get());
            } finally {
                client.close();
            }
        }
    }

    /**
     * Reproducer for JERSEY-2318.
     *
     * The test verifies that the {@link org.glassfish.jersey.client.spi.ConnectorProvider} configured
     * on one client instance is transferred to another client instance when the new client instance is
     * created from the original client instance configuration.
     */
    @Test
    public void testConnectorProviderPreservedOnClientConfigCopy() {
        final ClientConfig clientConfig = new ClientConfig().connectorProvider(new TestConnectorProvider());

        final Client client = ClientBuilder.newClient(clientConfig);
        try {
            Response response;

            final WebTarget target = client.target("http://wherever.org/");
            response = target.request().get();
            // let's first verify we are using the test loop-back connector.
            assertThat(response.getStatus(), equalTo(TestConnector.TEST_LOOPBACK_CODE));
            assertThat(response.readEntity(String.class), equalTo("http://wherever.org/"));

            // and now with the filter...
            target.register(TestClientFilter.class);
            response = target.request().get();
            // check if the connector provider has been propagated:
            assertThat(response.getStatus(), equalTo(TestConnector.TEST_LOOPBACK_CODE));
            // check if the filter has been invoked:
            assertThat(response.readEntity(String.class), equalTo("http://wherever.org/filtered"));
        } finally {
            client.close();
        }
    }

}
