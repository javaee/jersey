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

/**
 * An intermediate path matching router builder.
 *
 * This builder completes a routing information for a single routed {@link org.glassfish.jersey.uri.PathPattern}.
 * In case the unmatched right-hand part of the request path is matched by the routed path pattern, the request
 * processing context will be serially routed to all the child routers attached to the routing pattern using this
 * routing completion builder.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
@SuppressWarnings("ClassReferencesSubclass")
interface PathToRouterBuilder {

    /**
     * Register a new next-level router to be used for request routing in case the routing pattern matches the
     * unmatched right-hand part of the request path.
     *
     * @param router new next-level router to be registered with the routed path pattern.
     * @return updated route builder ready to build a new {@link Router router} instance
     * (or add more routes to the currently built one).
     */
    PathMatchingRouterBuilder to(Router router);
}
