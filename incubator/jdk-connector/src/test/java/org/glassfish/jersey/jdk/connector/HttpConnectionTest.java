/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.jdk.connector;

import java.net.CookieManager;
import java.net.SocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.AfterClass;
import org.junit.Test;
import static org.glassfish.jersey.jdk.connector.HttpConnection.State.CLOSED;
import static org.glassfish.jersey.jdk.connector.HttpConnection.State.CONNECTING;
import static org.glassfish.jersey.jdk.connector.HttpConnection.State.CONNECT_TIMEOUT;
import static org.glassfish.jersey.jdk.connector.HttpConnection.State.ERROR;
import static org.glassfish.jersey.jdk.connector.HttpConnection.State.IDLE;
import static org.glassfish.jersey.jdk.connector.HttpConnection.State.IDLE_TIMEOUT;
import static org.glassfish.jersey.jdk.connector.HttpConnection.State.RECEIVED;
import static org.glassfish.jersey.jdk.connector.HttpConnection.State.RECEIVING_BODY;
import static org.glassfish.jersey.jdk.connector.HttpConnection.State.RECEIVING_HEADER;
import static org.glassfish.jersey.jdk.connector.HttpConnection.State.RESPONSE_TIMEOUT;
import static org.glassfish.jersey.jdk.connector.HttpConnection.State.SENDING_REQUEST;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Petr Janouch (petr.janouch at oracle.com)
 */
public class HttpConnectionTest extends JerseyTest {

    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final Throwable testError = new Throwable();

    @AfterClass
    public static void cleanUp() {
        scheduler.shutdownNow();
    }

    @Test
    public void testBasic() {
        HttpConnection.State[] expectedStates = new HttpConnection.State[] {CONNECTING, IDLE, SENDING_REQUEST,
                RECEIVING_HEADER, RECEIVING_BODY, RECEIVED, IDLE};
        HttpRequest request = HttpRequest.createBodyless("GET", target("hello").getUri());
        doTest(ERROR_STATE.NONE, expectedStates, request);
    }

    @Test
    public void testMultipleRequests() {
        HttpConnection.State[] expectedStates = new HttpConnection.State[] {CONNECTING, IDLE, SENDING_REQUEST,
                RECEIVING_HEADER, RECEIVING_BODY, RECEIVED, IDLE, SENDING_REQUEST, RECEIVING_HEADER, RECEIVING_BODY, RECEIVED,
                IDLE};
        HttpRequest request = HttpRequest.createBodyless("GET", target("hello").getUri());
        doTest(ERROR_STATE.NONE, expectedStates, request, request);
    }

    @Test
    public void testErrorSending() {
        HttpConnection.State[] expectedStates = new HttpConnection.State[] {CONNECTING, IDLE, SENDING_REQUEST, ERROR, CLOSED};
        HttpRequest request = HttpRequest.createBodyless("GET", target("hello").getUri());
        doTest(ERROR_STATE.SENDING, expectedStates, request);
    }

    @Test
    public void testErrorReceiving() {
        HttpConnection.State[] expectedStates = new HttpConnection.State[] {CONNECTING, IDLE, SENDING_REQUEST,
                RECEIVING_HEADER, ERROR, CLOSED};
        HttpRequest request = HttpRequest.createBodyless("GET", target("hello").getUri());
        doTest(ERROR_STATE.RECEIVING_HEADER, expectedStates, request);
    }

    @Test
    public void testTimeoutConnecting() {
        HttpConnection.State[] expectedStates = new HttpConnection.State[] {CONNECTING, CONNECT_TIMEOUT, CLOSED};
        HttpRequest request = HttpRequest.createBodyless("GET", target("hello").getUri());
        ConnectorConfiguration configuration = new ConnectorConfiguration(client(), client().getConfiguration()) {
            @Override
            int getConnectTimeout() {
                return 100;
            }
        };
        doTest(ERROR_STATE.LOST_CONNECT, configuration, expectedStates, request);
    }

    @Test
    public void testResponseTimeout() {
        HttpConnection.State[] expectedStates = new HttpConnection.State[] {CONNECTING, IDLE, SENDING_REQUEST,
                RECEIVING_HEADER, RESPONSE_TIMEOUT, CLOSED};
        HttpRequest request = HttpRequest.createBodyless("GET", target("hello").getUri());
        ConnectorConfiguration configuration = new ConnectorConfiguration(client(), client().getConfiguration()) {

            @Override
            int getResponseTimeout() {
                return 100;
            }
        };

        doTest(ERROR_STATE.LOST_REQUEST, configuration, expectedStates, request);
    }

    @Test
    public void testIdleTimeout() {
        HttpConnection.State[] expectedStates = new HttpConnection.State[] {CONNECTING, IDLE, SENDING_REQUEST,
                RECEIVING_HEADER, RECEIVING_BODY, RECEIVED, IDLE, IDLE_TIMEOUT, CLOSED};
        HttpRequest request = HttpRequest.createBodyless("GET", target("hello").getUri());
        ConnectorConfiguration configuration = new ConnectorConfiguration(client(), client().getConfiguration()) {

            @Override
            int getConnectionIdleTimeout() {
                return 500;
            }
        };

        doTest(ERROR_STATE.NONE, configuration, expectedStates, request);
    }

