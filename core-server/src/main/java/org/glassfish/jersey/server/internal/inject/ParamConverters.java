/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.server.internal.inject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.security.AccessController;
import java.text.ParseException;
import java.util.Date;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;

import javax.inject.Singleton;

import org.glassfish.jersey.internal.util.ReflectionHelper;
import org.glassfish.jersey.message.internal.HttpDateFormat;

import org.glassfish.hk2.api.ServiceLocator;

/**
 * Container of several different {@link ParamConverterProvider param converter providers}
 * implementations. The nested provider implementations encapsulate various different
 * strategies of constructing an instance from a {@code String} value.
 *
 * @author Paul Sandoz
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
@Singleton
class ParamConverters {

    private static abstract class AbstractStringReader<T> implements ParamConverter<T> {

        @Override
        public T fromString(String value) {
            try {
                return _fromString(value);
            } catch (InvocationTargetException ex) {
                // if the value is an empty string, return null
                if (value.isEmpty()) {
                    return null;
                }
                Throwable cause = ex.getCause();
                if (cause instanceof WebApplicationException) {
                    throw (WebApplicationException) cause;
                } else {
                    throw new ExtractorException(cause);
                }
            } catch (Exception ex) {
                throw new ProcessingException(ex);
            }
        }

        protected abstract T _fromString(String value) throws Exception;

        @Override
        public String toString(T value) throws IllegalArgumentException {
            return value.toString();
        }

    }


    /**
     * Provider of {@link ParamConverter param converter} that produce the target Java type instance
     * by invoking a single {@code String} parameter constructor on the target type.
     */
    @Singleton
    public static class StringConstructor implements ParamConverterProvider {

        @Override
        public <T> ParamConverter<T> getConverter(final Class<T> rawType, Type genericType, Annotation[] annotations) {

            final Constructor constructor = AccessController.doPrivileged(ReflectionHelper.getStringConstructorPA(rawType));

            return (constructor == null) ? null : new AbstractStringReader<T>() {

                @Override
                protected T _fromString(String value) throws Exception {
                    return rawType.cast(constructor.newInstance(value));
                }
            };
        }

    }

    /**
     * Provider of {@link ParamConverter param converter} that produce the target Java type instance
     * by invoking a static {@code valueOf(String)} method on the target type.
     */
    @Singleton
    public static class TypeValueOf implements ParamConverterProvider {

        @Override
        public <T> ParamConverter<T> getConverter(final Class<T> rawType, Type genericType, Annotation[] annotations) {

            final Method valueOf = AccessController.doPrivileged(ReflectionHelper.getValueOfStringMethodPA(rawType));

            return (valueOf == null) ? null : new AbstractStringReader<T>() {

                @Override
                public T _fromString(String value) throws Exception {
                    return rawType.cast(valueOf.invoke(null, value));
                }
            };
        }
    }

    /**
     * Provider of {@link ParamConverter param converter} that produce the target Java type instance
     * by invoking a static {@code fromString(String)} method on the target type.
     */
    @Singleton
    public static class TypeFromString implements ParamConverterProvider {

        @Override
        public <T> ParamConverter<T> getConverter(final Class<T> rawType, Type genericType, Annotation[] annotations) {

            final Method fromStringMethod = AccessController.doPrivileged(ReflectionHelper.getFromStringStringMethodPA(rawType));

            return (fromStringMethod == null) ? null : new AbstractStringReader<T>() {

                @Override
                public T _fromString(String value) throws Exception {
                    return rawType.cast(fromStringMethod.invoke(null, value));
                }
            };
        }
    }

    /**
     * Provider of {@link ParamConverter param converter} that produce the target Java {@link Enum enum} type instance
     * by invoking a static {@code fromString(String)} method on the target enum type.
     */
    @Singleton
    public static class TypeFromStringEnum extends TypeFromString {

        @Override
        public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
            return (!Enum.class.isAssignableFrom(rawType)) ? null : super.getConverter(rawType, genericType, annotations);
        }
    }

    /**
     * Provider of {@link ParamConverter param converter} that convert the supplied string into a Java
     * {@link Date} instance using conversion method from the
     * {@link HttpDateFormat http date formatter} utility class.
     */
    @Singleton
    public static class DateProvider implements ParamConverterProvider {

        @Override
        public <T> ParamConverter<T> getConverter(final Class<T> rawType, Type genericType, Annotation[] annotations) {
            return (rawType != Date.class) ? null : new ParamConverter<T>() {

                @Override
                public T fromString(String value) {
                    try {
                        return rawType.cast(HttpDateFormat.readDate(value));
                    } catch (ParseException ex) {
                        throw new ExtractorException(ex);
                    }
                }

                @Override
                public String toString(T value) throws IllegalArgumentException {
                    return value.toString();
                }
            };
        }
    }

    /**
     * Aggregated {@link ParamConverterProvider param converter provider}.
     */
    @Singleton
    public static class AggregatedProvider implements ParamConverterProvider {

        private final ParamConverterProvider[] providers;

        /**
         * Create new aggregated {@link ParamConverterProvider param converter provider}.
         *
         * @param locator HK2 service locator.
         */
        public AggregatedProvider(@Context ServiceLocator locator) {
            providers = new ParamConverterProvider[]{
                    // custom converters
                    locator.createAndInitialize(JaxbStringReaderProvider.RootElementProvider.class),
                    locator.createAndInitialize(DateProvider.class),
                    // spec converters
                    locator.createAndInitialize(TypeFromStringEnum.class),
                    locator.createAndInitialize(TypeValueOf.class),
                    locator.createAndInitialize(TypeFromString.class),
                    locator.createAndInitialize(StringConstructor.class)
            };
        }

        @Override
        public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
            for (ParamConverterProvider p : providers) {
                // This iteration trough providers is important. It can't be replaced by just registering all the internal
                // providers of this class. Using iteration trough array the correct ordering of providers is ensured (see
                // javadoc of PathParam, HeaderParam, ... - there is defined a fixed order of constructing objects form Strings).
                final ParamConverter<T> reader = p.getConverter(rawType, genericType, annotations);
                if (reader != null) {
                    return reader;
                }
            }
            return null;
        }
    }
}
