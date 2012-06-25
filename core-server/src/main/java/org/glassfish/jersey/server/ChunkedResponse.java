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
package org.glassfish.jersey.server;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

import javax.ws.rs.core.GenericType;

/**
 * Used for sending messages in "typed" chunks. Useful for long running processes,
 * which needs to produce partial responses.
 *
 * @param <T> chunk type.
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
// TODO:  something like prequel/sequel - usable for EventChannelWriter and XML related writers
public class ChunkedResponse<T> implements Closeable {
    /**
     * A request-scoped property indicating that the a chunked response
     * is used by the user.
     */
    static final String CHUNKED_MODE = "jersey.config.server.chunked-mode";

    private final BlockingDeque<T> queue = new LinkedBlockingDeque<T>();
    private final GenericType<T> chunkType;

    private boolean closed = false;
    private ContainerRequest requestContext;
    private ContainerResponse responseContext;

    /**
     * Create {@link ChunkedResponse} with specified type.
     *
     * @param chunkType chunk type
     */
    public ChunkedResponse(Type chunkType) {
        this.chunkType = new GenericType<T>(chunkType);
    }

    /**
     * Creates a new instance of ChunkResponse, passing the chunk type as an instance of {@link GenericType}.
     *
     * @param genericType chunk type
     */
    public ChunkedResponse(GenericType<T> genericType) {
        this.chunkType = genericType;
    }

    /**
     * Write a chunk.
     *
     * @param chunk a chunk instance to be written.
     * @throws IllegalStateException when {@link ChunkedResponse} is closed.
     * @throws IOException           when encountered any problem during serializing or writing a chunk.
     */
    public void write(T chunk) throws IOException {
        if (closed) {
            throw new IllegalStateException();
        }

        if (chunk != null) {
            queue.add(chunk);
        }

        flushQueue();
    }

    private synchronized void flushQueue() throws IOException {
        if (requestContext == null) {
            return;
        }

        T t;
        while ((t = queue.poll()) != null) {
            requestContext.getWorkers().writeTo(
                    t,
                    t.getClass(),
                    chunkType.getType(),
                    responseContext.getEntityAnnotations(),
                    responseContext.getMediaType(),
                    responseContext.getHeaders(),
                    requestContext.getPropertiesDelegate(),
                    responseContext.getEntityStream(),
                    null,
                    true);
        }

        if (closed) {
            responseContext.getEntityStream().flush();
            responseContext.getEntityStream().close();
            requestContext.getResponseWriter().commit();
        }
    }

    /**
     * Close this response - it will be finalized and underlying connections will be closed
     * or made available for another response.
     */
    @Override
    public void close() throws IOException {
        closed = true;
        flushQueue();
    }

    /**
     * Get state information.
     *
     * Please note that {@link ChunkedResponse} can be closed by the client side - client can close connection
     * from its side.
     *
     * @return true when closed, false otherwise.
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * Set context used for writing chunks.
     *
     * @param requestContext request context.
     * @param responseContext response context.
     * @throws IOException when encountered any problem during serializing or writing a chunk.
     */
    void setContext(ContainerRequest requestContext,
                    ContainerResponse responseContext) throws IOException {
        this.requestContext = requestContext;
        this.responseContext = responseContext;
        flushQueue();
    }
}
