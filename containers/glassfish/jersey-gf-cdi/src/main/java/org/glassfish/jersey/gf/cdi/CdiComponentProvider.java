/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.gf.cdi;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.logging.Logger;

import javax.ws.rs.core.Application;

import javax.annotation.ManagedBean;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.ProcessInjectionTarget;


import javax.naming.InitialContext;

import org.glassfish.hk2.api.ClassAnalyzer;
import org.glassfish.hk2.api.DynamicConfiguration;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.binding.ScopedBindingBuilder;
import org.glassfish.hk2.utilities.binding.ServiceBindingBuilder;
import org.glassfish.jersey.internal.inject.Injections;
import org.glassfish.jersey.internal.inject.Providers;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.spi.ComponentProvider;

/**
 * Jersey CDI integration implementation.
 * Implements {@link ComponentProvider Jersey component provider} to serve CDI beans
 * obtained from the actual CDI bean manager.
 * To properly inject JAX-RS/Jersey managed beans into CDI, it also
 * serves as a {@link Extension CDI Extension}, that intercepts CDI injection targets.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public class CdiComponentProvider implements ComponentProvider, Extension {

    private static final Logger LOGGER = Logger.getLogger(CdiComponentProvider.class.getName());

    private ServiceLocator locator;
    private BeanManager beanManager;

    private Map<Class<?>, Set<Method>> methodsToSkip = new HashMap<Class<?>, Set<Method>>();
    private Map<Class<?>, Set<Field>> fieldsToSkip = new HashMap<Class<?>, Set<Field>>();

    /**
     * HK2 factory to provide CDI components obtained from CDI bean manager.
     * The factory handles CDI managed components as well as non-contextual managed beans.
     */
    private static class CdiFactory<T> implements Factory<T> {

        private interface InstanceManager<T> {
            /**
             * Get me correctly instantiated and injected instance.
             *
             * @param clazz type of the component to instantiate.
             * @return injected component instance.
             */
            T getInstance(Class<T> clazz);

            /**
             * Do whatever needs to be done before given instance is destroyed.
             *
             * @param instance to be destroyed.
             */
            void preDestroy(T instance);
        }

        final Class<T> clazz;
        final BeanManager beanManager;
        final ServiceLocator locator;
        final InstanceManager<T> referenceProvider;

        @SuppressWarnings("unchecked")
        @Override
        public T provide() {
            final T instance = referenceProvider.getInstance(clazz);
            if (instance != null) {
                return instance;
            }
            throw new NoSuchElementException(LocalizationMessages.CDI_LOOKUP_FAILED(clazz));
        }

        @Override
        public void dispose(T instance) {
            referenceProvider.preDestroy(instance);
        }

        /**
         * Create new factory instance for given type and bean manager.
         *
         * @param rawType type of the components to provide.
         * @param locator actual HK2 service locator instance
         * @param beanManager current bean manager to get references from.
         * @param cdiManaged set to true if the component should be managed by CDI
         */
        CdiFactory(final Class<T> rawType, final ServiceLocator locator, final BeanManager beanManager, boolean cdiManaged) {
            this.clazz = rawType;
            this.beanManager = beanManager;
            this.locator = locator;
            this.referenceProvider = cdiManaged ? new InstanceManager<T>() {
                @Override
                public T getInstance(Class<T> clazz) {

                    final Set<Bean<?>> beans = beanManager.getBeans(clazz);
                    for (Bean b : beans) {
                        final Object instance = beanManager.getReference(b, clazz, beanManager.createCreationalContext(b));
                        return (T) instance;
                    }
                    return null;
                }

                @Override
                public void preDestroy(T instance) {
                    // do nothing
                }


            } : new InstanceManager<T>() {

                final AnnotatedType<T> annotatedType = beanManager.createAnnotatedType(clazz);
                final InjectionTarget<T> injectionTarget = beanManager.createInjectionTarget(annotatedType);
                final CreationalContext creationalContext = beanManager.createCreationalContext(null);

                @Override
                public T getInstance(Class<T> clazz) {
                    final T instance = injectionTarget.produce(creationalContext);
                    injectionTarget.inject(instance, creationalContext);
                    if (locator != null) {
                        locator.inject(instance, ProviderSkippingClassAnalyzer.NAME);
                    }
                    injectionTarget.postConstruct(instance);

                    return instance;
                }

                @Override
                public void preDestroy(T instance) {
                    injectionTarget.preDestroy(instance);
                }
            };
        }
    }

    @Override
    public void initialize(final ServiceLocator locator) {

        this.locator = locator;

        beanManager = beanManagerFromJndi();
        if (beanManager != null) {
            final CdiComponentProvider extension = beanManager.getExtension(this.getClass());
            if (extension != null) {
                extension.locator = this.locator;
                this.fieldsToSkip = extension.fieldsToSkip;
                this.methodsToSkip = extension.methodsToSkip;
                LOGGER.config(LocalizationMessages.CDI_PROVIDER_INITIALIZED());
            }
        }
    }

    @Override
    public boolean bind(final Class<?> clazz, final Set<Class<?>> providerContracts) {

        if (beanManager == null) {
            return false;
        }

        final boolean isCdiManaged = isCdiComponent(clazz);
        final boolean isManagedBean = isManagedBean(clazz);
        final boolean isJaxRsComponent = isJaxRsComponentType(clazz);

        if (!isCdiManaged && !isManagedBean && !isJaxRsComponent) {
            return false;
        }

        DynamicConfiguration dc = Injections.getConfiguration(locator);

        final ServiceBindingBuilder bindingBuilder =
                Injections.newFactoryBinder(new CdiFactory(clazz, locator, beanManager, isCdiManaged));

        bindingBuilder.to(clazz);
        for (Class contract : providerContracts) {
            bindingBuilder.to(contract);
        }

        Injections.addBinding(bindingBuilder, dc);

        dc.commit();
        return true;
    }

    @Override
    public void done() {
        bindProviderSkippingAnalyzer();
    }

    private boolean isCdiComponent(Class<?> component) {
        return !beanManager.getBeans(component).isEmpty();
    }

    private boolean isManagedBean(Class<?> component) {
        return component.isAnnotationPresent(ManagedBean.class);
    }

    @SuppressWarnings("unused")
    private void processInjectionTarget(@Observes ProcessInjectionTarget event) {
        final InjectionTarget it = event.getInjectionTarget();
        final Class<?> componentClass = event.getAnnotatedType().getJavaClass();

        final Set<InjectionPoint> injectionPoints = it.getInjectionPoints();

        for (InjectionPoint injectionPoint : injectionPoints) {
            final Member member = injectionPoint.getMember();
            if (member instanceof Field) {
                addInjecteeToSkip(componentClass, fieldsToSkip, (Field) member);
            } else if (member instanceof Method) {
                addInjecteeToSkip(componentClass, methodsToSkip, (Method) member);
            }
        }

       if (isJaxRsComponentType(componentClass)) {
           event.setInjectionTarget(new InjectionTarget() {

                @Override
                public void inject(Object t, CreationalContext cc) {
                   it.inject(t, cc);
                    if (locator != null) {
                        locator.inject(t, ProviderSkippingClassAnalyzer.NAME);
                    }
                }

                @Override
                public void postConstruct(Object t) {
                    it.postConstruct(t);
                }

                @Override
                public void preDestroy(Object t) {
                    it.preDestroy(t);
                }

                @Override
                public Object produce(CreationalContext cc) {
                    return it.produce(cc);
                }

                @Override
                public void dispose(Object t) {
                    it.dispose(t);
                }

                @Override
                public Set getInjectionPoints() {
                    return it.getInjectionPoints();
                }
            });
        }
    }

    private <T> void addInjecteeToSkip(final Class<?> componentClass, final Map<Class<?>, Set<T>> toSkip, final T member) {
        if (!toSkip.containsKey(componentClass)) {
            toSkip.put(componentClass, new HashSet<T>());
        }
        toSkip.get(componentClass).add(member);
    }

    /**
     * Introspect given type to determine if it represents a JAX-RS component.
     *
     * @param clazz type to be introspected.
     * @return true if the type represents a JAX-RS component type.
     */
    /* package */static boolean isJaxRsComponentType(Class<?> clazz) {
        return Application.class.isAssignableFrom(clazz) ||
                Providers.isJaxRsProvider(clazz) ||
                  Resource.from(clazz) != null;
    }

    private static BeanManager beanManagerFromJndi() {
        try {
            return (BeanManager)new InitialContext().lookup("java:comp/BeanManager");
        } catch (Exception ex) {
            LOGGER.config(LocalizationMessages.CDI_BEAN_MANAGER_JNDI_LOOKUP_FAILED());
            return null;
        }
    }

    private void bindProviderSkippingAnalyzer() {

        final ProviderSkippingClassAnalyzer
                customizedClassAnalyzer =new ProviderSkippingClassAnalyzer(
                                                locator.getService(ClassAnalyzer.class, ClassAnalyzer.DEFAULT_IMPLEMENTATION_NAME),
                                                methodsToSkip,
                                                fieldsToSkip);

        DynamicConfiguration dc = Injections.getConfiguration(locator);

        final ScopedBindingBuilder bindingBuilder =
                Injections.newBinder(customizedClassAnalyzer);

        bindingBuilder.analyzeWith(ClassAnalyzer.DEFAULT_IMPLEMENTATION_NAME)
                .to(ClassAnalyzer.class)
                .named(ProviderSkippingClassAnalyzer.NAME);

        Injections.addBinding(bindingBuilder, dc);

        dc.commit();
    }
}