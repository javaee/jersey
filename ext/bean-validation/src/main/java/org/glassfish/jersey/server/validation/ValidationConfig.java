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

package org.glassfish.jersey.server.validation;

import javax.validation.ConstraintValidatorFactory;
import javax.validation.MessageInterpolator;
import javax.validation.ParameterNameProvider;
import javax.validation.TraversableResolver;

/**
 * Configuration class for Bean Validation provider.
 *
 * @author Michal Gajdos
 */
public final class ValidationConfig {

    private MessageInterpolator messageInterpolator;
    private TraversableResolver traversableResolver;
    private ConstraintValidatorFactory constraintValidatorFactory;
    private ParameterNameProvider parameterNameProvider;

    /**
     * Return {@code MessageInterpolator} implementation used for configuration.
     *
     * @return instance of {@code MessageInterpolator} or {@code null} if not defined.
     */
    public MessageInterpolator getMessageInterpolator() {
        return messageInterpolator;
    }

    /**
     * Return {@code TraversableResolver} implementation used for configuration.
     *
     * @return instance of {@code TraversableResolver} or {@code null} if not defined.
     */
    public TraversableResolver getTraversableResolver() {
        return traversableResolver;
    }

    /**
     * Return {@code ConstraintValidatorFactory} implementation used for configuration.
     *
     * @return instance of {@code ConstraintValidatorFactory} or {@code null} if not defined.
     */
    public ConstraintValidatorFactory getConstraintValidatorFactory() {
        return constraintValidatorFactory;
    }

    /**
     * Return {@code ParameterNameProvider} implementation used for configuration.
     *
     * @return instance of {@code ParameterNameProvider} or {@code null} if not defined.
     */
    public ParameterNameProvider getParameterNameProvider() {
        return parameterNameProvider;
    }

    /**
     * Defines the message interpolator.
     * If {@code null} is passed, the default message interpolator is used.
     *
     * @param messageInterpolator message interpolator implementation.
     */
    public ValidationConfig messageInterpolator(final MessageInterpolator messageInterpolator) {
        this.messageInterpolator = messageInterpolator;
        return this;
    }

    /**
     * Defines the traversable resolver.
     * If {@code null} is passed, the default traversable resolver is used.
     *
     * @param traversableResolver traversable resolver implementation.
     */
    public ValidationConfig traversableResolver(final TraversableResolver traversableResolver) {
        this.traversableResolver = traversableResolver;
        return this;
    }

    /**
     * Defines the constraint validator factory.
     * If {@code null} is passed, the default constraint validator factory is used.
     *
     * @param constraintValidatorFactory constraint factory implementation.
     */
    public ValidationConfig constraintValidatorFactory(final ConstraintValidatorFactory constraintValidatorFactory) {
        this.constraintValidatorFactory = constraintValidatorFactory;
        return this;
    }

    /**
     * Defines the parameter name provider.
     * If {@code null} is passed, the default parameter name provider is used.
     *
     * @param parameterNameProvider parameter name provider implementation.
     */
    public ValidationConfig parameterNameProvider(final ParameterNameProvider parameterNameProvider) {
        this.parameterNameProvider = parameterNameProvider;
        return this;
    }
}
