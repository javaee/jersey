/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.server.filter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Response;

import javax.annotation.Priority;

import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.RequestContextBuilder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;

import org.glassfish.hk2.utilities.binding.AbstractBinder;

import org.junit.Assert;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import jersey.repackaged.com.google.common.collect.Lists;

/**
 * Test for JAX-RS filters.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class ApplicationFilterTest {

    /**
     * Utility Injection binder that may be used for registering provider instances of provider
     * type {@code T} in HK2.
     */
    static class ProviderInstanceBindingBinder<T> extends AbstractBinder {

        private final Iterable<? extends T> providers;
        private final Class<T> providerType;

        /**
         * Create an injection binder for the supplied collection of provider instances.
         *
         * @param providers list of provider instances.
         * @param providerType registered provider contract type.
         */
        public ProviderInstanceBindingBinder(final Iterable<? extends T> providers, final Class<T> providerType) {
            this.providers = providers;
            this.providerType = providerType;
        }

        @Override
        protected void configure() {
            for (final T provider : providers) {
                bind(provider).to(providerType);
            }
        }
    }

    @Test
    public void testSingleRequestFilter() throws Exception {

        final AtomicInteger called = new AtomicInteger(0);

        final List<ContainerRequestFilter> requestFilters = Lists.newArrayList();
        requestFilters.add(new ContainerRequestFilter() {
            @Override
            public void filter(final ContainerRequestContext context) throws IOException {
                called.incrementAndGet();
            }
        });

        final ResourceConfig resourceConfig = new ResourceConfig()
                .register(new ProviderInstanceBindingBinder<>(requestFilters, ContainerRequestFilter.class));

        final Resource.Builder rb = Resource.builder("test");
        rb.addMethod("GET").handledBy(new Inflector<ContainerRequestContext, Response>() {

            @Override
            public Response apply(final ContainerRequestContext request) {
                return Response.ok().build();
            }
        });
        resourceConfig.registerResources(rb.build());
        final ApplicationHandler application = new ApplicationHandler(resourceConfig);

        assertEquals(200, application.apply(RequestContextBuilder.from("/test", "GET").build()).get().getStatus());
        assertEquals(1, called.intValue());
    }

    @Test
    public void testSingleResponseFilter() throws Exception {
        final AtomicInteger called = new AtomicInteger(0);

        final List<ContainerResponseFilter> responseFilterList = Lists.newArrayList();
        responseFilterList.add(new ContainerResponseFilter() {
            @Override
            public void filter(final ContainerRequestContext requestContext, final ContainerResponseContext responseContext)
                    throws IOException {
                called.incrementAndGet();
            }
        });

        final ResourceConfig resourceConfig = new ResourceConfig()
                .register(new ProviderInstanceBindingBinder<>(responseFilterList, ContainerResponseFilter.class));

        final Resource.Builder rb = Resource.builder("test");
        rb.addMethod("GET").handledBy(new Inflector<ContainerRequestContext, Response>() {

            @Override
            public Response apply(final ContainerRequestContext request) {
                return Response.ok().build();
            }
        });
        resourceConfig.registerResources(rb.build());
        final ApplicationHandler application = new ApplicationHandler(resourceConfig);

        assertEquals(200, application.apply(RequestContextBuilder.from("/test", "GET").build()).get().getStatus());
        assertEquals(1, called.intValue());
    }

    @Test
    public void testFilterCalledOn200() throws Exception {
        final SimpleFilter simpleFilter = new SimpleFilter();
        final ResourceConfig resourceConfig = new ResourceConfig(SimpleResource.class).register(simpleFilter);
        final ApplicationHandler application = new ApplicationHandler(resourceConfig);
        final ContainerResponse response = application.apply(RequestContextBuilder.from("/simple", "GET").build()).get();
        assertEquals(200, response.getStatus());
        Assert.assertTrue(simpleFilter.called);
    }

    @Test
    public void testFilterNotCalledOn404() throws Exception {
        // not found
        final SimpleFilter simpleFilter = new SimpleFilter();
        final ResourceConfig resourceConfig = new ResourceConfig(SimpleResource.class).register(simpleFilter);
        final ApplicationHandler application = new ApplicationHandler(resourceConfig);
        final ContainerResponse response = application.apply(RequestContextBuilder.from("/NOT-FOUND", "GET").build()).get();
        assertEquals(404, response.getStatus());
        Assert.assertFalse(simpleFilter.called);
    }

    @Test
    public void testFilterNotCalledOn405() throws Exception {
        // method not allowed
        final SimpleFilter simpleFilter = new SimpleFilter();
        final ResourceConfig resourceConfig = new ResourceConfig(SimpleResource.class).register(simpleFilter);
        final ApplicationHandler application = new ApplicationHandler(resourceConfig);
        final ContainerResponse response = application.apply(RequestContextBuilder.from("/simple", "POST").entity("entity")
                .build()).get();
        assertEquals(405, response.getStatus());
        Assert.assertFalse(simpleFilter.called);
    }

    @Path("simple")
    public static class SimpleResource {

        @GET
        public String get() {
            return "get";
        }
    }

    public abstract class CommonFilter implements ContainerRequestFilter {

        public boolean called = false;

        @Override
        public void filter(final ContainerRequestContext context) throws IOException {
            verify();
            called = true;
        }

        protected abstract void verify();
    }

    public class SimpleFilter extends CommonFilter {

        @Override
        protected void verify() {
        }
    }

    @Priority(1)
    public class Filter1 extends CommonFilter {

        private Filter10 filter10;
        private Filter100 filter100;

        public void setFilters(final Filter10 filter10, final Filter100 filter100) {
            this.filter10 = filter10;
            this.filter100 = filter100;
        }

        @Override
        protected void verify() {
            assertTrue(filter10.called == false);
            assertTrue(filter100.called == false);
        }
    }

    @Priority(10)
    public class Filter10 extends CommonFilter {

        private Filter1 filter1;
        private Filter100 filter100;

        public void setFilters(final Filter1 filter1, final Filter100 filter100) {
            this.filter1 = filter1;
            this.filter100 = filter100;
        }

        @Override
        protected void verify() {
            assertTrue(filter1.called == true);
            assertTrue(filter100.called == false);
        }
    }

    @Priority(100)
    public class Filter100 extends CommonFilter {

        private Filter1 filter1;
        private Filter10 filter10;

        public void setFilters(final Filter1 filter1, final Filter10 filter10) {
            this.filter1 = filter1;
            this.filter10 = filter10;
        }

        @Override
        protected void verify() {
            assertTrue(filter1.called);
            assertTrue(filter10.called);
        }
    }

    @Test
    public void testMultipleFiltersWithBindingPriority() throws Exception {

        final Filter1 filter1 = new Filter1();
        final Filter10 filter10 = new Filter10();
        final Filter100 filter100 = new Filter100();

        filter1.setFilters(filter10, filter100);
        filter10.setFilters(filter1, filter100);
        filter100.setFilters(filter1, filter10);

        final List<ContainerRequestFilter> requestFilterList = Lists.newArrayList();
        requestFilterList.add(filter100);
        requestFilterList.add(filter1);
        requestFilterList.add(filter10);

        final ResourceConfig resourceConfig = new ResourceConfig()
                .register(new ProviderInstanceBindingBinder<>(requestFilterList, ContainerRequestFilter.class));

        final Resource.Builder rb = Resource.builder("test");
        rb.addMethod("GET").handledBy(new Inflector<ContainerRequestContext, Response>() {

            @Override
            public Response apply(final ContainerRequestContext request) {
                return Response.ok().build();
            }
        });
        resourceConfig.registerResources(rb.build());
        final ApplicationHandler application = new ApplicationHandler(resourceConfig);

        assertEquals(200, application.apply(RequestContextBuilder.from("/test", "GET").build()).get().getStatus());
    }

    public class ExceptionFilter implements ContainerRequestFilter {

        @Override
        public void filter(final ContainerRequestContext context) throws IOException {
            throw new IOException("test");
        }
    }

    @Test
    public void testFilterExceptionHandling() throws Exception {

        final List<ContainerRequestFilter> requestFilterList = Lists.newArrayList();
        requestFilterList.add(new ExceptionFilter());

        final ResourceConfig resourceConfig = new ResourceConfig()
                .register(new ProviderInstanceBindingBinder<>(requestFilterList, ContainerRequestFilter.class));

        final Resource.Builder rb = Resource.builder("test");
        rb.addMethod("GET").handledBy(new Inflector<ContainerRequestContext, Response>() {

            @Override
            public Response apply(final ContainerRequestContext request) {
                return Response.ok().build();
            }
        });
        resourceConfig.registerResources(rb.build());
        final ApplicationHandler application = new ApplicationHandler(resourceConfig);
        try {
            application.apply(RequestContextBuilder.from("/test", "GET").build()).get().getStatus();
            Assert.fail("should throw an exception");
        } catch (final Exception e) {
            // ok
        }
    }
}
