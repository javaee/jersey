/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.server.model;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.RequestContextBuilder;
import org.glassfish.jersey.server.ResourceConfig;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test of fix for issues JERSEY-1088 and JERSEY-1089.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class AsyncContentAndEntityTypeTest {

    private ApplicationHandler createApplication(Class<?>... classes) {
        return new ApplicationHandler(new ResourceConfig(classes));
    }

    @Path("/")
    public static class AsyncResource {
        static BlockingQueue<AsyncResponse> ctxQueue = new ArrayBlockingQueue<AsyncResponse>(1);

        @Produces("application/foo")
        @GET
        public void getFoo(@Suspended AsyncResponse ar) throws InterruptedException {
            AsyncResource.ctxQueue.put(ar);
        }

        @POST
        @Consumes("application/foo")
        public void postFoo(String foo) throws InterruptedException {
            AsyncResource.ctxQueue.take().resume(foo);
        }
    }

    @Test
    public void testAsyncContentType() throws Exception {
        final ApplicationHandler app = createApplication(AsyncResource.class);

        MediaType foo = MediaType.valueOf("application/foo");

        Future<ContainerResponse> responseFuture =
                Executors.newFixedThreadPool(1).submit(new Callable<ContainerResponse>() {
                    @Override
                    public ContainerResponse call() throws Exception {
                        return app.apply(RequestContextBuilder.from("/", "GET").accept("*/*").build()).get();
                    }
                });


        ContainerResponse response;
        // making sure the JVM optimization does not swap the order of the calls.
        synchronized (this) {
            app.apply(RequestContextBuilder.from("/", "POST").entity("Foo").build());
            response = responseFuture.get();
        }

        assertTrue("Status: " + response.getStatus(), response.getStatus() < 300);
        assertEquals("Foo", response.getEntity());
        assertEquals(foo, response.getMediaType());

        final GenericType stringType = new GenericType(String.class);
        assertEquals(stringType.getRawType(), response.getEntityClass());
        assertEquals(stringType.getType(), response.getEntityType());
    }

}
