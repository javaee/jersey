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
package org.glassfish.jersey.server.model;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.BeanParam;
import javax.ws.rs.CookieParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.Encoded;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;

import org.glassfish.jersey.internal.util.ReflectionHelper;
import org.glassfish.jersey.internal.util.collection.ClassTypePair;
import org.glassfish.jersey.server.Uri;

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
    public enum Source {

        /**
         * Context parameter injection source.
         */
        CONTEXT,
        /**
         * Cookie parameter injection source.
         */
        COOKIE,
        /**
         * Entity parameter injection source.
         */
        ENTITY,
        /**
         * Form parameter injection source.
         */
        FORM,
        /**
         * Header parameter injection source.
         */
        HEADER,
        /**
         * Uri parameter injection source.
         */
        URI,
        /**
         * Matrix parameter injection source.
         */
        MATRIX,
        /**
         * Path parameter injection source.
         */
        PATH,
        /**
         * Query parameter injection source.
         */
        QUERY,
        /**
         * Suspended async response injection source.
         */
        SUSPENDED,
        /**
         * Bean param parameter injection source.
         */
        BEAN_PARAM,
        /**
         * Unknown parameter injection source.
         */
        UNKNOWN
    }

    private interface ParamAnnotationHelper<T extends Annotation> {

        public String getValueOf(T a);

        public Parameter.Source getSource();
    }

    private static Map<Class, ParamAnnotationHelper> createParamAnnotationHelperMap() {
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
        m.put(Suspended.class, new ParamAnnotationHelper<Suspended>() {

            @Override
            public String getValueOf(Suspended a) {
                return Suspended.class.getName();
            }

            @Override
            public Parameter.Source getSource() {
                return Parameter.Source.SUSPENDED;
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
        m.put(BeanParam.class, new ParamAnnotationHelper<BeanParam>() {

            @Override
            public String getValueOf(BeanParam a) {
                return null;
            }

            @Override
            public Parameter.Source getSource() {
                return Parameter.Source.BEAN_PARAM;
            }
        });
        return Collections.unmodifiableMap(m);
    }

    private static final Map<Class, ParamAnnotationHelper> ANNOTATION_HELPER_MAP = createParamAnnotationHelperMap();

    /**
     * Create a parameter model.
     *
     * @param concreteClass   concrete resource method handler implementation class.
     * @param declaringClass  declaring class of the method the parameter belongs to or field that this parameter represents.
     * @param encodeByDefault flag indicating whether the parameter should be encoded by default or not. Note that a presence
     *                        of {@link Encoded} annotation in the list of the parameter {@code annotations} will override any
     *                        value set in the flag to {@code true}.
     * @param rawType         raw Java parameter type.
     * @param type            generic Java parameter type.
     * @param annotations     parameter annotations.
     * @return new parameter model.
     */
    @SuppressWarnings("unchecked")
    public static Parameter create(
            Class concreteClass,
            Class declaringClass,
            boolean encodeByDefault,
            Class<?> rawType,
            Type type,
            Annotation[] annotations) {

        if (null == annotations) {
            return null;
        }

        Annotation paramAnnotation = null;
        Parameter.Source paramSource = null;
        String paramName = null;
        boolean paramEncoded = encodeByDefault;
        String paramDefault = null;

        /**
         * Create a parameter from the list of annotations. Unknown annotated
         * parameters are also supported, and in such a cases the last
         * unrecognized annotation is taken to be that associated with the
         * parameter.
         */
        for (Annotation annotation : annotations) {
            if (ANNOTATION_HELPER_MAP.containsKey(annotation.annotationType())) {
                ParamAnnotationHelper helper = ANNOTATION_HELPER_MAP.get(annotation.annotationType());
                paramAnnotation = annotation;
                paramSource = helper.getSource();
                paramName = helper.getValueOf(annotation);
            } else if (Encoded.class == annotation.annotationType()) {
                paramEncoded = true;
            } else if (DefaultValue.class == annotation.annotationType()) {
                paramDefault = ((DefaultValue) annotation).value();
            } else {
                // Take latest unknown annotation, but don't override known annotation
                if ((paramAnnotation == null) || (paramSource == Source.UNKNOWN)) {
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

        if (paramSource == Source.BEAN_PARAM) {
            return new BeanParameter(
                    annotations,
                    paramAnnotation,
                    paramName,
                    ct.rawClass(),
                    ct.type(),
                    paramEncoded,
                    paramDefault);
        } else {
            return new Parameter(
                    annotations,
                    paramAnnotation,
                    paramSource,
                    paramName,
                    ct.rawClass(),
                    ct.type(),
                    paramEncoded,
                    paramDefault);
        }
    }

    private static List<Parameter> create(
            Class concreteClass,
            Class declaringClass,
            boolean keepEncoded,
            Class[] parameterTypes,
            Type[] genericParameterTypes,
            Annotation[][] parameterAnnotations) {

        final List<Parameter> parameters = new ArrayList<>(parameterTypes.length);

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
     * @param concreteClass  concrete resource method handler implementation class.
     * @param declaringClass class where the method has been declared.
     * @param ctor           injectable constructor of the resource method handler.
     * @param keepEncoded    set to {@code true} to disable automatic decoding
     *                       of all the constructor parameters. (See {@link Encoded}.
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
     * @param concreteClass  concrete resource method handler implementation class.
     * @param declaringClass the class declaring the handling Java method.
     * @param javaMethod     Java method handling a resource method, sub-resource
     *                       method or a sub-resource locator.
     * @param keepEncoded    set to {@code true} to disable automatic decoding
     *                       of all the method parameters. (See {@link Encoded}.
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
     * @param source   new overriding parameter source.
     * @return source-overridden copy of the original parameter.
     */
    public static Parameter overrideSource(Parameter original, Parameter.Source source) {

        return new Parameter(
                original.annotations,
                original.sourceAnnotation,
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
    private final Annotation sourceAnnotation;
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
        this.sourceAnnotation = marker;
        this.source = source;
        this.sourceName = sourceName;
        this.rawType = rawType;
        this.type = type;
        this.encoded = encoded;
        this.defaultValue = defaultValue;
    }

    /**
     * Bean Parameter class represents a parameter annotated with {@link BeanParam} which in fact represents
     * additional set of parameters.
     */
    public static class BeanParameter extends Parameter {

        private final Collection<Parameter> parameters;

        private BeanParameter(final Annotation[] markers,
                              final Annotation marker,
                              final String sourceName,
                              final Class<?> rawType,
                              final Type type, final boolean encoded, final String defaultValue) {
            super(markers, marker, Source.BEAN_PARAM, sourceName, rawType, type, encoded, defaultValue);

            final Collection<Parameter> parameters = new LinkedList<>();

            for (Field field : AccessController.doPrivileged(ReflectionHelper.getDeclaredFieldsPA(rawType))) {
                if (field.getDeclaredAnnotations().length > 0) {
                    Parameter beanParamParameter = Parameter.create(
                            rawType,
                            field.getDeclaringClass(),
                            field.isAnnotationPresent(Encoded.class),
                            field.getType(),
                            field.getGenericType(),
                            field.getAnnotations());
                    parameters.add(beanParamParameter);
                }
            }
            for (Constructor constructor : AccessController
                    .doPrivileged(ReflectionHelper.getDeclaredConstructorsPA(rawType))) {
                for (Parameter parameter : Parameter.create(rawType, rawType, constructor, false)) {
                    parameters.add(parameter);
                }
            }

            this.parameters = Collections.unmodifiableCollection(parameters);
        }

        /**
         * @return The transitively associated parameters through this {@link BeanParam} parameter.
         */
        public Collection<Parameter> getParameters() {
            return parameters;
        }
    }

    /**
     * Get the parameter source annotation.
     *
     * @return parameter source annotation.
     */
    public Annotation getSourceAnnotation() {
        return sourceAnnotation;
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
     * Get the parameter source name, i.e. value of the parameter source annotation.
     *
     * @return parameter source name.
     */
    public String getSourceName() {
        return sourceName;
    }

    /**
     * If {@code true}, the injected parameter value should remain encoded.
     *
     * @return {@code true} if the parameter value should remain encoded,
     * {@code false} otherwise.
     */
    public boolean isEncoded() {
        return encoded;
    }

    /**
     * Check if the parameter has a default value set.
     *
     * @return {@code true} if the default parameter value has been set,
     * {@code false} otherwise.
     */
    public boolean hasDefaultValue() {
        return defaultValue != null;
    }

    /**
     * Get the default parameter value.
     *
     * @return default parameter value or {@code null} if no default value has
     * been set for the parameter.
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

    @Override
    public String toString() {
        return String.format("Parameter [type=%s, source=%s, defaultValue=%s]",
                getRawType(), getSourceName(), getDefaultValue());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Parameter parameter = (Parameter) o;

        if (encoded != parameter.encoded) {
            return false;
        }
        if (!Arrays.equals(annotations, parameter.annotations)) {
            return false;
        }
        if (defaultValue != null ? !defaultValue.equals(parameter.defaultValue) : parameter.defaultValue != null) {
            return false;
        }
        if (rawType != null ? !rawType.equals(parameter.rawType) : parameter.rawType != null) {
            return false;
        }
        if (source != parameter.source) {
            return false;
        }
        if (sourceAnnotation != null ? !sourceAnnotation.equals(parameter.sourceAnnotation) : parameter.sourceAnnotation
                != null) {
            return false;
        }
        if (sourceName != null ? !sourceName.equals(parameter.sourceName) : parameter.sourceName != null) {
            return false;
        }
        if (type != null ? !type.equals(parameter.type) : parameter.type != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = annotations != null ? Arrays.hashCode(annotations) : 0;
        result = 31 * result + (sourceAnnotation != null ? sourceAnnotation.hashCode() : 0);
        result = 31 * result + (source != null ? source.hashCode() : 0);
        result = 31 * result + (sourceName != null ? sourceName.hashCode() : 0);
        result = 31 * result + (encoded ? 1 : 0);
        result = 31 * result + (defaultValue != null ? defaultValue.hashCode() : 0);
        result = 31 * result + (rawType != null ? rawType.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }
}
