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
package org.glassfish.jersey.server.model;

import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import org.glassfish.hk2.Factory;

import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.spi.internal.ResourceMethodDispatcher;
import org.glassfish.jersey.server.spi.internal.ResourceMethodInvocationHandlerProvider;

import org.glassfish.hk2.inject.Injector;
import org.glassfish.jersey.server.internal.routing.RouterModule;

import org.jvnet.hk2.annotations.Inject;

/**
 * Server-side request-response {@link Inflector inflector} for invoking methods
 * of annotation-based resource classes.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class ResourceMethodInvoker implements Inflector<Request, Response> {

    /**
     * Resource method invoker "assisted" injection helper.
     *
     * The injectable builder API provides means for constructing a properly
     * injected {@link ResourceMethodInvoker resource method invoker} instances.
     */
    public static class Builder {

        @Inject
        private Injector injector;
        @Inject
        private Factory<RouterModule.RoutingContext> routingContextFactory;
        @Inject
        private ResourceMethodDispatcherFactory dispatcherProviderFactory;
        @Inject
        private ResourceMethodInvocationHandlerFactory invocationHandlerProviderFactory;

        public ResourceMethodInvoker build(InvocableResourceMethod method) {
            return new ResourceMethodInvoker(injector, routingContextFactory, dispatcherProviderFactory, invocationHandlerProviderFactory, method);
        }
    }

    private final Injector injector;
    private Factory<RouterModule.RoutingContext> routingContextFactory;
    private final InvocableResourceMethod method;
    private final ResourceMethodDispatcher dispatcher;

    private ResourceMethodInvoker(
            Injector injector,
            Factory<RouterModule.RoutingContext> routingContextFactory,
            ResourceMethodDispatcher.Provider dispatcherProvider,
            ResourceMethodInvocationHandlerProvider invocationHandlerProvider,
            InvocableResourceMethod method) {
        this.injector = injector;
        this.routingContextFactory = routingContextFactory;

        this.method = method;
        this.dispatcher = dispatcherProvider.create(method, invocationHandlerProvider.create(method));
    }

    @Override
    public Response apply(final Request request) {
        final Object resource = routingContextFactory.get().peekMatchedResource();
        final Response response = dispatcher.dispatch(resource, request);

        routingContextFactory.get().setResponseMethodType(method.getGenericReturnType());
        routingContextFactory.get().setResponseMethodAnnotations(method.getMethod().getDeclaredAnnotations());

        return response;
    }
}
