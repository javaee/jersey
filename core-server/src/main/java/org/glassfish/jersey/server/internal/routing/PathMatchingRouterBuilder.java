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
package org.glassfish.jersey.server.internal.routing;

import java.util.LinkedList;
import java.util.List;

import org.glassfish.jersey.uri.PathPattern;

/**
 /**
 * A request path pattern matching router hierarchy builder entry point.
 *
 * @author Paul Sandoz
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
final class PathMatchingRouterBuilder implements PathToRouterBuilder {

    private final List<Route> acceptedRoutes = new LinkedList<>();
    private List<Router> currentRouters;


    /**
     * Create new request path pattern matching router builder.
     *
     * @param pattern request path matching pattern.
     * @return new request path pattern matching router builder.
     */
    static PathToRouterBuilder newRoute(final PathPattern pattern) {
        final PathMatchingRouterBuilder builder = new PathMatchingRouterBuilder();
        builder.startNewRoute(pattern);
        return builder;
    }

    private PathMatchingRouterBuilder() {
        // preventing direct instantiation
    }

    private void startNewRoute(final PathPattern pattern) {
        currentRouters = new LinkedList<>();
        acceptedRoutes.add(Route.of(pattern, currentRouters));
    }

    /**
     * Get the list of the registered sub-routes.
     *
     * @return list of the registered sub-routes.
     */
    protected List<Route> acceptedRoutes() {
        return acceptedRoutes;
    }

    @Override
    public PathMatchingRouterBuilder to(final Router router) {
        currentRouters.add(router);
        return this;
    }

    /**
     * Complete the currently built unfinished sub-route (if any) and start building a new one.
     *
     * The completed sub-route is added to the list of the routes accepted by the router that is being built.
     *
     * @param pattern routing pattern for the new sub-route.
     * @return updated router builder.
     */
    public PathToRouterBuilder route(final PathPattern pattern) {
        startNewRoute(pattern);
        return this;
    }

    /**
     * Build a {@link org.glassfish.jersey.server.internal.routing.Router hierarchical request path matching processor}.
     *
     * @return hierarchical request path matching processor (i.e. router).
     */
    public PathMatchingRouter build() {
        return new PathMatchingRouter(acceptedRoutes());
    }

}
