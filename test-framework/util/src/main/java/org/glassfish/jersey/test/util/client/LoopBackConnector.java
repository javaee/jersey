/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.test.util.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientRequest;
import org.glassfish.jersey.client.ClientResponse;
import org.glassfish.jersey.client.spi.AsyncConnectorCallback;
import org.glassfish.jersey.client.spi.Connector;
import org.glassfish.jersey.message.internal.HeaderUtils;
import org.glassfish.jersey.message.internal.OutboundMessageContext;

/**
 * Loop-Back connector used for testing/benchmarking purposes. It returns a response that contains the same data (headers, entity)
 * as the processed request. The status of the response is {@code 600}.
 *
 * @author Michal Gajdos
 * @since 2.17
 */
final class LoopBackConnector implements Connector {

    /**
     * Test loop-back status code.
     */
    static final int TEST_LOOPBACK_CODE = 600;

    /**
     * Test loop-back status type.
     */
    static final Response.StatusType LOOPBACK_STATUS = new Response.StatusType() {
        @Override
        public int getStatusCode() {
            return TEST_LOOPBACK_CODE;
        }

        @Override
        public Response.Status.Family getFamily() {
            return Response.Status.Family.OTHER;
        }

        @Override
        public String getReasonPhrase() {
            return "Test connector loop-back";
        }
    };

    private volatile boolean closed = false;

    @Override
    public ClientResponse apply(final ClientRequest request) {
        return _apply(request);
    }

    @Override
    public Future<?> apply(final ClientRequest request, final AsyncConnectorCallback callback) {
        CompletableFuture<ClientResponse> future = new CompletableFuture<>();
        try {
            ClientResponse response = _apply(request);
            callback.response(response);
            future.complete(response);
        } catch (final Throwable t) {
            callback.failure(t);
            future.completeExceptionally(t);
        }
        return future;
    }

    private ClientResponse _apply(final ClientRequest request) {
        checkNotClosed();
        final ClientResponse response = new ClientResponse(LOOPBACK_STATUS, request);

        // Headers.
        response.headers(HeaderUtils.asStringHeaders(request.getHeaders()));

        // Entity.
        if (request.hasEntity()) {
            response.setEntityStream(new ByteArrayInputStream(bufferEntity(request)));
        }

        return response;
    }

    private byte[] bufferEntity(final ClientRequest requestContext) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream(512);

        requestContext.setStreamProvider(new OutboundMessageContext.StreamProvider() {

            @Override
            public OutputStream getOutputStream(final int contentLength) throws IOException {
                return baos;
            }
        });

        try {
            requestContext.writeEntity();
        } catch (final IOException ioe) {
            throw new ProcessingException("Error buffering the entity.", ioe);
        }

        return baos.toByteArray();
    }

    @Override
    public String getName() {
        return "test-loop-back-connector";
    }

    @Override
    public void close() {
        closed = true;
    }

    private void checkNotClosed() {
        if (closed) {
            throw new IllegalStateException("Loop-back Connector closed.");
        }
    }
}
