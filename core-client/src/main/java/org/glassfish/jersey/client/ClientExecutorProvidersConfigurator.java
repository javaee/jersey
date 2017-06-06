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

package org.glassfish.jersey.client;

import java.lang.reflect.Method;
import java.security.AccessController;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.jersey.internal.BootstrapBag;
import org.glassfish.jersey.internal.inject.Bindings;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.InstanceBinding;
import org.glassfish.jersey.internal.util.ReflectionHelper;
import org.glassfish.jersey.internal.util.collection.Value;
import org.glassfish.jersey.internal.util.collection.Values;
import org.glassfish.jersey.model.internal.ComponentBag;
import org.glassfish.jersey.model.internal.ManagedObjectsFinalizer;
import org.glassfish.jersey.process.internal.AbstractExecutorProvidersConfigurator;
import org.glassfish.jersey.spi.ExecutorServiceProvider;
import org.glassfish.jersey.spi.ScheduledExecutorServiceProvider;

/**
 * Configurator which initializes and register {@link ExecutorServiceProvider} and
 * {@link ScheduledExecutorServiceProvider}.
 *
 * @author Petr Bouda (petr.bouda at oracle.com)
 */
class ClientExecutorProvidersConfigurator extends AbstractExecutorProvidersConfigurator {

    private static final Logger LOGGER = Logger.getLogger(ClientExecutorProvidersConfigurator.class.getName());
    private static final ExecutorService MANAGED_EXECUTOR_SERVICE = lookupManagedExecutorService();

    private final ComponentBag componentBag;
    private final JerseyClient client;

    ClientExecutorProvidersConfigurator(ComponentBag componentBag, JerseyClient client) {
        this.componentBag = componentBag;
        this.client = client;
    }

    @Override
    public void init(InjectionManager injectionManager, BootstrapBag bootstrapBag) {
        Map<String, Object> runtimeProperties = bootstrapBag.getConfiguration().getProperties();
        ManagedObjectsFinalizer finalizer = bootstrapBag.getManagedObjectsFinalizer();

        ExecutorServiceProvider defaultAsyncExecutorProvider = null;
        ScheduledExecutorServiceProvider defaultScheduledExecutorProvider = null;

        final ExecutorService clientExecutorService = client.getExecutorService();

        // if there is a users provided executor service, use it
        if (clientExecutorService != null) {
            defaultAsyncExecutorProvider = new ClientExecutorServiceProvider(clientExecutorService);
        // otherwise, check for ClientProperties.ASYNC_THREADPOOL_SIZE - if that is set, Jersey will create the
        // ExecutorService to be used. If not and running on Java EE container, ManagedExecutorService will be used.
        // Final fallback is DefaultClientAsyncExecutorProvider with defined default.
        } else {

            // Default async request executors support
            Integer asyncThreadPoolSize = ClientProperties
                    .getValue(runtimeProperties, ClientProperties.ASYNC_THREADPOOL_SIZE, Integer.class);

            if (asyncThreadPoolSize != null) {
                // TODO: Do we need to register DEFAULT Executor and ScheduledExecutor to InjectionManager?
                asyncThreadPoolSize = (asyncThreadPoolSize < 0) ? 0 : asyncThreadPoolSize;
                InstanceBinding<Integer> asyncThreadPoolSizeBinding = Bindings
                        .service(asyncThreadPoolSize)
                        .named("ClientAsyncThreadPoolSize");
                injectionManager.register(asyncThreadPoolSizeBinding);

                defaultAsyncExecutorProvider = new DefaultClientAsyncExecutorProvider(asyncThreadPoolSize);
            } else {
                if (MANAGED_EXECUTOR_SERVICE != null) {
                    defaultAsyncExecutorProvider = new ClientExecutorServiceProvider(MANAGED_EXECUTOR_SERVICE);
                } else {
                    defaultAsyncExecutorProvider = new DefaultClientAsyncExecutorProvider(0);
                }
            }
        }

        InstanceBinding<ExecutorServiceProvider> executorBinding = Bindings
                .service(defaultAsyncExecutorProvider)
                .to(ExecutorServiceProvider.class);

        injectionManager.register(executorBinding);
        finalizer.registerForPreDestroyCall(defaultAsyncExecutorProvider);

        final ScheduledExecutorService clientScheduledExecutorService = client.getScheduledExecutorService();
        if (clientScheduledExecutorService != null) {
            defaultScheduledExecutorProvider =
                    new ClientScheduledExecutorServiceProvider(Values.of(clientScheduledExecutorService));
        } else {
            ScheduledExecutorService scheduledExecutorService = lookupManagedScheduledExecutorService();
            defaultScheduledExecutorProvider =
                scheduledExecutorService == null
                        // default client background scheduler disposes the executor service when client is closed.
                        // we don't need to do that for user provided (via ClientBuilder) or managed executor service.
                        ? new DefaultClientBackgroundSchedulerProvider()
                        : new ClientScheduledExecutorServiceProvider(Values.of(scheduledExecutorService));
        }

        InstanceBinding<ScheduledExecutorServiceProvider> schedulerBinding = Bindings
                .service(defaultScheduledExecutorProvider)
                .to(ScheduledExecutorServiceProvider.class);
        injectionManager.register(schedulerBinding);
        finalizer.registerForPreDestroyCall(defaultScheduledExecutorProvider);

        registerExecutors(injectionManager, componentBag, defaultAsyncExecutorProvider, defaultScheduledExecutorProvider);
    }

