/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2016 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.simple;

import java.io.Closeable;

/**
 * Simple server facade providing convenient methods to obtain info about the server (i.e. port).
 *
 * @author Michal Gajdos
 * @since 2.9
 */
public interface SimpleServer extends Closeable {

    /**
     * The port the server is listening to for incomming HTTP connections. If the port is not
     * specified the {@link org.glassfish.jersey.server.spi.Container.DEFAULT_PORT} is used.
     *
     * @return the port the server is listening on
     */
    public int getPort();

    /**
     * If this is true then very low level I/O operations are logged. Typically this is used to debug
     * I/O issues such as HTTPS handshakes or performance issues by analysing the various latencies
     * involved in the HTTP conversation.
     * <p/>
     * There is a minimal performance penalty if this is enabled and it is perfectly suited to being
     * enabled in a production environment, at the cost of logging overhead.
     *
     * @return {@code true} if debug is enabled, false otherwise.
     * @since 2.23
     */
    public boolean isDebug();

    /**
     * To enable very low level logging this can be enabled. This goes far beyond logging issues such
     * as connection establishment of request dispatch, it can trace the TCP operations latencies
     * involved.
     *
     * @param enable if {@code true} debug tracing will be enabled.
     * @since 2.23
     */
    public void setDebug(boolean enable);
}
