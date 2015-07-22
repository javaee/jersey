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
package org.glassfish.jersey.server.validation.internal;

import java.util.Iterator;

import javax.validation.ValidationException;

import org.glassfish.jersey.server.model.Invocable;
import org.glassfish.jersey.server.spi.ValidationInterceptor;
import org.glassfish.jersey.server.spi.ValidationInterceptorContext;

/**
 * Validation executor for resource method validation processing. It is intended for a one-off usage
 * when the executor instance serves also as a {@link org.glassfish.jersey.server.spi.ValidationInterceptorContext}.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
final class ValidationInterceptorExecutor implements ValidationInterceptorContext {

    private Object resource;
    private Object[] args;
    private final Invocable invocable;

    private final Iterator<ValidationInterceptor> iterator;

    /**
     * Create a one-off validation executor for given resource, invocable and parameter and given
     * interceptors.
     *
     * @param resource  actual resource instance to get validated
     * @param invocable resource method
     * @param args      actual resource method parameters
     * @param iterator  validator interceptors to be involved
     */
    public ValidationInterceptorExecutor(
            final Object resource,
            final Invocable invocable,
            final Object[] args,
            final Iterator<ValidationInterceptor> iterator) {

        this.resource = resource;
        this.invocable = invocable;
        this.args = args;
        this.iterator = iterator;
    }

    @Override
    public Object getResource() {
        return resource;
    }

    @Override
    public void setResource(final Object resource) {
        this.resource = resource;
    }

    @Override
    public Invocable getInvocable() {
        return invocable;
    }

    @Override
    public Object[] getArgs() {
        return args;
    }

    @Override
    public void setArgs(final Object[] args) {
        this.args = args;
    }

    @Override
    public void proceed() throws ValidationException {
        final ValidationInterceptor validationInterceptor = iterator.next();
        validationInterceptor.onValidate(this);
    }
}
