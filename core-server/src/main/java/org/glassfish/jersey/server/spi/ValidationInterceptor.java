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

import javax.validation.ConstraintViolationException;
import javax.validation.ValidationException;

import org.glassfish.jersey.spi.Contract;

/**
 * Interceptor for method validation processing.
 *
 * Allows to override the default Jersey behaviour. By default, the Jersey runtime throws a
 * {@link javax.validation.ValidationException} or one of its subclasses,
 * that gets mapped to a HTTP 400 response, if any validation issues occur. In such case
 * the actual resource method does not get invoked at all.
 * <p>
 * Validation interceptor implementation allows to e.g. swallow the {@link ConstraintViolationException}
 * and handle the validation issues in the resource method. It is also possible to tweak
 * validated components. This could be utilized in case of proxied resources,
 * when field validation is not possible on a dynamic proxy, and the validator requires
 * the original delegated instance.
 * </p>
 * <p>
 * Each validation interceptor implementation must invoke proceed
 * method on provided interceptor context as part of interception processing.
 * </p>
 *
 * @author Jakub Podlesak (jakub.podleak at oracle.com)
 * @since 2.18
 */
@Contract
public interface ValidationInterceptor {

    /**
     * Used to intercept resource method validation processing.
     * <p/>
     * To allow further validation processing, every and each implementation
     * must invoke {@link ValidationInterceptorContext#proceed()} method.
     *
     * @param context method validation context.
     * @throws ValidationException in case the validation exception should be thrown as a result of the validation processing.
     */
    public void onValidate(ValidationInterceptorContext context) throws ValidationException;
}
