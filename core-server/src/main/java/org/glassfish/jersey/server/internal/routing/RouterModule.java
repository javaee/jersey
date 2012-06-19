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
package org.glassfish.jersey.server.internal.routing;

import java.util.regex.Pattern;

import javax.ws.rs.core.UriInfo;

import org.glassfish.jersey.internal.inject.AbstractModule;
import org.glassfish.jersey.process.internal.RequestScope;
import org.glassfish.jersey.uri.ExtendedUriInfo;
import org.glassfish.jersey.uri.PathPattern;

import org.glassfish.hk2.ComponentException;
import org.glassfish.hk2.Factory;
import org.glassfish.hk2.TypeLiteral;

import org.jvnet.hk2.annotations.Inject;

/**
 * Provides routing configuration functionality.
 *
 * @author Paul Sandoz
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class RouterModule extends AbstractModule {

    public interface RootRouteBuilder<T> extends RouteBuilder<T> {

        Router root(Router routingRoot);
    }

    public interface RouteBuilder<T> {

        RouteToBuilder<T> route(String pattern);

        RouteToBuilder<T> route(T pattern);
    }

    public interface RouteToBuilder<T> {

        RouteToPathBuilder<T> to(Router.Builder ab);

        RouteToPathBuilder<T> to(Router a);

        RouteToPathBuilder<T> to(Class<? extends Router> ca);

        RouteToPathBuilder<T> to(Factory<? extends Router> pa);
    }

    public interface RouteToPathBuilder<T> extends RouteBuilder<T>, RouteToBuilder<T>, Router.Builder {
    }

    @Override
    public void configure() {
        bind(new TypeLiteral<RootRouteBuilder<Pattern>>() {
        }).to(PatternRouteBuilder.class);
        bind(new TypeLiteral<RootRouteBuilder<PathPattern>>() {
        }).to(PathPatternRouteBuilder.class);

        /**
         * Note: Bellow bindings work because UriRoutingContextFactory is bound via class,
         * which causes HK2 to instantiate and inject it properly whenever it needs to be
         * used (typically once per run-time request scope instance).
         */
        bind(UriRoutingContext.class).to(UriRoutingContext.class).in(RequestScope.class);
        bind(RoutingContext.class).toFactory(UriRoutingContextFactory.class).in(RequestScope.class);
        bind(ExtendedUriInfo.class).toFactory(UriRoutingContextFactory.class).in(RequestScope.class);
        bind(UriInfo.class).toFactory(UriRoutingContextFactory.class).in(RequestScope.class);

        // "Assisted" bindings
        bind().to(MatchResultInitializerRouter.Builder.class);
        bind().to(PatternRouter.Builder.class);
        bind().to(PathPatternRouter.Builder.class);
    }

    private static class UriRoutingContextFactory implements Factory<UriRoutingContext> {

        @Inject
        private UriRoutingContext ctx;

        @Override
        public UriRoutingContext get() throws ComponentException {
            return ctx;
        }
    }
}
