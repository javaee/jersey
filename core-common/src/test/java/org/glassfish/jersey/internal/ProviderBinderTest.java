/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.RuntimeDelegate;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.internal.inject.ContextInjectionResolver;
import org.glassfish.jersey.internal.inject.CustomAnnotationImpl;
import org.glassfish.jersey.internal.inject.Module;
import org.glassfish.jersey.internal.inject.Providers;
import org.glassfish.jersey.internal.inject.Utilities;
import org.glassfish.jersey.message.internal.MessagingModules;
import org.glassfish.jersey.message.internal.StringMessageProvider;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import junit.framework.Assert;

/**
 * ServiceProviders unit test.
 *
 * @author Santiago Pericas-Geertsen (santiago.pericasgeertsen at oracle.com)
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class ProviderBinderTest {

    private static class MyProvider implements MessageBodyReader, MessageBodyWriter {

        @Override
        public boolean isReadable(Class type, Type genericType, Annotation[] annotations,
                MediaType mediaType) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Object readFrom(Class type, Type genericType, Annotation[] annotations, MediaType mediaType,
                MultivaluedMap httpHeaders, InputStream entityStream)
                throws IOException, WebApplicationException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isWriteable(Class type, Type genericType, Annotation[] annotations,
                MediaType mediaType) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public long getSize(Object t, Class type, Type genericType, Annotation[] annotations,
                MediaType mediaType) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void writeTo(Object t, Class type, Type genericType, Annotation[] annotations,
                MediaType mediaType, MultivaluedMap httpHeaders, OutputStream entityStream)
                throws IOException, WebApplicationException {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    private static Module[] initModules(Module... modules) {
        List<Module> moduleList = Lists.newArrayList(modules);

        moduleList.add(new ContextInjectionResolver.Module());
        moduleList.add(new ProviderBinder.ProviderBinderModule());
        moduleList.add(new MessagingModules.MessageBodyProviders());

        return moduleList.toArray(new Module[moduleList.size()]);
    }

    public ProviderBinderTest() {
        RuntimeDelegate.setInstance(new TestRuntimeDelegate());
    }

    @Test
    public void testServicesNotEmpty() {
        ServiceLocator services = Utilities.create(null, null, initModules());
        Set<MessageBodyReader> providers = Providers.getProviders(services, MessageBodyReader.class);
        assertTrue(providers.size() > 0);
    }

    @Test
    public void testServicesMbr() {
        ServiceLocator services = Utilities.create(null, null, initModules());
        Set<MessageBodyReader> providers = Providers.getProviders(services, MessageBodyReader.class);
        assertEquals(1, instancesOfType(StringMessageProvider.class, providers).size());
    }

    @Test
    public void testServicesMbw() {
        ServiceLocator services = Utilities.create(null, null, initModules());
        Set<MessageBodyWriter> providers = Providers.getProviders(services, MessageBodyWriter.class);
        assertEquals(1, instancesOfType(StringMessageProvider.class, providers).size());
    }

    @Test
    public void testProvidersMbr() {
        ServiceLocator services = Utilities.create(null, null, initModules());
        ProviderBinder providerBinder = services.getService(ProviderBinder.class);
        providerBinder.bindClasses(Sets.<Class<?>>newHashSet(MyProvider.class));
        Set<MessageBodyReader> providers = Providers.getCustomProviders(services, MessageBodyReader.class);
        assertEquals(1, instancesOfType(MyProvider.class, providers).size());
    }

    @Test
    public void testProvidersMbw() {
        ServiceLocator services = Utilities.create(null, null, initModules());
        ProviderBinder providerBinder = services.getService(ProviderBinder.class);
        providerBinder.bindClasses(Sets.<Class<?>>newHashSet(MyProvider.class));

        Set<MessageBodyWriter> providers = Providers.getCustomProviders(services, MessageBodyWriter.class);
        final Collection<MyProvider> myProviders = instancesOfType(MyProvider.class, providers);
        assertEquals(1, myProviders.size());
    }

    @Test
    public void testProvidersMbrInstance() {
        ServiceLocator services = Utilities.create(null, null, initModules());
        ProviderBinder providerBinder = services.getService(ProviderBinder.class);
        providerBinder.bindInstances(Sets.<Object>newHashSet(new MyProvider()));
        Set<MessageBodyReader> providers = Providers.getCustomProviders(services, MessageBodyReader.class);
        assertEquals(1, instancesOfType(MyProvider.class, providers).size());
    }

    @Test
    public void testProvidersMbwInstance() {
        ServiceLocator services = Utilities.create(null, null, initModules());
        ProviderBinder providerBinder = services.getService(ProviderBinder.class);
        providerBinder.bindInstances(Sets.newHashSet((Object) new MyProvider()));

        Set<MessageBodyWriter> providers = Providers.getCustomProviders(services,MessageBodyWriter.class);
        assertEquals(instancesOfType(MyProvider.class, providers).size(), 1);
    }

    private <T> Collection<T> instancesOfType(final Class<T> c, Collection<?> collection) {
        return Collections2.transform(Collections2.filter(collection, new Predicate<Object>() {

            @Override
            public boolean apply(Object input) {
                return input.getClass() == c;
            }
        }), new Function<Object, T>() {

            @Override
            public T apply(Object input) {
                return c.cast(input);
            }
        });
    }


    @Test
    public void testCustomRegistration() {
        ServiceLocator services = Utilities.create(null, null, new ProviderBinder.ProviderBinderModule());

        ProviderBinder providerBinder = services.getService(ProviderBinder.class);
        providerBinder.bindClasses(Child.class);
        providerBinder.bindClasses(NotFilterChild.class);

        ContainerRequestFilter requestFilter = getRequestFilter(services);
        ContainerRequestFilter requestFilter2 = getRequestFilter(services);
        Assert.assertEquals(requestFilter, requestFilter2);


        ContainerResponseFilter responseFilter = getResponseFilter(services);
        ContainerResponseFilter responseFilter2 = getResponseFilter(services);
        Assert.assertTrue(responseFilter == responseFilter2);

        Assert.assertTrue(responseFilter == requestFilter);

        // only one filter should be registered
        Collection<ContainerResponseFilter> filters = Providers.getCustomProviders(services, ContainerResponseFilter.class);
        Assert.assertEquals(1, filters.size());

        Child child = services.getService(Child.class);
        Child child2 = services.getService(Child.class);

        Assert.assertTrue(child != responseFilter);

        Assert.assertTrue(child == child2);
    }

    private ContainerResponseFilter getResponseFilter(ServiceLocator services) {
        ContainerResponseFilter responseFilter = services.getService(ContainerResponseFilter.class, new CustomAnnotationImpl());
        Assert.assertEquals(Child.class, responseFilter.getClass());
        return responseFilter;
    }

    private ContainerRequestFilter getRequestFilter(ServiceLocator services) {
        ContainerRequestFilter requestFilter = services.getService(ContainerRequestFilter.class, new CustomAnnotationImpl());
        Assert.assertEquals(Child.class, requestFilter.getClass());
        return requestFilter;
    }

    public static interface ParentInterface {
    }

    public static interface ChildInterface extends ChildSuperInterface {
    }


    public static interface SecondChildInterface {
    }

    public static interface ChildSuperInterface extends ContainerResponseFilter {
    }

    @Singleton
    public static class Parent implements ParentInterface, ContainerRequestFilter {
        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
        }
    }

    @Singleton
    public static class Child extends Parent implements ChildInterface, SecondChildInterface {
        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        }
    }

    public static class NotFilterChild implements ParentInterface {
    }

    public static interface SingletonTest {
        public int getCount();
    }


    public static interface SingletonTestStr {
        public int getCountStr();
    }

    public static class SingletonClass implements SingletonTest, SingletonTestStr {
        private int counter = 1;

        @Override
        public int getCount() {
            return counter++;
        }

        @Override
        public int getCountStr() {
            return counter++;
        }
    }
}
