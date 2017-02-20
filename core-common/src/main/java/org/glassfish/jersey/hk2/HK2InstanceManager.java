/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.hk2;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.glassfish.jersey.internal.LocalizationMessages;
import org.glassfish.jersey.spi.ServiceHolder;
import org.glassfish.jersey.spi.ServiceHolderImpl;
import org.glassfish.jersey.spi.inject.Binder;
import org.glassfish.jersey.spi.inject.CompositeBinder;
import org.glassfish.jersey.spi.inject.Descriptor;
import org.glassfish.jersey.spi.inject.Descriptors;
import org.glassfish.jersey.spi.inject.InstanceManager;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.ServiceLocatorFactory;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;

import org.jvnet.hk2.external.runtime.ServiceLocatorRuntimeBean;

/**
 * Implementation of {@link InstanceManager} for HK Dependency Injection Framework.
 */
public class HK2InstanceManager implements InstanceManager {

    private static final Logger LOGGER = Logger.getLogger(HK2InstanceManager.class.getName());

    private static final ServiceLocatorFactory factory = ServiceLocatorFactory.getInstance();

    private ServiceLocator locator;

    /**
     * Default constructor to be able to find a {@link HK2InstanceManager} using {@link java.util.ServiceLoader}.
     */
    public HK2InstanceManager() {
    }

    /**
     * Creates a new instance using the underlying {@link ServiceLocator}. More information about how to use this c'tor can found
     * here {@link #createInstanceManager(ServiceLocator)}.
     *
     * @param locator underlying HK2 instance manager
     */
    private HK2InstanceManager(ServiceLocator locator) {
        this.locator = locator;
    }

    /**
     * Creates a new {@link ServiceLocator} instance from static {@link ServiceLocatorFactory} and adds the provided parent
     * locator if the instance is not null.
     *
     * @param name   name of the instance manager.
     * @param parent parent of the new instance manager.
     * @return new instance of instance manager.
     */
    private static ServiceLocator createLocator(String name, InstanceManager parent) {
        assertParentLocatorType(parent);

        ServiceLocator parentLocator = null;
        if (parent != null) {
            parentLocator = ((HK2InstanceManager) parent).locator;
        }

        ServiceLocator result = factory.create(name, parentLocator, null, ServiceLocatorFactory.CreatePolicy.DESTROY);
        result.setNeutralContextClassLoader(false);
        ServiceLocatorUtilities.enablePerThreadScope(result);
        return result;
    }

    /**
     * Creates a new instance of {@link HK2InstanceManager} and automatically add the provided instance of {@code  ServiceLocator}
     * as a underlying instance manager.
     * <p>
     * Commonly used in this scenarios:
     * - create {@code InstanceManager} with externally provided {@code ServiceLocator}.
     * - create {@code InstanceManager} which is immediately put into the different {@code InstanceManager} as a parent.
     *
     * @param locator HK2 ServiceLocator
     * @return new instance of {@link HK2InstanceManager} with underlying {@link ServiceLocator}.
     */
    public static InstanceManager createInstanceManager(ServiceLocator locator) {
        return new HK2InstanceManager(locator);
    }

    /**
     * Returns {@link ServiceLocator} dedicated to this instance of {@link HK2InstanceManager}.
     *
     * @return underlying instance of {@code ServiceLocator}.
     */
    public ServiceLocator getServiceLocator() {
        return locator;
    }

    @Override
    public void initialize(String name, InstanceManager parent, String defaultClassAnalyzer, Binder... binders) {
        this.locator = createLocator(name, parent);

        // First service the current BeanManager to be able to inject itself into other services.
        Hk2Helper.bind(locator, Descriptors.service(this).to(InstanceManager.class));

        // Compose together the initialization binders and bind them as a whole.
        Hk2Helper.bind(locator, CompositeBinder.wrap(binders));

        this.locator.setDefaultClassAnalyzerName(defaultClassAnalyzer);

        // clear HK2 caches
        ServiceLocatorRuntimeBean serviceLocatorRuntimeBean = locator.getService(ServiceLocatorRuntimeBean.class);
        if (serviceLocatorRuntimeBean != null) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(LocalizationMessages.CLEARING_HK_2_CACHE(
                        serviceLocatorRuntimeBean.getServiceCacheSize(),
                        serviceLocatorRuntimeBean.getReflectionCacheSize()));
            }
            serviceLocatorRuntimeBean.clearReflectionCache();
            serviceLocatorRuntimeBean.clearServiceCache();
        }
    }

    /**
     * Checks if the parent is null then must be an instance of {@link HK2InstanceManager}.
     *
     * @param parent paren {@code InstanceManager}.
     */
    private static void assertParentLocatorType(InstanceManager parent) {
        if (parent != null && !(parent instanceof HK2InstanceManager)) {
            throw new RuntimeException(LocalizationMessages.HK_2_UNKNOWN_PARENT_INSTANCE_MANAGER(
                    parent.getClass().getSimpleName()));
        }
    }

    @Override
    public void shutdown() {
        if (factory.find(locator.getName()) != null) {
            factory.destroy(locator.getName());
        } else {
            locator.shutdown();
        }
    }

    @Override
    public void register(Descriptor descriptor) {
        Hk2Helper.bind(locator, descriptor);
    }

    @Override
    public void register(Iterable<Descriptor> descriptors) {
        Hk2Helper.bind(locator, descriptors);
    }

    @Override
    public void register(org.glassfish.jersey.spi.inject.Binder binder) {
        Hk2Helper.bind(locator, binder);
    }

    @Override
    public <U> U createAndInitialize(Class<U> clazz) {
        return locator.createAndInitialize(clazz);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<ServiceHolder<T>> getAllServiceHolders(Class<T> contract, Annotation... qualifiers) {
        return locator.getAllServiceHandles(contract, qualifiers).stream()
                .map(sh -> new ServiceHolderImpl<>(
                        sh.getService(),
                        (Class<T>) sh.getActiveDescriptor().getImplementationClass(),
                        sh.getActiveDescriptor().getContractTypes(),
                        sh.getActiveDescriptor().getRanking()))
                .collect(Collectors.toList());
    }

    @Override
    public <T> T getInstance(Class<T> clazz, Annotation... annotations) {
        return locator.getService(clazz, annotations);
    }

    @Override
    public <T> T getInstance(Type clazz) {
        return locator.getService(clazz);
    }

    @Override
    public <T> T getInstance(Class<T> clazz) {
        return locator.getService(clazz);
    }

    @Override
    public <T> T getInstance(Class<T> clazz, String classAnalyzer) {
        return locator.getService(clazz, classAnalyzer);
    }

    @Override
    public <T> List<T> getAllInstances(Type clazz) {
        return locator.getAllServices(clazz);
    }

    @Override
    public void inject(Object injectMe) {
        locator.inject(injectMe);
    }

    @Override
    public void inject(Object injectMe, String classAnalyzer) {
        locator.inject(injectMe, classAnalyzer);
    }

    @Override
    public void preDestroy(Object preDestroyMe) {
        locator.preDestroy(preDestroyMe);
    }
}
