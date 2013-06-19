/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

/**
 * This is internal Jersey SPI to hook to Jersey servlet initialization process driven by
 * {@link org.glassfish.jersey.servlet.init.JerseyServletContainerInitializer Jersey's ServletContainerInitializer}.
 *
 * @author Libor Kramolis (libor.kramolis at oracle.com)
 * @since 2.1
 */
public interface ServletContainerProvider {

    /**
     * Do your initialization job before Jersey starts its servlet initialization.
     *
     * @param servletContext the {@link ServletContext} of the web application that is being started
     * @throws ServletException if an error has occurred. {@link javax.servlet.ServletContainerInitializer#onStartup} is interrupted.
     */
    public void init(ServletContext servletContext) throws ServletException;

    /**
     * This method is called for each {@link ServletContainer} instance initialization,
     * i.e. during {@link org.glassfish.jersey.servlet.WebComponent} initialization.
     * The method is also called during {@link ServletContainer#reload()} or {@link ServletContainer#reload(ResourceConfig)}
     * methods invocation.
     *
     * It does not matter servlet container is configured in {@code web.xml} or by
     * {@link org.glassfish.jersey.servlet.init.JerseyServletContainerInitializer}.
     *
     * @param resourceConfig Jersey application configuration.
     * @throws ServletException if an error has occurred.
     */
    public void configure(ResourceConfig resourceConfig) throws ServletException;

    /**
     * Notifies the provider about all registered Jersey servlets by its names.
     * Currently it provides names of registered {@link ServletContainer} or
     * {@link org.glassfish.jersey.servlet.portability.PortableServletContainer} servlets.
     *
     * It does not matter servlet container is configured in {@code web.xml} or by
     * {@link org.glassfish.jersey.servlet.init.JerseyServletContainerInitializer}.
     *
     * @param servletNames Jersey's ServletContainer names. May be empty.
     * @throws ServletException if an error has occurred.
     */
    public void onRegister(ServletContext servletContext, String... servletNames) throws ServletException;

}
