/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.tests.cdi.bv;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;

import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.validation.ConstraintViolation;

/**
 * Helper class to implement support for {@code javax.mvc.validation.ValidationResult}.
 *
 * @author Santiago Pericas-Geertsen
 */
public final class ValidationResultUtil {

    private static final String VALIDATION_RESULT = ValidationResult.class.getName();

    private ValidationResultUtil() {
        throw new AssertionError("Instantiation not allowed.");
    }

    /**
     * Search for a {@code javax.mvc.validation.ValidationResult} field in the resource's
     * class hierarchy. Field must be annotated with {@link javax.inject.Inject}.
     *
     * @param resource resource instance.
     * @return field or {@code null} if none is found.
     */
    public static Field getValidationResultField(final Object resource) {
        Class<?> clazz = resource.getClass();
        do {
            for (Field f : clazz.getDeclaredFields()) {
                // Of ValidationResult and CDI injectable
                if (f.getType().getName().equals(VALIDATION_RESULT)
                        && f.getAnnotation(Inject.class) != null) {
                    return f;
                }
            }
            clazz = clazz.getSuperclass();
        } while (clazz != Object.class);
        return null;
    }

    /**
     * Updates a {@code javax.mvc.validation.ValidationResult} field. In pseudo-code:
     * <p/>
     * resource.field.setViolations(constraints)
     *
     * @param resource    resource instance.
     * @param field       field to be updated.
     * @param constraints new set of constraints.
     */
    public static void updateValidationResultField(Object resource, Field field,
                                                   Set<ConstraintViolation<?>> constraints) {
        try {
            field.setAccessible(true);
            final Object obj = field.get(resource);
            Method setter;
            try {
                setter = obj.getClass().getMethod("setViolations", Set.class);
            } catch (NoSuchMethodException e) {
                setter = obj.getClass().getSuperclass().getMethod("setViolations", Set.class);
            }
            setter.invoke(obj, constraints);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            // ignore for now
            System.out.println("Damn it...");
        } catch (Throwable t) {
            System.out.println("What the heck...");
        }
    }

    /**
     * Updates a {@code javax.mvc.validation.ValidationResult} property. In pseudo-code:
     * <p/>
     * obj = getter.invoke(resource);
     * obj.setViolations(constraints);
     * setter.invoke(resource, obj);
     *
     * @param resource    resource instance.
     * @param getter      getter to be used.
     * @param constraints new set of constraints.
     */
    public static void updateValidationResultProperty(Object resource, Method getter,
                                                      Set<ConstraintViolation<?>> constraints) {
        try {
            final Object obj = getter.invoke(resource);
            Method setViolations;
            try {
                setViolations = obj.getClass().getMethod("setViolations", Set.class);
            } catch (NoSuchMethodException e) {
                setViolations = obj.getClass().getSuperclass().getMethod("setViolations", Set.class);
            }
            setViolations.invoke(obj, constraints);

            final Method setter = getValidationResultSetter(resource);

            if (setter != null) {
                setter.invoke(resource, obj);
            }
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            // ignore for now
        }
    }

    /**
     * Determines if a resource has a property of type {@code javax.mvc.validation.ValidationResult}.
     *
     * @param resource resource instance.
     * @return outcome of test.
     */
    public static boolean hasValidationResultProperty(final Object resource) {
        return getValidationResultGetter(resource) != null && getValidationResultSetter(resource) != null;
    }

    /**
     * Returns a getter for {@code javax.mvc.validation.ValidationResult} or {@code null}
     * if one cannot be found.
     *
     * @param resource resource instance.
     * @return getter or {@code null} if not available.
     */
    public static Method getValidationResultGetter(final Object resource) {
        Class<?> clazz = resource.getClass();
        do {
            for (Method m : clazz.getDeclaredMethods()) {
                if (isValidationResultGetter(m)) {
                    return m;
                }
            }
            clazz = clazz.getSuperclass();
        } while (clazz != Object.class);
        return null;
    }

    /**
     * Determines if a method is a getter for {@code javax.mvc.validation.ValidationResult}.
     *
     * @param m method to test.
     * @return outcome of test.
     */
    private static boolean isValidationResultGetter(Method m) {
        return m.getName().startsWith("get")
                && m.getReturnType().getName().equals(VALIDATION_RESULT)
                && Modifier.isPublic(m.getModifiers()) && m.getParameterTypes().length == 0;
    }

    /**
     * Returns a setter for {@code javax.mvc.validation.ValidationResult} or {@code null}
     * if one cannot be found.
     *
     * @param resource resource instance.
     * @return setter or {@code null} if not available.
     */
    public static Method getValidationResultSetter(final Object resource) {
        return getValidationResultSetter(resource.getClass());
    }

    private static Method getValidationResultSetter(final Class<?> resourceClass) {
        Class<?> clazz = resourceClass;
        do {
            for (Method m : clazz.getDeclaredMethods()) {
                if (isValidationResultSetter(m)) {
                    return m;
                }
            }
            clazz = clazz.getSuperclass();
        } while (clazz != Object.class);
        return null;
    }

    /**
     * Determines if a method is a setter for {@code javax.mvc.validation.ValidationResult}.
     * As a CDI initializer method, it must be annotated with {@link javax.inject.Inject}.
     *
     * @param m method to test.
     * @return outcome of test.
     */
    private static boolean isValidationResultSetter(Method m) {
        return m.getName().startsWith("set") && m.getParameterTypes().length == 1
                && m.getParameterTypes()[0].getName().equals(VALIDATION_RESULT)
                && m.getReturnType() == Void.TYPE && Modifier.isPublic(m.getModifiers())
                && m.getAnnotation(Inject.class) != null;
    }
}
