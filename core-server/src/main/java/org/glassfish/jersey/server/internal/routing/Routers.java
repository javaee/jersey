/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2015 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.jersey.server.internal.process.Endpoint;
import org.glassfish.jersey.server.internal.process.RequestProcessingContext;

/**
 * Routing tree assembly utilities.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
final class Routers {

    private static final Router IDENTITY_ROUTER = new Router() {

        @Override
        public Continuation apply(final RequestProcessingContext data) {
            return Continuation.of(data);
        }
    };

    private Routers() {
        throw new AssertionError("No instances of this class.");
    }

    /**
     * Create a terminal "no-op router" that accepts any input context and returns the unchanged request and an empty
     * continuation iterator.
     *
     * @return a terminal "no-op" router.
     */
    public static Router noop() {
        return IDENTITY_ROUTER;
    }

    /**
     * Creates a terminal {@link Router} that wraps the given {@link org.glassfish.jersey.server.internal.process.Endpoint
     * endpoint}.
     *
     * The {@link Router#apply} method of the created hierarchical router returns the unchanged request and an empty
     * continuation iterator.
     *
     * @param endpoint a server-side endpoint to be wrapped in a router instance.
     * @return a router that wraps the supplied endpoint.
     */
    public static Router endpoint(final Endpoint endpoint) {
        return new EndpointRouter(endpoint);
    }

    /**
     * Extract endpoint stored in a router (if any).
     *
     * @param router router from which a server endpoint should be extracted.
     * @return extracted endpoint or {@code null} if there was no endpoint stored in the router.
     */
    public static Endpoint extractEndpoint(final Router router) {
        if (router instanceof EndpointRouter) {
            return ((EndpointRouter) router).endpoint;
        }

        return null;
    }

    private static class EndpointRouter implements Router {

        private final Endpoint endpoint;

        public EndpointRouter(final Endpoint endpoint) {
            this.endpoint = endpoint;
        }

        @Override
        public Continuation apply(RequestProcessingContext context) {
            return Continuation.of(context);
        }
    }
}
