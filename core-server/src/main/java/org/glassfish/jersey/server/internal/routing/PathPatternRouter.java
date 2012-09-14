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

import java.util.List;
import java.util.regex.MatchResult;

import javax.inject.Inject;
import javax.inject.Provider;

import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.uri.PathPattern;

/**
 * Matches the un-matched right-hand request path to a configured
 * {@link PathPattern path pattern}.
 *
 * @author Paul Sandoz
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
class PathPatternRouter implements Router {

    /**
     * "Assisted injection" factory interface for {@link PathPatternRouter}.
     *
     * See also <a href="http://code.google.com/p/google-guice/wiki/AssistedInject">
     * assisted injection in Guice</a>.
     */
    public static class Builder {

        @Inject
        private Provider<RoutingContext> contextProvider;

        /**
         * Build a path pattern request router.
         *
         * @param routes next-level request pre-processing stages to be returned in case the request
         *               matching in the built router is successful.
         * @return a path pattern request router.
         */
        public PathPatternRouter build(final List<Route<PathPattern>> routes) {
            return new PathPatternRouter(contextProvider, routes);
        }
    }

    private final Provider<RoutingContext> contextProvider;
    private final List<Route<PathPattern>> acceptedRoutes;

    /**
     * Constructs route methodAcceptorPair that uses {@link PathPattern} instances for
     * patch matching.
     *
     * @param provider {@link RoutingContext} injection provider.
     * @param routes   next-level request routers to be returned in case the router matching
     *                 the built router is successful.
     */
    private PathPatternRouter(final Provider<RoutingContext> provider,
                              final List<Route<PathPattern>> routes) {

        this.contextProvider = provider;
        this.acceptedRoutes = routes;
    }

    @Override
    public Router.Continuation apply(final ContainerRequest request) {
        final RoutingContext rc = contextProvider.get();
        // Peek at matching information to obtain path to match
        String path = rc.getFinalMatchingGroup();

        for (final Route<PathPattern> acceptedRoute : acceptedRoutes) {
            final MatchResult m = acceptedRoute.routingPattern().match(path);
            if (m != null) {
                // Push match result information and rest of path to match
                rc.pushMatchResult(m);
                rc.pushTemplate(acceptedRoute.routingPattern().getTemplate());

                return Router.Continuation.of(request, acceptedRoute.next());
            }
        }

        // No match
        return Router.Continuation.of(request);
    }
}
