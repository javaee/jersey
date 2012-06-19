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

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyWriter;

import org.glassfish.jersey.internal.ContextResolverFactory;
import org.glassfish.jersey.internal.ExceptionMapperFactory;
import org.glassfish.jersey.internal.JaxrsProviders;
import org.glassfish.jersey.internal.ServiceFinderModule;
import org.glassfish.jersey.internal.ServiceProvidersModule;
import org.glassfish.jersey.internal.inject.AbstractModule;
import org.glassfish.jersey.internal.inject.ContextInjectionResolver;
import org.glassfish.jersey.internal.inject.ReferencingFactory;
import org.glassfish.jersey.internal.util.collection.Ref;
import org.glassfish.jersey.message.internal.MessageBodyFactory;
import org.glassfish.jersey.message.internal.MessagingModules;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.process.internal.*;
import org.glassfish.jersey.server.internal.inject.CloseableServiceModule;
import org.glassfish.jersey.server.internal.inject.ParameterInjectionModule;
import org.glassfish.jersey.server.internal.routing.RouterModule;
import org.glassfish.jersey.server.model.ResourceModelModule;
import org.glassfish.jersey.server.spi.ContainerProvider;
import org.glassfish.jersey.spi.ExceptionMappers;

import org.glassfish.hk2.Factory;
import org.glassfish.hk2.TypeLiteral;
import org.glassfish.hk2.scopes.PerLookup;
import org.glassfish.hk2.scopes.Singleton;

import org.jvnet.hk2.annotations.Inject;

