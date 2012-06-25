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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.glassfish.jersey.server.ContainerRequest;

import org.glassfish.hk2.Factory;

import org.jvnet.hk2.annotations.Inject;

/**
 * Matches the un-matched right-hand request path to a configured {@link Pattern pattern}.
 *
 * @author Paul Sandoz
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
class PatternRouter implements Router {

    /**
     * "Assisted injection" factory interface for {@link PatternRouter}.
     *
     * See also <a href="http://code.google.com/p/google-guice/wiki/AssistedInject">
     * assisted injection in Guice</a>.
     */
    public static class Builder {

        @Inject
        private Factory<RoutingContext> contextProvider;

        /**
         * Build a pattern request router.
         *
         * @param routes next-level request pre-processing stages to be returned in case the request
         *               matching in the built router is successful.
         * @return a pattern request router.
         */
        public PatternRouter build(List<Route<Pattern>> routes) {
            return new PatternRouter(contextProvider, routes);
        }
    }

    private final Factory<RoutingContext> contextProvider;
    private final List<Route<Pattern>> acceptedRoutes;

    private PatternRouter(Factory<RoutingContext> contextProvider,
                          List<Route<Pattern>> routes) {

        this.contextProvider = contextProvider;
        this.acceptedRoutes = routes;
    }

    @Override
    public Continuation apply(final ContainerRequest request) {
        final RoutingContext rc = contextProvider.get();
        // Peek at matching information to obtain the remaining path to match
        String path = rc.getFinalMatchingGroup();

        /**
         * For hierarchical matching Jersey modifies the regular
         * expressions generated from @Path as follows:
         * 1) prefix a '/' if one is not present.
         * 2) remove any '/' if present from end, and postfix with a
         *    '(/.*)?' or '(/)?' matching group
         * This "normalization" simplifies the matching algorithm
         * and enables the support of automatic redirection.
         */
        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        for (final Route<Pattern> acceptedRoute : acceptedRoutes) {
            final Matcher m = acceptedRoute.routingPattern().matcher(path);
            if (m.matches()) {
                // Push match result information and rest of path to match
                rc.pushMatchResult(m.toMatchResult());

                return Continuation.of(request, acceptedRoute.next());
            }
        }

        // No match
        return Continuation.of(request);
    }
}
