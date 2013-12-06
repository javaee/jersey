/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.jetty.connector;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.container.TimeoutHandler;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * @author Arul Dhesiaseelan (aruld at acm.org)
 */
public class AsyncTest extends JerseyTest {

    private static final Logger LOGGER = Logger.getLogger(AsyncTest.class.getName());

    private static final String PATH = "async";

    @Path("/async")
    public static class AsyncResource {
        @GET
        public void asyncGet(@Suspended final AsyncResponse asyncResponse) {
            new Thread(new Runnable() {

                @Override
                public void run() {
                    String result = veryExpensiveOperation();
                    asyncResponse.resume(result);
                }

                private String veryExpensiveOperation() {
                    // ... very expensive operation that typically finishes within 5 seconds, simulated using sleep()
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
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
                public void handleTimeout(AsyncResponse asyncResponse) {
                    asyncResponse.resume(Response.status(Response.Status.SERVICE_UNAVAILABLE)
                            .entity("Operation time out.").build());
                }
            });
            asyncResponse.setTimeout(3, TimeUnit.SECONDS);

            new Thread(new Runnable() {

                @Override
                public void run() {
                    String result = veryExpensiveOperation();
                    asyncResponse.resume(result);
                }

                private String veryExpensiveOperation() {
                    // ... very expensive operation that typically finishes within 10 seconds, simulated using sleep()
                    try {
                        Thread.sleep(7000);
                    } catch (InterruptedException e) {
                        // ignore
                    }
                    return "DONE";
                }
            }).start();
        }

    }

    @Override
    protected Application configure() {
        ResourceConfig config = new ResourceConfig(AsyncResource.class);
        config.register(new LoggingFilter(LOGGER, true));
        return config;
    }

    @Override
    protected void configureClient(ClientConfig config) {
        config.connectorProvider(new JettyConnectorProvider());
    }

    @Test
    public void testAsyncGet() throws ExecutionException, InterruptedException {
        final Future<Response> responseFuture = target(PATH).request().async().get();
        // Request is being processed asynchronously.
        final Response response = responseFuture.get();
        // get() waits for the response
        assertEquals("DONE", response.readEntity(String.class));
    }

    @Test
    public void testAsyncGetWithTimeout() throws ExecutionException, InterruptedException, TimeoutException {
        final Future<Response> responseFuture = target(PATH).path("timeout").request().async().get();
        // Request is being processed asynchronously.
        final Response response = responseFuture.get();

        // get() waits for the response
        assertEquals(503, response.getStatus());
        assertEquals("Operation time out.", response.readEntity(String.class));
    }
}
