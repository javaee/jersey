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

package org.glassfish.jersey.ext.cdi1x.transaction.internal;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.transaction.TransactionalException;

import org.glassfish.jersey.ext.cdi1x.internal.JerseyVetoed;
import org.glassfish.jersey.spi.ExceptionMappers;
import org.glassfish.jersey.spi.ExtendedExceptionMapper;

/**
 * Helper class to handle exceptions thrown by JTA layer. If this mapper was not
 * registered, no {@link WebApplicationException} thrown from a transactional
 * CDI bean would get properly mapped to corresponding response.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
@ApplicationScoped
@JerseyVetoed
public class TransactionalExceptionMapper implements ExtendedExceptionMapper<TransactionalException> {

    @Inject
    @TransactionalExceptionInterceptorProvider.WaeQualifier
    private WebAppExceptionHolder waeHolder;

    @Inject
    private BeanManager beanManager;

    @Inject
    private Provider<ExceptionMappers> mappers;

    @Override
    public Response toResponse(TransactionalException exception) {
        final ExceptionMapper mapper = mappers.get().findMapping(exception);

        if (mapper != null && !TransactionalExceptionMapper.class.isAssignableFrom(mapper.getClass())) {
            return mapper.toResponse(exception);
        } else {
            if (waeHolder != null) {
                final WebApplicationException wae = waeHolder.getException();
                if (wae != null) {
                    return wae.getResponse();
                }
            }
            throw exception;
        }
    }

    @Override
    public boolean isMappable(TransactionalException exception) {
        return true;
    }
}
