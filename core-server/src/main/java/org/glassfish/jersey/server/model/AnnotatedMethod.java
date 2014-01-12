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
package org.glassfish.jersey.server.model;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.Encoded;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import org.glassfish.jersey.internal.util.ReflectionHelper;

/**
 * Annotated method representation.
 *
 * @author Paul Sandoz
 */
public final class AnnotatedMethod implements AnnotatedElement {

    @SuppressWarnings("unchecked")
    private static final Set<Class<? extends Annotation>> METHOD_META_ANNOTATIONS = getSet(
            HttpMethod.class);
    @SuppressWarnings("unchecked")
    private static final Set<Class<? extends Annotation>> METHOD_ANNOTATIONS = getSet(
            Path.class,
            Produces.class,
            Consumes.class);
    @SuppressWarnings("unchecked")
    private static final Set<Class<? extends Annotation>> PARAMETER_ANNOTATIONS = getSet(
            Context.class,
            Encoded.class,
            DefaultValue.class,
            MatrixParam.class,
            QueryParam.class,
            CookieParam.class,
            HeaderParam.class,
            PathParam.class,
            FormParam.class);

    private static Set<Class<? extends Annotation>> getSet(Class<? extends Annotation>... cs) {
        Set<Class<? extends Annotation>> s = new HashSet<Class<? extends Annotation>>();
        s.addAll(Arrays.asList(cs));
        return s;
    }

    private final Method m;
    private final Method am;
    private final Annotation[] methodAnnotations;
    private final Annotation[][] parameterAnnotations;

    /**
     * Create annotated method instance from the {@link Method Java method}.
     *
     * @param method Java method.
     */
    public AnnotatedMethod(Method method) {
        this.m = method;
        this.am = findAnnotatedMethod(method);

        if (method.equals(am)) {
            methodAnnotations = method.getAnnotations();
            parameterAnnotations = method.getParameterAnnotations();
        } else {
            methodAnnotations = mergeMethodAnnotations(method, am);
            parameterAnnotations = mergeParameterAnnotations(method, am);
        }
    }

    /**
     * Get the underlying Java method.
     *
     * @return the underlying Java method.
     */
    public Method getMethod() {
        return am;
    }

    /**
     * Get the underlying declared Java method. This method overrides or is the same as the one retrieved by {@code getMethod}.
     *
     * @return the underlying declared Java method.
     */
    Method getDeclaredMethod() {
        return m;
    }

    /**
     * Get method parameter annotations.
     *
     * @return method parameter annotations.
     */
    public Annotation[][] getParameterAnnotations() {
        return parameterAnnotations.clone();
    }

    /**
     * Get method parameter types.
     *
     * See also {@link Method#getParameterTypes()}.
     *
     * @return method parameter types.
     */
    public Class<?>[] getParameterTypes() {
        return am.getParameterTypes();
    }

    /**
     * Get method type parameters.
     *
     * See also {@link Method#getTypeParameters()}.
     *
     * @return method type parameters.
     */
    public TypeVariable<Method>[] getTypeParameters() {
        return am.getTypeParameters();
    }

    /**
     * Get generic method parameter types.
     *
     * See also {@link Method#getGenericParameterTypes()}.
     *
     * @return generic method parameter types.
     */
    public Type[] getGenericParameterTypes() {
        return am.getGenericParameterTypes();
    }

    /**
     * Get all instances of the specified meta-annotation type found on the method
     * annotations.
     *
     * @param <T>        meta-annotation type.
     * @param annotation meta-annotation class to be searched for.
     * @return meta-annotation instances of a given type annotating the method
     *         annotations.
     */
    public <T extends Annotation> List<T> getMetaMethodAnnotations(
            Class<T> annotation) {
        List<T> ma = new ArrayList<T>();
        for (Annotation a : methodAnnotations) {
            final T metaAnnotation = a.annotationType().getAnnotation(annotation);
            if (metaAnnotation != null) {
                ma.add(metaAnnotation);
            }
        }

        return ma;
    }

    @Override
    public String toString() {
        return m.toString();
    }

