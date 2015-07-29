/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.server.internal.routing;

import java.util.concurrent.ExecutionException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.RequestContextBuilder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.model.ModelProcessor;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceModel;

import org.junit.Before;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Test that sub-resource locators (models and routers) are being cached.
 * <p/>
 * TODO: proxy (class) tests
 *
 * @author Michal Gajdos
 */
public class SubResourceLocatorCachingTest {

    private static final int INVOCATION_COUNT = 3;

    @Path("root")
    public static class RootResource {

        private static final Resource RESOURCE = createResource();

        private static Resource createResource() {
            final Resource.Builder builder = Resource.builder();
            builder.addMethod("GET").handledBy(new Inflector<ContainerRequestContext, Response>() {

                @Override
                public Response apply(final ContainerRequestContext containerRequestContext) {
                    return Response.ok("sub").build();
                }
            }).build();
            return builder.build();
        }

        @Path("class")
        public Class<?> locatorClass() {
            return SubResource.class;
        }

        @Path("concrete-class")
        public Class<SubResource> locatorConcreteClass() {
            return SubResource.class;
        }

        @Path("instance")
        public SubResource locatorInstance() {
            return new SubResource("sub");
        }

        @Path("resource-from-class")
        public Resource locatorResourceFromClass() {
            return Resource.from(SubResource.class);
        }

        @Path("resource-singleton")
        public Resource locatorResourceSingleton() {
            return RESOURCE;
        }

        @Path("resource")
        public Resource locatorResource() {
            return createResource();
        }

        @Path("object-class")
        public Object locatorObjectClass() {
            return SubResource.class;
        }

        @Path("object-instance")
        public Object locatorObjectInstance() {
            return new SubResource("sub");
        }
    }

    public static class SubResource {

        private final String message;

        public SubResource() {
            this("sub");
        }

        public SubResource(final String message) {
            this.message = message;
        }

        @GET
        public String sub() {
            return message;
        }
    }

    public static class CountingModelProcessor implements ModelProcessor {

        private volatile int resourceMehtodCalls = 0;
        private volatile int subresourceMehtodCalls = 0;

        @Override
        public ResourceModel processResourceModel(final ResourceModel resourceModel, final Configuration configuration) {
            resourceMehtodCalls++;
            return resourceModel;
        }

        @Override
        public ResourceModel processSubResource(final ResourceModel subResourceModel, final Configuration configuration) {
            subresourceMehtodCalls++;
            return subResourceModel;
        }
    }

    private CountingModelProcessor processor;
    private ResourceConfig config;

    @Before
    public void setUp() throws Exception {
        processor = new CountingModelProcessor();
        config = new ResourceConfig(RootResource.class).register(processor);
    }

    private void _test(final String requestUri) throws InterruptedException, ExecutionException {
        _test(requestUri, 1);
    }

    private void _test(final String requestUri, final int subResourceCalls) throws InterruptedException, ExecutionException {
        _test(requestUri, subResourceCalls, false);
    }

    private void _test(final String requestUri, final int subResourceCalls, final boolean sleep)
            throws InterruptedException, ExecutionException {

        final ApplicationHandler applicationHandler = new ApplicationHandler(config);

        ContainerResponse response = null;
        for (int i = 0; i < INVOCATION_COUNT; i++) {
            response = applicationHandler.apply(RequestContextBuilder.from(requestUri, "GET").build()).get();

            if (sleep) {
                Thread.sleep(1100);
            }
        }

        assertThat(response.getStatus(), is(200));
        assertThat((String) response.getEntity(), is("sub"));

        // Test how many times model processors method has been called.
        assertThat(processor.resourceMehtodCalls, is(1));
        assertThat(processor.subresourceMehtodCalls, is(subResourceCalls));
    }

    @Test
    public void testLocatorAsClass() throws Exception {
        _test("/root/class");
    }

    @Test
    public void testLocatorAsConcreteClass() throws Exception {
        _test("/root/class");
    }

    @Test
    public void testLocatorAsInstance() throws Exception {
        _test("/root/instance");
    }

    @Test
    public void testLocatorAsResourceFromClass() throws Exception {
        _test("/root/resource-from-class", INVOCATION_COUNT);
    }

    @Test
    public void testLocatorAsResourceSingletonCachingEnabled() throws Exception {
        config.property(ServerProperties.SUBRESOURCE_LOCATOR_CACHE_JERSEY_RESOURCE_ENABLED, true);

        _test("/root/resource-singleton");
    }

    @Test
    public void testLocatorAsResourceSingletonCachingDisabled() throws Exception {
        _test("/root/resource-singleton", INVOCATION_COUNT);
    }

    @Test
    public void testLocatorAsResource() throws Exception {
        _test("/root/resource", INVOCATION_COUNT);
    }

    @Test
    public void testLocatorAsClassObject() throws Exception {
        _test("/root/object-class");
    }

    @Test
    public void testLocatorAsInstanceObject() throws Exception {
        _test("/root/object-instance");
    }

    @Test
    public void testLocatorCacheAging() throws Exception {
        config.property(ServerProperties.SUBRESOURCE_LOCATOR_CACHE_AGE, 1);

        _test("/root/class", INVOCATION_COUNT, true);
    }

    @Test
    public void testLocatorCacheInvalidAging() throws Exception {
        config.property(ServerProperties.SUBRESOURCE_LOCATOR_CACHE_AGE, "foo");

        _test("/root/class", 1, true);
    }
}
