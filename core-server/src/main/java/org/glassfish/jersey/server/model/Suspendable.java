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

package org.glassfish.jersey.server.model;

import java.util.concurrent.TimeUnit;

/**
 * Jersey model component that is suspendable and may hold suspend-related
 * information.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public interface Suspendable {

    /**
     * Check if the component is marked for suspending.
     *
     * @return {@code true} if the component is marked for suspending,
     *     {@code false} otherwise.
     */
    public boolean isSuspendDeclared();

    /**
     * Check if the component is marked to be executed asynchronously by using
     * an internal Jersey {@link java.util.concurrent.ExecutorService executor service}.
     *
     * @return {@code true} if the component is marked for managed asynchronous execution,
     *     {@code false} otherwise.
     */
    public boolean isManagedAsyncDeclared();

    /**
     * Get the suspend timeout value in the given {@link #getSuspendTimeoutUnit()
     * time unit}.
     *
     * @return suspend timeout value.
     */
    public long getSuspendTimeout();

    /**
     * Get the suspend {@link #getSuspendTimeout() timeout value} time unit.
     *
     * @return time unit of the suspend timeout value.
     */
    public TimeUnit getSuspendTimeoutUnit();
}
