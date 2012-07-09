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
package org.glassfish.jersey.internal.inject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.glassfish.hk2.api.Descriptor;
import org.glassfish.hk2.api.DynamicConfiguration;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.FactoryDescriptors;
import org.glassfish.hk2.api.HK2Loader;
import org.glassfish.hk2.api.TypeLiteral;
import org.glassfish.hk2.utilities.AbstractActiveDescriptor;
import org.glassfish.hk2.utilities.ActiveDescriptorBuilder;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.glassfish.hk2.utilities.reflection.ParameterizedTypeImpl;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * Abstract binding builder implementation.
 *
 * @param <T> bound service type.
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
abstract class AbstractBindingBuilder<T> implements
        ServiceBindingBuilder<T>, NamedBindingBuilder<T>, ScopedBindingBuilder<T>, ScopedNamedBindingBuilder<T> {

    /**
     * Contracts the service should be bound to.
     */
    Set<Type> contracts = new HashSet<Type>();
    /**
     * Bound service loader.
     */
    HK2Loader loader = null;
    /**
     * Binding metadata (e.g. useful for filtering).
     */
    Multimap<String, String> metadata = HashMultimap.create();
    /**
     * Qualifiers (other than @Named).
     */
    Set<Annotation> qualifiers = new HashSet<Annotation>();
    /**
     * Binding scope.
     */
    Class<? extends Annotation> scope = null;
    /**
     * Binding rank.
     */
    Integer ranked = null;
    /**
     * Binding name (see @Named).
     */
    String name = null;

    @Override
    public AbstractBindingBuilder<T> to(Class<? super T> contract) {
        contracts.add(contract);
        return this;
    }

    @Override
    public AbstractBindingBuilder<T> to(TypeLiteral<?> contract) {
        contracts.add(contract.getType());
        return this;
    }

    @Override
    public AbstractBindingBuilder<T> loadedBy(HK2Loader loader) {
        this.loader = loader;
        return this;
    }

    @Override
    public AbstractBindingBuilder<T> withMetadata(String key, String value) {
        this.metadata.put(key, value);
        return this;
    }

    @Override
    public AbstractBindingBuilder<T> withMetadata(String key, List<String> values) {
        this.metadata.putAll(key, values);
        return this;
    }

    @Override
    public AbstractBindingBuilder<T> qualifiedBy(Annotation annotation) {
        this.qualifiers.add(annotation);
        return this;
    }

    @Override
    public AbstractBindingBuilder<T> in(Class<? extends Annotation> scopeAnnotation) {
        this.scope = scopeAnnotation;
        return this;
    }

    @Override
    public AbstractBindingBuilder<T> named(String name) {
        this.name = name;
        return this;
    }

    @Override
    public void ranked(int rank) {
        this.ranked = rank;
    }

    /**
     * Build the binding descriptor and bind it in the {@link DynamicConfiguration
     * dynamic configuration}.
     *
     * @param configuration dynamic binding configuration.
     * @param defaultLoader default HK2 loader that should be used in case a custom loader
     *                      was not set.
     */
    abstract void complete(DynamicConfiguration configuration, HK2Loader defaultLoader);

    private static class ClassBasedBindingBuilder<T> extends AbstractBindingBuilder<T> {
        private final Class<T> service;

        public ClassBasedBindingBuilder(Class<T> service, Type serviceContractType) {
            this.service = service;
            if (serviceContractType != null) {
                super.contracts.add(serviceContractType);
            }
        }

        @Override
        void complete(final DynamicConfiguration configuration, final HK2Loader defaultLoader) {
            if (this.loader == null) {
                this.loader = defaultLoader;
            }

            ActiveDescriptorBuilder builder = BuilderHelper.activeLink(service)
                    .named(name)
                    .andLoadWith(this.loader);

            if (scope != null) {
                builder.in(scope);
            }

            if (ranked != null) {
                builder.ofRank(ranked);
            }

            for (String key : metadata.keySet()) {
                for (String value : metadata.get(key)) {
                    builder.has(key, value);
                }
            }

            for (Annotation annotation : qualifiers) {
                builder.qualifiedBy(annotation);
            }

            for (Type contract : contracts) {
                builder.to(contract);
            }

            configuration.bind(builder.build());
        }
    }

    private static class InstanceBasedBindingBuilder<T> extends AbstractBindingBuilder<T> {
        private final T service;

        public InstanceBasedBindingBuilder(T service) {
            this.service = service;
        }

        @Override
        void complete(DynamicConfiguration configuration, HK2Loader defaultLoader) {
            if (this.loader == null) {
                this.loader = defaultLoader;
            }
            AbstractActiveDescriptor descriptor = BuilderHelper.createConstantDescriptor(service);
            descriptor.setName(name);
            descriptor.setLoader(this.loader);

            if (scope != null) {
                descriptor.setScope(scope.getName());
            }

            if (ranked != null) {
                descriptor.setRanking(ranked);
            }

            for (String key : metadata.keySet()) {
                for (String value : metadata.get(key)) {
                    descriptor.addMetadata(key, value);
                }
            }

            for (Annotation annotation : qualifiers) {
                descriptor.addQualifierAnnotation(annotation);
            }

            for (Type contract : contracts) {
                descriptor.addContractType(contract);
            }

            configuration.bind(descriptor);
        }
    }

    private static class FactoryInstanceBasedBindingBuilder<T> extends AbstractBindingBuilder<T> {
        private final Factory<T> factory;

        public FactoryInstanceBasedBindingBuilder(Factory<T> factory) {
            this.factory = factory;
        }

        @Override
        void complete(DynamicConfiguration configuration, HK2Loader defaultLoader) {
            if (this.loader == null) {
                this.loader = defaultLoader;
            }

            AbstractActiveDescriptor factoryContractDescriptor = BuilderHelper.createConstantDescriptor(factory);
            factoryContractDescriptor.addContractType(factory.getClass());
            factoryContractDescriptor.setName(name);
            factoryContractDescriptor.setLoader(this.loader);

            ActiveDescriptorBuilder descriptorBuilder = BuilderHelper.activeLink(factory.getClass())
                    .named(name)
                    .andLoadWith(this.loader);
            if (scope != null) {
                descriptorBuilder.in(scope);
            }

            if (ranked != null) {
                descriptorBuilder.ofRank(ranked);
            }

            for (Annotation qualifier : qualifiers) {
                factoryContractDescriptor.addQualifierAnnotation(qualifier);
                descriptorBuilder.qualifiedBy(qualifier);
            }

            for (Type contract : contracts) {
                factoryContractDescriptor.addContractType(new ParameterizedTypeImpl(Factory.class, contract));
                descriptorBuilder.to(contract);
            }

            configuration.bind(new FactoryDescriptorsImpl(
                    factoryContractDescriptor,
                    descriptorBuilder.buildFactory()));
        }
    }

    private static class FactoryTypeBasedBindingBuilder<T> extends AbstractBindingBuilder<T> {
        private final Class<? extends Factory<T>> factoryClass;
        private final Class<? extends Annotation> factoryScope;

        public FactoryTypeBasedBindingBuilder(Class<? extends Factory<T>> factoryClass, Class<? extends Annotation> factoryScope) {
            this.factoryClass = factoryClass;
            this.factoryScope = factoryScope;
        }

        @Override
        void complete(DynamicConfiguration configuration, HK2Loader defaultLoader) {
            if (this.loader == null) {
                this.loader = defaultLoader;
            }

            ActiveDescriptorBuilder factoryDescriptorBuilder = BuilderHelper.activeLink(factoryClass)
                    .named(name)
                    .andLoadWith(this.loader);
            if (factoryScope != null) {
                factoryDescriptorBuilder.in(factoryScope);
            }

            ActiveDescriptorBuilder descriptorBuilder = BuilderHelper.activeLink(factoryClass)
                    .named(name)
                    .andLoadWith(this.loader);
            if (scope != null) {
                descriptorBuilder.in(scope);
            }

            if (ranked != null) {
//                factoryContractDescriptor.ofRank(factoryRank);
                descriptorBuilder.ofRank(ranked);
            }

            for (Annotation qualifier : qualifiers) {
                factoryDescriptorBuilder.qualifiedBy(qualifier);
                descriptorBuilder.qualifiedBy(qualifier);
            }

            for (Type contract : contracts) {
                factoryDescriptorBuilder.to(new ParameterizedTypeImpl(Factory.class, contract)) ;
                descriptorBuilder.to(contract);
            }

            configuration.bind(new FactoryDescriptorsImpl(
                    factoryDescriptorBuilder.build(),
                    descriptorBuilder.buildFactory()));
        }
    }

    private static class FactoryDescriptorsImpl implements FactoryDescriptors {
        private final Descriptor serviceDescriptor;
        private final Descriptor factoryDescriptor;

        public FactoryDescriptorsImpl(Descriptor serviceDescriptor, Descriptor factoryDescriptor) {
            this.serviceDescriptor = serviceDescriptor;
            this.factoryDescriptor = factoryDescriptor;
        }

        @Override
        public Descriptor getFactoryAsAService() {
            return serviceDescriptor;
        }

        @Override
        public Descriptor getFactoryAsAFactory() {
            return factoryDescriptor;
        }

        public String toString() {
            return "FactoryDescriptorsImpl(\n" +
                    serviceDescriptor + ",\n" + factoryDescriptor + ",\n\t" + System.identityHashCode(this) + ")";
        }
    }

    /**
     * TODO javadoc.
     */
    static <T> AbstractBindingBuilder<T> create(Class<T> serviceType, boolean bindAsContract) {
        return new ClassBasedBindingBuilder<T>(serviceType, bindAsContract ? serviceType : null);
    }

    /**
     * TODO javadoc.
     */
    static <T> AbstractBindingBuilder<T> create(TypeLiteral<T> serviceType, boolean bindAsContract) {
        return new ClassBasedBindingBuilder<T>(serviceType.getRawType(), bindAsContract ? serviceType.getType() : null);
    }

    /**
     * TODO javadoc.
     */
    static <T> AbstractBindingBuilder<T> create(T service) {
        return new InstanceBasedBindingBuilder<T>(service);
    }

    /**
     * TODO javadoc.
     */
    static <T> AbstractBindingBuilder<T> createFactoryBinder(Factory<T> factory) {
        return new FactoryInstanceBasedBindingBuilder<T>(factory);
    }

    /**
     * TODO javadoc.
     */
    static <T> AbstractBindingBuilder<T> createFactoryBinder(Class<? extends Factory<T>> factoryType, Class<? extends Annotation> factoryScope) {
        return new FactoryTypeBasedBindingBuilder<T>(factoryType, factoryScope);
    }
}
