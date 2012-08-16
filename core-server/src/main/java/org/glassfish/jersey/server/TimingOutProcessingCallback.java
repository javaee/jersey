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

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.Response;

import org.glassfish.jersey.process.internal.ProcessingCallback;
import org.glassfish.jersey.process.internal.ProcessingContext;

import com.google.common.util.concurrent.AbstractFuture;

/**
 * Callback implementation with a simple timeout support.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
abstract class TimingOutProcessingCallback extends AbstractFuture<ContainerResponse>
        implements ProcessingCallback<ContainerResponse> {

    private static final Logger logger = Logger.getLogger(TimingOutProcessingCallback.class.getName());
    private static final Timer TIMER = new Timer("Jersey application request timer");
    private final Object suspendLock = new Object();
    private final AtomicBoolean done = new AtomicBoolean(false);
    private ProcessingContext processingCtx = null;
    private TimerTask timeoutTask = null;

    /**
     * Construct a new invocation callback with a time-out support for a given request context.
     */
    protected TimingOutProcessingCallback() {
    }

    @Override
    public void result(ContainerResponse response) {
        if (done.compareAndSet(false, true)) {
            try {
                set(handleResponse(response));
            } catch (Throwable t) {
                setException(t);
            } finally {
                release();
            }
        }
    }

    @Override
    public void failure(final Throwable failure) {
        if (done.compareAndSet(false, true)) {
            try {
                set(handleFailure(failure));
            } catch (Throwable t) {
                setException(t);
            } finally {
                release();
            }
        }
    }

    /**
     * Convert the exception into a {@link Response}.
     *
     * @param exception to be converted.
     * @return failure response context.
     */
    protected abstract ContainerResponse handleFailure(final Throwable exception);

    @Override
    public void cancelled() {
        if (done.compareAndSet(false, true)) {
            try {
                cancel(true);
            } finally {
                release();
            }
        }
    }

    @Override
    public void suspended(final long time, final TimeUnit unit, final ProcessingContext context) {
        final TimerTask task = new TimerTask() {

            @Override
            public void run() {
                if (done.compareAndSet(false, true)) {
                    try {
                        set(handleTimeout(processingCtx));
                    } catch (Throwable t) {
                        setException(t);
                    } finally {
                        release();
                    }
                }
            }
        };
        synchronized (suspendLock) {
            if (processingCtx != null) {
                throw new IllegalStateException("Already suspended");
            }
            processingCtx = context;
            if (time <= 0) {
                return; // never time out
            }

            timeoutTask = task;
        }
        try {
            TIMER.schedule(task, unit.toMillis(time));
        } catch (IllegalStateException ex) {
            logger.log(Level.WARNING, "Error while scheduling a timeout task.", ex);
        }
    }

    /**
     * Provide a timeout {@link ContainerResponse}.
     *
     * @param context invocation context that has timed out.
     * @return timeout response context.
     */
    protected abstract ContainerResponse handleTimeout(final ProcessingContext context);

    @Override
    public void suspendTimeoutChanged(final long time, final TimeUnit unit) {
        if (!done.get()) {
            final TimerTask task = new TimerTask() {

                @Override
                public void run() {
                    if (done.compareAndSet(false, true)) {
                        try {
                            set(handleTimeout(processingCtx));
                        } catch (Throwable t) {
                            setException(t);
                        }
                    }
                }
            };
            synchronized (suspendLock) {
                if (timeoutTask != null) {
                    timeoutTask.cancel();
                }
                timeoutTask = task;
            }
            try {
                TIMER.schedule(task, unit.toMillis(time));
            } catch (IllegalStateException ex) {
                logger.log(Level.WARNING, "Error while scheduling a timeout task.", ex);
            }
        }
    }

    @Override
    public void resumed() {
        synchronized (suspendLock) {
            if (timeoutTask != null) {
                timeoutTask.cancel();
            }
        }
    }

    /**
     * Modify returned {@link ContainerResponse response context}.
     *
     * @param response original response context.
     * @return modified response context.
     */
    protected abstract ContainerResponse handleResponse(final ContainerResponse response);

    /**
     * Release all resources as the request processing is truly over now.
     */
    protected abstract void release();

}
