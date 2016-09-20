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

package org.glassfish.jersey.simple;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.container.TimeoutHandler;
import javax.ws.rs.core.Response;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author Arul Dhesiaseelan (aruld at acm.org)
 * @author Michal Gajdos
 */
public class AsyncTest extends AbstractSimpleServerTester {

    @Path("/async")
    @SuppressWarnings("VoidMethodAnnotatedWithGET")
    public static class AsyncResource {

        public static AtomicInteger INVOCATION_COUNT = new AtomicInteger(0);

        @GET
        public void asyncGet(@Suspended final AsyncResponse asyncResponse) {
            new Thread(new Runnable() {

                @Override
                public void run() {
                    final String result = veryExpensiveOperation();
                    asyncResponse.resume(result);
                }

                private String veryExpensiveOperation() {
                    // ... very expensive operation that typically finishes within 5 seconds, simulated using
                    // sleep()
                    try {
                        Thread.sleep(5000);
                    } catch (final InterruptedException e) {
                        // ignore
                    }
                    return "DONE";
                }
            }).start();
        }

        @GET
        @Path("timeout")
        public void asyncGetWithTimeout(@Suspended final AsyncResponse asyncResponse) {
            asyncResponse.setTimeoutHandler(new TimeoutHandler() {

                @Override
                public void handleTimeout(final AsyncResponse asyncResponse) {
                    asyncResponse.resume(Response.status(Response.Status.SERVICE_UNAVAILABLE)
                                                 .entity("Operation time out.").build());
                }
            });
            asyncResponse.setTimeout(3, TimeUnit.SECONDS);

            new Thread(new Runnable() {

                @Override
                public void run() {
                    final String result = veryExpensiveOperation();
                    asyncResponse.resume(result);
                }

                private String veryExpensiveOperation() {
                    // ... very expensive operation that typically finishes within 10 seconds, simulated using
                    // sleep()
                    try {
                        Thread.sleep(7000);
                    } catch (final InterruptedException e) {
                        // ignore
                    }
                    return "DONE";
                }
            }).start();
        }

        @GET
        @Path("multiple-invocations")
        public void asyncMultipleInvocations(@Suspended final AsyncResponse asyncResponse) {
            INVOCATION_COUNT.incrementAndGet();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    asyncResponse.resume("OK");
                }
            }).start();
        }
    }

    private Client client;

    @Before
    public void setUp() throws Exception {
        startServer(AsyncResource.class);
        client = ClientBuilder.newClient();
    }

    @Override
    @After
    public void tearDown() {
        super.tearDown();
        client = null;
    }

    @Test
    public void testAsyncGet() throws ExecutionException, InterruptedException {
        final Future<Response> responseFuture =
                client.target(getUri().path("/async")).request().async().get();
        // Request is being processed asynchronously.
        final Response response = responseFuture.get();
        // get() waits for the response
        assertEquals("DONE", response.readEntity(String.class));
    }

    @Test
    public void testAsyncGetWithTimeout()
            throws ExecutionException, InterruptedException, TimeoutException {
        final Future<Response> responseFuture =
                client.target(getUri().path("/async/timeout")).request().async().get();
        // Request is being processed asynchronously.
        final Response response = responseFuture.get();

        // get() waits for the response
        assertEquals(503, response.getStatus());
        assertEquals("Operation time out.", response.readEntity(String.class));
    }

    /**
     * JERSEY-2616 reproducer. Make sure resource method is only invoked once per one request.
     */
    @Test
    public void testAsyncMultipleInvocations() throws Exception {
        final Response response =
                client.target(getUri().path("/async/multiple-invocations")).request().get();

        assertThat(AsyncResource.INVOCATION_COUNT.get(), is(1));

        assertThat(response.getStatus(), is(200));
        assertThat(response.readEntity(String.class), is("OK"));
    }
}
