/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.server.model;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.message.internal.MediaTypes;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.RequestContextBuilder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;

import org.junit.Assert;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.sun.research.ws.wadl.Application;

/**
 *
 * @author Paul Sandoz
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 * @author Miroslav Fuksa
 */
public class OptionsTest {

    @Target({ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @HttpMethod("patch")
    public @interface PATCH {
    }

    private ApplicationHandler app;

    private void initiateWebApplication(Class<?>... classes) {
        app = new ApplicationHandler(new ResourceConfig(classes));
    }

    @Path("/")
    public static class ResourceNoOptions {

        @GET
        public String get() {
            return "GET";
        }

        @PUT
        public String put(String e) {
            return "PUT";
        }

        @POST
        public String post(String e) {
            return "POST";
        }

        @DELETE
        public void delete() {
        }

        @PATCH
        public String patch(String e) {
            return "PATCH";
        }
    }

    @Test
    public void testNoOptions() throws Exception {
        initiateWebApplication(ResourceNoOptions.class);

        ContainerResponse response = app.apply(RequestContextBuilder.from("/", "OPTIONS").build()).get();
        assertEquals(200, response.getStatus());
        _checkAllowContent(response.getHeaderString("Allow"));

        final MediaType mediaType = MediaType.APPLICATION_XML_TYPE;
        response = app.apply(RequestContextBuilder.from("/", "OPTIONS").accept(mediaType).build()).get();
        assertEquals(200, response.getStatus());
        assertEquals(mediaType, response.getMediaType());
        assertEquals(0, response.getLength());
        _checkAllowContent(response.getHeaderString("Allow"));
    }

    private void _checkAllowContent(String allow) {
        assertTrue(allow.contains("OPTIONS"));
        assertTrue(allow.contains("GET"));
        assertTrue(allow.contains("PUT"));
        assertTrue(allow.contains("POST"));
        assertTrue(allow.contains("DELETE"));
        assertTrue(allow.contains("PATCH"));
    }

    @Path("/")
    public static class ResourceWithOptions {

        @OPTIONS
        public Response options() {
            return Response.ok("OPTIONS")
                    .header("Allow", "OPTIONS, GET, PUT, POST, DELETE, PATCH")
                    .header("X-TEST", "OVERRIDE").build();
        }

        @GET
        public String get() {
            return "GET";
        }

        @PUT
        public String put(String e) {
            return "PUT";
        }

        @POST
        public String post(String e) {
            return "POST";
        }

        @DELETE
        public void delete() {
        }

        @PATCH
        public String patch(String e) {
            return "PATCH";
        }
    }

    @Test
    public void testWithOptions() throws Exception {
        initiateWebApplication(ResourceWithOptions.class);

        ContainerResponse response = app.apply(RequestContextBuilder.from("/", "OPTIONS").build()).get();

        assertEquals(200, response.getStatus());

        String allow = response.getHeaderString("Allow");
        assertTrue(allow.contains("OPTIONS"));
        assertTrue(allow.contains("GET"));
        assertTrue(allow.contains("PUT"));
        assertTrue(allow.contains("POST"));
        assertTrue(allow.contains("DELETE"));
        assertTrue(allow.contains("PATCH"));

        assertEquals("OVERRIDE", response.getHeaderString("X-TEST"));
    }

    @Path("resource")
    public static class WadlResource {

        @GET
        public String get() {
            return "get";
        }
    }

    @Test
    public void testRequestNoType() throws ExecutionException, InterruptedException {
        ApplicationHandler application = new ApplicationHandler(new ResourceConfig(WadlResource.class));
        final ContainerRequest request = RequestContextBuilder.from("/resource", "OPTIONS").build();
        final ContainerResponse response = application.apply(request).get();
        Assert.assertEquals(200, response.getStatus());
        final MediaType type = response.getMediaType();
        Assert.assertTrue(type.equals(MediaTypes.WADL_TYPE) || type.equals(MediaType.TEXT_HTML_TYPE)
                || type.equals(MediaType.TEXT_PLAIN));

    }

    @Test
    public void testRequestTextPlain() throws ExecutionException, InterruptedException {
        ApplicationHandler application = new ApplicationHandler(new ResourceConfig(WadlResource.class, ResponseTextFilter.class));
        final ContainerResponse response = testOptions(MediaType.TEXT_PLAIN_TYPE, application, "/resource");
        final String entity = (String) response.getEntity();
        Assert.assertTrue(entity.contains("GET"));
    }

    @Test
    public void testRequestTextHtml() throws ExecutionException, InterruptedException {
        ApplicationHandler application = new ApplicationHandler(new ResourceConfig(WadlResource.class, ResponseHtmlFilter.class));
        final MediaType requestType = MediaType.TEXT_HTML_TYPE;
        final ContainerResponse response = testOptions(requestType, application, "/resource");
        Assert.assertTrue(response.getAllowedMethods().contains("GET"));
    }

    @Test
    public void testRequestWadl() throws ExecutionException, InterruptedException {
        ApplicationHandler application = new ApplicationHandler(new ResourceConfig(WadlResource.class, ResponseWadlFilter.class));
        final MediaType requestType = MediaTypes.WADL_TYPE;
        final ContainerResponse response = testOptions(requestType, application, "/resource");
    }

    private static class ResponseTextFilter implements ContainerResponseFilter {

        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
            final MediaType type = responseContext.getMediaType();
            Assert.assertEquals(MediaType.TEXT_PLAIN_TYPE, type);

        }
    }

    private static class ResponseHtmlFilter implements ContainerResponseFilter {

        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
            final MediaType type = responseContext.getMediaType();
            Assert.assertEquals(MediaType.TEXT_HTML_TYPE, type);

        }
    }

