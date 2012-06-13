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
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.server.spi.ContainerResponseWriter;

/**
 * Used for sending messages in "typed" chunks.
 *
 * Useful for long running processes, which needs to produce partial responses.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 *
 * TODO:  something like prequel/sequel - usable for EventChannelWriter and XML related writers
 */
public class ChunkedResponse<T> implements Closeable {
    private final BlockingDeque<T> queue = new LinkedBlockingDeque<T>();
    private Class<T> clazz;

    private boolean closed = false;
    private MessageBodyWorkers messageBodyWorkers = null;

    private Annotation[] annotations;
    private MediaType mediaType;
    private MultivaluedMap<String, Object> httpHeaders;
    private Map<String, Object> responseProperties;

    private OutputStream outputStream;
    private ContainerResponseWriter containerResponseWriter;

    /**
     * Create {@link ChunkedResponse} with specified type.
     *
     * @param clazz chunk type
     */
    public ChunkedResponse(Class<T> clazz) {
        this.clazz = clazz;
    }

    /**
     * Used when you need to create descendant with specific chunk type.
     *
     * <p>When used, implementation is required to call {@link ChunkedResponse#setClazz(Class)} in its constructor.</p>
     *
     */
    protected ChunkedResponse() {
    }

    /**
     * Set chunk type.
     *
     * @param clazz
     */
    protected void setClazz(Class<T> clazz) {
        this.clazz = clazz;
    }

    /**
     * Write a chunk.
     *
     * @param chunk a chunk instance to be written.
     * @throws IllegalStateException when {@link ChunkedResponse} is closed.
     * @throws IOException when encountered any problem during serializing or writing a chunk.
     */
    public void write(T chunk) throws IOException {
        if(closed) {
            throw new IllegalStateException();
        }

        if(chunk != null) {
            queue.add(chunk);
        }

        if(messageBodyWorkers != null) {
            flushQueue();
        }
    }

    private synchronized void flushQueue() throws IOException {
        if(outputStream == null || messageBodyWorkers == null) {
            return;
        }

        T t;
        while((t = queue.poll()) != null) {
            messageBodyWorkers.writeTo(t, GenericType.<Object>of(clazz, clazz), annotations, mediaType, httpHeaders, responseProperties, outputStream, null, true);
        }

        if(closed) {
            outputStream.flush();
            outputStream.close();
            containerResponseWriter.commit();
        }
    }

    /**
     * Close this response - it will be finalized and underlying connections will be closed
     * or made available for another response.
     *
     */
    public void close() throws IOException {
        closed = true;
        if(messageBodyWorkers != null) {
            flushQueue();
        }
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
     * Set arguments used for {@link javax.ws.rs.ext.MessageBodyWriter} selection and for writing chunks.
     *
     * @param outputStream used for writing chunks.
     * @param containerResponseWriter container needs to be notified when {@link ChunkedResponse} is closed to release/close
     *                                connection and internal structures.
     * @param messageBodyWorkers used for writing chunks.
     * @param annotations used for writing chunks.
     * @param mediaType used for writing chunks.
     * @param httpHeaders used for writing chunks.
     * @param responseProperties used for writing chunks.
     * @throws IOException when encountered any problem during serializing or writing a chunk.
     */
    void setWriterRelatedArgs(OutputStream outputStream,
                              ContainerResponseWriter containerResponseWriter,
                              MessageBodyWorkers messageBodyWorkers,
                              Annotation[] annotations,
                              MediaType mediaType,
                              MultivaluedMap<String, Object> httpHeaders,
                              Map<String, Object> responseProperties) throws IOException {
        this.outputStream = outputStream;
        this.containerResponseWriter = containerResponseWriter;
        this.messageBodyWorkers = messageBodyWorkers;
        this.annotations = annotations;
        this.mediaType = mediaType;
        this.httpHeaders = httpHeaders;
        this.responseProperties = responseProperties;

        flushQueue();
    }
}
