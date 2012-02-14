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
import org.glassfish.jersey.internal.util.collection.Pair;
import org.glassfish.jersey.internal.util.collection.Tuples;
import org.glassfish.jersey.process.internal.TreeAcceptor;
import org.glassfish.jersey.process.internal.TreeAcceptor.Builder;

import org.glassfish.hk2.Factory;
import org.glassfish.hk2.Services;

import com.google.common.collect.Lists;

/**
 *
 * @author Paul Sandoz
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
abstract class AbstractRouteToPathBuilder<T> implements RouterModule.RouteToPathBuilder<T> {

    private final Services services;
    private final List<Pair<T, List<Factory<TreeAcceptor>>>> acceptedRoutes = Lists.newLinkedList();
    private List<Factory<TreeAcceptor>> currentAcceptors;

    protected AbstractRouteToPathBuilder(Services services, T pattern) {
        this.services = services;
        _route(pattern);
    }

    protected final RouterModule.RouteToBuilder<T> _route(T pattern) {
        currentAcceptors = Lists.newLinkedList();
        acceptedRoutes.add(Tuples.of(pattern, currentAcceptors));
        return this;
    }

    @SuppressWarnings("unchecked")
    protected final RouterModule.RouteToPathBuilder<T> _to(Factory<? extends TreeAcceptor> pa) {
        currentAcceptors.add((Factory<TreeAcceptor>) pa);
        return this;
    }

    protected List<Pair<T, List<Factory<TreeAcceptor>>>> acceptedRoutes() {
        return acceptedRoutes;
    }

    // RouteToBuilder<T>
    @Override
    public final RouterModule.RouteToPathBuilder<T> to(TreeAcceptor.Builder ab) {
        return to(ab.build());
    }

    @Override
    public final RouterModule.RouteToPathBuilder<T> to(final TreeAcceptor a) {
        // TODO    return to(Providers.of(a));

        return to(Providers.factoryOf(a));
    }

    @Override
    public final RouterModule.RouteToPathBuilder<T> to(Class<? extends TreeAcceptor> ca) {
        return to(Providers.asFactory(services.forContract(ca).getProvider()));
    }

    @Override
    public final RouterModule.RouteToPathBuilder<T> to(Factory<? extends TreeAcceptor> pa) {
        return _to(pa);
    }

    // RouteBuilder<T>
    @Override
    public abstract RouterModule.RouteToBuilder<T> route(String pattern);

    @Override
    public RouterModule.RouteToBuilder<T> route(T pattern) {
        return _route(pattern);
    }

    // TreeAcceptor.Builder
    @Override
    public Builder child(TreeAcceptor child) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public abstract TreeAcceptor build();
}
