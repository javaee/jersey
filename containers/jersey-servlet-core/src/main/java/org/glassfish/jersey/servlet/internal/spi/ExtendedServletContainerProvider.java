/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015-2017 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.servlet.internal.spi;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.spi.RequestScopedInitializer;

/**
 * Implementations could provide their own {@link HttpServletRequest} and {@link HttpServletResponse}
 * binding implementation in HK2 locator and also an implementation of {@link RequestScopedInitializer}
 * that is used to set actual request/response references in injection manager within each request.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 * @since 2.21
 */
public interface ExtendedServletContainerProvider extends ServletContainerProvider {

    /**
     * Give me a {@link RequestScopedInitializerProvider} instance, that will be utilized
     * at runtime to set the actual HTTP Servlet request and response.
     *
     * The provider returned will be used at runtime for every and each incoming request
     * so that the actual request/response instances could be made accessible
     * from Jersey injection manager.
     *
     * @return request scoped initializer provider.
     */
    public RequestScopedInitializerProvider getRequestScopedInitializerProvider();

    /**
     * Used by Jersey runtime to tell if the extension covers HTTP Servlet request response
     * handling with respect to underlying injection manager.
     *
     * Return {@code true}, if your implementation configures HK2 bindings
     * for {@link HttpServletRequest} and {@link HttpServletResponse}
     * in {@link #configure(ResourceConfig)} method
     * and also provides a {@link RequestScopedInitializer} implementation
     * via {@link #getRequestScopedInitializerProvider()}.
     *
     * @return {@code true} if the extension fully covers HTTP request/response handling.
     */
    public boolean bindsServletRequestResponse();
}
