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

package org.glassfish.jersey.servlet.internal;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.glassfish.jersey._remove.Helper;
import org.glassfish.jersey.message.internal.CommittingOutputStream;
import org.glassfish.jersey.message.internal.OutboundMessageContext;
import org.glassfish.jersey.server.ContainerException;
import org.glassfish.jersey.server.spi.ContainerResponseWriter;
import org.glassfish.jersey.servlet.spi.AsyncContextDelegate;

import com.google.common.util.concurrent.SettableFuture;

/**
 * An internal implementation of {@link ContainerResponseWriter} for Servlet containers.
 * The writer depends on provided {@link AsyncContextDelegate} to support async functionality.
 *
 * @author Paul Sandoz (paul.sandoz at oracle.com)
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public class ResponseWriter implements ContainerResponseWriter {

    private static final Logger LOGGER = Logger.getLogger(ResponseWriter.class.getName());

    // TODO remove?
    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final CommittingOutputStream out;
    private final boolean useSetStatusOn404;
    private final SettableFuture<Response> jerseyResponse;
    private long contentLength;
    private final AtomicBoolean statusAndHeadersWritten;
    private final AsyncContextDelegate asyncExt;

    /**
     * Creates a new instance to write a single Jersey response.
     *
     * @param useSetStatusOn404 true if status should be written explicitly when 404 is returned
     * @param request           original HttpServletRequest
     * @param response          original HttpResponseRequest
     * @param asyncExt          delegate to use for async features implementation
     */
    public ResponseWriter(final boolean useSetStatusOn404, final HttpServletRequest request, final HttpServletResponse response, AsyncContextDelegate asyncExt) {
        this.useSetStatusOn404 = useSetStatusOn404;
        this.request = request;
        this.response = response;
        this.out = new CommittingOutputStream();
        this.out.setStreamProvider(new OutboundMessageContext.StreamProvider() {

            @Override
            public void commit() throws IOException {
                ResponseWriter.this.writeStatusAndHeaders();
            }

            @Override
            public OutputStream getOutputStream() throws IOException {
                return ResponseWriter.this.response.getOutputStream();
            }
        });
        this.statusAndHeadersWritten = new AtomicBoolean(false);
        this.asyncExt = asyncExt;
        this.jerseyResponse = SettableFuture.create();
    }

    @Override
    public void suspend(final long timeOut, final TimeUnit timeUnit, final TimeoutHandler timeoutHandler) throws IllegalStateException {
        asyncExt.suspend(this, timeOut, timeUnit, timeoutHandler);
    }

    @Override
    public void setSuspendTimeout(long timeOut, TimeUnit timeUnit) throws IllegalStateException {
        asyncExt.setSuspendTimeout(timeOut, timeUnit);
    }

    @Override
    public OutputStream writeResponseStatusAndHeaders(long contentLength, Response jerseyResponse) throws ContainerException {
        this.contentLength = contentLength;
        this.jerseyResponse.set(jerseyResponse);
        this.statusAndHeadersWritten.set(false);
        return out;
    }

    @Override
    public void commit() {
        if (!statusAndHeadersWritten.compareAndSet(false, true)) {
            return;
        }
        try {
            // Note that the writing of headers MUST be performed before
            // the invocation of sendError as on some Servlet implementations
            // modification of the response headers will have no effect
            // after the invocation of sendError.
            writeHeaders();
            final Response actualJerseyResponse = getActualJerseyResponse();
            final int status = actualJerseyResponse.getStatus();
            if (status >= 400) {
                if (useSetStatusOn404 && status == 404) {
                    response.setStatus(status);
                } else {
                    final String reason = actualJerseyResponse.getStatusInfo().getReasonPhrase();
                    try {
                        if (reason == null || reason.isEmpty()) {
                            response.sendError(status);
                        } else {
                            response.sendError(status, reason);
                        }
                    } catch (IOException ex) {
                        throw new ContainerException("I/O exception occured while sending [" + status + "] error response.", ex);
                    }
                }
            } else {
                response.setStatus(status);
            }
        } finally {
            asyncExt.complete();
        }
    }

    @Override
    public void cancel() {
        if (!response.isCommitted()) {
            try {
                response.reset();
            } catch (IllegalStateException ex) {
                // a race condition externally commiting the response can still occur...
                LOGGER.log(Level.FINER, "Unable to reset cancelled response.", ex);
            } finally {
                asyncExt.complete();
            }
        }
    }

    private void writeStatusAndHeaders() {
        if (!statusAndHeadersWritten.compareAndSet(false, true)) {
            return;
        }
        writeHeaders();
        response.setStatus(getActualJerseyResponse().getStatus());
    }

    private void writeHeaders() {
        if (contentLength != -1 && contentLength < Integer.MAX_VALUE) {
            response.setContentLength((int) contentLength);
        }
        MultivaluedMap<String, String> headers = Helper.unwrap(getActualJerseyResponse()).getHeaders();
        for (Map.Entry<String, List<String>> e : headers.entrySet()) {
            for (String v : e.getValue()) {
                response.addHeader(e.getKey(), v);
            }
        }
    }

    /**
     * Provides response status captured when {@link #writeResponseStatusAndHeaders(long, javax.ws.rs.core.Response)} has been invoked.
     * The method will block if the write method has not been called yet.
     *
     * @return response status
     */
    public int getResponseStatus() {
        return getActualJerseyResponse().getStatus();
    }

    private Response getActualJerseyResponse() {
        try {
            return jerseyResponse.get();
        } catch (InterruptedException ex) {
            throw new ContainerException(ex);
        } catch (ExecutionException ex) {
            throw new ContainerException(ex);
        }
    }
}
