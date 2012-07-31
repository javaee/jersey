/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

import java.util.concurrent.Future;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyWriter;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.glassfish.jersey.internal.ContextResolverFactory;
import org.glassfish.jersey.internal.ExceptionMapperFactory;
import org.glassfish.jersey.internal.JaxrsProviders;
import org.glassfish.jersey.internal.ServiceFinderBinder;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.internal.inject.ContextInjectionResolver;
import org.glassfish.jersey.internal.inject.ReferencingFactory;
import org.glassfish.jersey.internal.util.collection.Ref;
import org.glassfish.jersey.message.internal.MessageBodyFactory;
import org.glassfish.jersey.message.internal.MessagingBinders;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.process.internal.*;
import org.glassfish.jersey.server.internal.ServerExecutorsFactory;
import org.glassfish.jersey.server.internal.inject.CloseableServiceBinder;
import org.glassfish.jersey.server.internal.inject.ParameterInjectionBinder;
import org.glassfish.jersey.server.internal.routing.RouterBinder;
import org.glassfish.jersey.server.internal.routing.SingletonResourceBinder;
import org.glassfish.jersey.server.model.ResourceModelBinder;
import org.glassfish.jersey.server.spi.ContainerProvider;
import org.glassfish.jersey.spi.ExceptionMappers;

import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.TypeLiteral;

import com.google.common.util.concurrent.SettableFuture;

