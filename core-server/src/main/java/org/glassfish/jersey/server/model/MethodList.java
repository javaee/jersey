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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.glassfish.jersey.internal.util.ReflectionHelper;

/**
 * Iterable list of methods on a single class with convenience getters for
 * additional method information.
 *
 * @author Paul Sandoz
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public final class MethodList implements Iterable<AnnotatedMethod> {

    private AnnotatedMethod[] methods;

    /**
     * Create new method list for a class.
     *
     * The method list contains {@link Class#getMethods() all methods} available
     * on the class.
     *
     * The {@link Method#isBridge() bridge methods} and methods declared directly
     * on the {@link Object} class are filtered out.
     *
     * @param c class from which the method list is created.
     */
    public MethodList(Class<?> c) {
        this(c, false);
    }

    /**
     * Create new method list for a class.
     *
     * The method list contains {@link Class#getMethods() all methods} available
     * on the class or {@link Class#getDeclaredMethods() declared methods} only,
     * depending on the value of the {@code declaredMethods} parameter.
     *
     * The {@link Method#isBridge() bridge methods} and methods declared directly
     * on the {@link Object} class are filtered out.
     *
     * @param c class from which the method list is created.
     * @param declaredMethods if {@code true} only the {@link Class#getDeclaredMethods()
     *     declared methods} will be included in the method list; otherwise
     *     {@link Class#getMethods() all methods} will be listed.
     */
    public MethodList(Class<?> c, boolean declaredMethods) {
        this(declaredMethods ? getAllDeclaredMethods(c) : getMethods(c));
    }

    private static List<Method> getAllDeclaredMethods(Class<?> c) {
        List<Method> l = new ArrayList<>();
        while (c != null && c != Object.class) {
            l.addAll(AccessController.doPrivileged(ReflectionHelper.getDeclaredMethodsPA(c)));
            c = c.getSuperclass();
        }
        return l;
    }

    private static List<Method> getMethods(Class<?> c) {
        return Arrays.asList(c.getMethods());
    }

    /**
     * Create new method list from the given collection of methods.
     *
     * The {@link Method#isBridge() bridge methods} and methods declared directly
     * on the {@link Object} class are filtered out.
     *
     * @param methods methods to be included in the method list.
     */
    public MethodList(Collection<Method> methods) {
        List<AnnotatedMethod> l = new ArrayList<>(methods.size());
        for (Method m : methods) {
            if (!m.isBridge() && m.getDeclaringClass() != Object.class) {
                l.add(new AnnotatedMethod(m));
            }
        }

        this.methods = new AnnotatedMethod[l.size()];
        this.methods = l.toArray(this.methods);
    }

    /**
     * Create new method list from the given array of methods.
     *
     * The {@link Method#isBridge() bridge methods} and methods declared directly
     * on the {@link Object} class are filtered out.
     *
     * @param methods methods to be included in the method list.
     */
    public MethodList(Method... methods) {
        this(Arrays.asList(methods));
    }

    /**
     * Create new method list from the given array of {@link AnnotatedMethod
     * annotated methods}.
     *
     * @param methods methods to be included in the method list.
     */
    public MethodList(AnnotatedMethod... methods) {
        this.methods = methods;
    }

    /**
     * Iterator over the list of {@link AnnotatedMethod annotated methods} contained
     * in this method list.
     *
     * @return method list iterator.
     */
    @Override
    public Iterator<AnnotatedMethod> iterator() {
        return Arrays.asList(methods).iterator();
    }

    /**
     * Get a new sub-list of methods containing all the methods from this method
     * list that are not public.
     *
     * @return new filtered method sub-list.
     */
    public MethodList isNotPublic() {
        return filter(new Filter() {

            @Override
            public boolean keep(AnnotatedMethod m) {
                return !Modifier.isPublic(m.getMethod().getModifiers());
            }
        });
    }

    /**
     * Get a new sub-list of methods containing all the methods from this method
     * list that have the specific number of parameters.
     *
     * @param paramCount number of method parameters.
     * @return new filtered method sub-list.
     */
    public MethodList hasNumParams(final int paramCount) {
        return filter(new Filter() {

            @Override
            public boolean keep(AnnotatedMethod m) {
                return m.getParameterTypes().length == paramCount;
            }
        });
    }

    /**
     * Get a new sub-list of methods containing all the methods from this method
     * list that declare the specified return type.
     *
     * @param returnType method return type.
     * @return new filtered method sub-list.
     */
    public MethodList hasReturnType(final Class<?> returnType) {
        return filter(new Filter() {

            @Override
            public boolean keep(AnnotatedMethod m) {
                return m.getMethod().getReturnType() == returnType;
            }
        });
    }

    /**
     * Get a new sub-list of methods containing all the methods from this method
     * list with a specified method name prefix.
     *
     * @param prefix method name prefix.
     * @return new filtered method sub-list.
     */
    public MethodList nameStartsWith(final String prefix) {
        return filter(new Filter() {

            @Override
            public boolean keep(AnnotatedMethod m) {
                return m.getMethod().getName().startsWith(prefix);
            }
        });
    }

    /**
     * Get a new sub-list of methods containing all the methods from this method
     * list with a specified method-level annotation declared.
     *
     * @param <T> annotation type.
     *
     * @param annotation annotation class.
     * @return new filtered method sub-list.
     */
    public <T extends Annotation> MethodList withAnnotation(final Class<T> annotation) {
        return filter(new Filter() {

            @Override
            public boolean keep(AnnotatedMethod m) {
                return m.getAnnotation(annotation) != null;
            }
        });
    }

    /**
     * Get a new sub-list of methods containing all the methods from this method
     * list with a method-level annotation declared that is itself annotated with
     * a specified meta-annotation.
     *
     * @param <T> meta-annotation type.
     *
     * @param annotation meta-annotation class.
     * @return new filtered method sub-list.
     */
    public <T extends Annotation> MethodList withMetaAnnotation(final Class<T> annotation) {
        return filter(new Filter() {

            @Override
            public boolean keep(AnnotatedMethod m) {
                for (Annotation a : m.getAnnotations()) {
                    if (a.annotationType().getAnnotation(annotation) != null) {
                        return true;
                    }
                }
                return false;
            }
        });
    }

    /**
     * Get a new sub-list of methods containing all the methods from this method
     * list without a specified method-level annotation declared.
     *
     * @param <T> annotation type.
     *
     * @param annotation annotation class.
     * @return new filtered method sub-list.
     */
    public <T extends Annotation> MethodList withoutAnnotation(final Class<T> annotation) {
        return filter(new Filter() {

            @Override
            public boolean keep(AnnotatedMethod m) {
                return m.getAnnotation(annotation) == null;
            }
        });
    }

    /**
     * Get a new sub-list of methods containing all the methods from this method
     * list without any method-level annotation declared that would itself be
     * annotated with a specified meta-annotation.
     *
     * @param <T> meta-annotation type.
     *
     * @param annotation meta-annotation class.
     * @return new filtered method sub-list.
     */
    public <T extends Annotation> MethodList withoutMetaAnnotation(final Class<T> annotation) {
        return filter(new Filter() {

            @Override
            public boolean keep(AnnotatedMethod m) {
                for (Annotation a : m.getAnnotations()) {
                    if (a.annotationType().getAnnotation(annotation) != null) {
                        return false;
                    }
                }
                return true;
            }
        });
    }

    /**
     * Method list filter.
     *
     * @see MethodList#filter(Filter)
     */
    public interface Filter {

        /**
         * Decide whether the method should remain in the list or should be filtered
         * out.
         *
         * @param method annotated method.
         * @return {@code true} if the method should be kept in the method list,
         *     {@code false} if it should be filtered out.
         */
        boolean keep(AnnotatedMethod method);
    }

    /**
     * Created a new method list containing only the methods supported by the
     * {@link Filter method list filter}.
     *
     * @param filter method list filter.
     *
     * @return new filtered method list.
     */
    public MethodList filter(Filter filter) {
        List<AnnotatedMethod> result = new ArrayList<>();
        for (AnnotatedMethod m : methods) {
            if (filter.keep(m)) {
                result.add(m);
            }
        }
        return new MethodList(result.toArray(new AnnotatedMethod[result.size()]));
    }
}
