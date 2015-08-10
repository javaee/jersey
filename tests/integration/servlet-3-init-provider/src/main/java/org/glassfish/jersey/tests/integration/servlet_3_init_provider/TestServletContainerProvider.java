/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.tests.integration.servlet_3_init_provider;

import java.util.EnumSet;
import java.util.Set;

import javax.servlet.DispatcherType;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.internal.spi.RequestScopedInitializerProvider;
import org.glassfish.jersey.servlet.internal.spi.ServletContainerProvider;

/**
 * This is just test purpose implementation of Jersey internal SPI {@link ServletContainerProvider}.
 *
 * @author Libor Kramolis (libor.kramolis at oracle.com)
 */
public class TestServletContainerProvider implements ServletContainerProvider {

    public static final String TEST_FILTER = "TestFilter";

    private static Set<String> SERVLET_NAMES;
    private static boolean immutableServletNames = false;

    @Override
    public void preInit(final ServletContext servletContext, final Set<Class<?>> classes) throws ServletException {
        classes.add(AbstractHelloWorldResource.class);
    }

    @Override
    public void postInit(final ServletContext servletContext, final Set<Class<?>> classes, final Set<String> servletNames)
            throws ServletException {
        try {
            servletNames.add("TEST");
        } catch (final UnsupportedOperationException ex) {
            TestServletContainerProvider.setImmutableServletNames(true);
        }
    }

    @Override
    public void onRegister(final ServletContext servletContext, final Set<String> servletNames) throws ServletException {
        TestServletContainerProvider.setServletNames(servletNames);

        servletContext.addFilter(TEST_FILTER, TestFilter.class)
                .addMappingForServletNames(EnumSet.allOf(DispatcherType.class), false,
                        servletNames.toArray(new String[servletNames.size()]));
    }

    @Override
    public void configure(final ResourceConfig resourceConfig) throws ServletException {
        if (!resourceConfig.isRegistered(TestContainerLifecycleListener.class)) {
            resourceConfig.register(TestContainerLifecycleListener.class);
        }
    }

    public static Set<String> getServletNames() {
        return SERVLET_NAMES;
    }

    public static boolean isImmutableServletNames() {
        return immutableServletNames;
    }

    private static void setServletNames(final Set<String> servletNames) {
        TestServletContainerProvider.SERVLET_NAMES = servletNames;
    }

    public static void setImmutableServletNames(final boolean immutableServletNames) {
        TestServletContainerProvider.immutableServletNames = immutableServletNames;
    }
}
