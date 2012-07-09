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

import org.glassfish.jersey.internal.inject.Providers;
import org.glassfish.jersey.server.internal.routing.Router.Builder;

import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;

import com.google.common.collect.Lists;

/**
 * Abstract request routing hierarchy builder.
 *
 * @param <T> routing pattern type.
 * @author Paul Sandoz
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
abstract class AbstractRouteToPathBuilder<T> implements RouterBinder.RouteToPathBuilder<T> {

    private final ServiceLocator serviceLocator;
    private final List<Route<T>> acceptedRoutes = Lists.newLinkedList();
    private List<Factory<Router>> currentRouters;

    /**
     * Initialize the abstract {@link RouterBinder.RouteToPathBuilder route to path builder}.
     *
     * @param serviceLocator HK2 service locator.
     * @param pattern  request path routing pattern.
     */
    protected AbstractRouteToPathBuilder(ServiceLocator serviceLocator, T pattern) {
        this.serviceLocator = serviceLocator;
        _route(pattern);
    }

    /**
     * Complete the currently built sub-route and start building a new one.
     *
     * The completed route is added to the list of the accepted routes.
     *
     * @param pattern routing pattern for the new sub-route.
     * @return updated builder.
     */
    protected final RouterBinder.RouteToBuilder<T> _route(T pattern) {
        currentRouters = Lists.newLinkedList();
        acceptedRoutes.add(Route.of(pattern, currentRouters));
        return this;
    }

    /**
     * Add new stage to the currently built sub-route.
     *
     * @param pa stage provider.
     * @return updated builder.
     */
    @SuppressWarnings("unchecked")
    protected final RouterBinder.RouteToPathBuilder<T> _to(Factory<? extends Router> pa) {
        currentRouters.add((Factory<Router>) pa);
        return this;
    }

    /**
     * Get the list of the registered sub-routes.
     *
     * @return list of the registered sub-routes.
     */
    protected List<Route<T>> acceptedRoutes() {
        return acceptedRoutes;
    }

    // RouteToBuilder<T>
    @Override
    public final RouterBinder.RouteToPathBuilder<T> to(Router.Builder ab) {
        return to(ab.build());
    }

    @Override
    public final RouterBinder.RouteToPathBuilder<T> to(final Router a) {
        // TODO    return to(Providers.of(a));

        return to(Providers.factoryOf(a));
    }

    @Override
    public final RouterBinder.RouteToPathBuilder<T> to(Class<? extends Router> ca) {
        final ServiceHandle<? extends Router> serviceHandle = serviceLocator.getServiceHandle(ca);
        Factory<? extends Router> factory = new Factory<Router>() {
            @Override
            public Router provide() {
                return serviceHandle.getService();
            }

            @Override
            public void dispose(Router instance) {
                //not used
            }
        };
        return to(factory);
    }

    @Override
    public final RouterBinder.RouteToPathBuilder<T> to(Factory<? extends Router> pa) {
        return _to(pa);
    }

    // RouteBuilder<T>
    @Override
    public abstract RouterBinder.RouteToBuilder<T> route(String pattern);

    @Override
    public RouterBinder.RouteToBuilder<T> route(T pattern) {
        return _route(pattern);
    }

    // Router.Builder
    @Override
    public Builder child(Router child) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public abstract Router build();
}
