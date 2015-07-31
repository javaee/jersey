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

package org.glassfish.jersey.server.validation.internal;

import java.lang.annotation.ElementType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.util.concurrent.ConcurrentMap;

import javax.validation.Path;
import javax.validation.TraversableResolver;

import org.glassfish.jersey.internal.util.ReflectionHelper;

import jersey.repackaged.com.google.common.collect.Maps;

/**
 * {@link TraversableResolver Traversable resolver} used for handling {@link javax.validation.executable.ValidateOnExecution}
 * annotations present on property getters when validating resource class.
 *
 * @author Michal Gajdos
 */
class ValidateOnExecutionTraversableResolver implements TraversableResolver {

    private final TraversableResolver delegate;

    private final ConcurrentMap<String, Method> propertyToMethod = Maps.newConcurrentMap();

    private final ValidateOnExecutionHandler validateOnExecutionHandler;

    private final boolean validateExecutable;

    /**
     * Create a new {@link ValidateOnExecutionTraversableResolver}.
     *
     * @param delegate delegate requests to this underlying traversable resolver if this one cannot resolve it.
     * @param validateOnExecutionHandler handler to determine whether a getter should be validated or not.
     * @param validateExecutable bootstrap flag to enable/disable global validation of executables.
     */
    public ValidateOnExecutionTraversableResolver(final TraversableResolver delegate,
                                                  final ValidateOnExecutionHandler validateOnExecutionHandler,
                                                  final boolean validateExecutable) {
        this.delegate = delegate;
        this.validateExecutable = validateExecutable;
        this.validateOnExecutionHandler = validateOnExecutionHandler;
    }

    @Override
    public boolean isReachable(final Object traversableObject,
                               final Path.Node traversableProperty,
                               final Class<?> rootBeanType,
                               final Path pathToTraversableObject,
                               final ElementType elementType) {
        // Make sure only getters on entities are validated (not getters on resource classes).
        final Class<?> traversableObjectClass = traversableObject.getClass();
        final boolean isEntity = !rootBeanType.equals(traversableObjectClass);

        if (isEntity && validateExecutable && ElementType.METHOD.equals(elementType)) {
            final String propertyName = traversableProperty.getName();
            final String propertyKey = traversableObjectClass.getName() + "#" + propertyName;

            if (!propertyToMethod.containsKey(propertyKey)) {
                final Method getter = getGetterMethod(traversableObjectClass, propertyName);

                if (getter != null) {
                    propertyToMethod.putIfAbsent(propertyKey, getter);
                }
            }

            final Method getter = propertyToMethod.get(propertyKey);
            return getter != null && validateOnExecutionHandler.validateGetter(traversableObjectClass, getter);
        }

        return delegate.isReachable(traversableObject, traversableProperty, rootBeanType, pathToTraversableObject, elementType);
    }

    @Override
    public boolean isCascadable(final Object traversableObject,
                                final Path.Node traversableProperty,
                                final Class<?> rootBeanType,
                                final Path pathToTraversableObject,
                                final ElementType elementType) {
        return delegate.isCascadable(traversableObject, traversableProperty, rootBeanType, pathToTraversableObject, elementType);
    }

    /**
     * Return getter method defined on {@code clazz} of property with given {@code propertyName}.
     *
     * @param clazz class to find a getter method on.
     * @param propertyName name of the property to find a getter for.
     * @return getter method or {@code null} if the method cannot be found.
     */
    private Method getGetterMethod(final Class<?> clazz, final String propertyName) {
        // Property type.
        Class<?> propertyType = null;
        for (final Field field : AccessController.doPrivileged(ReflectionHelper.getAllFieldsPA(clazz))) {
            if (field.getName().equals(propertyName)) {
                propertyType = field.getType();
            }
        }

        final char[] chars = propertyName.toCharArray();
        chars[0] = Character.toUpperCase(chars[0]);
        final String getterPropertyName = new String(chars);

        final String isGetter = "is" + getterPropertyName;
        final String getGetter = "get" + getterPropertyName;

        for (final Method method : AccessController.doPrivileged(ReflectionHelper.getMethodsPA(clazz))) {
            final String methodName = method.getName();

            if ((methodName.equals(isGetter) || methodName.equals(getGetter))
                    && ReflectionHelper.isGetter(method)
                    && (propertyType == null || propertyType.isAssignableFrom(method.getReturnType()))) {
                return AccessController.doPrivileged(ReflectionHelper.findMethodOnClassPA(clazz, method));
            }
        }

        return null;
    }
}