/**
 * Server injection binding configuration module.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class ServerModule extends AbstractModule {

    private static class RequestReferencingFactory extends ReferencingFactory<Request> {

        public RequestReferencingFactory(@Inject Factory<Ref<Request>> referenceFactory) {
            super(referenceFactory);
        }
    }

    private static class HttpHeadersReferencingFactory extends ReferencingFactory<HttpHeaders> {

        public HttpHeadersReferencingFactory(@Inject Factory<Ref<HttpHeaders>> referenceFactory) {
            super(referenceFactory);
        }
    }

    private static class RequestContextInjectionFactory extends ReferencingFactory<JerseyContainerRequestContext> {

        public RequestContextInjectionFactory(@Inject Factory<Ref<JerseyContainerRequestContext>> referenceFactory) {
            super(referenceFactory);
        }
    }

    /**
     * Injection-enabled client side {@link org.glassfish.jersey.process.internal.RequestInvoker} instance builder.
     */
    static final class RequestInvokerBuilder {
        @Inject
        private RequestScope requestScope;
        @Inject
        private ResponseProcessor.Builder<JerseyContainerResponseContext> responseProcessorBuilder;
        @Inject
        private Factory<Ref<InvocationContext>> invocationContextReferenceFactory;
        @Inject
        private ProcessingExecutorsFactory executorsFactory;

        /**
         * Build a new {@link org.glassfish.jersey.process.internal.RequestInvoker request invoker} configured to use
         * the supplied request processor for processing requests.
         *
         * @param rootStage root processing stage.
         * @return new request invoker instance.
         */
        public RequestInvoker<JerseyContainerRequestContext, JerseyContainerResponseContext> build(
                final Stage<JerseyContainerRequestContext> rootStage) {

            final AsyncInflectorAdapter.Builder<JerseyContainerRequestContext,JerseyContainerResponseContext> asyncAdapterBuilder =
                    new AsyncInflectorAdapter.Builder<JerseyContainerRequestContext, JerseyContainerResponseContext>() {
                        @Override
                        public AsyncInflectorAdapter<JerseyContainerRequestContext, JerseyContainerResponseContext> create(
                                Inflector<JerseyContainerRequestContext, JerseyContainerResponseContext> wrapped,
                                InvocationCallback<JerseyContainerResponseContext> callback) {
                            return new AsyncInflectorAdapter<JerseyContainerRequestContext, JerseyContainerResponseContext>(
                                    wrapped, callback) {

                                @Override
                                protected JerseyContainerResponseContext convertResponse(
                                        JerseyContainerRequestContext requestContext, Response response) {
                                    return new JerseyContainerResponseContext(requestContext, response);
                                }
                            };
                        }
                    };

            return new RequestInvoker<JerseyContainerRequestContext, JerseyContainerResponseContext>(
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
    static class ResponseProcessorBuilder implements ResponseProcessor.Builder<JerseyContainerResponseContext> {
        @Inject
        private RequestScope requestScope;
        @Inject
        private Factory<ResponseProcessor.RespondingContext<JerseyContainerResponseContext>> respondingCtxProvider;
        @Inject
        private Factory<ExceptionMappers> exceptionMappersProvider;
        @Inject
        private Factory<JerseyContainerRequestContext> requestContextFactory;

        /**
         * Default constructor meant to be used by injection framework.
         */
        public ResponseProcessorBuilder() {
            // Injection constructor
        }

        @Override
        public ResponseProcessor<JerseyContainerResponseContext> build(
                final Future<JerseyContainerResponseContext> inflectedResponse,
                final InvocationCallback<JerseyContainerResponseContext> callback,
                final RequestScope.Instance scopeInstance) {

            return new ResponseProcessor<JerseyContainerResponseContext>(
                    callback,
                    inflectedResponse,
                    respondingCtxProvider,
                    scopeInstance,
                    requestScope,
                    exceptionMappersProvider) {

                @Override
                protected JerseyContainerResponseContext convertResponse(Response exceptionResponse) {
                    return (exceptionResponse == null) ? null :
                            new JerseyContainerResponseContext(requestContextFactory.get(), exceptionResponse);
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
                new ServiceProvidersModule(),
                new MessageBodyFactory.Module(Singleton.class),
                new ExceptionMapperFactory.Module(Singleton.class),
                new ContextResolverFactory.Module(Singleton.class),
                new JaxrsProviders.Module(),
                new FilterModule(),
                new SecurityContextModule(),
                new ParameterInjectionModule(),
                new ResourceModelModule(),
                new RouterModule(),
                new ServiceFinderModule<ContainerProvider>(ContainerProvider.class),
                new CloseableServiceModule());

        // Request/Response injection interfaces
        bind(Request.class).toFactory(RequestReferencingFactory.class).in(PerLookup.class);
        bind(new TypeLiteral<Ref<Request>>() {
        }).toFactory(ReferencingFactory.<Request>referenceFactory()).in(RequestScope.class);

        bind(HttpHeaders.class).toFactory(HttpHeadersReferencingFactory.class).in(PerLookup.class);
        bind(new TypeLiteral<Ref<HttpHeaders>>() {
        }).toFactory(ReferencingFactory.<HttpHeaders>referenceFactory()).in(RequestScope.class);

        // server-side processing chain
        bind(JerseyContainerRequestContext.class)
                .toFactory(RequestContextInjectionFactory.class)
                .in(RequestScope.class);
        bind(new TypeLiteral<Ref<JerseyContainerRequestContext>>() {
        })
                .toFactory(ReferencingFactory.<JerseyContainerRequestContext>referenceFactory())
                .in(RequestScope.class);

        bind(new TypeLiteral<ResponseProcessor.RespondingContext<JerseyContainerResponseContext>>() {
        }).to(new TypeLiteral<DefaultRespondingContext<JerseyContainerResponseContext>>() {
        }).in(RequestScope.class);

        bind(new TypeLiteral<ResponseProcessor.Builder<JerseyContainerResponseContext>>() {
        }).to(ResponseProcessorBuilder.class).in(Singleton.class);


        //ChunkedResponseWriter
        bind(MessageBodyWriter.class).to(ChunkedResponseWriter.class).in(Singleton.class);
    }
}
