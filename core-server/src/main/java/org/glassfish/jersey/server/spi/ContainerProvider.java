/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2014 Oracle and/or its affiliates. All rights reserved.
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
import javax.ws.rs.ProcessingException;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.core.Application;

import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.spi.Contract;

/**
 * Service-provider interface for creating container instances.
 *
 * If supported by the provider, a container instance of the requested Java type
 * will be created.
 * <p>
 * The created container is responsible for listening on a communication chanel
 * for new client requests, dispatching these requests to the registered
 * {@link ApplicationHandler Jersey application handler} using the handler's
 * {@link ApplicationHandler#handle(org.glassfish.jersey.server.ContainerRequest)}
 * handle(requestContext)} method and sending the responses provided by the
 * application back to the client.
 * </p>
 * <p>
 * A provider shall support a one-to-one mapping between a type, provided the type
 * is not {@link Object}. A provider may also support mapping of sub-types of a type
 * (provided the type is not {@code Object}). It is expected that each provider
 * supports mapping for distinct set of types and subtypes so that different providers
 * do not conflict with each other.
 * </p>
 * <p>
 * An implementation can identify itself by placing a Java service provider configuration
 * file (if not already present) - {@code org.glassfish.jersey.server.spi.ContainerProvider}
 * - in the resource directory {@code META-INF/services}, and adding the fully
 * qualified service-provider-class of the implementation in the file.
 * </p>
 *
 * @author Paul Sandoz
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
@Contract
@ConstrainedTo(RuntimeType.SERVER)
public interface ContainerProvider {

    /**
     * Create an container of a given type.
     *
     * @param <T>         the type of the container.
     * @param type        the type of the container.
     * @param application JAX-RS / Jersey application.
     * @return the container, otherwise {@code null} if the provider does not support the requested {@code type}.
     *
     * @throws ProcessingException if there is an error creating the container.
     */
    public <T> T createContainer(Class<T> type, Application application) throws ProcessingException;
}
