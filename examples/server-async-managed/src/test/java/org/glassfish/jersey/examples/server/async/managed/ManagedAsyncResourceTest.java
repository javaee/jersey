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
package org.glassfish.jersey.examples.server.async.managed;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.ws.rs.client.Configuration;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.InvocationException;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.media.json.JsonJacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;

import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * Test for the asynchronous managed resources example.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class ManagedAsyncResourceTest extends JerseyTest {

    private static final Logger LOGGER = Logger.getLogger(ManagedAsyncResourceTest.class.getName());

    @Override
    protected ResourceConfig configure() {
        // mvn test -DargLine="-Djersey.config.test.containerFactory=org.glassfish.jersey.test.inmemory.InMemoryTestContainerFactory"
        // mvn test -DargLine="-Djersey.config.test.containerFactory=org.glassfish.jersey.test.grizzly.GrizzlyTestContainerFactory"

        enable(TestProperties.LOG_TRAFFIC);
        enable(TestProperties.DUMP_ENTITY);
        return App.create();
    }

    @Override
    protected void configureClient(Configuration config) {
        config.register(new JsonJacksonFeature());
    }

    @Test
    @Ignore
    // FIXME fix the async issues and un-ignore
    public void testLongRunningResource() throws InterruptedException {
        final WebTarget resourceTarget = target().path(App.ASYNC_LONG_RUNNING_MANAGED_OP_PATH);
        final String expectedResponse = SimpleJerseyExecutorManagedLongRunningResource.NOTIFICATION_RESPONSE;

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
                            final String response = resourceTarget.queryParam("id", requestId).request().get(String.class);
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
            messageBuilder.append("GET response for message ").append(getResponseEntry.getKey()).append(": ").append(getResponseEntry.getValue()).append('\n');
        }
        LOGGER.info(messageBuilder.toString());

        assertEquals(MAX_MESSAGES, getResponses.size());
        for (Map.Entry<Integer, String> entry : getResponses.entrySet()) {
            assertTrue(
                    "Unexpected GET notification response for message " + entry.getKey(),
                    entry.getValue().contains(expectedResponse));
        }
    }

    @Test
    public void testChatResource() throws InterruptedException {
        final WebTarget resourceTarget = target().path("chat");
        final int MAX_MESSAGES = 1;
        final int LATCH_WAIT_TIMEOUT = 100000;
        final boolean debugMode = false;
        final boolean sequentialGet = false;
        final boolean sequentialPost = false;
        final Object sequentialGetLock = new Object();
        final Object sequentialPostLock = new Object();

        final ExecutorService executor = Executors.newCachedThreadPool(
                new ThreadFactoryBuilder().setNameFormat("async-resource-test-%d").build());

        final Map<Integer, Integer> postResponses = new ConcurrentHashMap<Integer, Integer>();
        final Map<Integer, Message> getResponses = new ConcurrentHashMap<Integer, Message>();

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
                            Response response = resourceTarget.request().post(Entity.json(new Message("" + requestId, "" + requestId)));
                            postResponses.put(requestId, response.getStatus());
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
                            final Message response = resourceTarget.request("application/json").get(Message.class);
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
        for (Map.Entry<Integer, Integer> postResponseEntry : postResponses.entrySet()) {
            messageBuilder.append("POST response for message ").append(postResponseEntry.getKey()).append(": ").append(postResponseEntry.getValue()).append('\n');
        }
        messageBuilder.append('\n');
        for (Map.Entry<Integer, Message> getResponseEntry : getResponses.entrySet()) {
            messageBuilder.append("GET response for message ").append(getResponseEntry.getKey()).append(": ").append(getResponseEntry.getValue()).append('\n');
        }
        LOGGER.info(messageBuilder.toString());

        assertEquals(MAX_MESSAGES, postResponses.size());
        for (Map.Entry<Integer, Integer> postResponseEntry : postResponses.entrySet()) {
            assertEquals(
                    "Unexpected POST notification response for message " + postResponseEntry.getKey(),
                    200, postResponseEntry.getValue().intValue());
        }

        assertEquals(MAX_MESSAGES, getResponses.size());
        final Collection<Message> getResponseValues = getResponses.values();
        for (int i = 0; i < MAX_MESSAGES; i++) {
            assertTrue("Detected a message loss: " + i, getResponseValues.contains(new Message("" + i, "" + i)));
        }
    }
}
