/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2015 Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.Response;

import javax.validation.Configuration;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import javax.validation.executable.ExecutableType;
import javax.validation.executable.ExecutableValidator;
import javax.validation.metadata.BeanDescriptor;
import javax.validation.metadata.MethodDescriptor;

import org.glassfish.jersey.server.internal.inject.ConfiguredValidator;
import org.glassfish.jersey.server.model.Invocable;
import org.glassfish.jersey.server.spi.ValidationInterceptor;
import org.glassfish.jersey.server.spi.ValidationInterceptorContext;

/**
 * Default {@link ConfiguredValidator} implementation - delegates calls to the underlying {@link Validator}.
 *
 * @author Michal Gajdos
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
class DefaultConfiguredValidator implements ConfiguredValidator, ValidationInterceptor {

    private final Validator delegate;
    private final Configuration configuration;
    private final ValidateOnExecutionHandler validateOnExecutionHandler;
    private final List<ValidationInterceptor> interceptors;

    /**
     * Create a configured validator instance.
     *
     * @param delegate                   validator to delegate calls to.
     * @param configuration              configuration to obtain {@link ExecutableType executable types} configured in descriptor
     *                                   from.
     * @param validateOnExecutionHandler handler for processing {@link javax.validation.executable.ValidateOnExecution}
     *                                   annotations.
     * @param interceptors               custom validation interceptors.
     */
    DefaultConfiguredValidator(final Validator delegate, final Configuration configuration,
                               final ValidateOnExecutionHandler validateOnExecutionHandler,
                               final Iterable<ValidationInterceptor> interceptors) {
        this.delegate = delegate;
        this.configuration = configuration;
        this.validateOnExecutionHandler = validateOnExecutionHandler;
        this.interceptors = createInterceptorList(interceptors);
    }

    private List<ValidationInterceptor> createInterceptorList(Iterable<ValidationInterceptor> interceptors) {
        List<ValidationInterceptor> result = new LinkedList<>();
        for (ValidationInterceptor i : interceptors) {
            result.add(i);
        }
        result.add(this);
        return result;
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

        ValidationInterceptorExecutor validationExecutor = new ValidationInterceptorExecutor(
                resource,
                resourceMethod,
                args,
                interceptors.iterator());

        validationExecutor.proceed();
    }

    // Invoked as the last validation interceptor method in the chain.
    @Override
    public void onValidate(final ValidationInterceptorContext ctx) {

        final Object resource = ctx.getResource();
        final Invocable resourceMethod = ctx.getInvocable();
        final Object[] args = ctx.getArgs();

        final Set<ConstraintViolation<Object>> constraintViolations = new HashSet<>();
        final BeanDescriptor beanDescriptor = getConstraintsForClass(resource.getClass());

        // Resource validation.
        if (beanDescriptor.isBeanConstrained()) {
            constraintViolations.addAll(validate(resource));
        }

        if (resourceMethod != null
                && configuration.getBootstrapConfiguration().isExecutableValidationEnabled()) {
            final Method handlingMethod = resourceMethod.getHandlingMethod();

            // Resource method validation - input parameters.
            final MethodDescriptor methodDescriptor = beanDescriptor.getConstraintsForMethod(handlingMethod.getName(),
                    handlingMethod.getParameterTypes());

            if (methodDescriptor != null
                    && methodDescriptor.hasConstrainedParameters()) {
                constraintViolations.addAll(forExecutables().validateParameters(resource, handlingMethod, args));
            }
        }

        if (!constraintViolations.isEmpty()) {
            throw new ConstraintViolationException(constraintViolations);
        }
    }

    @Override
    public void validateResult(final Object resource, final Invocable resourceMethod, final Object result) {
        if (configuration.getBootstrapConfiguration().isExecutableValidationEnabled()) {
            final Set<ConstraintViolation<Object>> constraintViolations = new HashSet<>();
            final Method handlingMethod = resourceMethod.getHandlingMethod();

            final BeanDescriptor beanDescriptor = getConstraintsForClass(resource.getClass());
            final MethodDescriptor methodDescriptor = beanDescriptor.getConstraintsForMethod(handlingMethod.getName(),
                    handlingMethod.getParameterTypes());

            final Method definitionMethod = resourceMethod.getDefinitionMethod();

            if (methodDescriptor != null
                    && methodDescriptor.hasConstrainedReturnValue()
                    && validateOnExecutionHandler.validateMethod(resource.getClass(), definitionMethod, handlingMethod)) {
                constraintViolations.addAll(forExecutables().validateReturnValue(resource, handlingMethod, result));

                if (result instanceof Response) {
                    constraintViolations.addAll(forExecutables().validateReturnValue(resource, handlingMethod,
                            ((Response) result).getEntity()));
                }
            }

            if (!constraintViolations.isEmpty()) {
                throw new ConstraintViolationException(constraintViolations);
            }
        }
    }
}
