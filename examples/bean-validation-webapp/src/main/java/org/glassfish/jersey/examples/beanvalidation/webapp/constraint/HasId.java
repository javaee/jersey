/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package org.glassfish.jersey.examples.beanvalidation.webapp.constraint;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

import org.glassfish.jersey.examples.beanvalidation.webapp.domain.ContactCard;

/**
 * Checks whether a given {@link org.glassfish.jersey.examples.beanvalidation.webapp.domain.ContactCard} entity has ID.
 * Only return values are supposed to be annotated with this annotation.
 *
 * @author Michal Gajdos
 */
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {HasId.Validator.class, HasId.ListValidator.class})
public @interface HasId {

    String message() default "{org.glassfish.jersey.examples.beanvalidation.webapp.constraint.HasId.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    public class Validator implements ConstraintValidator<HasId, ContactCard>  {

        @Override
        public void initialize(final HasId hasId) {
        }

        @Override
        public boolean isValid(final ContactCard contact, final ConstraintValidatorContext constraintValidatorContext) {
            return contact == null || contact.getId() != null;
        }
    }

    public class ListValidator implements ConstraintValidator<HasId, List<ContactCard>>  {

        private Validator validator = new Validator();

        @Override
        public void initialize(final HasId hasId) {
        }

        @Override
        public boolean isValid(final List<ContactCard> contacts, final ConstraintValidatorContext constraintValidatorContext) {
            boolean isValid = true;
            for (final ContactCard contactCard : contacts) {
                isValid &= validator.isValid(contactCard, constraintValidatorContext);
            }
            return isValid;
        }
    }
}
