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
package org.glassfish.jersey.client;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.client.ClientException;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;

import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.message.MessageBodyWorkers.MessageBodySizeCallback;
import org.glassfish.jersey.message.internal.OutboundMessageContext;

/**
 * A request writer for writing header values and a request entity.
 *
 * @author Paul Sandoz
 */
public class RequestWriter {

    private static final Logger LOGGER = Logger.getLogger(RequestWriter.class.getName());

    /**
     * Create new request writer.
     */
    public RequestWriter() {
    }

    /**
     * A listener for listening to events when writing a request entity.
     */
    // TODO how to register/obtain the listener in a generic way?
    protected interface RequestEntityWriterListener extends MessageBodySizeCallback {

        /**
         * Called when the output stream is required to write the request entity.
         *
         * @return the output stream provider for writing the request entity.
         * @throws java.io.IOException in case of an error while opening the output stream.
         */
        OutboundMessageContext.StreamProvider onGetStreamProvider() throws IOException;
    }

    /**
     * A writer for writing a request entity.
     */
    // TODO how to register/obtain the writer in a generic way?
    protected interface RequestEntityWriter {

        /**
         * Get the size, in bytes, of the request entity, otherwise -1
         * if the size cannot be determined before serialization.
         *
         * @return size the size, in bytes, of the request entity, otherwise -1
         *         if the size cannot be determined before serialization.
         */
        long getSize();

        /**
         * Write the request entity.
         *
         * @param out the output stream to write the request entity.
         * @throws java.io.IOException in case of IO error.
         */
        void writeRequestEntity(OutputStream out) throws IOException;
    }

    /**
     * Default {@link RequestEntityWriter} implementation.
     */
    private final class DefaultRequestEntityWriter implements RequestEntityWriter {

        private final ClientRequest requestContext;
        private final long size;
        private final MessageBodyWriter writer;

        /**
         * Create new default request entity writer.
         *
         * @param requestContext Jersey client request context.
         */
        @SuppressWarnings("unchecked")
        public DefaultRequestEntityWriter(ClientRequest requestContext) {
            this.requestContext = requestContext;

            if (!requestContext.hasEntity()) {
                throw new IllegalArgumentException("The entity of the client request is null");
            }

            RequestWriter.this.ensureMediaType(requestContext);

            final MessageBodyWorkers workers = requestContext.getWorkers();
            final MediaType mediaType = requestContext.getMediaType();
            final Annotation[] entityAnnotations = requestContext.getEntityAnnotations();
            final Class<?> entityRawType = requestContext.getEntityClass();
            final Type entityType = requestContext.getEntityType();
            this.writer = workers.getMessageBodyWriter(
                    entityRawType,
                    entityType,
                    entityAnnotations,
                    mediaType);
            if (writer == null) {
                String message = "A message body writer for Java class " + entityRawType.getName()
                        + ", and Java type " + entityType
                        + ", and MIME media type " + mediaType + " was not found";
                LOGGER.severe(message);
                Map<MediaType, List<MessageBodyWriter>> m = workers.getWriters(mediaType);
                LOGGER.log(Level.SEVERE,
                        "The registered message body writers compatible with the MIME media type are:\n{0}",
                        workers.writersToString(m));

                throw new ClientException(message);
            }

            this.size = requestContext.getHeaders().containsKey(HttpHeaders.CONTENT_ENCODING)
                    ? -1
                    : writer.getSize(
                    requestContext.getEntity(),
                    entityRawType,
                    entityType,
                    entityAnnotations,
                    mediaType);
        }

        @Override
        public long getSize() {
            return size;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void writeRequestEntity(OutputStream out) throws IOException {
            // TODO interceptors?
            try {
                final GenericType<?> entityType = new GenericType(requestContext.getEntityType());
                writer.writeTo(
                        requestContext.getEntity(),
                        entityType.getRawType(),
                        entityType.getType(),
                        requestContext.getEntityAnnotations(),
                        requestContext.getMediaType(),
                        requestContext.getHeaders(),
                        out);
                out.flush();
            } finally {
                out.close();
            }
        }
    }

    /**
     * Get a request entity writer capable of writing the request entity.
     *
     * @param request the client request.
     * @return the request entity writer.
     */
    protected RequestEntityWriter getRequestEntityWriter(final ClientRequest request) {
        return new DefaultRequestEntityWriter(request);
    }

    /**
     * Write a request entity using an appropriate message body writer.
     * <p>
     * The method {@link RequestEntityWriterListener#onRequestEntitySize(long) } will be invoked
     * with the size of the request entity to be serialized.
     * The method {@link RequestEntityWriterListener#onGetStreamProvider } will be invoked
     * when the output stream is required to write the request entity.
     *
     * @param requestContext the client request context containing the request entity. If the
     *                       request entity is null then the method will not write any entity.
     * @param listener       the request entity listener.
     * @throws IOException in case of an IO error.
     */
    @SuppressWarnings("unchecked")
    protected void writeRequestEntity(ClientRequest requestContext, final RequestEntityWriterListener listener)
            throws IOException {

        ensureMediaType(requestContext);
        final MultivaluedMap<String, Object> headers = requestContext.getHeaders();

        MessageBodyWorkers.MessageBodySizeCallback sizeCallback = null;
        if (headers.containsKey(HttpHeaders.CONTENT_ENCODING)) {
            listener.onRequestEntitySize(-1);
        } else {
            sizeCallback = listener;
        }

        final MessageBodyWorkers workers = requestContext.getWorkers();
        requestContext.setStreamProvider(listener.onGetStreamProvider());
        OutputStream entityStream = null;
        try {
            entityStream = workers.writeTo(
                    requestContext.getEntity(),
                    requestContext.getEntity().getClass(),
                    requestContext.getEntityType(),
                    requestContext.getEntityAnnotations(),
                    requestContext.getMediaType(),
                    headers,
                    requestContext.getPropertiesDelegate(),
                    requestContext.getEntityStream(),
                    sizeCallback,
                    true);
            requestContext.setEntityStream(entityStream);
        } finally {
            if (entityStream != null) {
                try {
                    entityStream.close();
                } catch (IOException ex) {
                    LOGGER.log(Level.FINE, "Error closing output stream", ex);
                }
            }
        }
    }

    private void ensureMediaType(final ClientRequest requestContext) {
        if (requestContext.getMediaType() == null) {
            // Content-Type is not present choose a default type
            final GenericType<?> entityType = new GenericType(requestContext.getEntityType());
            final List<MediaType> mediaTypes = requestContext.getWorkers().getMessageBodyWriterMediaTypes(
                    entityType.getRawType(), entityType.getType(), requestContext.getEntityAnnotations());

            requestContext.setMediaType(getMediaType(mediaTypes));
        }
    }

    private MediaType getMediaType(List<MediaType> mediaTypes) {
        if (mediaTypes.isEmpty()) {
            return MediaType.APPLICATION_OCTET_STREAM_TYPE;
        } else {
            MediaType mediaType = mediaTypes.get(0);
            if (mediaType.isWildcardType() || mediaType.isWildcardSubtype()) {
                mediaType = MediaType.APPLICATION_OCTET_STREAM_TYPE;
            }
            return mediaType;
        }
    }
}
