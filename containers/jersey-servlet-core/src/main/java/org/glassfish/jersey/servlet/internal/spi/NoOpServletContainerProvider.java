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

package org.glassfish.jersey.servlet.internal.spi;

import java.lang.reflect.Type;
import java.util.Set;

import javax.ws.rs.core.GenericType;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.glassfish.jersey.internal.util.collection.Ref;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * Basic {@link ExtendedServletContainerProvider} that provides
 * dummy no-op method implementation. It should be convenient to extend if you only need to implement
 * a subset of the original SPI methods.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public class NoOpServletContainerProvider implements ExtendedServletContainerProvider {

    public final Type HTTP_SERVLET_REQUEST_TYPE = (new GenericType<Ref<HttpServletRequest>>() { }).getType();
    public final Type HTTP_SERVLET_RESPONSE_TYPE = (new GenericType<Ref<HttpServletResponse>>() { }).getType();

    @Override
    public void preInit(final ServletContext servletContext, final Set<Class<?>> classes) throws ServletException {
        // no-op
    }

    @Override
    public void postInit(
            final ServletContext servletContext, final Set<Class<?>> classes, final Set<String> servletNames) {
        // no-op
    }

    @Override
    public void onRegister(
            final ServletContext servletContext, final Set<String> servletNames) throws ServletException {
        // no-op
    }

    @Override
    public void configure(final ResourceConfig resourceConfig) throws ServletException {
        // no-op
    }

    @Override
    public boolean bindsServletRequestResponse() {
        return false;
    }

    @Override
    public RequestScopedInitializerProvider getRequestScopedInitializerProvider() {
        return null;
    }
}
