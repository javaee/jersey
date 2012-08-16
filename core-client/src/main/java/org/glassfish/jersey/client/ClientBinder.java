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

import java.util.concurrent.Future;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.glassfish.jersey.internal.ContextResolverFactory;
import org.glassfish.jersey.internal.ExceptionMapperFactory;
import org.glassfish.jersey.internal.JaxrsProviders;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.internal.inject.ContextInjectionResolver;
import org.glassfish.jersey.internal.inject.ReferencingFactory;
import org.glassfish.jersey.internal.util.collection.Ref;
import org.glassfish.jersey.message.internal.ExceptionWrapperInterceptor;
import org.glassfish.jersey.message.internal.MessageBodyFactory;
import org.glassfish.jersey.message.internal.MessagingBinders;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.process.internal.AsyncInflectorAdapter;
import org.glassfish.jersey.process.internal.DefaultRespondingContext;
import org.glassfish.jersey.process.internal.ExecutorsFactory;
import org.glassfish.jersey.process.internal.ProcessingCallback;
import org.glassfish.jersey.process.internal.ProcessingContext;
import org.glassfish.jersey.process.internal.ProcessingBinder;
import org.glassfish.jersey.process.internal.RequestInvoker;
import org.glassfish.jersey.process.internal.RequestScope;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.glassfish.jersey.process.internal.ResponseProcessor;
import org.glassfish.jersey.process.internal.Stage;
import org.glassfish.jersey.spi.ExceptionMappers;

import org.glassfish.hk2.api.TypeLiteral;

import com.google.common.util.concurrent.SettableFuture;

/**
 * Registers all binders necessary for {@link Client} runtime.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
class ClientBinder extends AbstractBinder {

    private static class RequestContextInjectionFactory extends ReferencingFactory<ClientRequest> {
        @Inject
        public RequestContextInjectionFactory(Provider<Ref<ClientRequest>> referenceFactory) {
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
        private ResponseProcessor.Builder<ClientResponse> responseProcessorBuilder;
        @Inject
        private Provider<Ref<ProcessingContext>> invocationContextReferenceFactory;
        @Inject
        private ExecutorsFactory<ClientRequest> executorsFactory;

        /**
         * Build a new {@link RequestInvoker request invoker} configured to use
         * the supplied request processor for processing requests.
         *
         * @param rootStage root processing stage.
         * @return new request invoker instance.
         */
        public RequestInvoker<ClientRequest, ClientResponse> build(
                final Stage<ClientRequest> rootStage) {

            final AsyncInflectorAdapter.Builder<ClientRequest, ClientResponse> asyncAdapterBuilder =
                    new AsyncInflectorAdapter.Builder<ClientRequest, ClientResponse>() {
                        @Override
                        public AsyncInflectorAdapter<ClientRequest, ClientResponse> create(
                                Inflector<ClientRequest, ClientResponse> wrapped, ProcessingCallback<ClientResponse> callback) {
                            return new AsyncInflectorAdapter<ClientRequest, ClientResponse>(
                                    wrapped, callback) {

                                @Override
                                protected ClientResponse convertResponse(ClientRequest requestContext, Response response) {
                                    // TODO get rid of this code on the client side
                                    return new ClientResponse(requestContext, response);
                                }
                            };
                        }
                    };

            return new RequestInvoker<ClientRequest, ClientResponse>(
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
    static class ResponseProcessorBuilder implements ResponseProcessor.Builder<ClientResponse> {
        @Inject
        private RequestScope requestScope;
        @Inject
        private Provider<ResponseProcessor.RespondingContext<ClientResponse>> respondingCtxProvider;
        @Inject
        private Provider<ExceptionMappers> exceptionMappersProvider;
        @Inject
        private Provider<ClientRequest> requestContextFactory;

        /**
         * Default constructor meant to be used by injection framework.
         */
        public ResponseProcessorBuilder() {
            // Injection constructor
        }

        @Override
        public ResponseProcessor<ClientResponse> build(
                final Future<ClientResponse> inflectedResponse,
                final SettableFuture<ClientResponse> processedResponse,
                final ProcessingCallback<ClientResponse> callback,
                final RequestScope.Instance scopeInstance) {

            return new ResponseProcessor<ClientResponse>(
                    callback,
                    inflectedResponse,
                    processedResponse,
                    respondingCtxProvider,
                    scopeInstance,
                    requestScope,
                    exceptionMappersProvider) {

                @Override
                protected ClientResponse convertResponse(Response exceptionResponse) {
                    return (exceptionResponse == null) ? null : new ClientResponse(
                            exceptionResponse.getStatusInfo(),
                            requestContextFactory.get());
                }
            };
        }
    }

    @Override
    protected void configure() {
        install(new RequestScope.Binder(), // must go first as it registers the request scope instance.
                new ProcessingBinder(),
                new ContextInjectionResolver.Binder(),
                new MessagingBinders.MessageBodyProviders(),
                new MessagingBinders.HeaderDelegateProviders(),
                new MessageBodyFactory.Binder(),
                new ExceptionMapperFactory.Binder(),
                new ContextResolverFactory.Binder(),
                new JaxrsProviders.Binder(),
                new ClientFilteringStage.Binder(),
                new ExceptionWrapperInterceptor.Binder(),
                new ClientExecutorsFactory.ClientExecutorBinder());

        bindFactory(ReferencingFactory.<ClientConfig>referenceFactory()).to(new TypeLiteral<Ref<ClientConfig>>() {
        }).in(RequestScoped.class);

        // Client-side processing chain
        bindFactory(RequestContextInjectionFactory.class).
                to(ClientRequest.class).
                in(RequestScoped.class);

        bindFactory(ReferencingFactory.<ClientRequest>referenceFactory()).to(new TypeLiteral<Ref<ClientRequest>>() {
        }).in(RequestScoped.class);

        bind(DefaultRespondingContext.class)
                .to(new TypeLiteral<ResponseProcessor.RespondingContext<ClientResponse>>() {}).in(RequestScoped.class);

        bind(ResponseProcessorBuilder.class).to(new TypeLiteral<ResponseProcessor.Builder<ClientResponse>>() {
                }).in(Singleton.class);
    }
}
