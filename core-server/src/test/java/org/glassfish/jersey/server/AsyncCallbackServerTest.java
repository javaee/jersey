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

package org.glassfish.jersey.server;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.ws.rs.GET;
import javax.ws.rs.NameBinding;
import javax.ws.rs.Path;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.CompletionCallback;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.ResumeCallback;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Response;

import javax.inject.Singleton;

import org.junit.Test;

import junit.framework.Assert;

/**
 * Tests {@link ResumeCallback} and {@link CompletionCallback}.
 *
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 *
 */
public class AsyncCallbackServerTest {
    public static boolean onResumeCalled;
    public static boolean onCompletionCalled;
    public static boolean onCompletionFailedCalled;
    public static boolean onResumeFailedCalled;

    public static void reset() {
        onResumeCalled = false;
        onCompletionCalled = false;
        onCompletionFailedCalled = false;
        onResumeFailedCalled = false;
    }

    @Test
    public void testResumeCallbackOneMethod() throws ExecutionException, InterruptedException {
        reset();
        ApplicationHandler app = new ApplicationHandler(new ResourceConfig(Resource.class,
                CheckingResumeFilter.class));
        ContainerRequest req = RequestContextBuilder.from(
                "/resource/oneMethod", "GET").build();

        final ContainerResponse response = app.apply(req).get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertTrue("MyResumeCallback.onResume was not called.", onResumeCalled);

    }

    @Test
    public void testResumeCallbackTwoMethods() throws ExecutionException, InterruptedException {
        reset();
        ApplicationHandler app = new ApplicationHandler(new ResourceConfig(Resource.class,
                CheckingResumeFilter.class));

        final Future<ContainerResponse> future = app.apply(RequestContextBuilder.from(
                "/resource/suspendResponse", "GET").build());
        Assert.assertFalse(future.isDone());
        Assert.assertFalse(onResumeCalled);
        ContainerResponse response = app.apply(RequestContextBuilder.from(
                "/resource/resumeSuspendedResponse", "GET").build()).get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertTrue("MyResumeCallback.onResume was not called.", onResumeCalled);
    }

    @Test
    public void testResumeFail() throws ExecutionException, InterruptedException {
        reset();
        ApplicationHandler app = new ApplicationHandler(new ResourceConfig(Resource.class,
                CheckingResumeFailFilter.class));

        try {
            ContainerResponse response = app.apply(RequestContextBuilder.from(
                    "/resource/resumeFail", "GET").build()).get();
            Assert.fail("should fail");
        } catch (Exception e) {
            // ok - should throw an exception
        }
        Assert.assertTrue("MyResumeCallback.onResume was not called.", onResumeFailedCalled);
    }


    public static class MyResumeCallback implements ResumeCallback {
        @Override
        public void onResume(AsyncResponse resuming, Response response) {
            Assert.assertFalse(onResumeCalled);
            onResumeCalled = true;
        }

        @Override
        public void onResume(AsyncResponse resuming, Throwable error) {
            Assert.assertFalse(onResumeFailedCalled);
            onResumeFailedCalled = true;
        }
    }


