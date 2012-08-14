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
package org.glassfish.jersey.server.model;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.CookieParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.Encoded;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.Uri;
import javax.ws.rs.core.Context;

import org.glassfish.jersey.internal.util.ReflectionHelper;
import org.glassfish.jersey.internal.util.collection.ClassTypePair;

/**
 * Method parameter model.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class Parameter implements AnnotatedElement {
    private static final Logger LOGGER = Logger.getLogger(Parameter.class.getName());

    /**
     * Parameter injection sources type.
     */
    public static enum Source {

        /**
         * Entity parameter injection source.
         */
        ENTITY,
        /**
         * Query parameter injection source.
         */
        QUERY,
        /**
         * Matrix parameter injection source.
         */
        MATRIX,
        /**
         * Path parameter injection source.
         */
        PATH,
        /**
         * Cookie parameter injection source.
         */
        COOKIE,
        /**
         * Header parameter injection source.
         */
        HEADER,
        /**
         * Context parameter injection source.
         */
        CONTEXT,
        /**
         * Form parameter injection source.
         */
        FORM,
        /**
         * Uri parameter injection source.
         */
        URI,
        /**
         * Unknown parameter injection source.
         */
        UNKNOWN
    }

    private static interface ParamAnnotationHelper<T extends Annotation> {

        public String getValueOf(T a);

        public Parameter.Source getSource();
    }

    private static Map<Class, ParamAnnotationHelper> createParamAnotHelperMap() {
        Map<Class, ParamAnnotationHelper> m = new WeakHashMap<Class, ParamAnnotationHelper>();
        m.put(Context.class, new ParamAnnotationHelper<Context>() {

            @Override
            public String getValueOf(Context a) {
                return null;
            }

            @Override
            public Parameter.Source getSource() {
                return Parameter.Source.CONTEXT;
            }
        });
        m.put(HeaderParam.class, new ParamAnnotationHelper<HeaderParam>() {

            @Override
            public String getValueOf(HeaderParam a) {
                return a.value();
            }

            @Override
            public Parameter.Source getSource() {
                return Parameter.Source.HEADER;
            }
        });
        m.put(CookieParam.class, new ParamAnnotationHelper<CookieParam>() {

            @Override
            public String getValueOf(CookieParam a) {
                return a.value();
            }

            @Override
            public Parameter.Source getSource() {
                return Parameter.Source.COOKIE;
            }
        });
        m.put(MatrixParam.class, new ParamAnnotationHelper<MatrixParam>() {

            @Override
            public String getValueOf(MatrixParam a) {
                return a.value();
            }

            @Override
            public Parameter.Source getSource() {
                return Parameter.Source.MATRIX;
            }
        });
        m.put(QueryParam.class, new ParamAnnotationHelper<QueryParam>() {

            @Override
            public String getValueOf(QueryParam a) {
                return a.value();
            }

            @Override
            public Parameter.Source getSource() {
                return Parameter.Source.QUERY;
            }
        });
        m.put(PathParam.class, new ParamAnnotationHelper<PathParam>() {

            @Override
            public String getValueOf(PathParam a) {
                return a.value();
            }

            @Override
            public Parameter.Source getSource() {
                return Parameter.Source.PATH;
            }
        });
        m.put(FormParam.class, new ParamAnnotationHelper<FormParam>() {

            @Override
            public String getValueOf(FormParam a) {
                return a.value();
            }

            @Override
            public Parameter.Source getSource() {
                return Parameter.Source.FORM;
            }
        });
        m.put(Uri.class, new ParamAnnotationHelper<Uri>() {

            @Override
            public String getValueOf(Uri a) {
                return a.value();
            }

            @Override
            public Parameter.Source getSource() {
                return Parameter.Source.URI;
            }
        });
        return Collections.unmodifiableMap(m);
    }
    private final static Map<Class, ParamAnnotationHelper> ANOT_HELPER_MAP = createParamAnotHelperMap();

    /**
     * Create a parameter model.
     *
     * @param concreteClass concrete resource method handler implementation class.
     * @param declaringClass declaring class of the method the parameter belongs to.
     * @param keepEncoded set to {@code true} to disable automatic decoding
     *     of all the constructor parameters. (See {@link Encoded}.
     * @param rawType     raw Java parameter type.
     * @param type        generic Java parameter type.
     * @param annotations parameter annotations.
     * @return new parameter model.
     */
    @SuppressWarnings("unchecked")
    public static Parameter create(
            Class concreteClass,
            Class declaringClass,
            boolean keepEncoded,
            Class<?> rawType,
            Type type,
            Annotation[] annotations) {

        if (null == annotations) {
            return null;
        }

        Annotation paramAnnotation = null;
        Parameter.Source paramSource = null;
        String paramName = null;
        boolean paramEncoded = keepEncoded;
        String paramDefault = null;

        /**
         * Create a parameter from the list of annotations. Unknown annotated
         * parameters are also supported, and in such a cases the last
         * unrecognized annotation is taken to be that associated with the
         * parameter.
         */
        for (Annotation annotation : annotations) {
            if (ANOT_HELPER_MAP.containsKey(annotation.annotationType())) {
                ParamAnnotationHelper helper = ANOT_HELPER_MAP.get(annotation.annotationType());
                paramAnnotation = annotation;
                paramSource = helper.getSource();
                paramName = helper.getValueOf(annotation);
            } else if (Encoded.class == annotation.annotationType()) {
                paramEncoded = true;
            } else if (DefaultValue.class == annotation.annotationType()) {
                paramDefault = ((DefaultValue) annotation).value();
            } else {
                // lets only clear things down if we've not found a ANOT_HELPER_MAP annotation already
                if (paramAnnotation == null) {
                    paramAnnotation = annotation;
                    paramSource = Source.UNKNOWN;
                    paramName = getValue(annotation);
                }
            }
        }

        if (paramAnnotation == null) {
            paramSource = Parameter.Source.ENTITY;
        }

        ClassTypePair ct = ReflectionHelper.resolveGenericType(
                concreteClass, declaringClass, rawType, type);

        return new Parameter(
                annotations, paramAnnotation,
                paramSource,
                paramName,
                ct.rawClass(),
                ct.type(),
                paramEncoded,
                paramDefault);
    }

    private static List<Parameter> create(
            Class concreteClass,
            Class declaringClass,
            boolean keepEncoded,
            Class[] parameterTypes,
            Type[] genericParameterTypes,
            Annotation[][] parameterAnnotations) {

        final List<Parameter> parameters = new ArrayList<Parameter>(parameterTypes.length);

        for (int i = 0; i < parameterTypes.length; i++) {
            final Parameter parameter = Parameter.create(
                    concreteClass,
                    declaringClass,
                    keepEncoded,
                    parameterTypes[i],
                    genericParameterTypes[i],
                    parameterAnnotations[i]);
            if (null != parameter) {
                parameters.add(parameter);
            } else {
                // TODO throw IllegalStateException instead?
                return Collections.emptyList();
            }
        }

        return parameters;
    }

    /**
     * Create a list of parameter models for a given resource method handler
     * injectable constructor.
     *
     * @param concreteClass concrete resource method handler implementation class.
     * @param declaringClass TODO ???
     * @param ctor injectable constructor of the resource method handler.
     * @param keepEncoded set to {@code true} to disable automatic decoding
     *     of all the constructor parameters. (See {@link Encoded}.
     *
     * @return a list of constructor parameter models.
     */
    public static List<Parameter> create(
            Class concreteClass,
            Class declaringClass,
            Constructor<?> ctor,
            boolean keepEncoded) {

        Class[] parameterTypes = ctor.getParameterTypes();
        Type[] genericParameterTypes = ctor.getGenericParameterTypes();
        // Workaround bug http://bugs.sun.com/view_bug.do?bug_id=5087240
        if (parameterTypes.length != genericParameterTypes.length) {
            Type[] _genericParameterTypes = new Type[parameterTypes.length];
            _genericParameterTypes[0] = parameterTypes[0];
            System.arraycopy(genericParameterTypes, 0, _genericParameterTypes, 1, genericParameterTypes.length);
            genericParameterTypes = _genericParameterTypes;
        }

        return create(
                concreteClass, declaringClass,
                ((null != ctor.getAnnotation(Encoded.class)) || keepEncoded),
                parameterTypes,
                genericParameterTypes,
                ctor.getParameterAnnotations());
    }

    /**
     * Create a list of parameter models for a given Java method handling a resource
     * method, sub-resource method or a sub-resource locator.
     *
     * @param concreteClass concrete resource method handler implementation class.
     * @param declaringClass the class declaring the handling Java method.
     * @param javaMethod Java method handling a resource method, sub-resource
     *     method or a sub-resource locator.
     * @param keepEncoded set to {@code true} to disable automatic decoding
     *     of all the method parameters. (See {@link Encoded}.
     *
     * @return a list of handling method parameter models.
     */
    public static List<Parameter> create(
            Class concreteClass,
            Class declaringClass,
            Method javaMethod,
            boolean keepEncoded) {

        AnnotatedMethod method = new AnnotatedMethod(javaMethod);

        return create(
                concreteClass, declaringClass,
                ((null != method.getAnnotation(Encoded.class)) || keepEncoded),
                method.getParameterTypes(),
                method.getGenericParameterTypes(),
                method.getParameterAnnotations());
    }

    /**
     * Create new parameter model by overriding {@link Parameter.Source source}
     * of the original parameter model.
     *
     * @param original original parameter model.
     * @param source new overriding parameter source.
     * @return source-overridden copy of the original parameter.
     */
    public static Parameter overrideSource(Parameter original, Parameter.Source source) {

        return new Parameter(
                    original.annotations,
                    original.annotation,
                    source,
                    source.name(),
                    original.rawType,
                    original.type,
                    original.encoded,
                    original.defaultValue);
    }

    private static String getValue(Annotation a) {
        try {
            Method m = a.annotationType().getMethod("value");
            if (m.getReturnType() != String.class) {
                return null;
            }
            return (String) m.invoke(a);
        } catch (Exception ex) {
            if (LOGGER.isLoggable(Level.FINER)) {
                LOGGER.log(Level.FINER,
                        String.format("Unable to get the %s annotation value property", a.getClass().getName()), ex);
            }
        }
        return null;
    }
    // Instance
    private final Annotation[] annotations;
    private final Annotation annotation;
    private final Parameter.Source source;
    private final String sourceName;
    private final boolean encoded;
    private final String defaultValue;
    private final Class<?> rawType;
    private final Type type;

    private Parameter(
            Annotation[] markers,
            Annotation marker,
            Source source,
            String sourceName,
            Class<?> rawType,
            Type type,
            boolean encoded,
            String defaultValue) {
        this.annotations = markers;
        this.annotation = marker;
        this.source = source;
        this.sourceName = sourceName;
        this.rawType = rawType;
        this.type = type;
        this.encoded = encoded;
        this.defaultValue = defaultValue;
    }

    /**
     * Get the parameter annotations.
     *
     * @return parameter annotations.
     */
    public Annotation getAnnotation() {
        return annotation;
    }

    /**
     * Get the parameter value source type.
     *
     * @return parameter value source type.
     */
    public Parameter.Source getSource() {
        return source;
    }

    /**
     * Get the parameter value source name.
     *
     * @return parameter value source name.
     */
    public String getSourceName() {
        return sourceName;
    }

    /**
     * If {@code true}, the injected parameter value should remain encoded.
     *
     * @return {@code true} if the parameter value should remain encoded,
     *     {@code false} otherwise.
     */
    public boolean isEncoded() {
        return encoded;
    }

    /**
     * Check if the parameter has a default value set.
     *
     * @return {@code true} if the default parameter value has been set,
     *     {@code false} otherwise.
     */
    public boolean hasDefaultValue() {
        return defaultValue != null;
    }

    /**
     * Get the default parameter value.
     *
     * @return default parameter value or {@code null} if no default value has
     *     been set for the parameter.
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * Get raw type information for the parameter.
     *
     * @return raw parameter type information.
     */
    public Class<?> getRawType() {
        return rawType;
    }

    /**
     * Get generic type information for the parameter.
     *
     * @return generic parameter type information.
     */
    public Type getType() {
        return type;
    }

    /**
     * Check if the parameter is {@link ParamQualifier qualified}.
     *
     * @return {@code true} if the parameter is qualified, {@code false} otherwise.
     */
    public boolean isQualified() {
        for (Annotation a : getAnnotations()) {
            if (a.annotationType().isAnnotationPresent(ParamQualifier.class)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
        return getAnnotation(annotationClass) != null;
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        if (annotationClass == null) {
            return null;
        }
        for (Annotation a : annotations) {
            if (a.annotationType() == annotationClass) {
                return annotationClass.cast(a);
            }
        }
        return null;
    }

    @Override
    public Annotation[] getAnnotations() {
        return annotations.clone();
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
        return annotations.clone();
    }
}
