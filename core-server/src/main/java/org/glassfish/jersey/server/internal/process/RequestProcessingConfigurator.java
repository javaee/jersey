/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.server.internal.process;

import java.util.function.Supplier;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import javax.inject.Inject;

import org.glassfish.jersey.internal.BootstrapBag;
import org.glassfish.jersey.internal.BootstrapConfigurator;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.glassfish.jersey.server.CloseableService;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ExtendedUriInfo;
import org.glassfish.jersey.server.internal.routing.UriRoutingContext;
import org.glassfish.jersey.server.spi.ExternalRequestScope;

/**
 * Configurator which initializes and register {@link ExternalRequestScope} instance into {@link InjectionManager}.
 *
 * @author Petr Bouda (petr.bouda at oracle.com)
 */
public class RequestProcessingConfigurator implements BootstrapConfigurator {

    @Override
    public void init(InjectionManager injectionManager, BootstrapBag bootstrapBag) {
        injectionManager.register(new ServerProcessingBinder());
    }

    private static class ContainerRequestFactory implements Supplier<ContainerRequest> {

        private final RequestProcessingContextReference reference;

        @Inject
        private ContainerRequestFactory(RequestProcessingContextReference reference) {
            this.reference = reference;
        }

        @Override
        public ContainerRequest get() {
            return reference.get().request();
        }
    }

    private static class UriRoutingContextFactory implements Supplier<UriRoutingContext> {

        private final RequestProcessingContextReference reference;

        @Inject
        private UriRoutingContextFactory(RequestProcessingContextReference reference) {
            this.reference = reference;
        }

        @Override
        public UriRoutingContext get() {
            return reference.get().uriRoutingContext();
        }
    }

    private static class CloseableServiceFactory implements Supplier<CloseableService> {

        private final RequestProcessingContextReference reference;

        @Inject
        private CloseableServiceFactory(RequestProcessingContextReference reference) {
            this.reference = reference;
        }

        @Override
        public CloseableService get() {
            return reference.get().closeableService();
        }
    }

    private static class AsyncContextFactory implements Supplier<AsyncContext> {

        private final RequestProcessingContextReference reference;

        @Inject
        private AsyncContextFactory(RequestProcessingContextReference reference) {
            this.reference = reference;
        }

        @Override
        public AsyncContext get() {
            return reference.get().asyncContext();
        }
    }

    /**
     * Defines server-side request processing injection bindings.
     *
     * @author Marek Potociar (marek.potociar at oracle.com)
     */
    private class ServerProcessingBinder extends AbstractBinder {

        @Override
        protected void configure() {
            bindAsContract(RequestProcessingContextReference.class)
                    .in(RequestScoped.class);

            // Bind non-proxiable ContainerRequest injection injection points
            bindFactory(ContainerRequestFactory.class)
                    .to(ContainerRequest.class).to(ContainerRequestContext.class)
                    .proxy(false)
                    .in(RequestScoped.class);

            // Bind proxiable HttpHeaders, Request and ContainerRequestContext injection injection points
            bindFactory(ContainerRequestFactory.class)
                    .to(HttpHeaders.class).to(Request.class)
                    .proxy(true).proxyForSameScope(false)
                    .in(RequestScoped.class);

            // Bind proxiable UriInfo, ExtendedUriInfo and ResourceInfo injection points
            bindFactory(UriRoutingContextFactory.class)
                    .to(UriInfo.class).to(ExtendedUriInfo.class).to(ResourceInfo.class)
                    .proxy(true).proxyForSameScope(false)
                    .in(RequestScoped.class);

            // Bind proxiable SecurityContext injection point.
            // NOTE:
            // SecurityContext must be injected using the Injectee. The reason is that SecurityContext can be changed by filters,
            // but the proxy internally caches the first SecurityContext value injected in the RequestScope. This is prevented by
            // using SecurityContextInjectee that does not cache the SecurityContext instances and instead delegates calls to
            // the SecurityContext instance retrieved from current ContainerRequestContext.
            bind(SecurityContextInjectee.class)
                    .to(SecurityContext.class)
                    .proxy(true).proxyForSameScope(false)
                    .in(RequestScoped.class);

            // Bind proxiable CloseableService injection point.
            bindFactory(CloseableServiceFactory.class)
                    .to(CloseableService.class)
                    .proxy(true).proxyForSameScope(false)
                    .in(RequestScoped.class);

            // Bind proxiable AsyncContext and AsyncResponse injection points.
            // TODO maybe we can get rid of these completely? Or at least for AsyncContext?
            bindFactory(AsyncContextFactory.class)
                    .to(AsyncContext.class)
                    .to(AsyncResponse.class)
                    .in(RequestScoped.class);
        }
    }
}
