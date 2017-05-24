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
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.glassfish.jersey.internal.LocalizationMessages;
import org.glassfish.jersey.internal.inject.AliasBinding;
import org.glassfish.jersey.internal.inject.Binding;
import org.glassfish.jersey.internal.inject.Bindings;
import org.glassfish.jersey.internal.inject.ClassBinding;
import org.glassfish.jersey.internal.inject.DisposableSupplier;
import org.glassfish.jersey.internal.inject.InjectionResolverBinding;
import org.glassfish.jersey.internal.inject.InstanceBinding;
import org.glassfish.jersey.internal.inject.PerLookup;
import org.glassfish.jersey.internal.inject.PerThread;
import org.glassfish.jersey.internal.inject.SupplierClassBinding;
import org.glassfish.jersey.internal.inject.SupplierInstanceBinding;

import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.DynamicConfiguration;
import org.glassfish.hk2.api.DynamicConfigurationService;
import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.AbstractActiveDescriptor;
import org.glassfish.hk2.utilities.ActiveDescriptorBuilder;
import org.glassfish.hk2.utilities.Binder;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.hk2.utilities.binding.ServiceBindingBuilder;
import org.glassfish.hk2.utilities.reflection.ParameterizedTypeImpl;

/**
 * This class contains the convenient methods for translation from jersey classes to HK2 and visa versa, then contains methods
 * for binding structures such as {@link org.glassfish.jersey.internal.inject.Binder} or {@link Binding} to a provided service
 * locator.
 */
class Hk2Helper {

    /**
     * Bind a translated Jersey-like {@link org.glassfish.jersey.internal.inject.Binder} to HK2-like {@link Binder}.
     *
     * @param injectionManager HK2 injection manager.
     * @param jerseyBinder     Jersey-like binder.
     */
    static void bind(AbstractHk2InjectionManager injectionManager, org.glassfish.jersey.internal.inject.Binder jerseyBinder) {
        bind(injectionManager.getServiceLocator(), Bindings.getBindings(injectionManager, jerseyBinder));
    }

    /**
     * Bind descriptors to Hk2-like {@link Binder}.
     *
     * @param locator HK2 locator.
     * @param binding single descriptor.
     */
    static void bind(ServiceLocator locator, Binding binding) {
        bindBinding(locator, binding);
    }

    /**
     * Bind descriptors to Hk2-like {@link Binder}.
     *
     * @param locator     HK2 locator.
     * @param descriptors collection of descriptors.
     */
    static void bind(ServiceLocator locator, Iterable<Binding> descriptors) {
        DynamicConfiguration dc = getDynamicConfiguration(locator);
        for (Binding binding : descriptors) {
            bindBinding(locator, dc, binding);
        }
        dc.commit();
    }

    /**
     * Gets {@link DynamicConfigurationService} object from {@link ServiceLocator HK2 Locator} and creates a new object of
     * {@link DynamicConfiguration} to bind new services.
     *
     * @param locator HK2 locator.
     * @return new instance of {@code DynamicConfiguration} to bind new services.
     */
    private static DynamicConfiguration getDynamicConfiguration(ServiceLocator locator) {
        DynamicConfigurationService dcs = locator.getService(DynamicConfigurationService.class);
        return dcs.createDynamicConfiguration();
    }

    /**
     * Binds the single descriptor using a single {@link DynamicConfiguration}.
     *
     * @param locator HK2 injection manager.
     * @param binding Jersey descriptor as a holder of information about an injection point.
     */
    private static void bindBinding(ServiceLocator locator, Binding<?, ?> binding) {
        DynamicConfiguration dc = getDynamicConfiguration(locator);
        bindBinding(locator, dc, binding);
        dc.commit();
    }

