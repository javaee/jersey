/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.MultivaluedMap;

import javax.servlet.http.HttpServletResponse;

import org.glassfish.jersey.server.ContainerException;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.internal.JerseyRequestTimeoutHandler;
import org.glassfish.jersey.server.spi.ContainerResponseWriter;
import org.glassfish.jersey.servlet.spi.AsyncContextDelegate;

import com.google.common.util.concurrent.SettableFuture;

/**
 * An internal implementation of {@link ContainerResponseWriter} for Servlet containers.
 * The writer depends on provided {@link AsyncContextDelegate} to support async functionality.
 *
 * @author Paul Sandoz (paul.sandoz at oracle.com)
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 * @author Martin Matula (martin.matula at oracle.com)
 */
public class ResponseWriter implements ContainerResponseWriter {

    private static final Logger LOGGER = Logger.getLogger(ResponseWriter.class.getName());

    private final HttpServletResponse response;
    private final boolean useSetStatusOn404;
    private final SettableFuture<ContainerResponse> responseContext;
    private final AsyncContextDelegate asyncExt;

    private final JerseyRequestTimeoutHandler requestTimeoutHandler;

    /**
     * Creates a new instance to write a single Jersey response.
     *
     * @param useSetStatusOn404 true if status should be written explicitly when 404 is returned
     * @param response          original HttpResponseRequest
     * @param asyncExt          delegate to use for async features implementation
     */
    public ResponseWriter(final boolean useSetStatusOn404, final HttpServletResponse response, AsyncContextDelegate asyncExt) {
        this.useSetStatusOn404 = useSetStatusOn404;
        this.response = response;
        this.asyncExt = asyncExt;
        this.responseContext = SettableFuture.create();

        this.requestTimeoutHandler = new JerseyRequestTimeoutHandler(this);
    }

    @Override
    public boolean suspend(final long timeOut, final TimeUnit timeUnit, final TimeoutHandler timeoutHandler) {
        try {
            // Suspend the servlet.
            asyncExt.suspend();

            // Suspend the internal request timeout handler.
            return requestTimeoutHandler.suspend(timeOut, timeUnit, timeoutHandler);
        } catch (IllegalStateException ex) {
            return false;
        }
    }

    @Override
    public void setSuspendTimeout(long timeOut, TimeUnit timeUnit) throws IllegalStateException {
        requestTimeoutHandler.setSuspendTimeout(timeOut, timeUnit);
    }

    @Override
    public OutputStream writeResponseStatusAndHeaders(long contentLength, ContainerResponse responseContext)
            throws ContainerException {
        this.responseContext.set(responseContext);

        // first set the content length, so that if headers have an explicit value, it takes precedence over this one
        if (responseContext.hasEntity() && contentLength != -1 && contentLength < Integer.MAX_VALUE) {
            response.setContentLength((int) contentLength);
        }
        // Note that the writing of headers MUST be performed before
        // the invocation of sendError as on some Servlet implementations
        // modification of the response headers will have no effect
        // after the invocation of sendError.
        MultivaluedMap<String, String> headers = getResponseContext().getStringHeaders();
        for (Map.Entry<String, List<String>> e : headers.entrySet()) {
            final Iterator<String> it = e.getValue().iterator();
            if (!it.hasNext()) {
                continue;
            }
            final String header = e.getKey();
            if (response.containsHeader(header)) {
                // replace any headers previously set with values from Jersey container response.
                response.setHeader(header, it.next());
            }

            while (it.hasNext()) {
                response.addHeader(header, it.next());
            }
        }
        response.setStatus(responseContext.getStatus());

        if (!responseContext.hasEntity()) {
            return null;
        } else {
            try {
                return response.getOutputStream();
            } catch (IOException e) {
                throw new ContainerException(e);
            }
        }
    }

    @Override
    public void commit() {
        try {
            if (!response.isCommitted()) {
                final ContainerResponse responseContext = getResponseContext();
                final int status = responseContext.getStatus();
                if (status >= 400 && !(useSetStatusOn404 && status == 404)) {
                    final String reason = responseContext.getStatusInfo().getReasonPhrase();
                    try {
                        if (reason == null || reason.isEmpty()) {
                            response.sendError(status);
                        } else {
                            response.sendError(status, reason);
                        }
                    } catch (IOException ex) {
                        throw new ContainerException("I/O exception occurred while sending [" + status + "] error response.", ex);
                    }
                }
            }
        } finally {
            asyncExt.complete();
        }
    }

    @Override
    public void failure(Throwable error) {
        try {
            if (!response.isCommitted()) {
                try {
                    response.reset();
                    response.sendError(500, "Request failed.");
                } catch (IllegalStateException ex) {
                    // a race condition externally committing the response can still occur...
                    LOGGER.log(Level.FINER, "Unable to reset failed response.", ex);
                } catch (IOException ex) {
                    throw new ContainerException("I/O exception occurred while sending 'Request failed.' error response.", ex);
                } finally {
                    asyncExt.complete();
                }
            }
        } finally {
            rethrow(error);
        }
    }

    @Override
    public boolean enableResponseBuffering() {
        return true;
    }

    /**
     * Rethrow the original exception as required by JAX-RS, 3.3.4
     *
     * @param error throwable to be re-thrown
     */
    private void rethrow(Throwable error) {
        if (error instanceof RuntimeException) {
            throw (RuntimeException) error;
        } else {
            throw new ContainerException(error);
        }
    }

    /**
     * Provides response status captured when {@link #writeResponseStatusAndHeaders(long, org.glassfish.jersey.server.ContainerResponse)} has been invoked.
     * The method will block if the write method has not been called yet.
     *
     * @return response status
     */
    public int getResponseStatus() {
        return getResponseContext().getStatus();
    }

    private ContainerResponse getResponseContext() {
        try {
            return responseContext.get();
        } catch (InterruptedException ex) {
            throw new ContainerException(ex);
        } catch (ExecutionException ex) {
            throw new ContainerException(ex);
        }
    }
}
