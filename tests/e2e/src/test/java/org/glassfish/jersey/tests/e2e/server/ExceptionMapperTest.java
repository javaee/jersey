/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Type;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NameBinding;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.Providers;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * Tests throwing exceptions in {@link MessageBodyReader} and {@link MessageBodyWriter}.
 *
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 */
public class ExceptionMapperTest extends JerseyTest {
    @Override
    protected Application configure() {
        return new ResourceConfig(
                Resource.class,
                MyMessageBodyWritter.class,
                MyMessageBodyReader.class,
                MyExceptionMapper.class,
                MyExceptionMapperCauseAnotherException.class,
                // JERSEY-1515
                TestResource.class,
                VisibilityExceptionMapper.class,
                // JERSEY-1525
                ExceptionTestResource.class,
                ExceptionThrowingFilter.class,
                ThrowableMapper.class
        );
    }

    @Test
    public void testReaderThrowsException() {
        Response res = target().path("test").request("test/test").header("reader-exception", "throw").post(Entity.entity
                ("post", "test/test"));
        assertEquals(200, res.getStatus());
        final String entity = res.readEntity(String.class);
        assertEquals("reader-exception-mapper", entity);
    }


    @Test
    public void testWriterThrowsExceptionBeforeFirstBytesAreWritten() {
        Response res = target().path("test/before").request("test/test").get();
        assertEquals(200, res.getStatus());
        assertEquals("exception-before-first-bytes-exception-mapper", res.readEntity(String.class));
    }


    @Test
    public void testWriterThrowsExceptionAfterFirstBytesAreWritten() {
        Response res = target().path("test/after").request("test/test").get();
        assertEquals(200, res.getStatus());
        assertEquals("something", res.readEntity(String.class));
    }


    @Test
    public void testPreventMultipleExceptionMapping() {
        Response res = target().path("test/exception").request("test/test").get();
        // firstly exception is thrown in the resource method and is correctly mapped. Then it is again thrown in MBWriter but
        // exception can be mapped only once, so second exception in MBWriter cause 500 response code.
        assertEquals(500, res.getStatus());
    }

    @Path("test")
    public static class Resource {
        @GET
        @Path("before")
        @Produces("test/test")
        public Response exceptionBeforeFirstBytesAreWritten() {
            return Response.status(200).header("writer-exception", "before-first-byte").entity("ok").build();
        }


        @GET
        @Path("after")
        @Produces("test/test")
        public Response exceptionAfterFirstBytesAreWritten() {
            return Response.status(200).header("writer-exception", "after-first-byte").entity("ok").build();
        }

        @POST
        @Produces("test/test")
        public String post(String str) {
            return "post";
        }


        @GET
        @Path("exception")
        @Produces("test/test")
        public Response throwsException() {
            throw new MyAnotherException("resource");
        }

    }

    public static class MyExceptionMapper implements ExceptionMapper<MyException> {

        @Override
        public Response toResponse(MyException exception) {
            return Response.ok().entity(exception.getMessage() + "-exception-mapper").build();
        }
    }


    public static class MyExceptionMapperCauseAnotherException implements ExceptionMapper<MyAnotherException> {

        @Override
        public Response toResponse(MyAnotherException exception) {
            // the header causes exception to be thrown again in MyMessageBodyWriter
            return Response.ok().header("writer-exception", "before-first-byte").entity(exception.getMessage() +
                    "-another-exception-mapper").build();
        }
    }

    @Produces("test/test")
    public static class MyMessageBodyWritter implements MessageBodyWriter<String> {

        @Override
        public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return type == String.class;
        }

        @Override
        public long getSize(String s, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return s.length();
        }

        @Override
        public void writeTo(String s, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException,
                WebApplicationException {
            final List<Object> header = httpHeaders.get("writer-exception");

            OutputStreamWriter osw = new OutputStreamWriter(entityStream);

            if (header != null && header.size() > 0) {
                if (header.get(0).equals("before-first-byte")) {
                    throw new MyException("exception-before-first-bytes");
                } else if (header.get(0).equals("after-first-byte")) {
                    osw.write("something");
                    osw.flush();
                    throw new MyException("exception-after-first-bytes");
                }
            }
            osw.write(s);
            osw.flush();
        }
    }


    @Consumes("test/test")
    public static class MyMessageBodyReader implements MessageBodyReader<String> {