    /**
     * Binds the single descriptor using an external {@link DynamicConfiguration}.
     *
     * @param locator HK2 injection manager.
     * @param dc      HK2 Dynamic configuration to bind the object.
     * @param binding Jersey descriptor as a holder of information about an injection point.
     */
    private static void bindBinding(ServiceLocator locator, DynamicConfiguration dc, Binding<?, ?> binding) {
        if (ClassBinding.class.isAssignableFrom(binding.getClass())) {
            ActiveDescriptor<?> activeDescriptor = translateToActiveDescriptor((ClassBinding<?>) binding);
            bindBinding(locator, dc, activeDescriptor, binding.getAliases());

        } else if (InstanceBinding.class.isAssignableFrom(binding.getClass())) {
            ActiveDescriptor<?> activeDescriptor = translateToActiveDescriptor((InstanceBinding<?>) binding);
            bindBinding(locator, dc, activeDescriptor, binding.getAliases());

        } else if (InjectionResolverBinding.class.isAssignableFrom(binding.getClass())) {
            InjectionResolverBinding resolverDescriptor = (InjectionResolverBinding) binding;
            bindBinding(locator, dc, wrapInjectionResolver(resolverDescriptor), binding.getAliases());
            bindBinding(locator, dc, translateToActiveDescriptor(resolverDescriptor), binding.getAliases());

        } else if (SupplierClassBinding.class.isAssignableFrom(binding.getClass())) {
            bindSupplierClassBinding(locator, (SupplierClassBinding<?>) binding);

        } else if (SupplierInstanceBinding.class.isAssignableFrom(binding.getClass())) {
            bindSupplierInstanceBinding(locator, (SupplierInstanceBinding<?>) binding);

        } else {
            throw new RuntimeException(LocalizationMessages.UNKNOWN_DESCRIPTOR_TYPE(binding.getClass().getSimpleName()));
        }
    }

    @SuppressWarnings("unchecked")
    private static ActiveDescriptor<?> wrapInjectionResolver(InjectionResolverBinding resolverDescriptor) {
        InjectionResolverWrapper<?> wrappedResolver = new InjectionResolverWrapper<>(resolverDescriptor.getResolver());
        return translateToActiveDescriptor(Bindings.service(wrappedResolver),
                new ParameterizedTypeImpl(InjectionResolver.class, resolverDescriptor.getResolver().getAnnotation()));
    }

    /**
     * Registers a new instance {@link Binder} using the information from the Jersey binding {@link SupplierInstanceBinding}.
     *
     * @param locator HK2 instance manager.
     * @param binding Jersey descriptor as a holder of information about an injection point.
     */
    private static void bindSupplierInstanceBinding(ServiceLocator locator, SupplierInstanceBinding<?> binding) {
        Consumer<AbstractBinder> bindConsumer = binder -> {
            Supplier<?> supplier = binding.getSupplier();
            boolean disposable = DisposableSupplier.class.isAssignableFrom(supplier.getClass());
            // Bind the Supplier itself to be able to inject - Supplier<T> or DisposableSupplier<T>;
            // The contract of the supplier is not registered that means that the instance of the supplier can be retrieved
            // only using Supplier interface and not using implementation class itself. Supplier can be registered only once with
            // all provided contracts.
            AbstractActiveDescriptor<? extends Supplier<?>> supplierBuilder = BuilderHelper.createConstantDescriptor(supplier);
            binding.getContracts().forEach(contract -> {
                supplierBuilder.addContractType(new ParameterizedTypeImpl(Supplier.class, contract));
                if (disposable) {
                    supplierBuilder.addContractType(new ParameterizedTypeImpl(DisposableSupplier.class, contract));
                }
            });
            // Always call SupplierFactoryBridge.
            supplierBuilder.setName(binding.getName());
            binding.getQualifiers().forEach(supplierBuilder::addQualifierAnnotation);
            binder.bind(supplierBuilder);

            // Register wrapper for factory functionality, wrapper automatically call service locator which is able to retrieve
            // the service in the proper context and scope. Bridge is registered for all contracts but is able to lookup from
            // service locator only using the first contract.
            ServiceBindingBuilder<?> builder = binder.bindFactory(new InstanceSupplierFactoryBridge<>(supplier, disposable));
            setupSupplierFactoryBridge(binding, builder);
        };

        ServiceLocatorUtilities.bind(locator, createBinder(bindConsumer));
    }

