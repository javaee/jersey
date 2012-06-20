/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.jersey.internal.ContextResolverFactory;
import org.glassfish.jersey.internal.ExceptionMapperFactory;
import org.glassfish.jersey.internal.ServiceProviders;
import org.glassfish.jersey.internal.util.collection.Ref;
import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.message.internal.MessageBodyFactory;
import org.glassfish.jersey.spi.ContextResolvers;
import org.glassfish.jersey.spi.ExceptionMappers;

import org.glassfish.hk2.Factory;
import org.glassfish.hk2.inject.Injector;

import org.jvnet.hk2.annotations.Inject;

import com.google.common.base.Function;

/**
 * Function that can be put to an acceptor chain to properly initialize
 * the client-side request-scoped processing injection for the current
 * request and response exchange.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class RequestProcessingInitializationStage implements Function<JerseyClientRequestContext, JerseyClientRequestContext> {

    private static final class References {

        @Inject
        private Ref<JerseyConfiguration> configuration;
        @Inject
        private Ref<ServiceProviders> serviceProviders;
        @Inject
        private Ref<ExceptionMappers> exceptionMappers;
        @Inject
        private Ref<MessageBodyWorkers> messageBodyWorkers;
        @Inject
        private Ref<ContextResolvers> contextResolvers;
        @Inject
        Ref<JerseyClientRequestContext> requestContextRef;
    }

    private final Injector injector;
    private final ServiceProviders.Builder serviceProvidersBuilder;

    /**
     * Create new {@link org.glassfish.jersey.message.MessageBodyWorkers} initialization function
     * for requests and responses.
     *
     * @param injector HK2 injector.
     * @param serviceProvidersBuilder Jersey service providers builder.
     * @param workersFactory {@code MessageBodyWorkers} factory.
     */
    public RequestProcessingInitializationStage(
            @Inject Injector injector,
            @Inject ServiceProviders.Builder serviceProvidersBuilder,
            @Inject Factory<MessageBodyWorkers> workersFactory) {
        this.injector = injector;
        this.serviceProvidersBuilder = serviceProvidersBuilder;
    }


    @Override
    public JerseyClientRequestContext apply(JerseyClientRequestContext requestContext) {
        References refs = injector.inject(References.class); // request-scoped

        final JerseyConfiguration cfg = requestContext.getConfiguration();
        final ServiceProviders providers = serviceProvidersBuilder
                .setProviderClasses(cfg.getProviderClasses()).setProviderInstances(cfg.getProviderInstances())
                .build();
        final ExceptionMapperFactory mappers = new ExceptionMapperFactory(providers);
        final MessageBodyWorkers workers = new MessageBodyFactory(providers);
        final ContextResolvers resolvers = new ContextResolverFactory(providers);

        refs.configuration.set(cfg);
        refs.serviceProviders.set(providers);
        refs.exceptionMappers.set(mappers);
        refs.messageBodyWorkers.set(workers);
        refs.contextResolvers.set(resolvers);
        refs.requestContextRef.set(requestContext);

        requestContext.setWorkers(workers);

        return requestContext;
    }
}