/**
 * Server injection binder.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class ServerBinder extends AbstractBinder {

    private static class RequestReferencingFactory extends ReferencingFactory<Request> {
        @Inject
        public RequestReferencingFactory(Provider<Ref<Request>> referenceFactory) {
            super(referenceFactory);
        }
    }

    private static class HttpHeadersFactory implements Factory<HttpHeaders> {
        private final Provider<ContainerRequest> containerRequestContextProvider;

        @Inject
        public HttpHeadersFactory(Provider<ContainerRequest> containerRequestContextProvider) {
            this.containerRequestContextProvider = containerRequestContextProvider;
        }

        @Override
        public HttpHeaders provide() {
            return containerRequestContextProvider.get();
        }

        @Override
        public void dispose(HttpHeaders httpHeaders) {
            // nothing to dispose
        }
    }


    private static class RequestContextInjectionFactory extends ReferencingFactory<ContainerRequest> {
        @Inject
        public RequestContextInjectionFactory(Provider<Ref<ContainerRequest>> referenceFactory) {
            super(referenceFactory);
        }

        @Override
        @RequestScoped
        public ContainerRequest provide() {
            return super.provide();
        }
    }

    /**
     * Injection-enabled client side {@link org.glassfish.jersey.process.internal.RequestInvoker} instance builder.
     */
    static final class RequestInvokerBuilder {
        @Inject
        private RequestScope requestScope;
        @Inject
        private ResponseProcessor.Builder<ContainerResponse> responseProcessorBuilder;
        @Inject
        private Provider<Ref<InvocationContext>> invocationContextReferenceFactory;
        @Inject
        private ExecutorsFactory<ContainerRequest> executorsFactory;

        /**
         * Build a new {@link org.glassfish.jersey.process.internal.RequestInvoker request invoker} configured to use
         * the supplied request processor for processing requests.
         *
         * @param rootStage root processing stage.
         * @return new request invoker instance.
         */
        public RequestInvoker<ContainerRequest, ContainerResponse> build(
                final Stage<ContainerRequest> rootStage) {

            return new RequestInvoker<ContainerRequest, ContainerResponse>(
                    rootStage,
                    requestScope,
                    new AsyncInflectorAdapter.Builder<ContainerRequest, ContainerResponse>() {
                        @Override
                        public AsyncInflectorAdapter<ContainerRequest, ContainerResponse> create(
                                Inflector<ContainerRequest, ContainerResponse> wrapped,
                                InvocationCallback<ContainerResponse> callback) {
                            return new AsyncInflectorAdapter<ContainerRequest, ContainerResponse>(
                                    wrapped, callback) {

                                @Override
                                protected ContainerResponse convertResponse(
                                        ContainerRequest requestContext, Response response) {
                                    return new ContainerResponse(requestContext, response);
                                }
                            };
                        }
                    },
                    responseProcessorBuilder,
                    invocationContextReferenceFactory,
                    executorsFactory);
        }

    }

    /**
     * Injection-enabled client side {@link ResponseProcessor} instance builder.
     */
    static class ResponseProcessorBuilder implements ResponseProcessor.Builder<ContainerResponse> {
        @Inject
        private RequestScope requestScope;
        @Inject
        private Provider<ResponseProcessor.RespondingContext<ContainerResponse>> respondingCtxProvider;
        @Inject
        private Provider<ExceptionMappers> exceptionMappersProvider;
        @Inject
        private Provider<ContainerRequest> requestContextFactory;

        /**
         * Default constructor meant to be used by injection framework.
         */
        public ResponseProcessorBuilder() {
            // Injection constructor
        }

        @Override
        public ResponseProcessor<ContainerResponse> build(
                final Future<ContainerResponse> inflectedResponse,
                final SettableFuture<ContainerResponse> processedResponse,
                final InvocationCallback<ContainerResponse> callback,
                final RequestScope.Instance scopeInstance) {

            return new ResponseProcessor<ContainerResponse>(
                    callback,
                    inflectedResponse,
                    processedResponse,
                    respondingCtxProvider,
                    scopeInstance,
                    requestScope,
                    exceptionMappersProvider) {

                @Override
                protected ContainerResponse convertResponse(Response exceptionResponse) {
                    return (exceptionResponse == null) ? null :
                            new ContainerResponse(requestContextFactory.get(), exceptionResponse);
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
                new MessageBodyFactory.Binder(),
                new ExceptionMapperFactory.Binder(),
                new ContextResolverFactory.Binder(),
                new JaxrsProviders.Binder(),
                new ContainerFilteringStage.Binder(),
                new SecurityContextBinder(),
                new ParameterInjectionBinder(),
                new ResourceModelBinder(),
                new RouterBinder(),
                new ServiceFinderBinder<ContainerProvider>(ContainerProvider.class),
                new CloseableServiceBinder(),
                new SingletonResourceBinder.SingletonResourceBinderBinder(),
                new ServerExecutorsFactory.ServerExecutorBinder());

        // Request/Response injection interfaces
        bindFactory(RequestReferencingFactory.class).to(Request.class).in(PerLookup.class);
        bindFactory(ReferencingFactory.<Request>referenceFactory()).to(new TypeLiteral<Ref<Request>>() {
        }).in(RequestScoped.class);


        bindFactory(HttpHeadersFactory.class, Singleton.class).to(HttpHeaders.class).in(PerLookup.class);

        // server-side processing chain
        bindFactory(RequestContextInjectionFactory.class).to(ContainerRequest.class).in(RequestScoped.class);
        bindFactory(RequestContextInjectionFactory.class).to(ContainerRequestContext.class).in(RequestScoped.class);

        bindFactory(ReferencingFactory.<ContainerRequest>referenceFactory()).to(new TypeLiteral<Ref<ContainerRequest>>() {
        }).in(RequestScoped.class);

        bind(DefaultRespondingContext.class).to(new TypeLiteral<ResponseProcessor.RespondingContext<ContainerResponse>>() {
        }).in(RequestScoped.class);

        bind(ResponseProcessorBuilder.class).to(new TypeLiteral<ResponseProcessor.Builder<ContainerResponse>>() {
        }).in(Singleton.class);

        //ChunkedResponseWriter
        bind(ChunkedResponseWriter.class).to(MessageBodyWriter.class).in(Singleton.class);

        bindAsContract(RequestInvokerBuilder.class);
        bindAsContract(ReferencesInitializer.class);
    }
}
