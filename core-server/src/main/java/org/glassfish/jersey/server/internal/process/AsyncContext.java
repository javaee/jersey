/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2014 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.server.internal.process;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.internal.util.Producer;

/**
 * Injectable asynchronous processing context that can be used to control various aspects
 * of asynchronous processing of a single request.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public interface AsyncContext extends AsyncResponse {

    /**
     * Asynchronous processing context state.
     */
    public static enum State {

        /**
         * Indicates the asynchronous processing context is running. This is a default state
         * the processing context is in case the processing execution flow has not been explicitly
         * modified (yet).
         */
        RUNNING,
        /**
         * Indicates the asynchronous processing context has been suspended.
         *
         * @see AsyncContext#suspend()
         */
        SUSPENDED,
        /**
         * Indicates the asynchronous processing context has been resumed.
         */
        RESUMED,
        /**
         * Indicates the processing represented by this asynchronous processing context
         * has been completed.
         */
        COMPLETED,
    }

    /**
     * Suspend the current asynchronous processing context.
     *
     * The method returns {@code true} if the context has been successfully suspended,
     * {@code false} otherwise.
     *
     * @return {@code true} if the request processing has been suspended successfully suspended,
     *         {@code false} otherwise.
     */
    public boolean suspend();

    /**
     * Invoke the provided response producer in a Jersey-managed asynchronous thread.
     *
     * @param producer response producer.
     */
    public void invokeManaged(Producer<Response> producer);
}
