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
package org.glassfish.jersey.internal.inject;

import java.lang.annotation.Annotation;

import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.Descriptor;
import org.glassfish.hk2.api.DynamicConfiguration;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.FactoryDescriptors;
import org.glassfish.hk2.api.Filter;
import org.glassfish.hk2.api.HK2Loader;
import org.glassfish.hk2.api.MultiException;
import org.glassfish.hk2.api.TypeLiteral;
import org.glassfish.hk2.utilities.Binder;
import org.glassfish.hk2.utilities.DescriptorImpl;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Skeleton implementation of injection binder with convenience methods for
 * binding definitions.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public abstract class AbstractBinder implements Binder, DynamicConfiguration {

    private transient DynamicConfiguration configuration;
    private transient AbstractBindingBuilder<?> currentBuilder;
    private transient HK2Loader defaultLoader;

    /**
     * Start building a new class-based service binding.
     *
     * Does NOT bind the service type itself as a contract type.
     *
     * @param <T>         service type.
     * @param serviceType service class.
     * @return initialized binding builder.
     */
    public <T> ServiceBindingBuilder<T> bind(Class<T> serviceType) {
        return resetBuilder(AbstractBindingBuilder.create(serviceType, false));
    }

    /**
     * Start building a new class-based service binding.
     *
     * Binds the service type itself as a contract type.
     *
     * @param <T>         service type.
     * @param serviceType service class.
     * @return initialized binding builder.
     */
    public <T> ServiceBindingBuilder<T> bindAsContract(Class<T> serviceType) {
        return resetBuilder(AbstractBindingBuilder.create(serviceType, true));
    }

    /**
     * Start building a new generic type-based service binding.
     *
     * Binds the generic service type itself as a contract type.
     *
     * @param <T>         service type.
     * @param serviceType generic service type information.
     * @return initialized binding builder.
     */
    public <T> ServiceBindingBuilder<T> bindAsContract(TypeLiteral<T> serviceType) {
        return resetBuilder(AbstractBindingBuilder.create(serviceType, true));
    }

    /**
     * Start building a new instance-based service binding. The binding is naturally
     * considered to be a {@link javax.inject.Singleton singleton-scoped}.
     *
     * Does NOT bind the service type itself as a contract type.
     *
     * @param <T>     service type.
     * @param service service instance.
     * @return initialized binding builder.
     */
    public <T> ScopedBindingBuilder<T> bind(T service) {
        return resetBuilder(AbstractBindingBuilder.create(service));
    }

    /**
     * Start building a new factory class-based service binding.
     *
     * @param <T>          service type.
     * @param factoryType  service factory class.
     * @param factoryScope factory scope.
     * @return initialized binding builder.
     */
    public <T> ServiceBindingBuilder<T> bindFactory(
            Class<? extends Factory<T>> factoryType, Class<? extends Annotation> factoryScope) {
        return resetBuilder(AbstractBindingBuilder.<T>createFactoryBinder(factoryType, factoryScope));
    }

    /**
     * Start building a new factory class-based service binding.
     *
     * The factory itself is bound in a {@link org.glassfish.hk2.api.PerLookup per-lookup} scope.
     *
     * @param <T>         service type.
     * @param factoryType service factory class.
     * @return initialized binding builder.
     */
    public <T> ServiceBindingBuilder<T> bindFactory(Class<? extends Factory<T>> factoryType) {
        return resetBuilder(AbstractBindingBuilder.<T>createFactoryBinder(factoryType, null));
    }

    /**
     * Start building a new factory instance-based service binding.
     *
     * @param <T>     service type.
     * @param factory service instance.
     * @return initialized binding builder.
     */
    public <T> ServiceBindingBuilder<T> bindFactory(Factory<T> factory) {
        return resetBuilder(AbstractBindingBuilder.createFactoryBinder(factory));
    }

    @Override
    public void bind(DynamicConfiguration configuration) {
        checkState(this.configuration == null, "Recursive configuration call detected.");

        this.configuration = checkNotNull(configuration, "configuration");
        try {
            configure();
        } finally {
            complete();
        }
    }

    private <T> AbstractBindingBuilder<T> resetBuilder(AbstractBindingBuilder<T> newBuilder) {
        if (currentBuilder != null) {
            currentBuilder.complete(configuration(), getDefaultBinderLoader());
        }

        currentBuilder = newBuilder;

        return newBuilder;
    }

    private void complete() {
        try {
            resetBuilder(null);
        } finally {
            this.configuration = null;
        }
    }

    /**
     * Implement to provide binding definitions using the exposed binding
     * methods.
     */
    protected abstract void configure();

    /**
     * Get the active {@link DynamicConfiguration binder factory} instance used for
     * binding configuration. This method can only be called from within the
     * scope of the {@link #configure()} method.
     *
     * @return dynamic configuration instance used for binding configuration.
     * @throws IllegalStateException in case the method is not called from within
     *                               an active call to {@link #configure()} method.
     */
    private DynamicConfiguration configuration() {
        checkState(configuration != null, "Dynamic configuration accessed from outside of an active binder configuration scope.");
        return configuration;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method can be called only in the execution context of the {@link #configure()}
     * method.
     * </p>
     */
    @Override
    public <T> ActiveDescriptor<T> bind(Descriptor descriptor) {
        setLoader(descriptor);
        return configuration().bind(descriptor);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method can be called only in the execution context of the {@link #configure()}
     * method.
     * </p>
     */
    @Override
    public FactoryDescriptors bind(FactoryDescriptors factoryDescriptors) {
        setLoader(factoryDescriptors.getFactoryAsAService());
        setLoader(factoryDescriptors.getFactoryAsAFactory());

        return configuration().bind(factoryDescriptors);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method can be called only in the execution context of the {@link #configure()}
     * method.
     * </p>
     */
    @Override
    public <T> ActiveDescriptor<T> addActiveDescriptor(ActiveDescriptor<T> activeDescriptor) throws IllegalArgumentException {
        return configuration().addActiveDescriptor(activeDescriptor);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method can be called only in the execution context of the {@link #configure()}
     * method.
     * </p>
     */
    @Override
    public <T> ActiveDescriptor<T> addActiveDescriptor(Class<T> rawClass) throws MultiException, IllegalArgumentException {
        return configuration().addActiveDescriptor(rawClass);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method can be called only in the execution context of the {@link #configure()}
     * method.
     * </p>
     */
    @Override
    public void addUnbindFilter(Filter unbindFilter) throws IllegalArgumentException {
        configuration().addUnbindFilter(unbindFilter);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method can be called only in the execution context of the {@link #configure()}
     * method.
     * </p>
     */
    @Override
    public void commit() throws MultiException {
        configuration().commit();
    }

    /**
     * Adds all binding definitions from the binders to the binding configuration.
     *
     * @param binders binders whose binding definitions should be configured.
     */
    public final void install(Binder... binders) {
        for (Binder binder : binders) {
            binder.bind(this);
        }
    }

    private void setLoader(Descriptor descriptor) {
        if (descriptor.getLoader() == null && descriptor instanceof DescriptorImpl) {
            ((DescriptorImpl) descriptor).setLoader(getDefaultBinderLoader());
        } // else who knows?
    }

    private HK2Loader getDefaultBinderLoader() {
        if (defaultLoader == null) {
            ClassLoader loader = this.getClass().getClassLoader();
            final ClassLoader binderClassLoader = loader == null ? ClassLoader.getSystemClassLoader() : loader;
            defaultLoader = new HK2Loader() {
                @Override
                public Class<?> loadClass(String className) throws MultiException {
                    try {
                        return binderClassLoader.loadClass(className);
                    } catch (ClassNotFoundException e) {
                        throw new MultiException(e);
                    }
                }
            };
        }
        return defaultLoader;
    }
}
