/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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

import java.util.Map;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.WriterInterceptor;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.glassfish.jersey.internal.ContextResolverFactory;
import org.glassfish.jersey.internal.ExceptionMapperFactory;
import org.glassfish.jersey.internal.JaxrsProviders;
import org.glassfish.jersey.internal.JerseyErrorService;
import org.glassfish.jersey.internal.ServiceFinderBinder;
import org.glassfish.jersey.internal.inject.ContextInjectionResolver;
import org.glassfish.jersey.internal.inject.HttpHeadersInjectee;
import org.glassfish.jersey.internal.inject.JerseyClassAnalyzer;
import org.glassfish.jersey.internal.inject.ReferencingFactory;
import org.glassfish.jersey.internal.inject.RequestInjectee;
import org.glassfish.jersey.internal.inject.SecurityContextInjectee;
import org.glassfish.jersey.internal.inject.UriInfoInjectee;
import org.glassfish.jersey.internal.spi.AutoDiscoverable;
import org.glassfish.jersey.internal.util.collection.Ref;
import org.glassfish.jersey.message.internal.MessageBodyFactory;
import org.glassfish.jersey.message.internal.MessagingBinders;
import org.glassfish.jersey.process.internal.RequestScope;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.glassfish.jersey.server.internal.JerseyResourceContext;
import org.glassfish.jersey.server.internal.JsonWithPaddingInterceptor;
import org.glassfish.jersey.server.internal.MappableExceptionWrapperInterceptor;
import org.glassfish.jersey.server.internal.ProcessingProviders;
import org.glassfish.jersey.server.internal.RuntimeExecutorsBinder;
import org.glassfish.jersey.server.internal.inject.CloseableServiceBinder;
import org.glassfish.jersey.server.internal.inject.ParameterInjectionBinder;
import org.glassfish.jersey.server.internal.process.RespondingContext;
import org.glassfish.jersey.server.internal.routing.RouterBinder;
import org.glassfish.jersey.server.model.internal.ResourceModelBinder;
import org.glassfish.jersey.server.spi.ContainerProvider;

import org.glassfish.hk2.api.TypeLiteral;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

/**
 * Server injection binder.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @author Libor Kramolis (libor.kramolis at oracle.com)
 * @todo Move this class to the internal package???
 */
public class ServerBinder extends AbstractBinder {

    private final RuntimeType runtimeType;

    private final Map<String, Object> applicationProperties;

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

    public ServerBinder(Map<String, Object> applicationProperties, RuntimeType runtimeType) {
        this.applicationProperties = applicationProperties;
        this.runtimeType = runtimeType;
    }

    @Override
    protected void configure() {
        install(new RequestScope.Binder(), // must go first as it registers the request scope instance.
                new JerseyErrorService.Binder(),
                new ProcessingBinder(),
                new ContextInjectionResolver.Binder(),
                new ParameterInjectionBinder(),
                new JerseyClassAnalyzer.Binder(),
                new MessagingBinders.MessageBodyProviders(applicationProperties, runtimeType),
                new MessageBodyFactory.Binder(),
                new ExceptionMapperFactory.Binder(),
                new ContextResolverFactory.Binder(),
                new JaxrsProviders.Binder(),
                new ProcessingProviders.Binder(),
                new ContainerFilteringStage.Binder(),
                new ResourceModelBinder(),
                new RuntimeExecutorsBinder(),
                new RouterBinder(),
                new ServiceFinderBinder<ContainerProvider>(ContainerProvider.class, applicationProperties, runtimeType),
                new CloseableServiceBinder(),
                new JerseyResourceContext.Binder(),
                new ServiceFinderBinder<AutoDiscoverable>(AutoDiscoverable.class, applicationProperties, runtimeType),
                new MappableExceptionWrapperInterceptor.Binder());

        // Request/Response injection interfaces
        bindFactory(ReferencingFactory.<Request>referenceFactory()).to(new TypeLiteral<Ref<Request>>() {
        }).in(RequestScoped.class);

        // server-side processing chain
        bindFactory(RequestContextInjectionFactory.class).to(ContainerRequest.class).in(RequestScoped.class);
        bindFactory(RequestContextInjectionFactory.class).to(ContainerRequestContext.class).in(RequestScoped.class);

        bindFactory(ReferencingFactory.<ContainerRequest>referenceFactory()).to(new TypeLiteral<Ref<ContainerRequest>>() {
        }).in(RequestScoped.class);

        bind(DefaultRespondingContext.class).to(RespondingContext.class).in(RequestScoped.class);

        //ChunkedResponseWriter
        bind(ChunkedResponseWriter.class).to(MessageBodyWriter.class).in(Singleton.class);

        // JSONP
        bind(JsonWithPaddingInterceptor.class).to(WriterInterceptor.class).in(Singleton.class);

        bindAsContract(ReferencesInitializer.class);

        // JAX-RS proxiable request scoped injections
        bind(UriInfoInjectee.class).to(UriInfoInjectee.class).to(UriInfo.class).proxy(true).in(RequestScoped.class);
        bind(HttpHeadersInjectee.class).to(HttpHeadersInjectee.class).to(HttpHeaders.class).proxy(true).in(RequestScoped.class);
        bind(RequestInjectee.class).to(RequestInjectee.class).to(Request.class).proxy(true).in(RequestScoped.class);
        bind(SecurityContextInjectee.class).to(SecurityContextInjectee.class).to(SecurityContext.class).proxy(true).in(RequestScoped.class);

    }
}
