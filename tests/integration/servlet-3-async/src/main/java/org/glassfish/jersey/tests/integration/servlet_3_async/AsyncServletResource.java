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
package org.glassfish.jersey.tests.integration.servlet_3_async;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;

/**
 * Asynchronous servlet-deployed resource.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
@Path("async")
public class AsyncServletResource {
    /**
     * Hello world message.
     */
    public static final String HELLO_ASYNC_WORLD = "Hello Async World!";
    public static final String CANCELED = "Canceled";

    private static BlockingQueue<CanceledRequest> cancelingQueue = new ArrayBlockingQueue<CanceledRequest>(5);

    private static class CanceledRequest {
        private final String id;
        private final AsyncResponse asyncResponse;

        private CanceledRequest(String id, AsyncResponse asyncResponse) {
            this.id = id;
            this.asyncResponse = asyncResponse;
        }
    }

    /**
     * Get the async "Hello World" message.
     */
    @GET
    @Produces("text/plain")
    public void get(@Suspended final AsyncResponse ar) {
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(100);
                    ar.resume(HELLO_ASYNC_WORLD);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Get a canceled request.
     *
     * @param id request id.
     * @throws InterruptedException in case of not being able to put the request
     *                              to an internal queue for canceling.
     */
    @GET
    @Path("canceled")
    public void getCanceled(@Suspended final AsyncResponse ar, @QueryParam("id") final String id) throws InterruptedException {
        cancelingQueue.put(new CanceledRequest(id, ar));
    }

    /**
     * Cancel a request that is on top of the canceling queue.
     *
     * @return notification message about successful request canceling.
     * @throws InterruptedException in case of not being able to take a cancelled request
     *                              from an internal canceling queue.
     */
    @POST
    @Produces("text/plain")
    @Path("canceled")
    public String cancel(String requestId) throws InterruptedException {
        final CanceledRequest canceledRequest = cancelingQueue.take();
        canceledRequest.asyncResponse.cancel();

        return CANCELED + " " + canceledRequest.id + " by POST " + requestId;
    }

}
