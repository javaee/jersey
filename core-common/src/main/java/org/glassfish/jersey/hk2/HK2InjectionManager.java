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
import org.glassfish.jersey.internal.inject.Binder;
import org.glassfish.jersey.internal.inject.Binding;
import org.glassfish.jersey.internal.inject.Bindings;
import org.glassfish.jersey.internal.inject.ClassBinding;
import org.glassfish.jersey.internal.inject.CompositeBinder;
import org.glassfish.jersey.internal.inject.ForeignDescriptor;
import org.glassfish.jersey.internal.inject.ForeignDescriptorImpl;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.InstanceBinding;
import org.glassfish.jersey.spi.ServiceHolder;
import org.glassfish.jersey.spi.ServiceHolderImpl;

import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.ServiceLocatorFactory;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;

import org.jvnet.hk2.external.runtime.ServiceLocatorRuntimeBean;

/**
 * Implementation of {@link InjectionManager} for HK Dependency Injection Framework.
 */
public class HK2InjectionManager implements InjectionManager {

    private static final Logger LOGGER = Logger.getLogger(HK2InjectionManager.class.getName());

    private static final ServiceLocatorFactory factory = ServiceLocatorFactory.getInstance();

    private ServiceLocator locator;

    /**
     * Default constructor to be able to find a {@link HK2InjectionManager} using {@link java.util.ServiceLoader}.
     */
    public HK2InjectionManager() {
    }

    /**
     * Creates a new instance using the underlying {@link ServiceLocator}. More information about how to use this c'tor can found
     * here {@link #createInjectionManager(ServiceLocator)}.
     *
     * @param locator underlying HK2 injection manager
     */
    private HK2InjectionManager(ServiceLocator locator) {
        this.locator = locator;
    }

    /**
     * Creates a new {@link ServiceLocator} instance from static {@link ServiceLocatorFactory} and adds the provided parent
     * locator if the instance is not null.
     *
     * @param name   name of the injection manager.
     * @param parent parent of the new injection manager.
     * @return new instance of injection manager.
     */
    private static ServiceLocator createLocator(String name, InjectionManager parent) {
        assertParentLocatorType(parent);

        ServiceLocator parentLocator = null;
        if (parent != null) {
            parentLocator = ((HK2InjectionManager) parent).locator;
        }

        ServiceLocator result = factory.create(name, parentLocator, null, ServiceLocatorFactory.CreatePolicy.DESTROY);
        result.setNeutralContextClassLoader(false);
        ServiceLocatorUtilities.enablePerThreadScope(result);
        return result;
    }

    /**
     * Creates a new instance of {@link HK2InjectionManager} and automatically add the provided instance of {@code  ServiceLocator}
     * as a underlying injection manager.
     * <p>
     * Commonly used in this scenarios:
     * - create {@code InjectionManager} with externally provided {@code ServiceLocator}.
     * - create {@code InjectionManager} which is immediately put into the different {@code InjectionManager} as a parent.
     *
     * @param locator HK2 ServiceLocator
     * @return new instance of {@link HK2InjectionManager} with underlying {@link ServiceLocator}.
     */
    public static InjectionManager createInjectionManager(ServiceLocator locator) {
        return new HK2InjectionManager(locator);
    }

    /**
     * Returns {@link ServiceLocator} dedicated to this instance of {@link HK2InjectionManager}.
     *
     * @return underlying instance of {@code ServiceLocator}.
     */
    public ServiceLocator getServiceLocator() {
        return locator;
    }

    @Override
    public void initialize(String name, InjectionManager parent, Binder... binders) {
        this.locator = createLocator(name, parent);
        ServiceLocatorUtilities.bind(locator, new JerseyClassAnalyzer.Binder(locator));

        // First service the current BeanManager to be able to inject itself into other services.
        Hk2Helper.bind(locator, Bindings.service(this).to(InjectionManager.class));

        // Add support for Context annotation.
        Hk2Helper.bind(locator, new ContextInjectionResolverImpl.Binder());

        // Compose together the initialization binders and bind them as a whole.
        Hk2Helper.bind(locator, CompositeBinder.wrap(binders));

        this.locator.setDefaultClassAnalyzerName(JerseyClassAnalyzer.NAME);

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
     * Checks if the parent is null then must be an instance of {@link HK2InjectionManager}.
     *
     * @param parent parent {@code InjectionManager}.
     */
    private static void assertParentLocatorType(InjectionManager parent) {
        if (parent != null && !(parent instanceof HK2InjectionManager)) {
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
    public void register(Binding binding) {
        Hk2Helper.bind(locator, binding);
    }

    @Override
    public void register(Iterable<Binding> descriptors) {
        Hk2Helper.bind(locator, descriptors);
    }

    @Override
    public void register(Binder binder) {
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
    public Object getInstance(ForeignDescriptor foreignDescriptor) {
        return locator.getServiceHandle((ActiveDescriptor<?>) foreignDescriptor.get()).getService();
    }

    @Override
    public ForeignDescriptor createForeignDescriptor(Binding binding) {
        ForeignDescriptor foreignDescriptor = createAndTranslateForeignDescriptor(binding);
        ActiveDescriptor<Object> activeDescriptor = ServiceLocatorUtilities
                .addOneDescriptor(locator, (org.glassfish.hk2.api.Descriptor) foreignDescriptor.get(), false);
        return new ForeignDescriptorImpl(activeDescriptor);
    }

    private ForeignDescriptor createAndTranslateForeignDescriptor(Binding binding) {
        ActiveDescriptor<?> activeDescriptor;
        if (ClassBinding.class.isAssignableFrom(binding.getClass())) {
            activeDescriptor = Hk2Helper.translateToActiveDescriptor((ClassBinding<?>) binding);
        } else if (InstanceBinding.class.isAssignableFrom(binding.getClass())) {
            activeDescriptor = Hk2Helper.translateToActiveDescriptor((InstanceBinding<?>) binding);
        } else {
            throw new RuntimeException(LocalizationMessages.UNKNOWN_DESCRIPTOR_TYPE(binding.getClass().getSimpleName()));
        }

        return new ForeignDescriptorImpl(activeDescriptor);
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
