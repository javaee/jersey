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

package org.glassfish.jersey.server.internal.inject;

import javax.validation.ConstraintViolationException;
import javax.validation.Validator;

import org.glassfish.jersey.server.model.Invocable;
import org.glassfish.jersey.spi.Contract;

/**
 * Configured validator for Jersey validation purposes.
 *
 * @author Michal Gajdos
 */
@Contract
public interface ConfiguredValidator extends Validator {

    /**
     * Validates resource class instance and input parameters of the {@code method}. {@link ConstraintViolationException} raised
     * from this method should be mapped to HTTP 400 status.
     *
     * @param resource resource class instance.
     * @param resourceMethod invocable containing handling and validation methods.
     * @param args input method parameters.
     * @throws ConstraintViolationException if {@link javax.validation.ConstraintViolation} occurs (should be mapped to HTTP
     * 400 status).
     */
    void validateResourceAndInputParams(final Object resource, final Invocable resourceMethod, final Object[] args)
            throws ConstraintViolationException;

    /**
     * Validates response instance / response entity of the {@code method}. {@link ConstraintViolationException} raised
     * from this method should be mapped to HTTP 500 status.
     *
     * @param resource resource class instance.
     * @param resourceMethod invocable containing handling and validation methods.
     * @param result response entity.
     * @throws ConstraintViolationException if {@link javax.validation.ConstraintViolation} occurs (should be mapped to HTTP
     * 500 status).
     */
    void validateResult(final Object resource, final Invocable resourceMethod, final Object result)
            throws ConstraintViolationException;
}
