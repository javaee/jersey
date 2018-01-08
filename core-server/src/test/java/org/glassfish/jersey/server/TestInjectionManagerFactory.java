/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2017 Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;
import java.util.List;

import javax.ws.rs.RuntimeType;

import org.glassfish.jersey.internal.AutoDiscoverableConfigurator;
import org.glassfish.jersey.internal.BootstrapConfigurator;
import org.glassfish.jersey.internal.ContextResolverFactory;
import org.glassfish.jersey.internal.ExceptionMapperFactory;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.internal.inject.Binder;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.Injections;
import org.glassfish.jersey.message.internal.MessageBodyFactory;
import org.glassfish.jersey.message.internal.MessagingBinders;
import org.glassfish.jersey.model.internal.ManagedObjectsFinalizer;
import org.glassfish.jersey.process.internal.RequestScope;
import org.glassfish.jersey.server.internal.inject.ParamConverterConfigurator;
import org.glassfish.jersey.server.internal.inject.ParamExtractorConfigurator;
import org.glassfish.jersey.server.internal.inject.ValueParamProviderConfigurator;
import org.glassfish.jersey.server.internal.process.RequestProcessingConfigurator;
import org.glassfish.jersey.server.model.internal.ResourceMethodInvokerConfigurator;

/**
 * Utility class to create initialized server-side injection manager.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public final class TestInjectionManagerFactory {

    private static Binder EMPTY_BINDER = new AbstractBinder() {
        @Override
        protected void configure() {
        }
    };

    private TestInjectionManagerFactory() {
        // prevents instantiation
    }

    /**
     * Create new initialized server injection manager.
     *
     * @return new initialized server injection manager.
     */
    public static BootstrapResult createInjectionManager() {
        return createInjectionManager(new ResourceConfig(), EMPTY_BINDER);
    }

    /**
     * Create new initialized server injection manager.
     *
     * @param runtimeConfig runtime config with test's components.
     * @return new initialized server injection manager.
     */
    public static BootstrapResult createInjectionManager(ResourceConfig runtimeConfig) {
        return createInjectionManager(runtimeConfig, EMPTY_BINDER);
    }

    /**
     * Create new initialized server injection manager.
     *
     * @param runtimeConfig runtime config with test's components.
     * @param customBinder  binder which is immediately registered.
     * @return new initialized server injection manager.
     */
    public static BootstrapResult createInjectionManager(ResourceConfig runtimeConfig,  Binder customBinder) {
        InjectionManager injectionManager = Injections.createInjectionManager();
        injectionManager.register(new ServerBinder());
        injectionManager.register(new MessagingBinders.MessageBodyProviders(runtimeConfig.getProperties(), RuntimeType.SERVER));
        injectionManager.register(customBinder);

        ServerBootstrapBag bootstrapBag = new ServerBootstrapBag();
        ManagedObjectsFinalizer managedObjectsFinalizer = new ManagedObjectsFinalizer(injectionManager);
        bootstrapBag.setManagedObjectsFinalizer(managedObjectsFinalizer);

        List<BootstrapConfigurator> bootstrapConfigurators = Arrays.asList(
                new RequestProcessingConfigurator(),
                new RequestScope.RequestScopeConfigurator(),
                new ParamConverterConfigurator(),
                new ParamExtractorConfigurator(),
                new ValueParamProviderConfigurator(),
                new JerseyResourceContextConfigurator(),
                new ComponentProviderConfigurator(),
                new TestConfigConfigurator(runtimeConfig),
                new ContextResolverFactory.ContextResolversConfigurator(),
                new MessageBodyFactory.MessageBodyWorkersConfigurator(),
                new ExceptionMapperFactory.ExceptionMappersConfigurator(),
                new ResourceMethodInvokerConfigurator(),
                new ProcessingProvidersConfigurator(),
                new ContainerProviderConfigurator(RuntimeType.SERVER),
                new AutoDiscoverableConfigurator(RuntimeType.SERVER),
                new ResourceBagConfigurator(),
                new ExternalRequestScopeConfigurator(),
                new ModelProcessorConfigurator(),
                new ResourceModelConfigurator(),
                new ServerExecutorProvidersConfigurator());

        bootstrapConfigurators.forEach(configurator -> configurator.init(injectionManager, bootstrapBag));

        // Configure binders and features.
        bootstrapBag.getRuntimeConfig().configureMetaProviders(injectionManager, bootstrapBag.getManagedObjectsFinalizer());

        injectionManager.completeRegistration();
        bootstrapConfigurators.forEach(configurator -> configurator.postInit(injectionManager, bootstrapBag));
        return new BootstrapResult(injectionManager, bootstrapBag);
    }

    public static class BootstrapResult {
        public InjectionManager injectionManager;
        public ServerBootstrapBag bootstrapBag;

        BootstrapResult(InjectionManager injectionManager, ServerBootstrapBag bootstrapBag) {
            this.injectionManager = injectionManager;
            this.bootstrapBag = bootstrapBag;
        }
    }
}
