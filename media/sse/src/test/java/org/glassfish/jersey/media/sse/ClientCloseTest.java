/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015-2017 Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;

import javax.inject.Singleton;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertTrue;

/**
 * @author Petr Janouch (petr.janouch at oracle.com)
 */
public class ClientCloseTest extends JerseyTest {

    /**
     * The test test that SSE connection is really closed when EventSource.close() is called.
     * <p/>
     * This test is very HttpURLConnection and Grizzly server specific, so it will probably fail, if other client and server
     * transport are used.
     */
    @Test
    public void testClose() throws InterruptedException {
        WebTarget sseTarget = target("sse");

        CountDownLatch eventLatch = new CountDownLatch(3);
        CountDownLatch eventLatch2 = new CountDownLatch(4);
        EventSource eventSource = new EventSource(sseTarget) {
            @Override
            public void onEvent(final InboundEvent inboundEvent) {
                eventLatch.countDown();
                eventLatch2.countDown();
            }
        };

        // Server sends 3 events we are interested in.
        IntStream.range(0, 3).forEach((i) -> assertEquals("OK",
                target("sse/send").request().get().readEntity(String.class)));
        assertTrue(eventLatch.await(5, TimeUnit.SECONDS));

        // After receiving the 3 events, we try to close.
        eventSource.close();

        // Unfortunately the HTTPURLConnection is blocked in read() method, so it will close only after receiving the next event.
        assertEquals("OK", target("sse/send").request().get().readEntity(String.class));
        // Wait for the event that will unblock the HTTPURLConnection and result in sending FIN.
        assertTrue(eventLatch2.await(5, TimeUnit.SECONDS));
        // Now it is interesting. Client has sent FIN, but Grizzly does not listen for client input (selector READ key is
        // disabled), while streaming the response. For some reason we need to send 2 more events for Grizzly to notice
        // that the client is gone.
        assertEquals("OK", target("sse/send").request().get().readEntity(String.class));
        assertEquals("OK", target("sse/send").request().get().readEntity(String.class));
        // Now the grizzly should notice that the SSE connection is finally dead and sending events from the server will fail.
        assertEquals("NOK", target("sse/send").request().get().readEntity(String.class));
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(SseEndpoint.class);
    }

    @Override
    protected void configureClient(ClientConfig config) {
        config.register(SseFeature.class);
    }

    @Singleton
    @Path("sse")
    public static class SseEndpoint {

        private final EventOutput eventOutput = new EventOutput();

        @GET
        @Path("send")
        public String sendEvent() throws InterruptedException {
            OutboundEvent event = new OutboundEvent.Builder().data("An event").build();
            try {
                if (eventOutput.isClosed()) {
                    return "NOK";
                }

                eventOutput.write(event);
            } catch (IOException e) {
                return "NOK";
            }

            return "OK";
        }

        @GET
        @Produces(SseFeature.SERVER_SENT_EVENTS)
        public EventOutput get() {
            return eventOutput;
        }
    }
}
