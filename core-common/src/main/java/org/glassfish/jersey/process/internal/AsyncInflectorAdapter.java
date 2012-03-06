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

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.Suspend;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.internal.LocalizationMessages;
import org.glassfish.jersey.internal.util.LazyUid;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.process.internal.RequestScope.Snapshot;

import com.google.common.base.Objects;
import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.Monitor;

/**
 * Suspendable, asynchronous {@link Request} to {@link Response} inflector adapter
 * that provides implementation of the request suspend/resume capabilities of the
 * {@link RequestInvoker.InvocationContext invocation context} and returns
 * a {@link ListenableFuture listenable response future} instead of a plain response
 * object.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
class AsyncInflectorAdapter extends AbstractFuture<Response>
        implements Inflector<Request, ListenableFuture<Response>>, InvocationContext {

    private static final Logger LOGGER = Logger.getLogger(AsyncInflectorAdapter.class.getName());
    /*
     * Invocation context instance UUID.
     *
     * For performance reasons, it's only generated if toString() method is invoked,
     * e.g. as part of some low-level logging.
     */
    private final LazyUid id = new LazyUid();
    private final BlockingDeque<Snapshot> requestScopeSnapshots = new LinkedBlockingDeque<Snapshot>();
    // state variable synced via monitor => doesn't have to be volatile
    private State executionState = State.RUNNING;
    private final Monitor executionStateMonitor = new Monitor();
    private final Monitor.Guard resumableState = new Monitor.Guard(executionStateMonitor) {

        @Override
        public boolean isSatisfied() {
            return executionState == State.RUNNING || executionState == State.SUSPENDED;
        }
    };
    private final Monitor.Guard runningState = new Monitor.Guard(executionStateMonitor) {

        @Override
        public boolean isSatisfied() {
            return executionState == State.RUNNING;
        }
    };
    private final Monitor.Guard cancellableState = new Monitor.Guard(executionStateMonitor) {

        @Override
        public boolean isSatisfied() {
            return executionState == State.RUNNING || executionState == State.SUSPENDED;
        }
    };
    //
    private long defaultTimeout = Suspend.NEVER;
    private TimeUnit defaultTimeoutUnit = TimeUnit.MILLISECONDS;
    private AtomicReference<Response> defaultResponse = new AtomicReference<Response>();
    //
    private final Inflector<Request, Response> wrapped;
    private final InvocationCallback callback;

    AsyncInflectorAdapter(final Inflector<Request, Response> wrapped, final InvocationCallback callback) {
        this.wrapped = wrapped;
        this.callback = callback;
    }

    @Override
    public ListenableFuture<Response> apply(Request request) {
        try {
            Response response = wrapped.apply(request);
            if (executionStateMonitor.enterIf(runningState)) {
                // mark as resumed & don't invoke callback.resume() since we are resuming synchronously
                try {
                    executionState = State.RESUMED;
                    set(response);
                } finally {
                    executionStateMonitor.leave();
                }
            } else if (response != null) {
                LOGGER.log(Level.FINE, LocalizationMessages.REQUEST_SUSPENDED_RESPONSE_IGNORED(response));
            }
        } catch (Exception ex) {
            resume(ex); // resume with exception (if not resumed by an external event already)
        }
        return this;
    }

    @Override
    public void pushRequestScope(Snapshot snapshot) {
        requestScopeSnapshots.push(snapshot);
    }

    @Override
    public Snapshot popRequestScope() {
        try {
            return requestScopeSnapshots.take();
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public State state() {
        executionStateMonitor.enter();
        try {
            return executionState;
        } finally {
            executionStateMonitor.leave();
        }
    }

    @Override
    public void resume(final Object response) {
        if (executionStateMonitor.enterIf(resumableState)) {
            try {
                executionState = State.RESUMED;
            } finally {
                executionStateMonitor.leave();
            }
            set(toJaxrsResponse(response));
        } else {
            throw new IllegalStateException(LocalizationMessages.ILLEGAL_INVOCATION_CONTEXT_STATE(executionState, "resume"));
        }
    }

    @Override
    public void resume(final Exception response) {
        if (executionStateMonitor.enterIf(resumableState)) {
            try {
                executionState = State.RESUMED;
            } finally {
                executionStateMonitor.leave();
            }
            setException(response);
        } else {
            throw new IllegalStateException(LocalizationMessages.ILLEGAL_INVOCATION_CONTEXT_STATE(executionState, "resume"));
        }
    }

    @Override
    public void setSuspendTimeout(long time, TimeUnit unit) {
        this.defaultTimeout = time;
        this.defaultTimeoutUnit = unit;
    }

    @Override
    public boolean trySuspend() {
        return _suspend(defaultTimeout, defaultTimeoutUnit, false);
    }

    @Override
    public Future<?> suspend() {
        _suspend(defaultTimeout, defaultTimeoutUnit, true);
        return this;
    }

    @Override
    public Future<?> suspend(long millis) {
        _suspend(millis, TimeUnit.MILLISECONDS, true);
        return this;
    }

    @Override
    public Future<?> suspend(long time, TimeUnit unit) {
        _suspend(time, unit, true);
        return this;
    }

    private boolean _suspend(long time, TimeUnit unit, boolean failOnError) throws IllegalStateException {
        boolean suspendSuccessful = false;

        try {
            executionStateMonitor.enter();
            switch (executionState) {
                case RESUMED:
                    break;
                case SUSPENDED:
                case CANCELLED:
                    if (failOnError) {
                        throw new IllegalStateException(
                                LocalizationMessages.ILLEGAL_INVOCATION_CONTEXT_STATE(executionState, "suspend"));
                    }
                    break;
                case RUNNING:
                    executionState = State.SUSPENDED;
                    suspendSuccessful = true;
            }
        } finally {
            executionStateMonitor.leave();
        }

        // we don't want to invoke the callback or log message
        // as part of the synchronized code
        if (suspendSuccessful) {
            callback.suspended(time, unit, this);
        } else {
            // already resumed - just log fine message & ignore the call
            LOGGER.log(Level.FINE, LocalizationMessages.REQUEST_SUSPEND_FAILED(executionState));
        }

        return suspendSuccessful;
    }

    @Override
    public void cancel() {
        if (executionStateMonitor.enterIf(cancellableState)) {
            try {
                executionState = State.CANCELLED;
            } finally {
                executionStateMonitor.leave();
            }
            super.cancel(true);
        } else {
            // just log fine message & ignore the call
            LOGGER.log(Level.FINE, LocalizationMessages.REQUEST_CANCEL_FAILED(executionState));
        }
    }

    @Override
    protected void interruptTask() {
        callback.cancelled();
    }

    @Override
    public void setResponse(Object response) {
        defaultResponse.set(toJaxrsResponse(response));
    }

    @Override
    public Response getResponse() {
        return defaultResponse.get();
    }

    private Response toJaxrsResponse(final Object response) {
        if (response instanceof Response) {
            return (Response) response;
        } else {
            return Response.ok(response).build();
        }
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this).add("id", id.value()).toString();
    }
}
