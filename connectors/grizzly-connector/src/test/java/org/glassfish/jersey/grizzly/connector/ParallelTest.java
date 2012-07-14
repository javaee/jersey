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
package org.glassfish.jersey.grizzly.connector;

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

import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Tests the parallel execution of multiple requests.
 *
 * @author Stepan Kopriva (stepan.kopriva at oracle.com)
 */
public class ParallelTest extends JerseyTest {

    private static final int numberOfThreads = 5;
    private static final String PATH = "test";
    private static AtomicInteger receivedCounter = new AtomicInteger(0);
    private static AtomicInteger resourceCounter = new AtomicInteger(0);

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

    private static enum Method {

        GET, PUT, POST
    }

    private class ResourceThread extends Thread {

        private WebTarget target;
        private String path;
        private Method method;

        public ResourceThread(WebTarget target, String path, Method method) {
            this.target = target;
            this.path = path;
            this.method = method;
        }

        @Override
        public void run() {
            Response response;
            response = target.path(path).request().get();
            assertEquals("GET", response.getEntity().toString());
            receivedCounter.addAndGet(1);
        }
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(ParallelTest.MyResource.class);
    }

    @Override
    protected void configureClient(ClientConfig clientConfig) {
        clientConfig.connector(new GrizzlyConnector(clientConfig));
    }

    @Ignore
    @Test
    public void testParallel() {
        for (int i = 1; i <= numberOfThreads; i++) {
            ResourceThread rt = new ResourceThread(target(), PATH, Method.GET);
            rt.start();
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            Logger.getLogger(ParallelTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        int result = receivedCounter.get();
        assertEquals(numberOfThreads, result);
    }
}
