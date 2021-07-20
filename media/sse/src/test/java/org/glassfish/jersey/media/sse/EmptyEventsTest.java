/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016-2017 Oracle and/or its affiliates. All rights reserved.
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

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

/**
 * Tests handling of empty SSE events.
 */
public class EmptyEventsTest extends JerseyTest {

    @Override
    protected Application configure() {
        return new ResourceConfig(SseTestResource.class, SseFeature.class);
    }

    @Override
    protected void configureClient(ClientConfig config) {
        config.register(SseFeature.class);
    }

    /**
     * Tests a situation when 2 non-empty SSE events are separated with an empty one.
     */
    @Test
    public void test1EmptyEvent() throws InterruptedException {
        doTest("sse/1");
    }

    /**
     * Tests a situation when 2 non-empty SSE events are separated with 2 empty ones.
     */
    @Test
    public void test2EmptyEvents() throws InterruptedException {
        doTest("sse/2");
    }

    /**
     * Tests a situation when 2 non-empty SSE events are separated with 3 empty ones.
     */
    @Test
    public void test3EmptyEvents() throws InterruptedException {
        doTest("sse/3");
    }

    private void doTest(String target) throws InterruptedException {
        List<String> receivedNames = new ArrayList<>();
        List<String> receivedData = new LinkedList<>();

        WebTarget sseTarget = target(target);
        CountDownLatch latch = new CountDownLatch(2);
        new EventSource(sseTarget) {
            @Override
            public void onEvent(InboundEvent inboundEvent) {
                receivedNames.add(inboundEvent.getName());
                try {
                    receivedData.add(new String(inboundEvent.getRawData(), "ASCII"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                latch.countDown();
            }
        };

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertTrue(receivedNames.contains("e1"));
        assertTrue(receivedNames.contains("e2"));
        assertTrue(receivedData.contains("d1"));
        assertTrue(receivedData.contains("d2"));
    }

    @Path("/sse")
    public static class SseTestResource {

        @GET
        @Path("/1")
        @Produces("text/event-stream;charset=utf-8")
        public String send1EmptyEvent() {
            return "event: e1\r\n"
                    + "data: d1\r\n"
                    + "\r\n"
                    // end of e1
                    + "\r\n"
                    // end of an empty event
                    + "event: e2\r\n"
                    + "data: d2\r\n"
                    + "\r\n";
        }

        @GET
        @Path("/2")
        @Produces("text/event-stream;charset=utf-8")
        public String send2EmptyEvents() {
            return "event: e1\r\n"
                    + "data: d1\r\n"
                    + "\r\n"
                    // end of e1
                    + "\r\n"
                    // end of an empty event
                    + "\r\n"
                    // end of an empty event
                    + "event: e2\r\n"
                    + "data: d2\r\n"
                    + "\r\n";
        }

        @GET
        @Path("/3")
        @Produces("text/event-stream;charset=utf-8")
        public String send3EmptyEvents() {
            return "event: e1\r\n"
                    + "data: d1\r\n"
                    + "\r\n"
                    // end of e1
                    + "\r\n"
                    // end of an empty event
                    + "\r\n"
                    // end of an empty event
                    + "\r\n"
                    // end of an empty event
                    + "event: e2\r\n"
                    + "data: d2\r\n"
                    + "\r\n";
        }
    }
}
