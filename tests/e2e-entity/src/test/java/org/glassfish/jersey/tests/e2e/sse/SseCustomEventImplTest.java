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

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.Produces;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.InboundSseEvent;
import javax.ws.rs.sse.OutboundSseEvent;
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
 * Test that {@link org.glassfish.jersey.media.sse.OutboundEventWriter} works with custom
 * {@link javax.ws.rs.sse.OutboundSseEvent} implementation.
 *
 * @author Adam Lindenthal (adam.lindenthal at oracle.com)
 */
public class SseCustomEventImplTest extends JerseyTest {

    private static final String SSE_EVENT_NAME = "custom-message";

    @Override
    protected Application configure() {
        return new ResourceConfig(SseResource.class);
    }

    @Path("events")
    @Singleton
    public static class SseResource {

        @GET
        @Produces(MediaType.SERVER_SENT_EVENTS)
        public void getServerSentEvents(@Context final SseEventSink eventSink) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                eventSink.send(new MyOutboundEvent("foo"));
                eventSink.send(new MyOutboundEvent("bar"));
                eventSink.send(new MyOutboundEvent("baz"));
            });
        }
    }

    @Test
    public void testWithJaxRsAPI() {
        final WebTarget endpoint = target().path("events");
        final List<InboundSseEvent> results = new ArrayList<>();
        try (final SseEventSource eventSource = SseEventSource.target(endpoint).build()) {
            final CountDownLatch receivedLatch = new CountDownLatch(3);
            eventSource.register((event) -> {
                results.add(event);
                receivedLatch.countDown();
            });

            eventSource.open();
            final boolean allReceived = receivedLatch.await(5000, TimeUnit.MILLISECONDS);
            Assert.assertTrue(allReceived);
            Assert.assertEquals(3, results.size());
            Assert.assertEquals("foo", results.get(0).readData());
            Assert.assertEquals("bar", results.get(1).readData());
            Assert.assertEquals("baz", results.get(2).readData());
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testWithJerseyAPI() throws InterruptedException {
        final WebTarget endpoint = target().path("events");
        final EventSource eventSource = EventSource.target(endpoint).build();
        final CountDownLatch receiveLatch = new CountDownLatch(3);

        final List<String> results = new ArrayList<>();
        final EventListener listener = inboundEvent -> {
            try {
                results.add(inboundEvent.readData());
                receiveLatch.countDown();
                Assert.assertEquals(SSE_EVENT_NAME, inboundEvent.getName());
            } catch (ProcessingException ex) {
                throw new RuntimeException("Error when deserializing of data.", ex);
            }
        };
        eventSource.register(listener, SSE_EVENT_NAME);
        eventSource.open();
        Assert.assertTrue(receiveLatch.await(5000, TimeUnit.MILLISECONDS));
        Assert.assertEquals(3, results.size());
        Assert.assertEquals("foo", results.get(0));
        Assert.assertEquals("bar", results.get(1));
        Assert.assertEquals("baz", results.get(2));
    }

    static class MyOutboundEvent implements OutboundSseEvent {

        private String data;

        public MyOutboundEvent(String data) {
            this.data = data;
        }

        @Override
        public Class<?> getType() {
            return String.class;
        }

        @Override
        public Type getGenericType() {
            return String.class;
        }

        @Override
        public MediaType getMediaType() {
            return MediaType.TEXT_PLAIN_TYPE;
        }

        @Override
        public String getData() {
            return data;
        }

        @Override
        public String getId() {
            return null;
        }

        @Override
        public String getName() {
            return SSE_EVENT_NAME;
        }

        @Override
        public String getComment() {
            return "";
        }

        @Override
        public long getReconnectDelay() {
            return 0;
        }

        @Override
        public boolean isReconnectDelaySet() {
            return false;
        }
    }
}
