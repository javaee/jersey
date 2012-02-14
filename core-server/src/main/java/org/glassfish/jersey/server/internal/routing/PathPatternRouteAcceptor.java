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

import java.util.Iterator;
import java.util.List;
import java.util.regex.MatchResult;

import javax.ws.rs.core.Request;

import org.glassfish.jersey.internal.util.collection.Pair;
import org.glassfish.jersey.internal.util.collection.Tuples;
import org.glassfish.jersey.process.internal.Stages;
import org.glassfish.jersey.process.internal.TreeAcceptor;
import org.glassfish.jersey.server.internal.routing.RouterModule.RoutingContext;
import org.glassfish.jersey.uri.PathPattern;

import org.glassfish.hk2.Factory;

import org.jvnet.hk2.annotations.Inject;

import com.google.common.collect.Iterators;

/**
 *
 * @author Paul Sandoz
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class PathPatternRouteAcceptor implements TreeAcceptor {

    /**
     * "Assisted injection" factory interface for {@link PathPatternRouteTreeAcceptor}.
     *
     * @see com.google.inject.assistedinject.FactoryModuleBuilder
     */
    public class Builder {

        private final Factory<RoutingContext> contextProvider;

        public Builder(@Inject Factory<RoutingContext> contextProvider) {
            this.contextProvider = contextProvider;
        }

        public PathPatternRouteAcceptor build(List<Pair<PathPattern, List<Factory<TreeAcceptor>>>> routes) {
            return new PathPatternRouteAcceptor(contextProvider, routes);
        }
    }
    private final Factory<RoutingContext> contextProvider;
    private final List<Pair<PathPattern, List<Factory<TreeAcceptor>>>> acceptedRoutes;

    /**
     * Constructs route acceptor that uses {@link PathPattern} instances for
     * patch matching.
     *
     * @param provider {@link RoutingContext} injection provider
     * @param routes
     */
    private PathPatternRouteAcceptor(final Factory<RoutingContext> provider,
            final List<Pair<PathPattern, List<Factory<TreeAcceptor>>>> routes) {

        this.contextProvider = provider;
        this.acceptedRoutes = routes;
    }

    @Override
    public Pair<Request, Iterator<TreeAcceptor>> apply(final Request request) {
        final RoutingContext rc = contextProvider.get();
        // Peek at matching information to obtain path to match
        String path = rc.getFinalMatchingGroup();

        for (final Pair<PathPattern, List<Factory<TreeAcceptor>>> acceptedRoute : acceptedRoutes) {
            final MatchResult m = acceptedRoute.left().match(path);
            if (m != null) {
                // Push match result information and rest of path to match
                rc.pushMatchResult(m);
                rc.pushTemplate(acceptedRoute.left().getTemplate());

                final Iterator<TreeAcceptor> acceptors;

                final List<Factory<TreeAcceptor>> acceptorProviders = acceptedRoute.right();
                if (acceptorProviders.isEmpty()) {
                    acceptors = Iterators.emptyIterator();
                } else if (acceptorProviders.size() == 1) {
                    acceptors = Iterators.transform(
                            Iterators.singletonIterator(acceptorProviders.iterator().next()),
                            RouterModule.FACTORY_TO_ACCEPTOR_TRANSFORMATION);
                } else {
                    acceptors = Iterators.transform(
                            acceptorProviders.iterator(),
                            RouterModule.FACTORY_TO_ACCEPTOR_TRANSFORMATION);
                }

                return Tuples.of(request, acceptors);
            }
        }

        // No match
        return Stages.terminalTreeContinuation(request);
    }
}
