/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2015 Oracle and/or its affiliates. All rights reserved.
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

import java.util.Collections;

import org.glassfish.jersey.server.internal.process.RequestProcessingContext;

/**
 * Hierarchical request router that can be used to create dynamic routing tree
 * structures.  Each routing tree can be executed using a dedicated
 * {@link RoutingStage routing stage}.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
interface Router {
    /**
     * Hierarchical request routing continuation.
     * <p>
     * A continuation of a hierarchical request routing is represented
     * by an ordered collection of next level of routers resulting
     * in a hierarchical depth-first (depth-only) request routing.
     * </p>
     */
    public static final class Continuation {
        private final RequestProcessingContext requestProcessingContext;
        private final Iterable<Router> next;

        /**
         * Create a terminal continuation from the routed request.
         *
         * @param result routed request.
         * @return terminal continuation with no {@link #next() next level routers}
         *         in the routing hierarchy and the supplied routed request.
         */
        static Continuation of(final RequestProcessingContext result) {
            return new Continuation(result, null);
        }

        /**
         * Create a continuation from the routed request and a collection
         * of next level routers.
         *
         * @param result routed request.
         * @param next   next level routers.
         * @return a continuation with the supplied next level routers to be invoked
         *         {@link #next() next} in the routing chain and the supplied routed
         *         request.
         */
        static Continuation of(final RequestProcessingContext result, Iterable<Router> next) {
            return new Continuation(result, next);
        }

        /**
         * Create a continuation from the routed request and a single
         * of next level routers.
         *
         * @param request routed request.
         * @param next    next level router.
         * @return a continuation with the supplied next level router to be invoked
         *         {@link #next() next} in the routing chain and the supplied routed
         *         request.
         */
        static Continuation of(final RequestProcessingContext request, final Router next) {
            return new Continuation(request, Collections.singletonList(next));
        }

        private Continuation(final RequestProcessingContext request, final Iterable<Router> next) {
            this.requestProcessingContext = request;
            this.next = (next == null) ? Collections.<Router>emptyList() : next;
        }

        /**
         * Get the routed request context.
         *
         * @return routed request context.
         */
        RequestProcessingContext requestContext() {
            return requestProcessingContext;
        }

        /**
         * Get the next level routers to be invoked or {@code an empty} if no next
         * level routers are present.
         *
         * @return the next level routers to be invoked or an empty collection if not
         *         present.
         */
        Iterable<Router> next() {
            return next;
        }
    }

    /**
     * Performs a request routing task and returns the routed request together with
     * a {@link Continuation routing continuation}.
     *
     * @param data data to be transformed.
     * @return a processing continuation.
     */
    public Continuation apply(RequestProcessingContext data);
}
