/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.server.internal;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.container.AsyncResponse;

import org.glassfish.jersey.server.spi.ContainerResponseWriter;
import org.glassfish.jersey.server.spi.ContainerResponseWriter.TimeoutHandler;

/**
 * Common {@link ContainerResponseWriter#suspend(long, TimeUnit, ContainerResponseWriter.TimeoutHandler)}
 * and {@link ContainerResponseWriter#setSuspendTimeout(long, TimeUnit)} handler that can be used in
 * {@link ContainerResponseWriter} implementations instead of the underlying infrastructure.
 *
 * @author Michal Gajdos
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class JerseyRequestTimeoutHandler {

    private static final Logger LOGGER = Logger.getLogger(JerseyRequestTimeoutHandler.class.getName());

    private ScheduledFuture<?> timeoutTask = null; // guarded by runtimeLock
    private ContainerResponseWriter.TimeoutHandler timeoutHandler = null; // guarded by runtimeLock
    private boolean suspended = false; // guarded by runtimeLock
    private final Object runtimeLock = new Object();

    private final ContainerResponseWriter containerResponseWriter;
    private final ScheduledExecutorService executor;

    /**
     * Create request timeout handler for the giver {@link ContainerResponseWriter response writer}.
     *
     * @param containerResponseWriter response writer to create request timeout handler for.
     * @param timeoutTaskExecutor     Jersey runtime executor used for background execution of timeout
     *                                handling tasks.
     */
    public JerseyRequestTimeoutHandler(final ContainerResponseWriter containerResponseWriter,
                                       final ScheduledExecutorService timeoutTaskExecutor) {
        this.containerResponseWriter = containerResponseWriter;
        this.executor = timeoutTaskExecutor;
    }

    /**
     * Suspend the request/response processing.
     *
     * @param timeOut time-out value. Value less or equal to 0, indicates that
     *                the processing is suspended indefinitely.
     * @param unit    time-out time unit.
     * @param handler time-out handler to process a time-out event if it occurs.
     * @return {@code true} if the suspend operation completed successfully, {@code false} otherwise.
     * @see ContainerResponseWriter#suspend(long, TimeUnit, ContainerResponseWriter.TimeoutHandler)
     */
    public boolean suspend(final long timeOut, final TimeUnit unit, final TimeoutHandler handler) {
        synchronized (runtimeLock) {
            if (suspended) {
                return false;
            }

            suspended = true;
            timeoutHandler = handler;

            containerResponseWriter.setSuspendTimeout(timeOut, unit);
            return true;
        }
    }

    /**
     * Set the suspend timeout.
     *
     * @param timeOut time-out value. Value less or equal to 0, indicates that
     *                the processing is suspended indefinitely.
     * @param unit    time-out time unit.
     * @throws IllegalStateException in case the response writer has not been suspended yet.
     * @see ContainerResponseWriter#setSuspendTimeout(long, TimeUnit)
     */
    public void setSuspendTimeout(final long timeOut, final TimeUnit unit) throws IllegalStateException {
        synchronized (runtimeLock) {
            if (!suspended) {
                throw new IllegalStateException(LocalizationMessages.SUSPEND_NOT_SUSPENDED());
            }

            close(true);

            if (timeOut <= AsyncResponse.NO_TIMEOUT) {
                return;
            }

            try {
                timeoutTask = executor.schedule(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            synchronized (runtimeLock) {
                                timeoutHandler.onTimeout(containerResponseWriter);
                            }
                        } catch (final Throwable throwable) {
                            LOGGER.log(Level.WARNING, LocalizationMessages.SUSPEND_HANDLER_EXECUTION_FAILED(), throwable);
                        }
                    }
                }, timeOut, unit);
            } catch (final IllegalStateException ex) {
                LOGGER.log(Level.WARNING, LocalizationMessages.SUSPEND_SCHEDULING_ERROR(), ex);
            }
        }
    }

    /**
     * Cancel the suspended task.
     */
    public void close() {
        close(false);
    }

    private synchronized void close(final boolean interruptIfRunning) {
        if (timeoutTask != null) {
            timeoutTask.cancel(interruptIfRunning);
            timeoutTask = null;
        }
    }
}
