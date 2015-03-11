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

package org.glassfish.jersey.media.multipart.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

/**
 * A JAX-RS <code>Provider</code> that knows how to serialize and deserialize
 * {@link MultiPartBean} instances.
 */
@Provider
@Consumes("x-application/x-format")
@Produces("x-application/x-format")
public class MultiPartBeanProvider implements MessageBodyReader<MultiPartBean>, MessageBodyWriter<MultiPartBean> {

    private static final MediaType CUSTOM_MEDIA_TYPE = new MediaType("x-application", "x-format");

    public boolean isReadable(final Class<?> type,
                              final Type genericType,
                              final Annotation[] annotations,
                              final MediaType mediaType) {

        return type.isAssignableFrom(MultiPartBean.class) && mediaType.equals(CUSTOM_MEDIA_TYPE);
    }

    public MultiPartBean readFrom(final Class<MultiPartBean> type,
                                  final Type genericType,
                                  final Annotation[] annotations,
                                  final MediaType mediaType,
                                  final MultivaluedMap<String, String> headers,
                                  final InputStream stream) throws IOException, WebApplicationException {

        final InputStreamReader reader = new InputStreamReader(stream);
        final StringBuilder sb = new StringBuilder();

        while (true) {
            int ch = reader.read();
            if ((ch < 0) || ((char) ch == '\n')) {
                break;
            } else {
                sb.append((char) ch);
            }
        }

        String line = sb.toString();
        int equals = line.indexOf('=');
        if (equals < 0) {
            throw new WebApplicationException(
                    new IllegalArgumentException("Input content '" + line + "' is not in a valid format"));
        }

        return new MultiPartBean(line.substring(0, equals), line.substring(equals + 1));
    }

    public boolean isWriteable(final Class<?> type,
                               final Type genericType,
                               final Annotation[] annotations,
                               final MediaType mediaType) {

        return type.isAssignableFrom(MultiPartBean.class) && mediaType.equals(CUSTOM_MEDIA_TYPE);
    }

    public long getSize(final MultiPartBean entity,
                        final Class<?> type,
                        final Type genericType,
                        final Annotation[] annotations,
                        final MediaType mediaType) {
        return -1;
    }

    public void writeTo(final MultiPartBean entity,
                        final Class<?> type,
                        final Type genericType,
                        final Annotation[] annotations,
                        final MediaType mediaType,
                        final MultivaluedMap<String, Object> headers,
                        final OutputStream stream) throws IOException, WebApplicationException {

        OutputStreamWriter writer = new OutputStreamWriter(stream);
        writer.write(entity.getName());
        writer.write('=');
        writer.write(entity.getValue());
        writer.write('\n');
        writer.flush();
    }

}
