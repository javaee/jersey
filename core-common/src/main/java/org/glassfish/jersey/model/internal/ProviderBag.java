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

package org.glassfish.jersey.model.internal;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.glassfish.jersey.model.ContractProvider;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * A container for contract provider models used during the server or client
 * initialization.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public final class ProviderBag {

    /**
     * Provider bag builder.
     */
    public final static class Builder {
        /**
         * Contract provider classes.
         */
        private final Set<Class<?>> classes = Sets.newIdentityHashSet();
        /**
         * Contract provider instances.
         */
        private final Set<Object> instances = Sets.newIdentityHashSet();
        private final Set<Class<?>> instanceClasses = Sets.newIdentityHashSet();
        /**
         * Contract provider models.
         */
        private final Map<Class<?>, ContractProvider> models = Maps.newIdentityHashMap();

        public Builder() {
        }

        public Builder(final Builder original) {
            this.classes.addAll(original.classes);
            this.instances.addAll(original.instances);
            this.instanceClasses.addAll(original.instanceClasses);
            this.models.putAll(original.models);
        }

        public Builder(final ProviderBag original) {
            this.classes.addAll(original.getClasses());
            this.instances.addAll(original.getInstances());
            this.models.putAll(original.getModels());

            for (final Object instance : original.getInstances()) {
                this.instanceClasses.add(instance.getClass());
            }
        }

        /**
         * Build a provider bag.
         *
         * @return new provider bag initialized with the content of the provider bag builder.
         */
        public ProviderBag build() {
            return new ProviderBag(classes, instances, models);
        }

        /**
         * Register a class as a contract provider.
         *
         * @param providerClass class to be introspected as a contract provider and registered.
         * @param isResource if {@code true} the class is a treated as a resource class, i.e. it
         *                   will not be added to the set of provider classes.
         * @return {@code true} if the class was successfully registered as provider.
         */
        public boolean register(final Class<?> providerClass, final boolean isResource) {
            if (isResource) {
                final ContractProvider model = ContractProvider.from(providerClass);
                if (model == null) {
                    return false;
                }
                models.put(providerClass, model);

                return true;
            } else {
                return register(providerClass, ContractProvider.NO_PRIORITY, Collections.<Class<?>>emptySet());
            }
        }

        /**
         * Register a class as a contract provider.
         *
         * @param providerClass class to be introspected as a contract provider and registered.
         * @param bindingPriority priority to bind the providers contracts to.
         * @param contracts contracts to bind the provider to.
         * @return {@code true} if the class was successfully registered as provider.
         */
        public boolean register(final Class<?> providerClass,
                                final int bindingPriority,
                                final Set<Class<?>> contracts) {
            // see Configurable#register(Class) paragraph 3
            if (instanceClasses.contains(providerClass)) {
                return false;
            }
            boolean success = updateContractProvider(providerClass, bindingPriority, contracts);
            return success && classes.add(providerClass);
        }

        private boolean updateContractProvider(final Class<?> providerClass,
                                               final int bindingPriority, final Set<Class<?>> contracts) {

            final ContractProvider contractProvider = models.get(providerClass);

            if (contractProvider == null) {
                final ContractProvider model = ContractProvider.from(providerClass, bindingPriority, contracts);
                if (model == null) {
                    return false;
                }
                models.put(providerClass, model);
            } else {
                final Set<Class<?>> currentContracts = contractProvider.getContracts();
                ContractProvider.Builder builder = null;

                // Add more contracts to a provider / adjust priority.
                for (final Class<?> contract : contracts) {
                    if (!currentContracts.contains(contract)
                            || contractProvider.getPriority(contract) == ContractProvider.NO_PRIORITY) {
                        builder = builder == null ? ContractProvider.builder(contractProvider) : builder;
                        builder.addContract(contract, bindingPriority);
                    }
                }

                if (builder != null) {
                    models.put(providerClass, builder.build());
                }
            }

            return true;
        }

        /**
         * Register an instance as a contract provider.
         *
         * @param provider instance to be introspected as a contract provider and registered.
         * @param isResource if {@code true} the instance is a treated as a resource class, i.e. it
         *                   will not be added to the set of provider instance.
         * @return {@code true} if the instance was successfully registered as provider.
         */
        public boolean register(final Object provider, final boolean isResource) {
            if (isResource) {
                final Class<?> providerClass = provider.getClass();
                final ContractProvider model = ContractProvider.from(providerClass);
                if (model == null) {
                    return false;
                }
                models.put(providerClass, model);
                return true;
            } else {
                return register(provider, ContractProvider.NO_PRIORITY, Collections.<Class<?>>emptySet());
            }
        }

        /**
         * Register an instance as a contract provider.
         *
         * @param provider instance to be introspected as a contract provider and registered.
         * @param bindingPriority priority to bind the providers contracts to.
         * @param contracts contracts to bind the provider to.
         * @return {@code true} if the instance was successfully registered as provider.
         */
        public boolean register(final Object provider,
                                final int bindingPriority,
                                final Set<Class<?>> contracts) {
            final Class<?> providerClass = provider.getClass();

            // see Configurable#register(Class) paragraph 3
            if (classes.contains(providerClass) && !instanceClasses.contains(providerClass)) {
                classes.remove(providerClass);
                models.remove(providerClass);
            }

            boolean success = updateContractProvider(providerClass, bindingPriority, contracts);

            return success && instances.add(provider) && instanceClasses.add(providerClass);
        }

    }

    /**
     * Contract provider classes.
     */
    private final Set<Class<?>> classes;
    /**
     * Contract provider instances.
     */
    private final Set<Object> instances;
    /**
     * Contract provider models.
     */
    private final Map<Class<?>, ContractProvider> models;

    private ProviderBag(final Set<Class<?>> classes, final Set<Object> instances, final Map<Class<?>, ContractProvider> models) {
        this.classes = Collections.unmodifiableSet(classes);
        this.instances = Collections.unmodifiableSet(instances);
        this.models = Collections.unmodifiableMap(models);
    }

    public Set<Class<?>> getClasses() {
        return classes;
    }

    public Set<Object> getInstances() {
        return instances;
    }

    public Map<Class<?>, ContractProvider> getModels() {
        return models;
    }

    public ContractProvider getContractProvider(final Class<?> providerClass) {
        return models.get(providerClass);
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof ProviderBag)) {
            return false;
        }
        ProviderBag other = (ProviderBag) obj;

        return this == other
                && (classes == other.classes || classes.equals(other.classes))
                && (instances == other.instances || instances.equals(other.instances));
    }

    @Override
    public int hashCode() {
        int hash = 37;
        hash = 47 * hash + classes.hashCode();
        hash = 47 * hash + instances.hashCode();
        return hash;
    }
}
