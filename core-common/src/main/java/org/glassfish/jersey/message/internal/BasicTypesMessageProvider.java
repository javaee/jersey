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

package org.glassfish.jersey.message.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.security.AccessController;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NoContentException;

import javax.inject.Singleton;

import org.glassfish.jersey.internal.LocalizationMessages;
import org.glassfish.jersey.internal.util.ReflectionHelper;

/**
 * The basic types message body provider for {@link MediaType#TEXT_PLAIN} media type.
 * <p/>
 * The provider processes primitive types and also other {@link Number} implementations like {@link java.math.BigDecimal},
 * {@link java.math.BigInteger}, {@link AtomicInteger},  {@link AtomicLong} and all other implementations which has one String
 * argument constructor.
 *
 * @author Miroslav Fuksa
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
@Produces({"text/plain"})
@Consumes({"text/plain"})
@Singleton
final class BasicTypesMessageProvider extends AbstractMessageReaderWriterProvider<Object> {

    private static enum PrimitiveTypes {
        BYTE(Byte.class, byte.class) {
            @Override
            public Object convert(String s) {
                return Byte.valueOf(s);
            }
        },
        SHORT(Short.class, short.class) {
            @Override
            public Object convert(String s) {
                return Short.valueOf(s);
            }
        },
        INTEGER(Integer.class, int.class) {
            @Override
            public Object convert(String s) {
                return Integer.valueOf(s);
            }
        },
        LONG(Long.class, long.class) {
            @Override
            public Object convert(String s) {
                return Long.valueOf(s);
            }
        },
        FLOAT(Float.class, float.class) {
            @Override
            public Object convert(String s) {
                return Float.valueOf(s);
            }
        },
        DOUBLE(Double.class, double.class) {
            @Override
            public Object convert(String s) {
                return Double.valueOf(s);
            }
        },
        BOOLEAN(Boolean.class, boolean.class) {
            @Override
            public Object convert(String s) {
                return Boolean.valueOf(s);
            }
        },
        CHAR(Character.class, char.class) {
            @Override
            public Object convert(String s) {
                if (s.length() != 1) {
                    throw new MessageBodyProcessingException(LocalizationMessages
                            .ERROR_ENTITY_PROVIDER_BASICTYPES_CHARACTER_MORECHARS());
                }
                return s.charAt(0);
            }
        };

        public static PrimitiveTypes forType(Class<?> type) {
            for (PrimitiveTypes primitive : PrimitiveTypes.values()) {
                if (primitive.supports(type)) {
                    return primitive;
                }
            }
            return null;
        }

        private final Class<?> wrapper;
        private final Class<?> primitive;

        private PrimitiveTypes(Class<?> wrapper, Class<?> primitive) {
            this.wrapper = wrapper;
            this.primitive = primitive;
        }

        public abstract Object convert(String s);

        public boolean supports(Class<?> type) {
            return type == wrapper || type == primitive;
        }
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return canProcess(type);
    }

    @Override
    public Object readFrom(
            Class<Object> type,
            Type genericType,
            Annotation[] annotations,
            MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders,
            InputStream entityStream) throws IOException, WebApplicationException {
        final String entityString = readFromAsString(entityStream, mediaType);
        if (entityString.isEmpty()) {
            throw new NoContentException(LocalizationMessages.ERROR_READING_ENTITY_MISSING());
        }
        final PrimitiveTypes primitiveType = PrimitiveTypes.forType(type);
        if (primitiveType != null) {
            return primitiveType.convert(entityString);
        }

        final Constructor constructor = AccessController.doPrivileged(ReflectionHelper.getStringConstructorPA(type));
        if (constructor != null) {
            try {
                return type.cast(constructor.newInstance(entityString));
            } catch (Exception e) {
                throw new MessageBodyProcessingException(LocalizationMessages.ERROR_ENTITY_PROVIDER_BASICTYPES_CONSTRUCTOR(type));
            }
        }

        if (AtomicInteger.class.isAssignableFrom(type)) {
            return new AtomicInteger((Integer) PrimitiveTypes.INTEGER.convert(entityString));
        }

        if (AtomicLong.class.isAssignableFrom(type)) {
            return new AtomicLong((Long) PrimitiveTypes.LONG.convert(entityString));
        }

        throw new MessageBodyProcessingException(LocalizationMessages.ERROR_ENTITY_PROVIDER_BASICTYPES_UNKWNOWN(type));
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return canProcess(type);

    }

    private boolean canProcess(Class<?> type) {
        if (PrimitiveTypes.forType(type) != null) {
            return true;
        }
        if (Number.class.isAssignableFrom(type)) {
            final Constructor constructor = AccessController.doPrivileged(ReflectionHelper.getStringConstructorPA(type));
            if (constructor != null) {
                return true;
            }
            if (AtomicInteger.class.isAssignableFrom(type) || AtomicLong.class.isAssignableFrom(type)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public long getSize(Object t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return t.toString().length();
    }

    @Override
    public void writeTo(
            Object o,
            Class<?> type,
            Type genericType,
            Annotation[] annotations,
            MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders,
            OutputStream entityStream) throws IOException, WebApplicationException {
        writeToAsString(o.toString(), entityStream, mediaType);
    }
}
