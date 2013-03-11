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

package org.glassfish.jersey.server.validation.internal;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import javax.ws.rs.core.Response;

import javax.validation.Configuration;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import javax.validation.executable.ExecutableType;
import javax.validation.executable.ExecutableValidator;
import javax.validation.executable.ValidateExecutable;
import javax.validation.metadata.BeanDescriptor;
import javax.validation.metadata.MethodDescriptor;

import org.glassfish.jersey.internal.util.ReflectionHelper;
import org.glassfish.jersey.server.internal.inject.ConfiguredValidator;
import org.glassfish.jersey.server.model.Invocable;

import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;

/**
 * {@link ConfiguredValidator} implementation - delegates calls to the underlying {@link Validator}.
 *
 * @author Michal Gajdos (michal.gajdos at oracle.com)
 */
public class ConfiguredValidatorImpl implements ConfiguredValidator {

    private final Validator delegate;
    private final Configuration configuration;

    private final ConcurrentMap<Method, Boolean> validateMethodCache = Maps.newConcurrentMap();

    /**
     * Create a configured validator instance.
     *
     * @param delegate validator to delegate calls to.
     * @param configuration configuration to obtain {@link ExecutableType executable types} configured in descriptor from.
     */
    ConfiguredValidatorImpl(final Validator delegate, final Configuration configuration) {
        this.delegate = delegate;
        this.configuration = configuration;
    }

    @Override
    public <T> Set<ConstraintViolation<T>> validate(final T object, final Class<?>... groups) {
        return delegate.validate(object, groups);
    }

    @Override
    public <T> Set<ConstraintViolation<T>> validateProperty(final T object, final String propertyName, final Class<?>... groups) {
        return delegate.validateProperty(object, propertyName, groups);
    }

    @Override
    public <T> Set<ConstraintViolation<T>> validateValue(final Class<T> beanType, final String propertyName,
                                                         final Object value, final Class<?>... groups) {
        return delegate.validateValue(beanType, propertyName, value, groups);
    }

    @Override
    public BeanDescriptor getConstraintsForClass(final Class<?> clazz) {
        return delegate.getConstraintsForClass(clazz);
    }

    @Override
    public <T> T unwrap(final Class<T> type) {
        return delegate.unwrap(type);
    }

    @Override
    public ExecutableValidator forExecutables() {
        return delegate.forExecutables();
    }

    @Override
    public void validateResourceAndInputParams(final Object resource, final Invocable resourceMethod, final Object[] args) {
        final Set<ConstraintViolation<Object>> constraintViolations = Sets.newHashSet();
        final BeanDescriptor beanDescriptor = getConstraintsForClass(resource.getClass());

        // Resource validation.
        if (beanDescriptor.isBeanConstrained()) {
            constraintViolations.addAll(validate(resource));
        }

        if (beanDescriptor.hasConstrainedExecutables()) {
            final Method validationMethod = resourceMethod.getValidateMethod();

            // Resource method validation - input parameters.
            final Method handlingMethod = resourceMethod.getHandlingMethod();
            final MethodDescriptor methodDescriptor = beanDescriptor.getConstraintsForMethod(validationMethod.getName(),
                    validationMethod.getParameterTypes());

            if (methodDescriptor != null
                    && methodDescriptor.areParametersConstrained()
                    && validateMethod(resource.getClass(), handlingMethod, validationMethod)) {
                constraintViolations.addAll(forExecutables().validateParameters(resource, validationMethod, args));
            }
        }

        if (!constraintViolations.isEmpty()) {
            throw new ConstraintViolationException(constraintViolations);
        }
    }

    @Override
    public void validateResult(final Object resource, final Invocable resourceMethod, final Object result) {
        final Set<ConstraintViolation<Object>> constraintViolations = Sets.newHashSet();
        final Method validationMethod = resourceMethod.getValidateMethod();

        final BeanDescriptor beanDescriptor = getConstraintsForClass(resource.getClass());

        if (beanDescriptor.hasConstrainedExecutables()) {
            final Method handlingMethod = resourceMethod.getHandlingMethod();
            final MethodDescriptor methodDescriptor = beanDescriptor.getConstraintsForMethod(validationMethod.getName(),
                    validationMethod.getParameterTypes());

            if (methodDescriptor != null
                    && methodDescriptor.isReturnValueConstrained()
                    && validateMethod(resource.getClass(), handlingMethod, validationMethod)) {
                constraintViolations.addAll(forExecutables().validateReturnValue(resource, validationMethod, result));

                if (result instanceof Response) {
                    constraintViolations.addAll(forExecutables().validateReturnValue(resource, validationMethod,
                            ((Response) result).getEntity()));
                }
            }

            if (!constraintViolations.isEmpty()) {
                throw new ConstraintViolationException(constraintViolations);
            }
        }
    }

