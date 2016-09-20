/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2016 Oracle and/or its affiliates. All rights reserved.
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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.glassfish.jersey.server.ResourceConfig;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Tests the parallel execution of multiple requests.
 *
 * @author Stepan Kopriva
 * @author Arul Dhesiaseelan (aruld at acm.org)
 */
public class ParallelTest extends AbstractSimpleServerTester {

    // Server-side dispatcher and selector pool configuration
    private static final int selectorThreads = Runtime.getRuntime().availableProcessors();
    private static final int dispatcherThreads = Math.max(8, selectorThreads * 2);

    private static final int numberOfThreads = 100;

    private static final String PATH = "test";
    private static AtomicInteger receivedCounter = new AtomicInteger(0);
    private static AtomicInteger resourceCounter = new AtomicInteger(0);
    private static CountDownLatch latch = new CountDownLatch(numberOfThreads);

    @Path(PATH)
    public static class MyResource {

        @GET
        public String get() {
            this.sleep();
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

    private class ResourceThread extends Thread {

        private WebTarget target;
        private String path;

        public ResourceThread(WebTarget target, String path) {
            this.target = target;
            this.path = path;
        }

        @Override
        public void run() {
            assertEquals("GET", target.path(path).request().get(String.class));
            receivedCounter.addAndGet(1);
            latch.countDown();
        }
    }

    @Test
    public void testParallel() {
        ResourceConfig config = new ResourceConfig(MyResource.class);
        startServer(config, dispatcherThreads, selectorThreads);
        WebTarget target = ClientBuilder.newClient().target(getUri().path("/").build());

        for (int i = 1; i <= numberOfThreads; i++) {
            ResourceThread rt = new ResourceThread(target, PATH);
            rt.start();
        }

        try {
            latch.await();
        } catch (InterruptedException ex) {
            Logger.getLogger(ParallelTest.class.getName()).log(Level.SEVERE, null, ex);
        }

        int result = receivedCounter.get();
        assertEquals(numberOfThreads, result);
    }
}
