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

package org.glassfish.jersey.hk2;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import org.glassfish.jersey.internal.LocalizationMessages;
import org.glassfish.jersey.spi.inject.AliasDescriptor;
import org.glassfish.jersey.spi.inject.ClassBeanDescriptor;
import org.glassfish.jersey.spi.inject.ClassFactoryDescriptor;
import org.glassfish.jersey.spi.inject.Descriptor;
import org.glassfish.jersey.spi.inject.InstanceBeanDescriptor;
import org.glassfish.jersey.spi.inject.InstanceFactoryDescriptor;

import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.DynamicConfiguration;
import org.glassfish.hk2.api.DynamicConfigurationService;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.AbstractActiveDescriptor;
import org.glassfish.hk2.utilities.ActiveDescriptorBuilder;
import org.glassfish.hk2.utilities.Binder;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.glassfish.hk2.utilities.FactoryDescriptorsImpl;
import org.glassfish.hk2.utilities.reflection.ParameterizedTypeImpl;

class Hk2Helper {

    /**
     * Bind a translated Jersey-like {@link org.glassfish.jersey.spi.inject.Binder} to HK2-like {@link Binder}.
     *
     * @param locator      HK2 locator.
     * @param jerseyBinder Jersey-like binder.
     */
    static void bind(ServiceLocator locator, org.glassfish.jersey.spi.inject.Binder jerseyBinder) {
        bind(locator, jerseyBinder.getDescriptors());
    }

    /**
     * Bind descriptors to Hk2-like {@link Binder}.
     *
     * @param locator    HK2 locator.
     * @param descriptor single descriptor.
     */
    static void bind(ServiceLocator locator, Descriptor descriptor) {
        bindDescriptor(locator, descriptor);
    }

