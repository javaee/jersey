/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.model;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.BindingPriority;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Configurable;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import org.glassfish.jersey.internal.inject.Injections;
import org.glassfish.jersey.internal.inject.ProviderBinder;
import org.glassfish.jersey.internal.inject.Providers;
import org.glassfish.jersey.model.internal.FeatureBag;
import org.glassfish.jersey.model.internal.ProviderBag;
import org.glassfish.jersey.model.internal.DefaultConfig;
import org.glassfish.jersey.model.internal.RankedComparator;

import org.glassfish.hk2.api.ServiceLocator;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Test cases for {@link javax.ws.rs.core.Configurable}.
 *
 * @author Michal Gajdos (michal.gajdos at oracle.com)
 */
public class  ConfigurableTest {

    private Configurable configurable;

    @Before
    public void setUp() throws Exception {
        configurable = new DefaultConfig();
    }

    @Test
    public void testGetProperties() throws Exception {
        try {
            configurable.getProperties().put("foo", "bar");
            fail("Returned properties collection should be immutable.");
        } catch (Exception e) {
            // OK.
        }
    }

    @Test
    public void testSetProperties() throws Exception {
        configurable = configurable.setProperty("foo", "bar");
        assertEquals("bar", configurable.getProperty("foo"));

        final Map<String, String> properties = Maps.newHashMap();
        properties.put("hello", "world");
        configurable = configurable.setProperties(properties);

        assertEquals(1, configurable.getProperties().size());
        assertEquals("world", configurable.getProperty("hello"));

        properties.put("one", "two");
        assertEquals(1, configurable.getProperties().size());
        assertNull(configurable.getProperty("one"));

        configurable = configurable.setProperties(Maps.<String, String>newHashMap());
        assertTrue(configurable.getProperties().isEmpty());
    }

    @Test
    public void testSetGetProperty() throws Exception {
        configurable = configurable.setProperty("foo", "bar");
        assertEquals("bar", configurable.getProperty("foo"));

        configurable.setProperty("hello", "world");
        configurable.setProperty("foo", null);

        assertEquals(null, configurable.getProperty("foo"));
        assertEquals(1, configurable.getProperties().size());
    }

    public static class EmptyFeature implements Feature {

        @Override
        public boolean configure(final Configurable configurable) {
            return true;
        }
    }

    public static class UnconfigurableFeature implements Feature {

