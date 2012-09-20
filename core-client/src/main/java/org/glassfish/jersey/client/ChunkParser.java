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
package org.glassfish.jersey.client;

import java.io.IOException;
import java.io.InputStream;

/**
 * Chunk data parser.
 *
 * Implementations of this interface are used by a {@link org.glassfish.jersey.client.ChunkedInput}
 * instance for parsing response entity input stream into chunks.
 * <p>
 * Chunk parsers are expected to read data from the response entity input stream
 * until a non-empty data chunk is fully read and then return the chunk data back
 * to the {@link org.glassfish.jersey.client.ChunkedInput} instance for further
 * processing (i.e. conversion into a specific Java type).
 * </p>
 * <p>
 * Chunk parsers are typically expected to skip any empty chunks (the chunks that do
 * not contain any data) or any control meta-data associated with chunks, however it
 * is not a hard requirement to do so. The decision depends on the knowledge of which
 * {@link javax.ws.rs.ext.MessageBodyReader} implementation is selected for de-serialization
 * of the chunk data.
 * </p>
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public interface ChunkParser {
    /**
     * Invoked by {@link org.glassfish.jersey.client.ChunkedInput} to get the data for
     * the next chunk.
     *
     * @param responseStream response entity input stream.
     * @return next chunk data represented as an array of bytes, or {@code null}
     *         if no more chunks are available.
     * @throws java.io.IOException in case reading from the response entity fails.
     */
    public byte[] readChunk(InputStream responseStream) throws IOException;
}
