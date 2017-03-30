/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015-2017 Oracle and/or its affiliates. All rights reserved.
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

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.glassfish.jersey.internal.guava.ThreadFactoryBuilder;
import org.glassfish.jersey.tests.integration.async.AbstractAsyncJerseyTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * JERSEY-2812 reproducer.
 * <p/>
 * This test must not run in parallel.
 *
 * @author Stepan Vavra (stepan.vavra at oracle.com)
 */
public class Jersey2812ITCase extends AbstractAsyncJerseyTest {

    private static final Logger LOGGER = Logger.getLogger(Jersey2812ITCase.class.getName());
    private static long WAIT_TIMEOUT = 5000;

    private AtomicReference<String> asyncResult = new AtomicReference<>();
    private String uuid = UUID.randomUUID().toString();
    private ExecutorService executorService = Executors
            .newSingleThreadExecutor(new ThreadFactoryBuilder().setDaemon(true).build());

    @Before
    public void triggerTheWaitRequestInSeparateThread() throws Exception {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                LOGGER.finer("Running a request to /async/wait in a separate thread.");
                asyncResult.set(target("/asyncTest/async/wait").path(uuid).request().get(String.class));
            }
        });
    }

    /**
     * Tests whether the server-side thread that is processing a http request to the servlet-filter-based Jersey setup ends up
     * stuck or returned back to the pool of available threads.
     * <p/>
     * This test prevents a regression reported in JERSEY-2812.
     * <p/>
     * When the {@link javax.ws.rs.container.AsyncResponse} was left intact in the RESTful resource (as implemented in {@link
     * TestWaitResource#waitForEvent(AsyncResponse, HttpServletRequest, String)}), the server-side Jersey thread ended up in
     * {@link org.glassfish.jersey.servlet.internal.ResponseWriter#getResponseContext()} blocked because of the resolution of http
     * response status from {@link org.glassfish.jersey.servlet.ServletContainer#doFilter(HttpServletRequest, HttpServletResponse,
     * FilterChain, String, String, String)}
     * <p/>
     * This test uses a separate thread to call {@code /async/wait/{uuid}} resource which blocks until the {@code
     * /async/release/{uuid}} is called. In the meantime the JUnit thread calls {@code /async/await/{uuid}} to discover whether
     * the server-side thread processing the request to {@code /async/await/{uuid}/started} did start processing of the request.
     * Consecutively, the JUnit thread calls {@code /async/await/{uuid}/finished} with a timeout {@code #WAIT_TIMEOUT} to discover
     * whether the server-side thread got stuck (which is what JERSEY-2812 reported) or not.
     *
     * @throws Exception
     */
    @Test
    public void asyncSuspendedResourceDoesNotGetStuck() throws Exception {
        // [1] wait for the /async/wait request to be processed
        final Response startResponse = target("/asyncTest/async/await").path(uuid).path("started")
                .queryParam("millis", WAIT_TIMEOUT).request().get();
        assertTrue("The server-side thread handling the request to /async/wait didn't start in timely fashion. "
                        + "This error indicates this test is not executed / designed properly rather than a regression in "
                        + "JERSEY-2812 fix.",
                startResponse.readEntity(Boolean.class));

        // [2] wait for the /async/wait request to finish
        final Response finishResponse = target("/asyncTest/async/await").path(uuid).path("finished")
                .queryParam("millis", WAIT_TIMEOUT).request().get();
        assertTrue("The thread processing the /async/wait request did not respond in timely fashion. "
                        + "Memory leak / thread got stuck detected!",
                finishResponse.readEntity(Boolean.class));

        // [3] release the blocked http call to /async/wait
        final String releaseResponse = target("/asyncTest/async/release").path(uuid).request().post(null, String.class);
        assertEquals("RELEASED", releaseResponse);

        // [4] test whether everything ended as expected
        executorService.shutdown();
        assertTrue("The test thread did not finish in timely fashion!",
                executorService.awaitTermination(WAIT_TIMEOUT, TimeUnit.MILLISECONDS));
        assertEquals("async-OK-" + uuid, asyncResult.get());
    }

    @After
    public void releaseResources() {
        // release the server-side thread regardless of whether left un-attended
        target("/asyncTest/async/release").path(uuid).request().post(null);
    }

    @After
    public void terminateThread() {
        // forcibly terminate the test client thread
        executorService.shutdownNow();
    }

}
