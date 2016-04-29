/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2016 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.tests.e2e.common;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Logger;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;

import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Michal Gajdos
 */
public class ContentTypeTest extends JerseyTest {

    private static final Logger LOGGER = Logger.getLogger(ContentTypeTest.class.getName());

    @Path(value = "/ContentType")
    public static class ContentTypeResource {

        @POST
        @Produces("text/plain")
        @SuppressWarnings("UnusedParameters")
        public void postTest(final String str) {
            // Ignore to generate response 204 - NoContent.
        }

        @POST
        @Path("changeTest")
        @Produces("foo/bar")
        @CtFix(ContainerRequestFilter.class)
        public String changeContentTypeTest(String echo) {
            return echo;
        }

        @POST
        @Path("null-content")
        public String process(@HeaderParam(HttpHeaders.CONTENT_TYPE) String mediaType, @Context ContainerRequest request) {
            return mediaType + "-" + request.hasEntity();
        }

    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    @interface CtFix {

        Class<? super ContentTypeFixProvider> value();
    }

    public static class ContentTypeFixProvider
            implements ReaderInterceptor, ContainerRequestFilter, ClientResponseFilter {

        @Override
        public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
            System.out.println("filter1");
            if (responseContext.getMediaType().toString().equals("foo/bar")) {
                responseContext.getHeaders().putSingle(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN);
            }
        }

        @Override
        public void filter(ContainerRequestContext context) throws IOException {
            System.out.println("filter2");
            if (context.getMediaType().toString().equals("foo/bar")) {
                context.getHeaders().putSingle(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN);
            }
        }

        @Override
        public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException, WebApplicationException {
            System.out.println("reader");
            if (context.getMediaType().toString().equals("foo/bar")) {
                context.getHeaders().putSingle(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN);
            }

            return context.proceed();
        }
    }

    public static class ContentTypeFixFeature implements DynamicFeature {

        @Override
        public void configure(ResourceInfo resourceInfo, FeatureContext context) {
            final CtFix annotation = resourceInfo.getResourceMethod().getAnnotation(CtFix.class);
            if (annotation != null) {
                context.register(ContentTypeFixProvider.class, annotation.value());
            }
        }
    }

    @Produces("foo/bar")
    public static class FooBarStringWriter implements MessageBodyWriter<String> {

        @Override
        public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return type == String.class && mediaType.toString().equals("foo/bar");
        }

        @Override
        public long getSize(String s, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return s.length();
        }

        @Override
        public void writeTo(String s,
                            Class<?> type,
                            Type genericType,
                            Annotation[] annotations,
                            MediaType mediaType,
                            MultivaluedMap<String, Object> httpHeaders,
                            OutputStream entityStream) throws IOException, WebApplicationException {

            entityStream.write(s.getBytes());
        }
    }

    @Override
    protected Application configure() {
        enable(TestProperties.DUMP_ENTITY);
        enable(TestProperties.LOG_TRAFFIC);

        return new ResourceConfig()
                .registerClasses(ContentTypeResource.class, ContentTypeFixFeature.class, FooBarStringWriter.class)
                .registerInstances(new LoggingFeature(LOGGER, LoggingFeature.Verbosity.PAYLOAD_ANY));
    }

    @Test
    public void testContentTypeHeaderForNoContentResponse() {
        final Response response = target().path("ContentType").request().post(Entity.entity("", MediaType.TEXT_PLAIN_TYPE));

        assertEquals(204, response.getStatus());
        assertNull(response.getHeaderString("Content-Type"));
    }

    @Test
    public void testInvalidContentTypeHeader() throws Exception {
        final URL url = new URL(getBaseUri().toString() + "ContentType");
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");
        connection.setRequestProperty("Accept", "text/plain");
        connection.setRequestProperty("Content-Type", "^^^");

        connection.setDoOutput(true);
        connection.connect();

        final OutputStream outputStream = connection.getOutputStream();
        outputStream.write("HelloWorld!".getBytes());
        outputStream.write('\n');
        outputStream.flush();

        assertEquals(400, connection.getResponseCode());
    }

    @Test
    public void testChangeContentTypeHeader() throws Exception {
        WebTarget target;
        Response response;

        // filter test
        target = target().path("ContentType").path("changeTest");
        target.register(ContentTypeFixProvider.class, ClientResponseFilter.class)
                .register(FooBarStringWriter.class);

        response = target
                .request().post(Entity.entity("test", "foo/bar"));

        assertEquals(200, response.getStatus());
        assertEquals(MediaType.TEXT_PLAIN_TYPE, response.getMediaType());
        assertEquals("test", response.readEntity(String.class));
        assertEquals(MediaType.TEXT_PLAIN_TYPE, response.getMediaType());

        // interceptor test
        target = target().path("ContentType").path("changeTest");
        target.register(ContentTypeFixProvider.class, ReaderInterceptor.class)
                .register(FooBarStringWriter.class);

        response = target
                .request().post(Entity.entity("test", "foo/bar"));

        assertEquals(200, response.getStatus());
        assertEquals(MediaType.valueOf("foo/bar"), response.getMediaType());
        assertEquals("test", response.readEntity(String.class));
        assertEquals(MediaType.TEXT_PLAIN_TYPE, response.getMediaType());
    }

    @Test
    public void testEmptyPostRequestWithContentType() {
        String response = target().path("ContentType/null-content").request("text/plain")
                .post(Entity.entity(null, "foo/bar"), String.class);

        assertEquals("foo/bar-false", response);
    }
}
