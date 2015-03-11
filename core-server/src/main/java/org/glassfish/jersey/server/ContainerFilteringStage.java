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

package org.glassfish.jersey.server;

import java.util.ArrayList;

import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.internal.inject.Providers;
import org.glassfish.jersey.message.internal.TracingLogger;
import org.glassfish.jersey.model.internal.RankedComparator;
import org.glassfish.jersey.model.internal.RankedProvider;
import org.glassfish.jersey.process.internal.AbstractChainableStage;
import org.glassfish.jersey.process.internal.Stages;
import org.glassfish.jersey.server.internal.ServerTraceEvent;
import org.glassfish.jersey.server.internal.process.Endpoint;
import org.glassfish.jersey.server.internal.process.MappableException;
import org.glassfish.jersey.server.internal.process.RequestProcessingContext;
import org.glassfish.jersey.server.monitoring.RequestEvent;

/**
 * Container filtering stage responsible for execution of request and response filters
 * on each request-response message exchange.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @author Martin Matula
 */
class ContainerFilteringStage extends AbstractChainableStage<RequestProcessingContext> {

    private final Iterable<RankedProvider<ContainerRequestFilter>> requestFilters;
    private final Iterable<RankedProvider<ContainerResponseFilter>> responseFilters;

    /**
     * Create a new container filtering stage specifying global request and response filters. This stage class
     * is reused for both pre and post match filtering phases.
     * <p>
     * All global response filters are passed in the pre-match stage, since if a pre-match filter aborts,
     * response filters should still be executed. For the post-match filter stage creation, {@code null} is passed
     * to the responseFilters parameter.
     * </p>
     *
     * @param requestFilters  list of global (unbound) request filters (either pre or post match - depending on the
     *                        stage being created).
     * @param responseFilters list of global response filters (for pre-match stage) or {@code null} (for post-match
     *                        stage).
     */
    ContainerFilteringStage(
            Iterable<RankedProvider<ContainerRequestFilter>> requestFilters,
            Iterable<RankedProvider<ContainerResponseFilter>> responseFilters) {

        this.requestFilters = requestFilters;
        this.responseFilters = responseFilters;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Continuation<RequestProcessingContext> apply(RequestProcessingContext context) {
        Iterable<ContainerRequestFilter> sortedRequestFilters;
        final boolean postMatching = responseFilters == null;

        final ContainerRequest request = context.request();

        final TracingLogger tracingLogger = TracingLogger.getInstance(request);
        if (postMatching) {
            // post-matching
            final ArrayList<Iterable<RankedProvider<ContainerRequestFilter>>> rankedProviders =
                    new ArrayList<>(2);
            rankedProviders.add(requestFilters);
            rankedProviders.add(request.getRequestFilters());
            sortedRequestFilters = Providers.mergeAndSortRankedProviders(
                    new RankedComparator<ContainerRequestFilter>(), rankedProviders);

            context.monitoringEventBuilder().setContainerRequestFilters(sortedRequestFilters);
            context.triggerEvent(RequestEvent.Type.REQUEST_MATCHED);

        } else {
            // pre-matching (response filter stage is pushed in pre-matching phase, so that if pre-matching filter
            // throws exception, response filters get still invoked)
            context.push(new ResponseFilterStage(context, responseFilters, tracingLogger));
            sortedRequestFilters = Providers.sortRankedProviders(new RankedComparator<ContainerRequestFilter>(), requestFilters);
        }

        final TracingLogger.Event summaryEvent =
                (postMatching ? ServerTraceEvent.REQUEST_FILTER_SUMMARY : ServerTraceEvent.PRE_MATCH_SUMMARY);
        final long timestamp = tracingLogger.timestamp(summaryEvent);
        int processedCount = 0;
        try {
            final TracingLogger.Event filterEvent = (postMatching ? ServerTraceEvent.REQUEST_FILTER : ServerTraceEvent.PRE_MATCH);
            for (ContainerRequestFilter filter : sortedRequestFilters) {
                final long filterTimestamp = tracingLogger.timestamp(filterEvent);
                try {
                    filter.filter(request);
                } catch (Exception exception) {
                    throw new MappableException(exception);
                } finally {
                    processedCount++;
                    tracingLogger.logDuration(filterEvent, filterTimestamp, filter);
                }

                final Response abortResponse = request.getAbortResponse();
                if (abortResponse != null) {
                    // abort accepting & return response
                    return Continuation.of(context, Stages.asStage(
                            new Endpoint() {
                                @Override
                                public ContainerResponse apply(
                                        final RequestProcessingContext requestContext) {
                                    return new ContainerResponse(requestContext.request(), abortResponse);
                                }
                            }));
                }
            }
        } finally {
            if (postMatching) {
                context.triggerEvent(RequestEvent.Type.REQUEST_FILTERED);
            }
            tracingLogger.logDuration(summaryEvent, timestamp, processedCount);
        }

        return Continuation.of(context, getDefaultNext());
    }

    private static class ResponseFilterStage extends AbstractChainableStage<ContainerResponse> {
        // TODO remove the field - processing context should be made available on the response chain directly.
        private final RequestProcessingContext processingContext;
        private final Iterable<RankedProvider<ContainerResponseFilter>> filters;
        private final TracingLogger tracingLogger;

        private ResponseFilterStage(final RequestProcessingContext processingContext,
                                    final Iterable<RankedProvider<ContainerResponseFilter>> filters,
                                    final TracingLogger tracingLogger) {
            this.processingContext = processingContext;
            this.filters = filters;
            this.tracingLogger = tracingLogger;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Continuation<ContainerResponse> apply(ContainerResponse responseContext) {
            final ArrayList<Iterable<RankedProvider<ContainerResponseFilter>>> rankedProviders = new ArrayList<>(2);
            rankedProviders.add(filters);
            rankedProviders.add(responseContext.getRequestContext().getResponseFilters());
            Iterable<ContainerResponseFilter> sortedResponseFilters = Providers.mergeAndSortRankedProviders(
                    new RankedComparator<ContainerResponseFilter>(RankedComparator.Order.DESCENDING), rankedProviders);

            final ContainerRequest request = responseContext.getRequestContext();
            processingContext.monitoringEventBuilder().setContainerResponseFilters(sortedResponseFilters);
            processingContext.triggerEvent(RequestEvent.Type.RESP_FILTERS_START);

            final long timestamp = tracingLogger.timestamp(ServerTraceEvent.RESPONSE_FILTER_SUMMARY);
            int processedCount = 0;
            try {
                for (ContainerResponseFilter filter : sortedResponseFilters) {
                    final long filterTimestamp = tracingLogger.timestamp(ServerTraceEvent.RESPONSE_FILTER);
                    try {
                        filter.filter(request, responseContext);
                    } catch (Exception ex) {
                        throw new MappableException(ex);
                    } finally {
                        processedCount++;
                        tracingLogger.logDuration(ServerTraceEvent.RESPONSE_FILTER, filterTimestamp, filter);
                    }
                }
            } finally {
                processingContext.triggerEvent(RequestEvent.Type.RESP_FILTERS_FINISHED);
                tracingLogger.logDuration(ServerTraceEvent.RESPONSE_FILTER_SUMMARY, timestamp, processedCount);
            }

            return Continuation.of(responseContext, getDefaultNext());
        }
    }
}