    private void doTest(ERROR_STATE errorState,
                        ConnectorConfiguration configuration,
                        HttpConnection.State[] expectedStates,
                        HttpRequest... httpRequests) {
        CountDownLatch latch = new CountDownLatch(1);
        TestStateListener stateListener = new TestStateListener(expectedStates, latch, httpRequests);
        HttpConnection connection = createConnection(httpRequests[0].getUri(), stateListener, errorState, configuration);
        connection.connect();

        try {
            assertTrue(latch.await(5, TimeUnit.SECONDS));
        } catch (Throwable t) {
            // continue
        }

        assertEquals(Arrays.asList(expectedStates), stateListener.getObservedStates());

        if (errorState == ERROR_STATE.SENDING || errorState == ERROR_STATE.CONNECTING
                || errorState == ERROR_STATE.RECEIVING_HEADER) {
            assertTrue(testError == connection.getError());
        }
    }

    private void doTest(ERROR_STATE errorState, HttpConnection.State[] expectedStates, HttpRequest... httpRequests) {
        ConnectorConfiguration configuration = new ConnectorConfiguration(client(), client().getConfiguration());
        doTest(errorState, configuration, expectedStates, httpRequests);
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(EchoResource.class);
    }

    @Override
    protected void configureClient(ClientConfig config) {
        config.property(JdkConnectorProvider.CONNECTION_IDLE_TIMEOUT, 30_000);
        config.connectorProvider(new JdkConnectorProvider());
    }

    @Path("/hello")
    public static class EchoResource {

        @GET
        public String getHello() {
            return "Hello";
        }
    }

    private HttpConnection createConnection(URI uri,
                                            TestStateListener stateListener,
                                            final ERROR_STATE errorState,
                                            ConnectorConfiguration configuration) {
        return new HttpConnection(uri, new CookieManager(), configuration, scheduler, stateListener) {
            @Override
            protected Filter<HttpRequest, HttpResponse, HttpRequest, HttpResponse> createFilterChain(URI uri,
                                                                                                     ConnectorConfiguration
                                                                                                             configuration) {
                Filter<HttpRequest, HttpResponse, HttpRequest, HttpResponse> filterChain = super
                        .createFilterChain(uri, configuration);
                return new InterceptorFilter(filterChain, errorState);
            }
        };
    }

    private static class TestStateListener implements HttpConnection.StateChangeListener {

        private final List<HttpConnection.State> observedStates = new ArrayList<>();
        private final HttpRequest[] httpRequests;
        private final AtomicInteger sentRequests = new AtomicInteger(0);
        private final CountDownLatch latch;
        private final Queue<HttpConnection.State> expectedStates;

        public TestStateListener(HttpConnection.State[] expectedStates, CountDownLatch latch, HttpRequest... httpRequests) {
            this.httpRequests = httpRequests;
            this.latch = latch;
            this.expectedStates = new LinkedList<>(Arrays.asList(expectedStates));
        }

        @Override
        public void onStateChanged(HttpConnection connection, HttpConnection.State oldState, HttpConnection.State newState) {

            observedStates.add(newState);

            HttpConnection.State expectedState = expectedStates.poll();
            if (expectedState != newState) {
                latch.countDown();
            }

            if (newState == IDLE && httpRequests.length > sentRequests.get()) {
                connection.send(httpRequests[sentRequests.get()]);
                sentRequests.incrementAndGet();
            }

            if (expectedStates.peek() == null) {
                latch.countDown();
            }
        }

        public List<HttpConnection.State> getObservedStates() {
            return observedStates;
        }
    }

    private static class InterceptorFilter extends Filter<HttpRequest, HttpResponse, HttpRequest, HttpResponse> {

        private final ERROR_STATE errorState;

        InterceptorFilter(Filter<HttpRequest, HttpResponse, HttpRequest, HttpResponse> downstreamFilter, ERROR_STATE errroState) {
            super(downstreamFilter);
            this.errorState = errroState;
        }

        @Override
        void write(HttpRequest data, final CompletionHandler<HttpRequest> completionHandler) {
            if (errorState == ERROR_STATE.LOST_REQUEST) {
                completionHandler.completed(data);
                return;
            }

            if (errorState == ERROR_STATE.SENDING) {
                completionHandler.failed(testError);
                return;
            }

            if (errorState == ERROR_STATE.RECEIVING_HEADER) {
                downstreamFilter.write(data, new CompletionHandler<HttpRequest>() {
                    @Override
                    public void completed(HttpRequest result) {
                        completionHandler.completed(result);
                    }
                });
                downstreamFilter.onError(testError);
                return;
            }

            downstreamFilter.write(data, completionHandler);
        }

        @Override
        void connect(SocketAddress address, Filter<?, ?, HttpRequest, HttpResponse> upstreamFilter) {
            if (errorState == ERROR_STATE.LOST_CONNECT) {
                return;
            }

            if (errorState == ERROR_STATE.CONNECTING) {
                return;
            }

            super.connect(address, upstreamFilter);
        }
    }

    private enum ERROR_STATE {
        NONE,
        CONNECTING,
        SENDING,
        RECEIVING_HEADER,
        LOST_REQUEST,
        LOST_CONNECT
    }
}
