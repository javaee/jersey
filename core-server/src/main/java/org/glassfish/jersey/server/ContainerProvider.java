/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.server;

import org.glassfish.jersey.internal.ProcessingException;

/**
 * Service-provider interface for creating container instances.
 * <p>
 * A container instance will be created according to the
 * the supporting generic type of the container.
 * <p>
 * A provider shall support a one-to-one mapping between a type that is not of
 * the type Object. A provider may support
 * more than one one-to-one mapping or a mapping of sub-types of a type
 * (that is not of the type Object). A provider shall not conflict with other
 * providers.
 * <p>
 * An implementation (a service-provider) identifies itself by placing a
 * provider-configuration file (if not already present),
 * "com.sun.jersey.spi.container.ContainerProvider" in the
 * resource directory <tt>META-INF/services</tt>, and including the fully qualified
 * service-provider-class of the implementation in the file.
 *
 * @param <T> the type of the container.
 *
 * @author Paul Sandoz
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public interface ContainerProvider<T> {

    /**
     * Create an container of type T.
     * <p>
     * The container provider SHOULD NOT initiate the Web application. The container
     * provider MAY modify the resource configuration.
     * <p>
     * @return the container, otherwise null if the provider does not support
     *         the requested <code>type</code>.
     * @param type the type of the container.
     * @param application the Web application the container delegates to for
     *         the handling of HTTP requests.
     * @throws ProcessingException if there is an error creating the container.
     */
    T createContainer(Class<T> type, // TODO: add config parameter(s) (ResourceConfig)
            Application application)
            throws ProcessingException;
}