    private static class ResponseWadlFilter implements ContainerResponseFilter {

        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
            final MediaType type = responseContext.getMediaType();
            Assert.assertEquals(MediaTypes.WADL_TYPE, type);
            responseContext.getEntity().getClass().equals(Application.class);

        }
    }

    @Path("no-get")
    private static class ResourceWithoutGetMethod {

        @POST
        public String post(String entity) {
            return entity;
        }

        @POST
        @Path("sub")
        public String subPost(String entity) {
            return entity;
        }

    }

    @Test
    public void testNoHead() throws ExecutionException, InterruptedException {
        ApplicationHandler application = new ApplicationHandler(new ResourceConfig(ResourceWithoutGetMethod.class));
        final ContainerResponse response = testOptions(MediaType.TEXT_PLAIN_TYPE, application, "/no-get");

        Assert.assertFalse(((String) response.getEntity()).contains("HEAD"));
    }

    @Test
    public void testNoHeadWildcard() throws ExecutionException, InterruptedException {
        final ResourceConfig resourceConfig = new ResourceConfig(ResourceWithoutGetMethod.class);
        resourceConfig.property(ServerProperties.WADL_FEATURE_DISABLE, true);
        ApplicationHandler application = new ApplicationHandler(resourceConfig);

        final ContainerRequest request = RequestContextBuilder.from("/no-get", "OPTIONS").accept(MediaType.MEDIA_TYPE_WILDCARD)
                .build();
        final ContainerResponse response = application.apply(request).get();
        Assert.assertEquals(200, response.getStatus());

        final List<String> strings = response.getStringHeaders().get(HttpHeaders.ALLOW);
        for (String allow : strings) {
            Assert.assertFalse(allow.contains("HEAD"));
        }
    }

    private ContainerResponse testOptions(MediaType requestType, ApplicationHandler application, String path)
            throws InterruptedException, ExecutionException {
        final ContainerRequest request = RequestContextBuilder.from(path, "OPTIONS").accept(requestType)
                .build();
        final ContainerResponse response = application.apply(request).get();
        Assert.assertEquals(200, response.getStatus());
        final MediaType type = response.getMediaType();
        Assert.assertEquals(requestType, type);
        return response;
    }

    @Test
    public void testNoHeadInSub() throws ExecutionException, InterruptedException {
        ApplicationHandler application = new ApplicationHandler(new ResourceConfig(ResourceWithoutGetMethod.class));
        final MediaType requestType = MediaType.TEXT_PLAIN_TYPE;
        final ContainerResponse response = testOptions(requestType, application, "/no-get/sub");
        Assert.assertFalse(((String) response.getEntity()).contains("HEAD"));
    }
}