        @Override
        public boolean configure(final Configurable configurable) {
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
        public boolean configure(final Configurable configurable) {
            throw new UnsupportedOperationException();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetFeatures() throws Exception {
        final EmptyFeature emptyFeature = new EmptyFeature();
        final UnconfigurableFeature unconfigurableFeature = new UnconfigurableFeature();

        _testCollectionsCommon("GetFeatures", configurable.getFeatures(), emptyFeature);

        configurable.register(emptyFeature);
        configurable.register(unconfigurableFeature);
        configurable.register(new ComplexEmptyProviderFeature(), ReaderInterceptor.class);

        final DefaultConfig defaultConfig = (DefaultConfig) configurable;
        final FeatureBag featureBag = defaultConfig.getFeatureBag();
        assertEquals(0, featureBag.getEnabledFeatures().size());
        assertEquals(2, featureBag.getUnconfiguredFeatures().size());
        assertTrue(featureBag.isRegistered(new FeatureBag.RegisteredFeature(null, emptyFeature)));
        assertTrue(featureBag.isRegistered(new FeatureBag.RegisteredFeature(null, unconfigurableFeature)));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetProviderClasses() throws Exception {
        _testCollectionsCommon("GetProviderClasses", configurable.getProviderClasses(), EmptyFeature.class);

        configurable.register(ComplexEmptyProviderFeature.class, WriterInterceptor.class);
        assertEquals(0, configurable.getProviderClasses().size());

        configurable.register(EmptyFeature.class);
        configurable.register(ComplexEmptyProviderFeature.class, ReaderInterceptor.class);
        configurable.register(ComplexEmptyProviderFeature.class, ContainerRequestFilter.class);

        final Set<Class<?>> providerClasses = configurable.getProviderClasses();
        assertEquals(1, providerClasses.size());
        assertTrue(providerClasses.contains(ComplexEmptyProviderFeature.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetProviderInstances() throws Exception {
        _testCollectionsCommon("GetProviderInstances", configurable.getProviderInstances(), new EmptyFeature());

        configurable.register(new ComplexEmptyProviderFeature(), WriterInterceptor.class);
        assertEquals(0, configurable.getProviderInstances().size());

        final ComplexEmptyProviderFeature providerFeature1 = new ComplexEmptyProviderFeature();

        configurable.register(new EmptyFeature());
        configurable.register(providerFeature1, ReaderInterceptor.class);
        configurable.register(providerFeature1, ContainerRequestFilter.class);

        Set<Object> providerInstances = configurable.getProviderInstances();
        assertEquals(1, providerInstances.size());
        assertTrue(providerInstances.contains(providerFeature1));

        final ComplexEmptyProviderFeature providerFeature2 = new ComplexEmptyProviderFeature();

        configurable.register(providerFeature2, ExceptionMapper.class);
        providerInstances = configurable.getProviderInstances();
        assertEquals(2, providerInstances.size());
        assertTrue(providerInstances.contains(providerFeature1));
        assertTrue(providerInstances.contains(providerFeature2));
    }

    @Test
    public void testRegisterClass() throws Exception {
        try {
            final Class clazz = null;
            configurable.register(clazz);
            fail("Cannot register null.");
        } catch (IllegalArgumentException e) {
            // OK.
        }

        for (int i = 0; i < 2; i++) {
            configurable.register(ComplexEmptyProvider.class);

            final DefaultConfig defaultConfig = (DefaultConfig) configurable;
            final ContractProvider contractProvider =
                    defaultConfig.getProviderBag().getModels().get(ComplexEmptyProvider.class);
            final Set<Class<?>> contracts = contractProvider.getContracts();

            assertEquals(3, contracts.size());  // Feature is not there.
            assertTrue(contracts.contains(ReaderInterceptor.class));
            assertTrue(contracts.contains(ContainerRequestFilter.class));
            assertTrue(contracts.contains(ExceptionMapper.class));
        }
    }

    @Test
    public void testRegisterInstance() throws Exception {
        try {
            configurable.register(null);
            fail("Cannot register null.");
        } catch (IllegalArgumentException e) {
            // OK.
        }

        for (int i = 0; i < 2; i++) {
            configurable.register(new ComplexEmptyProvider());

            final DefaultConfig defaultConfig = (DefaultConfig) configurable;
            final ContractProvider contractProvider =
                    defaultConfig.getProviderBag().getModels().get(ComplexEmptyProvider.class);
            final Set<Class<?>> contracts = contractProvider.getContracts();

            assertEquals(3, contracts.size()); // Feature is not there.
            assertTrue(contracts.contains(ReaderInterceptor.class));
            assertTrue(contracts.contains(ContainerRequestFilter.class));
            assertTrue(contracts.contains(ExceptionMapper.class));
        }
    }

    @Test
    public void testRegisterClassInstanceClash() throws Exception {
        final ComplexEmptyProvider complexEmptyProvider = new ComplexEmptyProvider();

        configurable.register(ComplexEmptyProvider.class);
        configurable.register(complexEmptyProvider);
        configurable.register(ComplexEmptyProvider.class);

        final DefaultConfig defaultConfig = (DefaultConfig) configurable;
        final ProviderBag providerBag = defaultConfig.getProviderBag();

        assertFalse(providerBag.getClasses().contains(ComplexEmptyProvider.class));
        assertTrue(providerBag.getInstances().contains(complexEmptyProvider));

        final ContractProvider contractProvider =
                providerBag.getModels().get(ComplexEmptyProvider.class);
        final Set<Class<?>> contracts = contractProvider.getContracts();

        assertEquals(3, contracts.size()); // Feature is not there.
        assertTrue(contracts.contains(ReaderInterceptor.class));
        assertTrue(contracts.contains(ContainerRequestFilter.class));
        assertTrue(contracts.contains(ExceptionMapper.class));
    }

    @Test
    public void testRegisterClassBingingPriority() throws Exception {
        try {
            final Class clazz = null;
            configurable.register(clazz, BindingPriority.USER);
            fail("Cannot register null.");
        } catch (IllegalArgumentException e) {
            // OK.
        }

        for (int priority : new int[] {BindingPriority.USER, BindingPriority.AUTHENTICATION}) {
            configurable.register(ComplexEmptyProvider.class, priority);

            final DefaultConfig defaultConfig = (DefaultConfig) configurable;
            final ContractProvider contractProvider =
                    defaultConfig.getProviderBag().getModels().get(ComplexEmptyProvider.class);
            final Set<Class<?>> contracts = contractProvider.getContracts();

            assertEquals(3, contracts.size()); // Feature is not there.
            assertTrue(contracts.contains(ReaderInterceptor.class));
            assertTrue(contracts.contains(ContainerRequestFilter.class));
            assertTrue(contracts.contains(ExceptionMapper.class));

            // All priorities are the same.
            assertEquals(BindingPriority.USER, contractProvider.getPriority(ReaderInterceptor.class));
            assertEquals(BindingPriority.USER, contractProvider.getPriority(ContainerRequestFilter.class));
            assertEquals(BindingPriority.USER, contractProvider.getPriority(ExceptionMapper.class));
        }
    }

    @Test
    public void testRegisterInstanceBingingPriority() throws Exception {
        try {
            configurable.register(null, BindingPriority.USER);
            fail("Cannot register null.");
        } catch (IllegalArgumentException e) {
            // OK.
        }

        final Class<ComplexEmptyProvider> providerClass = ComplexEmptyProvider.class;

        for (int priority : new int[] {BindingPriority.USER, BindingPriority.AUTHENTICATION}) {
            configurable.register(providerClass, priority);

            final DefaultConfig defaultConfig = (DefaultConfig) configurable;
            final ContractProvider contractProvider =
                    defaultConfig.getProviderBag().getModels().get(providerClass);
            final Set<Class<?>> contracts = contractProvider.getContracts();

            assertEquals(3, contracts.size()); // Feature is not there.
            assertTrue(contracts.contains(ReaderInterceptor.class));
            assertTrue(contracts.contains(ContainerRequestFilter.class));
            assertTrue(contracts.contains(ExceptionMapper.class));

            // All priorities are the same.
            assertEquals(BindingPriority.USER, contractProvider.getPriority(ReaderInterceptor.class));
            assertEquals(BindingPriority.USER, contractProvider.getPriority(ContainerRequestFilter.class));
            assertEquals(BindingPriority.USER, contractProvider.getPriority(ExceptionMapper.class));
        }
    }

    @Test
    public void testRegisterClassInstanceBindingPriorityClash() throws Exception {
        final ComplexEmptyProvider complexEmptyProvider = new ComplexEmptyProvider();

        configurable.register(ComplexEmptyProvider.class, BindingPriority.USER);
        configurable.register(complexEmptyProvider, BindingPriority.AUTHENTICATION);
        configurable.register(ComplexEmptyProvider.class, BindingPriority.USER);

        final DefaultConfig defaultConfig = (DefaultConfig) configurable;
        final ProviderBag providerBag = defaultConfig.getProviderBag();

        assertFalse(providerBag.getClasses().contains(ComplexEmptyProvider.class));
        assertTrue(providerBag.getInstances().contains(complexEmptyProvider));

        final ContractProvider contractProvider =
                providerBag.getModels().get(ComplexEmptyProvider.class);
        final Set<Class<?>> contracts = contractProvider.getContracts();

        assertEquals(3, contracts.size()); // Feature is not there.
        assertTrue(contracts.contains(ReaderInterceptor.class));
        assertTrue(contracts.contains(ContainerRequestFilter.class));
        assertTrue(contracts.contains(ExceptionMapper.class));

        // All priorities are the same.
        assertEquals(BindingPriority.AUTHENTICATION, contractProvider.getPriority(ReaderInterceptor.class));
        assertEquals(BindingPriority.AUTHENTICATION, contractProvider.getPriority(ContainerRequestFilter.class));
        assertEquals(BindingPriority.AUTHENTICATION, contractProvider.getPriority(ExceptionMapper.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRegisterClassContracts() throws Exception {
        try {
            final Class clazz = null;
            configurable.register(clazz, ReaderInterceptor.class);
            fail("Cannot register null.");
        } catch (IllegalArgumentException e) {
            // OK.
        }

        final Set<Class<?>> registeredContracts = Sets.newIdentityHashSet();
        for (Class contract : new Class[] {ReaderInterceptor.class, ContainerRequestFilter.class, WriterInterceptor.class}) {
            configurable.register(ComplexEmptyProvider.class, contract);

            if (!WriterInterceptor.class.equals(contract)) {
                registeredContracts.add(contract);
            }

            final DefaultConfig defaultConfig = (DefaultConfig) configurable;
            final ContractProvider contractProvider =
                    defaultConfig.getProviderBag().getModels().get(ComplexEmptyProvider.class);
            final Set<Class<?>> contracts = contractProvider.getContracts();

            assertEquals(registeredContracts.size(), contracts.size());

            for (Class<?> registeredContract : registeredContracts) {
                if (!WriterInterceptor.class.equals(registeredContract)) {
                    assertTrue(registeredContract + " is not registered.", contracts.contains(registeredContract));
                } else {
                    assertFalse(registeredContract + " should not be registered.", contracts.contains(registeredContract));
                }
            }
        }

        assertTrue(configurable.getProviderInstances().isEmpty());
        assertTrue(configurable.getProviderClasses().contains(ComplexEmptyProvider.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRegisterInstancesContracts() throws Exception {
        try {
            configurable.register(null, ReaderInterceptor.class);
            fail("Cannot register null.");
        } catch (IllegalArgumentException e) {
            // OK.
        }

        final Set<Class<?>> registeredContracts = Sets.newIdentityHashSet();
        final ComplexEmptyProvider complexEmptyProvider = new ComplexEmptyProvider();

        for (Class contract : new Class[] {ReaderInterceptor.class, ContainerRequestFilter.class, WriterInterceptor.class}) {
            configurable.register(complexEmptyProvider, contract);

            if (!WriterInterceptor.class.equals(contract)) {
                registeredContracts.add(contract);
            }

            final DefaultConfig defaultConfig = (DefaultConfig) configurable;
            final ContractProvider contractProvider =
                    defaultConfig.getProviderBag().getModels().get(ComplexEmptyProvider.class);
            final Set<Class<?>> contracts = contractProvider.getContracts();

            assertEquals(registeredContracts.size(), contracts.size());

            for (Class<?> registeredContract : registeredContracts) {
                if (!WriterInterceptor.class.equals(registeredContract)) {
                    assertTrue(registeredContract + " is not registered.", contracts.contains(registeredContract));
                } else {
                    assertFalse(registeredContract + " should not be registered.", contracts.contains(registeredContract));
                }
            }
        }

        assertTrue(configurable.getProviderInstances().contains(complexEmptyProvider));
        assertTrue(configurable.getProviderClasses().isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRegisterClassContractsFeatureNotInvoked() throws Exception {
        configurable.register(ComplexEmptyProviderFeature.class, ReaderInterceptor.class);
        assertTrue(configurable.getFeatures().isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRegisterInstancesContractsFeatureNotInvoked() throws Exception {
        configurable.register(new ComplexEmptyProviderFeature(), ReaderInterceptor.class);
        assertTrue(configurable.getFeatures().isEmpty());
    }

    @Test
    public void testRegisterClassNullContracts() throws Exception {
        configurable.register(ComplexEmptyProvider.class, (Class) null);

        final DefaultConfig defaultConfig = (DefaultConfig) configurable;
        final ContractProvider contractProvider =
                defaultConfig.getProviderBag().getModels().get(ComplexEmptyProvider.class);
        final Set<Class<?>> contracts = contractProvider.getContracts();

        assertEquals(3, contracts.size()); // Feature is not there.
        assertTrue(contracts.contains(ReaderInterceptor.class));
        assertTrue(contracts.contains(ContainerRequestFilter.class));
        assertTrue(contracts.contains(ExceptionMapper.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRegisterInstanceNullContracts() throws Exception {
        configurable.register(new ComplexEmptyProvider(), (Class) null);

        final DefaultConfig defaultConfig = (DefaultConfig) configurable;
        final ContractProvider contractProvider =
                defaultConfig.getProviderBag().getModels().get(ComplexEmptyProvider.class);
        final Set<Class<?>> contracts = contractProvider.getContracts();

        assertEquals(3, contracts.size()); // Feature is not there.
        assertTrue(contracts.contains(ReaderInterceptor.class));
        assertTrue(contracts.contains(ContainerRequestFilter.class));
        assertTrue(contracts.contains(ExceptionMapper.class));
    }

    @Test
    public void testRegisterClassEmptyContracts() throws Exception {
        //noinspection unchecked
        configurable.register(ComplexEmptyProvider.class, new Class[0]);

        final DefaultConfig defaultConfig = (DefaultConfig) configurable;
        final ContractProvider contractProvider =
                defaultConfig.getProviderBag().getModels().get(ComplexEmptyProvider.class);
        final Set<Class<?>> contracts = contractProvider.getContracts();

        assertEquals(3, contracts.size()); // Feature is not there.
        assertTrue(contracts.contains(ReaderInterceptor.class));
        assertTrue(contracts.contains(ContainerRequestFilter.class));
        assertTrue(contracts.contains(ExceptionMapper.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRegisterInstanceEmptyContracts() throws Exception {
        configurable.register(new ComplexEmptyProvider(), new Class[0]);

        final DefaultConfig defaultConfig = (DefaultConfig) configurable;
        final ContractProvider contractProvider =
                defaultConfig.getProviderBag().getModels().get(ComplexEmptyProvider.class);
        final Set<Class<?>> contracts = contractProvider.getContracts();

        assertEquals(3, contracts.size()); // Feature is not there.
        assertTrue(contracts.contains(ReaderInterceptor.class));
        assertTrue(contracts.contains(ContainerRequestFilter.class));
        assertTrue(contracts.contains(ExceptionMapper.class));
    }

    @BindingPriority(300)
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

    @BindingPriority(200)
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

    @BindingPriority(100)
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
        final DefaultConfig configurable = new DefaultConfig();

        configurable.register(MidPriorityProvider.class, 500);
        configurable.register(LowPriorityProvider.class, 20);
        configurable.register(HighPriorityProvider.class, 150);

        ProviderBinder.bindProviders(configurable.getProviderBag(), locator);
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
        final DefaultConfig configurable = new DefaultConfig();

        configurable.register(MidPriorityProvider.class, 50);
        configurable.register(LowPriorityProvider.class, 2000);
        configurable.register(HighPriorityProvider.class);

        ProviderBinder.bindProviders(configurable.getProviderBag(), locator);
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
        final DefaultConfig configurable = new DefaultConfig();

        configurable.register(MidPriorityProvider.class);
        configurable.register(LowPriorityProvider.class);
        configurable.register(HighPriorityProvider.class);

        ProviderBinder.bindProviders(configurable.getProviderBag(), locator);
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
        // Different priorities for different contracts.
        final ServiceLocator locator = Injections.createLocator();
        final DefaultConfig configurable = new DefaultConfig();

        configurable.register(MidPriorityProvider.class, WriterInterceptor.class);
        configurable.register(LowPriorityProvider.class, WriterInterceptor.class);
        configurable.register(HighPriorityProvider.class, WriterInterceptor.class);

        configurable.register(MidPriorityProvider.class, 2000, ReaderInterceptor.class);
        configurable.register(LowPriorityProvider.class, 1000, ReaderInterceptor.class);
        configurable.register(HighPriorityProvider.class, 3000, ReaderInterceptor.class);

        ProviderBinder.bindProviders(configurable.getProviderBag(), locator);
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
        } catch (Exception e) {
            // OK.
        }
    }
}