    /**
     * Determine whether the given {@link Method method} to-be-executed on the given {@link Class clazz} should be validated.
     *
     * @param clazz class on which the method will be invoked.
     * @param method method to be examined.
     * @param validationMethod method used for cache.
     * @return {@code true} if the method should be validated, {@code false otherwise}.
     */
    private boolean validateMethod(final Class<?> clazz, final Method method, final Method validationMethod) {
        if (!validateMethodCache.containsKey(validationMethod)) {
            // Overridden methods.
            for (final Class<?> overriddenClass : getValidationClassHierarchy(clazz)) {
                final Method overriddenMethod = ReflectionHelper.findMethodOnClass(overriddenClass, method);

                if (overriddenMethod != null) {
                    // Method.
                    ExecutableType[] executableTypes = getExecutableTypes(overriddenMethod);
                    if (executableTypes != null) {
                        validateMethodCache.putIfAbsent(validationMethod, validateMethod(overriddenMethod, executableTypes));
                        break;
                    }
                    // Class.
                    executableTypes = getExecutableTypes(overriddenClass);
                    if (executableTypes != null) {
                        validateMethodCache.putIfAbsent(validationMethod, validateMethod(overriddenMethod, executableTypes));
                        break;
                    }
                }
            }

            // Return value from validation.xml.
            validateMethodCache.putIfAbsent(validationMethod, validateMethod(method, configuration.getBootstrapConfiguration()
                    .getValidatedExecutableTypes()));
        }
        return validateMethodCache.get(validationMethod);
    }

    /**
     * Determine whether the given {@link Method method} should be validated depending on the given {@code executableTypes}.
     *
     * @param method method to be examined.
     * @param executableTypes executable types assigned to the method.
     * @return {@code true} if the method should be validated, {@code false otherwise}.
     */
    private boolean validateMethod(final Method method, final ExecutableType... executableTypes) {
        return validateMethod(method, Sets.newHashSet(executableTypes));
    }

    /**
     * Determine whether the given {@link Method method} should be validated depending on the given {@code executableTypes}.
     *
     * @param method method to be examined.
     * @param executableTypes executable types assigned to the method.
     * @return {@code true} if the method should be validated, {@code false otherwise}.
     */
    private boolean validateMethod(final Method method, final Set<ExecutableType> executableTypes) {
        if (executableTypes.contains(ExecutableType.ALL)) {
            return true;
        }
        return ReflectionHelper.isGetter(method) ? executableTypes.contains(ExecutableType.GETTER_METHODS)
                : executableTypes.contains(ExecutableType.NON_GETTER_METHODS);
    }

    /**
     * Return an array of executable types contained in {@link ValidateExecutable} annotation belonging to the {@code element}.
     *
     * @param element element to be examined for {@link ValidateExecutable}.
     * @return an array of executable types or {@code null} if the element is not annotated with {@link ValidateExecutable}.
     */
    private ExecutableType[] getExecutableTypes(final AnnotatedElement element) {
        final ValidateExecutable validateExecutable = element.getAnnotation(ValidateExecutable.class);
        return validateExecutable != null ? validateExecutable.value() : null;
    }

    /**
     * Get a class hierarchy for the given {@code clazz} suitable to be looked for {@link ValidateExecutable} annotation
     * in order according to the priority defined by Bean Validation spec (superclasses, interfaces).
     *
     * @param clazz class to obtain hierarchy for.
     * @return class hierarchy.
     */
    private List<Class<?>> getValidationClassHierarchy(final Class<?> clazz) {
        final List<Class<?>> hierarchy = Lists.newArrayList();

        // Get all superclasses.
        for (Class<?> currentClass = clazz; currentClass != Object.class; currentClass = currentClass.getSuperclass()) {
            hierarchy.add(clazz);
        }

        hierarchy.addAll(getAllValidationInterfaces(clazz));
        Collections.reverse(hierarchy);

        return hierarchy;
    }

    private List<Class<?>> getAllValidationInterfaces(final Class<?> clazz) {
        final Multimap<Integer, Class<?>> map = Multimaps.newListMultimap(Maps.<Integer, Collection<Class<?>>>newTreeMap(),
                new Supplier<List<Class<?>>>() {
            @Override
            public List<Class<?>> get() {
                return Lists.newArrayList();
            }
        });

        retrieveAllValidationInterfaces(clazz, map);

        final List<Class<?>> interfaces = Lists.newArrayList(map.values());
        Collections.reverse(interfaces);

        return interfaces;
    }

    private int retrieveAllValidationInterfaces(Class<?> clazz, final Multimap<Integer, Class<?>> map) {
        if (clazz == null) {
            return 0;
        }

        int minDepth = 0;

        while (clazz != null) {
            for (final Class<?> iface : clazz.getInterfaces()) {
                int depth = retrieveAllValidationInterfaces(iface, map);

                if (!map.containsValue(iface)) {
                    map.put(depth, iface);
                }
                minDepth = minDepth > depth ? depth : minDepth;
            }

            clazz = clazz.getSuperclass();
        }

        return minDepth + 1;
    }
}
