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

package org.glassfish.jersey.client;

import java.io.IOException;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.client.ResponseProcessingException;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.internal.inject.Providers;
import org.glassfish.jersey.model.internal.RankedComparator;
import org.glassfish.jersey.process.internal.AbstractChainableStage;
import org.glassfish.jersey.process.internal.ChainableStage;

import org.glassfish.hk2.api.ServiceLocator;

/**
 * Client filtering stage factory.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
class ClientFilteringStages {

    private ClientFilteringStages() {
        // Prevents instantiation
    }

    /**
     * Create client request filtering stage using the service locator. May return {@code null}.
     *
     * @param locator HK2 service locator to be used.
     * @return configured request filtering stage, or {@code null} in case there are no
     *         {@link ClientRequestFilter client request filters} registered in the service
     *         locator.
     */
    static ChainableStage<ClientRequest> createRequestFilteringStage(final ServiceLocator locator) {
        final RankedComparator<ClientRequestFilter> comparator = new RankedComparator<ClientRequestFilter>(
                RankedComparator.Order.ASCENDING);
        final Iterable<ClientRequestFilter> requestFilters = Providers
                .getAllProviders(locator, ClientRequestFilter.class, comparator);

        return requestFilters.iterator().hasNext() ? new RequestFilteringStage(requestFilters) : null;
    }

    /**
     * Create client response filtering stage using the service locator. May return {@code null}.
     *
     * @param locator HK2 service locator to be used.
     * @return configured response filtering stage, or {@code null} in case there are no
     *         {@link ClientResponseFilter client response filters} registered in the service
     *         locator.
     */
    static ChainableStage<ClientResponse> createResponseFilteringStage(final ServiceLocator locator) {
        final RankedComparator<ClientResponseFilter> comparator = new RankedComparator<ClientResponseFilter>(
                RankedComparator.Order.DESCENDING);
        final Iterable<ClientResponseFilter> responseFilters = Providers
                .getAllProviders(locator, ClientResponseFilter.class, comparator);

        return responseFilters.iterator().hasNext() ? new ResponseFilterStage(responseFilters) : null;
    }

    private static final class RequestFilteringStage extends AbstractChainableStage<ClientRequest> {

        private final Iterable<ClientRequestFilter> requestFilters;

        private RequestFilteringStage(final Iterable<ClientRequestFilter> requestFilters) {
            this.requestFilters = requestFilters;
        }

        @Override
        public Continuation<ClientRequest> apply(ClientRequest requestContext) {
            for (ClientRequestFilter filter : requestFilters) {
                try {
                    filter.filter(requestContext);
                    final Response abortResponse = requestContext.getAbortResponse();
                    if (abortResponse != null) {
                        throw new AbortException(new ClientResponse(requestContext, abortResponse));
                    }
                } catch (IOException ex) {
                    throw new ProcessingException(ex);
                }
            }
            return Continuation.of(requestContext, getDefaultNext());
        }
    }

    private static class ResponseFilterStage extends AbstractChainableStage<ClientResponse> {

        private final Iterable<ClientResponseFilter> filters;

        private ResponseFilterStage(Iterable<ClientResponseFilter> filters) {
            this.filters = filters;
        }

        @Override
        public Continuation<ClientResponse> apply(ClientResponse responseContext) {
            try {
                for (ClientResponseFilter filter : filters) {
                    filter.filter(responseContext.getRequestContext(), responseContext);
                }
            } catch (IOException ex) {
                InboundJaxrsResponse response = new InboundJaxrsResponse(responseContext, null);
                throw new ResponseProcessingException(response, ex);
            }

            return Continuation.of(responseContext, getDefaultNext());
        }
    }
}
