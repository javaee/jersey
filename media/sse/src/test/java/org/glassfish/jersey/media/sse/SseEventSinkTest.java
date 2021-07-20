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
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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
import java.util.concurrent.TimeUnit;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseEventSink;
import javax.ws.rs.sse.SseEventSource;

import javax.inject.Singleton;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class SseEventSinkTest extends JerseyTest {

    private static final CountDownLatch CLIENT_RECEIVED_A_MESSAGE_LATCH = new CountDownLatch(1);
    private static final CountDownLatch RESOURCE_METHOD_END_LATCH = new CountDownLatch(1);

    private static volatile SseEventSink output = null;

    @Override
    protected Application configure() {
        return new ResourceConfig(SseEndpoint.class);
    }

    @Singleton
    @Path("sse")
    public static class SseEndpoint {

        @GET
        @Produces(SseFeature.SERVER_SENT_EVENTS)
        public void get(@Context SseEventSink output, @Context Sse sse) throws InterruptedException {
            SseEventSinkTest.output = output;

            System.out.println("### Server is about to send a message.");

            output.send(sse.newEvent("How will this end?"));

            System.out.println("### Server waiting for client to receive a message.");

            CLIENT_RECEIVED_A_MESSAGE_LATCH.await();

            System.out.println("### Server resource method invocation end.");

            RESOURCE_METHOD_END_LATCH.countDown();
        }
    }

    /**
     * The test test that SSE connection is really closed when SseEventSource.close() is called.
     * <p/>
     * This test is very HttpURLConnection and Grizzly server specific, so it will probably fail, if other client and server
     * transport are used.
     */
    @Test
    public void testBlockingResourceMethod() throws InterruptedException {
        WebTarget sseTarget = target("sse");

        final CountDownLatch eventLatch = new CountDownLatch(3);
        SseEventSource eventSource = SseEventSource.target(sseTarget).build();
        eventSource.register((event) -> {
            System.out.println("### Client received: " + event);
            CLIENT_RECEIVED_A_MESSAGE_LATCH.countDown();
        });
        eventSource.open();

        // client waiting for confirmation that resource method ended.
        assertTrue(RESOURCE_METHOD_END_LATCH.await(10000, TimeUnit.MILLISECONDS));
    }
}
