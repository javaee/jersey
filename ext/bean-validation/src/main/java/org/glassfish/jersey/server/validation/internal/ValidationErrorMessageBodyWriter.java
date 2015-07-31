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

package org.glassfish.jersey.server.validation.internal;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;

import org.glassfish.jersey.message.MessageUtils;
import org.glassfish.jersey.server.validation.ValidationError;

/**
 * {@link MessageBodyWriter} providing support for (collections of) {@link ValidationError}
 * that is able to output instances to {@code text/plain}/{@code text/html}.
 *
 * @author Michal Gajdos
 */
final class ValidationErrorMessageBodyWriter implements MessageBodyWriter<Object> {

    @Override
    public boolean isWriteable(final Class<?> type,
                               final Type genericType,
                               final Annotation[] annotations,
                               final MediaType mediaType) {
        return isSupportedMediaType(mediaType) && isSupportedType(type, genericType);
    }

    private boolean isSupportedType(final Class<?> type, final Type genericType) {
        if (ValidationError.class.isAssignableFrom(type)) {
            return true;
        } else if (Collection.class.isAssignableFrom(type)) {
            if (genericType instanceof ParameterizedType) {
                return ValidationError.class
                        .isAssignableFrom((Class) ((ParameterizedType) genericType).getActualTypeArguments()[0]);
            }
        }
        return false;
    }

    private boolean isSupportedMediaType(final MediaType mediaType) {
        return MediaType.TEXT_HTML_TYPE.equals(mediaType) || MediaType.TEXT_PLAIN_TYPE.equals(mediaType);
    }

    @Override
    public long getSize(final Object validationErrors,
                        final Class<?> type,
                        final Type genericType,
                        final Annotation[] annotations,
                        final MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(final Object entity,
                        final Class<?> type,
                        final Type genericType,
                        final Annotation[] annotations,
                        final MediaType mediaType,
                        final MultivaluedMap<String, Object> httpHeaders,
                        final OutputStream entityStream) throws IOException, WebApplicationException {
        final Collection<ValidationError> errors;

        if (entity instanceof ValidationError) {
            errors = Collections.singleton((ValidationError) entity);
        } else {
            //noinspection unchecked
            errors = (Collection<ValidationError>) entity;
        }

        final boolean isPlain = MediaType.TEXT_PLAIN_TYPE.getSubtype().equals(mediaType.getSubtype());

        final StringBuilder builder = new StringBuilder();

        // Root <div>
        if (!isPlain) {
            builder.append("<div class=\"validation-errors\">");
        }

        for (final ValidationError error : errors) {
            if (!isPlain) {
                builder.append("<div class=\"validation-error\">");
            }

            // Message.
            builder.append(isPlain ? error.getMessage() : "<span class=\"message\">" + error.getMessage() + "</span>");
            builder.append(' ');

            builder.append('(');

            // Path.
            builder.append(isPlain ? "path = " : ("<span class=\"path\"><strong>path</strong> = "));
            builder.append(isPlain ? error.getPath() : (error.getPath() + "</span>"));
            builder.append(',');
            builder.append(' ');

            // Invalid value.
            builder.append(isPlain ? "invalidValue = " : ("<span class=\"invalid-value\"><strong>invalidValue</strong> = "));
            builder.append(isPlain ? error.getInvalidValue() : (error.getInvalidValue() + "</span>"));

            builder.append(')');

            if (!isPlain) {
                builder.append("</div>");
            } else {
                builder.append('\n');
            }
        }

        // Root <div>
        if (!isPlain) {
            builder.append("</div>");
        }

        entityStream.write(builder.toString().getBytes(MessageUtils.getCharset(mediaType)));
        entityStream.flush();
    }
}
