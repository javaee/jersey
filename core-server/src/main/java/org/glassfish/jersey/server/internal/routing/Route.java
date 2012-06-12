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

import java.util.Collections;
import java.util.List;

import org.glassfish.hk2.Factory;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

/**
 * Request routing information. Contains a {@link #routingPattern() routing pattern}
 * and a {@link #next() list of next-level stages} to be processed in case the
 * routing pattern successfully matches the un-matched right-hand part of the request.
 *
 * @param <T> routing pattern type.
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
final class Route<T> {
    /**
     * Transformation function that converts router provider into router.
     */
    private static final Function<Factory<Router>, Router> PROVIDER_TO_ROUTER_TRANSFORMATION =
            new Function<Factory<Router>, Router>() {

                @Override
                public Router apply(Factory<Router> f) {
                    return f.get();
                }
            };

    private final T routingPattern;
    private final List<Factory<Router>> routerProviders;

    /**
     * Create a new request route.
     *
     * @param <T>             routing pattern type.
     * @param routingPattern  request path routing pattern.
     * @param routerProviders next-level routers to be processed in case the routing
     *                        pattern matches the unmatched right-hand part of the request path.
     * @return new request route.
     */
    static <T> Route<T> of(T routingPattern, List<Factory<Router>> routerProviders) {
        return new Route<T>(routingPattern, routerProviders);
    }

    private Route(T routingPattern, List<Factory<Router>> routerProviders) {
        this.routingPattern = routingPattern;
        this.routerProviders = routerProviders;
    }

    /**
     * Get the request path routing pattern.
     *
     * @return request path routing pattern.
     */
    public T routingPattern() {
        return routingPattern;
    }

    /**
     * Get next-level routers to be processed in case the routing pattern matches
     * the unmatched right-hand part of the request path.
     *
     * @return routed next-level next.
     */
    public List<Router> next() {
        if (routerProviders.isEmpty()) {
            return Collections.emptyList();
        } else {
            return Lists.transform(routerProviders, Route.PROVIDER_TO_ROUTER_TRANSFORMATION);
        }
    }
}
