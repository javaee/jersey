/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015-2016 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.server.internal.inject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;

import org.glassfish.jersey.internal.util.collection.Ref;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.RequestContextBuilder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.internal.process.RequestProcessingContext;

import org.glassfish.hk2.api.DescriptorType;
import org.glassfish.hk2.api.DescriptorVisibility;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.TypeLiteral;
import org.glassfish.hk2.utilities.AbstractActiveDescriptor;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

import org.jvnet.hk2.internal.ServiceHandleImpl;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Make sure i can bind an active descriptor into application service locator
 * to get better control over types of instances that are being injected.
 * I should be able to inject different types based on scope of the injected component.
 *
 * This is an implementation sketch for complex use cases like the one from JERSEY-2855.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public class ActiveDescriptorBindingTest extends AbstractTest {

    public static final String X_COUNTER_HEADER = "X-COUNTER";

    public static final String REQUEST_TAG = "REQUEST";
    public static final String SINGLETON_TAG = "SINGLETON";
    public static final String PROXY_TAG = "PROXY";

    private static AtomicInteger counter = new AtomicInteger(1);

    /**
     * Both injected types will implement this interface.
     */
    public interface MyRequestData {
        String getReqInfo();
    }

    /**
     * This will get injected into request scoped components and parameters,
     * where injection happens for every and each request, so that
     * injected instance can keep request scoped data directly.
     */
    public static class MyRequestDataDirect implements MyRequestData {

        final String s;

        /**
         * Create new data bean.
         *
         * @param s request scoped data.
         */
        public MyRequestDataDirect(String s) {
            this.s = s;
        }

        @Override
        public String getReqInfo() {
            return s;
        }
    }

    @Path("req")
    public static class ReqResource {

        @Context MyRequestData field;

        @GET
        public String getReq() {
            return REQUEST_TAG + field.getReqInfo();
        }

        @GET
        @Path("param")
        public String getParam(@Context MyRequestData param) {
            return REQUEST_TAG + param.getReqInfo();
        }
    }

    @Path("singleton")
    @Singleton
    public static class SingletonResource {

        @Context MyRequestData field;

        @GET
        public String getReq() {
            return SINGLETON_TAG + field.getReqInfo();
        }

        @GET
        @Path("param")
        public String getParam(@Context MyRequestData param) {
            return SINGLETON_TAG + param.getReqInfo();
        }
    }

    @Test
    public void testReq() throws Exception {

        // bootstrap the test application
        ResourceConfig myConfig = new ResourceConfig();
        myConfig.register(ReqResource.class);
        myConfig.register(SingletonResource.class);
        final MyRequestDataDescriptor activeDescriptor = new MyRequestDataDescriptor();
        myConfig.register(new AbstractBinder() {
            @Override
            protected void configure() {
                addActiveDescriptor(activeDescriptor);
            }

        });
        initiateWebApplication(myConfig);
        activeDescriptor.locator = app().getServiceLocator();
        // end bootstrap

        String response;

        response = getResponseEntity("/req");
        assertThat(response, containsString(REQUEST_TAG));
        assertThat(response, not(containsString(PROXY_TAG)));

        response = getResponseEntity("/req/param");
        assertThat(response, containsString(REQUEST_TAG));
        assertThat(response, not(containsString(PROXY_TAG)));

        response = getResponseEntity("/singleton");
        assertThat(response, containsString(SINGLETON_TAG));
        assertThat(response, containsString(PROXY_TAG));

        response = getResponseEntity("/req");
        assertThat(response, containsString(REQUEST_TAG));
        assertThat(response, not(containsString(PROXY_TAG)));

        response = getResponseEntity("/singleton");
        assertThat(response, containsString(SINGLETON_TAG));
        assertThat(response, containsString(PROXY_TAG));

        response = getResponseEntity("/singleton/param");
        assertThat(response, containsString(SINGLETON_TAG));
        assertThat(response, not(containsString(PROXY_TAG)));

        response = getResponseEntity("/singleton/param");
        assertThat(response, containsString(SINGLETON_TAG));
        assertThat(response, not(containsString(PROXY_TAG)));
    }

    /**
     * Return response body as obtained from the resource with given URI.
     * Make sure that the response contains unique data sent out in a request header.
     *
     * @param uri tested resource URI.
     * @return response body content.
     * @throws Exception if request/response data does not match or any other error occurs.
     */
    private String getResponseEntity(final String uri) throws Exception {

        final String counterValue = Integer.toString(counter.getAndIncrement());

        final String entity = (String) responseFrom(uri, counterValue).getEntity();
        assertThat(entity, containsString(counterValue));

        return entity;
    }

    private ContainerResponse responseFrom(final String uri, final String counterValue) throws Exception {
        return apply(RequestContextBuilder.from(uri, "GET").header(X_COUNTER_HEADER, counterValue).build());
    }

    /**
     * HK2 active descriptor that produces instances of two different types depending
     * on into which component it is actually injecting.
     */
    private static class MyRequestDataDescriptor extends AbstractActiveDescriptor<MyRequestData> {

        ServiceLocator locator;

        static Set<Type> advertisedContracts = new HashSet<Type>() {
            {
                add(MyRequestData.class);
            }
        };

        /**
         * Create a new custom descriptor.
         */
        public MyRequestDataDescriptor() {
            super(advertisedContracts,
                    PerLookup.class,
                    null, new HashSet<Annotation>(),
                    DescriptorType.CLASS, DescriptorVisibility.LOCAL,
                    0, null, null, null, null);
        }

        @Override
        public Class<?> getImplementationClass() {
            return MyRequestData.class;
        }

        @Override
        public Type getImplementationType() {
            return getImplementationClass();
        }

        @Override
        public MyRequestData create(ServiceHandle<?> serviceHandle) {

            boolean direct = false;

            final javax.inject.Provider<Ref<RequestProcessingContext>> ctxRef =
                    locator.getService(new TypeLiteral<javax.inject.Provider<Ref<RequestProcessingContext>>>() {
                                        }.getType());

            if (serviceHandle instanceof ServiceHandleImpl) {
                final ServiceHandleImpl serviceHandleImpl = (ServiceHandleImpl) serviceHandle;
                final Class<? extends Annotation> scopeAnnotation =
                        serviceHandleImpl.getOriginalRequest().getInjecteeDescriptor().getScopeAnnotation();

                if (scopeAnnotation == RequestScoped.class || scopeAnnotation == null) {
                    direct = true;
                }
            }

            return direct
                    ? new MyRequestDataDirect(ctxRef.get().get().request().getHeaderString(X_COUNTER_HEADER))
                    // in case of singleton, we need to make sure request scoped data are still accessible
                    : new MyRequestData() {
                        @Override
                        public String getReqInfo() {
                            return PROXY_TAG + ctxRef.get().get().request().getHeaderString(X_COUNTER_HEADER);
                        }
                };
        }

        @Override
        public synchronized String getImplementation() {
            return MyRequestData.class.getName();
        }
    }
}
