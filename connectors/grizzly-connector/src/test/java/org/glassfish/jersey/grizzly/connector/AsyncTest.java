/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2016 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.grizzly.connector;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.client.Entity;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.container.TimeoutHandler;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.hamcrest.Matchers;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * Asynchronous connector test.
 *
 * @author Arul Dhesiaseelan (aruld at acm.org)
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class AsyncTest extends JerseyTest {

    private static final Logger LOGGER = Logger.getLogger(AsyncTest.class.getName());
    private static final String PATH = "async";

    /**
     * Asynchronous test resource.
     */
    @Path(PATH)
    public static class AsyncResource {

        /**
         * Typical long-running operation duration.
         */
        public static final long OPERATION_DURATION = 1000;

        /**
         * Long-running asynchronous post.
         *
         * @param asyncResponse async response.
         * @param id            post request id (received as request payload).
         */
        @POST
        public void asyncPost(@Suspended final AsyncResponse asyncResponse, final String id) {
            LOGGER.info("Long running post operation called with id " + id + " on thread " + Thread.currentThread().getName());
            new Thread(new Runnable() {

                @Override
                public void run() {
                    final String result = veryExpensiveOperation();
                    asyncResponse.resume(result);
                }

                private String veryExpensiveOperation() {
                    // ... very expensive operation that typically finishes within 1 seconds, simulated using sleep()
                    try {
                        Thread.sleep(OPERATION_DURATION);
                        return "DONE-" + id;
                    } catch (final InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return "INTERRUPTED-" + id;
                    } finally {
                        LOGGER.info("Long running post operation finished on thread " + Thread.currentThread().getName());
                    }
                }
            }, "async-post-runner-" + id).start();
        }

        /**
         * Long-running async get request that times out.
         *
         * @param asyncResponse async response.
         */
        @GET
        @Path("timeout")
        public void asyncGetWithTimeout(@Suspended final AsyncResponse asyncResponse) {
            LOGGER.info("Async long-running get with timeout called on thread " + Thread.currentThread().getName());
            asyncResponse.setTimeoutHandler(new TimeoutHandler() {

                @Override
                public void handleTimeout(final AsyncResponse asyncResponse) {
                    asyncResponse.resume(Response.status(Response.Status.SERVICE_UNAVAILABLE)
                            .entity("Operation time out.").build());
                }
            });
            asyncResponse.setTimeout(1, TimeUnit.SECONDS);

            new Thread(new Runnable() {

                @Override
                public void run() {
                    final String result = veryExpensiveOperation();
                    asyncResponse.resume(result);
                }

                private String veryExpensiveOperation() {
                    // very expensive operation that typically finishes within 1 second but can take up to 5 seconds,
                    // simulated using sleep()
                    try {
                        Thread.sleep(5 * OPERATION_DURATION);
                        return "DONE";
                    } catch (final InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return "INTERRUPTED";
                    } finally {
                        LOGGER.info("Async long-running get with timeout finished on thread " + Thread.currentThread().getName());
                    }
                }
            }).start();
        }

    }

    @Override
    protected Application configure() {
        return new ResourceConfig(AsyncResource.class)
                .register(new LoggingFeature(LOGGER, LoggingFeature.Verbosity.PAYLOAD_ANY));
    }

    @Override
    protected void configureClient(final ClientConfig config) {
        config.register(new LoggingFeature(LOGGER, LoggingFeature.Verbosity.PAYLOAD_ANY));
        config.connectorProvider(new GrizzlyConnectorProvider());
    }

    /**
     * Test asynchronous POST.
     * <p/>
     * Send 3 async POST requests and wait to receive the responses. Check the response content and
     * assert that the operation did not take more than twice as long as a single long operation duration
     * (this ensures async request execution).
     *
     * @throws Exception in case of a test error.
     */
    @Test
    public void testAsyncPost() throws Exception {
        final long tic = System.currentTimeMillis();

        // Submit requests asynchronously.
        final Future<Response> rf1 = target(PATH).request().async().post(Entity.text("1"));
        final Future<Response> rf2 = target(PATH).request().async().post(Entity.text("2"));
        final Future<Response> rf3 = target(PATH).request().async().post(Entity.text("3"));
        // get() waits for the response
        final String r1 = rf1.get().readEntity(String.class);
        final String r2 = rf2.get().readEntity(String.class);
        final String r3 = rf3.get().readEntity(String.class);

        final long toc = System.currentTimeMillis();

        assertEquals("DONE-1", r1);
        assertEquals("DONE-2", r2);
        assertEquals("DONE-3", r3);

        assertThat("Async processing took too long.", toc - tic, Matchers.lessThan(3 * AsyncResource.OPERATION_DURATION));
    }

    /**
     * Test accessing an operation that times out on the server.
     *
     * @throws Exception in case of a test error.
     */
    @Test
    public void testAsyncGetWithTimeout() throws Exception {
        final Future<Response> responseFuture = target(PATH).path("timeout").request().async().get();
        // Request is being processed asynchronously.
        final Response response = responseFuture.get();

        // get() waits for the response
        assertEquals(503, response.getStatus());
        assertEquals("Operation time out.", response.readEntity(String.class));
    }
}
