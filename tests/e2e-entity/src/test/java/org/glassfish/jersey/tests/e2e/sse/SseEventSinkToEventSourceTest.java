/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.Produces;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.InboundSseEvent;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseEventSink;
import javax.ws.rs.sse.SseEventSource;

import javax.inject.Singleton;

import org.glassfish.jersey.media.sse.EventListener;
import org.glassfish.jersey.media.sse.EventSource;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Assert;
import org.junit.Test;

/**
 * JAX-RS {@link SseEventSource} and {@link SseEventSink} test.
 *
 * @author Adam Lindenthal (adam.lindenthal at oracle.com)
 */
public class SseEventSinkToEventSourceTest extends JerseyTest {

    private static final String INTEGER_SSE_NAME = "integer-message";
    private static final Logger LOGGER = Logger.getLogger(SseEventSinkToEventSourceTest.class.getName());

    @Override
    protected Application configure() {
        return new ResourceConfig(SseResource.class);
    }

    private static final int MSG_COUNT = 10;
    private static volatile CountDownLatch transmitLatch;

    @Path("events")
    @Singleton
    public static class SseResource {

        @GET
        @Produces(MediaType.SERVER_SENT_EVENTS)
        public void getServerSentEvents(@Context final SseEventSink eventSink, @Context final Sse sse) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                int i = 0;
                while (transmitLatch.getCount() > 0) {
                    eventSink.send(sse.newEventBuilder()
                            .name(INTEGER_SSE_NAME)
                            .mediaType(MediaType.TEXT_PLAIN_TYPE)
                            .data(Integer.class, i)
                            .build());

                    // send another event with name "foo" -> should be ignored by the client
                    eventSink.send(sse.newEventBuilder()
                            .name("foo")
                            .mediaType(MediaType.TEXT_PLAIN_TYPE)
                            .data(String.class, "bar")
                            .build());

                    // send another unnamed event -> should be ignored by the client
                    eventSink.send(sse.newEventBuilder()
                            .mediaType(MediaType.TEXT_PLAIN_TYPE)
                            .data(String.class, "baz")
                            .build());
                    transmitLatch.countDown();
                    i++;
                }
            });
        }
    }

    @Test
    public void testWithSimpleSubscriber() {
        transmitLatch = new CountDownLatch(MSG_COUNT);
        final WebTarget endpoint = target().path("events");
        final List<InboundSseEvent> results = new ArrayList<>();
        try (final SseEventSource eventSource = SseEventSource.target(endpoint).build()) {
            final CountDownLatch receivedLatch = new CountDownLatch(3 * MSG_COUNT);
            eventSource.register((event) -> {
                results.add(event);
                receivedLatch.countDown();
            });

            eventSource.open();
            final boolean allTransmitted = transmitLatch.await(5000, TimeUnit.MILLISECONDS);
            final boolean allReceived = receivedLatch.await(5000, TimeUnit.MILLISECONDS);
            Assert.assertTrue(allTransmitted);
            Assert.assertTrue(allReceived);
            Assert.assertEquals(30, results.size());
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testWithJerseyApi() throws InterruptedException {
        final WebTarget endpoint = target().path("events");
        final EventSource eventSource = EventSource.target(endpoint).build();
        transmitLatch = new CountDownLatch(MSG_COUNT);
        final CountDownLatch receiveLatch = new CountDownLatch(MSG_COUNT);

        final List<Integer> results = new ArrayList<>();
        final EventListener listener = inboundEvent -> {
            try {
                results.add(inboundEvent.readData(Integer.class));
                receiveLatch.countDown();
                Assert.assertEquals(INTEGER_SSE_NAME, inboundEvent.getName());
            } catch (ProcessingException ex) {
                throw new RuntimeException("Error when deserializing of data.", ex);
            }
        };
        eventSource.register(listener, INTEGER_SSE_NAME);
        eventSource.open();
        Assert.assertTrue(transmitLatch.await(5000, TimeUnit.MILLISECONDS));
        Assert.assertTrue(receiveLatch.await(5000, TimeUnit.MILLISECONDS));
        Assert.assertEquals(10, results.size());
    }


    @Test
    public void testWithEventSource() throws InterruptedException {
        transmitLatch = new CountDownLatch(2 * MSG_COUNT);
        final WebTarget endpoint = target().path("events");
        final SseEventSource eventSource = SseEventSource.target(endpoint).build();

        final CountDownLatch count1 = new CountDownLatch(3 * MSG_COUNT);
        final CountDownLatch count2 = new CountDownLatch(3 * MSG_COUNT);

        eventSource.register(new InboundHandler("consumer1", count1));
        eventSource.register(new InboundHandler("consumer2", count2));

        eventSource.open();
        final boolean sent = transmitLatch.await(5 * getAsyncTimeoutMultiplier(), TimeUnit.SECONDS);
        Assert.assertTrue("Awaiting for SSE message has timeout. Not all message were sent.", sent);

        final boolean handled2 = count2.await(5 * getAsyncTimeoutMultiplier(), TimeUnit.SECONDS);
        Assert.assertTrue(
                "Awaiting for SSE message has timeout. Not all message were handled by eventSource2.", handled2);

        final boolean handled1 = count1.await(5 * getAsyncTimeoutMultiplier(), TimeUnit.SECONDS);
        Assert.assertTrue(
                "Awaiting for SSE message has timeout. Not all message were handled by eventSource1.", handled1);

    }

    private class InboundHandler implements Consumer<InboundSseEvent> {
        private final CountDownLatch latch;
        private final String name;

        InboundHandler(final String name, final CountDownLatch latch) {
            this.latch = latch;
            this.name = name;
        }

        @Override
        public void accept(final InboundSseEvent inboundSseEvent) {
            try {
                if (INTEGER_SSE_NAME.equals(inboundSseEvent.getName())) {
                    final Integer data = inboundSseEvent.readData(Integer.class);
                    LOGGER.info(String.format("[%s] Integer data received: [id=%s name=%s comment=%s reconnectDelay=%d value=%d]",
                            name,
                            inboundSseEvent.getId(),
                            inboundSseEvent.getName(), inboundSseEvent.getComment(), inboundSseEvent.getReconnectDelay(), data));
                } else {
                    final String data = inboundSseEvent.readData();
                    LOGGER.info(String.format("[%s] String data received: [id=%s name=%s comment=%s reconnectDelay=%d value=%s]",
                            name,
                            inboundSseEvent.getId(),
                            inboundSseEvent.getName(), inboundSseEvent.getComment(), inboundSseEvent.getReconnectDelay(), data));
                }
                latch.countDown();
            } catch (final ProcessingException ex) {
                throw new RuntimeException("Error when deserializing the data.", ex);
            }
        }
    }
}
