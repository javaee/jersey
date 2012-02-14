/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.hk2.Factory;
import org.glassfish.hk2.TypeLiteral;
import org.glassfish.hk2.scopes.Singleton;
import org.glassfish.jersey.FeaturesAndProperties;
import org.glassfish.jersey.internal.ContextResolverFactory;
import org.glassfish.jersey.internal.ExceptionMapperFactory;
import org.glassfish.jersey.internal.JaxrsProviders;
import org.glassfish.jersey.internal.ServiceProvidersModule;
import org.glassfish.jersey.internal.inject.AbstractModule;
import org.glassfish.jersey.internal.inject.ContextInjectionResolver;
import org.glassfish.jersey.internal.inject.ReferencingFactory;
import org.glassfish.jersey.internal.util.collection.Ref;
import org.glassfish.jersey.message.internal.MessageBodyFactory;
import org.glassfish.jersey.message.internal.MessagingModules;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.process.internal.DefaultRespondingContext;
import org.glassfish.jersey.process.internal.DefaultStagingContext;
import org.glassfish.jersey.process.internal.FilterModule;
import org.glassfish.jersey.process.internal.LinearAcceptor;
import org.glassfish.jersey.process.internal.LinearRequestProcessor;
import org.glassfish.jersey.process.internal.RequestInvoker;
import org.glassfish.jersey.process.internal.RequestProcessor;
import org.glassfish.jersey.process.internal.RequestScope;
import org.glassfish.jersey.process.internal.ResponseProcessor;
import org.glassfish.jersey.process.internal.ResponseProcessor.RespondingContext;
import org.glassfish.jersey.process.internal.Stage;
import org.glassfish.jersey.process.internal.Stages;
import org.glassfish.jersey.process.internal.StagingContext;
import org.jvnet.hk2.annotations.Inject;

import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

/**
 * Registers all modules necessary for {@link Client} runtime.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public class ClientModule extends AbstractModule {

    private static class ConfigurationInjectionFactory extends ReferencingFactory<Configuration> {

        public ConfigurationInjectionFactory(@Inject Factory<Ref<Configuration>> referenceFactory) {
            super(referenceFactory);
        }
    }
    //
    private final Inflector<Request, Response> connector;

    /**
     * Creates {@link ClientModule} with custom connector.
     *
     * @see Inflector
     * @param connector used for transforming {@link Request} to {@link Response}
     */
    public ClientModule(Inflector<Request, Response> connector) {
        this.connector = connector;
    }

    @Override
    protected void configure() {
        install(new RequestScope.Module(), // must go first as it registers the request scope instance.
                new ContextInjectionResolver.Module(),
                new MessagingModules.MessageBodyProviders(),
                new ServiceProvidersModule(),
                new MessageBodyFactory.Module(Singleton.class),
                new ExceptionMapperFactory.Module(Singleton.class),
                new ContextResolverFactory.Module(Singleton.class),
                new JaxrsProviders.Module(),
                new FilterModule());

        // Request/Response staging contexts
        bind(new TypeLiteral<StagingContext<Request>>() {}).to(new TypeLiteral<DefaultStagingContext<Request>>() {})
                .in(RequestScope.class);

        bind(new TypeLiteral<StagingContext<Response>>() {}).to(new TypeLiteral<DefaultStagingContext<Response>>() {})
                .in(RequestScope.class);


        // Request processor

        // TODO uncomment qualifier once the HK2 issue is fixed
        if (connector != null) {
            bind(LinearAcceptor.class).annotatedWith(Stage.Root.class).toInstance(Stages.asLinearAcceptor(connector));
        } else {
            bind(LinearAcceptor.class).annotatedWith(Stage.Root.class).toInstance(Stages.asLinearAcceptor(
                    new HttpUrlConnector()));
        }

        bind(RequestProcessor.class).to(LinearRequestProcessor.class);
        // Request invoker
        bind(RespondingContext.class).to(DefaultRespondingContext.class).in(RequestScope.class);

        bind().to(ResponseProcessor.Builder.class);

        bind().to(RequestInvoker.class);

        bind(javax.ws.rs.client.Configuration.class)
                .toFactory(ConfigurationInjectionFactory.class)
                .in(RequestScope.class);
        bind(FeaturesAndProperties.class)
                .toFactory(ConfigurationInjectionFactory.class)
                .in(RequestScope.class);
        bind(new TypeLiteral<Ref<Configuration>>() {})
                .toFactory(ReferencingFactory.<Configuration>referenceFactory())
                .in(RequestScope.class);
    }
}