    @ResumeBinding
    public static class CheckingResumeFilter implements ContainerResponseFilter {
        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
            Assert.assertTrue("onResume(AsyncResponse resuming, Response response) callback has not been called.",
                    onResumeCalled);
        }
    }

    @ResumeFailBinding
    public static class CheckingResumeFailFilter implements ContainerResponseFilter {
        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
            Assert.assertTrue("onResume(AsyncResponse resuming, Throwable error) callback has not been called.",
                    onResumeFailedCalled);
        }
    }


    @Path("resource")
    @Singleton
    public static class Resource {
        private AsyncResponse storedAsyncResponse;


        @ResumeBinding
        @GET
        @Path("oneMethod")
        public void get(@Suspended AsyncResponse asyncResponse) {
            Assert.assertFalse("onResume(AsyncResponse resuming, Response response) callback has already been called.",
                    onResumeCalled);
            asyncResponse.register(MyResumeCallback.class);
            asyncResponse.resume(Response.ok().entity("resumed").build());
            Assert.assertTrue("onResume(AsyncResponse resuming, Response response) callback has not been called.",
                    onResumeCalled);
        }

        @ResumeBinding
        @GET
        @Path("suspendResponse")
        public void suspendResponse(@Suspended AsyncResponse asyncResponse) throws InterruptedException {

            storedAsyncResponse = asyncResponse;
            Assert.assertFalse("onResume(AsyncResponse resuming, Response response) callback has already been called.",
                    onResumeCalled);
            asyncResponse.register(MyResumeCallback.class);
        }

        @ResumeBinding
        @GET
        @Path("resumeSuspendedResponse")
        public String resumeSuspendedResponse() {
            Assert.assertFalse("onResume(AsyncResponse resuming, Response response) callback has already been called.",
                    onResumeCalled);

            storedAsyncResponse.resume(Response.ok().entity("resumed").build());
            Assert.assertTrue("onResume(AsyncResponse resuming, Response response) callback has not been called.",
                    onResumeCalled);
            return "not-from-async";
        }

        @ResumeFailBinding
        @GET
        @Path("resumeFail")
        public void resumeFails(@Suspended AsyncResponse asyncResponse) {
            Assert.assertFalse("onResume(AsyncResponse resuming, Throwable error) callback has already been called.",
                    onResumeFailedCalled);
            asyncResponse.register(MyResumeCallback.class);
            asyncResponse.resume(new RuntimeException("test-exception"));
            Assert.assertTrue("onResume(AsyncResponse resuming, Throwable error) callback has not been called.",
                    onResumeFailedCalled);
        }
    }

    @Test
    public void testCompletionCallback() throws ExecutionException, InterruptedException {
        ApplicationHandler app = new ApplicationHandler(new ResourceConfig(CompletionResource.class,
                CheckingCompletionFilter.class));
        ContainerRequest req = RequestContextBuilder.from(
                "/completion/onCompletion", "GET").build();

        final ContainerResponse response = app.apply(req).get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertTrue("onComplete() was not called.", onCompletionCalled);

    }

    @Test
    public void testCompletionFail() throws ExecutionException, InterruptedException {
        ApplicationHandler app = new ApplicationHandler(new ResourceConfig(CompletionResource.class,
                CheckingCompletionFilter.class));

        try {
            final ContainerResponse response = app.apply(RequestContextBuilder.from(
                    "/completion/onError", "GET").build()).get();
            Assert.fail("should fail");
        } catch (Exception e) {
            // ok - should throw an exception
        }
        Assert.assertTrue("onError().", onCompletionFailedCalled);
    }

    @CompletionBinding
    public static class CheckingCompletionFilter implements ContainerResponseFilter {

        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
            Assert.assertFalse("onComplete() callback has already been called.",
                    onCompletionCalled);
        }
    }


    public static class MyCompletionCallback implements CompletionCallback {
        @Override
        public void onComplete() {
            Assert.assertFalse("onComplete() has already been called.", onCompletionCalled);
            onCompletionCalled = true;
        }

        @Override
        public void onError(Throwable throwable) {
            Assert.assertFalse("onError() has already been called.", onCompletionFailedCalled);
            onCompletionFailedCalled = true;
        }
    }


    @Path("completion")
    @Singleton
    public static class CompletionResource {
        @GET
        @Path("onCompletion")
        @CompletionBinding
        public void onComplete(@Suspended AsyncResponse asyncResponse) {
            Assert.assertFalse(onCompletionCalled);
            asyncResponse.register(MyCompletionCallback.class);
            asyncResponse.resume("ok");
            Assert.assertTrue(onCompletionCalled);
        }

        @GET
        @Path("onError")
        @CompletionBinding
        public void onError(@Suspended AsyncResponse asyncResponse) {
            Assert.assertFalse(onCompletionFailedCalled);
            asyncResponse.register(MyCompletionCallback.class);
            asyncResponse.resume(new RuntimeException("test-exception"));
            Assert.assertTrue(onCompletionFailedCalled);
        }
    }


    @NameBinding
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(value = RetentionPolicy.RUNTIME)
    public @interface CompletionBinding {
    }

    @NameBinding
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(value = RetentionPolicy.RUNTIME)
    public @interface ResumeBinding {
    }

    @NameBinding
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(value = RetentionPolicy.RUNTIME)
    public @interface ResumeFailBinding {
    }

}
