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

import org.glassfish.jersey.message.internal.TracingLogger;
import org.glassfish.jersey.process.internal.AbstractChainableStage;
import org.glassfish.jersey.process.internal.Stage;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.internal.ServerTraceEvent;
import org.glassfish.jersey.server.internal.process.Endpoint;
import org.glassfish.jersey.server.internal.process.RequestProcessingContext;
import org.glassfish.jersey.server.monitoring.RequestEvent;

/**
 * Request pre-processing stage that encapsulates hierarchical resource matching
 * and request routing.
 *
 * Once the routing is finished, an endpoint (if matched) is
 * {@link RoutingContext#setEndpoint stored in the routing context}.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @see MatchedEndpointExtractorStage
 */
final class RoutingStage extends AbstractChainableStage<RequestProcessingContext> {

    private final Router routingRoot;

    /**
     * Create a new routing stage instance.
     *
     * @param routingRoot root router.
     */
     RoutingStage(final Router routingRoot) {
        this.routingRoot = routingRoot;
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
    public Continuation<RequestProcessingContext> apply(final RequestProcessingContext context) {
        final ContainerRequest request = context.request();
        context.triggerEvent(RequestEvent.Type.MATCHING_START);

        final TracingLogger tracingLogger = TracingLogger.getInstance(request);
        final long timestamp = tracingLogger.timestamp(ServerTraceEvent.MATCH_SUMMARY);
        try {
            final RoutingResult result = _apply(context, routingRoot);

            Stage<RequestProcessingContext> nextStage = null;
            if (result.endpoint != null) {
                context.routingContext().setEndpoint(result.endpoint);
                nextStage = getDefaultNext();
            }

            return Continuation.of(result.context, nextStage);
        } finally {
            tracingLogger.logDuration(ServerTraceEvent.MATCH_SUMMARY, timestamp);
        }
    }

    @SuppressWarnings("unchecked")
    private RoutingResult _apply(final RequestProcessingContext request, final Router router) {

        final Router.Continuation continuation = router.apply(request);

        for (Router child : continuation.next()) {
            RoutingResult result = _apply(continuation.requestContext(), child);

            if (result.endpoint != null) {
                // we're done
                return result;
            } // else continue
        }

        Endpoint endpoint = Routers.extractEndpoint(router);
        if (endpoint != null) {
            // inflector at terminal stage found
            return RoutingResult.from(continuation.requestContext(), endpoint);
        }

        // inflector at terminal stage not found
        return RoutingResult.from(continuation.requestContext());
    }

    private static final class RoutingResult {
        private final RequestProcessingContext context;
        private final Endpoint endpoint;

        private static RoutingResult from(final RequestProcessingContext requestProcessingContext, final Endpoint endpoint) {
            return new RoutingResult(requestProcessingContext, endpoint);
        }

        private static RoutingResult from(final RequestProcessingContext requestProcessingContext) {
            return new RoutingResult(requestProcessingContext, null);
        }

        private RoutingResult(final RequestProcessingContext context, final Endpoint endpoint) {
            this.context = context;
            this.endpoint = endpoint;
        }
    }
}
