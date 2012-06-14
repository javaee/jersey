/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.examples.server.async;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.InvocationException;
import javax.ws.rs.client.WebTarget;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

//@Ignore
public class AsyncResourceTest extends JerseyTest {
    private static final Logger LOGGER = Logger.getLogger(AsyncResourceTest.class.getName());

    @Override
    protected ResourceConfig configure() {
        // mvn test -DargLine="-Djersey.config.test.containerFactory=org.glassfish.jersey.test.inmemory.InMemoryTestContainerFactory"
        // mvn test -DargLine="-Djersey.config.test.containerFactory=org.glassfish.jersey.test.grizzly.GrizzlyTestContainerFactory"
        enable(TestProperties.LOG_TRAFFIC);
        enable(TestProperties.DUMP_ENTITY);
        return App.create();
    }

    @Test
    public void testFireAndForgetChatResource() throws InterruptedException {
        executeChatTest(target().path(App.ASYNC_MESSAGING_FIRE_N_FORGET_PATH), FireAndForgetChatResource.POST_NOTIFICATION_RESPONSE);
    }

    @Test
    public void testBlockingPostChatResource() throws InterruptedException {
        executeChatTest(target().path(App.ASYNC_MESSAGING_BLOCKING_PATH), BlockingPostChatResource.POST_NOTIFICATION_RESPONSE);
    }

    private void executeChatTest(final WebTarget resourceTarget, final String expectedPostResponse) throws InterruptedException {
        final int MAX_MESSAGES = 10;
        final int LATCH_WAIT_TIMEOUT = 10;
        final boolean debugMode = false;
        final boolean sequentialGet = false;
        final boolean sequentialPost = false;
        final Object sequentialGetLock = new Object();
        final Object sequentialPostLock = new Object();

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
                        if (debugMode || sequentialPost) {
                            synchronized (sequentialPostLock) {
                                post();
                            }
                        } else {
                            post();
                        }
                    }

                    private void post() throws InvocationException {
                        try {
                            final String response = resourceTarget.request().post(Entity.text("" + requestId), String.class);
                            postResponses.put(requestId, response);
                        } finally {
                            postRequestLatch.countDown();
                        }
                    }
                });
                executor.submit(new Runnable() {

                    @Override
                    public void run() {
                        if (debugMode || sequentialGet) {
                            synchronized (sequentialGetLock) {
                                get();
                            }
                        } else {
                            get();
                        }
                    }

                    private void get() throws InvocationException {
                        try {
                            final String response = resourceTarget.queryParam("id", requestId).request().get(String.class);
                            getResponses.put(requestId, response);
                        } finally {
                            getRequestLatch.countDown();
                        }
                    }
                });
            }

            if (debugMode) {
                postRequestLatch.await();
                getRequestLatch.await();
            } else {
                assertTrue("Waiting for all POST requests to complete has timed out.", postRequestLatch.await(LATCH_WAIT_TIMEOUT, TimeUnit.SECONDS));
                assertTrue("Waiting for all GET requests to complete has timed out.", getRequestLatch.await(LATCH_WAIT_TIMEOUT, TimeUnit.SECONDS));
            }
        } finally {
            executor.shutdownNow();
        }

        StringBuilder messageBuilder = new StringBuilder();
        for (Map.Entry<Integer, String> postResponseEntry : postResponses.entrySet()) {
            messageBuilder.append("POST response for message ")
                    .append(postResponseEntry.getKey()).append(": ")
                    .append(postResponseEntry.getValue()).append('\n');
        }
        messageBuilder.append('\n');
        for (Map.Entry<Integer, String> getResponseEntry : getResponses.entrySet()) {
            messageBuilder.append("GET response for message ")
                    .append(getResponseEntry.getKey()).append(": ")
                    .append(getResponseEntry.getValue()).append('\n');
        }
        LOGGER.info(messageBuilder.toString());

        assertEquals(MAX_MESSAGES, postResponses.size());
        for (Map.Entry<Integer, String> postResponseEntry : postResponses.entrySet()) {
            assertEquals(
                    "Unexpected POST notification response for message " + postResponseEntry.getKey(),
                    expectedPostResponse, postResponseEntry.getValue());
        }

        assertEquals(MAX_MESSAGES, getResponses.size());
        final Collection<String> getResponseValues = getResponses.values();
        for (int i = 0; i < MAX_MESSAGES; i++) {
            assertTrue("Detected a message loss: " + i, getResponseValues.contains("" + i));
        }
    }

    @Test
    public void testLongRunningResource() throws InterruptedException {
        final WebTarget resourceTarget = target().path(App.ASYNC_LONG_RUNNING_OP_PATH);
        final String expectedResponse = SimpleLongRunningResource.NOTIFICATION_RESPONSE;

        final int MAX_MESSAGES = 50;
        final int LATCH_WAIT_TIMEOUT = 10;
        final boolean debugMode = false;
        final boolean sequentialGet = false;
        final Object sequentialGetLock = new Object();

        final ExecutorService executor = Executors.newCachedThreadPool(
                new ThreadFactoryBuilder().setNameFormat("async-resource-test-%d").build());

        final Map<Integer, String> getResponses = new ConcurrentHashMap<Integer, String>();

        final CountDownLatch getRequestLatch = new CountDownLatch(MAX_MESSAGES);

        try {
            for (int i = 0; i < MAX_MESSAGES; i++) {
                final int requestId = i;
                executor.submit(new Runnable() {

                    @Override
                    public void run() {
                        if (debugMode || sequentialGet) {
                            synchronized (sequentialGetLock) {
                                get();
                            }
                        } else {
                            get();
                        }
                    }

                    private void get() throws InvocationException {
                        try {
                            final String response = resourceTarget.request().get(String.class);
                            getResponses.put(requestId, response);
                        } finally {
                            getRequestLatch.countDown();
                        }
                    }
                });
            }

            if (debugMode) {
                getRequestLatch.await();
            } else {
                assertTrue("Waiting for all GET requests to complete has timed out.", getRequestLatch.await(LATCH_WAIT_TIMEOUT, TimeUnit.SECONDS));
            }
        } finally {
            executor.shutdownNow();
        }

        StringBuilder messageBuilder = new StringBuilder();
        for (Map.Entry<Integer, String> getResponseEntry : getResponses.entrySet()) {
            messageBuilder.append("GET response for message ")
                    .append(getResponseEntry.getKey()).append(": ")
                    .append(getResponseEntry.getValue()).append('\n');
        }
        LOGGER.info(messageBuilder.toString());

        assertEquals(MAX_MESSAGES, getResponses.size());
        for (Map.Entry<Integer, String> entry : getResponses.entrySet()) {
            assertEquals(
                    "Unexpected GET notification response for message " + entry.getKey(),
                    expectedResponse, entry.getValue());
        }
    }

}
