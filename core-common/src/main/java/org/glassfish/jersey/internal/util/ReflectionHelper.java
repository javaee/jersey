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
package org.glassfish.jersey.internal.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.jersey.internal.LocalizationMessages;
import org.glassfish.jersey.internal.util.collection.ClassTypePair;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;

/**
 * Utility methods for Java reflection.
 *
 * @author Paul Sandoz
 */
public class ReflectionHelper {

    private static final Logger LOGGER = Logger.getLogger(ReflectionHelper.class.getName());

    /**
     * Get the declaring class of an accessible object. Supported are {@link Method},
     * {@link Field} and {@link Constructor} accessible object types.
     *
     * @param ao an accessible object.
     * @return the declaring class of an accessible object.
     * @throws IllegalArgumentException in case the type of the accessible object
     *                                  is not supported.
     */
    public static Class<?> getDeclaringClass(AccessibleObject ao) {
        if (ao instanceof Method) {
            return ((Method) ao).getDeclaringClass();
        } else if (ao instanceof Field) {
            return ((Field) ao).getDeclaringClass();
        } else if (ao instanceof Constructor) {
            return ((Constructor) ao).getDeclaringClass();
        } else {
            throw new IllegalArgumentException("Unsupported accessible object type: " + ao.getClass().getName());
        }
    }

