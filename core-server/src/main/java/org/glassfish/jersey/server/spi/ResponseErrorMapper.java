/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2015 Oracle and/or its affiliates. All rights reserved.
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

import javax.ws.rs.ConstrainedTo;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.spi.Contract;

/**
 * Contract for a provider that maps response processing errors to {@link javax.ws.rs.core.Response}.
 * <p>
 * Providers implementing {@code ResponseErrorMapper} contract must be either programmatically registered in a JAX-RS runtime or
 * must be annotated with {@link javax.ws.rs.ext.Provider &#64;Provider} annotation to be automatically discovered by the JAX-RS
 * runtime during a provider scanning phase.
 * </p>
 * <p>
 * {@link org.glassfish.jersey.server.ServerProperties#PROCESSING_RESPONSE_ERRORS_ENABLED} property has to be enabled in order to
 * use this contract.
 * </p>
 *
 * @author Michal Gajdos
 * @see javax.ws.rs.ext.Provider
 * @see javax.ws.rs.core.Response
 * @since 2.8
 */
@Contract
@ConstrainedTo(RuntimeType.SERVER)
public interface ResponseErrorMapper {

    public Response toResponse(Throwable throwable);
}
