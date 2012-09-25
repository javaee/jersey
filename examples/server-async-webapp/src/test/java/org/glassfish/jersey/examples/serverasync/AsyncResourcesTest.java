/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.examples.serverasync;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.client.ClientException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * Server-side asynchronous web application example test.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class AsyncResourcesTest extends JerseyTest {
    private static final String CONTEXT_PATH = "server-async-webapp";
    private static final Logger LOGGER = Logger.getLogger(AsyncResourcesTest.class.getName());

    @Override
    protected Application configure() {
        return new AsyncJaxrsApplication();
    }

    @Override
    protected URI getBaseUri() {
        return UriBuilder.fromUri(super.getBaseUri()).path(CONTEXT_PATH).build();

    }

    /**
     * Test asynchronous message exchange resource.
     *
     * @throws Exception in case of a test failure.
     */
    @Test
    public void testBlockingPostChatResource() throws Exception {
        final WebTarget resourceTarget = target().path("message");

        final int MAX_MESSAGES = 100;
        final int LATCH_WAIT_TIMEOUT = 10;

        final ExecutorService executor = Executors.newCachedThreadPool(
                new ThreadFactoryBuilder().setNameFormat("async-resource-test-%d").build());

        final Map<Integer, String> postResponses = new ConcurrentHashMap<Integer, String>();
        final Map<Integer, String> getResponses = new ConcurrentHashMap<Integer, String>();

        final CountDownLatch postRequestLatch = new CountDownLatch(MAX_MESSAGES);
        final CountDownLatch getRequestLatch = new CountDownLatch(MAX_MESSAGES);

        try {
            for (int i = 0; i < MAX_MESSAGES; i++) {
                final int requestId = i;
                executor.submit(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            int attemptCounter = 0;
                            while (true) {
                                attemptCounter++;
                                try {
                                    final String response = resourceTarget.request()
                                            .post(Entity.text("" + requestId), String.class);
                                    postResponses.put(requestId, response);
                                    break;
                                } catch (Throwable t) {
                                    LOGGER.log(Level.WARNING, String.format("Error POSTING message <%s> for %d. time.",
                                            requestId, attemptCounter), t);
                                }
                                if (attemptCounter > 3) {
                                    break;
                                }
                                Thread.sleep(10);
                            }
                        } catch (InterruptedException ignored) {
                            LOGGER.log(Level.WARNING,
                                    String.format("Error POSTING message <%s>: Interrupted", requestId), ignored);
                        } finally {
                            postRequestLatch.countDown();
                        }
                    }
                });
                executor.submit(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            int attemptCounter = 0;
                            while (true) {
                                attemptCounter++;
                                try {
                                    final String response = resourceTarget.queryParam("id", requestId).request()
                                            .get(String.class);
                                    getResponses.put(requestId, response);
                                    break;
                                } catch (Throwable t) {
                                    LOGGER.log(Level.SEVERE, String.format("Error sending GET request <%s> for %d. time.",
                                            requestId, attemptCounter), t);
                                }
                                if (attemptCounter > 3) {
                                    break;
                                }
                                Thread.sleep(10);
                            }
                        } catch (InterruptedException ignored) {
                            LOGGER.log(Level.WARNING,
                                    String.format("Error sending GET message <%s>: Interrupted", requestId), ignored);
                        } finally {
                            getRequestLatch.countDown();
                        }
                    }
                });
            }

            if (!postRequestLatch.await(LATCH_WAIT_TIMEOUT, TimeUnit.SECONDS)) {
                LOGGER.log(Level.SEVERE, "Waiting for all POST requests to complete has timed out.");
            }
            if (!getRequestLatch.await(LATCH_WAIT_TIMEOUT, TimeUnit.SECONDS)) {
                LOGGER.log(Level.SEVERE, "Waiting for all GET requests to complete has timed out.");
            }
        } finally {
            executor.shutdownNow();
        }

        StringBuilder messageBuilder = new StringBuilder("POST responses received: ").append(postResponses.size()).append("\n");
        for (Map.Entry<Integer, String> postResponseEntry : postResponses.entrySet()) {
            messageBuilder.append("POST response for message ")
                    .append(postResponseEntry.getKey()).append(": ")
                    .append(postResponseEntry.getValue()).append('\n');
        }
        messageBuilder.append('\n');
        messageBuilder.append("GET responses received: ").append(getResponses.size()).append("\n");
        for (Map.Entry<Integer, String> getResponseEntry : getResponses.entrySet()) {
            messageBuilder.append("GET response for message ")
                    .append(getResponseEntry.getKey()).append(": ")
                    .append(getResponseEntry.getValue()).append('\n');
        }
        LOGGER.info(messageBuilder.toString());

        for (Map.Entry<Integer, String> postResponseEntry : postResponses.entrySet()) {
            assertEquals(
                    "Unexpected POST notification response for message " + postResponseEntry.getKey(),
                    "Message posted.", postResponseEntry.getValue());
        }

        final List<Integer> lost = new LinkedList<Integer>();
        final Collection<String> getResponseValues = getResponses.values();
        for (int i = 0; i < MAX_MESSAGES; i++) {
            if (!getResponseValues.contains("" + i)) {
                lost.add(i);
            }
        }
        if (!lost.isEmpty()) {
            fail("Detected a posted message loss(es): " + lost.toString());
        }
        assertEquals(MAX_MESSAGES, postResponses.size());
        assertEquals(MAX_MESSAGES, getResponses.size());
    }

    /**
     * Test asynchronous long-running resource.
     *
     * @throws Exception in case of a test failure.
     */
    @Test
    public void testLongRunningResource() throws Exception {
        final WebTarget resourceTarget = target().path("long-running");

        final int MAX_MESSAGES = 100;
        final int LATCH_WAIT_TIMEOUT = 10;

        final ExecutorService executor = Executors.newCachedThreadPool(
                new ThreadFactoryBuilder().setNameFormat("async-resource-test-%d").build());

        final Map<Integer, String> getResponses = new ConcurrentHashMap<Integer, String>();

        final CountDownLatch getRequestLatch = new CountDownLatch(MAX_MESSAGES);

        try {
            for (int i = 0; i < MAX_MESSAGES; i++) {
                final int requestId = i;
                resourceTarget.request().async().get(new InvocationCallback<String>() {
                    private AtomicInteger failures = new AtomicInteger(0);

                    @Override
                    public void completed(String response) {
                        getResponses.put(requestId, response);
                        getRequestLatch.countDown();
                    }

                    @Override
                    public void failed(ClientException error) {
                        if (failures.incrementAndGet() > 3) {
                            return;
                        }
                        if (error.getCause() instanceof IOException) {
                            resourceTarget.request().async().get(this);
                        }
                    }
                });
            }

            if (!getRequestLatch.await(LATCH_WAIT_TIMEOUT, TimeUnit.SECONDS)) {
                LOGGER.log(Level.SEVERE, "Waiting for all GET requests to complete has timed out.");
            }
        } finally {
            executor.shutdownNow();
        }

        StringBuilder messageBuilder = new StringBuilder("GET responses received: ").append(getResponses.size()).append("\n");
        for (Map.Entry<Integer, String> getResponseEntry : getResponses.entrySet()) {
            messageBuilder.append("GET response for message ")
                    .append(getResponseEntry.getKey()).append(": ")
                    .append(getResponseEntry.getValue()).append('\n');
        }
        LOGGER.info(messageBuilder.toString());

        for (Map.Entry<Integer, String> entry : getResponses.entrySet()) {
            assertEquals(
                    "Unexpected GET notification response for message " + entry.getKey(),
                    "Hello async world!", entry.getValue());
        }
        assertEquals(MAX_MESSAGES, getResponses.size());
    }
}
