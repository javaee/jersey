/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2015 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.jersey.server.internal.process.RequestProcessingContext;
import org.glassfish.jersey.uri.UriTemplate;

/**
 * Router that pushes {@link UriTemplate uri template} of matched resource of subResource
 * to {@link org.glassfish.jersey.server.internal.routing.RoutingContext routing context}.
 * Before calling this router the {@link PathMatchingRouter} must be called which matches the path
 * and pushes the {@link java.util.regex.MatchResult matched result} into the routing context.
 *
 * @author Miroslav Fuksa
 * @see RoutingContext#pushTemplates(org.glassfish.jersey.uri.UriTemplate, org.glassfish.jersey.uri.UriTemplate)
 */
final class PushMatchedTemplateRouter implements Router {

    private final UriTemplate resourceTemplate;
    private final UriTemplate methodTemplate;

    /**
     * Create a new instance of the push matched template router.
     * <p>
     * This constructor should be used in case a path matching has been performed on both a resource and method paths
     * (in case of sub-resource methods and locators).
     * </p>
     *
     * @param resourceTemplate resource URI template that should be pushed.
     * @param methodTemplate   (sub-resource) method or locator URI template that should be pushed.
     */
    PushMatchedTemplateRouter(final UriTemplate resourceTemplate,
                              final UriTemplate methodTemplate) {
        this.resourceTemplate = resourceTemplate;
        this.methodTemplate = methodTemplate;
    }

    /**
     * Create a new instance of the push matched template router.
     * <p>
     * This constructor should be used in case a single path matching has been performed (in case of resource methods,
     * only the resource path is matched).
     * </p>
     *
     * @param resourceTemplate resource URI template that should be pushed.
     */
    PushMatchedTemplateRouter(final UriTemplate resourceTemplate) {
        this.resourceTemplate = resourceTemplate;
        this.methodTemplate = null;
    }

    @Override
    public Continuation apply(final RequestProcessingContext context) {
        context.routingContext().pushTemplates(resourceTemplate, methodTemplate);

        return Continuation.of(context);
    }
}
