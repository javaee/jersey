/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2015 Oracle and/or its affiliates. All rights reserved.
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

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.inject.Singleton;

import jersey.repackaged.com.google.common.collect.Maps;
import jersey.repackaged.com.google.common.collect.Sets;

/**
 * Jersey contract provider model.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public final class ContractProvider implements Scoped, NameBound {
    /**
     * "No priority" constant.
     */
    public static final int NO_PRIORITY = -1;

    /**
     * Create new contract provider model builder.
     *
     * @return new contract provider builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Create new contract provider model builder from an existing one.
     *
     * @param original existing contract provider model.
     * @return new contract provider builder.
     */
    public static Builder builder(final ContractProvider original) {
        return new Builder(original);
    }

    /**
     * Contract provider model builder.
     */
    public static final class Builder {

        private static final ContractProvider EMPTY_MODEL = new ContractProvider(
                Singleton.class,
                Collections.<Class<?>, Integer>emptyMap(),
                NO_PRIORITY,
                Collections.<Class<? extends Annotation>>emptySet());

        private Class<? extends Annotation> scope = null;
        private Map<Class<?>, Integer> contracts = Maps.newHashMap();
        private int defaultPriority = NO_PRIORITY;
        private Set<Class<? extends Annotation>> nameBindings = Sets.newIdentityHashSet();

        private Builder() {
        }

        private Builder(final ContractProvider original) {
            this.scope = original.scope;
            this.contracts.putAll(original.contracts);
            this.defaultPriority = original.defaultPriority;
            this.nameBindings.addAll(original.nameBindings);
        }

        /**
         * Change contract provider scope. (Default scope is {@link Singleton}.)
         *
         * @param scope contract provider scope.
         * @return updated builder.
         */
        public Builder scope(final Class<? extends Annotation> scope) {
            this.scope = scope;
            return this;
        }

        /**
         * Add a new provided contract.
         *
         * @param contract additional provided contract.
         * @return updated builder.
         */
        public Builder addContract(final Class<?> contract) {
            return addContract(contract, defaultPriority);
        }

        /**
         * Add a new provided contract with priority.
         *
         * @param contract additional provided contract.
         * @param priority priority for the contract.
         * @return updated builder.
         */
        public Builder addContract(final Class<?> contract, final int priority) {
            contracts.put(contract, priority);
            return this;
        }

        /**
         * Add a new provided contracts.
         *
         * @param contracts additional provided contracts.
         * @return updated builder.
         */
        public Builder addContracts(final Map<Class<?>, Integer> contracts) {
            this.contracts.putAll(contracts);
            return this;
        }

        /**
         * Add a new provided contracts.
         *
         * @param contracts additional provided contracts.
         * @return updated builder.
         */
        public Builder addContracts(final Collection<Class<?>> contracts) {
            for (final Class<?> contract : contracts) {
                addContract(contract, defaultPriority);
            }
            return this;
        }

        /**
         * Set the contract default provider priority. (Default value is {@link ContractProvider#NO_PRIORITY})
         *
         * @param defaultPriority default contract provider priority.
         * @return updated builder.
         */
        public Builder defaultPriority(final int defaultPriority) {
            this.defaultPriority = defaultPriority;
            return this;
        }

        /**
         * Add a new contract provider name binding.
         *
         * @param binding name binding.
         * @return updated builder.
         */
        public Builder addNameBinding(final Class<? extends Annotation> binding) {
            this.nameBindings.add(binding);
            return this;
        }

        /**
         * Get the scope of the built contract provider model.
         *
         * @return scope associated with the model or {@code null} if no scope
         * has been set explicitly.
         */
        public Class<? extends Annotation> getScope() {
            return scope;
        }

        /**
         * Get the map of contracts for the built contract provider model.
         *
         * @return contracts associated with the model.
         */
        public Map<Class<?>, Integer> getContracts() {
            return contracts;
        }

        /**
         * Get the default priority of the built contract provider model.
         *
         * @return default priority associated with the model.
         */
        public int getDefaultPriority() {
            return defaultPriority;
        }

        /**
         * Get name bindings of the built contract provider model.
         *
         * @return name bindings associated with the model.
         */
        public Set<Class<? extends Annotation>> getNameBindings() {
            return nameBindings;
        }

        /**
         * Build a new contract provider model.
         *
         * @return new contract provider model.
         */
        public ContractProvider build() {
            if (scope == null) {
                scope = Singleton.class;
            }

            final Map<Class<?>, Integer> _contracts = (contracts.isEmpty())
                    ? Collections.<Class<?>, Integer>emptyMap()
                    : Maps.transformEntries(contracts, new Maps.EntryTransformer<Class<?>, Integer, Integer>() {
                        @Override
                        public Integer transformEntry(final Class<?> contract, final Integer priority) {
                            return (priority != NO_PRIORITY) ? priority : defaultPriority;
                        }
                    });

            final Set<Class<? extends Annotation>> bindings = (nameBindings.isEmpty())
                    ? Collections.<Class<? extends Annotation>>emptySet() : Collections.unmodifiableSet(nameBindings);

            if (scope == Singleton.class && _contracts.isEmpty() && defaultPriority == NO_PRIORITY && bindings.isEmpty()) {
                return EMPTY_MODEL;
            }

            return new ContractProvider(scope, _contracts, defaultPriority, bindings);
        }
    }

    private final Map<Class<?>, Integer> contracts;
    private final int defaultPriority;
    private final Set<Class<? extends Annotation>> nameBindings;
    private final Class<? extends Annotation> scope;

    private ContractProvider(
            final Class<? extends Annotation> scope,
            final Map<Class<?>, Integer> contracts,
            final int defaultPriority,
            final Set<Class<? extends Annotation>> nameBindings) {

        this.scope = scope;
        this.contracts = contracts;
        this.defaultPriority = defaultPriority;
        this.nameBindings = nameBindings;
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return scope;
    }

    /**
     * Get provided contracts recognized by Jersey.
     *
     * @return provided contracts.
     *
     * @see org.glassfish.jersey.spi.Contract
     */
    public Set<Class<?>> getContracts() {
        return contracts.keySet();
    }

    /**
     * Get the map of contracts and their priorities.
     *
     * @return contracts and their priorities.
     */
    public Map<Class<?>, Integer> getContractMap() {
        return contracts;
    }

    @Override
    public boolean isNameBound() {
        return !nameBindings.isEmpty();
    }

    /**
     * Get the provider contract priority, if set, default component provider, if not set.
     *
     * @param contract provider contract.
     * @return provider priority.
     *
     * @see javax.annotation.Priority
     */
    public int getPriority(final Class<?> contract) {
        if (contracts.containsKey(contract)) {
            return contracts.get(contract);
        }
        return defaultPriority;
    }

    @Override
    public Set<Class<? extends Annotation>> getNameBindings() {
        return nameBindings;
    }
}
