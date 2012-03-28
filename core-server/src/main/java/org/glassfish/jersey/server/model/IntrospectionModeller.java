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
import java.lang.reflect.*;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.internal.util.AnnotatedMethod;
import org.glassfish.jersey.internal.util.MethodList;
import org.glassfish.jersey.internal.util.ReflectionHelper;
import org.glassfish.jersey.internal.util.collection.ClassTypePair;
import org.glassfish.jersey.server.internal.LocalizationMessages;
import org.glassfish.jersey.server.model.Parameter.Source;

/**
 * Utility class for constructing resource model from JAX-RS annotated POJO.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public class IntrospectionModeller {

    private static final Logger LOGGER = Logger.getLogger(IntrospectionModeller.class.getName());

    /**
     * Create a resource model for the class.
     *
     * Method performs an {@link #isAcceptable(java.lang.Class) acceptability} check,
     * on the resource class prior to the resource model creation.
     *
     * @param resourceClass resource class to be modeled.
     * @return resource model of the class.
     * @throws IllegalArgumentException in case the class is not
     *     {@link #isAcceptable(java.lang.Class) acceptable} as a JAX-RS resource.
     *
     */
    public static ResourceClass createResource(Class<?> resourceClass) throws IllegalArgumentException {
        return createResource(resourceClass, true);
    }

    /**
     * Create a resource model for the instance.
     *
     * Unlike {@link #createResource(java.lang.Class)}, this method does not perform
     * {@link #isAcceptable(java.lang.Class) acceptability} check, since it is
     * assumed that the instance of the resource has already been created and is
     * acceptable.
     *
     * @param resource resource instance to be modeled.
     * @return resource model of the instance.
     */
    public static ResourceClass createResource(Object resource) {
        return createResource(resource.getClass(), true);
    }

    private static ResourceClass createResource(Class<?> resourceClass, boolean skipAcceptableCheck) throws IllegalArgumentException {
        if (!skipAcceptableCheck && !isAcceptable(resourceClass)) {
            throw new IllegalArgumentException(LocalizationMessages.NON_INSTANTIATABLE_CLASS(resourceClass));
        }

        final Class<?> annotatedResourceClass = getAnnotatedResourceClass(resourceClass);
        final Path rPathAnnotation = annotatedResourceClass.getAnnotation(Path.class);
        final boolean isRootResourceClass = (null != rPathAnnotation);

        final boolean isEncodedAnotOnClass =
                (null != annotatedResourceClass.getAnnotation(Encoded.class));

        ResourceClass resource;

        if (isRootResourceClass) {
            resource = new ResourceClass(resourceClass,
                    new PathValue(rPathAnnotation.value()));
        } else { // just a subresource class
            resource = new ResourceClass(resourceClass);
        }

        final MethodList methodList = new MethodList(resourceClass);

        final Consumes classScopeConsumesAnnotation =
                annotatedResourceClass.getAnnotation(Consumes.class);
        final Produces classScopeProducesAnnotation =
                annotatedResourceClass.getAnnotation(Produces.class);

        workOutResourceMethodsList(resource, methodList, isEncodedAnotOnClass,
                classScopeConsumesAnnotation, classScopeProducesAnnotation);
        workOutSubResourceMethodsList(resource, methodList, isEncodedAnotOnClass,
                classScopeConsumesAnnotation, classScopeProducesAnnotation);
        workOutSubResourceLocatorsList(resource, methodList, isEncodedAnotOnClass);

        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest(LocalizationMessages.NEW_AR_CREATED_BY_INTROSPECTION_MODELER(
                    resource.toString()));
        }

        return resource;
    }

    /**
     * Check if the class is acceptable as a JAX-RS provider or resource.
     *
     * Method returns {@code false} if the class is either
     * <ul>
     *      <li>abstract</li>
     *      <li>interface</li>
     *      <li>annotation</li>
     *      <li>primitive</li>
     *      <li>local class</li>
     *      <li>non-static member class</li>
     * </ul>
     *
     * @param c class to be checked.
     * @return {@code true} if the class is an acceptable JAX-RS provider or
     *     resource, {@code false} otherwise.
     */
    public static boolean isAcceptable(Class<?> c) {
        return !((c.getModifiers() & Modifier.ABSTRACT) != 0
                || c.isPrimitive()
                || c.isAnnotation()
                || c.isInterface()
                || c.isLocalClass()
                || (c.isMemberClass() && (c.getModifiers() & Modifier.STATIC) == 0));
    }

    /**
     * Checks whether the provided class is a root resource class.
     *
     * @param resourceClass resource class.
     * @return {@code true} if the class is a root resource class, {@code false} otherwise.
     */
    public static boolean isRootResource(Class<?> resourceClass) {
        final Path rPathAnnotation = getAnnotatedResourceClass(resourceClass).getAnnotation(Path.class);
        return null != rPathAnnotation;
    }

    private static Class<?> getAnnotatedResourceClass(Class<?> rc) {
        if (rc.isAnnotationPresent(Path.class)) {
            return rc;
        }

        for (Class<?> i : rc.getInterfaces()) {
            if (i.isAnnotationPresent(Path.class)) {
                return i;
            }
        }

        return rc;
    }

    private static void addConsumes(
            AnnotatedMethod am,
            AbstractResourceMethod resourceMethod,
            Consumes consumeMimeAnnotation) {
        // Override annotation is present in method
        if (am.isAnnotationPresent(Consumes.class)) {
            consumeMimeAnnotation = am.getAnnotation(Consumes.class);
        }

        resourceMethod.setAreInputTypesDeclared(consumeMimeAnnotation != null);

        if (consumeMimeAnnotation != null) {
            for (String mt : consumeMimeAnnotation.value()) {
                resourceMethod.getSupportedInputTypes().add(
                        MediaType.valueOf(mt));

            }
        }
    }

    private static void declareSuspend(
            AnnotatedMethod am,
            AbstractResourceMethod resourceMethod) {
        // Override annotation is present in method
        final Suspend suspend = am.getAnnotation(Suspend.class);
        if (suspend != null) {
            resourceMethod.declareSuspend(suspend.timeOut(), suspend.timeUnit());
        }
    }

    private static void addProduces(
            AnnotatedMethod am,
            AbstractResourceMethod resourceMethod,
            Produces produceMimeAnnotation) {
        // Override annotation is present in method
        if (am.isAnnotationPresent(Produces.class)) {
            produceMimeAnnotation = am.getAnnotation(Produces.class);
        }

        resourceMethod.setAreOutputTypesDeclared(produceMimeAnnotation != null);
        if (produceMimeAnnotation != null) {
            for (String mt : produceMimeAnnotation.value()) {
                resourceMethod.getSupportedOutputTypes().add(
                        MediaType.valueOf(mt));

            }
        }
    }

    private static void workOutResourceMethodsList(
            ResourceClass resource,
            MethodList methodList,
            boolean isEncoded,
            Consumes classScopeConsumesAnnotation,
            Produces classScopeProducesAnnotation) {
        for (AnnotatedMethod m : methodList.hasMetaAnnotation(HttpMethod.class).
                hasNotAnnotation(Path.class)) {

            final ClassTypePair ct = getGenericReturnType(resource.getResourceClass(), m.getMethod());
            final ResourceMethod resourceMethod = new ResourceMethod(
                    resource,
                    m.getMethod(),
                    ct.rawClass(), ct.type(),
                    m.getMetaMethodAnnotations(HttpMethod.class).get(0).value(),
                    m.getAnnotations());

            addConsumes(m, resourceMethod, classScopeConsumesAnnotation);
            addProduces(m, resourceMethod, classScopeProducesAnnotation);
            declareSuspend(m, resourceMethod);
            processParameters(
                    resourceMethod.getDeclaringResource().getResourceClass(),
                    resourceMethod.getMethod().getDeclaringClass(),
                    resourceMethod, m, isEncoded);

            resource.getResourceMethods().add(resourceMethod);
        }
    }

    private static ClassTypePair getGenericReturnType(
            Class concreteClass,
            Method m) {
        return getGenericType(concreteClass, m.getDeclaringClass(), m.getReturnType(), m.getGenericReturnType());
    }

    private static void workOutSubResourceMethodsList(
            ResourceClass resource,
            MethodList methodList,
            boolean isEncoded,
            Consumes classScopeConsumesAnnotation,
            Produces classScopeProducesAnnotation) {

        for (AnnotatedMethod m : methodList.hasMetaAnnotation(HttpMethod.class).hasAnnotation(Path.class)) {

            final Path mPathAnnotation = m.getAnnotation(Path.class);
            final PathValue pv = new PathValue(mPathAnnotation.value());

            final boolean emptySegmentCase = "/".equals(pv.getValue()) || "".equals(pv.getValue());

            if (!emptySegmentCase) {
                final ClassTypePair ct = getGenericReturnType(resource.getResourceClass(), m.getMethod());
                final SubResourceMethod abstractSubResourceMethod = new SubResourceMethod(
                        resource,
                        m.getMethod(),
                        ct.rawClass(), ct.type(),
                        pv,
                        m.getMetaMethodAnnotations(HttpMethod.class).get(0).value(),
                        m.getAnnotations());

                addConsumes(m, abstractSubResourceMethod, classScopeConsumesAnnotation);
                addProduces(m, abstractSubResourceMethod, classScopeProducesAnnotation);
                declareSuspend(m, abstractSubResourceMethod);
                processParameters(
                        abstractSubResourceMethod.getDeclaringResource().getResourceClass(),
                        abstractSubResourceMethod.getMethod().getDeclaringClass(),
                        abstractSubResourceMethod, m, isEncoded);

                resource.getSubResourceMethods().add(abstractSubResourceMethod);

            } else { // treat the sub-resource method as a resource method
                final ClassTypePair ct = getGenericReturnType(resource.getResourceClass(), m.getMethod());
                final ResourceMethod abstractResourceMethod = new ResourceMethod(
                        resource,
                        m.getMethod(),
                        ct.rawClass(), ct.type(),
                        m.getMetaMethodAnnotations(HttpMethod.class).get(0).value(),
                        m.getAnnotations());

                addConsumes(m, abstractResourceMethod, classScopeConsumesAnnotation);
                addProduces(m, abstractResourceMethod, classScopeProducesAnnotation);
                processParameters(
                        abstractResourceMethod.getDeclaringResource().getResourceClass(),
                        abstractResourceMethod.getMethod().getDeclaringClass(),
                        abstractResourceMethod, m, isEncoded);

                resource.getResourceMethods().add(abstractResourceMethod);
            }
        }
    }

    private static void workOutSubResourceLocatorsList(
            ResourceClass resource,
            MethodList methodList,
            boolean isEncoded) {

        for (AnnotatedMethod m : methodList.hasNotMetaAnnotation(HttpMethod.class).
                hasAnnotation(Path.class)) {
            final Path mPathAnnotation = m.getAnnotation(Path.class);
            final SubResourceLocator subResourceLocator = new SubResourceLocator(
                    resource,
                    m.getMethod(),
                    new PathValue(
                    mPathAnnotation.value()),
                    m.getAnnotations());

            processParameters(
                    subResourceLocator.getResource().getResourceClass(),
                    subResourceLocator.getMethod().getDeclaringClass(),
                    subResourceLocator, m, isEncoded);

            resource.getSubResourceLocators().add(subResourceLocator);
        }
    }

    private static void processParameters(
            Class concreteClass,
            Class declaringClass,
            Parameterized parametrized,
            Constructor<?> ctor,
            boolean isEncoded) {
        Class[] parameterTypes = ctor.getParameterTypes();
        Type[] genericParameterTypes = ctor.getGenericParameterTypes();
        // Workaround bug http://bugs.sun.com/view_bug.do?bug_id=5087240
        if (parameterTypes.length != genericParameterTypes.length) {
            Type[] _genericParameterTypes = new Type[parameterTypes.length];
            _genericParameterTypes[0] = parameterTypes[0];
            System.arraycopy(genericParameterTypes, 0, _genericParameterTypes, 1, genericParameterTypes.length);
            genericParameterTypes = _genericParameterTypes;
        }

        processParameters(
                concreteClass, declaringClass,
                parametrized,
                ((null != ctor.getAnnotation(Encoded.class)) || isEncoded),
                parameterTypes,
                genericParameterTypes,
                ctor.getParameterAnnotations());
    }

    private static void processParameters(
            Class concreteClass,
            Class declaringClass,
            Parameterized parametrized,
            AnnotatedMethod method,
            boolean isEncoded) {
        processParameters(
                concreteClass, declaringClass,
                parametrized,
                ((null != method.getAnnotation(Encoded.class)) || isEncoded),
                method.getParameterTypes(),
                method.getGenericParameterTypes(),
                method.getParameterAnnotations());
    }

    private static void processParameters(
            Class concreteClass,
            Class declaringClass,
            Parameterized parametrized,
            boolean isEncoded,
            Class[] parameterTypes,
            Type[] genericParameterTypes,
            Annotation[][] parameterAnnotations) {

        for (int i = 0; i < parameterTypes.length; i++) {
            Parameter parameter = createParameter(
                    concreteClass, declaringClass,
                    isEncoded, parameterTypes[i],
                    genericParameterTypes[i],
                    parameterAnnotations[i]);
            if (null != parameter) {
                parametrized.getParameters().add(parameter);
            } else {
                // clean up the parameters
                parametrized.getParameters().removeAll(parametrized.getParameters());
                break;
            }
        }
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
        return Collections.unmodifiableMap(m);
    }
    private final static Map<Class, ParamAnnotationHelper> ANOT_HELPER_MAP =
            createParamAnotHelperMap();

    @SuppressWarnings("unchecked")
    public static Parameter createParameter(
            Class concreteClass,
            Class declaringClass,
            boolean isEncoded,
            Class<?> paramClass,
            Type paramType,
            Annotation[] annotations) {

        if (null == annotations) {
            return null;
        }

        Annotation paramAnnotation = null;
        Parameter.Source paramSource = null;
        String paramName = null;
        boolean paramEncoded = isEncoded;
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
                paramAnnotation = annotation;
                paramSource = Source.UNKNOWN;
                paramName = getValue(annotation);
            }
        }

        if (paramAnnotation == null) {
            paramSource = Parameter.Source.ENTITY;
        }

        ClassTypePair ct = getGenericType(concreteClass, declaringClass, paramClass, paramType);
        paramType = ct.type();
        paramClass = ct.rawClass();

        return new Parameter(
                annotations, paramAnnotation,
                paramSource,
                paramName, paramType, paramClass,
                paramEncoded, paramDefault);
    }

    private static String getValue(Annotation a) {
        try {
            Method m = a.annotationType().getMethod("value");
            if (m.getReturnType() != String.class) {
                return null;
            }
            return (String) m.invoke(a);
        } catch (Exception ex) {
        }
        return null;
    }

    private static ClassTypePair getGenericType(
            final Class concreteClass,
            final Class declaringClass,
            final Class c,
            final Type t) {
        if (t instanceof TypeVariable) {
            ClassTypePair ct = ReflectionHelper.resolveTypeVariable(
                    concreteClass,
                    declaringClass,
                    (TypeVariable) t);

            if (ct != null) {
                return ct;
            }
        } else if (t instanceof ParameterizedType) {
            final ParameterizedType pt = (ParameterizedType) t;
            final Type[] ptts = pt.getActualTypeArguments();
            boolean modified = false;
            for (int i = 0; i < ptts.length; i++) {
                ClassTypePair ct =
                        getGenericType(concreteClass, declaringClass, (Class) pt.getRawType(), ptts[i]);
                if (ct.type() != ptts[i]) {
                    ptts[i] = ct.type();
                    modified = true;
                }
            }
            if (modified) {
                ParameterizedType rpt = new ParameterizedType() {

                    @Override
                    public Type[] getActualTypeArguments() {
                        return ptts.clone();
                    }

                    @Override
                    public Type getRawType() {
                        return pt.getRawType();
                    }

                    @Override
                    public Type getOwnerType() {
                        return pt.getOwnerType();
                    }
                };
                return ClassTypePair.of((Class<?>) pt.getRawType(), rpt);
            }
        } else if (t instanceof GenericArrayType) {
            GenericArrayType gat = (GenericArrayType) t;
            final ClassTypePair ct =
                    getGenericType(concreteClass, declaringClass, null, gat.getGenericComponentType());
            if (gat.getGenericComponentType() != ct.type()) {
                try {
                    Class ac = ReflectionHelper.getArrayClass(ct.rawClass());
                    return ClassTypePair.of(ac);
                } catch (Exception e) {
                }
            }
        }

        return ClassTypePair.of(c, t);
    }

    /**
     * Prevents instantiation.
     */
    private IntrospectionModeller() {
    }
}