    /**
     * Registers a new instance {@link Binder} using the information from the Jersey binding {@link SupplierClassBinding}.
     *
     * @param locator HK2 instance manager.
     * @param binding Jersey descriptor as a holder of information about an injection point.
     */
    private static void bindSupplierClassBinding(ServiceLocator locator, SupplierClassBinding<?> binding) {
        Consumer<AbstractBinder> bindConsumer = binder -> {
            boolean disposable = DisposableSupplier.class.isAssignableFrom(binding.getSupplierClass());

            // Bind the Supplier itself to be able to inject - Supplier<T> supplier;
            // The contract of the supplier is not registered that means that the instance of the supplier can be retrieved
            // only using Supplier interface and not using implementation class itself. Supplier can be registered only once with
            // all provided contracts.
            ServiceBindingBuilder<?> supplierBuilder = binder.bind(binding.getSupplierClass());
            binding.getContracts().forEach(contract -> {
                supplierBuilder.to(new ParameterizedTypeImpl(Supplier.class, contract));
                if (disposable) {
                    supplierBuilder.to(new ParameterizedTypeImpl(DisposableSupplier.class, contract));
                }
            });
            binding.getQualifiers().forEach(supplierBuilder::qualifiedBy);
            supplierBuilder.named(binding.getName());
            supplierBuilder.in(transformScope(binding.getSupplierScope()));
            binder.bind(supplierBuilder);

            // Register wrapper for factory functionality, wrapper automatically call service locator which is able to retrieve
            // the service in the proper context and scope. Bridge is registered for all contracts but is able to lookup from
            // service locator only using the first contract.
            Type contract = null;
            if (binding.getContracts().iterator().hasNext()) {
                contract = binding.getContracts().iterator().next();
            }

            ServiceBindingBuilder<?> builder = binder.bindFactory(
                    new SupplierFactoryBridge<>(locator, contract, binding.getName(), disposable));
            setupSupplierFactoryBridge(binding, builder);
            if (binding.getImplementationType() != null) {
                builder.asType(binding.getImplementationType());
            }
        };

        ServiceLocatorUtilities.bind(locator, createBinder(bindConsumer));
    }

    private static void setupSupplierFactoryBridge(Binding<?, ?> binding, ServiceBindingBuilder<?> builder) {
        builder.named(binding.getName());
        binding.getContracts().forEach(builder::to);
        binding.getQualifiers().forEach(builder::qualifiedBy);
        builder.in(transformScope(binding.getScope()));

        if (binding.getRank() != null) {
            builder.ranked(binding.getRank());
        }

        if (binding.isProxiable() != null) {
            builder.proxy(binding.isProxiable());
        }

        if (binding.isProxiedForSameScope() != null) {
            builder.proxyForSameScope(binding.isProxiedForSameScope());
        }
    }

    static ActiveDescriptor<?> translateToActiveDescriptor(ClassBinding<?> desc) {
        ActiveDescriptorBuilder binding = BuilderHelper.activeLink(desc.getService()).named(desc.getName())
                .analyzeWith(desc.getAnalyzer());

        if (desc.getScope() != null) {
            binding.in(transformScope(desc.getScope()));
        }

        if (desc.getRank() != null) {
            binding.ofRank(desc.getRank());
        }

        for (Annotation annotation : desc.getQualifiers()) {
            binding.qualifiedBy(annotation);
        }

        for (Type contract : desc.getContracts()) {
            binding.to(contract);
        }

        if (desc.isProxiable() != null) {
            binding.proxy(desc.isProxiable());
        }

        if (desc.isProxiedForSameScope() != null) {
            binding.proxyForSameScope(desc.isProxiedForSameScope());
        }

        if (desc.getImplementationType() != null) {
            binding.asType(desc.getImplementationType());
        }

        return binding.build();
    }

