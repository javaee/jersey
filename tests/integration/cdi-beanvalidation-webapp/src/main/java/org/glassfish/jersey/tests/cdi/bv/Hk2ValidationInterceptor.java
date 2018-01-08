/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015-2017 Oracle and/or its affiliates. All rights reserved.
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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.Supplier;

import javax.ws.rs.core.Context;

import javax.enterprise.inject.Vetoed;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.validation.ConstraintViolationException;
import javax.validation.ValidationException;

import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.server.spi.ValidationInterceptor;
import org.glassfish.jersey.server.spi.ValidationInterceptorContext;

/**
 * HK2 managed validation interceptor.
 */
@Vetoed
public class Hk2ValidationInterceptor implements ValidationInterceptor {


    private final Provider<Hk2ValidationResult> validationResult;

    public Hk2ValidationInterceptor(Provider<Hk2ValidationResult> validationResult) {
        this.validationResult = validationResult;
    }

    public static class Binder extends AbstractBinder {

        @Override
        protected void configure() {
            bindFactory(ValidationInterceptorFactory.class, Singleton.class)
                    .to(ValidationInterceptor.class);
        }

    }

    private static class ValidationInterceptorFactory implements Supplier<ValidationInterceptor> {

        @Inject
        Provider<Hk2ValidationResult> validationResultProvider;

        @Override
        public ValidationInterceptor get() {
            return new Hk2ValidationInterceptor(validationResultProvider);
        }
    }

    @Override
    public void onValidate(
            ValidationInterceptorContext ctx) throws ValidationException {
        try {
            ctx.proceed();
        } catch (ConstraintViolationException ex) {
            ensureValidationResultInjected(ctx, ex);
            validationResult.get().setViolations(ex.getConstraintViolations());
        }
    }

    private void ensureValidationResultInjected(
            final ValidationInterceptorContext ctx, final ConstraintViolationException ex) {

        if (!isValidationResultInArgs(ctx.getArgs())
                && !isValidationResultInResource(ctx)
                && !hasValidationResultProperty(ctx.getResource())) {

            throw ex;
        }
    }

    private boolean isValidationResultInResource(ValidationInterceptorContext ctx) {
        Class<?> clazz = ctx.getResource().getClass();
        do {
            for (Field f : clazz.getDeclaredFields()) {
                // Of ValidationResult and JAX-RS injectable
                if (ValidationResult.class.isAssignableFrom(f.getType())
                        && f.getAnnotation(Context.class) != null) {
                    return true;
                }
            }
            clazz = clazz.getSuperclass();
        } while (clazz != Object.class);
        return false;
    }

    private boolean isValidationResultInArgs(Object[] args) {
        for (Object a : args) {
            if (a != null && ValidationResult.class.isAssignableFrom(a.getClass())) {
                return true;
            }
        }
        return false;
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
                && ValidationResult.class.isAssignableFrom(m.getReturnType())
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
        Class<?> clazz = resource.getClass();
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
                && ValidationResult.class.isAssignableFrom(m.getParameterTypes()[0])
                && m.getReturnType() == Void.TYPE && Modifier.isPublic(m.getModifiers())
                && m.getAnnotation(Context.class) != null;
    }

}
