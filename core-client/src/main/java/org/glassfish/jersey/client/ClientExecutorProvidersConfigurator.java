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

import java.util.Map;

import org.glassfish.jersey.internal.BootstrapBag;
import org.glassfish.jersey.internal.inject.Bindings;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.InstanceBinding;
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

    private final ComponentBag componentBag;

    ClientExecutorProvidersConfigurator(ComponentBag componentBag) {
        this.componentBag = componentBag;
    }

    @Override
    public void init(InjectionManager injectionManager, BootstrapBag bootstrapBag) {
        Map<String, Object> runtimeProperties = bootstrapBag.getConfiguration().getProperties();
        ManagedObjectsFinalizer finalizer = bootstrapBag.getManagedObjectsFinalizer();

        // Default async request executors support
        int asyncThreadPoolSize = ClientProperties.getValue(runtimeProperties, ClientProperties.ASYNC_THREADPOOL_SIZE, 0);
        asyncThreadPoolSize = (asyncThreadPoolSize < 0) ? 0 : asyncThreadPoolSize;

        // TODO: Do we need to register DEFAULT Executor and ScheduledExecutor to InjectionManager?
        InstanceBinding<Integer> asyncThreadPoolSizeBinding = Bindings
                .service(asyncThreadPoolSize)
                .named("ClientAsyncThreadPoolSize");
        injectionManager.register(asyncThreadPoolSizeBinding);

        ScheduledExecutorServiceProvider defaultScheduledExecutorProvider = new DefaultClientBackgroundSchedulerProvider();
        InstanceBinding<ScheduledExecutorServiceProvider> schedulerBinding = Bindings
                .service(defaultScheduledExecutorProvider)
                .to(ScheduledExecutorServiceProvider.class);
        injectionManager.register(schedulerBinding);
        finalizer.registerForPreDestroyCall(defaultScheduledExecutorProvider);

        ExecutorServiceProvider defaultAsyncExecutorProvider = new DefaultClientAsyncExecutorProvider(asyncThreadPoolSize);
        InstanceBinding<ExecutorServiceProvider> executorBinding = Bindings
                .service(defaultAsyncExecutorProvider)
                .to(ExecutorServiceProvider.class);
        injectionManager.register(executorBinding);
        finalizer.registerForPreDestroyCall(defaultAsyncExecutorProvider);

        registerExecutors(injectionManager, componentBag, defaultAsyncExecutorProvider, defaultScheduledExecutorProvider);
    }
}