    /**
     * Binds a new instance {@link Binding} using the information from the Jersey descriptor {@link InstanceBinding}.
     * <p>
     * Along with a new instance, the method is able to register aliases belonging to the new service.
     *
     * @param locator          HK2 injection manager.
     * @param dc               HK2 Dynamic configuration to bind the object.
     * @param activeDescriptor HK2 active descriptor.
     * @param aliases          aliases belonging to the given descriptor.
     */
    private static void bindBinding(ServiceLocator locator, DynamicConfiguration dc, ActiveDescriptor<?> activeDescriptor,
            Set<AliasBinding> aliases) {
        ActiveDescriptor<Object> boundDescriptor = dc.bind(activeDescriptor);
        for (AliasBinding alias : aliases) {
            dc.bind(createAlias(locator, boundDescriptor, alias));
        }
    }

    static ActiveDescriptor<?> translateToActiveDescriptor(InstanceBinding<?> desc, Type... contracts) {
        AbstractActiveDescriptor<?> binding;
        if (contracts.length == 0) {
            binding = BuilderHelper.createConstantDescriptor(desc.getService());
        } else {
            binding = BuilderHelper.createConstantDescriptor(desc.getService(), null, contracts);
        }

        binding.setName(desc.getName());
        binding.setClassAnalysisName(desc.getAnalyzer());

        if (desc.getScope() != null) {
            binding.setScope(desc.getScope().getName());
        }

        if (desc.getRank() != null) {
            binding.setRanking(desc.getRank());
        }

        for (Annotation annotation : desc.getQualifiers()) {
            binding.addQualifierAnnotation(annotation);
        }

        for (Type contract : desc.getContracts()) {
            binding.addContractType(contract);
        }

        if (desc.isProxiable() != null) {
            binding.setProxiable(desc.isProxiable());
        }

        if (desc.isProxiedForSameScope() != null) {
            binding.setProxyForSameScope(desc.isProxiedForSameScope());
        }

        return binding;
    }

    private static ActiveDescriptor<?> translateToActiveDescriptor(InjectionResolverBinding<?> desc) {
        ParameterizedTypeImpl parameterizedType = new ParameterizedTypeImpl(
                org.glassfish.jersey.internal.inject.InjectionResolver.class, desc.getResolver().getAnnotation());

        return BuilderHelper.createConstantDescriptor(desc.getResolver(), null, parameterizedType);
    }

    /**
     * Creates the alias object to a provided descriptor.
     *
     * @param locator    locator used to create an alias.
     * @param descriptor descriptor which the alias belongs to.
     * @param alias      source of the alias information.
     * @return populated alias object, ready to bindBinder using {@link DynamicConfiguration}.
     */
    private static org.glassfish.hk2.utilities.AliasDescriptor<?> createAlias(ServiceLocator locator,
            ActiveDescriptor<?> descriptor, AliasBinding alias) {
        org.glassfish.hk2.utilities.AliasDescriptor<?> hk2Alias = new org.glassfish.hk2.utilities.AliasDescriptor<>(locator,
                descriptor, alias.getContract().getName(), null);
        alias.getQualifiers().forEach(hk2Alias::addQualifierAnnotation);
        alias.getScope().ifPresent(hk2Alias::setScope);
        alias.getRank().ifPresent(hk2Alias::setRanking);
        return hk2Alias;
    }

    /**
     * Creates a new binder and automatically use it to bind the the descriptors in {@code bindConsumer}.
     *
     * @param bindConsumer consumer used to process the defined operation with a binder.
     * @return populated binder.
     */
    private static Binder createBinder(Consumer<AbstractBinder> bindConsumer) {
        return new AbstractBinder() {
            @Override
            protected void configure() {
                bindConsumer.accept(this);
            }
        };
    }

    /**
     * Transforms Jersey scopes/annotations to HK2 equivalents.
     *
     * @param scope Jersey scope/annotation.
     * @return HK2 equivalent scope/annotation.
     */
    private static Class<? extends Annotation> transformScope(Class<? extends Annotation> scope) {
        if (scope == PerLookup.class) {
            return org.glassfish.hk2.api.PerLookup.class;
        } else if (scope == PerThread.class) {
            return org.glassfish.hk2.api.PerThread.class;
        }
        return scope;
    }
}
