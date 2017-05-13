/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.jdk.connector;

import java.io.OutputStream;

/**
 * TODO consider exposing the mode as part of the API, so the user can make decisions based on the mode
 * <p/>
 * An extension of {@link OutputStream} that adds method that allow to use the stream asynchronously.
 * It is inspired by and works in a very similar way as Servlet asynchronous streams introduced in Servlet 3.1.
 * <p/>
 * The stream supports 2 modes SYNCHRONOUS and ASYNCHRONOUS.
 * The stream is one of the following 3 states:
 * <ul>
 * <li>UNDECIDED</li>
 * <li>SYNCHRONOUS</li>
 * <li>ASYNCHRONOUS</li>
 * </ul>
 * UNDECIDED is an initial mode and it commits either to SYNCHRONOUS or ASYNCHRONOUS. Once it commits to one of these
 * 2 modes it cannot change to the other. The mode it commits to is decided based on the first use of the stream.
 * If {@link #setWriteListener(WriteListener)} is invoked before any of the write methods, it commits to ASYNCHRONOUS
 * mode and similarly if any of the write methods is invoked before {@link #setWriteListener(WriteListener)},
 * it commits to SYNCHRONOUS mode.
 */
abstract class BodyOutputStream extends OutputStream {

    /**
     * Instructs the stream to invoke the provided {@link WriteListener} when it is possible to write.
     * <p/>
     * If the stream is in UNDECIDED state, invoking this method will commit the stream to ASYNCHRONOUS mode.
     *
     * @param writeListener the {@link WriteListener} that should be notified
     *                      when it's possible to write.
     * @throws IllegalStateException if one of the following conditions is true
     *                               <ul>
     *                               <li>the stream has already committed to SYNCHRONOUS mode. <li/>
     *                               <li>setWriteListener is called more than once within the scope of the same request. <li/>
     *                               </ul>
     * @throws NullPointerException  if writeListener is null
     */
    public abstract void setWriteListener(WriteListener writeListener);

    /**
     * Returns true if data can be written without blocking else returns
     * false.
     * <p/>
     * If the stream is in ASYNCHRONOUS mode and the user attempts to write to it even though this method returns
     * false, an {@link IllegalStateException} is thrown.
     *
     * @return <code>true</code> if data can be obtained without blocking,
     * otherwise returns <code>false</code>.
     */
    public abstract boolean isReady();
}
