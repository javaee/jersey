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

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.glassfish.jersey.internal.BootstrapBag;
import org.glassfish.jersey.internal.BootstrapConfigurator;
import org.glassfish.jersey.internal.inject.Binding;
import org.glassfish.jersey.internal.inject.Bindings;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.model.ContractProvider;
import org.glassfish.jersey.model.internal.ComponentBag;
import org.glassfish.jersey.server.model.ModelProcessor;
import org.glassfish.jersey.server.wadl.processor.OptionsMethodProcessor;

/**
 * Configurator which initializes and register {@link ModelProcessor} instances into {@link BootstrapBag}.
 *
 * @author Petr Bouda (petr.bouda at oracle.com)
 */
class ModelProcessorConfigurator implements BootstrapConfigurator {

    private static final Function<Object, ModelProcessor> CAST_TO_MODEL_PROCESSOR = ModelProcessor.class::cast;

    private static final Predicate<Binding> BINDING_MODEL_PROCESSOR_ONLY =
            binding -> binding.getContracts().contains(ModelProcessor.class);

    private static final Predicate<ContractProvider> CONTRACT_PROVIDER_MODEL_PROCESSOR_ONLY =
            provider -> provider.getContracts().contains(ModelProcessor.class);

    @Override
    public void init(InjectionManager injectionManager, BootstrapBag bootstrapBag) {
        ServerBootstrapBag serverBag = (ServerBootstrapBag) bootstrapBag;
        ResourceConfig runtimeConfig = serverBag.getRuntimeConfig();
        ComponentBag componentBag = runtimeConfig.getComponentBag();

        OptionsMethodProcessor optionsMethodProcessor = new OptionsMethodProcessor();
        injectionManager.register(Bindings.service(optionsMethodProcessor).to(ModelProcessor.class));

        // Get all model processors, registered as an instance or class
        List<ModelProcessor> modelProcessors =
                Stream.concat(
                        componentBag.getClasses(CONTRACT_PROVIDER_MODEL_PROCESSOR_ONLY).stream()
                                .map(injectionManager::createAndInitialize),
                        componentBag.getInstances(CONTRACT_PROVIDER_MODEL_PROCESSOR_ONLY).stream())
                        .map(CAST_TO_MODEL_PROCESSOR)
                        .collect(Collectors.toList());
        modelProcessors.add(optionsMethodProcessor);

        // model processors registered using binders
        List<ModelProcessor> modelProcessorsFromBinders = ComponentBag
                .getFromBinders(injectionManager, componentBag, CAST_TO_MODEL_PROCESSOR, BINDING_MODEL_PROCESSOR_ONLY);
        modelProcessors.addAll(modelProcessorsFromBinders);

        serverBag.setModelProcessors(modelProcessors);
    }
}
