/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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

import javax.inject.Inject;
import javax.inject.Provider;

import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.model.Resource;

/**
 * Router that pushes matched resource or child resource to {@link RoutingContext routing context}.
 * The router can work in child resource mode in which it pushes resource as child resource.
 *
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 */
class PushMatchedResourceRouter implements Router {

    /**
     * Builder for creating {@link PushMatchedResourceRouter push matched resource router} instances.
     */
    static class Builder {
        @Inject
        private Provider<RoutingContext> routingContext;

        private Resource resource;
        private boolean childResourceMode;


        /**
         * Builds new instance of router.
         * @return Instances created from builder.
         */
        PushMatchedResourceRouter build() {
            return new PushMatchedResourceRouter(resource, childResourceMode, routingContext);
        }


        /**
         * Set the resource or child resources.
         * @param resource Resource or child resource.
         * @return Builder.
         */
        Builder setResource(Resource resource) {
            this.resource = resource;
            return this;
        }

        /**
         * Sets flag denoting whether resource set by {@link #setResource(org.glassfish.jersey.server.model.Resource)}
         * is child resource.
         *
         * @param childResourceMode {@code true} if the resource is child resource, {@code false} otherwise.
         * @return Builder.
         */
        Builder setChildResourceMode(boolean childResourceMode) {
            this.childResourceMode = childResourceMode;
            return this;
        }
    }


    private final Resource resource;
    private final boolean childResourceMode;

    private final Provider<RoutingContext> routingContext;

    private PushMatchedResourceRouter(Resource resource, boolean childResourceMode, Provider<RoutingContext> routingContext) {
        this.resource = resource;
        this.childResourceMode = childResourceMode;
        this.routingContext = routingContext;
    }

    @Override
    public Continuation apply(ContainerRequest data) {
        routingContext.get().pushMatchedResource(resource, childResourceMode);
        return Continuation.of(data);
    }
}
