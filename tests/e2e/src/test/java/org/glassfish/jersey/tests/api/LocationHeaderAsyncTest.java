/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.tests.api;

import java.net.URI;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.glassfish.jersey.server.ManagedAsync;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.glassfish.jersey.test.util.runner.ConcurrentRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test if the location relativer URI is correctly resolved within asynchronous processing cases.
 *
 * @author Adam Lindenthal (adam.lindenthal at oracle.com)
 */
@RunWith(ConcurrentRunner.class)
public class LocationHeaderAsyncTest extends JerseyTest {

    private static final Logger LOGGER = Logger.getLogger(LocationHeaderAsyncTest.class.getName());
    static ExecutorService executor;

    private static final AtomicBoolean executorComparisonFailed = new AtomicBoolean(false);
    private static final AtomicBoolean interrupted = new AtomicBoolean(false);

    @Override
    protected ResourceConfig configure() {
        enable(TestProperties.LOG_TRAFFIC);
        return new ResourceConfig(ResponseTest.class);
    }

    /**
     * Prepare test infrastructure.
     *
     * In this case it prepares executor thread pool of size one and initializes the thread.
     * @throws Exception
     */
    @Override
    public void setUp() throws Exception {
        super.setUp();
        /* thread pool for custom executor async test */
        LocationHeaderAsyncTest.executor = Executors.newFixedThreadPool(1);

        // Force the thread to be eagerly instantiated - this prevents the instantiation later and ensures, that the thread
        // will not be a child thread of the request handling thread, so the thread-local baseUri variable will not be inherited.
        LocationHeaderAsyncTest.executor.submit(new Runnable() {
            @Override
            public void run() {
                LOGGER.info("Thread pool initialized.");
            }
        });
    }

    /**
     * Test JAX-RS resource
     */
    @SuppressWarnings("VoidMethodAnnotatedWithGET")
    @Path(value = "/ResponseTest")
    public static class ResponseTest {

        /* injected request URI for assertions in the resource methods */
        @Context
        private UriInfo uriInfo;

        /**
         * Asynchronous resource method for testing if the URI is absolutized also in case of asynchronous processing;
         *
         * The response is created in the separate thread. This tests, that the thread still has access to the request baseUri
         * thread-local variable in {@link org.glassfish.jersey.message.internal.OutboundJaxrsResponse.Builder}.
         */
        @GET
        @Path("locationAsync")
        public void locationAsync(@Suspended final AsyncResponse asyncResponse) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    final URI uri = getUriBuilder().segment("locationAsync").build();
                    final Response result = Response.created(uri).build();

                    final URI location = result.getLocation();
                    if (uriInfo.getAbsolutePath().equals(location)) {
                        asyncResponse.resume(result);
                    } else {
                        asyncResponse.resume(Response.serverError().entity(location.toString()).build());
                    }

                }
            }).start();
        }

        /**
         * Resource method for async test with custom executor.
         *
         * It runs in a thread that was not created within the request scope, so it does not inherit the baseUri thread-local
         * variable value.
         * In this case, URI will not be absolutized until calling {@link AsyncResponse#resume(Object)}.
         */
        @GET
        @Path("executorAsync")
        @ManagedAsync
        public void executorAsync(@Suspended final AsyncResponse asyncResponse) {
            LocationHeaderAsyncTest.executor.submit(new Runnable() {
                @Override
                public void run() {
                    final URI uri = getUriBuilder().segment("executorAsync").build();
                    final Response result = Response.created(uri).build();
                    asyncResponse.resume(result);
                    if (!uriInfo.getAbsolutePath().equals(result.getLocation())) {
                        executorComparisonFailed.set(true);
                    }
                }
            });
        }

        /**
         * Placeholder for the suspended async responses;
         * For the current test a simple static field would be enough, but this is easily extensible;
         *
         * This is inspired by the {@link AsyncResponse} javadoc example
         */
        private static final BlockingQueue<AsyncResponse> suspended = new ArrayBlockingQueue<>(5);

        /**
         * Start of the async test - stores the asynchronous response object
         */
        @GET
        @Path("locationAsyncStart")
        public void locationAsyncStart(@Suspended final AsyncResponse asyncResponse) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        suspended.put(asyncResponse);
                    } catch (final InterruptedException e) {
                        asyncResponse.cancel();
                        Thread.currentThread().interrupt();
                        interrupted.set(true);
                    }
                }
            }).start();
        }

        /**
         * Finish of the async test - creates a response, checks the location header and resumes the asyncResponse
         * @return true if the URI was correctly absolutized, false if the URI is relative or differs from the expected URI
         */
        @GET
        @Path("locationAsyncFinish")
        public Boolean locationAsyncFinish() throws InterruptedException {
            final AsyncResponse asyncResponse = suspended.poll(2000, TimeUnit.MILLISECONDS);

            final URI uri = getUriBuilder().segment("locationAsyncFinish").build();
            final Response result = Response.created(uri).build();
            final boolean wasEqual = result.getLocation().equals(uriInfo.getAbsolutePath());

            asyncResponse.resume(result);
            return wasEqual;
        }

        /** Return UriBuilder with base pre-set {@code /ResponseTest} uri segment for this resource.
         *
         * @return UriBuilder
         */
        private UriBuilder getUriBuilder() {
            return UriBuilder.fromResource(ResponseTest.class);
        }
    }

    /**
     * Basic asynchronous testcase; checks if the URI is correctly absolutized also within a separate thread during
     * async processing
     */
    @Test
    public void testAsync() {
        final String expectedUri = getBaseUri() + "ResponseTest/locationAsync";
        final Response response = target().path("ResponseTest/locationAsync").request().get(Response.class);

        final String msg = String.format("Comparison failed in the resource method. \nExpected: %1$s\nActual: %2$s",
                expectedUri, response.readEntity(String.class));
        assertNotEquals(msg, response.getStatus(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());

        final String location = response.getHeaderString(HttpHeaders.LOCATION);
        LOGGER.info("Location resolved from response > " + location);
        assertEquals(expectedUri, location);
    }

    /**
     * Test with a thread from thread-pool (created out of request scope)
     */
    @Test
    public void testExecutorAsync() {
        final Response response = target().path("ResponseTest/executorAsync").request().get(Response.class);
        final String location = response.getHeaderString(HttpHeaders.LOCATION);
        LOGGER.info("Location resolved from response > " + location);
        assertFalse("The comparison failed in the resource method.", executorComparisonFailed.get());
        assertEquals(getBaseUri() + "ResponseTest/executorAsync", location);
    }

    /**
     * Asynchronous testcase split over two distinct requests
     */
    @Test
    public void testSeparatedAsync() throws ExecutionException, InterruptedException {
        final Future<Response> futureResponse = target().path("ResponseTest/locationAsyncStart").request().async().get();
        final Boolean result = target().path("ResponseTest/locationAsyncFinish").request().get(Boolean.class);
        assertFalse("Thread was interrupted on inserting into blocking queue.", interrupted.get());
        assertTrue(result);

        final Response response = futureResponse.get();
        final String location = response.getHeaderString(HttpHeaders.LOCATION);
        assertEquals(getBaseUri() + "ResponseTest/locationAsyncFinish", location);
    }
}



