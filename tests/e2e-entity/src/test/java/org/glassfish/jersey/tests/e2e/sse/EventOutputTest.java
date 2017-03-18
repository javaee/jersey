/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2017 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.tests.e2e.sse;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.grizzly.connector.GrizzlyConnectorProvider;
import org.glassfish.jersey.media.sse.EventInput;
import org.glassfish.jersey.media.sse.EventListener;
import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.EventSource;
import org.glassfish.jersey.media.sse.InboundEvent;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.glassfish.jersey.media.sse.SseFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Event output tests.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class EventOutputTest extends JerseyTest {

    @Override
    protected Application configure() {
        return new ResourceConfig(SseTestResource.class, SseFeature.class);
    }

    @Override
    protected void configureClient(ClientConfig config) {
        config.register(SseFeature.class);
    }

    /**
     * SSE Test resource.
     */
    @Path("test")
    @Produces(SseFeature.SERVER_SENT_EVENTS)
    public static class SseTestResource {

        @GET
        @Path("single")
        public EventOutput getSingleEvent() {
            final EventOutput output = new EventOutput();
            try {
                return output;
            } finally {
                new Thread() {
                    public void run() {
                        try {
                            output.write(new OutboundEvent.Builder().data(String.class, "single").build());
                            output.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                            fail();
                        }
                    }
                }.start();
            }
        }

        @GET
        @Path("closed-single")
        public EventOutput getClosedSingleEvent() throws IOException {
            final EventOutput output = new EventOutput();
            output.write(new OutboundEvent.Builder().data(String.class, "closed").build());
            output.close();
            return output;
        }

        @GET
        @Path("closed-empty")
        public EventOutput getClosedEmpty() throws IOException {
            final EventOutput output = new EventOutput();
            output.close();
            return output;
        }

        @GET
        @Path("charset")
        @Produces("text/event-stream;charset=utf-8")
        public EventOutput getSseWithCharset() throws IOException {
            final EventOutput output = new EventOutput();
            output.write(new OutboundEvent.Builder().data(String.class, "charset").build());
            output.close();
            return output;
        }

        @GET
        @Path("comments-only")
        public EventOutput getCommentsOnlyStream() throws IOException {
            final EventOutput output = new EventOutput();
            output.write(new OutboundEvent.Builder().comment("No comment #1").build());
            output.write(new OutboundEvent.Builder().comment("No comment #2").build());
            output.close();
            return output;
        }
    }

    @Test
    public void testReadSseEventAsPlainString() throws Exception {
        final Response r = target().path("test/single").request().get(Response.class);
        assertThat(r.readEntity(String.class), containsString("single"));
    }

    /**
     * Reproducer for JERSEY-2912: Sending and receiving comments-only events.
     *
     * @throws Exception
     */
    @Test
    public void testReadCommentsOnlySseEvents() throws Exception {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.property(ClientProperties.CONNECT_TIMEOUT, 15000);
        clientConfig.property(ClientProperties.READ_TIMEOUT, 0);
        clientConfig.property(ClientProperties.ASYNC_THREADPOOL_SIZE, 8);
        clientConfig.connectorProvider(new GrizzlyConnectorProvider());
        Client client = ClientBuilder.newBuilder().withConfig(clientConfig).build();

        final CountDownLatch latch = new CountDownLatch(2);
        final Queue<String> eventComments = new ArrayBlockingQueue<>(2);
        WebTarget single = client.target(getBaseUri()).path("test/comments-only");
        EventSource es = EventSource.target(single).build();
        es.register(new EventListener() {
            @Override
            public void onEvent(InboundEvent inboundEvent) {
                eventComments.add(inboundEvent.getComment());
                latch.countDown();
            }
        });

        boolean latchTimedOut;
        boolean closeTimedOut;
        try {
            es.open();
            latchTimedOut = latch.await(5 * getAsyncTimeoutMultiplier(), TimeUnit.SECONDS);
        } finally {
            closeTimedOut = es.close(5, TimeUnit.SECONDS);
        }

        assertEquals("Unexpected event count", 2, eventComments.size());
        for (int i = 1; i <= 2; i++) {
            assertEquals("Unexpected comment data on event #" + i, "No comment #" + i, eventComments.poll());
        }
        assertTrue("Event latch has timed out", latchTimedOut);
        assertTrue("EventSource.close() has timed out", closeTimedOut);
    }

    @Test
    public void testReadFromClosedOutput() throws Exception {
        /**
         * Need to disable HTTP Keep-Alive to prevent this test from hanging in HttpURLConnection
         * due to an attempt to read from a stale, out-of-sync connection closed by the server.
         * Thus setting the "Connection: close" HTTP header on all requests.
         */
        Response r;
        r = target().path("test/closed-empty").request().header("Connection", "close").get();
        assertTrue(r.readEntity(String.class).isEmpty());

        r = target().path("test/closed-single").request().header("Connection", "close").get();
        assertTrue(r.readEntity(String.class).contains("closed"));

        //

        EventInput input;
        input = target().path("test/closed-single").request().header("Connection", "close").get(EventInput.class);
        assertEquals("closed", input.read().readData());
        assertEquals(null, input.read());
        assertTrue(input.isClosed());

        input = target().path("test/closed-empty").request().header("Connection", "close").get(EventInput.class);
        assertEquals(null, input.read());
        assertTrue(input.isClosed());
    }

    @Test
    public void testSseContentTypeWithCharset() {
        /**
         * Need to disable HTTP Keep-Alive to prevent this test from hanging in HttpURLConnection
         * due to an attempt to read from a stale, out-of-sync connection closed by the server.
         * Thus setting the "Connection: close" HTTP header on all requests.
         */
        Response r;
        r = target().path("test/charset").request().header("Connection", "close").get();
        assertTrue(r.getMediaType().getParameters().get("charset").equalsIgnoreCase("utf-8"));
        final EventInput eventInput = r.readEntity(EventInput.class);
        String eventData = eventInput.read().readData();
        assertEquals("charset", eventData);
        eventInput.close();
    }

    @Test
    public void testGrizzlyConnectorWithEventSource() throws InterruptedException {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.property(ClientProperties.CONNECT_TIMEOUT, 15000);
        clientConfig.property(ClientProperties.READ_TIMEOUT, 0);
        clientConfig.property(ClientProperties.ASYNC_THREADPOOL_SIZE, 8);
        clientConfig.connectorProvider(new GrizzlyConnectorProvider());
        Client client = ClientBuilder.newBuilder().withConfig(clientConfig).build();

        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<String> eventData = new AtomicReference<String>();
        final AtomicInteger counter = new AtomicInteger(0);
        WebTarget single = client.target(getBaseUri()).path("test/single");
        EventSource es = EventSource.target(single).build();
        es.register(new EventListener() {
            @Override
            public void onEvent(InboundEvent inboundEvent) {
                final int i = counter.incrementAndGet();
                if (i == 1) {
                    eventData.set(inboundEvent.readData());
                }
                latch.countDown();
            }
        });

        boolean latchTimedOut;
        boolean closeTimedOut;
        try {
            es.open();
            latchTimedOut = latch.await(5 * getAsyncTimeoutMultiplier(), TimeUnit.SECONDS);
        } finally {
            closeTimedOut = es.close(5, TimeUnit.SECONDS);
        }

        assertEquals("Unexpected event count", 1, counter.get());
        assertEquals("Unexpected event data", "single", eventData.get());
        assertTrue("Event latch has timed out", latchTimedOut);
        assertTrue("EventSource.close() has timed out", closeTimedOut);
    }
}
