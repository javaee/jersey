/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2015 Oracle and/or its affiliates. All rights reserved.
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
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.WriterInterceptor;

import javax.inject.Singleton;

import org.glassfish.jersey.internal.ContextResolverFactory;
import org.glassfish.jersey.internal.ExceptionMapperFactory;
import org.glassfish.jersey.internal.JaxrsProviders;
import org.glassfish.jersey.internal.JerseyErrorService;
import org.glassfish.jersey.internal.ServiceFinderBinder;
import org.glassfish.jersey.internal.inject.ContextInjectionResolver;
import org.glassfish.jersey.internal.inject.JerseyClassAnalyzer;
import org.glassfish.jersey.internal.spi.AutoDiscoverable;
import org.glassfish.jersey.message.internal.MessageBodyFactory;
import org.glassfish.jersey.message.internal.MessagingBinders;
import org.glassfish.jersey.process.internal.RequestScope;
import org.glassfish.jersey.server.internal.JerseyResourceContext;
import org.glassfish.jersey.server.internal.JsonWithPaddingInterceptor;
import org.glassfish.jersey.server.internal.MappableExceptionWrapperInterceptor;
import org.glassfish.jersey.server.internal.ProcessingProviders;
import org.glassfish.jersey.server.internal.inject.ParameterInjectionBinder;
import org.glassfish.jersey.server.internal.monitoring.MonitoringContainerListener;
import org.glassfish.jersey.server.internal.process.ServerProcessingBinder;
import org.glassfish.jersey.server.model.internal.ResourceModelBinder;
import org.glassfish.jersey.server.spi.ContainerProvider;

import org.glassfish.hk2.utilities.binding.AbstractBinder;

/**
 * Server injection binder.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @author Libor Kramolis (libor.kramolis at oracle.com)
 */
class ServerBinder extends AbstractBinder {

    private final Map<String, Object> applicationProperties;

    /**
     * Create new {@code ServerBinder} instance.
     *
     * @param applicationProperties map of application-specific properties.
     */
    public ServerBinder(final Map<String, Object> applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    @Override
    protected void configure() {
        install(new RequestScope.Binder(), // must go first as it registers the request scope instance.
                new JerseyErrorService.Binder(),
                new ServerProcessingBinder(),
                new ContextInjectionResolver.Binder(),
                new ParameterInjectionBinder(),
                new JerseyClassAnalyzer.Binder(),
                new MessagingBinders.MessageBodyProviders(applicationProperties, RuntimeType.SERVER),
                new MessageBodyFactory.Binder(),
                new ExceptionMapperFactory.Binder(),
                new ContextResolverFactory.Binder(),
                new JaxrsProviders.Binder(),
                new ProcessingProviders.Binder(),
                new ResourceModelBinder(),
                new ServiceFinderBinder<>(ContainerProvider.class, applicationProperties, RuntimeType.SERVER),
                new JerseyResourceContext.Binder(),
                new ServiceFinderBinder<>(AutoDiscoverable.class, applicationProperties, RuntimeType.SERVER),
                new MappableExceptionWrapperInterceptor.Binder(),
                new MonitoringContainerListener.Binder());

        //ChunkedResponseWriter
        bind(ChunkedResponseWriter.class).to(MessageBodyWriter.class).in(Singleton.class);

        // JSONP
        bind(JsonWithPaddingInterceptor.class).to(WriterInterceptor.class).in(Singleton.class);
    }
}
