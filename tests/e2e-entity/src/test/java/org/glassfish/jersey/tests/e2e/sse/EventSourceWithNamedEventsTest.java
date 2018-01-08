/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2017 Oracle and/or its affiliates. All rights reserved.
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
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.Produces;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;

import javax.inject.Singleton;

import org.glassfish.jersey.media.sse.EventListener;
import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.EventSource;
import org.glassfish.jersey.media.sse.InboundEvent;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.glassfish.jersey.media.sse.SseFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests handling of SSEs with name defined in {@link EventSource}.
 *
 * @author Miroslav Fuksa
 *
 */
public class EventSourceWithNamedEventsTest extends JerseyTest {

    public static final String SSE_NAME = "message-to-client";

    @Override
    protected Application configure() {
        final ResourceConfig resourceConfig = new ResourceConfig(SseResource.class, SseFeature.class);
        return resourceConfig;
    }

    public static final int MSG_COUNT = 10;
    private static final CountDownLatch latch = new CountDownLatch(MSG_COUNT);

    @Path("events")
    @Singleton
    public static class SseResource {

        @GET
        @Produces(SseFeature.SERVER_SENT_EVENTS)
        public EventOutput getServerSentEvents() {
            final EventOutput eventOutput = new EventOutput();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        int i = 0;
                        while (latch.getCount() > 0) {

                            // send message with name "message-to-client" -> should be read by the client
                            eventOutput.write(new OutboundEvent.Builder()
                                    .name("message-to-client")
                                    .mediaType(MediaType.TEXT_PLAIN_TYPE)
                                    .data(Integer.class, i)
                                    .build());

                            // send another event with name "foo" -> should be ignored by the client
                            eventOutput.write(new OutboundEvent.Builder()
                                    .name("foo")
                                    .mediaType(MediaType.TEXT_PLAIN_TYPE)
                                    .data(String.class, "bar")
                                    .build());

                            // send another un-mamed event -> should be ignored by the client
                            eventOutput.write(new OutboundEvent.Builder()
                                    .mediaType(MediaType.TEXT_PLAIN_TYPE)
                                    .data(String.class, "baz")
                                    .build());
                            latch.countDown();
                            i++;
                        }

                    } catch (IOException e) {
                        throw new RuntimeException("Error when writing the event.", e);
                    } finally {
                        try {
                            eventOutput.close();
                        } catch (IOException ioClose) {
                            throw new RuntimeException("Error when closing the event output.", ioClose);
                        }
                    }
                }
            }).start();
            return eventOutput;
        }
    }


    @Test
    public void testWithEventSource() throws IOException, NoSuchAlgorithmException, InterruptedException {
        final WebTarget endpoint = target().register(SseFeature.class).path("events");
        EventSource eventSource = EventSource.target(endpoint).build();
        final CountDownLatch count = new CountDownLatch(MSG_COUNT);

        final EventListener listener = new EventListener() {
            @Override
            public void onEvent(InboundEvent inboundEvent) {
                try {
                    final Integer data = inboundEvent.readData(Integer.class);
                    System.out.println(inboundEvent.getName() + "; " + data);
                    Assert.assertEquals(SSE_NAME, inboundEvent.getName());
                    Assert.assertEquals(MSG_COUNT - count.getCount(), data.intValue());
                    count.countDown();
                } catch (ProcessingException ex) {
                    throw new RuntimeException("Error when deserializing of data.", ex);
                }
            }
        };
        eventSource.register(listener, "message-to-client");
        eventSource.open();
        final boolean sent = latch.await(5 * getAsyncTimeoutMultiplier(), TimeUnit.SECONDS);
        Assert.assertTrue("Awaiting for SSE message has timeout. Not all message were sent.", sent);
        final boolean handled = count.await(5 * getAsyncTimeoutMultiplier(), TimeUnit.SECONDS);
        Assert.assertTrue("Awaiting for SSE message has timeout. Not all message were handled by the listener.", handled);
    }
}
