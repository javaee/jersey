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
package org.glassfish.jersey.server.model;

import java.util.Iterator;

import javax.ws.rs.core.Request;

import org.glassfish.jersey.internal.util.collection.Pair;
import org.glassfish.jersey.process.internal.TreeAcceptor;
import org.glassfish.jersey.server.internal.routing.RouterModule.RoutingContext;

import org.glassfish.hk2.Factory;
import org.glassfish.hk2.inject.Injector;

/**
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
class PushResourceAndDelegateTreeAcceptor implements TreeAcceptor {
    final Injector injector;
    final Factory<RoutingContext> routingCtx;
    final ResourceClass rc;
    final TreeAcceptor acceptor;

    PushResourceAndDelegateTreeAcceptor(final Injector injector, final Factory<RoutingContext> routingContextFactory, final ResourceClass resource, final TreeAcceptor wrappedAcceptor) {
        this.injector = injector;
        this.routingCtx = routingContextFactory;
        this.rc = resource;
        this.acceptor = wrappedAcceptor;
    }

    @Override
    public Pair<Request, Iterator<TreeAcceptor>> apply(Request data) {
        if (rc != null) {
            routingCtx.get().pushMatchedResource(this.injector.inject(rc.getResourceClass()));
        }
        routingCtx.get().pushLeftHandPath();
        return acceptor.apply(data);
    }
}