    /**
     * Bind descriptors to Hk2-like {@link Binder}.
     *
     * @param locator     HK2 locator.
     * @param descriptors collection of descriptors.
     */
    static void bind(ServiceLocator locator, Iterable<Descriptor> descriptors) {
        DynamicConfiguration dc = getDynamicConfiguration(locator);
        for (Descriptor descriptor : descriptors) {
            bindDescriptor(locator, dc, descriptor);
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
     * @param locator    HK2 instance manager.
     * @param descriptor Jersey descriptor as a holder of information about an injection point.
     */
    private static void bindDescriptor(ServiceLocator locator, Descriptor<?, ?> descriptor) {
        DynamicConfiguration dc = getDynamicConfiguration(locator);
        bindDescriptor(locator, dc, descriptor);
        dc.commit();
    }

    /**
     * Binds the single descriptor using an external {@link DynamicConfiguration}.
     *
     * @param locator    HK2 instance manager.
     * @param dc         HK2 Dynamic configuration to bind the object.
     * @param descriptor Jersey descriptor as a holder of information about an injection point.
     */
    private static void bindDescriptor(ServiceLocator locator, DynamicConfiguration dc, Descriptor<?, ?> descriptor) {
        if (ClassBeanDescriptor.class.isAssignableFrom(descriptor.getClass())) {
            bindClassDescriptor(locator, dc, (ClassBeanDescriptor<?>) descriptor);

        } else if (InstanceBeanDescriptor.class.isAssignableFrom(descriptor.getClass())) {
            bindInstanceDescriptor(locator, dc, (InstanceBeanDescriptor<?>) descriptor);

        } else if (ClassFactoryDescriptor.class.isAssignableFrom(descriptor.getClass())) {
            bindClassFactoryDescriptor(dc, (ClassFactoryDescriptor<?>) descriptor);

        } else if (InstanceFactoryDescriptor.class.isAssignableFrom(descriptor.getClass())) {
            bindInstanceFactoryDescriptor(dc, (InstanceFactoryDescriptor<?>) descriptor);

        } else {
            throw new RuntimeException(LocalizationMessages.UNKNOWN_DESCRIPTOR_TYPE(descriptor.getClass().getSimpleName()));
        }
    }

    /**
     * Binds a new instance {@link org.glassfish.hk2.api.FactoryDescriptors} using the information from the Jersey descriptor
     * {@link InstanceFactoryDescriptor}.
     *
     * @param dc   HK2 Dynamic configuration to bind the object.
     * @param desc Jersey descriptor as a holder of information about an injection point.
     */
    private static void bindInstanceFactoryDescriptor(DynamicConfiguration dc, InstanceFactoryDescriptor<?> desc) {
        AbstractActiveDescriptor<?> factoryContractDescriptor = BuilderHelper.createConstantDescriptor(desc.getFactory());
        factoryContractDescriptor.addContractType(desc.getFactory().getClass());

        ActiveDescriptorBuilder binding = BuilderHelper.activeLink(desc.getFactory().getClass())
                .named(desc.getName())
                .analyzeWith(desc.getAnalyzer());

        if (desc.getScope() != null) {
            binding.in(desc.getScope());
        }

        if (desc.getRank() != null) {
            binding.ofRank(desc.getRank());
        }

        for (Annotation qualifier : desc.getQualifiers()) {
            factoryContractDescriptor.addQualifierAnnotation(qualifier);
            binding.qualifiedBy(qualifier);
        }

        for (Type contract : desc.getContracts()) {
            factoryContractDescriptor.addContractType(new ParameterizedTypeImpl(Factory.class, contract));
            binding.to(contract);
        }

        if (desc.isProxiable() != null) {
            binding.proxy(desc.isProxiable());
        }

        if (desc.isProxiedForSameScope() != null) {
            binding.proxyForSameScope(desc.isProxiedForSameScope());
        }

        dc.bind(new FactoryDescriptorsImpl(factoryContractDescriptor, binding.buildProvideMethod()));
    }

    /**
     * Binds a new instance {@link org.glassfish.hk2.api.FactoryDescriptors} using the information from the Jersey descriptor
     * {@link ClassFactoryDescriptor}.
     *
     * @param dc   HK2 Dynamic configuration to bind the object.
     * @param desc Jersey descriptor as a holder of information about an injection point.
     */
    private static void bindClassFactoryDescriptor(DynamicConfiguration dc, ClassFactoryDescriptor<?> desc) {
        AbstractActiveDescriptor<?> factoryContractDescriptor = BuilderHelper.createConstantDescriptor(desc.getFactoryClass());
        factoryContractDescriptor.addContractType(desc.getFactoryClass().getClass());

        ActiveDescriptorBuilder factoryDescriptorBuilder = BuilderHelper.activeLink(desc.getFactoryClass())
                .named(desc.getName())
                .analyzeWith(desc.getAnalyzer());

        if (desc.getFactoryScope() != null) {
            factoryDescriptorBuilder.in(desc.getFactoryScope());
        }

        ActiveDescriptorBuilder descriptorBuilder = BuilderHelper.activeLink(desc.getFactoryClass())
                .named(desc.getName())
                .analyzeWith(desc.getAnalyzer());

        if (desc.getScope() != null) {
            descriptorBuilder.in(desc.getScope());
        }

        if (desc.getRank() != null) {
            descriptorBuilder.ofRank(desc.getRank());
        }

        for (Annotation qualifier : desc.getQualifiers()) {
            factoryDescriptorBuilder.qualifiedBy(qualifier);
            descriptorBuilder.qualifiedBy(qualifier);
        }

        for (Type contract : desc.getContracts()) {
            factoryDescriptorBuilder.to(new ParameterizedTypeImpl(Factory.class, contract));
            descriptorBuilder.to(contract);
        }

        if (desc.isProxiable() != null) {
            descriptorBuilder.proxy(desc.isProxiable());
        }

        if (desc.isProxiedForSameScope() != null) {
            descriptorBuilder.proxyForSameScope(desc.isProxiedForSameScope());
        }

        dc.bind(new FactoryDescriptorsImpl(factoryDescriptorBuilder.build(), descriptorBuilder.buildProvideMethod()));
    }

    /**
     * Binds a new instance {@link Descriptor} using the information from the Jersey descriptor {@link ClassBeanDescriptor}.
     * <p>
     * Along with a new instance, the method is able to register aliases belonging to the new service.
     *
     * @param locator HK2 instance manager.
     * @param dc      HK2 Dynamic configuration to bind the object.
     * @param desc    Jersey descriptor as a holder of information about an injection point.
     */
    private static void bindClassDescriptor(ServiceLocator locator, DynamicConfiguration dc, ClassBeanDescriptor<?> desc) {
        ActiveDescriptorBuilder binding = BuilderHelper.activeLink(desc.getService())
                .named(desc.getName())
                .analyzeWith(desc.getAnalyzer());

        if (desc.getScope() != null) {
            binding.in(desc.getScope());
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

        ActiveDescriptor<Object> activeDescriptor = dc.bind(binding.build());
        for (AliasDescriptor alias : desc.getAliases()) {
            dc.bind(createAlias(locator, activeDescriptor, alias));
        }
    }

    /**
     * Binds a new instance {@link Descriptor} using the information from the Jersey descriptor {@link InstanceBeanDescriptor}.
     * <p>
     * Along with a new instance, the method is able to register aliases belonging to the new service.
     *
     * @param locator HK2 instance manager.
     * @param dc      HK2 Dynamic configuration to bind the object.
     * @param desc    Jersey descriptor as a holder of information about an injection point.
     */
    private static void bindInstanceDescriptor(ServiceLocator locator, DynamicConfiguration dc, InstanceBeanDescriptor<?> desc) {
        AbstractActiveDescriptor<?> binding = BuilderHelper.createConstantDescriptor(desc.getService());
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

        ActiveDescriptor<Object> activeDescriptor = dc.bind(binding);
        for (AliasDescriptor alias : desc.getAliases()) {
            dc.bind(createAlias(locator, activeDescriptor, alias));
        }
    }

    /**
     * Creates the alias object to a provided descriptor.
     *
     * @param locator    locator used to create an alias.
     * @param descriptor descriptor which the alias belongs to.
     * @param alias      source of the alias information.
     * @return populated alias object, ready to bindBinder using {@link DynamicConfiguration}.
     */
    private static org.glassfish.hk2.utilities.AliasDescriptor<?> createAlias(
            ServiceLocator locator, ActiveDescriptor<?> descriptor, AliasDescriptor alias) {
        org.glassfish.hk2.utilities.AliasDescriptor<?> hk2Alias =
                new org.glassfish.hk2.utilities.AliasDescriptor<>(locator, descriptor, alias.getContract(), null);
        alias.getQualifiers().forEach(hk2Alias::addQualifierAnnotation);
        alias.getScope().ifPresent(hk2Alias::setScope);
        alias.getRank().ifPresent(hk2Alias::setRanking);
        return hk2Alias;
    }
}
