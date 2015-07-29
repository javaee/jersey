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

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.ws.rs.core.Response;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.ElementKind;
import javax.validation.Path;

import org.glassfish.jersey.server.validation.ValidationError;

import jersey.repackaged.com.google.common.base.Function;
import jersey.repackaged.com.google.common.collect.Lists;

/**
 * Utility methods for Bean Validation processing.
 *
 * @author Michal Gajdos
 * @since 2.3
 */
public final class ValidationHelper {

    /**
     * Extract {@link ConstraintViolation constraint violations} from given exception and transform them into a list of
     * {@link ValidationError validation errors}.
     *
     * @param violation exception containing constraint violations.
     * @return list of validation errors (not {@code null}).
     */
    public static List<ValidationError> constraintViolationToValidationErrors(final ConstraintViolationException violation) {
        return Lists.transform(Lists.newArrayList(violation.getConstraintViolations()),
                new Function<ConstraintViolation<?>, ValidationError>() {

                    @Override
                    public ValidationError apply(final ConstraintViolation<?> violation) {
                        return new ValidationError(
                                violation.getMessage(),
                                violation.getMessageTemplate(),
                                getViolationPath(violation),
                                getViolationInvalidValue(violation.getInvalidValue())
                        );
                    }
                });
    }

    /**
     * Provide a string value of (invalid) value that caused the exception.
     *
     * @param invalidValue invalid value causing BV exception.
     * @return string value of given object or {@code null}.
     */
    private static String getViolationInvalidValue(final Object invalidValue) {
        if (invalidValue == null) {
            return null;
        }

        if (invalidValue.getClass().isArray()) {
            if (invalidValue instanceof Object[]) {
                return Arrays.toString((Object[]) invalidValue);
            } else if (invalidValue instanceof boolean[]) {
                return Arrays.toString((boolean[]) invalidValue);
            } else if (invalidValue instanceof byte[]) {
                return Arrays.toString((byte[]) invalidValue);
            } else if (invalidValue instanceof char[]) {
                return Arrays.toString((char[]) invalidValue);
            } else if (invalidValue instanceof double[]) {
                return Arrays.toString((double[]) invalidValue);
            } else if (invalidValue instanceof float[]) {
                return Arrays.toString((float[]) invalidValue);
            } else if (invalidValue instanceof int[]) {
                return Arrays.toString((int[]) invalidValue);
            } else if (invalidValue instanceof long[]) {
                return Arrays.toString((long[]) invalidValue);
            } else if (invalidValue instanceof short[]) {
                return Arrays.toString((short[]) invalidValue);
            }
        }

        return invalidValue.toString();
    }

    /**
     * Get a path to a field causing constraint violations.
     *
     * @param violation constraint violation.
     * @return path to a property that caused constraint violations.
     */
    private static String getViolationPath(final ConstraintViolation violation) {
        final String rootBeanName = violation.getRootBean().getClass().getSimpleName();
        final String propertyPath = violation.getPropertyPath().toString();

        return rootBeanName + (!"".equals(propertyPath) ? '.' + propertyPath : "");
    }

    /**
     * Determine the response status (400 or 500) from the given BV exception.
     *
     * @param violation BV exception.
     * @return response status (400 or 500).
     */
    public static Response.Status getResponseStatus(final ConstraintViolationException violation) {
        final Iterator<ConstraintViolation<?>> iterator = violation.getConstraintViolations().iterator();

        if (iterator.hasNext()) {
            for (final Path.Node node : iterator.next().getPropertyPath()) {
                final ElementKind kind = node.getKind();

                if (ElementKind.RETURN_VALUE.equals(kind)) {
                    return Response.Status.INTERNAL_SERVER_ERROR;
                }
            }
        }

        return Response.Status.BAD_REQUEST;
    }

    /**
     * Prevent instantiation.
     */
    private ValidationHelper() {
    }
}
