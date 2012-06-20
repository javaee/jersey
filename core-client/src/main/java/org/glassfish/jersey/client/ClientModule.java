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

import java.util.concurrent.Future;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.FeaturesAndProperties;
import org.glassfish.jersey.internal.ContextResolverFactory;
import org.glassfish.jersey.internal.ExceptionMapperFactory;
import org.glassfish.jersey.internal.JaxrsProviders;
import org.glassfish.jersey.internal.ServiceProvidersModule;
import org.glassfish.jersey.internal.inject.AbstractModule;
import org.glassfish.jersey.internal.inject.ContextInjectionResolver;
import org.glassfish.jersey.internal.inject.ReferencingFactory;
import org.glassfish.jersey.internal.util.collection.Ref;
import org.glassfish.jersey.message.internal.ExceptionWrapperInterceptor;
import org.glassfish.jersey.message.internal.MessageBodyFactory;
import org.glassfish.jersey.message.internal.MessagingModules;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.process.internal.AsyncInflectorAdapter;
import org.glassfish.jersey.process.internal.DefaultRespondingContext;
import org.glassfish.jersey.process.internal.InvocationCallback;
import org.glassfish.jersey.process.internal.InvocationContext;
import org.glassfish.jersey.process.internal.ProcessingExecutorsFactory;
import org.glassfish.jersey.process.internal.ProcessingModule;
import org.glassfish.jersey.process.internal.RequestInvoker;
import org.glassfish.jersey.process.internal.RequestScope;
import org.glassfish.jersey.process.internal.ResponseProcessor;
import org.glassfish.jersey.process.internal.Stage;
import org.glassfish.jersey.spi.ExceptionMappers;

import org.glassfish.hk2.Factory;
import org.glassfish.hk2.TypeLiteral;
import org.glassfish.hk2.scopes.Singleton;

import org.jvnet.hk2.annotations.Inject;

import com.google.common.util.concurrent.SettableFuture;

