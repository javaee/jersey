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

import java.util.concurrent.TimeUnit;

import org.glassfish.jersey.process.internal.ProcessingCallback;
import org.glassfish.jersey.process.internal.ProcessingContext;
import org.glassfish.jersey.server.spi.ContainerResponseWriter;

/**
 * Container response writer delegating invocation callback.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
abstract class ContainerResponseWriterCallback implements ProcessingCallback<ContainerResponse> {

    private boolean suspended;
    private boolean autosuspend;
    private boolean done;
    private boolean timeoutCancelled;
    private final Object stateUpdateLock;
    private ProcessingContext processingContext;

    /**
     * Request data.
     */
    protected final ContainerRequest requestContext;

    /**
     * Construct a new container response writer delegating invocation callback
     * for a given request context.
     *
     * @param requestContext request context.
     */
    public ContainerResponseWriterCallback(ContainerRequest requestContext) {
        this.suspended = false;
        this.autosuspend = false;
        this.timeoutCancelled = false;
        this.stateUpdateLock = new Object();

        this.requestContext = requestContext;
    }

    @Override
    public void result(ContainerResponse response) {
        synchronized (stateUpdateLock) {
            if (done) {
                return;
            }
            done = timeoutCancelled = true;
        }
        try {
            writeResponse(response);
        } finally {
            release();
        }
    }

    @Override
    public void failure(Throwable exception) {
        synchronized (stateUpdateLock) {
            if (done) {
                return;
            }
            done = timeoutCancelled = true;
        }
        try {
            writeResponse(exception);
        } finally {
            release();
        }
    }

    @Override
    public void cancelled() {
        synchronized (stateUpdateLock) {
            if (done) {
                return;
            }
            done = timeoutCancelled = true;
        }
        try {
            requestContext.getResponseWriter().cancel();
        } finally {
            release();
        }
    }

    @Override
    public void suspended(final long time, final TimeUnit unit, final ProcessingContext context) {
        synchronized (stateUpdateLock) {
            processingContext = context;
            if (autosuspend) {
                suspendTimeoutChanged(time, unit);
            } else {
                suspendWriter(time, unit);
            }
        }
    }

    /**
     * Suspend the container response writer.
     *
     * This method is always executed from within a synchronized block.
     *
     * @param time suspend timeout value.
     * @param unit suspend timeout time unit.
     */
    private void suspendWriter(final long time, final TimeUnit unit) {
        requestContext.getResponseWriter().suspend(time, unit, new ContainerResponseWriter.TimeoutHandler() {

            @Override
            public void onTimeout(ContainerResponseWriter responseWriter) {
                synchronized (stateUpdateLock) {
                    if (timeoutCancelled || done) {
                        return;
                    }
                    done = true;
                }
                writeTimeoutResponse(processingContext);
            }
        });
        suspended = true;
    }

    @Override
    public void suspendTimeoutChanged(long time, TimeUnit unit) {
        requestContext.getResponseWriter().setSuspendTimeout(time, unit);
    }

    @Override
    public void resumed() {
        synchronized (stateUpdateLock) {
            timeoutCancelled = true;
        }
    }

    /**
     * Suspend the underlying container response writer indefinitely if it has not
     * yet been committed or suspended.
     *
     * Calling this method prevents the container from automatically committing
     * the response writer once the control is returned from the application.
     * This is necessary to avoid race conditions in certain asynchronous
     * request processing scenarios (e.g. request or response processing is already
     * running on a different thread, but has not finished yet).
     */
    void suspendWriterIfRunning() {
        synchronized (stateUpdateLock) {
            if (done || suspended) {
                return;
            }
            autosuspend = true;
            suspendWriter(0, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Write the response result.
     *
     * @param response response data.
     */
    protected abstract void writeResponse(ContainerResponse response);

    /**
     * Write the failure response.
     *
     * @param exception failure.
     */
    protected abstract void writeResponse(Throwable exception);

    /**
     * Write the timeout response.
     *
     * @param context invocation context.
     */
    protected abstract void writeTimeoutResponse(ProcessingContext context);

    /**
     * Release all resources as the request processing is truly over now.
     */
    protected abstract void release();
}
