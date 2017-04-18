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

package org.glassfish.jersey.media.sse;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseEventSink;
import javax.ws.rs.sse.SseEventSource;

import javax.inject.Singleton;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test, that {@code SseEventSink} and the connection is closed eventually after closing {@code SseEventSource} on client side.
 *
 * @author Adam Lindenthal (adam.lindenthal at oracle.com)
 */
public class SseEventSinkCloseTest extends JerseyTest {

    private static Logger LOGGER = Logger.getLogger(SseEventSinkCloseTest.class.getName());
    private static volatile SseEventSink output = null;
    private static CountDownLatch openLatch = new CountDownLatch(1);

    @Singleton
    @Path("sse")
    public static class SseEndpoint {
        @GET
        @Path("send")
        public String sendEvent(@Context Sse sse) throws InterruptedException {
            OutboundSseEvent event = sse.newEventBuilder().data("An event").build();
            if (!output.isClosed()) {
                output.send(event);
                return "OK";
            }
            return "Closed";
        }

        @GET
        @Produces(SseFeature.SERVER_SENT_EVENTS)
        public void get(@Context SseEventSink output, @Context Sse sse) {
            SseEventSinkCloseTest.output = output;
            openLatch.countDown();
        }
    }

    /**
     * The test test that SSE connection is really closed when SseEventSource.close() is called.
     * <p/>
     * This test is very HttpURLConnection and Grizzly server specific, so it will probably fail, if other client and server
     * transport are used.
     */
    @Test
    public void testClose() throws InterruptedException {
        WebTarget sseTarget = target("sse");

        final CountDownLatch eventLatch = new CountDownLatch(3);
        SseEventSource eventSource = SseEventSource.target(sseTarget).build();
        eventSource.register((event) -> eventLatch.countDown());
        eventSource.open();
        openLatch.await();

        // Tell server to send us 3 events
        for (int i = 0; i < 3; i++) {
            final String response = target("sse/send").request().get().readEntity(String.class);
            assertEquals("OK", response);
        }

        // ... and wait for the events to be processed by the client side, then close the eventSource
        assertTrue("EventLatch timed out.", eventLatch.await(5, TimeUnit.SECONDS));
        eventSource.close();
        assertEquals("SseEventSource should have been already closed", false, eventSource.isOpen());

        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

        CountDownLatch closeLatch = new CountDownLatch(100);
        executor.scheduleAtFixedRate(() -> {
            if (output.isClosed()) {
                // countdown to zero
                while (closeLatch.getCount() > 0) {
                    closeLatch.countDown();
                    return;
                }
            }
            final Response response = target("sse/send").request().get();
            LOGGER.info(200 == response.getStatus() ? "Still alive" : "Error received");
            closeLatch.countDown();
        }, 0, 100, TimeUnit.MILLISECONDS);

        assertTrue(closeLatch.await(10000, TimeUnit.MILLISECONDS));
        executor.shutdown();
        assertTrue("SseEventOutput should have been already closed.", output.isClosed());
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(SseEndpoint.class);
    }
}
