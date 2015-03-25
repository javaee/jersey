/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved.
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

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.ConstraintViolationException;
import javax.validation.ValidationException;

import org.glassfish.jersey.server.spi.ValidationInterceptor;
import org.glassfish.jersey.server.spi.ValidationInterceptorContext;

import org.jboss.weld.interceptor.util.proxy.TargetInstanceProxy;

/**
 * CDI backed interceptor to handle validation issues.
 *
 * @author Jaku Podlesak (jakub.podlesak at oracle.com)
 */
@ApplicationScoped
public class CdiValidationInterceptor implements ValidationInterceptor {

    private final CdiValidationResult validationResult;
    private final CdiPropertyInjectedResource pir;

    /**
     * Empty constructor to make CDI happy.
     */
    @SuppressWarnings("UnusedDeclaration")
    public CdiValidationInterceptor() {
        this.validationResult = null;
        this.pir = null;
    }

    /**
     * Injection constructor.
     *
     * @param validationResult  CDI implementation of validation result.
     * @param resource CDI property-injected JAX-RS resource.
     */
    @Inject
    public CdiValidationInterceptor(CdiValidationResult validationResult, CdiPropertyInjectedResource resource) {
        this.validationResult = validationResult;
        this.pir = resource;
    }

    @Override
    public void onValidate(ValidationInterceptorContext ctx) throws ValidationException {

        final Object resource = ctx.getResource();
        if (resource instanceof TargetInstanceProxy) {
            ctx.setResource(((TargetInstanceProxy) resource).getTargetInstance());
        }

        try {
            ctx.proceed();
        } catch (ConstraintViolationException constraintViolationException) {

            // First check for a property
            if (ValidationResultUtil.hasValidationResultProperty(resource)) {
                final Method validationResultGetter = ValidationResultUtil.getValidationResultGetter(resource);
                ValidationResultUtil.updateValidationResultProperty(resource, validationResultGetter,
                        constraintViolationException.getConstraintViolations());
                pir.setValidationResult(validationResult);
            } else {
                // Then check for a field
                final Field vr = ValidationResultUtil.getValidationResultField(resource);
                if (vr != null) {
                    // we have the right guy, no need to use reflection:
                    validationResult.setViolations(constraintViolationException.getConstraintViolations());
                } else {
                    if (isValidationResultInArgs(ctx.getArgs())) {
                        this.validationResult.setViolations(constraintViolationException.getConstraintViolations());
                    } else {
                        throw constraintViolationException;
                    }
                }
            }
        }
    }

    private boolean isValidationResultInArgs(Object[] args) {
        for (Object a : args) {
            if (a != null) {
                Class<?> argClass = a.getClass();
                do {
                    if (ValidationResult.class.isAssignableFrom(argClass)) {
                        return true;
                    }
                    argClass = argClass.getSuperclass();
                } while (argClass != Object.class);
            }
        }
        return false;
    }
}
