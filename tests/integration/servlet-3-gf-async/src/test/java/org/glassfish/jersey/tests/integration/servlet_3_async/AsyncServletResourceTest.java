/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2017 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.tests.integration.servlet_3_async;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.internal.guava.ThreadFactoryBuilder;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.process.JerseyProcessingUncaughtExceptionHandler;
import org.glassfish.jersey.test.DeploymentContext;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.ServletDeploymentContext;
import org.glassfish.jersey.test.external.ExternalTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Asynchronous servlet-deployed resource test.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class AsyncServletResourceTest extends JerseyTest {

    private static final Logger LOGGER = Logger.getLogger(AsyncServletResourceTest.class.getName());

    private static class ResponseRecord {

        final int status;
        final String message;

        private ResponseRecord(final int status, final String message) {
            this.status = status;
            this.message = message;
        }

        @Override
        public String toString() {
            return status + " : \"" + message + '\"';
        }
    }

    @Override
    protected DeploymentContext configureDeployment() {
        return ServletDeploymentContext.builder(new Application())
                .contextPath("servlet-3-gf-async")
                .build();
    }

    @Override
    protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
        return new ExternalTestContainerFactory();
    }

    /**
     * Test asynchronous servlet-deployed resource.
     *
     * @throws InterruptedException in case the waiting for all requests to complete was interrupted.
     */
    @Test
    public void testAsyncServlet() throws InterruptedException {
        final WebTarget resourceTarget = target("async");
        resourceTarget.register(LoggingFeature.class);
        final String expectedResponse = AsyncServletResource.HELLO_ASYNC_WORLD;

        final int MAX_MESSAGES = 50;
        final int LATCH_WAIT_TIMEOUT = 10;
        final boolean debugMode = false;
        final boolean sequentialGet = false;
        final Object sequentialGetLock = new Object();

        final ExecutorService executor = Executors.newCachedThreadPool(new ThreadFactoryBuilder()
                .setNameFormat("async-resource-test-%d")
                .setUncaughtExceptionHandler(new JerseyProcessingUncaughtExceptionHandler())
                .build());

        final Map<Integer, ResponseRecord> getResponses = new ConcurrentHashMap<>();

        final CountDownLatch getRequestLatch = new CountDownLatch(MAX_MESSAGES);

        try {
            for (int i = 0; i < MAX_MESSAGES; i++) {
                final int requestId = i;
                executor.submit(new Runnable() {

                    @Override
                    public void run() {
                        //noinspection PointlessBooleanExpression,ConstantConditions
                        if (debugMode || sequentialGet) {
                            synchronized (sequentialGetLock) {
                                get();
                            }
                        } else {
                            get();
                        }
                    }

                    private void get() {
                        try {
                            final Response response = resourceTarget.request().get();
                            getResponses.put(requestId, new ResponseRecord(response.getStatus(),
                                    response.readEntity(String.class)));
                        } catch (final Throwable t) {
                            t.printStackTrace();
                        } finally {
                            getRequestLatch.countDown();
                        }
                    }
                });
            }

            //noinspection ConstantConditions
            if (debugMode) {
                getRequestLatch.await();
            } else {
                assertTrue("Waiting for all GET requests to complete has timed out.",
                        getRequestLatch.await(LATCH_WAIT_TIMEOUT * getAsyncTimeoutMultiplier(), TimeUnit.SECONDS));
            }
        } finally {
            executor.shutdownNow();
        }

        final StringBuilder messageBuilder = new StringBuilder();
        for (final Map.Entry<Integer, ResponseRecord> getResponseEntry : getResponses.entrySet()) {
            messageBuilder.append("GET response for message ").append(getResponseEntry.getKey()).append(": ")
                    .append(getResponseEntry.getValue().toString()).append('\n');
        }
        LOGGER.info(messageBuilder.toString());

        assertEquals(MAX_MESSAGES, getResponses.size());
        for (final Map.Entry<Integer, ResponseRecord> entry : getResponses.entrySet()) {
            assertEquals("Unexpected GET response status for request " + entry.getKey(), 200, entry.getValue().status);
            assertEquals("Unexpected GET response message for request " + entry.getKey(), expectedResponse,
                    entry.getValue().message);
        }
    }

    /**
     * Test canceling of an async request to a servlet-deployed resource.
     *
     * @throws InterruptedException in case the waiting for all requests to complete was interrupted.
     */
    @Test
    public void testAsyncRequestCanceling() throws InterruptedException {
        final WebTarget resourceTarget = target("async/canceled");
        resourceTarget.register(LoggingFeature.class);

        final int MAX_MESSAGES = 10;
        final int LATCH_WAIT_TIMEOUT = 10;
        final boolean debugMode = false;
        final boolean sequentialGet = false;
        final boolean sequentialPost = false;
        final Object sequentialGetLock = new Object();
        final Object sequentialPostLock = new Object();

        final ExecutorService executor = Executors.newCachedThreadPool(new ThreadFactoryBuilder()
                .setNameFormat("async-canceled-resource-test-%d")
                .setUncaughtExceptionHandler(new JerseyProcessingUncaughtExceptionHandler())
                .build());

        final Map<Integer, String> postResponses = new ConcurrentHashMap<>();
        final Map<Integer, String> getResponses = new ConcurrentHashMap<>();

        final CountDownLatch postRequestLatch = new CountDownLatch(MAX_MESSAGES);
        final CountDownLatch getRequestLatch = new CountDownLatch(MAX_MESSAGES);

        try {
            for (int i = 0; i < MAX_MESSAGES; i++) {
                final int requestId = i;
                executor.submit(new Runnable() {

                    @Override
                    public void run() {
                        //noinspection PointlessBooleanExpression,ConstantConditions
                        if (debugMode || sequentialGet) {
                            synchronized (sequentialGetLock) {
                                get();
                            }
                        } else {
                            get();
                        }
                    }

                    private void get() {
                        try {
                            final String response = resourceTarget.queryParam("id", requestId).request().get(String.class);
                            getResponses.put(requestId, response);
                        } catch (final WebApplicationException ex) {
                            final Response response = ex.getResponse();
                            getResponses.put(requestId, response.getStatus() + ": " + response.readEntity(String.class));
                        } finally {
                            getRequestLatch.countDown();
                        }
                    }
                });
                executor.submit(new Runnable() {

                    @Override
                    public void run() {
                        //noinspection PointlessBooleanExpression,ConstantConditions
                        if (debugMode || sequentialPost) {
                            synchronized (sequentialPostLock) {
                                post();
                            }
                        } else {
                            post();
                        }
                    }

                    private void post() throws ProcessingException {
                        try {
                            final String response = resourceTarget.request().post(Entity.text("" + requestId), String.class);
                            postResponses.put(requestId, response);
                        } finally {
                            postRequestLatch.countDown();
                        }
                    }
                });
            }

            //noinspection ConstantConditions
            if (debugMode) {
                postRequestLatch.await();
                getRequestLatch.await();
            } else {
                assertTrue("Waiting for all POST requests to complete has timed out.",
                        postRequestLatch.await(LATCH_WAIT_TIMEOUT * getAsyncTimeoutMultiplier(), TimeUnit.SECONDS));
                assertTrue("Waiting for all GET requests to complete has timed out.",
                        getRequestLatch.await(LATCH_WAIT_TIMEOUT * getAsyncTimeoutMultiplier(),
                                TimeUnit.SECONDS));
            }
        } finally {
            executor.shutdownNow();
        }

        final StringBuilder messageBuilder = new StringBuilder();
        for (final Map.Entry<Integer, String> postResponseEntry : postResponses.entrySet()) {
            messageBuilder.append("POST response for message ").append(postResponseEntry.getKey()).append(": ")
                    .append(postResponseEntry.getValue()).append('\n');
        }
        messageBuilder.append('\n');
        for (final Map.Entry<Integer, String> getResponseEntry : getResponses.entrySet()) {
            messageBuilder.append("GET response for message ").append(getResponseEntry.getKey()).append(": ")
                    .append(getResponseEntry.getValue()).append('\n');
        }
        LOGGER.info(messageBuilder.toString());

        assertEquals(MAX_MESSAGES, postResponses.size());
        for (final Map.Entry<Integer, String> postResponseEntry : postResponses.entrySet()) {
            assertTrue("Unexpected POST notification response for message " + postResponseEntry.getKey(),
                    postResponseEntry.getValue().startsWith(AsyncServletResource.CANCELED));
        }

        assertEquals(MAX_MESSAGES, getResponses.size());
        final Collection<Integer> getResponseKeys = getResponses.keySet();
        for (int i = 0; i < MAX_MESSAGES; i++) {
            assertTrue("Detected a GET message response loss: " + i, getResponseKeys.contains(i));
            final String getResponseEntry = getResponses.get(i);
            assertTrue("Unexpected canceled GET response status for request " + i, getResponseEntry.startsWith("503: "));
        }
    }
}