    private static ExecutorService lookupManagedExecutorService() {
        // Get the default ManagedExecutorService, if available
        try {
            // Android and some other environments don't have InitialContext class available.
            final Class<?> aClass =
                    AccessController.doPrivileged(ReflectionHelper.classForNamePA("javax.naming.InitialContext"));

            final Object initialContext = aClass.newInstance();

            final Method lookupMethod = aClass.getMethod("lookup", String.class);
            return (ExecutorService) lookupMethod.invoke(initialContext, "java:comp/DefaultManagedExecutorService");
        } catch (Exception e) {
            // ignore
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, e.getMessage(), e);
            }
        } catch (LinkageError error) {
            // ignore - JDK8 compact2 profile - http://openjdk.java.net/jeps/161
        }

        return null;
    }

    private ScheduledExecutorService lookupManagedScheduledExecutorService() {
        try {
            // Android and some other environments don't have InitialContext class available.
            final Class<?> aClass =
                    AccessController.doPrivileged(ReflectionHelper.classForNamePA("javax.naming.InitialContext"));
            final Object initialContext = aClass.newInstance();

            final Method lookupMethod = aClass.getMethod("lookup", String.class);
            return (ScheduledExecutorService) lookupMethod
                    .invoke(initialContext, "java:comp/DefaultManagedScheduledExecutorService");
        } catch (Exception e) {
            // ignore
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, e.getMessage(), e);
            }
        } catch (LinkageError error) {
            // ignore - JDK8 compact2 profile - http://openjdk.java.net/jeps/161
        }

        return null;
    }

    @ClientAsyncExecutor
    public static class ClientExecutorServiceProvider implements ExecutorServiceProvider {

        private final ExecutorService executorService;

        ClientExecutorServiceProvider(ExecutorService executorService) {
            this.executorService = executorService;
        }

        @Override
        public ExecutorService getExecutorService() {
            return executorService;
        }

        @Override
        public void dispose(ExecutorService executorService) {

        }
    }

    @ClientBackgroundScheduler
    public static class ClientScheduledExecutorServiceProvider implements ScheduledExecutorServiceProvider {

        private final Value<ScheduledExecutorService> executorService;

        ClientScheduledExecutorServiceProvider(Value<ScheduledExecutorService> executorService) {
            this.executorService = executorService;
        }

        @Override
        public ScheduledExecutorService getExecutorService() {
            return executorService.get();
        }

        @Override
        public void dispose(ExecutorService executorService) {

        }
    }
}
