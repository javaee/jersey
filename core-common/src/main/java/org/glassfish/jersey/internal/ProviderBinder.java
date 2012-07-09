/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.internal;

import java.lang.annotation.Annotation;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.glassfish.jersey.internal.inject.AbstractModule;
import org.glassfish.jersey.internal.inject.Custom;
import org.glassfish.jersey.internal.inject.CustomAnnotationImpl;
import org.glassfish.jersey.internal.inject.Providers;

import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.DynamicConfiguration;
import org.glassfish.hk2.api.DynamicConfigurationService;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.AbstractActiveDescriptor;
import org.glassfish.hk2.utilities.ActiveDescriptorBuilder;
import org.glassfish.hk2.utilities.AliasDescriptor;
import org.glassfish.hk2.utilities.BuilderHelper;

/**
 * Class used for registration of the custom providers into HK2 services.
 * <p>
 * Custom providers are classes that implements specific JAX-RS or Jersey
 * SPI interfaces (e.g. {@link javax.ws.rs.ext.MessageBodyReader} and are
 * supplied by the user. These providers will be bound into the HK2 services
 * annotated by a {@link Custom &#64;Custom} qualifier annotation.
 * </p>
 * <p>
 * Use the {@code &#64;Custom} qualifier annotation to retrieve these providers
 * from HK2 services. For example:
 * </p>
 * <pre>
 *  Collection&lt;Provider&lt;MessageBodyReader&gt;&gt; hk2Providers =
 *          services.forContract(MessageBodyReader.class)
 *                  .annotatedWith(Custom.class)
 *                  .all();
 * </pre>
 * <p>
 * You may also use a one of the provider accessor utility method defined in
 * {@link Providers} class.
 * </p>
 *
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class ProviderBinder {
    @Inject
    ServiceLocator locator;

    /**
     * Register/bind custom provider instances. Registered providers will be handled
     * always as Singletons.
     *
     * @param instances custom provider instances.
     */
    // TODO remove the method if not used?
    public <T> void bindInstances(T... instances) {
        if (instances != null && instances.length > 0) {
            DynamicConfigurationService dcs = locator.getService(DynamicConfigurationService.class);
            DynamicConfiguration dc = dcs.createDynamicConfiguration();
            for (T instance : instances) {
                bindInstance(instance, dc);
            }
            dc.commit();
        }
    }

    /**
     * Register/bind custom provider instances. Registered providers will be handled
     * always as Singletons.
     *
     * @param instances custom provider instances.
     */
    public void bindInstances(Set<Object> instances) {
        DynamicConfigurationService dcs = locator.getService(DynamicConfigurationService.class);
        DynamicConfiguration dc = dcs.createDynamicConfiguration();
        for (Object instance : instances) {
            bindInstance(instance, dc);
        }
        dc.commit();
    }


    /**
     * Register/bind custom provider classes. Registered providers will be handled
     * always as Singletons unless annotated by {@link org.glassfish.jersey.spi.PerLookup}.
     *
     * @param classes custom provider classes.
     */
    public void bindClasses(Class<?>... classes) {
        if (classes != null && classes.length > 0) {
            DynamicConfigurationService dcs = locator.getService(DynamicConfigurationService.class);
            DynamicConfiguration dc = dcs.createDynamicConfiguration();
            for (Class<?> clazz : classes) {
                bindClass(clazz, locator, dc, false);
            }
            dc.commit();
        }
    }

    /**
     * Register/bind custom provider classes. Registered providers will be handled
     * always as Singletons unless annotated by {@link org.glassfish.jersey.spi.PerLookup}.
     *
     * @param classes custom provider classes.
     */
    public void bindClasses(Iterable<Class<?>> classes) {
        bindClasses(classes, false);
    }

    /**
     * Register/bind custom provider classes that may also be resources. Registered
     * providers/resources will be handled always as Singletons unless annotated by
     * {@link org.glassfish.jersey.spi.PerLookup}.
     *
     * <p>
     * If {@code bindAsResources} is set to {@code true}, the providers will also be bound
     * as resources.
     * </p>
     *
     * @param classes         custom provider classes.
     * @param bindResources if {@code true}, the provider classes will also be bound as
     *                        resources.
     */
    public void bindClasses(Iterable<Class<?>> classes, boolean bindResources) {
        if (classes == null || !classes.iterator().hasNext()) {
            return;
        }

        DynamicConfigurationService dcs = locator.getService(DynamicConfigurationService.class);
        DynamicConfiguration dc = dcs.createDynamicConfiguration();
        for (Class<?> clazz : classes) {
            bindClass(clazz, locator, dc, bindResources);
        }
        dc.commit();
    }

    @SuppressWarnings("unchecked")
    private <T> void bindInstance(T instance, DynamicConfiguration dc) {
        for (Class contract : Providers.getProviderContracts(instance.getClass())) {
            AbstractActiveDescriptor<T> descriptor = BuilderHelper.createConstantDescriptor(instance);
            descriptor.addContractType(contract);
            descriptor.addQualifierAnnotation(new CustomAnnotationImpl());
            dc.bind(descriptor);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void bindClass(Class<T> clazz, ServiceLocator locator, DynamicConfiguration dc, boolean isResource) {
        Class<? extends Annotation> scope = getProviderScope(clazz);

        if (isResource) {
            final ActiveDescriptor<?> descriptor = dc.bind(BuilderHelper.activeLink(clazz).to(clazz).in(scope).build());

            for (Class contract : Providers.getProviderContracts(clazz)) {
                AliasDescriptor aliasDescriptor = new AliasDescriptor(locator, descriptor, contract.getName(), null);
                aliasDescriptor.setScope(scope.getName());
                aliasDescriptor.addQualifierAnnotation(new CustomAnnotationImpl());

                dc.bind(aliasDescriptor);
            }
        } else {
            final ActiveDescriptorBuilder descriptorBuilder =
                    BuilderHelper.activeLink(clazz).in(scope).qualifiedBy(new CustomAnnotationImpl());
            for (Class contract : Providers.getProviderContracts(clazz)) {
                descriptorBuilder.to(contract);
            }
            dc.bind(descriptorBuilder.build());
        }

    }

    private Class<? extends Annotation> getProviderScope(Class<?> clazz) {
        Class<? extends Annotation> hk2Scope = Singleton.class;
        if (clazz.isAnnotationPresent(org.glassfish.jersey.spi.PerLookup.class)) {
            hk2Scope = PerLookup.class;
        }
        return hk2Scope;
    }

    /**
     * Module which registers {@link ProviderBinder} into the the HK2 services.
     */
    public static class ProviderBinderModule extends AbstractModule {

        @Override
        protected void configure() {
//            bind(ProviderBinder.class).to(ProviderBinder.class).in(PerLookup.class);
            bind(BuilderHelper.link(ProviderBinder.class).in(PerLookup.class).build());
        }
    }
}
