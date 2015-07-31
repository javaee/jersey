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
package org.glassfish.jersey.model.internal;

import java.io.IOException;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.Priorities;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import javax.annotation.Priority;
import javax.inject.Inject;

import org.glassfish.jersey.internal.inject.Injections;
import org.glassfish.jersey.internal.inject.ProviderBinder;
import org.glassfish.jersey.internal.inject.Providers;
import org.glassfish.jersey.model.ContractProvider;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import jersey.repackaged.com.google.common.collect.Maps;

/**
 * Test cases for {@link javax.ws.rs.core.Configuration}.
 *
 * @author Michal Gajdos
 */
public class CommonConfigTest {

    private CommonConfig config;

    @Before
    public void setUp() throws Exception {
        config = new CommonConfig(null, ComponentBag.INCLUDE_ALL);
    }

    @Test
    public void testGetProperties() throws Exception {
        try {
            config.getConfiguration().getProperties().put("foo", "bar");
            fail("Returned properties collection should be immutable.");
        } catch (final Exception e) {
            // OK.
        }
    }

    @Test
    public void testSetProperties() throws Exception {
        config = config.property("foo", "bar");
        assertEquals("bar", config.getConfiguration().getProperty("foo"));

        final Map<String, String> properties = Maps.newHashMap();
        properties.put("hello", "world");
        config = config.setProperties(properties);

        assertEquals(1, config.getConfiguration().getProperties().size());
        assertEquals("world", config.getConfiguration().getProperty("hello"));

        properties.put("one", "two");
        assertEquals(1, config.getConfiguration().getProperties().size());
        assertNull(config.getConfiguration().getProperty("one"));

        config = config.setProperties(Maps.<String, String>newHashMap());
        assertTrue(config.getConfiguration().getProperties().isEmpty());
    }

    @Test
    public void testSetGetProperty() throws Exception {
        config = config.property("foo", "bar");
        assertEquals("bar", config.getConfiguration().getProperty("foo"));

        config.property("hello", "world");
        config.property("foo", null);

        assertEquals(null, config.getConfiguration().getProperty("foo"));
        assertEquals(1, config.getConfiguration().getProperties().size());
    }

    public static class EmptyFeature implements Feature {

        @Override
        public boolean configure(final FeatureContext configuration) {
            return true;
        }
    }

    public static class UnconfigurableFeature implements Feature {

        @Override
        public boolean configure(final FeatureContext configuration) {
            return false;
        }
    }

    public static class ComplexEmptyProvider implements ReaderInterceptor, ContainerRequestFilter, ExceptionMapper {

        @Override
        public void filter(final ContainerRequestContext requestContext) throws IOException {
            // Do nothing.
        }

        @Override
        public Object aroundReadFrom(final ReaderInterceptorContext context) throws IOException, WebApplicationException {
            return context.proceed();
        }

        @Override
        public Response toResponse(final Throwable exception) {
            throw new UnsupportedOperationException();
        }
    }

    public static class ComplexEmptyProviderFeature extends ComplexEmptyProvider implements Feature {

        @Override
        public boolean configure(final FeatureContext configuration) {
            throw new UnsupportedOperationException();
        }
    }

