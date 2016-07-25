/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.netty.connector;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Assert;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests the parallel execution of multiple requests.
 *
 * @author Stepan Kopriva
 */
public class ParallelTest extends JerseyTest {
    private static final Logger LOGGER = Logger.getLogger(ParallelTest.class.getName());

    private static final int PARALLEL_CLIENTS = 10;
    private static final String PATH = "test";
    private static final AtomicInteger receivedCounter = new AtomicInteger(0);
    private static final AtomicInteger resourceCounter = new AtomicInteger(0);
    private static final CyclicBarrier startBarrier = new CyclicBarrier(PARALLEL_CLIENTS + 1);
    private static final CountDownLatch doneLatch = new CountDownLatch(PARALLEL_CLIENTS);

    @Path(PATH)
    public static class MyResource {

        @GET
        public String get() {
            sleep();
            resourceCounter.addAndGet(1);
            return "GET";
        }

        private void sleep() {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
                Logger.getLogger(ParallelTest.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(ParallelTest.MyResource.class);
    }

    @Override
    protected void configureClient(ClientConfig config) {
        config.connectorProvider(new NettyConnectorProvider());
    }

    @Test
    public void testParallel() throws BrokenBarrierException, InterruptedException, TimeoutException {
        final ScheduledExecutorService executor = Executors.newScheduledThreadPool(PARALLEL_CLIENTS);

        try {
            final WebTarget target = target();
            for (int i = 1; i <= PARALLEL_CLIENTS; i++) {
                final int id = i;
                executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            startBarrier.await();
                            Response response;
                            response = target.path(PATH).request().get();
                            assertEquals("GET", response.readEntity(String.class));
                            receivedCounter.incrementAndGet();
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                            LOGGER.log(Level.WARNING, "Client thread " + id + " interrupted.", ex);
                        } catch (BrokenBarrierException ex) {
                            LOGGER.log(Level.INFO, "Client thread " + id + " failed on broken barrier.", ex);
                        } catch (Throwable t) {
                            t.printStackTrace();
                            LOGGER.log(Level.WARNING, "Client thread " + id + " failed on unexpected exception.", t);
                        } finally {
                            doneLatch.countDown();
                        }
                    }
                });
            }

            startBarrier.await(1, TimeUnit.SECONDS);

            assertTrue("Waiting for clients to finish has timed out.", doneLatch.await(5 * getAsyncTimeoutMultiplier(),
                                                                                       TimeUnit.SECONDS));

            assertEquals("Resource counter", PARALLEL_CLIENTS, resourceCounter.get());

            assertEquals("Received counter", PARALLEL_CLIENTS, receivedCounter.get());
        } finally {
            executor.shutdownNow();
            Assert.assertTrue("Executor termination", executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }
}
