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
import javax.ws.rs.core.Response;

/**
 * Injectable invocation context that can be used to control various aspects
 * of the invocation, e.g. the threading model.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public interface InvocationContext {

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
     * Resume the previously suspended request invocation with a response.
     *
     * See {@link javax.ws.rs.core.ExecutionContext#resume(java.lang.Object)} for
     * more details about the expected behavior.
     *
     * @param response response to be used in the resumed invocation processing.
     * @throws IllegalStateException in case the invocation context
     *     has not been suspended yet or has already been resumed.
     *
     * @see javax.ws.rs.core.ExecutionContext#resume(Object)
     */
    public void resume(Response response);

    /**
     * Resume the previously suspended request invocation with an exception.
     *
     * See {@link javax.ws.rs.core.ExecutionContext#resume(java.lang.Exception)}
     * for more details about the expected behavior.
     *
     * @param exception exception to be used in the resumed invocation processing.
     * @throws IllegalStateException in case the invocation context
     *     has not been suspended yet or has already been resumed.
     *
     * @see javax.ws.rs.core.ExecutionContext#resume(Exception)
     */
    public void resume(Throwable exception);

    /**
     * Cancel the request invocation.
     *
     * See {@link javax.ws.rs.core.ExecutionContext#cancel()} for more details
     * about the expected behavior.
     */
    public void cancel();

    /**
     * Suspend a request invocation. The method is re-entrant, IOW calling
     * the method multiple times has the same effect as calling it only once.
     *
     * In case the invocation has been {@link State#RESUMED resumed} or
     * {@link State#CANCELLED canceled} already, the call to suspend is ignored.
     * <p />
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
     */
    public Future<?> suspend();

    /**
     * Suspend a request invocation for up to the specified time in milliseconds.
     * <p/>
     * If called on an already suspended invocation, the existing timeout value
     * is overridden by a new value and the suspension timeout counter is reset.
     * This means that the suspended invocation will time out in:
     * <pre>
     *     System.currentTimeMillis() + timeInMillis
     * </pre>
     * .
     * In case the invocation has been {@link State#RESUMED resumed} or
     * {@link State#CANCELLED canceled} already, the call to suspend is ignored.
     * <p />
     * See {@link javax.ws.rs.core.ExecutionContext#suspend(long)} for more details
     * on the expected behavior.
     *
     * @param timeInMillis suspension timeout in milliseconds.
     * @return {@link Future future} representing a handle of the suspended
     *    request invocation that can be used for querying its current state
     *    via one of the {@code Future.isXxx()} methods. The handle can also
     *    be used to {@link Future#cancel(boolean) cancel} the invocation
     *    altogether.
     *
     * @see javax.ws.rs.core.ExecutionContext#suspend(long)
     */
    public Future<?> suspend(long timeInMillis);

    /**
     * Suspend a request invocation for up to the specified time.
     * <p/>
     * If called on an already suspended invocation, the existing timeout value
     * is overridden by a new value and the suspension timeout counter is reset.
     * This means that the suspended invocation will time out in:
     * <pre>
     *     System.currentTimeMillis() + unit.toMillis(time)
     * </pre>
     * .
     * In case the invocation has been {@link State#RESUMED resumed} or
     * {@link State#CANCELLED canceled} already, the call to suspend is ignored.
     * <p />
     * See {@link javax.ws.rs.core.ExecutionContext#suspend(long, java.util.concurrent.TimeUnit)}
     * for more details about the expected behavior.
     *
     * @param time suspension timeout value.
     * @param unit suspension timeout time unit.
     * @return {@link Future future} representing a handle of the suspended
     *    request invocation that can be used for querying its current state
     *    via one of the {@code Future.isXxx()} methods. The handle can also
     *    be used to {@link Future#cancel(boolean) cancel} the invocation
     *    altogether.
     *
     * @see javax.ws.rs.core.ExecutionContext#suspend(long, TimeUnit)
     */
    public Future<?> suspend(long time, TimeUnit unit);

    /**
     * Set the default response to be used in case the suspended request invocation
     * times out.
     *
     * See {@link javax.ws.rs.core.ExecutionContext#setResponse(java.lang.Object)}
     * for more details about the expected behavior.
     *
     * @param response data to be sent back to the client in case the suspended
     *     request invocation times out.
     *
     * @see javax.ws.rs.core.ExecutionContext#setResponse(Object)
     */
    public void setResponse(Response response);

    /**
     * Returns default response to be send back to the client in case the suspended
     * request invocation times out. The method may return {@code null} if no default
     * response was set in the invocation context.
     * <p />
     * See {@link javax.ws.rs.core.ExecutionContext#getResponse()} for more details
     * about the expected behavior.
     *
     * @return default response to be sent back to the client in case the suspended
     *     request invocation times out or {@code null} if no default response
     *     was set.
     *
     * @see javax.ws.rs.core.ExecutionContext#getResponse()
     */
    public Response getResponse();
}