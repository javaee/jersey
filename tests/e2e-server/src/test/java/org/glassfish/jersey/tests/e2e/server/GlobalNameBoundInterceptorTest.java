/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2017 Oracle and/or its affiliates. All rights reserved.
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
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.ws.rs.GET;
import javax.ws.rs.NameBinding;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import javax.annotation.Priority;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests name bound and global bound interceptors.
 *
 * @author Miroslav Fuksa
 *
 */
public class GlobalNameBoundInterceptorTest extends JerseyTest {

    @Override
    protected Application configure() {
        return new ResourceConfig(
                InterceptorTestBound.class,
                InterceptorGlobal.class,
                PreMatchFilter.class,
                TestResource.class,
                TestExceptionMapper.class,
                RequestFilter.class,
                ReaderInterceptorGlobal.class,
                ReaderInterceptorPostBound.class);
    }

    @NameBinding
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(value = RetentionPolicy.RUNTIME)
    public static @interface TestBound {}

    @NameBinding
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(value = RetentionPolicy.RUNTIME)
    public static @interface PostBound {}

    @Path("resource")
    public static class TestResource {
        @GET
        @TestBound
        public String get() {
            return "get";
        }

        @GET
        @Path("standard")
        public String getStandard() {
            return "ok";
        }

        @POST
        @PostBound
        @Path("postBound")
        public String postBound(String str) {
            return str;
        }

        @POST
        @Path("postGlobal")
        public String postGlobal(String str) {
            return str;
        }
    }

    public static class TestException extends RuntimeException {
        public TestException(String message) {
            super(message);
        }
    }

    public static class TestExceptionMapper implements ExceptionMapper<TestException> {

        @Override
        public Response toResponse(TestException exception) {
            return Response.ok("mapped-" + exception.getMessage()).build();
        }
    }


    @PreMatching
    public static class PreMatchFilter implements ContainerRequestFilter {

        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
            if ("exception".equals(requestContext.getHeaderString("pre-filter"))) {
                throw new TestException("(pre-matching-exception)");
            } else if ("abort".equals(requestContext.getHeaderString("pre-filter"))) {
                requestContext.abortWith(Response.ok("(pre-matching-abort)").build());
            }
        }
    }

    public static class RequestFilter implements ContainerRequestFilter {

        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
            if ("exception".equals(requestContext.getHeaderString("request-filter"))) {
                throw new TestException("(request-filter-exception)");
            } else if ("abort".equals(requestContext.getHeaderString("request-filter"))) {
                requestContext.abortWith(Response.ok("(request-filter-abort)").build());
            }
        }
    }

    @TestBound
    @Priority(200)
    public static class InterceptorTestBound implements WriterInterceptor {

        @Override
        public void aroundWriteTo(WriterInterceptorContext context)
                throws IOException, WebApplicationException {
            String entity = context.getEntity() + "-[test-bound]";
            context.setEntity(entity);
            context.proceed(); //Add one
        }
    }

    @Priority(100)
    public static class InterceptorGlobal implements WriterInterceptor {

        @Override
        public void aroundWriteTo(WriterInterceptorContext context)
                throws IOException, WebApplicationException {
            String entity = context.getEntity() + "-[global-bound]";
            context.setEntity(entity);
            context.proceed(); //Add one
        }
    }

    @Priority(200)
    public static class ReaderInterceptorGlobal implements ReaderInterceptor {

        @Override
        public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException, WebApplicationException {
            final String entity = (String) context.proceed();
            return entity + "-[global-reader-interceptor]";
        }
    }

    @Priority(100)
    @PostBound
    public static class ReaderInterceptorPostBound implements ReaderInterceptor {

        @Override
        public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException, WebApplicationException {
            final String entity = (String) context.proceed();
            return entity + "-[post-reader-interceptor]";
        }
    }

    @Test
    public void testPrematchingException() {
        final Response response = target("resource").request().header("pre-filter", "exception").get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("mapped-(pre-matching-exception)-[global-bound]", response.readEntity(String.class));
    }

    @Test
    public void testPrematchingAbort() {
        final Response response = target("resource").request().header("pre-filter", "abort").get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("(pre-matching-abort)-[global-bound]", response.readEntity(String.class));
    }

    @Test
    public void testRequestFilterException() {
        final Response response = target("resource").request().header("request-filter", "exception").get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("mapped-(request-filter-exception)-[global-bound]-[test-bound]", response.readEntity(String.class));
    }

    @Test
    public void testRequestFilterAbort() {
        final Response response = target("resource").request().header("request-filter", "abort").get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("(request-filter-abort)-[global-bound]-[test-bound]", response.readEntity(String.class));
    }

    @Test
    public void testStandardResource() {
        final Response response = target("resource/standard").request().get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("ok-[global-bound]", response.readEntity(String.class));
    }

    @Test
    public void testTestResource() {
        final Response response = target("resource").request().get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("get-[global-bound]-[test-bound]", response.readEntity(String.class));
    }


    @Test
    public void testPost() {
        final Response response = target("resource/postGlobal").request().post(Entity.entity("post", MediaType.TEXT_PLAIN_TYPE));
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("post-[global-reader-interceptor]-[global-bound]", response.readEntity(String.class));
    }

    @Test
    public void testPostBound() {
        final Response response = target("resource/postBound").request().post(Entity.entity("post", MediaType.TEXT_PLAIN_TYPE));
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("post-[global-reader-interceptor]-[post-reader-interceptor]-[global-bound]",
                response.readEntity(String.class));
    }

}
