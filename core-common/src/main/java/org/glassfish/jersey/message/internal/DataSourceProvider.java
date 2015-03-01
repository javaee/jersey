/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.message.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import javax.activation.DataSource;

/**
 * Provider for marshalling/un-marshalling of {@code application/octet-stream}
 * entity type to/from a {@link DataSource} instance.
 *
 * @author Paul Sandoz
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
@Produces({"application/octet-stream", "*/*"})
@Consumes({"application/octet-stream", "*/*"})
public class DataSourceProvider extends AbstractMessageReaderWriterProvider<DataSource> {

    /**
     * Modified from javax.mail.util.ByteArrayDataSource
     *
     * A DataSource backed by a byte array.  The byte array may be
     * passed in directly, or may be initialized from an InputStream
     * or a String.
     *
     * @since JavaMail 1.4
     * @author John Mani
     * @author Bill Shannon
     * @author Max Spivak
     */
    public static class ByteArrayDataSource implements DataSource {

        private final String type;

        private byte[] data;
        private int len = -1;
        private String name = "";

        static class DSByteArrayOutputStream extends ByteArrayOutputStream {

            public byte[] getBuf() {
                return buf;
            }

            public int getCount() {
                return count;
            }
        }

        public ByteArrayDataSource(final InputStream is, final String type) throws IOException {
            final DSByteArrayOutputStream os = new DSByteArrayOutputStream();
            ReaderWriter.writeTo(is, os);
            this.data = os.getBuf();
            this.len = os.getCount();

            /*
             * ByteArrayOutputStream doubles the size of the buffer every time
             * it needs to expand, which can waste a lot of memory in the worst
             * case with large buffers.  Check how much is wasted here and if
             * it's too much, copy the data into a new buffer and allow the
             * old buffer to be garbage collected.
             */
            if (this.data.length - this.len > 256 * 1024) {
                this.data = os.toByteArray();
                this.len = this.data.length;    // should be the same
            }
            this.type = type;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            if (data == null) {
                throw new IOException("no data");
            }
            if (len < 0) {
                len = data.length;
            }
            return new ByteArrayInputStream(data, 0, len);
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            throw new IOException("cannot do this");
        }

        @Override
        public String getContentType() {
            return type;
        }

        @Override
        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }

    @Override
    public boolean isReadable(final Class<?> type,
                              final Type genericType,
                              final Annotation[] annotations,
                              final MediaType mediaType) {
        return DataSource.class == type;
    }

    @Override
    public DataSource readFrom(
            final Class<DataSource> type,
            final Type genericType,
            final Annotation[] annotations,
            final MediaType mediaType,
            final MultivaluedMap<String, String> httpHeaders,
            final InputStream entityStream) throws IOException {
        return new ByteArrayDataSource(entityStream, (mediaType == null) ? null : mediaType.toString());
    }

    @Override
    public boolean isWriteable(final Class<?> type,
                               final Type genericType,
                               final Annotation[] annotations,
                               final MediaType mediaType) {
        return DataSource.class.isAssignableFrom(type);
    }

    @Override
    public void writeTo(
            final DataSource t,
            final Class<?> type,
            final Type genericType,
            final Annotation[] annotations,
            final MediaType mediaType,
            final MultivaluedMap<String, Object> httpHeaders,
            final OutputStream entityStream) throws IOException {
        final InputStream in = t.getInputStream();
        try {
            writeTo(in, entityStream);
        } finally {
            in.close();
        }
    }
}
