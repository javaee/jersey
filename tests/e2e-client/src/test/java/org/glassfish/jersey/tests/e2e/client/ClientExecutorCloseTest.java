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
package org.glassfish.jersey.tests.e2e.client;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import javax.ws.rs.sse.SseEventSource;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Petr Janouch (petr.janouch at oracle.com)
 */
public class ClientExecutorCloseTest extends JerseyTest {
    private static CountDownLatch cdl = new CountDownLatch(2);
    private static boolean schedulerFound = false;

    /**
     * Tests that closing a client shuts down a corresponding client async executor service.
     */
    @Test
    @Ignore("Jersey uses ForkJoin common pool by default, which shouldn't be closed when client closes.")
    public void testCloseAsyncExecutor() throws InterruptedException {
        assertFalse(clientExecutorThreadPresent());
        target("resource").request().async().get();
        final SseEventSource eventSource = SseEventSource
                .target(target("resource/fail"))
                .reconnectingEvery(11, TimeUnit.MILLISECONDS)
                .build();
        eventSource.register(System.out::println);
        eventSource.open();
        assertTrue("Waiting for eventSource to open time-outed", cdl.await(5000, TimeUnit.MILLISECONDS));
        assertTrue("Client async executor thread not found.", clientExecutorThreadPresent());
        assertTrue("Scheduler thread not found.", schedulerFound);
        client().close();
        assertFalse("Client async executor thread should have been already removed.",
                clientExecutorThreadPresent());
        assertFalse("Client background scheduler thread should have been already removed.",
                clientSchedulerThreadPresent());
    }

    private boolean clientExecutorThreadPresent() {
        Set<Thread> threads = Thread.getAllStackTraces().keySet();
        return threads.stream().map(Thread::getName).anyMatch(name -> name.contains("jersey-client-async-executor"));
    }

    private static boolean clientSchedulerThreadPresent() {
        Set<Thread> threads = Thread.getAllStackTraces().keySet();
        for (Thread thread : threads) {
            if (thread.getName().contains("jersey-client-background-scheduler")) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(Resource.class);
    }

    @Path("resource")
    public static class Resource {

        @GET
        public String getHello() {
            return "Hello";
        }

        @GET
        @Path("fail")
        public Response fail() {
            // should return false on first (regular) connect and true on reconnect
            schedulerFound = clientSchedulerThreadPresent();
            cdl.countDown();
            // simulate unsuccessful connect attempt -> force reconnect (eventSource will submit a task into scheduler)
            return Response.status(503).build();
        }
    }
}
