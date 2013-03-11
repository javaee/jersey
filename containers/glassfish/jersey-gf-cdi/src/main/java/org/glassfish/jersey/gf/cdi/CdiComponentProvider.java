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

import java.util.NoSuchElementException;
import java.util.Set;
import java.util.logging.Logger;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.ProcessInjectionTarget;

import javax.naming.InitialContext;

import org.glassfish.hk2.api.DynamicConfiguration;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.binding.ServiceBindingBuilder;
import org.glassfish.jersey.internal.inject.Injections;
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

    /**
     * HK2 factory to provide CDI components obtained from CDI bean manager.
     */
    private static class CdiFactory<T> implements Factory<T> {

        final Class<T> clazz;
        final BeanManager beanManager;

        @SuppressWarnings("unchecked")
        @Override
        public T provide() {
            final T instance = getClassReference();
            if (instance != null) {
                return instance;
            }
            throw new NoSuchElementException(LocalizationMessages.CDI_LOOKUP_FAILED(clazz));
        }

        @Override
        public void dispose(T instance) {
            // do nothing
        }

        /**
         * Create new factory instance for given type and bean manager.
         *
         * @param rawType is the type of the components to provide.
         * @param beanManager is the current bean manager to get references from.
         */
        CdiFactory(final Class<T> rawType, final BeanManager beanManager) {
            this.clazz = rawType;
            this.beanManager = beanManager;
        }

        /**
         * Lookup the component in CDI.
         *
         * @return CDI managed component instance.
         */
        private T getClassReference() {
            final Set<Bean<?>> beans = beanManager.getBeans(clazz);
            for (Bean b : beans) {
               final Object instance = beanManager.getReference(b, clazz, beanManager.createCreationalContext(b));
               return (T)instance;
            }
            return null;
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
                LOGGER.config(LocalizationMessages.CDI_PROVIDER_INITIALIZED());
            }
        }
    }

    @Override
    public boolean bind(final Class<?> clazz, final Set<Class<?>> providerContracts) {

       if (beanManager == null || !isCdiComponent(clazz)) {
            return false;
        }

        DynamicConfiguration dc = Injections.getConfiguration(locator);

        final ServiceBindingBuilder bindingBuilder =
                Injections.newFactoryBinder(new CdiFactory(clazz, beanManager));

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
    }

    private boolean isCdiComponent(Class<?> component) {
        if (beanManager == null) {
            return false;
        }
        return !beanManager.getBeans(component).isEmpty();
   }


    @SuppressWarnings("unused")
    private void processInjectionTarget(@Observes ProcessInjectionTarget event) {
        final InjectionTarget it = event.getInjectionTarget();
       if (JaxRsTypeChecker.isJaxRsComponentType(event.getAnnotatedType().getJavaClass())) {
           event.setInjectionTarget(new InjectionTarget() {

                @Override
                public void inject(Object t, CreationalContext cc) {
                   it.inject(t, cc);
                    if (locator != null) {
                        locator.inject(t);
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

    private static BeanManager beanManagerFromJndi() {
        try {
            return (BeanManager)new InitialContext().lookup("java:comp/BeanManager");
        } catch (Exception ex) {
            LOGGER.config(LocalizationMessages.CDI_BEAN_MANAGER_JNDI_LOOKUP_FAILED());
            return null;
        }
    }
}
