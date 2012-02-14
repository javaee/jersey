/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Containers implement this interface and provide an instance to the
 * {@link Application}.
 *
 * @author Paul Sandoz
 */
public interface ContainerResponseWriter {
    /**
     * Write the status and headers of the response and return an output stream
     * for the web application to write the entity of the response.
     *
     * @param contentLength >=0 if the content length in bytes of the
     *        entity to be written is known, otherwise -1. Containers
     *        may use this value to determine whether the "Content-Length"
     *        header can be set or utilize chunked transfer encoding.
     * @param response the container response. The status and headers are
     *        obtained from the response.
     * @return the output stream to write the entity (if any).
     * @throws java.io.IOException if an error occurred when writing out the
     *         status and headers or obtaining the output stream.
     */
    OutputStream writeStatusAndHeaders(
            long contentLength,
            Response response) throws IOException;

    /**
     * Finish writing the response. This enables the container response
     * writer to clean up any state or flush any streams.
     *
     * @throws java.io.IOException
     */
    void finish() throws IOException;
}