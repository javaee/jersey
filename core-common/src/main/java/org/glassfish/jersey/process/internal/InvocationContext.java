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

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.Suspend;
import javax.ws.rs.core.ExecutionContext;
import javax.ws.rs.core.Response;

/**
 * Injectable invocation context that can be used to control various aspects
 * of the invocation, e.g. the threading model.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public interface InvocationContext extends javax.ws.rs.core.ExecutionContext {

    /**
     * Invocation context state.
     */
    public static enum State {

        /**
         * Indicates the invocation context is running. This is a default state
         * the invocation context is in case the invocation execution flow
         * has not been explicitly modified (yet).
         */
        RUNNING,
        /**
         * Indicates the invocation running in the invocation context has been
         * canceled.
         */
        CANCELLED,
        /**
         * Indicates the invocation running in the invocation context has been
         * suspended.
         *
         * @see InvocationContext#suspend()
         * @see InvocationContext#suspend(long)
         * @see InvocationContext#suspend(long, TimeUnit)
         */
        SUSPENDED,
        /**
         * Indicates the invocation running in the invocation context has been
         * resumed.
         *
         * @see InvocationContext#resume(Response)
         * @see InvocationContext#resume(Throwable)
         */
        RESUMED
    }

    /**
     * Store a request scope snapshot in the internal stack.
     *
     * @param snapshot request scope snapshot to be stored.
     */
    public void pushRequestScope(RequestScope.Snapshot snapshot);

    /**
     * Retrieve a request scope snapshot stored in the internal stack.
     * <p/>
     * Note: the method blocks if no scope snapshot is stored and waits
     * until a snapshot is available.
     *
     * @return the most recently stored request scope snapshot.
     */
    public RequestScope.Snapshot popRequestScope();

    /**
     * Get the current state of the invocation context.
     *
     * @return current state of the invocation context
     */
    public State state();

    /**
     * Set the default time-out values to be used for {@link #suspend() suspending}
     * the invocation.
     *
     * This method can be used to propagate the time-out data from the JAX-RS
     * {@link javax.ws.rs.Suspend &#64;Suspend} annotation to the invocation context.
     * <p />
     * Note that the method is not thread safe.
     *
     * @param time suspension timeout value.
     * @param unit suspension timeout time unit.
     */
    public void setSuspendTimeout(long time, TimeUnit unit);

    /**
     * Try to {@link ExecutionContext#suspend() suspend} the request invocation.
     *
     * Unlike the {@code suspend()} method, this method does not throw an exception
     * in case the suspend operation fails. Instead, the method returns {@code true}
     * if the invocation has been suspended successfully, returns {@code false}
     * otherwise.
     *
     * @return {@code true} if the invocation has been suspended successfully,
     * returns {@code false} otherwise.
     */
    public boolean trySuspend();

    /**
     * Suspend a request invocation.
     *
     * See {@link javax.ws.rs.core.ExecutionContext#suspend()} for more details
     * on the expected behavior.
     *
     * @return {@link Future future} representing a handle of the suspended
     *    request invocation that can be used for querying its current state
     *    via one of the {@code Future.isXxx()} methods. The handle can also
     *    be used to {@link Future#cancel(boolean) cancel} the invocation
     *    altogether.
     *
     * @see javax.ws.rs.core.ExecutionContext#suspend()
     * @see #setSuspendTimeout(long, java.util.concurrent.TimeUnit)
     */
    @Override
    public Future<?> suspend();
}