/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.process.internal;

import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.internal.util.collection.Pair;
import org.glassfish.jersey.internal.util.collection.Tuples;
import org.glassfish.jersey.process.Inflector;

import org.glassfish.hk2.Factory;

import org.jvnet.hk2.annotations.Inject;

import com.google.common.base.Optional;

/**
 * Filtering {@link ChainableAcceptor chainable acceptor} that runs
 * {@link RequestFilterProcessor request filter processor} on a request
 * and registers {@link ResponseFilterProcessor response filter processor}
 * to be run on a response.
 * <p>
 * The acceptor may break the chain by directly returning a response in case
 * any of the executed request filters sets a response in the filter context.
 * </p>
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 * @author Santiago Pericas-Geertsen (santiago.pericasgeertsen at oracle.com)
 */
public class FilteringAcceptor extends AbstractChainableAcceptor {

    private final RequestFilterProcessor requestFilterProcessor;
    private final ResponseFilterProcessor responseFilterProcessor;
    private final Factory<JerseyFilterContext> filterContextFactory;
    private final Factory<ResponseProcessor.RespondingContext> respondingContextFactory;

    /**
     * Create a new filtering acceptor.
     *
     * @param requestFilterProcessor   request filter processor to be executed on
     *                                 requests.
     * @param responseFilterProcessor  response filter processor to be executed on
     *                                 responses.
     * @param filterContextFactory     factory providing request-scoped filter contexts.
     * @param respondingContextFactory factory providing request-scoped responding
     *                                 contexts.
     */
    public FilteringAcceptor(@Inject RequestFilterProcessor requestFilterProcessor,
                             @Inject ResponseFilterProcessor responseFilterProcessor,
                             @Inject Factory<JerseyFilterContext> filterContextFactory,
                             @Inject Factory<ResponseProcessor.RespondingContext> respondingContextFactory) {
        this.requestFilterProcessor = requestFilterProcessor;
        this.responseFilterProcessor = responseFilterProcessor;
        this.filterContextFactory = filterContextFactory;
        this.respondingContextFactory = respondingContextFactory;
    }

    @Override
    public Pair<Request, Optional<LinearAcceptor>> apply(Request request) {
        JerseyFilterContext filterContext = filterContextFactory.get();

        respondingContextFactory.get().push(responseFilterProcessor);

        filterContext.setResponse(null);
        final Request filteredRequest = requestFilterProcessor.apply(request);
        final Response filterContextResponse = filterContext.getResponse();

        if (filterContextResponse == null) {
            // continue accepting
            return Tuples.of(filteredRequest, getDefaultNext());
        } else {
            // abort accepting & return response
            return Tuples.of(filteredRequest, Optional.of(Stages.asLinearAcceptor(new Inflector<Request, Response>() {
                @Override
                public Response apply(Request request) {
                    return filterContextResponse;
                }
            })));
        }
    }
}
