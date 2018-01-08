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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseBroadcaster;
import javax.ws.rs.sse.SseEventSink;

import javax.inject.Singleton;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test broadcaster behaviour when closing.
 *
 * Proves, that broadcaster attempts to send the messages remaining in the buffer after it receives the close signal.
 *
 * @author Adam Lindenthal (adam.lindenthal at oracle.com)
 */
public class BroadcasterCloseTest extends JerseyTest {

    private static final int SLOW_SUBSCRIBER_LATENCY = 200;
    private static final int MSG_COUNT = 8;
    private static final CountDownLatch onCompleteLatch = new CountDownLatch(1);

    @Override
    protected Application configure() {
        return new ResourceConfig(SseResource.class);
    }

    @Path("events")
    @Singleton
    public static class SseResource {
        private final Sse sse;
        private final SseBroadcaster broadcaster;
        private final List<String> data = new ArrayList<>();

        public SseResource(@Context final Sse sse) {
            this.sse = sse;
            this.broadcaster = sse.newBroadcaster();
            this.broadcaster.register(new SseEventSink() {

                volatile boolean closed = false;

                @Override
                public boolean isClosed() {
                    return closed;
                }

                @Override
                public CompletionStage<?> send(OutboundSseEvent event) {
                    try {
                        Thread.sleep(SLOW_SUBSCRIBER_LATENCY);
                    } catch (InterruptedException e) {
                        System.out.println("Slow subscriber's sleep was interrupted.");
                    }
                    data.add("" + event.getData());

                    return CompletableFuture.completedFuture(null);
                }

                @Override
                public void close() {
                    System.out.println("Slow subscriber completed");
                    onCompleteLatch.countDown();
                    this.closed = true;
                }
            });
        }

        @GET
        @Produces(MediaType.SERVER_SENT_EVENTS)
        public void getServerSentEvents(@Context final SseEventSink eventSink) {
            broadcaster.register(eventSink);
        }

        @GET
        @Path("push/{msg}")
        public String addMessage(@PathParam("msg") String message) throws InterruptedException {
            broadcaster.broadcast(sse.newEvent(message));
            return "Message added.";
        }

        @GET
        @Path("close")
        public String closeMe() {
            broadcaster.close();
            return "Closed";
        }

        @GET
        @Path("result")
        public String getResult() {
            return data.stream().collect(Collectors.joining(","));
        }
    }

    @Test
    public void testBroadcasterKeepsSendingAfterCLose() throws InterruptedException {
        // push some events to the broadcaster
        IntStream.range(0, MSG_COUNT).forEach((i) -> {
            final Response response = target()
                    .path("events/push/{msg}")
                    .resolveTemplate("msg", "msg" + i)
                    .request()
                    .get();
            Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        });

        // instruct broadcaster to close
        final Response response = target().path("events/close").request().get();
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // send one more message (should be rejected -> request will fail)
        final Response badResponse = target()
                                        .path("events/push/{msg}")
                                        .resolveTemplate("msg", "too-late")
                                        .request()
                                        .get();
        Assert.assertNotEquals(Response.Status.OK.getStatusCode(), badResponse.getStatus());

        // wait up to latency * msgcount (+1 as reserve) before the server shuts down
        Assert.assertTrue(onCompleteLatch.await(SLOW_SUBSCRIBER_LATENCY * (MSG_COUNT + 1), TimeUnit.MILLISECONDS));

        // get data gathered by the slow subsciber
        String result = target().path("events/result").request().get(String.class);
        final String[] resultArray = result.split(",");

        // check, that broadcaster sent all the buffered events to the subscriber before completely closing
        Assert.assertEquals(MSG_COUNT, resultArray.length);
        for (int i = 0; i < MSG_COUNT; i++) {
            Assert.assertEquals("msg" + i, resultArray[i]);
        }
    }
}
