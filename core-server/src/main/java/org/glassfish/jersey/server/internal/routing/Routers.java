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
import java.util.LinkedList;
import java.util.List;

import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.process.internal.Inflecting;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ContainerResponse;

import com.google.common.base.Function;

/**
 * Routing tree assembly utilities.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
class Routers {

    private Routers() {
        // prevents instantiation
    }

    /**
     * Creates a leaf-node {@link Router} that implements {@link org.glassfish.jersey.process.internal.Inflecting}
     * interface and returns the provided {@link org.glassfish.jersey.process.Inflector} instance
     * when the {@link org.glassfish.jersey.process.internal.Inflecting#inflector()} method is called.
     * {@link Router#apply(org.glassfish.jersey.server.ContainerRequest)} method of the created
     * hierarchical router returns the unchanged request and an empty continuation iterator.
     *
     * @param inflector a request to response transformation to be wrapped in an
     *                  {@code Router} instance.
     * @return an {@code Router} that wraps the supplied {@code Inflector}.
     */
    @SuppressWarnings("unchecked")
    public static Router asTreeAcceptor(final Inflector<ContainerRequest, ContainerResponse> inflector) {
        return new InflectingRouter(inflector);
    }

    /**
     * Start building a hierarchical routing tree.
     *
     * @param transformation root transformation function.
     * @return hierarchical accepting tree builder.
     */
    public static Router.Builder acceptingTree(
            Function<ContainerRequest, ContainerRequest> transformation) {
        return new RouterBuilder(transformation);
    }

    private static class InflectingRouter
            implements Router, Inflecting<ContainerRequest, ContainerResponse> {

        private final Inflector<ContainerRequest, ContainerResponse> inflector;

        public InflectingRouter(final Inflector<ContainerRequest, ContainerResponse> inflector) {
            this.inflector = inflector;
        }

        @Override
        public Inflector<ContainerRequest, ContainerResponse> inflector() {
            return inflector;
        }

        @Override
        public Continuation apply(ContainerRequest request) {
            return Continuation.of(request);
        }
    }

    private static class RouterBuilder implements Router.Builder {

        private final Function<ContainerRequest, ContainerRequest> transformation;
        private List<Router> children;

        public RouterBuilder(Function<ContainerRequest, ContainerRequest> transformation) {
            this.transformation = transformation;
        }

        @Override
        public Router.Builder child(Router child) {
            if (children == null) {
                children = new LinkedList<Router>();
            }
            children.add(child);

            return this;
        }

        @Override
        public Router build() {
            return (children == null)
                    ? new LinkedRouter(transformation) : new LinkedRouter(transformation, children);
        }
    }

    private static class LinkedRouter implements Router {

        private final Function<ContainerRequest, ContainerRequest> transformation;
        private final List<Router> children;

        public LinkedRouter(
                Function<ContainerRequest, ContainerRequest> transformation, List<Router> children) {
            this.transformation = transformation;
            this.children = children;
        }

        public LinkedRouter(Function<ContainerRequest, ContainerRequest> transformation) {
            this.transformation = transformation;
            this.children = Collections.emptyList();
        }

        @Override
        public Continuation apply(ContainerRequest data) {
            return Continuation.of(transformation.apply(data), children);
        }
    }
}
