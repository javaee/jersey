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

import java.util.concurrent.TimeUnit;
import javax.ws.rs.core.Response;

/**
 * Request transformation callback used by a {@link RequestInvoker request invoker}
 * to provide asynchronous notifications about the request processing.
 * <p/>
 * The callback is invoked when the request transformation is suspended or completed
 * either successfully or by a failure.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public interface InvocationCallback {

    /**
     * Request processing suspend event notification.
     *
     * {@link RequestInvoker Request invoker} invokes the {@code suspended(...)}
     * callback method when it receives an instruction to suspend the current
     * request processing via one of the {@link InvocationContext invocation
     * context} {@code suspend(...)} methods.
     *
     * @param time suspend time-out value.
     * @param unit suspend time-out time unit.
     * @param context suspended invocation context.
     */
    public void suspended(final long time, final TimeUnit unit, final InvocationContext context);

    /**
     * Request processing suspend timeout change event notification.
     *
     * {@link RequestInvoker Request invoker} invokes the
     * {@code suspendTimeoutChanged(...)} callback method when it receives an
     * instruction to update the timeout value for the current (suspended)
     * request.
     *
     * @param time new suspend time-out value.
     * @param unit new suspend time-out time unit.
     * @param context suspended invocation context.
     */
    public void suspendTimeoutChanged(final long time, final TimeUnit unit);

    /**
     * Request processing resume event notification.
     *
     * {@link RequestInvoker Request invoker} invokes the {@code resumed()}
     * callback method when it receives an instruction to resume the current
     * <b>suspended</b> request processing via one of the {@link InvocationContext
     * invocation context} {@code resume(...)} methods.
     * <p />
     * Note that the {@code resumed()} callback method will not be invoked in
     * case the invocation context has not been in a suspended state when the
     * {@code resume(...)} method was invoked.
     */
    public void resumed();

    /**
     * Request processing cancel event notification.
     *
     * {@link RequestInvoker Request invoker} invokes the {@code cancelled()}
     * callback method when it receives an instruction to cancel the current
     * request processing via invocation context {@link InvocationContext#cancel()
     * cancel()} method.
     */
    public void cancelled();

    /**
     * A successful request-to-response transformation event notification.
     *
     * @param response transformation result.
     */
    public void result(final Response response);

    /**
     * A request-to-response transformation failure event notification.
     *
     * @param exception exception describing the failure.
     */
    public void failure(final Throwable exception);
}