    // AnnotatedElement
    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
        for (Annotation ma : methodAnnotations) {
            if (ma.annotationType() == annotationType) {
                return true;
            }
        }
        return false;
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
        for (Annotation ma : methodAnnotations) {
            if (ma.annotationType() == annotationType) {
                return annotationType.cast(ma);
            }
        }
        return am.getAnnotation(annotationType);
    }

    @Override
    public Annotation[] getAnnotations() {
        return methodAnnotations.clone();
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
        return getAnnotations();
    }

    private static Annotation[] mergeMethodAnnotations(Method m, Method am) {
        List<Annotation> al = asList(m.getAnnotations());
        for (Annotation a : am.getAnnotations()) {
            if (!m.isAnnotationPresent(a.getClass())) {
                al.add(a);
            }
        }

        return al.toArray(new Annotation[al.size()]);
    }

    private static Annotation[][] mergeParameterAnnotations(Method m, Method am) {
        Annotation[][] methodParamAnnotations = m.getParameterAnnotations();
        Annotation[][] annotatedMethodParamAnnotations = am.getParameterAnnotations();

        List<List<Annotation>> methodParamAnnotationsList = new ArrayList<List<Annotation>>();
        for (int i = 0; i < methodParamAnnotations.length; i++) {
            List<Annotation> al = asList(methodParamAnnotations[i]);
            for (Annotation a : annotatedMethodParamAnnotations[i]) {
                if (annotationNotInList(a.getClass(), al)) {
                    al.add(a);
                }
            }
            methodParamAnnotationsList.add(al);
        }

        Annotation[][] mergedAnnotations = new Annotation[methodParamAnnotations.length][];
        for (int i = 0; i < methodParamAnnotations.length; i++) {
            List<Annotation> paramAnnotations = methodParamAnnotationsList.get(i);
            mergedAnnotations[i] = paramAnnotations.toArray(new Annotation[paramAnnotations.size()]);
        }

        return mergedAnnotations;
    }

    private static boolean annotationNotInList(Class<? extends Annotation> ca, List<Annotation> la) {
        for (Annotation a : la) {
            if (ca == a.getClass()) {
                return false;
            }
        }
        return true;
    }

    private static Method findAnnotatedMethod(Method m) {
        Method am = findAnnotatedMethod(m.getDeclaringClass(), m);
        return (am != null) ? am : m;
    }

    private static Method findAnnotatedMethod(Class<?> c, Method m) {
        if (c == Object.class) {
            return null;
        }

        m = AccessController.doPrivileged(ReflectionHelper.findMethodOnClassPA(c, m));
        if (m == null) {
            return null;
        }

        if (hasAnnotations(m)) {
            return m;
        }

        // Super classes take precedence over interfaces
        Class<?> sc = c.getSuperclass();
        if (sc != null && sc != Object.class) {
            Method sm = findAnnotatedMethod(sc, m);
            if (sm != null) {
                return sm;
            }
        }

        for (Class<?> ic : c.getInterfaces()) {
            Method im = findAnnotatedMethod(ic, m);
            if (im != null) {
                return im;
            }
        }

        return null;
    }

    private static boolean hasAnnotations(Method m) {
        return hasMetaMethodAnnotations(m)
                || hasMethodAnnotations(m)
                || hasParameterAnnotations(m);
    }

    private static boolean hasMetaMethodAnnotations(Method m) {
        for (Class<? extends Annotation> ac : METHOD_META_ANNOTATIONS) {
            for (Annotation a : m.getAnnotations()) {
                if (a.annotationType().getAnnotation(ac) != null) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean hasMethodAnnotations(Method m) {
        for (Class<? extends Annotation> ac : METHOD_ANNOTATIONS) {
            if (m.isAnnotationPresent(ac)) {
                return true;
            }
        }

        return false;
    }

    private static boolean hasParameterAnnotations(Method m) {
        for (Annotation[] as : m.getParameterAnnotations()) {
            for (Annotation a : as) {
                if (PARAMETER_ANNOTATIONS.contains(a.annotationType())) {
                    return true;
                }
            }
        }

        return false;
    }

    private static <T> List<T> asList(T... ts) {
        List<T> l = new ArrayList<T>();
        l.addAll(Arrays.asList(ts));
        return l;
    }
}
