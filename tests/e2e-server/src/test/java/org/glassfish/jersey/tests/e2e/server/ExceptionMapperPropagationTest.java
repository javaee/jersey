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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.util.runner.ConcurrentRunner;

import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertEquals;

/**
 * Test exception mappers handling exceptions thrown from different part of code.
 * <p/>
 * There are more tests for exception mappers. This one focuses on testing that exceptions
 * thrown from providers are correctly propagated to the exception mapper.
 *
 * @author Miroslav Fuksa
 *
 */
@RunWith(ConcurrentRunner.class)
public class ExceptionMapperPropagationTest extends JerseyTest {

    public static final String EXCEPTION_TYPE = "exception-type";
    public static final String MAPPED = "-mapped-";
    public static final String MAPPED_WAE = "-wae-";
    public static final String PROVIDER = "provider";

    public static class TestRuntimeException extends RuntimeException {

        public TestRuntimeException(String message) {
            super(message);
        }

    }

    public static class TestCheckedException extends Exception {

        public TestCheckedException(String message) {
            super(message);
        }

    }

    public static class TestWebAppException extends WebApplicationException {

        public TestWebAppException(String message, Response response) {
            super(message, response);
        }

    }

    @Override
    protected Application configure() {
        return new ResourceConfig(
                UniversalThrowableMapper.class,
                ExceptionResource.class,
                TestResponseFilter.class,
                TestRequestFilter.class,
                WebAppMapper.class,
                TestMBR.class,
                TestMBW.class,
                TestWriterInterceptor.class,
                TestReaderInterceptor.class
        );
    }

    public static class UniversalThrowableMapper implements ExceptionMapper<Throwable> {

        @Override
        public Response toResponse(Throwable exception) {
            return Response.ok().entity(exception.getClass().getSimpleName() + MAPPED + exception.getMessage()).build();
        }

    }

    public static class WebAppMapper implements ExceptionMapper<TestWebAppException> {

        @Override
        public Response toResponse(TestWebAppException exception) {
            final Response response = exception.getResponse();
            return Response.status(response.getStatus())
                    .entity(exception.getClass().getSimpleName() + MAPPED_WAE + exception.getMessage())
                    .build();
        }
    }

    public static void throwException(String exceptionType, String provider, Class<?> currentClass) throws Throwable {

        if (shouldThrow(exceptionType, provider, currentClass)) {
            if (exceptionType.equals(TestCheckedException.class.getSimpleName())) {
                throw new TestCheckedException(provider);
            } else if (exceptionType.equals(TestRuntimeException.class.getSimpleName())) {
                throw new TestRuntimeException(provider);
            } else if (exceptionType.equals(TestWebAppException.class.getSimpleName())) {
                throw new TestWebAppException(provider, Response.ok().build());
            } else if (exceptionType.equals(ProcessingException.class.getSimpleName())) {
                throw new ProcessingException(provider);
            }
        }
    }

    private static boolean shouldThrow(String exceptionType, String provider, Class<?> currentClass) {
        return exceptionType != null && currentClass.getSimpleName().equals(provider);
    }

    @Path("exception")
    public static class ExceptionResource {

        @Path("general")
        @POST
        public Response post(@HeaderParam(EXCEPTION_TYPE) String exceptionType,
                             @HeaderParam(PROVIDER) String provider, String entity) throws Throwable {
            throwException(exceptionType, provider, this.getClass());
            return Response.ok().entity("exception/general#get called")
                    .header(EXCEPTION_TYPE, exceptionType)
                    .header(PROVIDER, provider).build();
        }

        @Path("sub")
        public SubResourceLocator subResourceLocator(@HeaderParam(EXCEPTION_TYPE) String exceptionType,
                                                     @HeaderParam(PROVIDER) String provider) throws Throwable {
            throwException(exceptionType, provider, this.getClass());
            return new SubResourceLocator();
        }

    }

    public static class SubResourceLocator {

        @POST
        public String post(@HeaderParam(EXCEPTION_TYPE) String exceptionType,
                           @HeaderParam(PROVIDER) String provider, String entity) throws Throwable {
            throwException(exceptionType, provider, this.getClass());
            return "sub-get";
        }
    }

    public static class TestResponseFilter implements ContainerResponseFilter {

        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
            final String exceptionType = responseContext.getHeaderString(EXCEPTION_TYPE);
            final String provider = responseContext.getHeaderString(PROVIDER);

