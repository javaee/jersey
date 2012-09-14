/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.gf.ejb;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Providers;

import javax.ejb.EJBException;

import org.glassfish.jersey.server.internal.process.MappableException;

/**
 * Helper class to handle exceptions wrapped by the EJB container with EJBException.
 * If this mapper was not registered, no {@link WebApplicationException}
 * would end up mapped to the corresponding response.
 *
 * @author Paul Sandoz (paul.sandoz at oracle.com)
 */
public class EjbExceptionMapper implements ExceptionMapper<EJBException> {

    private final Providers providers;

    /**
     * Create new EJB exception mapper.
     *
     * @param providers JAX-RS Providers.
     */
    public EjbExceptionMapper(@Context Providers providers) {
        this.providers = providers;
    }

    @Override
    public Response toResponse(EJBException exception) {
        final Exception cause = exception.getCausedByException();
        if (cause != null) {
            final ExceptionMapper mapper = providers.getExceptionMapper(cause.getClass());
            if (mapper != null) {
                //noinspection unchecked
                return mapper.toResponse(cause);
            } else if (cause instanceof WebApplicationException) {
                return ((WebApplicationException) cause).getResponse();
            }
        }

        // Re-throw so the exception can be passed through to the
        // servlet container
        throw new MappableException((cause == null) ? exception : cause);
    }
}