/**
 * Registers all modules necessary for {@link Client} runtime.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
class ClientModule extends AbstractModule {

    private static class ConfigurationInjectionFactory extends ReferencingFactory<JerseyConfiguration> {

        public ConfigurationInjectionFactory(@Inject Factory<Ref<JerseyConfiguration>> referenceFactory) {
            super(referenceFactory);
        }
    }

    private static class RequestContextInjectionFactory extends ReferencingFactory<JerseyClientRequestContext> {

        public RequestContextInjectionFactory(@Inject Factory<Ref<JerseyClientRequestContext>> referenceFactory) {
            super(referenceFactory);
        }
    }

    /**
     * Injection-enabled client side {@link RequestInvoker} instance builder.
     */
    static final class RequestInvokerBuilder {
        @Inject
        private RequestScope requestScope;
        @Inject
        private ResponseProcessor.Builder<JerseyClientResponseContext> responseProcessorBuilder;
        @Inject
        private Factory<Ref<InvocationContext>> invocationContextReferenceFactory;
        @Inject
        private ProcessingExecutorsFactory executorsFactory;

        /**
         * Build a new {@link RequestInvoker request invoker} configured to use
         * the supplied request processor for processing requests.
         *
         * @param rootStage root processing stage.
         * @return new request invoker instance.
         */
        public RequestInvoker<JerseyClientRequestContext, JerseyClientResponseContext> build(
                final Stage<JerseyClientRequestContext> rootStage) {

            final AsyncInflectorAdapter.Builder<JerseyClientRequestContext,JerseyClientResponseContext> asyncAdapterBuilder =
                    new AsyncInflectorAdapter.Builder<JerseyClientRequestContext, JerseyClientResponseContext>() {
                        @Override
                        public AsyncInflectorAdapter<JerseyClientRequestContext, JerseyClientResponseContext> create(
                                Inflector<JerseyClientRequestContext, JerseyClientResponseContext> wrapped, InvocationCallback<JerseyClientResponseContext> callback) {
                            return new AsyncInflectorAdapter<JerseyClientRequestContext, JerseyClientResponseContext>(
                                    wrapped, callback) {

                                @Override
                                protected JerseyClientResponseContext convertResponse(
                                        JerseyClientRequestContext requestContext, Response response) {
                                    // TODO get rid of this code on the client side
                                    return new JerseyClientResponseContext(requestContext, response);
                                }
                            };
                        }
                    };

            return new RequestInvoker<JerseyClientRequestContext, JerseyClientResponseContext>(
                    rootStage,
                    requestScope,
                    asyncAdapterBuilder,
                    responseProcessorBuilder,
                    invocationContextReferenceFactory,
                    executorsFactory);
        }

    }

    /**
     * Injection-enabled client side {@link ResponseProcessor} instance builder.
     */
    static class ResponseProcessorBuilder implements ResponseProcessor.Builder<JerseyClientResponseContext> {
        @Inject
        private RequestScope requestScope;
        @Inject
        private Factory<ResponseProcessor.RespondingContext<JerseyClientResponseContext>> respondingCtxProvider;
        @Inject
        private Factory<ExceptionMappers> exceptionMappersProvider;
        @Inject
        private Factory<JerseyClientRequestContext> requestContextFactory;

        /**
         * Default constructor meant to be used by injection framework.
         */
        public ResponseProcessorBuilder() {
            // Injection constructor
        }

        @Override
        public ResponseProcessor<JerseyClientResponseContext> build(
                final Future<JerseyClientResponseContext> inflectedResponse,
                final SettableFuture<JerseyClientResponseContext> processedResponse,
                final InvocationCallback<JerseyClientResponseContext> callback,
                final RequestScope.Instance scopeInstance) {

            return new ResponseProcessor<JerseyClientResponseContext>(
                    callback,
                    inflectedResponse,
                    processedResponse,
                    respondingCtxProvider,
                    scopeInstance,
                    requestScope,
                    exceptionMappersProvider) {

                @Override
                protected JerseyClientResponseContext convertResponse(Response exceptionResponse) {
                    return (exceptionResponse == null) ? null : new JerseyClientResponseContext(
                            exceptionResponse.getStatusInfo(),
                            requestContextFactory.get());
                }
            };
        }
    }

    @Override
    protected void configure() {
        install(new RequestScope.Module(), // must go first as it registers the request scope instance.
                new ProcessingModule(),
                new ContextInjectionResolver.Module(),
                new MessagingModules.MessageBodyProviders(),
                new MessagingModules.HeaderDelegateProviders(),
                new ServiceProvidersModule(RequestScope.class),
                new MessageBodyFactory.Module(RequestScope.class),
                new ExceptionMapperFactory.Module(RequestScope.class),
                new ContextResolverFactory.Module(RequestScope.class),
                new JaxrsProviders.Module(),
                new ClientFilteringStage.Module(),
                new ExceptionWrapperInterceptor.Module());

        bind(javax.ws.rs.client.Configuration.class)
                .toFactory(ConfigurationInjectionFactory.class)
                .in(RequestScope.class);
        bind(FeaturesAndProperties.class)
                .toFactory(ConfigurationInjectionFactory.class)
                .in(RequestScope.class);
        bind(new TypeLiteral<Ref<JerseyConfiguration>>() {
        })
                .toFactory(ReferencingFactory.<JerseyConfiguration>referenceFactory())
                .in(RequestScope.class);

        // Client-side processing chain
        bind(JerseyClientRequestContext.class)
                .toFactory(RequestContextInjectionFactory.class)
                .in(RequestScope.class);
        bind(new TypeLiteral<Ref<JerseyClientRequestContext>>() {
        })
                .toFactory(ReferencingFactory.<JerseyClientRequestContext>referenceFactory())
                .in(RequestScope.class);

        bind(new TypeLiteral<ResponseProcessor.RespondingContext<JerseyClientResponseContext>>() {
        }).to(new TypeLiteral<DefaultRespondingContext<JerseyClientResponseContext>>() {
        }).in(RequestScope.class);

        bind(new TypeLiteral<ResponseProcessor.Builder<JerseyClientResponseContext>>() {
        }).to(ResponseProcessorBuilder.class).in(Singleton.class);
    }
}
