/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.e2e.server.validation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.List;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

/**
 * @author Michal Gajdos
 */
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = FieldPropertyValidation.Validator.class)
public @interface FieldPropertyValidation {

    String message() default "one or more fields are not valid";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    String[] elements() default {};

    public class Validator implements ConstraintValidator<FieldPropertyValidation, FieldPropertyValidationResource.SubResource> {

        private List<String> properties;

        @Override
        public void initialize(final FieldPropertyValidation annotation) {
            this.properties = Arrays.asList(annotation.elements());
        }

        @Override
        public boolean isValid(final FieldPropertyValidationResource.SubResource bean,
                               final ConstraintValidatorContext constraintValidatorContext) {
            boolean result = true;

            for (final String property : properties) {
                if ("fieldAndClass".equals(property)) {
                    result &= bean.fieldAndClass != null;
                } else if ("propertyAndClass".equals(property)) {
                    result &= bean.getPropertyAndClass() != null;
                } else if ("propertyGetterAndClass".equals(property)) {
                    result &= bean.getPropertyGetterAndClass() != null;
                }
            }

            return result;
        }
    }
}
