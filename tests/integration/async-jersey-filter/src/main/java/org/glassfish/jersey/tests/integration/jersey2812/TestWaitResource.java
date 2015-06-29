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
package org.glassfish.jersey.tests.integration.jersey2812;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;

import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;

/**
 * This resource provides a way to reproduce JERSEY-2818.
 *
 * @author Stepan Vavra (stepan.vavra at oracle.com)
 */
@Path("/async")
@Produces("text/plain")
@Singleton
public class TestWaitResource {

    private static final Logger LOGGER = Logger.getLogger(TestWaitResource.class.getName());

    /**
     * Test context identified by UUID chosen by client.
     */
    private final ConcurrentMap<String, TestContext> testContextMap = new ConcurrentHashMap<>();

    private TestContext testContextForUUID(String uuid) {
        testContextMap.putIfAbsent(uuid, new TestContext());
        return testContextMap.get(uuid);
    }

    @GET
    @Path("wait/{uuid}")
    public void waitForEvent(@Suspended AsyncResponse asyncResponse,
                             @Context HttpServletRequest request,
                             @PathParam("uuid") String uuid) {

        LOGGER.finer("Adding response: " + asyncResponse);

        final TestContext testContext = testContextForUUID(uuid);
        final CountDownLatch finishedCdl = (CountDownLatch) request.getAttribute(TestFilter.CDL_FINISHED);

        if (finishedCdl == null) {
            throw new IllegalStateException("The " + TestFilter.class + " was not properly processed before this request!");
        }

        testContext.asyncResponse = asyncResponse;
        testContext.finishedCdl = finishedCdl;
        testContext.startedCdl.countDown();

        LOGGER.finer("Decreasing started cdl: " + testContext.startedCdl);
    }

    @POST
    @Path("release/{uuid}")
    public String releaseLastSuspendedAsyncRequest(@PathParam("uuid") String uuid) {

        LOGGER.finer("Releasing async response");

        if (!testContextMap.containsKey(uuid)) {
            throw new NotAcceptableException("UUID not found!" + uuid);
        }

        // clean it up
        final TestContext releasedTestContext = testContextMap.remove(uuid);
        releasedTestContext.finishedCdl.countDown();
        releasedTestContext.startedCdl.countDown();
        releasedTestContext.asyncResponse.resume("async-OK-" + uuid);

        return "RELEASED";
    }

    @GET
    @Path("await/{uuid}/started")
    public boolean awaitForTheAsyncRequestThreadToStart(@PathParam("uuid") String uuid, @QueryParam("millis") Long millis) {
        final CountDownLatch startedCdl = testContextForUUID(uuid).startedCdl;
        try {
            LOGGER.finer("Checking started cdl: " + startedCdl);
            return startedCdl.await(millis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new IllegalStateException("Interrupted while waiting for the thread to finish!", e);
        }
    }

    @GET
    @Path("await/{uuid}/finished")
    public boolean awaitForTheAsyncRequestThreadToFinish(@PathParam("uuid") String uuid, @QueryParam("millis") Long millis) {
        if (!testContextMap.containsKey(uuid)) {
            throw new NotAcceptableException("UUID not found!" + uuid);
        }
        try {
            LOGGER.finer("Decreasing finished cdl: " + testContextMap.get(uuid).finishedCdl);
            return testContextMap.get(uuid).finishedCdl.await(millis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new IllegalStateException("Interrupted while waiting for the thread to finish!", e);
        }
    }

    /**
     * Test context holder class.
     * <p/>
     * Holds the information for one test identified by UUID chosen by client.
     *
     * @see #testContextMap
     */
    private static class TestContext {

        /**
         * This CDL ensures the server-side thread processing the request to /async/wait/{uuid} has started handling the request.
         */
        final CountDownLatch startedCdl = new CountDownLatch(1);

        /**
         * This CDL ensures the server-side thread processing the request to /async/wait/{uuid} was returned to the thread-pool.
         */
        volatile CountDownLatch finishedCdl;

        /**
         * The async response that does get resumed outside of the request to /async/wait/{uuid}. This reproduces the JERSEY-2812
         * bug.
         */
        volatile AsyncResponse asyncResponse;
    }

}
