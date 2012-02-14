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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.core.Request;

import org.glassfish.jersey.internal.util.collection.Pair;
import org.glassfish.jersey.internal.util.collection.Tuples;
import org.glassfish.jersey.process.internal.Stages;
import org.glassfish.jersey.process.internal.TreeAcceptor;
import org.glassfish.jersey.server.internal.routing.RouterModule.RoutingContext;

import org.glassfish.hk2.Factory;

import org.jvnet.hk2.annotations.Inject;

import com.google.common.collect.Iterators;

/**
 *
 * @author Paul Sandoz
 */
public class PatternRouteAcceptor implements TreeAcceptor {

    public class Builder {

        private final Factory<RoutingContext> contextProvider;

        public Builder(@Inject Factory<RoutingContext> contextProvider) {
            this.contextProvider = contextProvider;
        }

        public PatternRouteAcceptor build(List<Pair<Pattern, List<Factory<TreeAcceptor>>>> routes) {
            return new PatternRouteAcceptor(contextProvider, routes);
        }
    }
    private final Factory<RoutingContext> contextProvider;
    private final List<Pair<Pattern, List<Factory<TreeAcceptor>>>> acceptedRoutes;

    private PatternRouteAcceptor(Factory<RoutingContext> contextProvider,
            List<Pair<Pattern, List<Factory<TreeAcceptor>>>> routes) {

        this.contextProvider = contextProvider;
        this.acceptedRoutes = routes;
    }

    @Override
    public Pair<Request, Iterator<TreeAcceptor>> apply(final Request request) {
        final RoutingContext rc = contextProvider.get();
        // Peek at matching information to obtain the remaining path to match
        String path = rc.getFinalMatchingGroup();

        /**
         * TODO this is fragile. Dealing with '/' is always problematic!
         *
         * For hierarchical matching Jersey modifies the regular
         * expressions generated from @Path as follows:
         * 1) prefix a '/' if one is not present.
         * 2) remove any '/' if present from end, and postfix with a
         *    '(/.*)?' or '(/)?' matching group
         * This "normalization" simplifies the matching algorithm
         * and enables the support of automatic redirection.
         * These cover a lot of cases but there are cases where this does not
         * apply, i suspect in these cases one breaks out of the hierarchical
         * matching (and may potentially explicitly go into another separate
         * hierarchical matching process).
         *
         */
        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        for (final Pair<Pattern, List<Factory<TreeAcceptor>>> acceptedRoute : acceptedRoutes) {
            final Matcher m = acceptedRoute.left().matcher(path);
            if (m.matches()) {
                // Push match result information and rest of path to match
                rc.pushMatchResult(m.toMatchResult());

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
