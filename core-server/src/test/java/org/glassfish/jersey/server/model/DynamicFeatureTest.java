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

package org.glassfish.jersey.server.model;

import java.io.IOException;
import java.lang.annotation.Annotation;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Providers;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import javax.inject.Inject;

import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.RequestContextBuilder;
import org.glassfish.jersey.server.ResourceConfig;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * Tests cases of {@code DynamicFeature} implementation.
 *
 * @author Michal Gajdos
 */
public class DynamicFeatureTest {

    @Path("resource")
    public static class Resource {

        @GET
        public String get() {
            return "get";
        }

        @GET
        @Path("postmatch")
        public String getPostMatch(@Context final HttpHeaders headers) {
            assertEquals("true", headers.getHeaderString("postmatch"));
            return "get";
        }

        @POST
        @Path("providers")
        public String getProviders(@Context final HttpHeaders headers,
                                   @Context final Providers providers,
                                   final String entity) {
            assertNull(providers.getContextResolver(String.class, MediaType.WILDCARD_TYPE));

            assertEquals("ProviderBall", headers.getHeaderString("reader"));
            assertEquals("bar", headers.getHeaderString("foo"));

            return entity;
        }

        @GET
        @Path("providers/error")
        public String getProvidersError() {
            throw new CustomException("error");
        }

        @Path("sub")
        public SubResource subResource() {
            return new SubResource();
        }
    }

    public static class SubResource {

        @GET
        public String get() {
            return "sub-get";
        }
    }

    public static class CustomResponseFilter implements ContainerResponseFilter {

        @Override
        public void filter(final ContainerRequestContext requestContext, final ContainerResponseContext responseContext)
                throws IOException {

            responseContext.setEntity(
                    responseContext.getEntity() + "-filtered",
                    new Annotation[0],
                    responseContext.getMediaType());
        }
    }

    @PreMatching
    public static class PreMatchingRequestFilter implements ContainerRequestFilter {

        @Override
        public void filter(final ContainerRequestContext requestContext) throws IOException {
            if (requestContext.getUriInfo().getMatchedURIs().isEmpty()) {
                fail("Filter executed in PreMatching phase.");
            } else {
                requestContext.getHeaders().add("postmatch", "true");
            }
        }
    }

    public static class PreMatchingDynamicFeature implements DynamicFeature {

        @Override
        public void configure(final ResourceInfo resourceInfo, final FeatureContext context) {
            context.register(PreMatchingRequestFilter.class);
        }
    }

    @Test
    public void testPreMatchingFilter() throws Exception {
        final ApplicationHandler application = createApplication(PreMatchingDynamicFeature.class);

        ContainerResponse response;

        response = application.apply(RequestContextBuilder.from("/resource/postmatch", "GET").build()).get();
        assertEquals(200, response.getStatus());
        assertEquals("get", response.getEntity());
    }

    public static class SubResourceDynamicFeature implements DynamicFeature {

        @Override
        public void configure(final ResourceInfo resourceInfo, final FeatureContext context) {
            if (resourceInfo.getResourceClass().equals(SubResource.class)
                    && "get".equals(resourceInfo.getResourceMethod().getName())) {
                context.register(new CustomResponseFilter());
            }
        }
    }

    @Test
    public void testSubResourceFeature() throws Exception {
        final ApplicationHandler application = createApplication(SubResourceDynamicFeature.class);

        ContainerResponse response;

        response = application.apply(RequestContextBuilder.from("/resource/sub", "GET").build()).get();
        assertEquals(200, response.getStatus());
        assertEquals("sub-get-filtered", response.getEntity());

        response = application.apply(RequestContextBuilder.from("/resource", "GET").build()).get();
        assertEquals(200, response.getStatus());
        assertEquals("get", response.getEntity());
    }

    public static class ProviderBall implements ReaderInterceptor, WriterInterceptor, ContextResolver<String>, ExceptionMapper {

        @Override
        public String getContext(final Class<?> type) {
            return "ProviderBall";
        }

        @Override
        public Response toResponse(final Throwable exception) {
            return Response.ok().entity("ProviderBall").build();
        }

