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

package org.glassfish.jersey.server;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.ws.rs.RuntimeType;

import org.glassfish.jersey.internal.BootstrapBag;
import org.glassfish.jersey.internal.BootstrapConfigurator;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.ProviderBinder;
import org.glassfish.jersey.internal.inject.Providers;
import org.glassfish.jersey.model.ContractProvider;
import org.glassfish.jersey.model.internal.ComponentBag;
import org.glassfish.jersey.server.internal.JerseyResourceContext;
import org.glassfish.jersey.server.internal.LocalizationMessages;
import org.glassfish.jersey.server.model.ModelProcessor;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceModel;
import org.glassfish.jersey.server.spi.ComponentProvider;

/**
 * Configurator which binds providers and resources into {@link InjectionManager}.
 *
 * @author Petr Bouda (petr.bouda at oracle.com)
 */
public class ResourceModelConfigurator implements BootstrapConfigurator {

    private static final Logger LOGGER = Logger.getLogger(ResourceModelConfigurator.class.getName());

    @Override
    public void init(InjectionManager injectionManager, BootstrapBag bootstrapBag) {
        ServerBootstrapBag serverBag = (ServerBootstrapBag) bootstrapBag;
        Collection<ModelProcessor> modelProcessors = serverBag.getModelProcessors();
        ResourceConfig runtimeConfig = serverBag.getRuntimeConfig();
        ResourceBag resourceBag = serverBag.getResourceBag();
        ComponentBag componentBag = runtimeConfig.getComponentBag();

        // Adds all providers from resource config to InjectionManager -> BootstrapConfigurators are able to work with these
        // services and get them.
        bindProvidersAndResources(
                injectionManager, serverBag, componentBag, resourceBag.classes, resourceBag.instances, runtimeConfig);

        ResourceModel resourceModel = new ResourceModel.Builder(resourceBag.getRootResources(), false).build();
        resourceModel = processResourceModel(modelProcessors, resourceModel, runtimeConfig);

        bindEnhancingResourceClasses(injectionManager, serverBag, resourceModel, resourceBag, runtimeConfig);
        serverBag.setResourceModel(resourceModel);

        // Add newly created resource model in ResourceContext.
        serverBag.getResourceContext().setResourceModel(resourceModel);
    }

    private ResourceModel processResourceModel(Collection<ModelProcessor> modelProcessors, ResourceModel resourceModel,
            ResourceConfig runtimeConfig) {
        for (final ModelProcessor modelProcessor : modelProcessors) {
            resourceModel = modelProcessor.processResourceModel(resourceModel, runtimeConfig);
        }
        return resourceModel;
    }

    private void bindEnhancingResourceClasses(
            InjectionManager injectionManager,
            ServerBootstrapBag bootstrapBag,
            ResourceModel resourceModel,
            ResourceBag resourceBag,
            ResourceConfig runtimeConfig) {
        Set<Class<?>> newClasses = new HashSet<>();
        Set<Object> newInstances = new HashSet<>();
        for (final Resource res : resourceModel.getRootResources()) {
            newClasses.addAll(res.getHandlerClasses());
            newInstances.addAll(res.getHandlerInstances());
        }
        newClasses.removeAll(resourceBag.classes);
        newInstances.removeAll(resourceBag.instances);

        ComponentBag emptyComponentBag = ComponentBag.newInstance(input -> false);
        bindProvidersAndResources(injectionManager, bootstrapBag, emptyComponentBag, newClasses, newInstances, runtimeConfig);
    }

    private void bindProvidersAndResources(
            InjectionManager injectionManager,
            ServerBootstrapBag bootstrapBag,
            ComponentBag componentBag,
            Collection<Class<?>> resourceClasses,
            Collection<Object> resourceInstances,
            ResourceConfig runtimeConfig) {

        Collection<ComponentProvider> componentProviders = bootstrapBag.getComponentProviders().get();
        JerseyResourceContext resourceContext = bootstrapBag.getResourceContext();

        Set<Class<?>> registeredClasses = runtimeConfig.getRegisteredClasses();

        /*
         * Check the {@code component} whether it is correctly configured for client or server {@link RuntimeType runtime}.
         */
        java.util.function.Predicate<Class<?>> correctlyConfigured =
                componentClass -> Providers.checkProviderRuntime(componentClass,
                        componentBag.getModel(componentClass),
                        RuntimeType.SERVER,
                        !registeredClasses.contains(componentClass),
                        resourceClasses.contains(componentClass));

        /*
         * Check the {@code resource class} whether it is correctly configured for client or server {@link RuntimeType runtime}.
         */
        BiPredicate<Class<?>, ContractProvider> correctlyConfiguredResource =
                (resourceClass, model) -> Providers.checkProviderRuntime(
                        resourceClass,
                        model,
                        RuntimeType.SERVER,
                        !registeredClasses.contains(resourceClass),
                        true);

        Set<Class<?>> componentClasses =
                componentBag.getClasses(ComponentBag.excludeMetaProviders(injectionManager)).stream()
                        .filter(correctlyConfigured)
                        .collect(Collectors.toSet());

        // Merge programmatic resource classes with component classes.
        Set<Class<?>> classes = Collections.newSetFromMap(new IdentityHashMap<>());
        classes.addAll(componentClasses);
        classes.addAll(resourceClasses);

        // Bind classes.
        for (final Class<?> componentClass: classes) {
            ContractProvider model = componentBag.getModel(componentClass);
            if (bindWithComponentProvider(componentClass, model, componentProviders)) {
                continue;
            }

            if (resourceClasses.contains(componentClass)) {
                if (!Resource.isAcceptable(componentClass)) {
                    LOGGER.warning(LocalizationMessages.NON_INSTANTIABLE_COMPONENT(componentClass));
                    continue;
                }

                if (model != null && !correctlyConfiguredResource.test(componentClass, model)) {
                    model = null;
                }
                resourceContext.unsafeBindResource(componentClass, model);
            } else {
                ProviderBinder.bindProvider(componentClass, model, injectionManager);
            }
        }

        // Merge programmatic resource instances with other component instances.
        Set<Object> instances =
                componentBag.getInstances(ComponentBag.excludeMetaProviders(injectionManager)).stream()
                        .filter(instance -> correctlyConfigured.test(instance.getClass()))
                        .collect(Collectors.toSet());
        instances.addAll(resourceInstances);

        // Bind instances.
        for (Object component: instances) {
            ContractProvider model = componentBag.getModel(component.getClass());
            if (resourceInstances.contains(component)) {
                if (model != null && !correctlyConfiguredResource.test(component.getClass(), model)) {
                    model = null;
                }
                resourceContext.unsafeBindResource(component, model);
            } else {
                ProviderBinder.bindProvider(component, model, injectionManager);
            }
        }
    }

    private boolean bindWithComponentProvider(
            Class<?> component, ContractProvider providerModel, Iterable<ComponentProvider> componentProviders) {
        Set<Class<?>> contracts = providerModel == null ? Collections.emptySet() : providerModel.getContracts();
        for (ComponentProvider provider : componentProviders) {
            if (provider.bind(component, contracts)) {
                return true;
            }
        }
        return false;
    }
}