            throwRuntimeExceptionAndIO(exceptionType, this.getClass(), provider);
        }
    }

    public static class TestRequestFilter implements ContainerRequestFilter {

        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
            final String exceptionType = requestContext.getHeaderString(EXCEPTION_TYPE);
            final String provider = requestContext.getHeaderString(PROVIDER);

            throwRuntimeExceptionAndIO(exceptionType, this.getClass(), provider);
        }
    }

    @Consumes(MediaType.TEXT_PLAIN)
    public static class TestMBR implements MessageBodyReader<String> {

        @Override
        public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return type == String.class;
        }

        @Override
        public String readFrom(Class<String> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                               MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException,
                WebApplicationException {
            final String exceptionType = httpHeaders.getFirst(EXCEPTION_TYPE);
            final String provider = httpHeaders.getFirst(PROVIDER);
            throwRuntimeExceptionAndIO(exceptionType, this.getClass(), provider);

            byte b;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            while ((b = (byte) entityStream.read()) != -1) {
                baos.write(b);
            }
            return new String(baos.toByteArray());
        }
    }

    public static class TestMBW implements MessageBodyWriter<String> {

        @Override
        public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return type == String.class;
        }

        @Override
        public long getSize(String s, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return 0;
        }

        @Override
        public void writeTo(String s, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
                throws IOException, WebApplicationException {
            final String exceptionType = (String) httpHeaders.getFirst(EXCEPTION_TYPE);
            final String provider = (String) httpHeaders.getFirst(PROVIDER);
            throwRuntimeExceptionAndIO(exceptionType, this.getClass(), provider);

            entityStream.write(s.getBytes());
            entityStream.flush();
        }
    }

    @Consumes(MediaType.TEXT_PLAIN)
    public static class TestReaderInterceptor implements ReaderInterceptor {

        @Override
        public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException, WebApplicationException {
            final String exceptionType = context.getHeaders().getFirst(EXCEPTION_TYPE);
            final String provider = context.getHeaders().getFirst(PROVIDER);

            throwRuntimeExceptionAndIO(exceptionType, this.getClass(), provider);
            return context.proceed();
        }
    }

    public static class TestWriterInterceptor implements WriterInterceptor {

        @Override
        public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
            final String exceptionType = (String) context.getHeaders().getFirst(EXCEPTION_TYPE);
            final String provider = (String) context.getHeaders().getFirst(PROVIDER);

            throwRuntimeExceptionAndIO(exceptionType, this.getClass(), provider);
            context.proceed();
        }
    }

    private static void throwRuntimeExceptionAndIO(String exceptionType, Class<?> providerClass, String provider)
            throws IOException {
        if (shouldThrow(exceptionType, provider, providerClass)) {
            if (exceptionType.equals(TestRuntimeException.class.getSimpleName())) {
                throw new TestRuntimeException(providerClass.getSimpleName());
            } else if (exceptionType.equals(IOException.class.getSimpleName())) {
                throw new IOException(providerClass.getSimpleName());
            } else if (exceptionType.equals(TestWebAppException.class.getSimpleName())) {
                throw new TestWebAppException(providerClass.getSimpleName(), Response.ok().build());
            } else if (exceptionType.equals(ProcessingException.class.getSimpleName())) {
                throw new ProcessingException(provider);
            }
        }
    }

    // Resource
    @Test
    public void testCheckedExceptionInResource() {
        _test(TestCheckedException.class, ExceptionResource.class);
    }

    @Test
    public void testRuntimeExceptionInResource() {
        _test(TestRuntimeException.class, ExceptionResource.class);
    }

    @Test
    public void testWebApplicationExceptionInResource() {
        _testWae(ExceptionResource.class);
    }

    @Test
    public void testProcessingExceptionInResource() {
        _test(ProcessingException.class, ExceptionResource.class);
    }

    // Sub resource
    @Test
    public void testCheckedExceptionInSubResourceLocatorMethod() {
        _test(TestCheckedException.class, ExceptionResource.class, "exception/sub");
    }

    @Test
    public void testRuntimeExceptionInSubResourceLocatorMethod() {
        _test(TestRuntimeException.class, ExceptionResource.class, "exception/sub");
    }

    @Test
    public void testWaeInSubResourceLocatorMethod() {
        _testWae(ExceptionResource.class, "exception/sub");
    }

    @Test
    public void testProcessingExceptionInSubResourceLocatorMethod() {
        _test(ProcessingException.class, ExceptionResource.class, "exception/sub");
    }

    @Test
    public void testCheckedExceptionInSubResource() {
        _test(TestCheckedException.class, SubResourceLocator.class, "exception/sub");
    }

    @Test
    public void testRuntimeExceptionInSubResource() {
        _test(TestRuntimeException.class, SubResourceLocator.class, "exception/sub");
    }

    @Test
    public void testWaeInSubResource() {
        _testWae(SubResourceLocator.class, "exception/sub");
    }

    @Test
    public void testProcessingExceptionInSubResource() {
        _test(ProcessingException.class, SubResourceLocator.class, "exception/sub");
    }

    // response filters
    @Test
    public void testRuntimeExceptionInResponseFilter() {
        _test(TestRuntimeException.class, TestResponseFilter.class);
    }

    @Test
    public void testIOExceptionInResponseFilter() {
        _test(IOException.class, TestResponseFilter.class);
    }

    @Test
    public void testWaeInResponseFilter() {
        _testWae(TestResponseFilter.class);
    }

    @Test
    public void testProcessingExceptionInResponseFilter() {
        _test(ProcessingException.class, TestResponseFilter.class);
    }

    // response filters
    @Test
    public void testRuntimeExceptionInRequestFilter() {
        _test(TestRuntimeException.class, TestRequestFilter.class);
    }

    @Test
    public void testIOExceptionInRequestFilter() {
        _test(IOException.class, TestRequestFilter.class);
    }

    @Test
    public void testWaeInRequestFilter() {
        _testWae(TestRequestFilter.class);
    }

    @Test
    public void testProcessingExceptionInRequestFilter() {
        _test(ProcessingException.class, TestRequestFilter.class);
    }

    // MBR/W
    @Test
    public void testRuntimeExceptionInMBW() {
        _test(TestRuntimeException.class, TestMBW.class);
    }

    @Test
    public void testIOExceptionInMBW() {
        _test(IOException.class, TestMBW.class);
    }

    @Test
    public void testWaeInMBW() {
        _testWae(TestMBW.class);
    }

    @Test
    public void testProcessingExceptionInMBW() {
        _test(ProcessingException.class, TestMBW.class);
    }

    @Test
    public void testRuntimeExceptionInMBR() {
        _test(TestRuntimeException.class, TestMBR.class);
    }

    @Test
    public void testIOExceptionInMBR() {
        _test(IOException.class, TestMBR.class);
    }

    @Test
    public void testWaeInMBR() {
        _testWae(TestMBR.class);
    }

    @Test
    public void testProcessingExceptionInMBR() {
        _test(ProcessingException.class, TestMBR.class);
    }

    // interceptors
    @Test
    public void testRuntimeExceptionInReaderInterceptor() {
        _test(TestRuntimeException.class, TestReaderInterceptor.class);
    }

    @Test
    public void testIOExceptionInReaderInterceptor() {
        _test(IOException.class, TestReaderInterceptor.class);
    }

    @Test
    public void testWaeInReaderInterceptor() {
        _testWae(TestReaderInterceptor.class);
    }

    @Test
    public void testProcessingExceptionInReaderInterceptor() {
        _test(ProcessingException.class, TestReaderInterceptor.class);
    }

    @Test
    public void testRuntimeExceptionInWriterInterceptor() {
        _test(TestRuntimeException.class, TestWriterInterceptor.class);
    }

    @Test
    public void testIOExceptionInWriterInterceptor() {
        _test(IOException.class, TestWriterInterceptor.class);
    }

    @Test
    public void testWaeInWriterInterceptor() {
        _testWae(TestWriterInterceptor.class);
    }

    @Test
    public void testProcessingExceptionInWriterInterceptor() {
        _test(ProcessingException.class, TestWriterInterceptor.class);
    }

    private void _test(Class<?> exceptionClass, Class<?> providerClass, String path) {
        final Response response = target(path).request()
                .header(EXCEPTION_TYPE, exceptionClass.getSimpleName()).header(PROVIDER, providerClass.getSimpleName())
                .post(Entity.entity("post", MediaType.TEXT_PLAIN_TYPE));
        assertEquals(200, response.getStatus());
        assertEquals(exceptionClass.getSimpleName() + MAPPED + providerClass.getSimpleName(), response.readEntity(String.class));
    }

    private void _testWae(Class<?> providerClass) {
        final String path = "exception/general";
        _testWae(providerClass, path);

    }

    private void _testWae(Class<?> providerClass, String path) {
        final Class<?> exceptionClass = TestWebAppException.class;
        final Response response = target(path).request()
                .header(EXCEPTION_TYPE, exceptionClass.getSimpleName()).header(PROVIDER, providerClass.getSimpleName())
                .post(Entity.entity("post", MediaType.TEXT_PLAIN_TYPE));
        assertEquals(200, response.getStatus());
        assertEquals(exceptionClass.getSimpleName() + MAPPED_WAE + providerClass.getSimpleName(),
                response.readEntity(String.class));
    }

    private void _test(Class<?> exceptionClass, Class<?> providerClass) {
        final String path = "exception/general";
        _test(exceptionClass, providerClass, path);
    }

    // sub resource locator

}
