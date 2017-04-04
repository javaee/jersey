/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.process.internal;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Named;
import javax.inject.Qualifier;
import javax.inject.Singleton;

import org.glassfish.jersey.internal.LocalizationMessages;
import org.glassfish.jersey.internal.inject.Bindings;
import org.glassfish.jersey.internal.inject.DisposableSupplier;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.Providers;
import org.glassfish.jersey.internal.inject.SupplierInstanceBinding;
import org.glassfish.jersey.internal.util.ExtendedLogger;
import org.glassfish.jersey.internal.util.ReflectionHelper;
import org.glassfish.jersey.spi.ExecutorServiceProvider;
import org.glassfish.jersey.spi.ScheduledExecutorServiceProvider;

/**
 * A utility class with a methods for handling executor injection registration and proper disposal.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public final class ExecutorProviders {

    private static final ExtendedLogger LOGGER =
            new ExtendedLogger(Logger.getLogger(ExecutorProviders.class.getName()), Level.FINEST);

    private ExecutorProviders() {
        throw new AssertionError("Instantiation not allowed.");
    }

    /**
     * Create qualified {@link ExecutorService} and {@link ScheduledExecutorService} injection bindings based on the registered
     * providers implementing the {@link ExecutorServiceProvider} and/or {@link ScheduledExecutorServiceProvider} SPI.
     * <p>
     * This method supports creation of qualified injection bindings based on custom {@link Qualifier qualifier annotations}
     * attached to the registered provider implementation classes as well as named injection bindings based on the {@link Named}
     * qualifier annotation attached to the registered provider implementation classes. {@link ExecutorServiceProvider} and
     * {@link ScheduledExecutorServiceProvider} will be retrieved from {@link InjectionManager}.
     *
     * @param injectionManager application's injection manager.
     */
    public static void registerExecutorBindings(InjectionManager injectionManager) {
        List<ExecutorServiceProvider> executorProviders =
                getExecutorProviders(injectionManager, ExecutorServiceProvider.class);
        List<ScheduledExecutorServiceProvider> scheduledProviders =
                getExecutorProviders(injectionManager, ScheduledExecutorServiceProvider.class);

        registerExecutorBindings(injectionManager, executorProviders, scheduledProviders);
    }

    private static <T> List<T> getExecutorProviders(InjectionManager injectionManager, Class<T> providerClass) {
        Set<T> customProviders = Providers.getCustomProviders(injectionManager, providerClass);
        Set<T> defaultProviders = Providers.getProviders(injectionManager, providerClass);
        // Get only default providers
        defaultProviders.removeAll(customProviders);

        List<T> executorProviders = new LinkedList<>(customProviders);
        executorProviders.addAll(defaultProviders);
        return executorProviders;
    }

    /**
     * Create qualified {@link ExecutorService} and {@link ScheduledExecutorService} injection bindings based on the registered
     * providers implementing the {@link ExecutorServiceProvider} and/or {@link ScheduledExecutorServiceProvider} SPI.
     * <p>
     * This method supports creation of qualified injection bindings based on custom {@link Qualifier qualifier annotations}
     * attached to the registered provider implementation classes as well as named injection bindings based on the {@link Named}
     * qualifier annotation attached to the registered provider implementation classes.
     *
     * @param injectionManager   injection manager to register newly created executor bindings.
     * @param executorProviders  all executor providers registered internally in Jersey and in configuration.
     * @param scheduledProviders all scheduled executor providers registered internally in Jersey and in configuration.
     */
    public static void registerExecutorBindings(
            InjectionManager injectionManager,
            List<ExecutorServiceProvider> executorProviders,
            List<ScheduledExecutorServiceProvider> scheduledProviders) {

        Map<Class<? extends Annotation>, List<ExecutorServiceProvider>> executorProviderMap =
                getQualifierToProviderMap(executorProviders);

        /*
         * Add ExecutorService into DI framework.
         */
        for (Map.Entry<Class<? extends Annotation>, List<ExecutorServiceProvider>> qualifierToProviders
                : executorProviderMap.entrySet()) {
            Class<? extends Annotation> qualifierAnnotationClass = qualifierToProviders.getKey();

            Iterator<ExecutorServiceProvider> bucketProviderIterator = qualifierToProviders.getValue().iterator();
            ExecutorServiceProvider executorProvider = bucketProviderIterator.next();
            logExecutorServiceProvider(qualifierAnnotationClass, bucketProviderIterator, executorProvider);

            SupplierInstanceBinding<ExecutorService> descriptor =
                    Bindings.supplier(new ExecutorServiceSupplier(executorProvider))
                            .in(Singleton.class)
                            .to(ExecutorService.class);

            Annotation qualifier = executorProvider.getClass().getAnnotation(qualifierAnnotationClass);
            if (qualifier instanceof Named) {
                descriptor.named(((Named) qualifier).value());
            } else {
                descriptor.qualifiedBy(qualifier);
            }

            injectionManager.register(descriptor);
        }

        Map<Class<? extends Annotation>, List<ScheduledExecutorServiceProvider>> schedulerProviderMap =
                getQualifierToProviderMap(scheduledProviders);

        /*
         * Add ScheduledExecutorService into DI framework.
         */
        for (Map.Entry<Class<? extends Annotation>, List<ScheduledExecutorServiceProvider>> qualifierToProviders
                : schedulerProviderMap.entrySet()) {
            Class<? extends Annotation> qualifierAnnotationClass = qualifierToProviders.getKey();

            Iterator<ScheduledExecutorServiceProvider> bucketProviderIterator = qualifierToProviders.getValue().iterator();
            ScheduledExecutorServiceProvider executorProvider = bucketProviderIterator.next();
            logScheduledExecutorProvider(qualifierAnnotationClass, bucketProviderIterator, executorProvider);

            SupplierInstanceBinding<ScheduledExecutorService> descriptor =
                    Bindings.supplier(new ScheduledExecutorServiceSupplier(executorProvider))
                            .in(Singleton.class)
                            .to(ScheduledExecutorService.class);

            if (!executorProviderMap.containsKey(qualifierAnnotationClass)) {
                // it is safe to register binding for ExecutorService too...
                descriptor.to(ExecutorService.class);
            }

            Annotation qualifier = executorProvider.getClass().getAnnotation(qualifierAnnotationClass);
            if (qualifier instanceof Named) {
                descriptor.named(((Named) qualifier).value());
            } else {
                descriptor.qualifiedBy(qualifier);
            }

            injectionManager.register(descriptor);
        }
    }

    private static void logScheduledExecutorProvider(Class<? extends Annotation> qualifierAnnotationClass,
                                                     Iterator<ScheduledExecutorServiceProvider> bucketProviderIterator,
                                                     ScheduledExecutorServiceProvider executorProvider) {
        if (LOGGER.isLoggable(Level.CONFIG)) {
            LOGGER.config(LocalizationMessages.USING_SCHEDULER_PROVIDER(
                    executorProvider.getClass().getName(), qualifierAnnotationClass.getName()));

            if (bucketProviderIterator.hasNext()) {
                StringBuilder msg = new StringBuilder(bucketProviderIterator.next().getClass().getName());
                while (bucketProviderIterator.hasNext()) {
                    msg.append(", ").append(bucketProviderIterator.next().getClass().getName());
                }
                LOGGER.config(LocalizationMessages.IGNORED_SCHEDULER_PROVIDERS(
                        msg.toString(), qualifierAnnotationClass.getName()));
            }
        }
    }

    private static void logExecutorServiceProvider(Class<? extends Annotation> qualifierAnnotationClass,
                                                   Iterator<ExecutorServiceProvider> bucketProviderIterator,
                                                   ExecutorServiceProvider executorProvider) {
        if (LOGGER.isLoggable(Level.CONFIG)) {
            LOGGER.config(LocalizationMessages.USING_EXECUTOR_PROVIDER(
                    executorProvider.getClass().getName(), qualifierAnnotationClass.getName()));

            if (bucketProviderIterator.hasNext()) {
                StringBuilder msg = new StringBuilder(bucketProviderIterator.next().getClass().getName());
                while (bucketProviderIterator.hasNext()) {
                    msg.append(", ").append(bucketProviderIterator.next().getClass().getName());
                }
                LOGGER.config(LocalizationMessages.IGNORED_EXECUTOR_PROVIDERS(
                        msg.toString(), qualifierAnnotationClass.getName()));
            }
        }
    }

    private static <T extends ExecutorServiceProvider> Map<Class<? extends Annotation>, List<T>> getQualifierToProviderMap(
            List<T> executorProviders) {

        // iterate over providers and map them by Qualifier annotations (custom ones will be added to the buckets first)
        final Map<Class<? extends Annotation>, List<T>> executorProviderMap = new HashMap<>();

        for (T provider : executorProviders) {
            for (Class<? extends Annotation> qualifier
                    : ReflectionHelper.getAnnotationTypes(provider.getClass(), Qualifier.class)) {

                List<T> providersForQualifier;
                if (!executorProviderMap.containsKey(qualifier)) {
                    providersForQualifier = new LinkedList<>();
                    executorProviderMap.put(qualifier, providersForQualifier);
                } else {
                    providersForQualifier = executorProviderMap.get(qualifier);
                }

                providersForQualifier.add(provider);
            }
        }

        return executorProviderMap;
    }

    private static class ExecutorServiceSupplier implements DisposableSupplier<ExecutorService> {

        private final ExecutorServiceProvider executorProvider;

        private ExecutorServiceSupplier(ExecutorServiceProvider executorServiceProvider) {
            executorProvider = executorServiceProvider;
        }

        @Override
        public ExecutorService get() {
            return executorProvider.getExecutorService();
        }

        @Override
        public void dispose(final ExecutorService instance) {
            executorProvider.dispose(instance);
        }
    }

    private static class ScheduledExecutorServiceSupplier implements DisposableSupplier<ScheduledExecutorService> {

        private final ScheduledExecutorServiceProvider executorProvider;

        private ScheduledExecutorServiceSupplier(ScheduledExecutorServiceProvider executorServiceProvider) {
            executorProvider = executorServiceProvider;
        }

        @Override
        public ScheduledExecutorService get() {
            return executorProvider.getExecutorService();
        }

        @Override
        public void dispose(final ScheduledExecutorService instance) {
            executorProvider.dispose(instance);
        }
    }

}
