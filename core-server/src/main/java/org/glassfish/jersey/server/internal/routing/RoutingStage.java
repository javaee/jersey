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

import org.glassfish.jersey.process.internal.AbstractChainableStage;
import org.glassfish.jersey.process.internal.Inflecting;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ContainerResponse;

import org.glassfish.hk2.Factory;

import org.jvnet.hk2.annotations.Inject;

/**
 * Request pre-processing stage that encapsulates hierarchical resource matching
 * and request routing.
 *
 * Once the routing is finished, an inflector (if found) is {@link
 * RoutingContext#setInflector(org.glassfish.jersey.process.Inflector) stored in the
 * routing context}.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @see RoutedInflectorExtractorStage
 */
public class RoutingStage extends AbstractChainableStage<ContainerRequest> {
    /**
     * Injectable {@link RoutingStage resource matching stage} builder.
     */
    public static class Builder {
        @Inject
        private Factory<RoutingContext> routingContextFactory;

        /**
         * Build a properly injected resource matching router.
         *
         * @param routingRoot root matching router.
         * @return properly injected resource matching router.
         */
        public RoutingStage build(final Router routingRoot) {
            return new RoutingStage(routingRoot, routingContextFactory);
        }
    }

    private final Router routingRoot;
    private final Factory<RoutingContext> routingContextFactory;

    private RoutingStage(final Router routingRoot,
                         final Factory<RoutingContext> routingContextFactory) {
        this.routingRoot = routingRoot;
        this.routingContextFactory = routingContextFactory;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Routing stage navigates through the nested {@link Router routing hierarchy}
     * using a depth-first transformation strategy until a request-to-response
     * inflector is {@link org.glassfish.jersey.process.internal.Inflecting found on
     * a leaf stage node}, in which case the request routing is terminated and an
     * {@link org.glassfish.jersey.process.Inflector inflector} (if found) is pushed
     * to the {@link RoutingContext routing context}.
     */
    @Override
    public Continuation<ContainerRequest> apply(ContainerRequest request) {
        final TransformableData<ContainerRequest, ContainerResponse> result =
                _apply(request, routingRoot);

        if (result.hasInflector()) {
            routingContextFactory.get().setInflector(result.inflector());
        }

        return Continuation.of(result.data(), getDefaultNext());
    }

    @SuppressWarnings("unchecked")
    private TransformableData<ContainerRequest, ContainerResponse> _apply(
            final ContainerRequest request, final Router router) {

        final Router.Continuation continuation = router.apply(request);

        for (Router child : continuation.next()) {
            TransformableData<ContainerRequest, ContainerResponse> result =
                    _apply(continuation.requestContext(), child);

            if (result.hasInflector()) {
                // we're done
                return result;
            } // else continue
        }


        if (router instanceof Inflecting) {
            // inflector at terminal stage found
            return TransformableData.of(continuation.requestContext(),
                    ((Inflecting<ContainerRequest, ContainerResponse>) router).inflector());
        }

        // inflector at terminal stage not found
        return TransformableData.of(continuation.requestContext());
    }
}
