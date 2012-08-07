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

import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

import javax.ws.rs.*;
import javax.ws.rs.container.*;
import javax.ws.rs.core.*;
import javax.ws.rs.ext.*;

import javax.inject.*;

import org.glassfish.jersey.internal.inject.*;
import org.glassfish.jersey.internal.inject.Providers;
import org.glassfish.jersey.message.internal.*;

import org.glassfish.hk2.api.*;

import org.junit.Test;
import static org.junit.Assert.*;

import com.google.common.base.*;
import com.google.common.collect.*;

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

    private static org.glassfish.hk2.utilities.Binder[] initBinders(org.glassfish.hk2.utilities.Binder... binders) {
        List<org.glassfish.hk2.utilities.Binder> binderList = Lists.newArrayList(binders);

        binderList.add(new ContextInjectionResolver.Binder());
        binderList.add(new MessagingBinders.MessageBodyProviders());

        return binderList.toArray(new org.glassfish.hk2.utilities.Binder[binderList.size()]);
    }

    public ProviderBinderTest() {
        RuntimeDelegate.setInstance(new TestRuntimeDelegate());
    }

    @Test
    public void testServicesNotEmpty() {
        ServiceLocator locator = Injections.createLocator(initBinders());
        Set<MessageBodyReader> providers = Providers.getProviders(locator, MessageBodyReader.class);
        assertTrue(providers.size() > 0);
    }

    @Test
    public void testServicesMbr() {
        ServiceLocator locator = Injections.createLocator(initBinders());
        Set<MessageBodyReader> providers = Providers.getProviders(locator, MessageBodyReader.class);
        assertTrue(providers.size() > 0);
    }

    @Test
    public void testServicesMbw() {
        ServiceLocator locator = Injections.createLocator(initBinders());
        Set<MessageBodyWriter> providers = Providers.getProviders(locator, MessageBodyWriter.class);
        assertTrue(providers.size() > 0);
    }

    @Test
    public void testProvidersMbr() {
        ServiceLocator locator = Injections.createLocator(initBinders());
        ProviderBinder providerBinder = new ProviderBinder(locator);
        providerBinder.bindClasses(Sets.<Class<?>>newHashSet(MyProvider.class));
        Set<MessageBodyReader> providers = Providers.getCustomProviders(locator, MessageBodyReader.class);
        assertEquals(1, instancesOfType(MyProvider.class, providers).size());
    }

    @Test
    public void testProvidersMbw() {
        ServiceLocator locator = Injections.createLocator(initBinders());
        ProviderBinder providerBinder = new ProviderBinder(locator);
        providerBinder.bindClasses(Sets.<Class<?>>newHashSet(MyProvider.class));

        Set<MessageBodyWriter> providers = Providers.getCustomProviders(locator, MessageBodyWriter.class);
        final Collection<MyProvider> myProviders = instancesOfType(MyProvider.class, providers);
        assertEquals(1, myProviders.size());
    }

    @Test
    public void testProvidersMbrInstance() {
        ServiceLocator locator = Injections.createLocator(initBinders());
        ProviderBinder providerBinder = new ProviderBinder(locator);
        providerBinder.bindInstances(Sets.<Object>newHashSet(new MyProvider()));
        Set<MessageBodyReader> providers = Providers.getCustomProviders(locator, MessageBodyReader.class);
        assertEquals(1, instancesOfType(MyProvider.class, providers).size());
    }

    @Test
    public void testProvidersMbwInstance() {
        ServiceLocator locator = Injections.createLocator(initBinders());
        ProviderBinder providerBinder = new ProviderBinder(locator);
        providerBinder.bindInstances(Sets.newHashSet((Object) new MyProvider()));

        Set<MessageBodyWriter> providers = Providers.getCustomProviders(locator, MessageBodyWriter.class);
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
        ServiceLocator locator = Injections.createLocator();

        ProviderBinder providerBinder = new ProviderBinder(locator);
        providerBinder.bindClasses(Child.class);
        providerBinder.bindClasses(NotFilterChild.class);

        ContainerRequestFilter requestFilter = getRequestFilter(locator);
        ContainerRequestFilter requestFilter2 = getRequestFilter(locator);
        Assert.assertEquals(requestFilter, requestFilter2);


        ContainerResponseFilter responseFilter = getResponseFilter(locator);
        ContainerResponseFilter responseFilter2 = getResponseFilter(locator);
        Assert.assertTrue(responseFilter == responseFilter2);

        Assert.assertTrue(responseFilter == requestFilter);

        // only one filter should be registered
        Collection<ContainerResponseFilter> filters = Providers.getCustomProviders(locator, ContainerResponseFilter.class);
        Assert.assertEquals(1, filters.size());

        Child child = locator.getService(Child.class);
        Child child2 = locator.getService(Child.class);

        Assert.assertTrue(child != responseFilter);

        Assert.assertTrue(child == child2);
    }

    private ContainerResponseFilter getResponseFilter(ServiceLocator locator) {
        ContainerResponseFilter responseFilter = locator.getService(ContainerResponseFilter.class, new CustomAnnotationImpl());
        Assert.assertEquals(Child.class, responseFilter.getClass());
        return responseFilter;
    }

    private ContainerRequestFilter getRequestFilter(ServiceLocator locator) {
        ContainerRequestFilter requestFilter = locator.getService(ContainerRequestFilter.class, new CustomAnnotationImpl());
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
