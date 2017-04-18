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

import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseBroadcaster;
import javax.ws.rs.sse.SseEventSink;
import javax.ws.rs.sse.SseEventSource;

import javax.inject.Singleton;

import org.glassfish.jersey.client.ClientAsyncExecutor;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.server.ManagedAsyncExecutor;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.spi.ThreadPoolExecutorProvider;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Assert;
import org.junit.Test;

/**
 * Managed executor service injection and propagation into broadcaster test.
 *
 * @author Adam Lindenthal (adam.lindenthal at oracle.com)
 */
public class BroadcasterExecutorTest extends JerseyTest {

    private static final String THREAD_PREFIX = "custom-executor-thread";
    private static Logger LOGGER = Logger.getLogger(BroadcasterExecutorTest.class.getName());

    private static CountDownLatch closeLatch = new CountDownLatch(1);
    private static CountDownLatch txLatch = new CountDownLatch(2);

    private static boolean sendThreadOk = false;
    private static boolean onCompleteThreadOk = false;

    @Path("sse")
    @Singleton
    public static class SseResource {
        private final Sse sse;
        private SseBroadcaster broadcaster;

        public SseResource(@Context final Sse sse) {
            this.sse = sse;
            broadcaster = sse.newBroadcaster();
            System.out.println("Broadcaster created: " + broadcaster);
        }

        @GET
        @Produces(MediaType.SERVER_SENT_EVENTS)
        @Path("events")
        public void getServerSentEvents(@Context final SseEventSink eventSink, @Context final Sse sse) {

            // TODO JAX-RS 2.1
            broadcaster.register(new SseEventSink() {
                @Override
                public boolean isClosed() {
                    return eventSink.isClosed();
                }

                @Override
                public CompletionStage<?> send(OutboundSseEvent event) {
                    final String name = Thread.currentThread().getName();
                    LOGGER.info("onNext called with [" + event + "] from " + name);
                    sendThreadOk = name.startsWith(THREAD_PREFIX);
                    txLatch.countDown();
                    return eventSink.send(event);
                }

                @Override
                public void close() {
                    final String name = Thread.currentThread().getName();
                    LOGGER.info("onComplete called from " + name);
                    onCompleteThreadOk = name.startsWith(THREAD_PREFIX);
                    closeLatch.countDown();
                    eventSink.close();
                }
            });
        }

        @Path("push/{msg}")
        @GET
        public String pushMessage(@PathParam("msg") final String msg) {
            broadcaster.broadcast(sse.newEventBuilder().data(msg).build());
            return "Broadcasting message: " + msg;
        }

        @Path("close")
        @GET
        public String close() {
            broadcaster.close();
            return "Closed.";
        }
    }

    @Override
    protected Application configure() {
        final ResourceConfig rc = new ResourceConfig(SseResource.class);
        rc.property(ServerProperties.WADL_FEATURE_DISABLE, true);
        rc.register(new CustomManagedAsyncExecutorProvider());
        return rc;
    }

    @Override
    protected void configureClient(ClientConfig config) {
        config.register(new CustomClientAsyncExecutor());
    }

    @ManagedAsyncExecutor
    private static class CustomManagedAsyncExecutorProvider extends ThreadPoolExecutorProvider {
        CustomManagedAsyncExecutorProvider() {
            super("custom-executor-thread");
        }
    }

    @ClientAsyncExecutor
    private static class CustomClientAsyncExecutor extends ThreadPoolExecutorProvider {
        CustomClientAsyncExecutor() {
            super("custom-client-executor");
        }
    }

    @Test
    public void test() throws InterruptedException {
        final String[] onEventThreadName = {""};
        SseEventSource eventSource = SseEventSource
                .target(target().path("sse/events"))
                .build();

        eventSource.register((event) -> {
                    LOGGER.info("Event: " + event + " from: " + Thread.currentThread().getName());
                    onEventThreadName[0] = Thread.currentThread().getName();
                }
        );

        eventSource.open();

        target().path("sse/push/firstBroadcast").request().get(String.class);
        target().path("sse/push/secondBroadcast").request().get(String.class);
        Assert.assertTrue("txLatch time-outed.", txLatch.await(2000, TimeUnit.MILLISECONDS));

        target().path("sse/close").request().get();
        Assert.assertTrue("closeLatch time-outed.", closeLatch.await(2000, TimeUnit.MILLISECONDS));

        Assert.assertTrue("send either not invoked at all or from wrong thread", sendThreadOk);
        Assert.assertTrue("onComplete either not invoked at all or from wrong thread", onCompleteThreadOk);

        Assert.assertTrue("Client event called from wrong thread ( " + onEventThreadName[0] + ")",
               onEventThreadName[0].startsWith("custom-client-executor"));
    }
}
