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

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.RequestHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.ResponseHeaders;

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
import org.glassfish.jersey.message.internal.Requests;
import org.glassfish.jersey.process.internal.*;
import org.glassfish.jersey.process.internal.DefaultRespondingContext;
import org.glassfish.jersey.process.internal.DefaultStagingContext;
import org.glassfish.jersey.process.internal.ExceptionMapper;
import org.glassfish.jersey.process.internal.FilterModule;
import org.glassfish.jersey.process.internal.HierarchicalRequestProcessor;
import org.glassfish.jersey.process.internal.RequestInvoker;
import org.glassfish.jersey.process.internal.RequestProcessor;
import org.glassfish.jersey.process.internal.RequestScope;
import org.glassfish.jersey.process.internal.ResponseProcessor;
import org.glassfish.jersey.process.internal.ResponseProcessor.RespondingContext;
import org.glassfish.jersey.process.internal.Stage;
import org.glassfish.jersey.process.internal.StagingContext;
import org.glassfish.jersey.server.internal.inject.CloseableServiceModule;
import org.glassfish.jersey.server.internal.inject.ParameterInjectionModule;
import org.glassfish.jersey.server.internal.routing.RouterModule;
import org.glassfish.jersey.server.model.ResourceModelModule;
import org.glassfish.jersey.server.spi.ContainerProvider;

import org.glassfish.hk2.Factory;
import org.glassfish.hk2.TypeLiteral;
import org.glassfish.hk2.scopes.PerLookup;
import org.glassfish.hk2.scopes.Singleton;

import org.jvnet.hk2.annotations.Inject;

/**
 *
 * @author Paul Sandoz
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class ServerModule extends AbstractModule {

    private static class ServerExceptionMapper implements ExceptionMapper<Throwable> {

        @Override
        public Response apply(Throwable exception) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    private static class RequestStagingContext extends DefaultStagingContext<Request> {

        @Inject
        private Ref<Request> requestReference;
        @Inject
        private Ref<RequestHeaders> requestHeadersReference;
        @Inject
        private Ref<HttpHeaders> httpHeadersReference;

        @Override
        protected void before(final Stage<Request, ?> stage, final Request request) {
            requestReference.set(request);
            requestHeadersReference.set(request.getHeaders());
            httpHeadersReference.set(Requests.httpHeaders(request));
        }

        @Override
        protected void after(final Stage<Request, ?> stage, final Request request) {
            requestReference.set(request);
            requestHeadersReference.set(request.getHeaders());
            httpHeadersReference.set(Requests.httpHeaders(request));
        }
    }

    private static class ResponseStagingContext extends DefaultStagingContext<Response> {

        @Inject
        private Ref<Response> responseReference;
        @Inject
        private Ref<ResponseHeaders> responseHeadersReference;

        @Override
        protected void before(final Stage<Response, ?> stage, final Response response) {
            responseReference.set(response);
            responseHeadersReference.set(response.getHeaders());
        }

        @Override
        protected void after(final Stage<Response, ?> stage, final Response response) {
            responseReference.set(response);
            responseHeadersReference.set(response.getHeaders());
        }
    }

    private static class RequestReferencingFactory extends ReferencingFactory<Request> {

        public RequestReferencingFactory(@Inject Factory<Ref<Request>> referenceFactory) {
            super(referenceFactory);
        }
    }

    private static class ResponseReferencingFactory extends ReferencingFactory<Response> {

        public ResponseReferencingFactory(@Inject Factory<Ref<Response>> referenceFactory) {
            super(referenceFactory);
        }
    }

    private static class RequestHeadersReferencingFactory extends ReferencingFactory<RequestHeaders> {

        public RequestHeadersReferencingFactory(@Inject Factory<Ref<RequestHeaders>> referenceFactory) {
            super(referenceFactory);
        }
    }

    private static class HttpHeadersReferencingFactory extends ReferencingFactory<HttpHeaders> {

        public HttpHeadersReferencingFactory(@Inject Factory<Ref<HttpHeaders>> referenceFactory) {
            super(referenceFactory);
        }
    }

    private static class ResponseHeadersReferencingFactory extends ReferencingFactory<ResponseHeaders> {

        public ResponseHeadersReferencingFactory(@Inject Factory<Ref<ResponseHeaders>> referenceFactory) {
            super(referenceFactory);
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

        // exception mapper
        bind(new TypeLiteral<ExceptionMapper<Throwable>>() {}).toInstance(new ServerExceptionMapper());

        // Request/Response staging contexts
        bind(new TypeLiteral<StagingContext<Request>>() {}).to(RequestStagingContext.class).in(RequestScope.class);

        bind(new TypeLiteral<StagingContext<Response>>() {}).to(ResponseStagingContext.class).in(RequestScope.class);

        bind(Request.class).toFactory(RequestReferencingFactory.class).in(PerLookup.class);
        bind(new TypeLiteral<Ref<Request>>() {}).toFactory(ReferencingFactory.<Request>referenceFactory()).in(RequestScope.class);

        bind(RequestHeaders.class).toFactory(RequestHeadersReferencingFactory.class).in(PerLookup.class);
        bind(new TypeLiteral<Ref<RequestHeaders>>() {}).toFactory(ReferencingFactory.<RequestHeaders>referenceFactory()).in(RequestScope.class);

        bind(HttpHeaders.class).toFactory(HttpHeadersReferencingFactory.class).in(PerLookup.class);
        bind(new TypeLiteral<Ref<HttpHeaders>>() {}).toFactory(ReferencingFactory.<HttpHeaders>referenceFactory()).in(RequestScope.class);

        bind(Response.class).toFactory(ResponseReferencingFactory.class).in(PerLookup.class);
        bind(new TypeLiteral<Ref<Response>>() {}).toFactory(ReferencingFactory.<Response>referenceFactory()).in(RequestScope.class);

        bind(ResponseHeaders.class).toFactory(ResponseHeadersReferencingFactory.class).in(PerLookup.class);
        bind(new TypeLiteral<Ref<ResponseHeaders>>() {}).toFactory(ReferencingFactory.<ResponseHeaders>referenceFactory()).in(RequestScope.class);

        // Request processor
        bind(RequestProcessor.class).to(HierarchicalRequestProcessor.class);
        // Request invoker
        bind(RespondingContext.class).to(DefaultRespondingContext.class).in(RequestScope.class);

        bind().to(ResponseProcessor.Builder.class);

        bind().to(RequestInvoker.class);
    }
}