        @Override
        public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return type == String.class;
        }

        @Override
        public String readFrom(Class<String> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                               MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException,
                WebApplicationException {
            final List<String> header = httpHeaders.get("reader-exception");
            if (header != null && header.size() > 0 && header.get(0).equals("throw")) {
                throw new MyException("reader");
            }
            return "aaa";
        }
    }


    public static class MyException extends RuntimeException {
        public MyException() {
        }

        public MyException(Throwable cause) {
            super(cause);
        }

        public MyException(String message) {
            super(message);
        }

        public MyException(String message, Throwable cause) {
            super(message, cause);
        }
    }


    public static class MyAnotherException extends RuntimeException {
        public MyAnotherException() {
        }

        public MyAnotherException(Throwable cause) {
            super(cause);
        }

        public MyAnotherException(String message) {
            super(message);
        }

        public MyAnotherException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * BEGIN: JERSEY-1515 reproducer code
     */
    public static class VisibilityException extends WebApplicationException {

        private static final long serialVersionUID = -1159407312691372429L;

    }

    public static class VisibilityExceptionMapper implements ExceptionMapper<VisibilityException> {
        private HttpHeaders headers;
        private UriInfo info;
        private Application application;
        private Request request;
        private Providers provider;

        protected VisibilityExceptionMapper(@Context HttpHeaders headers,
                                            @Context UriInfo info, @Context Application application,
                                            @Context Request request, @Context Providers provider) {
            super();
            this.headers = headers;
            this.info = info;
            this.application = application;
            this.request = request;
            this.provider = provider;
        }

        public VisibilityExceptionMapper(@Context HttpHeaders headers,
                                         @Context UriInfo info, @Context Application application,
                                         @Context Request request) {
            super();
            this.headers = headers;
            this.info = info;
            this.application = application;
            this.request = request;
        }

        public VisibilityExceptionMapper(@Context HttpHeaders headers,
                                         @Context UriInfo info, @Context Application application) {
            super();
            this.headers = headers;
            this.info = info;
            this.application = application;
        }

        public VisibilityExceptionMapper(@Context HttpHeaders headers,
                                         @Context UriInfo info) {
            super();
            this.headers = headers;
            this.info = info;
        }

        public VisibilityExceptionMapper(@Context HttpHeaders headers) {
            super();
            this.headers = headers;
        }

        @Override
        public Response toResponse(VisibilityException exception) {
            return Response.ok("visible").build();
        }
    }

    @Path("test/visible")
    public static class TestResource {
        @GET
        public String throwVisibleException() {
            throw new VisibilityException();
        }
    }

    @Test
    public void testJersey1515() {
        Response res = target().path("test/visible").request().get();
        assertEquals(200, res.getStatus());
        assertEquals("visible", res.readEntity(String.class));
    }
    /**
     * END: JERSEY-1515 reproducer code
     */

    /**
     * BEGIN: JERSEY-1525 reproducer code.
     */
    @Path("test/responseFilter")
    public static class ExceptionTestResource {
        @GET
        @ThrowsNPE
        public String getData() {
            return "method";
        }
    }

    @NameBinding
    @Retention(RetentionPolicy.RUNTIME)
    private static @interface ThrowsNPE {
    }

    @ThrowsNPE
    public static class ExceptionThrowingFilter implements ContainerResponseFilter {
        @Override
        public void filter(ContainerRequestContext requestContext,
                           ContainerResponseContext responseContext) throws IOException {
            // The if clause prevents throwing exception on a mapped response.
            // Not doing so would result in a second exception being thrown
            // which would not be mapped again; instead, it would be propagated
            // to the hosting container directly.
            if (!"mapped-response-filter-exception".equals(responseContext.getEntity())) {
                throw new NullPointerException("response-filter-exception");
            }
        }
    }

    @Provider
    public static class ThrowableMapper implements ExceptionMapper<Throwable> {

        @Override
        public Response toResponse(Throwable throwable) {
            return Response.status(Response.Status.OK).entity("mapped-" + throwable.getMessage()).build();
        }

    }

    @Test
    public void testJersey1525() {
        Response res = target().path("test/responseFilter").request().get();
        assertEquals(200, res.getStatus());
        assertEquals("mapped-response-filter-exception", res.readEntity(String.class));
    }
    /**
     * END: JERSEY-1525 reproducer code
     */
}
