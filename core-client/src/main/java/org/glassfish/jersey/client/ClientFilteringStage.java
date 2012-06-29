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
package org.glassfish.jersey.client;

import java.io.IOException;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.internal.inject.AbstractModule;
import org.glassfish.jersey.internal.inject.Providers;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.process.internal.AbstractChainableStage;
import org.glassfish.jersey.process.internal.PriorityComparator;
import org.glassfish.jersey.process.internal.ResponseProcessor;
import org.glassfish.jersey.process.internal.Stages;

import org.glassfish.hk2.Factory;
import org.glassfish.hk2.Services;

import org.jvnet.hk2.annotations.Inject;

/**
 * Client filtering stage responsible for execution of request and response filters
 * on each request-response message exchange.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
class ClientFilteringStage extends AbstractChainableStage<ClientRequest> {
    private final Factory<ResponseProcessor.RespondingContext<ClientResponse>> respondingContextFactory;
    private final Services services;

    /**
     * Injection constructor.
     *
     * @param services                 HK2 services.
     * @param respondingContextFactory responding context factory.
     */
    ClientFilteringStage(
            @Inject Factory<ResponseProcessor.RespondingContext<ClientResponse>> respondingContextFactory,
            @Inject Services services) {

        this.services = services;
        this.respondingContextFactory = respondingContextFactory;
    }


    @Override
    public Continuation<ClientRequest> apply(ClientRequest requestContext) {

        final List<ClientResponseFilter> responseFilters = Providers.getAllProviders(services, ClientResponseFilter.class,
                new PriorityComparator<ClientResponseFilter>(PriorityComparator.Order.DESCENDING));
        if (!responseFilters.isEmpty()) {
            respondingContextFactory.get().push(new ResponseFilterStage(responseFilters));
        }

        final List<ClientRequestFilter> requestFilters = Providers.getAllProviders(services, ClientRequestFilter.class,
                new PriorityComparator<ClientRequestFilter>(PriorityComparator.Order.ASCENDING));
        if (!requestFilters.isEmpty()) {
            for (ClientRequestFilter filter : requestFilters) {
                try {
                    filter.filter(requestContext);
                    final Response abortResponse = requestContext.getAbortResponse();
                    if (abortResponse != null) {
                        // abort accepting & return response
                        return Continuation.of(requestContext,
                                Stages.asStage(new Inflector<ClientRequest, ClientResponse>() {
                                    @Override
                                    public ClientResponse apply(final ClientRequest requestContext) {
                                        return new ClientResponse(requestContext, abortResponse);
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
        }
        return Continuation.of(requestContext, getDefaultNext());
    }

    private static class ResponseFilterStage extends AbstractChainableStage<ClientResponse> {
        private final List<ClientResponseFilter> filters;

        private ResponseFilterStage(List<ClientResponseFilter> filters) {
            this.filters = filters;
        }

        @Override
        public Continuation<ClientResponse> apply(ClientResponse responseContext) {
            try {
                for (ClientResponseFilter filter : filters) {
                    filter.filter(responseContext.getRequestContext(), responseContext);
                }
            } catch (IOException ex) {
                throw new WebApplicationException(ex);
            }

            return Continuation.of(responseContext, getDefaultNext());
        }
    }

    /**
     * Client filter processing injection binding module.
     */
    static class Module extends AbstractModule {

        @Override
        protected void configure() {
            bind().to(ClientFilteringStage.class);
        }
    }
}
