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
package org.glassfish.jersey.tests.e2e.server;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.logging.Level;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.glassfish.jersey.server.ManagedAsync;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;

import org.junit.Test;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class AsyncResponseTest extends JerseyTest {

    public static CountDownLatch callbackCalledSignal1 = new AsyncCallbackTest.TestLatch(3, "cancel() return value1");
    public static CountDownLatch callbackCalledSignal2 = new AsyncCallbackTest.TestLatch(3, "cancel() return value2");

    @Override
    protected Application configure() {
        set(TestProperties.RECORD_LOG_LEVEL, Level.WARNING.intValue());

        return new ResourceConfig(Resource.class, ErrorResource.class, MappedExceptionMapper.class);
    }

    @Test
    public void testMultipleCancel() throws InterruptedException, IOException {
        final Response response = target().path("resource/1").request().get();
        final InputStream inputStream = response.readEntity(InputStream.class);
        for (int i = 0; i < 500; i++) {
            inputStream.read();
        }
        response.close();
        callbackCalledSignal1.await();
    }

    @Test
    public void testCancelAfterResume() throws InterruptedException, IOException {
        final Response response = target().path("resource/2").request().get();
        final InputStream inputStream = response.readEntity(InputStream.class);
        for (int i = 0; i < 500; i++) {
            inputStream.read();
        }
        response.close();
        callbackCalledSignal2.await();

        assertTrue(getLoggedRecords().isEmpty());
    }

    @Test
    public void testResumeWebApplicationException() throws Exception {
        testResumeException("resumeWebApplicationException", "resumeWebApplicationException", 0);
    }

    @Test
    public void testResumeMappedException() throws Exception {
        testResumeException("resumeMappedException", "resumeMappedException", 0);
    }

    @Test
    public void testResumeRuntimeException() throws Exception {
        testResumeException("resumeRuntimeException", null, 2);
    }

    @Test
    public void testResumeCheckedException() throws Exception {
        testResumeException("resumeCheckedException", null, 2);
    }

    private void testResumeException(final String path, final String entity, final int errorsInLog) throws Exception {
        final WebTarget errorResource = target("errorResource");

        final Future<Response> suspended = errorResource.path("suspend").request().async().get();
        final Response response = errorResource.path(path).request().get();

        assertThat(response.getStatus(), equalTo(200));
        assertThat(response.readEntity(String.class), equalTo("ok"));

        final Response suspendedResponse = suspended.get();

        assertThat(suspendedResponse.getStatus(), equalTo(500));
        if (entity != null) {
            assertThat(suspendedResponse.readEntity(String.class), equalTo(entity));
        }

        suspendedResponse.close();

        assertThat(getLoggedRecords().size(), is(errorsInLog));
    }

    @Path("resource")
    public static class Resource {
        @GET
        @Path("1")
        @ManagedAsync
        public void get1(final @Suspended AsyncResponse asyncResponse) throws IOException, InterruptedException {
            if (asyncResponse.cancel()) {
                callbackCalledSignal1.countDown();
            }
            if (asyncResponse.cancel()) {
                callbackCalledSignal1.countDown();
            }
            if (asyncResponse.cancel()) {
                callbackCalledSignal1.countDown();
            }
        }

        @GET
        @Path("2")
        @ManagedAsync
        public void get2(final @Suspended AsyncResponse asyncResponse) throws IOException, InterruptedException {
            asyncResponse.resume("ok");

            if (!asyncResponse.cancel()) {
                callbackCalledSignal2.countDown();
            }
            if (!asyncResponse.cancel()) {
                callbackCalledSignal2.countDown();
            }
            if (!asyncResponse.cancel()) {
                callbackCalledSignal2.countDown();
            }
        }
    }

    public static class MappedException extends RuntimeException {

        public MappedException(final String message) {
            super(message);
        }
    }

    public static class MappedExceptionMapper implements ExceptionMapper<MappedException> {

        @Override
        public Response toResponse(final MappedException exception) {
            return Response.serverError().entity(exception.getMessage()).build();
        }
    }

    @Path("errorResource")
    public static class ErrorResource {

        private static final BlockingQueue<AsyncResponse> suspended = new ArrayBlockingQueue<AsyncResponse>(1);

        @GET
        @Path("suspend")
        public void suspend(final @Suspended AsyncResponse asyncResponse) {
            suspended.add(asyncResponse);
        }

        @GET
        @Path("resumeWebApplicationException")
        public String resumeWebApplicationException() throws Exception {
            return resume(new WebApplicationException(Response.serverError().entity("resumeWebApplicationException").build()));
        }

        @GET
        @Path("resumeMappedException")
        public String resumeMappedException() throws Exception {
            return resume(new MappedException("resumeMappedException"));
        }

        @GET
        @Path("resumeRuntimeException")
        public String resumeRuntimeException() throws Exception {
            return resume(new RuntimeException("resumeRuntimeException"));
        }

        @GET
        @Path("resumeCheckedException")
        public String resumeCheckedException() throws Exception {
            return resume(new IOException("resumeCheckedException"));
        }

        private String resume(final Throwable throwable) throws Exception {
            return suspended.take().resume(throwable) ? "ok" : "ko";
        }
    }
}