    /**
     * Create a string representation of an object.
     * <p>
     * Returns a string consisting of the name of the class of which the
     * object is an instance, the at-sign character {@code '&#64;'}, and
     * the unsigned hexadecimal representation of the hash code of the
     * object. In other words, this method returns a string equal to the
     * value of:
     * <blockquote>
     * <pre>
     * o.getClass().getName() + '@' + Integer.toHexString(o.hashCode())
     * </pre></blockquote>
     *
     * @param o the object.
     * @return the string representation of the object.
     */
    public static String objectToString(Object o) {
        if (o == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(o.getClass().getName()).
                append('@').append(Integer.toHexString(o.hashCode()));
        return sb.toString();
    }

    /**
     * Create a string representation of a method and an instance whose
     * class implements the method.
     * <p>
     * Returns a string consisting of the name of the class of which the object
     * is an instance, the at-sign character {@code '&#64;'},
     * the unsigned hexadecimal representation of the hash code of the
     * object, the character {@code '.'}, the name of the method,
     * the character {@code '('}, the list of method parameters, and
     * the character {@code ')'}. In other words, those method returns a
     * string equal to the value of:
     * <blockquote>
     * <pre>
     * o.getClass().getName() + '@' + Integer.toHexString(o.hashCode()) +
     * '.' + m.getName() + '(' + &lt;parameters&gt; + ')'.
     * </pre></blockquote>
     *
     * @param o the object whose class implements {@code m}.
     * @param m the method.
     * @return the string representation of the method and instance.
     */
    public static String methodInstanceToString(Object o, Method m) {
        StringBuilder sb = new StringBuilder();
        sb.append(o.getClass().getName()).
                append('@').append(Integer.toHexString(o.hashCode())).
                append('.').append(m.getName()).append('(');

        Class[] params = m.getParameterTypes();
        for (int i = 0; i < params.length; i++) {
            sb.append(getTypeName(params[i]));
            if (i < (params.length - 1)) {
                sb.append(",");
            }
        }

        sb.append(')');

        return sb.toString();
    }

    /**
     * @param type
     * @return
     */
    private static String getTypeName(Class<?> type) {
        if (type.isArray()) {
            try {
                Class<?> cl = type;
                int dimensions = 0;
                while (cl.isArray()) {
                    dimensions++;
                    cl = cl.getComponentType();
                }
                StringBuilder sb = new StringBuilder();
                sb.append(cl.getName());
                for (int i = 0; i < dimensions; i++) {
                    sb.append("[]");
                }
                return sb.toString();
            } catch (Throwable e) { /*FALLTHRU*/ }
        }
        return type.getName();
    }

    /**
     * Get the Class from the class name.
     * <p>
     * The context class loader will be utilized if accessible and non-null.
     * Otherwise the defining class loader of this class will
     * be utilized.
     *
     * @param <T>  class type.
     * @param name the class name.
     * @return the Class, otherwise null if the class cannot be found.
     */
    public static <T> Class<T> classForName(String name) {
        return classForName(name, getContextClassLoader());
    }

    /**
     * Get the Class from the class name.
     *
     * @param <T>  class type.
     * @param name the class name.
     * @param cl   the class loader to use, if null then the defining class loader
     *             of this class will be utilized.
     * @return the Class, otherwise null if the class cannot be found.
     */
    @SuppressWarnings("unchecked")
    public static <T> Class<T> classForName(String name, ClassLoader cl) {
        if (cl != null) {
            try {
                return (Class<T>) Class.forName(name, false, cl);
            } catch (ClassNotFoundException ex) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Unable to load class " + name + " using the supplied classloader " + cl.getClass().getName() + ".", ex);
                }
            }
        }
        try {
            return (Class<T>) Class.forName(name);
        } catch (ClassNotFoundException ex) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Unable to load class " + name + " using the current classloader.", ex);
            }
        }

        return null;
    }

    /**
     * Get the Class from the class name.
     * <p>
     * The context class loader will be utilized if accessible and non-null.
     * Otherwise the defining class loader of this class will
     * be utilized.
     *
     * @param <T>  class type.
     * @param name the class name.
     * @return the Class, otherwise null if the class cannot be found.
     * @throws ClassNotFoundException if the class cannot be found.
     */
    public static <T> Class<T> classForNameWithException(String name)
            throws ClassNotFoundException {
        return classForNameWithException(name, getContextClassLoader());
    }

    /**
     * Get the Class from the class name.
     *
     * @param <T>  class type.
     * @param name the class name.
     * @param cl   the class loader to use, if null then the defining class loader
     *             of this class will be utilized.
     * @return the Class, otherwise null if the class cannot be found.
     * @throws ClassNotFoundException if the class cannot be found.
     */
    @SuppressWarnings("unchecked")
    public static <T> Class<T> classForNameWithException(String name, ClassLoader cl)
            throws ClassNotFoundException {
        if (cl != null) {
            try {
                return (Class<T>) Class.forName(name, false, cl);
            } catch (ClassNotFoundException ex) {
            }
        }
        return (Class<T>) Class.forName(name);
    }

    /**
     * Get the context class loader.
     *
     * @return the context class loader, otherwise null security privileges
     *         are not set.
     */
    public static ClassLoader getContextClassLoader() {
        return AccessController.doPrivileged(
                new PrivilegedAction<ClassLoader>() {

                    @Override
                    public ClassLoader run() {
                        ClassLoader cl = null;
                        try {
                            cl = Thread.currentThread().getContextClassLoader();
                        } catch (SecurityException ex) {
                        }
                        return cl;
                    }
                });
    }

    /**
     * Set a method to be accessible.
     *
     * @param m the method to be set as accessible
     */
    public static void setAccessibleMethod(final Method m) {
        if (Modifier.isPublic(m.getModifiers())) {
            return;
        }

        AccessController.doPrivileged(new PrivilegedAction<Object>() {

            @Override
            public Object run() {
                if (!m.isAccessible()) {
                    m.setAccessible(true);
                }
                return m;
            }
        });
    }

    /**
     * Get the list of classes that represent the type arguments of a
     * {@link ParameterizedType parameterized} input type.
     * <p />
     * For any given argument in the returned list, following rules apply:
     * <ul>
     * <li>If a type argument is a class then the class is returned.</li>
     * <li>If the type argument is a generic array type and the generic component
     * type is a class then class of the array is returned.</li>
     * <li>If the type argument is a parameterized type and it's raw type is a
     * class then that class is returned.</li>
     * </ul>
     * If the {@code type} is not an instance of ParameterizedType an empty
     * list is returned.
     *
     * @param type parameterized type.
     * @return the list of classed representing the actual type arguments. May be empty,
     *         but may never be {@code null}.
     * @throws IllegalArgumentException if any of the generic type arguments is
     *                                  not a class, or a generic array type, or the generic component type
     *                                  of the generic array type is not class, or not a parameterized type
     *                                  with a raw type that is not a class.
     */
    public static List<Class<?>> getGenericTypeArgumentClasses(final Type type) throws IllegalArgumentException {
        final Type[] types = getTypeArguments(type);
        if (types == null) {
            return Collections.emptyList();
        }

        return Lists.newArrayList(Collections2.transform(Arrays.asList(types), new Function<Type, Class<?>>() {

            @Override
            public Class<?> apply(Type input) {
                return erasure(input);
            }
        }));
    }

    /**
     * Get the list of class-type pairs that represent the type arguments of a
     * {@link ParameterizedType parameterized} input type.
     * <p />
     * For any given {@link ClassTypePair#rawClass() class} part of each pair
     * in the returned list, following rules apply:
     * <ul>
     * <li>If a type argument is a class then the class is returned as raw class.</li>
     * <li>If the type argument is a generic array type and the generic component
     * type is a class then class of the array is returned as raw class.</li>
     * <li>If the type argument is a parameterized type and it's raw type is a
     * class then that class is returned as raw class.</li>
     * </ul>
     * If the {@code type} is not an instance of ParameterizedType an empty
     * list is returned.
     *
     * @param type parameterized type.
     * @return the list of class-type pairs representing the actual type arguments.
     *         May be empty, but may never be {@code null}.
     * @throws IllegalArgumentException if any of the generic type arguments is
     *                                  not a class, or a generic array type, or the generic component type
     *                                  of the generic array type is not class, or not a parameterized type
     *                                  with a raw type that is not a class.
     */
    public static List<ClassTypePair> getTypeArgumentAndClass(final Type type) throws IllegalArgumentException {
        final Type[] types = getTypeArguments(type);
        if (types == null) {
            return Collections.emptyList();
        }

        return Lists.newArrayList(Collections2.transform(Arrays.asList(types), new Function<Type, ClassTypePair>() {

            @Override
            public ClassTypePair apply(Type input) {
                return ClassTypePair.of(erasure(input), input);
            }
        }));
    }

    /**
     * Check if the given type is a primitive type.
     *
     * @param type type to be checked.
     */
    public static boolean isPrimitive(Type type) {
        if (type instanceof Class) {
            Class c = (Class) type;
            return c.isPrimitive();
        }
        return false;
    }

    /**
     * Get the type arguments for a parameterized type.
     *
     * In case the type is not a {@link ParameterizedType parameterized type},
     * the method returns {@code null}.
     *
     * @param type parameterized type.
     * @return type arguments for a parameterized type, or {@code null} in case the input type is
     *         not a parameterized type.
     */
    public static Type[] getTypeArguments(Type type) {
        if (!(type instanceof ParameterizedType)) {
            return null;
        }

        return ((ParameterizedType) type).getActualTypeArguments();
    }

    /**
     * Get a type argument at particular index for a parameterized type.
     *
     * In case the type is not a {@link ParameterizedType parameterized type},
     * the method returns {@code null}.
     *
     * @param type  parameterized type.
     * @param index type parameter index.
     * @return type argument for a parameterized type at a given index, or {@code null}
     *         in case the input type is not a parameterized type.
     */
    public static Type getTypeArgument(Type type, int index) {
        if (type instanceof ParameterizedType) {
            ParameterizedType p = (ParameterizedType) type;
            return fix(p.getActualTypeArguments()[index]);
        }

        return null;
    }

    /**
     * JDK 5.0 has a bug of creating {@link GenericArrayType} where it shouldn't.
     * fix that manually to work around the problem.
     *
     * See bug 6202725.
     */
    private static Type fix(Type t) {
        if (!(t instanceof GenericArrayType))
            return t;

        GenericArrayType gat = (GenericArrayType) t;
        if (gat.getGenericComponentType() instanceof Class) {
            Class c = (Class) gat.getGenericComponentType();
            return Array.newInstance(c, 0).getClass();
        }

        return t;
    }

    /**
     * Implements the logic for {@link #erasure(Type)}.
     */
    private static final TypeVisitor<Class> eraser = new TypeVisitor<Class>() {
        @Override
        protected Class onClass(Class clazz) {
            return clazz;
        }

        @Override
        protected Class onParameterizedType(ParameterizedType type) {
            // TODO: why getRawType returns Type? not Class?
            return visit(type.getRawType());
        }

        @Override
        protected Class onGenericArray(GenericArrayType type) {
            return Array.newInstance(visit(type.getGenericComponentType()), 0).getClass();
        }

        @Override
        protected Class onVariable(TypeVariable type) {
            return visit(type.getBounds()[0]);
        }

        @Override
        protected Class onWildcard(WildcardType type) {
            return visit(type.getUpperBounds()[0]);
        }

        @Override
        protected RuntimeException createError(Type type) {
            return new IllegalArgumentException(LocalizationMessages.TYPE_TO_CLASS_CONVERSION_NOT_SUPPORTED(type));
        }
    };

    /**
     * Get the {@link Class} representation of the given type.
     *
     * This corresponds to the notion of the erasure in JSR-14.
     *
     * @param type type to provide the erasure for.
     * @return the given type's erasure.
     */
    @SuppressWarnings("unchecked")
    public static <T> Class<T> erasure(Type type) {
        return eraser.visit(type);
    }

    /**
     * Check if {@code subType} is a sub-type of {@code superType}.
     *
     * @param subType   sub-type type.
     * @param superType super-type type.
     * @return {@code true} in case the {@code subType} is a sub-type of {@code superType},
     *         {@code false} otherwise.
     */
    public static boolean isSubClassOf(Type subType, Type superType) {
        return erasure(superType).isAssignableFrom(erasure(subType));
    }

    /**
     * Checks if the type is an array type.
     *
     * @param type type to check.
     * @return {@code true} in case the type is an array type, {@code false} otherwise.
     */
    public static boolean isArray(Type type) {
        if (type instanceof Class) {
            Class c = (Class) type;
            return c.isArray();
        }
        return type instanceof GenericArrayType;
    }

    /**
     * Checks if the type is an array of a given component type.
     *
     * @param type          type to check.
     * @param componentType array component type.
     * @return {@code true} in case the type is an array type of a given component type,
     *         {@code false} otherwise.
     */
    public static boolean isArrayOfType(Type type, Class<?> componentType) {
        if (type instanceof Class) {
            Class c = (Class) type;
            return c.isArray() && c != byte[].class;
        }
        if (type instanceof GenericArrayType) {
            final Type arrayComponentType = ((GenericArrayType) type).getGenericComponentType();
            return arrayComponentType == componentType;
        }
        return false;
    }

    /**
     * Gets the component type of the array.
     *
     * @param type must be an array.
     * @return array component type.
     * @throws IllegalArgumentException in case the type is not an array type.
     */
    public static Type getArrayComponentType(Type type) {
        if (type instanceof Class) {
            Class c = (Class) type;
            return c.getComponentType();
        }
        if (type instanceof GenericArrayType)
            return ((GenericArrayType) type).getGenericComponentType();

        throw new IllegalArgumentException();
    }

    /**
     * Get Array class of component type.
     *
     * @param c the component class of the array
     * @return the array class.
     */
    public static Class<?> getArrayForComponentType(Class<?> c) {
        try {
            Object o = Array.newInstance(c, 0);
            return o.getClass();
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Get the static valueOf(String ) method.
     *
     * @param c The class to obtain the method.
     * @return the method, otherwise null if the method is not present.
     */
    @SuppressWarnings("unchecked")
    public static Method getValueOfStringMethod(Class<?> c) {
        try {
            Method m = c.getDeclaredMethod("valueOf", String.class);
            if (!Modifier.isStatic(m.getModifiers()) && m.getReturnType() == c) {
                return null;
            }
            return m;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get the static fromString(String ) method.
     *
     * @param c The class to obtain the method.
     * @return the method, otherwise null if the method is not present.
     */
    @SuppressWarnings("unchecked")
    public static Method getFromStringStringMethod(Class<?> c) {
        try {
            Method m = c.getDeclaredMethod("fromString", String.class);
            if (!Modifier.isStatic(m.getModifiers()) && m.getReturnType() == c) {
                return null;
            }
            return m;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get the constructor that has a single parameter of String.
     *
     * @param c The class to obtain the constructor.
     * @return the constructor, otherwise null if the constructor is not present.
     */
    public static Constructor getStringConstructor(Class<?> c) {
        try {
            return c.getConstructor(String.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns collection of all annotation types attached to a given annotated element that have the provided meta
     * annotation attached.
     *
     * @param annotatedElement annotated element.
     * @param metaAnnotation   meta annotation attached to the annotation types we are looking for (if null, annotation
     *                         types of all attached annotations will be returned).
     * @return list of annotation types with a given meta annotation
     */
    public static Collection<Class<? extends Annotation>> getAnnotationTypes(
            AnnotatedElement annotatedElement,
            Class<? extends Annotation> metaAnnotation
    ) {
        HashSet<Class<? extends Annotation>> result = new HashSet<Class<? extends Annotation>>();
        for (Annotation a : annotatedElement.getAnnotations()) {
            Class<? extends Annotation> aType = a.annotationType();
            if (metaAnnotation == null || aType.getAnnotation(metaAnnotation) != null) {
                result.add(aType);
            }
        }
        return result;
    }

    /**
     * A tuple consisting of a concrete class and a declaring class that declares a
     * generic interface type.
     */
    public static class DeclaringClassInterfacePair {

        /**
         * Concrete class.
         */
        public final Class<?> concreteClass;
        /**
         * Declaring class.
         */
        public final Class<?> declaringClass;
        /**
         * Generic interface type.
         */
        public final Type genericInterface;

        private DeclaringClassInterfacePair(Class<?> concreteClass, Class<?> declaringClass, Type genericInteface) {
            this.concreteClass = concreteClass;
            this.declaringClass = declaringClass;
            this.genericInterface = genericInteface;
        }
    }

    /**
     * Get the parameterized class arguments for a declaring class that
     * declares a generic interface type.
     *
     * @param p the declaring class
     * @return the parameterized class arguments, or null if the generic
     *         interface type is not a parameterized type.
     */
    public static Class[] getParameterizedClassArguments(DeclaringClassInterfacePair p) {
        if (p.genericInterface instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) p.genericInterface;
            Type[] as = pt.getActualTypeArguments();
            Class[] cas = new Class[as.length];

            for (int i = 0; i < as.length; i++) {
                Type a = as[i];
                if (a instanceof Class) {
                    cas[i] = (Class) a;
                } else if (a instanceof ParameterizedType) {
                    pt = (ParameterizedType) a;
                    cas[i] = (Class) pt.getRawType();
                } else if (a instanceof TypeVariable) {
                    ClassTypePair ctp = resolveTypeVariable(p.concreteClass, p.declaringClass, (TypeVariable) a);
                    cas[i] = (ctp != null) ? ctp.rawClass() : Object.class;
                }
            }
            return cas;
        } else {
            return null;
        }
    }

    /**
     * Get the parameterized type arguments for a declaring class that
     * declares a generic interface type.
     *
     * @param p the declaring class
     * @return the parameterized type arguments, or null if the generic
     *         interface type is not a parameterized type.
     */
    public static Type[] getParameterizedTypeArguments(DeclaringClassInterfacePair p) {
        if (p.genericInterface instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) p.genericInterface;
            Type[] as = pt.getActualTypeArguments();
            Type[] ras = new Type[as.length];

            for (int i = 0; i < as.length; i++) {
                Type a = as[i];
                if (a instanceof Class) {
                    ras[i] = a;
                } else if (a instanceof ParameterizedType) {
                    ras[i] = a;
                } else if (a instanceof TypeVariable) {
                    ClassTypePair ctp = resolveTypeVariable(p.concreteClass, p.declaringClass, (TypeVariable) a);
                    ras[i] = ctp.type();
                }
            }
            return ras;
        } else {
            return null;
        }
    }

    /**
     * Find the declaring class that implements or extends an interface.
     *
     * @param concrete the concrete class than directly or indirectly
     *                 implements or extends an interface class.
     * @param iface    the interface class.
     * @return the tuple of the declaring class and the generic interface
     *         type.
     */
    public static DeclaringClassInterfacePair getClass(Class<?> concrete, Class<?> iface) {
        return getClass(concrete, iface, concrete);
    }

    private static DeclaringClassInterfacePair getClass(Class<?> concrete, Class<?> iface, Class<?> c) {
        Type[] gis = c.getGenericInterfaces();
        DeclaringClassInterfacePair p = getType(concrete, iface, c, gis);
        if (p != null) {
            return p;
        }

        c = c.getSuperclass();
        if (c == null || c == Object.class) {
            return null;
        }

        return getClass(concrete, iface, c);
    }

    private static DeclaringClassInterfacePair getType(Class<?> concrete, Class<?> iface, Class<?> c, Type[] ts) {
        for (Type t : ts) {
            DeclaringClassInterfacePair p = getType(concrete, iface, c, t);
            if (p != null) {
                return p;
            }
        }
        return null;
    }

    private static DeclaringClassInterfacePair getType(Class<?> concrete, Class<?> iface, Class<?> c, Type t) {
        if (t instanceof Class) {
            if (t == iface) {
                return new DeclaringClassInterfacePair(concrete, c, t);
            } else {
                return getClass(concrete, iface, (Class<?>) t);
            }
        } else if (t instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) t;
            if (pt.getRawType() == iface) {
                return new DeclaringClassInterfacePair(concrete, c, t);
            } else {
                return getClass(concrete, iface, (Class<?>) pt.getRawType());
            }
        }
        return null;
    }

    /**
     * Resolve generic type parameter(s) of a raw class and it's generic type
     * based on the class that declares the generic type parameter(s) to be resolved
     * and a concrete implementation of the declaring class.
     *
     * @param concreteClass       concrete implementation of the declaring class.
     * @param declaringClass      class declaring the generic type parameter(s) to be
     *                            resolved.
     * @param rawResolvedType     raw class of the generic type to be resolved.
     * @param genericResolvedType generic type information of th type to be resolved.
     * @return a pair of class and the generic type values with the the resolved
     *         generic parameter types.
     */
    public static ClassTypePair resolveGenericType(final Class concreteClass, final Class declaringClass,
                                                   final Class rawResolvedType, final Type genericResolvedType) {
        if (genericResolvedType instanceof TypeVariable) {
            ClassTypePair ct = resolveTypeVariable(
                    concreteClass,
                    declaringClass,
                    (TypeVariable) genericResolvedType);

            if (ct != null) {
                return ct;
            }
        } else if (genericResolvedType instanceof ParameterizedType) {
            final ParameterizedType pt = (ParameterizedType) genericResolvedType;
            final Type[] ptts = pt.getActualTypeArguments();
            boolean modified = false;
            for (int i = 0; i < ptts.length; i++) {
                ClassTypePair ct =
                        resolveGenericType(concreteClass, declaringClass, (Class) pt.getRawType(), ptts[i]);
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
        } else if (genericResolvedType instanceof GenericArrayType) {
            GenericArrayType gat = (GenericArrayType) genericResolvedType;
            final ClassTypePair ct =
                    resolveGenericType(concreteClass, declaringClass, null, gat.getGenericComponentType());
            if (gat.getGenericComponentType() != ct.type()) {
                try {
                    Class ac = ReflectionHelper.getArrayForComponentType(ct.rawClass());
                    return ClassTypePair.of(ac);
                } catch (Exception e) {
                    LOGGER.log(Level.FINEST, "", e);
                }
            }
        }

        return ClassTypePair.of(rawResolvedType, genericResolvedType);
    }

    /**
     * Given a type variable resolve the Java class of that variable.
     *
     * @param c  the concrete class from which all type variables are resolved.
     * @param dc the declaring class where the type variable was defined.
     * @param tv the type variable.
     * @return the resolved Java class and type, otherwise null if the type variable
     *         could not be resolved.
     */
    public static ClassTypePair resolveTypeVariable(Class<?> c, Class<?> dc, TypeVariable tv) {
        return resolveTypeVariable(c, dc, tv, new HashMap<TypeVariable, Type>());
    }

    private static ClassTypePair resolveTypeVariable(Class<?> c, Class<?> dc, TypeVariable tv,
                                                     Map<TypeVariable, Type> map) {
        Type[] gis = c.getGenericInterfaces();
        for (Type gi : gis) {
            if (gi instanceof ParameterizedType) {
                // process pt of interface
                ParameterizedType pt = (ParameterizedType) gi;
                ClassTypePair ctp = resolveTypeVariable(pt, (Class<?>) pt.getRawType(), dc, tv, map);
                if (ctp != null) {
                    return ctp;
                }
            }
        }

        Type gsc = c.getGenericSuperclass();
        if (gsc instanceof ParameterizedType) {
            // process pt of class
            ParameterizedType pt = (ParameterizedType) gsc;
            return resolveTypeVariable(pt, c.getSuperclass(), dc, tv, map);
        } else if (gsc instanceof Class) {
            return resolveTypeVariable(c.getSuperclass(), dc, tv, map);
        }
        return null;
    }

    private static ClassTypePair resolveTypeVariable(ParameterizedType pt, Class<?> c, Class<?> dc, TypeVariable tv,
                                                     Map<TypeVariable, Type> map) {
        Type[] typeArguments = pt.getActualTypeArguments();

        TypeVariable[] typeParameters = c.getTypeParameters();

        Map<TypeVariable, Type> submap = new HashMap<TypeVariable, Type>();
        for (int i = 0; i < typeArguments.length; i++) {
            // Substitute a type variable with the Java class
            if (typeArguments[i] instanceof TypeVariable) {
                Type t = map.get((TypeVariable) typeArguments[i]);
                submap.put(typeParameters[i], t);
            } else {
                submap.put(typeParameters[i], typeArguments[i]);
            }
        }

        if (c == dc) {
            Type t = submap.get(tv);
            if (t instanceof Class) {
                return ClassTypePair.of((Class) t);
            } else if (t instanceof GenericArrayType) {
                GenericArrayType gat = (GenericArrayType) t;
                t = gat.getGenericComponentType();
                if (t instanceof Class) {
                    c = (Class<?>) t;
                    try {
                        return ClassTypePair.of(getArrayForComponentType(c));
                    } catch (Exception e) {
                    }
                    return null;
                } else if (t instanceof ParameterizedType) {
                    Type rt = ((ParameterizedType) t).getRawType();
                    if (rt instanceof Class) {
                        c = (Class<?>) rt;
                    } else {
                        return null;
                    }
                    try {
                        return ClassTypePair.of(getArrayForComponentType(c), gat);
                    } catch (Exception e) {
                        return null;
                    }
                } else {
                    return null;
                }
            } else if (t instanceof ParameterizedType) {
                pt = (ParameterizedType) t;
                if (pt.getRawType() instanceof Class) {
                    return ClassTypePair.of((Class<?>) pt.getRawType(), pt);
                } else {
                    return null;
                }
            } else {
                return null;
            }
        } else {
            return resolveTypeVariable(c, dc, tv, submap);
        }
    }

    /**
     * Find a method on a class given an existing method.
     * <p>
     * If there exists a public method on the class that has the same name
     * and parameters as the existing method then that public method is
     * returned.
     * <p>
     * Otherwise, if there exists a public method on the class that has
     * the same name and the same number of parameters as the existing method,
     * and each generic parameter type, in order, of the public method is equal
     * to the generic parameter type, in the same order, of the existing method
     * or is an instance of {@link TypeVariable} then that public method is
     * returned.
     *
     * @param c the class to search for a public method
     * @param m the method to find
     * @return the found public method.
     */
    public static Method findMethodOnClass(Class<?> c, Method m) {
        try {
            return c.getMethod(m.getName(), m.getParameterTypes());
        } catch (NoSuchMethodException ex) {
            for (Method _m : c.getMethods()) {
                if (_m.getName().equals(m.getName())
                        && _m.getParameterTypes().length == m.getParameterTypes().length) {
                    if (compareParameterTypes(m.getGenericParameterTypes(),
                            _m.getGenericParameterTypes())) {
                        return _m;
                    }
                }
            }
        }
        return null;
    }

    private static boolean compareParameterTypes(Type[] ts, Type[] _ts) {
        for (int i = 0; i < ts.length; i++) {
            if (!ts[i].equals(_ts[i])) {
                if (!(_ts[i] instanceof TypeVariable)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Prevents instantiation.
     */
    private ReflectionHelper() {
    }
}
