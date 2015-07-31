/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.ext.cdi1x.internal;

import java.lang.annotation.Annotation;
import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.InjectionTargetFactory;

import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.ServiceLocator;

/**
 * Abstract HK2 factory to provide CDI components obtained from CDI bean manager.
 * The factory handles CDI managed components as well as non-contextual managed beans.
 * To specify HK2 scope of provided CDI beans, an extension of this factory
 * should implement properly annotated {@link Factory#provide()} method that
 * could just delegate to the existing {@link #_provide()} method.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public abstract class AbstractCdiBeanHk2Factory<T> implements Factory<T> {

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
    final InstanceManager<T> referenceProvider;
    final Annotation[] qualifiers;

    @SuppressWarnings(value = "unchecked")
    /* package */ T _provide() {
        final T instance = referenceProvider.getInstance(clazz);
        if (instance != null) {
            return instance;
        }
        throw new NoSuchElementException(LocalizationMessages.CDI_LOOKUP_FAILED(clazz));
    }

    @Override
    public void dispose(final T instance) {
        referenceProvider.preDestroy(instance);
    }

    /**
     * Create new factory instance for given type and bean manager.
     *
     * @param rawType     type of the components to provide.
     * @param locator     actual HK2 service locator instance.
     * @param beanManager current bean manager to get references from.
     * @param cdiManaged  set to {@code true} if the component should be managed by CDI.
     */
    public AbstractCdiBeanHk2Factory(final Class<T> rawType, final ServiceLocator locator, final BeanManager beanManager,
                                     final boolean cdiManaged) {

        this.clazz = rawType;
        this.qualifiers = CdiUtil.getQualifiers(clazz.getAnnotations());
        this.referenceProvider = cdiManaged ? new InstanceManager<T>() {

            final Iterator<Bean<?>> beans = beanManager.getBeans(clazz, qualifiers).iterator();
            final Bean bean = beans.hasNext() ? beans.next() : null;

            @Override
            public T getInstance(final Class<T> clazz) {
                return (bean != null) ? CdiUtil.getBeanReference(clazz, bean, beanManager) : null;
            }

            @Override
            public void preDestroy(final T instance) {
                // do nothing
            }
        } : new InstanceManager<T>() {

            final AnnotatedType<T> annotatedType = beanManager.createAnnotatedType(clazz);
            final InjectionTargetFactory<T> injectionTargetFactory = beanManager.getInjectionTargetFactory(annotatedType);
            final InjectionTarget<T> injectionTarget = injectionTargetFactory.createInjectionTarget(null);

            @Override
            public T getInstance(final Class<T> clazz) {
                final CreationalContext<T> creationalContext = beanManager.createCreationalContext(null);
                final T instance = injectionTarget.produce(creationalContext);
                injectionTarget.inject(instance, creationalContext);
                if (locator != null) {
                    locator.inject(instance, CdiComponentProvider.CDI_CLASS_ANALYZER);
                }
                injectionTarget.postConstruct(instance);
                return instance;
            }

            @Override
            public void preDestroy(final T instance) {
                injectionTarget.preDestroy(instance);
            }
        };
    }
}
