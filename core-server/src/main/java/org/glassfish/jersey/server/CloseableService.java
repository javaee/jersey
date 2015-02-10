/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2015 Oracle and/or its affiliates. All rights reserved.
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

import java.io.Closeable;

/**
 * A closeable service to add instances of {@link Closeable} that
 * are required to be closed.
 * <p>
 * This interface may be injected onto server-side components using
 * the {@link javax.ws.rs.core.Context} annotation.
 * <p>
 * The service may be used within the scope of a request to add instances
 * of {@link Closeable} that are to be closed when the request goes out
 * of scope, more specifically after the request has been processed and the
 * response has been returned.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @author Paul Sandoz
 */
public interface CloseableService {

    /**
     * Register a new instance of {@link Closeable} that is to be closed when the request goes out of scope.
     * <p>
     * After {@link #close()} has been called, this method will not accept any new instance registrations and
     * will return {@code false} instead.
     * </p>
     *
     * @param c the instance of {@link Closeable}.
     * @return {@code true} if the closeable service has not been closed yet and the closeable instance was successfully
     * registered with the service, {@code false} otherwise.
     */
    public boolean add(Closeable c);

    /**
     * Invokes {@code Closeable#close()} method on all instances of {@link Closeable} added by the {@code #add(Closeable)}
     * method.
     * Subsequent calls of this method should not do anything.
     */
    public void close();
}
