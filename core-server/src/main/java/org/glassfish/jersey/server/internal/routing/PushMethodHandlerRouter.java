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
package org.glassfish.jersey.server.internal.routing;

import javax.inject.Inject;
import javax.inject.Provider;

import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.model.MethodHandler;

import org.glassfish.hk2.api.ServiceLocator;

/**
 * Terminal router that pushes the matched method's handler instance to the stack
 * returned by {@link javax.ws.rs.core.UriInfo#getMatchedResources()} method.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
class PushMethodHandlerRouter implements Router {

    /**
     * An injectable factory of {@link PushMethodHandlerRouter} instances.
     */
    static class Builder {

        @Inject
        private Provider<RoutingContext> routingContextFactory;
        @Inject
        private ServiceLocator injector;
        @Inject
        private ServiceLocator locator;

        /**
         * Build a new {@link PushMethodHandlerRouter} instance.
         *
         * @param methodHandler method handler model providing the method handler
         *                      instance.
         * @param next          next router to be invoked after the this one.
         * @return new {@code PushMethodHandlerRouter} instance.
         */
        public PushMethodHandlerRouter build(final MethodHandler methodHandler, Router next) {
            return new PushMethodHandlerRouter(routingContextFactory, locator, methodHandler, next);
        }

    }

    private final ServiceLocator locator;
    private final Provider<RoutingContext> routingContextFactory;
    private final MethodHandler methodHandler;
    private final Router next;

    private PushMethodHandlerRouter(
            final Provider<RoutingContext> routingContextFactory,
            final ServiceLocator locator,
            final MethodHandler methodHandler,
            final Router next) {
        this.locator = locator;
        this.routingContextFactory = routingContextFactory;
        this.methodHandler = methodHandler;
        this.next = next;
    }

    @Override
    public Continuation apply(final ContainerRequest request) {
        Object handlerInstance = methodHandler.getInstance(locator);
        routingContextFactory.get().pushMatchedResource(handlerInstance);

        return Continuation.of(request, next);
    }
}
