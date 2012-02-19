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

import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.internal.LocalizationMessages;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.process.internal.RequestScope.Snapshot;

import org.glassfish.hk2.ComponentException;
import org.glassfish.hk2.DynamicBinderFactory;
import org.glassfish.hk2.Factory;
import org.glassfish.hk2.Services;

import org.jvnet.hk2.annotations.Inject;

import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.Monitor;

/**
 * Suspendable {@link Request} to {@link Response} inflector adapter that provides
 * implementation of the request suspend/resume capabilities of the
 * {@link RequestInvoker.InvocationContext invocation context} and returns
 * a {@link ListenableFuture listenable response future} instead of a plain response
 * object.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
class SuspendableInflectorAdapter extends AbstractFuture<Response>
        implements Inflector<Request, ListenableFuture<Response>>, InvocationContext {

    private static final Logger LOGGER = Logger.getLogger(SuspendableInflectorAdapter.class.getName());

    public static class Builder {

        @Inject
        private Services services;

        public Builder() {
            // Injection constructor
        }

        public Builder(Services requestScope) {
            this.services = requestScope;
        }

        public SuspendableInflectorAdapter build(Inflector<Request, Response> wrapped) {
            return new SuspendableInflectorAdapter(wrapped, services);
        }
    }
    private final BlockingDeque<Snapshot> requestScopeSnapshots = new LinkedBlockingDeque<Snapshot>();
    //
    private State status = State.RUNNING;
    private final Monitor statusMonitor = new Monitor();
    private final Monitor.Guard statusRunningOrSuspended = new Monitor.Guard(statusMonitor) {

        @Override
        public boolean isSatisfied() {
            return status == State.RUNNING || status == State.SUSPENDED;
        }
    };
    private final Monitor.Guard statusRunning = new Monitor.Guard(statusMonitor) {

        @Override
        public boolean isSatisfied() {
            return status == State.RUNNING;
        }
    };
    //
    private AtomicReference<Response> defaultResponse = new AtomicReference<Response>();
    //
    private final Inflector<Request, Response> wrapped;
    private final Services services;

    public SuspendableInflectorAdapter(Inflector<Request, Response> wrapped, Services services) {
        this.wrapped = wrapped;
        this.services = services;
    }

    @Override
    public ListenableFuture<Response> apply(Request request) {
        try {
            final DynamicBinderFactory dynamicBindings = services.bindDynamically();
            dynamicBindings.bind(InvocationContext.class).toFactory(new Factory<InvocationContext>() {

                @Override
                public InvocationContext get() throws ComponentException {
                    return SuspendableInflectorAdapter.this;
                }
            }).in(RequestScope.class);
            dynamicBindings.commit();

            Response response = wrapped.apply(request);
            if (statusMonitor.enterIf(statusRunning)) {
                // resume synchronously
                try {
                    status = State.RESUMED;
                    set(response);
                } finally {
                    statusMonitor.leave();
                }
            } else {
                // Ignore the returned response
            }
        } catch (Exception ex) {
            resume(ex); // resume with exception (if not resumed already by an external event)
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
        statusMonitor.enter();
        try {
            return status;
        } finally {
            statusMonitor.leave();
        }
    }

    @Override
    public void resume(final Response response) {
        if (statusMonitor.enterIf(statusRunningOrSuspended)) {
            try {
                status = State.RESUMED;
            } finally {
                statusMonitor.leave();
            }
            set(response);
        } else {
            throw new IllegalStateException(LocalizationMessages.ILLEGAL_INVOCATION_CONTEXT_STATE(status, "resume"));
        }
    }

    @Override
    public void resume(final Throwable response) {
        if (statusMonitor.enterIf(statusRunningOrSuspended)) {
            try {
                status = State.RESUMED;
            } finally {
                statusMonitor.leave();
            }
            setException(response);
        } else {
            throw new IllegalStateException(LocalizationMessages.ILLEGAL_INVOCATION_CONTEXT_STATE(status, "resume"));
        }
    }

    @Override
    public Future<?> suspend() {
        return suspend(0L, TimeUnit.MILLISECONDS);
    }

    @Override
    public Future<?> suspend(long millis) {
        return suspend(millis, TimeUnit.MILLISECONDS);
    }

    @Override
    public Future<?> suspend(long time, TimeUnit unit) {
        if (statusMonitor.enterIf(statusRunningOrSuspended)) {
            try {
                status = State.SUSPENDED;
                // TODO invoke callback
            } finally {
                statusMonitor.leave();
            }
        } else {
            LOGGER.log(Level.FINE, "Failed to suspend request invocation context in state \"{0}\"", status);
        }

        return this;
    }

    @Override
    public void cancel() {
        // TODO implement request processing cancellation.
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected void interruptTask() {
        // TODO implement cancellation logic here
    }

    @Override
    public void setResponse(Response response) {
        defaultResponse.set(response);
    }

    @Override
    public Response getResponse() {
        return defaultResponse.get();
    }
}
