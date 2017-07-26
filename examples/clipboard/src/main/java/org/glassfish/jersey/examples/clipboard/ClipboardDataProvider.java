/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package org.glassfish.jersey.examples.clipboard;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.glassfish.jersey.message.MessageUtils;

/**
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public abstract class ClipboardDataProvider implements MessageBodyWriter, MessageBodyReader {

    @Provider
    @Consumes("text/plain")
    @Produces("text/plain")
    public static class TextPlain extends ClipboardDataProvider {

        @Override
        public void writeTo(final Object t, final Class type, final Type genericType, final Annotation[] annotations,
                            final MediaType mediaType, final MultivaluedMap httpHeaders, final OutputStream entityStream)
                throws IOException, WebApplicationException {
            entityStream.write(t.toString().getBytes(MessageUtils.getCharset(mediaType)));
        }

        @Override
        public Object readFrom(final Class type, final Type genericType, final Annotation[] annotations,
                               final MediaType mediaType, final MultivaluedMap httpHeaders, final InputStream entityStream)
                throws IOException, WebApplicationException {
            return new ClipboardData(readStringFromStream(entityStream, MessageUtils.getCharset(mediaType)));
        }
    }

    @Provider
    @Consumes("application/json")
    @Produces("application/json")
    public static class ApplicationJson extends ClipboardDataProvider {

        private static final String JsonOpenning = "{\"content\":\"";
        private static final String JsonClosing = "\"}";

        @Override
        public void writeTo(final Object t, final Class type, final Type genericType, final Annotation[] annotations,
                            final MediaType mediaType, final MultivaluedMap httpHeaders, final OutputStream entityStream)
                throws IOException, WebApplicationException {
            entityStream.write(String.format("%s%s%s", JsonOpenning, t.toString(), JsonClosing)
                    .getBytes(MessageUtils.getCharset(mediaType)));
        }

        @Override
        public Object readFrom(final Class type, final Type genericType, final Annotation[] annotations,
                               final MediaType mediaType, final MultivaluedMap httpHeaders, final InputStream entityStream)
                throws IOException, WebApplicationException {
            final String jsonExpression = readStringFromStream(entityStream, MessageUtils.getCharset(mediaType));
            return new ClipboardData(jsonExpression.replace(JsonOpenning, "").replace(JsonClosing, ""));
        }
    }

    @Override
    public boolean isWriteable(final Class type, final Type genericType, final Annotation[] annotations,
                               final MediaType mediaType) {
        return isKnownType(type, genericType);
    }

    private boolean isKnownType(final Class<?> type, final Type genericType) {
        return type.isAssignableFrom(ClipboardData.class)
                || (Collection.class.isAssignableFrom(type)
                    && (((ParameterizedType) genericType).getActualTypeArguments()[0]).equals(String.class));
    }

    @Override
    public long getSize(final Object t, final Class type, final Type genericType, final Annotation[] annotations,
                        final MediaType mediaType) {
        return -1;
    }

    @Override
    public boolean isReadable(final Class type, final Type genericType, final Annotation[] annotations,
                              final MediaType mediaType) {
        return isKnownType(type, genericType);
    }

    private static String readStringFromStream(final InputStream entityStream, Charset charset) throws IOException {
        final StringBuilder result = new StringBuilder();
        final byte[] buf = new byte[2048];
        int i;
        while ((i = entityStream.read(buf)) != -1) {
            result.append(new String(buf, 0, i, charset));
        }
        return result.toString();
    }
}
