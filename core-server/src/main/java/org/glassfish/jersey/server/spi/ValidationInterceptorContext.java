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
package org.glassfish.jersey.server.spi;

import javax.validation.ValidationException;

import org.glassfish.jersey.server.model.Invocable;

/**
 * Context for resource method validation interception processing (see {@link ValidationInterceptor}).
 * The context gives access to key validation data.
 * It also allows interceptor implementation to tweak resource and/or parameters that are going to be validated.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 * @see ValidationInterceptor
 * @since 2.18
 */
public interface ValidationInterceptorContext {

    /**
     * Provide actual resource instance that will get validated.
     *
     * @return current resource instance.
     */
    public Object getResource();

    /**
     * Setter for resource instance that should get validated.
     *
     * @param resource instance to validate
     */
    public void setResource(Object resource);

    /**
     * Provide invocable for which validation will been done.
     *
     * @return actual invocable instance.
     */
    public Invocable getInvocable();

    /**
     * Provide method parameters for which validation will be done.
     *
     * @return actual method parameters.
     */
    public Object[] getArgs();

    /**
     * Method parameters setter.
     *
     * @param args method parameters to be used for validation.
     */
    public void setArgs(Object[] args);

    /**
     * Proceed with validation.
     *
     * This method must be invoked by a validation interceptor implementation.
     *
     * @throws javax.validation.ValidationException in case the further validation processing failed with a validation error.
     */
    public void proceed() throws ValidationException;
}
