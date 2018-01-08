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
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package org.glassfish.jersey.process.internal;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.glassfish.jersey.internal.BootstrapConfigurator;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.model.internal.ComponentBag;
import org.glassfish.jersey.spi.ExecutorServiceProvider;
import org.glassfish.jersey.spi.ScheduledExecutorServiceProvider;

/**
 * Abstract Configurator which initializes and register {@link ExecutorServiceProvider} and
 * {@link ScheduledExecutorServiceProvider}.
 *
 * @author Petr Bouda
 */
public abstract class AbstractExecutorProvidersConfigurator implements BootstrapConfigurator {

    private static final Function<Object, ExecutorServiceProvider> CAST_TO_EXECUTOR_PROVIDER =
            ExecutorServiceProvider.class::cast;

    private static final Function<Object, ScheduledExecutorServiceProvider> CAST_TO_SCHEDULED_EXECUTOR_PROVIDER =
            ScheduledExecutorServiceProvider.class::cast;

    /**
     * Retrieves registered {@link ExecutorServiceProvider} and {@link ScheduledExecutorServiceProvider} by an application and
     * adds the default implementations of those interfaces to binds them into {@link InjectionManager}.
     *
     * @param injectionManager                 injection manager used for binding selected executor service providers.
     * @param componentBag                     provides executor service providers registered by an application.
     * @param defaultAsyncExecutorProvider     default implementation of {@link ExecutorServiceProvider}.
     * @param defaultScheduledExecutorProvider default implementation of {@link ScheduledExecutorServiceProvider}.
     */
    protected void registerExecutors(
            InjectionManager injectionManager,
            ComponentBag componentBag,
            ExecutorServiceProvider defaultAsyncExecutorProvider,
            ScheduledExecutorServiceProvider defaultScheduledExecutorProvider) {

        List<ExecutorServiceProvider> customExecutors =
                Stream.concat(
                        componentBag.getClasses(ComponentBag.EXECUTOR_SERVICE_PROVIDER_ONLY).stream()
                                .map(injectionManager::createAndInitialize),
                        componentBag.getInstances(ComponentBag.EXECUTOR_SERVICE_PROVIDER_ONLY).stream())
                        .map(CAST_TO_EXECUTOR_PROVIDER)
                        .collect(Collectors.toList());
        customExecutors.add(defaultAsyncExecutorProvider);

        List<ScheduledExecutorServiceProvider> customScheduledExecutors =
                Stream.concat(
                        componentBag.getClasses(ComponentBag.SCHEDULED_EXECUTOR_SERVICE_PROVIDER_ONLY).stream()
                                .map(injectionManager::createAndInitialize),
                        componentBag.getInstances(ComponentBag.SCHEDULED_EXECUTOR_SERVICE_PROVIDER_ONLY).stream())
                        .map(CAST_TO_SCHEDULED_EXECUTOR_PROVIDER)
                        .collect(Collectors.toList());
        customScheduledExecutors.add(defaultScheduledExecutorProvider);

        ExecutorProviders.registerExecutorBindings(injectionManager, customExecutors, customScheduledExecutors);
    }
}