    @Test
    public void testReplaceWith() throws Exception {
        config.property("foo", "bar");
        final EmptyFeature emptyFeature = new EmptyFeature();
        config.register(emptyFeature);
        config.register(ComplexEmptyProvider.class, ExceptionMapper.class);

        final CommonConfig other = new CommonConfig(null, ComponentBag.INCLUDE_ALL);
        other.property("foo", "baz");
        other.register(UnconfigurableFeature.class);
        other.register(ComplexEmptyProvider.class, ReaderInterceptor.class, ContainerRequestFilter.class);

        assertEquals("baz", other.getProperty("foo"));
        assertEquals(1, other.getProperties().size());
        assertEquals(2, other.getClasses().size());
        assertEquals(0, other.getInstances().size());
        assertEquals(2, other.getContracts(ComplexEmptyProvider.class).size());
        assertTrue(other.getContracts(ComplexEmptyProvider.class).containsKey(ReaderInterceptor.class));
        assertTrue(other.getContracts(ComplexEmptyProvider.class).containsKey(ContainerRequestFilter.class));

        other.loadFrom(config);

        assertEquals("bar", other.getProperty("foo"));
        assertEquals(1, other.getProperties().size());
        assertEquals(1, other.getClasses().size());
        assertEquals(1, other.getInstances().size());
        assertEquals(1, other.getContracts(ComplexEmptyProvider.class).size());
        assertSame(ExceptionMapper.class, other.getContracts(ComplexEmptyProvider.class).keySet().iterator().next());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetFeatures() throws Exception {
        final EmptyFeature emptyFeature = new EmptyFeature();
        final UnconfigurableFeature unconfigurableFeature = new UnconfigurableFeature();
        final ComplexEmptyProviderFeature providerFeature = new ComplexEmptyProviderFeature();

        config.register(emptyFeature);
        config.register(unconfigurableFeature);
        config.register(providerFeature, ReaderInterceptor.class);

        assertFalse(config.getConfiguration().isEnabled(emptyFeature));
        assertFalse(config.getConfiguration().isEnabled(unconfigurableFeature));
        assertFalse(config.getConfiguration().isEnabled(providerFeature));

        assertTrue(config.getConfiguration().isRegistered(emptyFeature));
        assertTrue(config.getConfiguration().isRegistered(unconfigurableFeature));
        assertTrue(config.getConfiguration().isRegistered(providerFeature));
    }


    @Test
    // Regression test for JERSEY-1638.
    public void testGetNonExistentProviderContractsASEmptyMap() throws Exception {
        assertTrue(config.getConfiguration().getContracts(CommonConfigTest.class).isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetClasses() throws Exception {
        _testCollectionsCommon("GetProviderClasses", config.getClasses(), EmptyFeature.class);

        config.register(ComplexEmptyProviderFeature.class,
                WriterInterceptor.class, ReaderInterceptor.class, ContainerRequestFilter.class);
        assertEquals(1, config.getClasses().size());

        config.register(EmptyFeature.class);

        final Set<Class<?>> providerClasses = config.getClasses();
        assertEquals(2, providerClasses.size());
        assertTrue(providerClasses.contains(ComplexEmptyProviderFeature.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetInstances() throws Exception {
        _testCollectionsCommon("GetProviderInstances", config.getInstances(), new EmptyFeature());

        final ComplexEmptyProviderFeature providerFeature1 = new ComplexEmptyProviderFeature();
        config.register(providerFeature1, WriterInterceptor.class);
        assertEquals(1, config.getInstances().size());
        assertTrue(config.getInstances().contains(providerFeature1));

        final EmptyFeature emptyFeature = new EmptyFeature();
        config.register(emptyFeature);
        assertEquals(2, config.getInstances().size());
        assertTrue(config.getInstances().contains(emptyFeature));

        final ComplexEmptyProviderFeature providerFeature2 = new ComplexEmptyProviderFeature();
        config.register(providerFeature2, ReaderInterceptor.class, ContainerRequestFilter.class);
        assertEquals(2, config.getInstances().size());
        assertFalse(config.getInstances().contains(providerFeature2));
    }

    @Test
    public void testRegisterClass() throws Exception {
        try {
            final Class clazz = null;
            //noinspection ConstantConditions
            config.register(clazz);
            fail("Cannot register null.");
        } catch (final IllegalArgumentException e) {
            // OK.
        }

        for (int i = 0; i < 2; i++) {
            config.register(ComplexEmptyProvider.class);
        }

        final ContractProvider contractProvider =
                config.getComponentBag().getModel(ComplexEmptyProvider.class);
        final Set<Class<?>> contracts = contractProvider.getContracts();

        assertEquals(3, contracts.size());
        assertTrue(contracts.contains(ReaderInterceptor.class));
        assertTrue(contracts.contains(ContainerRequestFilter.class));
        assertTrue(contracts.contains(ExceptionMapper.class));

        assertTrue(config.isRegistered(ComplexEmptyProvider.class));
    }

    @Test
    public void testRegisterInstance() throws Exception {
        try {
            config.register(null);
            fail("Cannot register null.");
        } catch (final IllegalArgumentException e) {
            // OK.
        }

        final ComplexEmptyProvider[] ceps = new ComplexEmptyProvider[2];
        for (int i = 0; i < 2; i++) {
            ceps[i] = new ComplexEmptyProvider();
            config.register(ceps[i]);
        }

        final ContractProvider contractProvider =
                config.getComponentBag().getModel(ComplexEmptyProvider.class);
        final Set<Class<?>> contracts = contractProvider.getContracts();

        assertEquals(3, contracts.size());
        assertTrue(contracts.contains(ReaderInterceptor.class));
        assertTrue(contracts.contains(ContainerRequestFilter.class));
        assertTrue(contracts.contains(ExceptionMapper.class));

        assertTrue(config.isRegistered(ComplexEmptyProvider.class));
        assertTrue(config.isRegistered(ceps[0]));
        assertFalse(config.isRegistered(ceps[1]));
    }

    @Test
    public void testRegisterClassInstanceClash() throws Exception {
        final ComplexEmptyProvider complexEmptyProvider = new ComplexEmptyProvider();

        config.register(ComplexEmptyProvider.class);
        config.register(complexEmptyProvider);
        config.register(ComplexEmptyProvider.class);

        assertTrue(config.getClasses().contains(ComplexEmptyProvider.class));
        assertFalse(config.getInstances().contains(complexEmptyProvider));

        final ContractProvider contractProvider =
                config.getComponentBag().getModel(ComplexEmptyProvider.class);
        final Set<Class<?>> contracts = contractProvider.getContracts();

        assertEquals(3, contracts.size());
        assertTrue(contracts.contains(ReaderInterceptor.class));
        assertTrue(contracts.contains(ContainerRequestFilter.class));
        assertTrue(contracts.contains(ExceptionMapper.class));
    }

    @Test
    public void testRegisterClassBingingPriority() throws Exception {
        try {
            final Class clazz = null;
            //noinspection ConstantConditions
            config.register(clazz, Priorities.USER);
            fail("Cannot register null.");
        } catch (final IllegalArgumentException e) {
            // OK.
        }

        for (final int priority : new int[]{Priorities.USER, Priorities.AUTHENTICATION}) {
            config.register(ComplexEmptyProvider.class, priority);

            final ContractProvider contractProvider =
                    config.getComponentBag().getModel(ComplexEmptyProvider.class);
            final Set<Class<?>> contracts = contractProvider.getContracts();

            assertEquals(3, contracts.size());
            assertTrue(contracts.contains(ReaderInterceptor.class));
            assertTrue(contracts.contains(ContainerRequestFilter.class));
            assertTrue(contracts.contains(ExceptionMapper.class));

            // All priorities are the same.
            assertEquals(Priorities.USER, contractProvider.getPriority(ReaderInterceptor.class));
            assertEquals(Priorities.USER, contractProvider.getPriority(ContainerRequestFilter.class));
            assertEquals(Priorities.USER, contractProvider.getPriority(ExceptionMapper.class));
        }
    }

    @Test
    public void testRegisterInstanceBingingPriority() throws Exception {
        try {
            config.register(null, Priorities.USER);
            fail("Cannot register null.");
        } catch (final IllegalArgumentException e) {
            // OK.
        }

        final Class<ComplexEmptyProvider> providerClass = ComplexEmptyProvider.class;

        for (final int priority : new int[]{Priorities.USER, Priorities.AUTHENTICATION}) {
            config.register(providerClass, priority);

            final CommonConfig commonConfig = config;
            final ContractProvider contractProvider =
                    commonConfig.getComponentBag().getModel(providerClass);
            final Set<Class<?>> contracts = contractProvider.getContracts();

            assertEquals(3, contracts.size()); // Feature is not there.
            assertTrue(contracts.contains(ReaderInterceptor.class));
            assertTrue(contracts.contains(ContainerRequestFilter.class));
            assertTrue(contracts.contains(ExceptionMapper.class));

            // All priorities are the same.
            assertEquals(Priorities.USER, contractProvider.getPriority(ReaderInterceptor.class));
            assertEquals(Priorities.USER, contractProvider.getPriority(ContainerRequestFilter.class));
            assertEquals(Priorities.USER, contractProvider.getPriority(ExceptionMapper.class));
        }
    }

    @Test
    public void testRegisterClassInstanceBindingPriorityClash() throws Exception {
        final ComplexEmptyProvider complexEmptyProvider = new ComplexEmptyProvider();

        config.register(ComplexEmptyProvider.class, Priorities.AUTHENTICATION);
        config.register(complexEmptyProvider, Priorities.USER);


        assertTrue(config.getClasses().contains(ComplexEmptyProvider.class));
        assertFalse(config.getInstances().contains(complexEmptyProvider));

        final ComponentBag componentBag = config.getComponentBag();
        final ContractProvider contractProvider =
                componentBag.getModel(ComplexEmptyProvider.class);
        final Set<Class<?>> contracts = contractProvider.getContracts();

        assertEquals(3, contracts.size()); // Feature is not there.
        assertTrue(contracts.contains(ReaderInterceptor.class));
        assertTrue(contracts.contains(ContainerRequestFilter.class));
        assertTrue(contracts.contains(ExceptionMapper.class));

        // All priorities are the same.
        assertEquals(Priorities.AUTHENTICATION, contractProvider.getPriority(ReaderInterceptor.class));
        assertEquals(Priorities.AUTHENTICATION, contractProvider.getPriority(ContainerRequestFilter.class));
        assertEquals(Priorities.AUTHENTICATION, contractProvider.getPriority(ExceptionMapper.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRegisterClassContracts() throws Exception {
        try {
            final Class clazz = null;
            //noinspection ConstantConditions
            config.register(clazz, ReaderInterceptor.class);
            fail("Cannot register null.");
        } catch (final IllegalArgumentException e) {
            // OK.
        }

        config.register(ComplexEmptyProvider.class,
                ReaderInterceptor.class, ContainerRequestFilter.class, WriterInterceptor.class);
        final ContractProvider contractProvider = config.getComponentBag().getModel(ComplexEmptyProvider.class);
        final Set<Class<?>> contracts = contractProvider.getContracts();
        assertEquals(2, contracts.size());
        assertTrue(ReaderInterceptor.class + " is not registered.", contracts.contains(ReaderInterceptor.class));
        assertTrue(ContainerRequestFilter.class + " is not registered.", contracts.contains(ContainerRequestFilter.class));
        assertFalse(WriterInterceptor.class + " should not be registered.", contracts.contains(WriterInterceptor.class));

        assertTrue(config.getInstances().isEmpty());
        assertTrue(config.getClasses().contains(ComplexEmptyProvider.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRegisterInstancesContracts() throws Exception {
        try {
            config.register(null, ReaderInterceptor.class);
            fail("Cannot register null.");
        } catch (final IllegalArgumentException e) {
            // OK.
        }

        final ComplexEmptyProvider complexEmptyProvider = new ComplexEmptyProvider();
        config.register(complexEmptyProvider,
                ReaderInterceptor.class, ContainerRequestFilter.class, WriterInterceptor.class);
        final ContractProvider contractProvider = config.getComponentBag().getModel(ComplexEmptyProvider.class);
        final Set<Class<?>> contracts = contractProvider.getContracts();
        assertEquals(2, contracts.size());
        assertTrue(ReaderInterceptor.class + " is not registered.", contracts.contains(ReaderInterceptor.class));
        assertTrue(ContainerRequestFilter.class + " is not registered.", contracts.contains(ContainerRequestFilter.class));
        assertFalse(WriterInterceptor.class + " should not be registered.", contracts.contains(WriterInterceptor.class));

        assertTrue(config.getInstances().contains(complexEmptyProvider));
        assertTrue(config.getClasses().isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRegisterClassContractsFeatureNotInvoked() throws Exception {
        config.register(ComplexEmptyProviderFeature.class, ReaderInterceptor.class);
        assertFalse(config.getConfiguration().isEnabled(ComplexEmptyProviderFeature.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRegisterInstancesContractsFeatureNotInvoked() throws Exception {
        final ComplexEmptyProviderFeature feature = new ComplexEmptyProviderFeature();
        config.register(feature, ReaderInterceptor.class);
        assertFalse(config.getConfiguration().isEnabled(ComplexEmptyProviderFeature.class));
        assertFalse(config.getConfiguration().isEnabled(feature));
    }

    @Test
    public void testRegisterClassNullContracts() throws Exception {
        config.register(ComplexEmptyProvider.class, (Class) null);

        final ContractProvider contractProvider =
                config.getComponentBag().getModel(ComplexEmptyProvider.class);
        final Set<Class<?>> contracts = contractProvider.getContracts();

        assertEquals(0, contracts.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRegisterInstanceNullContracts() throws Exception {
        config.register(new ComplexEmptyProvider(), (Class) null);

        final ContractProvider contractProvider =
                config.getComponentBag().getModel(ComplexEmptyProvider.class);
        final Set<Class<?>> contracts = contractProvider.getContracts();

        assertEquals(0, contracts.size());
    }

    // Reproducer JERSEY-1637
    @Test
    public void testRegisterNullOrEmptyContracts() {
        final ComplexEmptyProvider provider = new ComplexEmptyProvider();

        config.register(ComplexEmptyProvider.class,  (Class<?>[]) null);
        assertFalse(config.getConfiguration().isRegistered(ComplexEmptyProvider.class));

        config.register(provider,  (Class<?>[]) null);
        assertFalse(config.getConfiguration().isRegistered(ComplexEmptyProvider.class));
        assertFalse(config.getConfiguration().isRegistered(provider));

        config.register(ComplexEmptyProvider.class,  new Class[0]);
        assertFalse(config.getConfiguration().isRegistered(ComplexEmptyProvider.class));

        config.register(provider,  new Class[0]);
        assertFalse(config.getConfiguration().isRegistered(ComplexEmptyProvider.class));
        assertFalse(config.getConfiguration().isRegistered(provider));
    }

    @Priority(300)
    public static class LowPriorityProvider implements WriterInterceptor, ReaderInterceptor {

        @Override
        public void aroundWriteTo(final WriterInterceptorContext context) throws IOException, WebApplicationException {
            // Do nothing.
        }

        @Override
        public Object aroundReadFrom(final ReaderInterceptorContext context) throws IOException, WebApplicationException {
            return context.proceed();
        }
    }

    @Priority(200)
    public static class MidPriorityProvider implements WriterInterceptor, ReaderInterceptor {

        @Override
        public void aroundWriteTo(final WriterInterceptorContext context) throws IOException, WebApplicationException {
            // Do nothing.
        }

        @Override
        public Object aroundReadFrom(final ReaderInterceptorContext context) throws IOException, WebApplicationException {
            return context.proceed();
        }
    }

    @Priority(100)
    public static class HighPriorityProvider implements WriterInterceptor, ReaderInterceptor {

        @Override
        public void aroundWriteTo(final WriterInterceptorContext context) throws IOException, WebApplicationException {
            // Do nothing.
        }

        @Override
        public Object aroundReadFrom(final ReaderInterceptorContext context) throws IOException, WebApplicationException {
            return context.proceed();
        }
    }

    @Test
    public void testProviderOrderManual() throws Exception {
        final ServiceLocator locator = Injections.createLocator();

        config.register(MidPriorityProvider.class, 500);
        config.register(LowPriorityProvider.class, 20);
        config.register(HighPriorityProvider.class, 150);

        ProviderBinder.bindProviders(config.getComponentBag(), locator);
        final Iterable<WriterInterceptor> allProviders =
                Providers.getAllProviders(locator, WriterInterceptor.class, new RankedComparator<WriterInterceptor>());

        final Iterator<WriterInterceptor> iterator = allProviders.iterator();

        assertEquals(LowPriorityProvider.class, iterator.next().getClass());
        assertEquals(HighPriorityProvider.class, iterator.next().getClass());
        assertEquals(MidPriorityProvider.class, iterator.next().getClass());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testProviderOrderSemiAutomatic() throws Exception {
        final ServiceLocator locator = Injections.createLocator();

        config.register(MidPriorityProvider.class, 50);
        config.register(LowPriorityProvider.class, 2000);
        config.register(HighPriorityProvider.class);

        ProviderBinder.bindProviders(config.getComponentBag(), locator);
        final Iterable<WriterInterceptor> allProviders =
                Providers.getAllProviders(locator, WriterInterceptor.class, new RankedComparator<WriterInterceptor>());

        final Iterator<WriterInterceptor> iterator = allProviders.iterator();

        assertEquals(MidPriorityProvider.class, iterator.next().getClass());
        assertEquals(HighPriorityProvider.class, iterator.next().getClass());
        assertEquals(LowPriorityProvider.class, iterator.next().getClass());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testProviderOrderAutomatic() throws Exception {
        final ServiceLocator locator = Injections.createLocator();

        config.register(MidPriorityProvider.class);
        config.register(LowPriorityProvider.class);
        config.register(HighPriorityProvider.class);

        ProviderBinder.bindProviders(config.getComponentBag(), locator);
        final Iterable<WriterInterceptor> allProviders =
                Providers.getAllProviders(locator, WriterInterceptor.class, new RankedComparator<WriterInterceptor>());

        final Iterator<WriterInterceptor> iterator = allProviders.iterator();

        assertEquals(HighPriorityProvider.class, iterator.next().getClass());
        assertEquals(MidPriorityProvider.class, iterator.next().getClass());
        assertEquals(LowPriorityProvider.class, iterator.next().getClass());
        assertFalse(iterator.hasNext());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testProviderOrderDifForContracts() throws Exception {
        final Map<Class<?>, Integer> contracts = new IdentityHashMap<Class<?>, Integer>();

        contracts.put(WriterInterceptor.class, ContractProvider.NO_PRIORITY);
        contracts.put(ReaderInterceptor.class, 2000);
        config.register(MidPriorityProvider.class, contracts);
        contracts.clear();

        contracts.put(WriterInterceptor.class, ContractProvider.NO_PRIORITY);
        contracts.put(ReaderInterceptor.class, 1000);
        config.register(LowPriorityProvider.class, contracts);
        contracts.clear();

        contracts.put(WriterInterceptor.class, ContractProvider.NO_PRIORITY);
        contracts.put(ReaderInterceptor.class, 3000);
        config.register(HighPriorityProvider.class, contracts);
        contracts.clear();

        final ServiceLocator locator = Injections.createLocator();
        ProviderBinder.bindProviders(config.getComponentBag(), locator);
        final Iterable<WriterInterceptor> writerInterceptors =
                Providers.getAllProviders(locator, WriterInterceptor.class, new RankedComparator<WriterInterceptor>());

        final Iterator<WriterInterceptor> writerIterator = writerInterceptors.iterator();

        assertEquals(HighPriorityProvider.class, writerIterator.next().getClass());
        assertEquals(MidPriorityProvider.class, writerIterator.next().getClass());
        assertEquals(LowPriorityProvider.class, writerIterator.next().getClass());
        assertFalse(writerIterator.hasNext());

        final Iterable<ReaderInterceptor> readerInterceptors =
                Providers.getAllProviders(locator, ReaderInterceptor.class, new RankedComparator<ReaderInterceptor>());

        final Iterator<ReaderInterceptor> readerIterator = readerInterceptors.iterator();

        assertEquals(LowPriorityProvider.class, readerIterator.next().getClass());
        assertEquals(MidPriorityProvider.class, readerIterator.next().getClass());
        assertEquals(HighPriorityProvider.class, readerIterator.next().getClass());
        assertFalse(readerIterator.hasNext());
    }


    private <T> void _testCollectionsCommon(final String testName, final Collection<T> collection, final T element)
            throws Exception {

        // Not null.
        assertNotNull(testName + " - returned collection is null.", collection);

        // Immutability.
        try {
            collection.add(element);
            fail(testName + " - returned collection should be immutable.");
        } catch (final Exception e) {
            // OK.
        }
    }

    public static final class CustomReaderA implements ReaderInterceptor {

        @Override
        public Object aroundReadFrom(final ReaderInterceptorContext context) throws IOException, WebApplicationException {
            return null;
        }
    }

    public static final class CustomReaderB implements ReaderInterceptor {

        @Override
        public Object aroundReadFrom(final ReaderInterceptorContext context) throws IOException, WebApplicationException {
            return null;
        }
    }

    public static final class SimpleFeatureA implements Feature {

        private boolean initB;

        public SimpleFeatureA() {
        }

        public SimpleFeatureA(final boolean initB) {
            this.initB = initB;
        }

        @Override
        public boolean configure(final FeatureContext config) {
            config.register(initB ? CustomReaderB.class : CustomReaderA.class);
            return true;
        }
    }

    public static final class SimpleFeatureB implements Feature {

        @Override
        public boolean configure(final FeatureContext config) {
            config.register(CustomReaderB.class);
            return true;
        }
    }

    public static final class InstanceFeatureA implements Feature {

        private boolean initB;

        public InstanceFeatureA() {
        }

        public InstanceFeatureA(final boolean initB) {
            this.initB = initB;
        }

        @Override
        public boolean configure(final FeatureContext config) {
            config.register(initB ? new CustomReaderB() : new CustomReaderA());
            return true;
        }
    }

    public static final class ComplexFeature implements Feature {

        @Override
        public boolean configure(final FeatureContext config) {
            config.register(SimpleFeatureA.class);
            config.register(SimpleFeatureB.class);
            return true;
        }
    }

    public static final class RecursiveFeature implements Feature {

        @Override
        public boolean configure(final FeatureContext config) {
            config.register(new CustomReaderA());
            config.register(RecursiveFeature.class);
            return true;
        }
    }

    public static final class RecursiveInstanceFeature implements Feature {

        @Override
        public boolean configure(final FeatureContext config) {
            config.register(new CustomReaderA());
            config.register(new RecursiveInstanceFeature());
            return true;
        }
    }

    @Test
    public void testConfigureFeatureHierarchy() throws Exception {
        config.register(ComplexFeature.class);

        config.configureMetaProviders(Injections.createLocator());

        assertTrue(config.getConfiguration().isEnabled(ComplexFeature.class));

        assertTrue(config.getConfiguration().isRegistered(CustomReaderA.class));
        assertTrue(config.getConfiguration().isRegistered(CustomReaderB.class));
    }

    @Test
    public void testConfigureFeatureRecursive() throws Exception {
        config.register(RecursiveFeature.class);
        config.configureMetaProviders(Injections.createLocator());

        assertTrue(config.getConfiguration().isEnabled(RecursiveFeature.class));
        assertEquals(1, config.getInstances().size());
        assertSame(CustomReaderA.class, config.getInstances().iterator().next().getClass());
    }

    @Test
    public void testConfigureFeatureInstances() throws Exception {
        final SimpleFeatureA f1 = new SimpleFeatureA();
        config.register(f1);
        final SimpleFeatureA f2 = new SimpleFeatureA(true);
        config.register(f2);

        config.configureMetaProviders(Injections.createLocator());

        assertTrue(config.getConfiguration().isEnabled(f1));
        assertFalse(config.getConfiguration().isEnabled(f2));

        assertTrue(config.getConfiguration().isRegistered(CustomReaderA.class));
        assertFalse(config.getConfiguration().isRegistered(CustomReaderB.class));
    }

    @Test
    public void testConfigureFeatureInstancesProviderInstances() throws Exception {
        final InstanceFeatureA f1 = new InstanceFeatureA();
        config.register(f1);
        final InstanceFeatureA f2 = new InstanceFeatureA(true);
        config.register(f2);

        config.configureMetaProviders(Injections.createLocator());

        assertTrue(config.getConfiguration().isEnabled(f1));
        assertFalse(config.getConfiguration().isEnabled(f2));

        final Set<Object> providerInstances = config.getInstances();
        assertEquals(2, providerInstances.size());

        final Set<Object> pureProviderInstances = config.getComponentBag().getInstances(ComponentBag.EXCLUDE_META_PROVIDERS);
        assertEquals(1, pureProviderInstances.size());

        int a = 0;
        int b = 0;
        for (final Object instance : pureProviderInstances) {
            if (instance instanceof CustomReaderA) {
                a++;
            } else {
                b++;
            }
        }
        assertEquals(1, a);
        assertEquals(0, b);
    }

    @Test
    public void testConfigureFeatureInstanceRecursive() throws Exception {
        config.register(new RecursiveInstanceFeature());
        config.configureMetaProviders(Injections.createLocator());
        assertEquals(0, config.getClasses().size());
        assertEquals(2, config.getInstances().size());
        final Set<Object> pureProviders = config.getComponentBag().getInstances(ComponentBag.EXCLUDE_META_PROVIDERS);
        assertEquals(1, pureProviders.size());
        assertSame(CustomReaderA.class, pureProviders.iterator().next().getClass());
    }

    public static interface Contract {
    }

    public static class Service implements Contract {
    }

    public static class ContractBinder extends AbstractBinder {

        @Override
        protected void configure() {
            bind(Service.class).to(Contract.class);
        }
    }

    public static class ContractBinderFeature implements Feature {

        @Override
        public boolean configure(final FeatureContext context) {
            context.register(new ContractBinder());
            return true;
        }
    }

    @Test
    public void testBinderConfiguringFeature() throws Exception {
        config.register(ContractBinderFeature.class);
        final ServiceLocator locator = Injections.createLocator();
        config.configureMetaProviders(locator);

        assertTrue(config.isEnabled(ContractBinderFeature.class));
        assertEquals(1, config.getInstances().size());
        assertSame(ContractBinder.class, config.getInstances().iterator().next().getClass());

        final Contract service = locator.getService(Contract.class);
        assertNotNull(service);
        assertSame(Service.class, service.getClass());
    }

    public static class InjectMe {
    }

    public static class InjectIntoFeatureInstance implements Feature {

        @Inject
        private InjectMe injectMe;

        @Override
        public boolean configure(final FeatureContext context) {
            context.property("instance-injected", injectMe != null);
            return true;
        }
    }

    public static class InjectIntoFeatureClass implements Feature {

        @Inject
        private InjectMe injectMe;

        @Override
        public boolean configure(final FeatureContext context) {
            context.property("class-injected", injectMe != null);
            return true;
        }
    }

    public static class BindInjectMeInFeature implements Feature {
        @Override
        public boolean configure(FeatureContext context) {
            context.register(new AbstractBinder() {
                @Override
                protected void configure() {
                    bind(new InjectMe());
                }
            });
            return true;
        }
    }

    @Test
    public void testFeatureInjections() throws Exception {
        config.register(InjectIntoFeatureClass.class)
                .register(new InjectIntoFeatureInstance())
                .register(new AbstractBinder() {
                    @Override
                    protected void configure() {
                        bind(new InjectMe());
                    }
                });

        final ServiceLocator locator = Injections.createLocator();
        config.configureMetaProviders(locator);

        assertThat("Feature instance not injected", config.getProperty("instance-injected").toString(), is("true"));
        assertThat("Feature class not injected", config.getProperty("class-injected").toString(), is("true"));
    }

    @Test
    @Ignore
    public void testFeatureInjectionsBindInFeature() throws Exception {
        config.register(new BindInjectMeInFeature());
        config.register(InjectIntoFeatureClass.class);
        config.register(new InjectIntoFeatureInstance());

        final ServiceLocator locator = Injections.createLocator();
        config.configureMetaProviders(locator);

        assertThat("Feature instance not injected", config.getProperty("instance-injected").toString(), is("true"));
        assertThat("Feature class not injected", config.getProperty("class-injected").toString(), is("true"));
    }

}
