/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.tests.integration.jersey1107;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.external.ExternalTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for JERSEY-1107: Thread gets stuck if no MessageBodyWriter is found in ApplicationHandler#writeResponse.
 * <p/>
 * If an exception (e.g. NPE caused by non-existent MessageBodyWriter) is thrown in ApplicationHandler#writeResponse before
 * headers and response status are written by ContainerResponseWriter#writeResponseStatusAndHeaders then the
 * ContainerResponseWriter#commit in the finally clause will stuck the thread.
 * <p/>
 * The purpose of the tests below is to show that a response is returned from the server and none of the threads gets stuck.
 *
 * @author Michal Gajdos
 */
public class ApplicationHandlerITCase extends JerseyTest {

    @Override
    protected ResourceConfig configure() {
        return new ResourceConfig().registerInstances(new Jersey1107());
    }

    @Override
    protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
        return new ExternalTestContainerFactory();
    }

    /**
     * Checks if a thread gets stuck when an {@code IOException} is thrown from the {@code
     * MessageBodyWriter#writeTo}.
     */
    @Test
    public void testIOExceptionInWriteResponseMethod() throws Exception {
        _testExceptionInWriteResponseMethod("ioe", "exception/ioexception", Response.Status.INTERNAL_SERVER_ERROR);
    }

    /**
     * Checks if a thread gets stuck when an {@code WebApplicationException} is thrown from the {@code
     * MessageBodyWriter#writeTo}.
     */
    @Test
    public void testWebApplicationExceptionInWriteResponseMethod() throws Exception {
        _testExceptionInWriteResponseMethod("wae", "exception/webapplicationexception", Response.Status.INTERNAL_SERVER_ERROR);
    }

    /**
     * Checks if a thread gets stuck when no {@code MessageBodyWriter} is found and therefore an {@code NPE} is thrown
     * when trying to invoke {@code MessageBodyWriter#writeTo} on an empty object.
     */
    @Test
    public void testNullPointerExceptionInWriteResponseMethod() throws Exception {
        _testExceptionInWriteResponseMethod("npe", "exception/nullpointerexception", Response.Status.INTERNAL_SERVER_ERROR);
    }

    /**
     * Creates a request to the server (with the whole process time set to the maximum of 5 seconds) for the given {@code path}
     * and {@code mediaType} that should result in the {@code expectedResponse}.
     */
    private void _testExceptionInWriteResponseMethod(final String path, final String mediaType,
                                                     final Response.Status expectedResponse) throws Exception {
        // Executor.
        final ExecutorService executor = Executors.newSingleThreadExecutor();

        final Future<Response> responseFuture = executor.submit(new Callable<Response>() {

            @Override
            public Response call() throws Exception {
                return target().path(path).request(mediaType).get();
            }

        });

        executor.shutdown();
        final boolean inTime = executor.awaitTermination(5000, TimeUnit.MILLISECONDS);

        // Asserts.
        assertTrue(inTime);

        // Response.
        final Response response = responseFuture.get();
        assertEquals(expectedResponse.getStatusCode(), response.getStatusInfo().getStatusCode());
    }

}
