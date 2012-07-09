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
import org.glassfish.jersey.internal.ProviderBinder;
import org.glassfish.jersey.internal.ServiceFinderModule;
import org.glassfish.jersey.internal.inject.AbstractModule;
import org.glassfish.jersey.internal.inject.ContextInjectionResolver;
import org.glassfish.jersey.internal.inject.ReferencingFactory;
import org.glassfish.jersey.internal.inject.Utilities;
import org.glassfish.jersey.internal.util.collection.Ref;
import org.glassfish.jersey.message.internal.MessageBodyFactory;
import org.glassfish.jersey.message.internal.MessagingModules;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.process.internal.AsyncInflectorAdapter;
import org.glassfish.jersey.process.internal.DefaultRespondingContext;
import org.glassfish.jersey.process.internal.ExecutorsFactory;
import org.glassfish.jersey.process.internal.InvocationCallback;
import org.glassfish.jersey.process.internal.InvocationContext;
import org.glassfish.jersey.process.internal.ProcessingModule;
import org.glassfish.jersey.process.internal.RequestInvoker;
import org.glassfish.jersey.process.internal.RequestScope;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.glassfish.jersey.process.internal.ResponseProcessor;
import org.glassfish.jersey.process.internal.Stage;
import org.glassfish.jersey.server.internal.ServerExecutorsFactory;
import org.glassfish.jersey.server.internal.inject.CloseableServiceModule;
import org.glassfish.jersey.server.internal.inject.ParameterInjectionModule;
import org.glassfish.jersey.server.internal.routing.RouterModule;
import org.glassfish.jersey.server.internal.routing.SingletonResourceBinder;
import org.glassfish.jersey.server.model.ResourceModelModule;
import org.glassfish.jersey.server.spi.ContainerProvider;
import org.glassfish.jersey.spi.ExceptionMappers;

import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.TypeLiteral;
import org.glassfish.hk2.utilities.BuilderHelper;

import com.google.common.util.concurrent.SettableFuture;

/**
 * Server injection binding configuration module.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class ServerModule extends AbstractModule {

    private static class RequestReferencingFactory extends ReferencingFactory<Request> {
        @Inject
        public RequestReferencingFactory(Provider<Ref<Request>> referenceFactory) {
            super(referenceFactory);
        }
    }

    private static class HttpHeadersReferencingFactory extends ReferencingFactory<HttpHeaders> {
        @Inject
        public HttpHeadersReferencingFactory(Provider<Ref<HttpHeaders>> referenceFactory) {
            super(referenceFactory);
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
        install(new RequestScope.Module(), // must go first as it registers the request scope instance.
                new ProcessingModule(),
                new ContextInjectionResolver.Module(),
                new MessagingModules.MessageBodyProviders(),
                new ProviderBinder.ProviderBinderModule(),
                new MessageBodyFactory.Module(Singleton.class),
                new ExceptionMapperFactory.Module(Singleton.class),
                new ContextResolverFactory.Module(Singleton.class),
                new JaxrsProviders.Module(),
                new ContainerFilteringStage.Module(),
                new SecurityContextModule(),
                new ParameterInjectionModule(),
                new ResourceModelModule(),
                new RouterModule(),
                new ServiceFinderModule<ContainerProvider>(ContainerProvider.class),
                new CloseableServiceModule(),
                new SingletonResourceBinder.SingletonResourceBinderModule(),
                new ServerExecutorsFactory.ServerExecutorModule());

        // Request/Response injection interfaces
        bind(BuilderHelper.link(RequestReferencingFactory.class).to(Request.class).in(PerLookup.class).buildFactory());
        bind(Utilities.createConstantFactoryDescriptor(
                ReferencingFactory.<Request>referenceFactory(),
                RequestScoped.class,
                null, null, null,
                (new TypeLiteral<Ref<Request>>() {
                }).getType()));


        bind(BuilderHelper.link(HttpHeadersReferencingFactory.class).to(HttpHeaders.class).in(PerLookup.class).buildFactory());
        bind(Utilities.createConstantFactoryDescriptor(
                ReferencingFactory.<HttpHeaders>referenceFactory(),
                RequestScoped.class,
                null, null, null,
                (new TypeLiteral<Ref<HttpHeaders>>() {
                }).getType()));

        // server-side processing chain
        bind(BuilderHelper.link(RequestContextInjectionFactory.class).to(ContainerRequest.class).in(RequestScoped.class)
                .buildFactory());
        bind(BuilderHelper.link(RequestContextInjectionFactory.class).to(ContainerRequestContext.class).in(RequestScoped.class)
                .buildFactory());

        bind(Utilities.createConstantFactoryDescriptor(
                ReferencingFactory.<ContainerRequest>referenceFactory(),
                RequestScoped.class,
                null, null, null,
                (new TypeLiteral<Ref<ContainerRequest>>() {
                }).getType()));


        bind(BuilderHelper.activeLink(DefaultRespondingContext.class).
                to((new TypeLiteral<ResponseProcessor.RespondingContext<ContainerResponse>>() {
                }).getType()).
                in(RequestScoped.class).build());


        bind(BuilderHelper.activeLink(ResponseProcessorBuilder.class).
                to((new TypeLiteral<ResponseProcessor.Builder<ContainerResponse>>() {
                }).getType()).
                in(Singleton.class).build());

        //ChunkedResponseWriter
        bind(BuilderHelper.link(ChunkedResponseWriter.class).to(MessageBodyWriter.class).in(Singleton.class).build());

        bind(BuilderHelper.link(RequestInvokerBuilder.class).build());
        bind(BuilderHelper.link(ReferencesInitializer.class).build());
    }
}
