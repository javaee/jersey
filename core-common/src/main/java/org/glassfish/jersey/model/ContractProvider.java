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

package org.glassfish.jersey.model;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Set;

import javax.ws.rs.BindingPriority;
import javax.ws.rs.NameBinding;

import javax.inject.Scope;
import javax.inject.Singleton;

import org.glassfish.jersey.internal.inject.Providers;

import com.google.common.collect.Sets;

/**
 * Jersey application contract provider model.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public final class ContractProvider implements Scoped, NameBound {
    /**
     * "No priority" constant.
     */
    public static final int NO_PRIORITY = -1;

    /**
     * Create a contract provider model by introspecting a contract provider/service
     * class.
     *
     * @param serviceClass contract provider/service class.
     * @return contract provider model for the class or {@code null} if the class does not
     *         implement any recognized provider contracts.
     */
    public static ContractProvider from(final Class<?> serviceClass) {
        Builder<?> builder = introspectService(serviceClass);
        return (builder == null) ? null : builder.build();
    }

    /**
     * Create a contract provider model by introspecting the class of a contract provider/service
     * instance.
     *
     * @param service contract provider/service instance.
     * @return contract provider model for the instance or {@code null} if the instance does not
     *         implement any recognized provider contracts.
     */
    public static ContractProvider from(final Object service) {
        return from(service.getClass());
    }

    private static Builder<?> introspectService(Class<?> serviceClass) {
        final Set<Class<?>> providerContracts = Providers.getProviderContracts(serviceClass);
        if (providerContracts.isEmpty()) {
            return null;
        }

        Builder<?> builder = builder().addContracts(providerContracts);

        for (Annotation annotation : serviceClass.getAnnotations()) {
            if (annotation instanceof BindingPriority) {
                builder.priority(((BindingPriority) annotation).value());
            } else {
                for (Annotation metaAnnotation : annotation.annotationType().getAnnotations()) {
                    if (metaAnnotation instanceof NameBinding) {
                        builder.addNameBinding(annotation.annotationType());
                    }
                    if (metaAnnotation instanceof Scope) {
                        builder.scope(annotation.annotationType());
                    }
                }
            }
        }
        return builder;
    }

    /**
     * Create new contract provider model builder.
     *
     * @return new contract provider builder.
     */
    public static <T> Builder<T> builder() {
        return new Builder<T>();
    }

    /**
     * Create new contract provider model builder from an existing one.
     *
     * @param original existing contract provider model.
     * @return new contract provider builder.
     */
    public static <T> Builder<T> builder(final ContractProvider original) {
        return new Builder<T>(original);
    }

    /**
     * Contract provider model builder.
     */
    public static final class Builder<T> {

        private Class<? extends Annotation> scope = Singleton.class;
        private Set<Class<?>> contracts = Sets.newIdentityHashSet();
        private int priority = NO_PRIORITY;
        private Set<Class<? extends Annotation>> nameBindings = Sets.newIdentityHashSet();

        private Builder() {
        }

        private Builder(final ContractProvider original) {
            this.scope = original.scope;
            this.contracts.addAll(original.contracts);
            this.priority = original.priority;
            this.nameBindings.addAll(original.nameBindings);
        }

        /**
         * Change contract provider scope. (Default scope is {@link Singleton}.)
         *
         * @param scope contract provider scope.
         * @return updated builder.
         */
        public Builder<T> scope(final Class<? extends Annotation> scope) {
            this.scope = scope;
            return this;
        }

        /**
         * Add a new provided contract.
         *
         * @param contract additional provided contract.
         * @return updated builder.
         */
        public Builder<T> addContract(final Class<? super T> contract) {
            this.contracts.add(contract);
            return this;
        }

        /**
         * Add a new provided contracts.
         *
         * @param contracts additional provided contracts.
         * @return updated builder.
         */
        private Builder<T> addContracts(final Collection<Class<?>> contracts) {
            this.contracts.addAll(contracts);
            return this;
        }

        /**
         * Set the contract provider priority. (By default is set to {@link ContractProvider#NO_PRIORITY}.)
         *
         * @param priority contract provider priority.
         * @return updated builder.
         */
        public Builder<T> priority(final int priority) {
            this.priority = priority;
            return this;
        }

        /**
         * Add a new contract provider name binding.
         *
         * @param binding name binding.
         * @return updated builder.
         */
        public Builder<T> addNameBinding(final Class<? extends Annotation> binding) {
            this.nameBindings.add(binding);
            return this;
        }

        /**
         * Build a new contract provider model.
         *
         * @return new contract provider model.
         */
        public ContractProvider build() {
            return new ContractProvider(scope, contracts, priority, nameBindings);
        }
    }

    private final Set<Class<?>> contracts;
    private final int priority;
    private final Set<Class<? extends Annotation>> nameBindings;
    private final Class<? extends Annotation> scope;

    private ContractProvider(
            final Class<? extends Annotation> scope,
            final Set<Class<?>> contracts,
            final int priority,
            final Set<Class<? extends Annotation>> nameBindings) {

        this.scope = scope;
        this.contracts = contracts;
        this.priority = priority;
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
     * @see org.glassfish.jersey.spi.Contract
     */
    public Set<Class<?>> getContracts() {
        return contracts;
    }

    @Override
    public boolean isNameBound() {
        return !nameBindings.isEmpty();
    }

    /**
     * Get the provider priority, if set, {@code -1} if not set.
     *
     * @return provider priority.
     * @see javax.ws.rs.BindingPriority
     */
    public int getPriority() {
        return priority;
    }

    @Override
    public Set<Class<? extends Annotation>> getNameBindings() {
        return nameBindings;
    }
}
