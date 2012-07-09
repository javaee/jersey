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

import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.Descriptor;
import org.glassfish.hk2.api.DynamicConfiguration;
import org.glassfish.hk2.api.FactoryDescriptors;
import org.glassfish.hk2.api.Filter;
import org.glassfish.hk2.api.HK2Loader;
import org.glassfish.hk2.api.MultiException;
import org.glassfish.hk2.utilities.DescriptorImpl;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Skeleton implementation of injection module with convenience methods for
 * binding definitions.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public abstract class AbstractModule implements Module, DynamicConfiguration {

    private transient DynamicConfiguration configuration;

    @Override
    public void bind(DynamicConfiguration configuration) {
        checkState(this.configuration == null, "Recursive configuration call detected.");

        this.configuration = checkNotNull(configuration, "configuration");
        try {
            configure();
        } finally {
            this.configuration = null;
        }
    }

    /**
     * Implement to provide module binding definitions using the exposed binding
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
     *     an active call to {@link #configure()} method.
     */
    private DynamicConfiguration configuration() {
        checkState(configuration != null, "Dynamic configuration accessed from outside of an active module configuration scope.");
        return configuration;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * This method can be called only in the execution context of the {@link #configure()}
     * method.
     */
    @Override
    public ActiveDescriptor<?> bind(Descriptor descriptor) {
        setLoader(descriptor);
        return configuration.bind(descriptor);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * This method can be called only in the execution context of the {@link #configure()}
     * method.
     */
    @Override
    public FactoryDescriptors bind(FactoryDescriptors factoryDescriptors) {
        setLoader(factoryDescriptors.getFactoryAsAService());
        setLoader(factoryDescriptors.getFactoryAsAFactory());

        return configuration.bind(factoryDescriptors);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * This method can be called only in the execution context of the {@link #configure()}
     * method.
     */
    @Override
    public <T> ActiveDescriptor<T> addActiveDescriptor(ActiveDescriptor<T> activeDescriptor) throws IllegalArgumentException {
        return configuration.addActiveDescriptor(activeDescriptor);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * This method can be called only in the execution context of the {@link #configure()}
     * method.
     */
    @Override
    public <T> ActiveDescriptor<T> addActiveDescriptor(Class<T> rawClass) throws MultiException, IllegalArgumentException {
        return configuration.addActiveDescriptor(rawClass);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * This method can be called only in the execution context of the {@link #configure()}
     * method.
     */
    @Override
    public void addUnbindFilter(Filter unbindFilter) throws IllegalArgumentException {
        configuration.addUnbindFilter(unbindFilter);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * This method can be called only in the execution context of the {@link #configure()}
     * method.
     */
    @Override
    public void commit() throws MultiException {
        configuration.commit();
    }

    /**
     * Adds all binding definitions from the modules to the binding configuration.
     *
     * @param modules modules whose binding definitions should be configured.
     */
    public final void install(Module... modules) {
        for (Module module : modules) {
            module.bind(this);
        }
    }

    private void setLoader(Descriptor descriptor) {
        if (descriptor instanceof DescriptorImpl) {
            final ClassLoader loader = getModuleClassloader();

            ((DescriptorImpl)descriptor).setLoader(new HK2Loader() {
                @Override
                public Class<?> loadClass(String className) throws MultiException {
                    try {
                        return loader.loadClass(className);
                    } catch (ClassNotFoundException e) {
                        throw new MultiException(e);
                    }
                }
            });
        } // else who knows?
    }

    private ClassLoader getModuleClassloader() {
        ClassLoader loader = this.getClass().getClassLoader();
        return loader == null ? ClassLoader.getSystemClassLoader() : loader;
    }
}
