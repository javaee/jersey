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
package org.glassfish.jersey.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.internal.inject.AbstractModule;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.process.internal.AbstractChainableStage;
import org.glassfish.jersey.process.internal.PriorityComparator;
import org.glassfish.jersey.process.internal.ResponseProcessor;
import org.glassfish.jersey.process.internal.Stages;
import org.glassfish.jersey.server.internal.routing.RoutingContext;

import org.glassfish.hk2.Factory;
import org.glassfish.hk2.Services;

import org.jvnet.hk2.annotations.Inject;

/**
 * Container filtering stage responsible for execution of request and response filters
 * on each request-response message exchange.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @author Martin Matula (martin.matula at oracle.com)
 */
class ContainerFilteringStage extends AbstractChainableStage<ContainerRequest> {

    private final Factory<ResponseProcessor.RespondingContext<ContainerResponse>> respondingContextFactory;
    private final Services services;
    private final List<ContainerRequestFilter> requestFilters;
    private final List<ContainerResponseFilter> responseFilters;

    /**
     * Injectable container filtering stage builder.
     */
    static class Builder {
        @Inject
        private Factory<ResponseProcessor.RespondingContext<ContainerResponse>> respondingContextFactory;

        @Inject
        private Services services;

        /**
         * Build a new container filtering stage specifying global request and response filters. This stage class
         * is reused for both pre and post match filtering phases.
         * <p>
         * All global response filters are passed in the pre-match stage, since if a pre-match filter aborts,
         * response filters should still be executed. For the post-match filter stage creation, {@code null} is passed
         * to the responseFilters parameter.
         * </p>
         *
         * @param requestFilters list of global (unbound) request filters (either pre or post match - depending on the
         *                       stage being created).
         * @param responseFilters list of global response filters (for pre-match stage) or {@code null} (for post-match
         *                        stage).
         * @return new container filtering stage.
         */
        public ContainerFilteringStage build(List<ContainerRequestFilter> requestFilters,
                                             List<ContainerResponseFilter> responseFilters) {
            return new ContainerFilteringStage(respondingContextFactory, services,
                    requestFilters, responseFilters);
        }

    }

    /**
     * Injection constructor.
     *
     * @param respondingContextFactory responding context factory.
     * @param services HK2 services.
     * @param requestFilters global request filters (pre or post match).
     * @param responseFilters global response filters or {@code null}.
     *
     */
    private ContainerFilteringStage(
            Factory<ResponseProcessor.RespondingContext<ContainerResponse>> respondingContextFactory,
            Services services,
            List<ContainerRequestFilter> requestFilters,
            List<ContainerResponseFilter> responseFilters
    ) {
        this.respondingContextFactory = respondingContextFactory;
        this.services = services;
        this.requestFilters = requestFilters;
        this.responseFilters = responseFilters;
    }

    @Override
    public Continuation<ContainerRequest> apply(ContainerRequest requestContext) {
        List<ContainerRequestFilter> sortedRequestFilters;

        if (responseFilters == null) {
            // post-matching (response filter stage is pushed in pre-matching phase, so that if pre-matching filter
            // throws exception, response filters get still invoked)
            RoutingContext rc = services.forContract(RoutingContext.class).get();
            sortedRequestFilters = new ArrayList<ContainerRequestFilter>(requestFilters);
            sortedRequestFilters.addAll(rc.getBoundRequestFilters());
            Collections.sort(sortedRequestFilters,
                    new PriorityComparator<ContainerRequestFilter>(PriorityComparator.Order.ASCENDING));
        } else {
            // pre-matching
            respondingContextFactory.get().push(new ResponseFilterStage(responseFilters, services));
            sortedRequestFilters = requestFilters;
        }

        for (ContainerRequestFilter filter : sortedRequestFilters) {
            try {
                filter.filter(requestContext);
                final Response abortResponse = requestContext.getAbortResponse();
                if (abortResponse != null) {
                    // abort accepting & return response
                    return Continuation.of(requestContext, Stages.asStage(
                            new Inflector<ContainerRequest, ContainerResponse>() {
                                @Override
                                public ContainerResponse apply(
                                        final ContainerRequest requestContext) {
                                    return new ContainerResponse(requestContext, abortResponse);
                                }
                            }));
                }
            } catch (IOException ex) {
                final Response abortResponse = requestContext.getAbortResponse();
                if (abortResponse == null) {
                    throw new WebApplicationException(ex);
                } else {
                    throw new WebApplicationException(ex, abortResponse);
                }
            }
        }
        return Continuation.of(requestContext, getDefaultNext());
    }

    private static class ResponseFilterStage extends AbstractChainableStage<ContainerResponse> {
        private final List<ContainerResponseFilter> filters;
        private final Services services;

        private ResponseFilterStage(List<ContainerResponseFilter> filters, Services services) {
            this.filters = filters;
            this.services = services;
        }

        @Override
        public Continuation<ContainerResponse> apply(ContainerResponse responseContext) {
            try {
                RoutingContext rc = services.forContract(RoutingContext.class).get();

                List<ContainerResponseFilter> sortedResponseFilters = new ArrayList<ContainerResponseFilter>(filters);
                if (rc != null) {
                    sortedResponseFilters.addAll(rc.getBoundResponseFilters());
                }
                Collections.sort(sortedResponseFilters,
                        new PriorityComparator<ContainerResponseFilter>(PriorityComparator.Order.DESCENDING));

                for (ContainerResponseFilter filter : sortedResponseFilters) {
                    filter.filter(responseContext.getRequestContext(), responseContext);
                }
            } catch (IOException ex) {
                throw new WebApplicationException(ex);
            }

            return Continuation.of(responseContext, getDefaultNext());
        }
    }

    /**
     * Container filter processing injection binding module.
     */
    static class Module extends AbstractModule {

        @Override
        protected void configure() {
            bind().to(ContainerFilteringStage.Builder.class);
        }
    }
}