        @Override
        public Object aroundReadFrom(final ReaderInterceptorContext context) throws IOException, WebApplicationException {
            context.getHeaders().add("reader", "ProviderBall");
            return context.proceed();
        }

        @Override
        public void aroundWriteTo(final WriterInterceptorContext context) throws IOException, WebApplicationException {
            context.getHeaders().add("writer", "ProviderBall");

            context.proceed();
        }
    }

    public static class SupportedProvidersDynamicFeature implements DynamicFeature {

        @Override
        public void configure(final ResourceInfo resourceInfo, final FeatureContext context) {
            context.register(ProviderBall.class);
            context.register(new ContainerRequestFilter() {
                @Override
                public void filter(final ContainerRequestContext requestContext) throws IOException {
                    requestContext.getHeaders().add("foo", "bar");
                }
            });
            //noinspection unchecked
            context.register(new CustomResponseFilter(), MessageBodyReader.class);
        }
    }

    @Test
    public void testSupportedProvidersFeature() throws Exception {
        final ApplicationHandler application = createApplication(SupportedProvidersDynamicFeature.class);

        ContainerResponse response;

        response = application.apply(RequestContextBuilder.from("/resource/providers", "POST").entity("get").build()).get();
        assertEquals(200, response.getStatus());
        assertEquals("get", response.getEntity());
        assertEquals("ProviderBall", response.getHeaderString("writer"));
    }

    public static class CustomException extends RuntimeException {

        public CustomException(final String error) {
            super(error);
        }
    }

    @Test
    public void testNegativeSupportedProvidersFeature() throws Exception {
        final ApplicationHandler application = createApplication(SupportedProvidersDynamicFeature.class);

        try {
            application.apply(RequestContextBuilder.from("/resource/providers/error", "GET").build()).get();
        } catch (Exception e) {
            while (!(e instanceof CustomException)) {
                e = (Exception) e.getCause();
            }
            assertEquals("error", e.getMessage());
        }
    }

    public static class InjectConfigurableProvider implements ContainerResponseFilter {

        private final Configuration configuration;

        @Inject
        public InjectConfigurableProvider(final Configuration configuration) {
            this.configuration = configuration;
        }

        @Override
        public void filter(final ContainerRequestContext requestContext, final ContainerResponseContext responseContext)
                throws IOException {

            assertNotNull(configuration);
            assertEquals("bar", configuration.getProperty("foo"));
            assertEquals("world", configuration.getProperty("hello"));
        }
    }

    public static class InjectConfigurableDynamicFeature implements DynamicFeature {

        @Override
        public void configure(final ResourceInfo resourceInfo, final FeatureContext context) {
            context.register(InjectConfigurableProvider.class);
            context.property("foo", "bar");

            assertEquals("world", context.getConfiguration().getProperty("hello"));
        }
    }

    @Test
    public void testInjectedConfigurable() throws Exception {
        final ResourceConfig resourceConfig = getTestResourceConfig(InjectConfigurableDynamicFeature.class);
        resourceConfig.property("hello", "world");

        final ApplicationHandler application = createApplication(resourceConfig);

        assertNull(application.getConfiguration().getProperty("foo"));

        final ContainerResponse response = application.apply(RequestContextBuilder.from("/resource", "GET").build()).get();
        assertEquals(200, response.getStatus());
        assertEquals("get", response.getEntity());

        assertNull(application.getConfiguration().getProperty("foo"));
        assertEquals("world", application.getConfiguration().getProperty("hello"));
    }

    private ApplicationHandler createApplication(final Class<?>... dynamicFeatures) {
        return createApplication(getTestResourceConfig(dynamicFeatures));
    }

    private ResourceConfig getTestResourceConfig(final Class<?>... dynamicFeatures) {
        return new ResourceConfig()
                .registerClasses(Resource.class, SubResource.class)
                .registerClasses(dynamicFeatures);
    }

    private ApplicationHandler createApplication(final ResourceConfig resourceConfig) {
        return new ApplicationHandler(resourceConfig);
    }
}
