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
package org.glassfish.jersey.process.internal;

import javax.ws.rs.core.Request;
import org.glassfish.jersey._remove.RequestBuilder;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.message.internal.Requests;
import org.glassfish.jersey.message.internal.Responses;

import org.glassfish.hk2.Factory;

import org.jvnet.hk2.annotations.Inject;

import com.google.common.base.Function;

/**
 * Function that can be put to an acceptor chain to properly initialize
 * {@link MessageBodyWorkers} instance on a current request and response.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class MessageBodyWorkersInitializer implements Function<Request, Request> {
    private final Factory<MessageBodyWorkers> workersFactory;
    private final Factory<ResponseProcessor.RespondingContext<Response>> respondingContextFactory;

    /**
     * Create new {@link MessageBodyWorkers} initialization function for requests
     * and responses.
     *
     * @param workersFactory {@code MessageBodyWorkers} factory.
     * @param respondingContextFactory {@link ResponseProcessor.RespondingContext} factory.
     */
    public MessageBodyWorkersInitializer(
            @Inject Factory<MessageBodyWorkers> workersFactory,
            @Inject Factory<ResponseProcessor.RespondingContext<Response>> respondingContextFactory) {
        this.workersFactory = workersFactory;
        this.respondingContextFactory = respondingContextFactory;
    }


    @Override
    public Request apply(Request request) {
        final RequestBuilder requestBuilder = Requests.toBuilder(request);
        final MessageBodyWorkers workers = workersFactory.get();
        Requests.setMessageWorkers(requestBuilder, workers);

        respondingContextFactory.get().push(new Function<Response, Response>() {
            @Override
            public Response apply(final Response response) {
                if (response != null) {
                    final Response.ResponseBuilder responseBuilder = Responses.toBuilder(response);
                    Responses.setMessageWorkers(responseBuilder, workers);
                    return responseBuilder.build();
                } else {
                    return null;
                }
            }
        });

        return requestBuilder.build();
    }
}
