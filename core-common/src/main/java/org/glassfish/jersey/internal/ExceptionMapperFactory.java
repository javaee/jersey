/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.internal;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.ext.ExceptionMapper;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.glassfish.jersey.internal.inject.Providers;
import org.glassfish.jersey.internal.util.ReflectionHelper;
import org.glassfish.jersey.internal.util.collection.ClassTypePair;
import org.glassfish.jersey.spi.ExceptionMappers;
import org.glassfish.jersey.spi.ExtendedExceptionMapper;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

import com.google.common.collect.Maps;

/**
 * {@link ExceptionMappers Exception mappers} implementation that aggregates
 * exception mappers and server as the main entry point for exception mapper
 * instance lookup.
 *
 * @author Paul Sandoz
 * @author Santiago Pericas-Geertsen (Santiago.PericasGeertsen at oracle.com)
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class ExceptionMapperFactory implements ExceptionMappers {
    private static final Logger LOGGER = Logger.getLogger(ExceptionMapperFactory.class.getName());

    /**
     * Exception mapper factory injection binder.
     */
    public static class Binder extends AbstractBinder {

        @Override
        protected void configure() {
            bindAsContract(ExceptionMapperFactory.class).to(ExceptionMappers.class).in(Singleton.class);
        }
    }

    private static class ExceptionMapperType {

        ExceptionMapper mapper;
        Class<? extends Throwable> exceptionType;

        public ExceptionMapperType(ExceptionMapper mapper, Class<? extends Throwable> exceptionType) {
            this.mapper = mapper;
            this.exceptionType = exceptionType;
        }
    }

    private Set<ExceptionMapperType> exceptionMapperTypes = new LinkedHashSet<ExceptionMapperType>();

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Throwable> ExceptionMapper<T> findMapping(T exceptionInstance) {
        return find((Class<T>)exceptionInstance.getClass(), exceptionInstance);
    }

    @Override
    public <T extends Throwable> ExceptionMapper<T> find(Class<T> type) {
        return find(type, null);
    }


    @SuppressWarnings("unchecked")
    private <T extends Throwable> ExceptionMapper<T> find(Class<T> type, T exceptionInstance) {

        Map<Integer, ExceptionMapper<T>> orderedMappers = Maps.newTreeMap();

        for (ExceptionMapperType mapperType : exceptionMapperTypes) {
            int d = distance(type, mapperType.exceptionType);
            if (d >= 0) {
                orderedMappers.put(d, mapperType.mapper);
            }
        }

        if (orderedMappers.size() == 0) {
            return null;
        }

        if (exceptionInstance != null) {
            for (ExceptionMapper<T> mapper : orderedMappers.values()) {
                if (mapper instanceof ExtendedExceptionMapper) {
                    final boolean mappable = ((ExtendedExceptionMapper<T>) mapper).isMappable(exceptionInstance);
                    if (mappable) {
                        return mapper;
                    }
                } else {
                    return mapper;
                }
            }
            return null;
        } else {
            return orderedMappers.values().iterator().next();
        }
    }


    /**
     * Create new exception mapper factory initialized with {@link ServiceLocator
     * HK2 service locator} instance that will be used to look up all providers implementing
     * {@link ExceptionMapper} interface.
     *
     * @param locator HK2 service locator.
     */
    @Inject
    public ExceptionMapperFactory(ServiceLocator locator) {
        for (ExceptionMapper<?> mapper : Providers.getAllProviders(locator, ExceptionMapper.class)) {
            Class<? extends Throwable> c = getExceptionType(mapper.getClass());
            if (c != null) {
                exceptionMapperTypes.add(new ExceptionMapperType(mapper, c));
            }
        }
    }

    private int distance(Class<?> c, Class<?> emtc) {
        int distance = 0;
        if (!emtc.isAssignableFrom(c)) {
            return -1;
        }

        while (c != emtc) {
            c = c.getSuperclass();
            distance++;
        }

        return distance;
    }

    @SuppressWarnings("unchecked")
    private Class<? extends Throwable> getExceptionType(Class<? extends ExceptionMapper> c) {
        Class<?> t = getType(c);
        if (Throwable.class.isAssignableFrom(t)) {
            return (Class<? extends Throwable>) t;
        }

        if (LOGGER.isLoggable(Level.WARNING)) {
            LOGGER.warning(LocalizationMessages.EXCEPTION_MAPPER_SUPPORTED_TYPE_UNKNOWN(c.getName()));
        }

        return null;
    }

    /**
     * Get exception type for given exception mapper class.
     *
     * @param clazz class to get exception type for.
     * @return exception type for given class.
     */
    private Class getType(final Class<? extends ExceptionMapper> clazz) {
        Class clazzHolder = clazz;

        while (clazzHolder != Object.class) {
            Class type = getTypeFromInterface(clazzHolder, clazz);
            if (type != null) {
                return type;
            }

            clazzHolder = clazzHolder.getSuperclass();
        }

        throw new ProcessingException(LocalizationMessages.ERROR_FINDING_EXCEPTION_MAPPER_TYPE(clazz));
    }

    /**
     * Iterate through interface hierarchy of {@code clazz} and get exception type for given class.
     *
     * @param clazz class to inspect.
     * @return exception type for given class or {@code null} if the class doesn't implement {@code ExceptionMapper}.
     */
    private Class getTypeFromInterface(Class<?> clazz, final Class<? extends ExceptionMapper> original) {
        final Type[] types = clazz.getGenericInterfaces();

        for (final Type type : types) {
            if (type instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) type;
                if (pt.getRawType() == ExceptionMapper.class
                        || pt.getRawType() == ExtendedExceptionMapper.class) {
                    return getResolvedType(pt.getActualTypeArguments()[0], original, clazz);
                }
            } else if (type instanceof Class<?>) {
                clazz = (Class<?>) type;

                if (ExceptionMapper.class.isAssignableFrom(clazz)) {
                    return getTypeFromInterface(clazz, original);
                }
            }
        }

        return null;
    }

    private Class getResolvedType(Type t, Class c, Class dc) {
        if (t instanceof Class) {
            return (Class) t;
        } else if (t instanceof TypeVariable) {
            ClassTypePair ct = ReflectionHelper.resolveTypeVariable(c, dc, (TypeVariable) t);
            if (ct != null) {
                return ct.rawClass();
            } else {
                return null;
            }
        } else if (t instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) t;
            return (Class) pt.getRawType();
        } else {
            return null;
        }
    }
}
