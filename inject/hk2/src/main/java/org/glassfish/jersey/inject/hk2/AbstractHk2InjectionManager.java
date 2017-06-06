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

package org.glassfish.jersey.inject.hk2;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.glassfish.jersey.internal.inject.Binding;
import org.glassfish.jersey.internal.inject.ClassBinding;
import org.glassfish.jersey.internal.inject.ForeignDescriptor;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.InstanceBinding;
import org.glassfish.jersey.internal.inject.ServiceHolder;
import org.glassfish.jersey.internal.inject.ServiceHolderImpl;

import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.Descriptor;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.ServiceLocatorFactory;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;

import org.jvnet.hk2.external.runtime.ServiceLocatorRuntimeBean;

/**
 * Abstract class dedicated to implementations of {@link InjectionManager} providing several convenient methods.
 *
 * @author Petr Bouda (petr.bouda at oracle.com)
 */
abstract class AbstractHk2InjectionManager implements InjectionManager {

    private static final Logger LOGGER = Logger.getLogger(AbstractHk2InjectionManager.class.getName());

    private static final ServiceLocatorFactory factory = ServiceLocatorFactory.getInstance();

    private ServiceLocator locator;

    /**
     * Private constructor.
     *
     * @param parent parent of type {@link org.glassfish.jersey.internal.inject.InjectionManager} or {@link ServiceLocator}.
     */
    AbstractHk2InjectionManager(Object parent) {
        ServiceLocator parentLocator = resolveServiceLocatorParent(parent);
        this.locator = createLocator(parentLocator);

        // Register all components needed for proper HK2 locator bootstrap
        ServiceLocatorUtilities.bind(locator, new Hk2BootstrapBinder(locator));

        this.locator.setDefaultClassAnalyzerName(JerseyClassAnalyzer.NAME);

        // clear HK2 caches
        ServiceLocatorRuntimeBean serviceLocatorRuntimeBean = locator.getService(ServiceLocatorRuntimeBean.class);
        if (serviceLocatorRuntimeBean != null) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(LocalizationMessages.HK_2_CLEARING_CACHE(serviceLocatorRuntimeBean.getServiceCacheSize(),
                        serviceLocatorRuntimeBean.getReflectionCacheSize()));
            }
            serviceLocatorRuntimeBean.clearReflectionCache();
            serviceLocatorRuntimeBean.clearServiceCache();
        }
    }


    /**
     * Creates a new {@link ServiceLocator} instance from static {@link ServiceLocatorFactory} and adds the provided parent
     * locator if the instance is not null.
     *
     * @param parentLocator parent locator, can be {@code null}.
     * @return new instance of injection manager.
     */
    private static ServiceLocator createLocator(ServiceLocator parentLocator) {
        ServiceLocator result = factory.create(null, parentLocator, null, ServiceLocatorFactory.CreatePolicy.DESTROY);
        result.setNeutralContextClassLoader(false);
        ServiceLocatorUtilities.enablePerThreadScope(result);
        return result;
    }

    private static ServiceLocator resolveServiceLocatorParent(Object parent) {
        assertParentLocatorType(parent);

        ServiceLocator parentLocator = null;
        if (parent != null) {
            if (parent instanceof ServiceLocator) {
                parentLocator = (ServiceLocator) parent;
            } else if (parent instanceof AbstractHk2InjectionManager) {
                parentLocator = ((AbstractHk2InjectionManager) parent).getServiceLocator();
            }
        }
        return parentLocator;
    }

    /**
     * Checks if the parent is null then must be an instance of {@link ServiceLocator} or {@link InjectionManager}.
     *
     * @param parent object represented by {@code ServiceLocator} or {@code HK2InjectionManager}.
     */
    private static void assertParentLocatorType(Object parent) {
        if (parent != null && !(parent instanceof ServiceLocator || parent instanceof AbstractHk2InjectionManager)) {
            throw new IllegalArgumentException(
                    LocalizationMessages.HK_2_UNKNOWN_PARENT_INJECTION_MANAGER(parent.getClass().getSimpleName()));
        }
    }

    public ServiceLocator getServiceLocator() {
        return locator;
    }

    @Override
    public boolean isRegistrable(Class<?> clazz) {
        return org.glassfish.hk2.utilities.Binder.class.isAssignableFrom(clazz);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<ServiceHolder<T>> getAllServiceHolders(Class<T> contract, Annotation... qualifiers) {
        return getServiceLocator().getAllServiceHandles(contract, qualifiers).stream()
                .map(sh -> new ServiceHolderImpl<>(
                        sh.getService(),
                        (Class<T>) sh.getActiveDescriptor().getImplementationClass(),
                        sh.getActiveDescriptor().getContractTypes(),
                        sh.getActiveDescriptor().getRanking()))
                .collect(Collectors.toList());
    }

    @Override
    public <T> T getInstance(Class<T> clazz, Annotation... annotations) {
        return getServiceLocator().getService(clazz, annotations);
    }

    @Override
    public <T> T getInstance(Type clazz) {
        return getServiceLocator().getService(clazz);
    }

    @Override
    public Object getInstance(ForeignDescriptor foreignDescriptor) {
        return getServiceLocator().getServiceHandle((ActiveDescriptor<?>) foreignDescriptor.get()).getService();
    }

    @Override
    public <T> T getInstance(Class<T> clazz) {
        return getServiceLocator().getService(clazz);
    }

    @Override
    public <T> T getInstance(Class<T> clazz, String classAnalyzer) {
        return getServiceLocator().getService(clazz, classAnalyzer);
    }

    @Override
    public <T> List<T> getAllInstances(Type clazz) {
        return getServiceLocator().getAllServices(clazz);
    }

    @Override
    public void preDestroy(Object preDestroyMe) {
        getServiceLocator().preDestroy(preDestroyMe);
    }

    @Override
    public void shutdown() {
        if (factory.find(getServiceLocator().getName()) != null) {
            factory.destroy(getServiceLocator().getName());
        } else {
            getServiceLocator().shutdown();
        }
    }

    @Override
    public <U> U createAndInitialize(Class<U> clazz) {
        return getServiceLocator().createAndInitialize(clazz);
    }

    @Override
    public ForeignDescriptor createForeignDescriptor(Binding binding) {
        ForeignDescriptor foreignDescriptor = createAndTranslateForeignDescriptor(binding);
        ActiveDescriptor<Object> activeDescriptor = ServiceLocatorUtilities
                .addOneDescriptor(getServiceLocator(), (Descriptor) foreignDescriptor.get(), false);
        return ForeignDescriptor.wrap(activeDescriptor, activeDescriptor::dispose);
    }

    @Override
    public void inject(Object injectMe) {
        getServiceLocator().inject(injectMe);
    }

    @Override
    public void inject(Object injectMe, String classAnalyzer) {
        getServiceLocator().inject(injectMe, classAnalyzer);
    }

    @SuppressWarnings("unchecked")
    private ForeignDescriptor createAndTranslateForeignDescriptor(Binding binding) {
        ActiveDescriptor activeDescriptor;
        if (ClassBinding.class.isAssignableFrom(binding.getClass())) {
            activeDescriptor = Hk2Helper.translateToActiveDescriptor((ClassBinding<?>) binding);
        } else if (InstanceBinding.class.isAssignableFrom(binding.getClass())) {
            activeDescriptor = Hk2Helper.translateToActiveDescriptor((InstanceBinding<?>) binding);
        } else {
            throw new RuntimeException(org.glassfish.jersey.internal.LocalizationMessages
                    .UNKNOWN_DESCRIPTOR_TYPE(binding.getClass().getSimpleName()));
        }

        return ForeignDescriptor.wrap(activeDescriptor, activeDescriptor::dispose);
    }
}
