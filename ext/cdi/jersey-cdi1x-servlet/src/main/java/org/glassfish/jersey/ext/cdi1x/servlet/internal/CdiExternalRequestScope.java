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
package org.glassfish.jersey.ext.cdi1x.servlet.internal;

import javax.enterprise.context.ApplicationScoped;

import org.glassfish.jersey.ext.cdi1x.internal.JerseyVetoed;
import org.glassfish.jersey.server.spi.ExternalRequestContext;
import org.glassfish.jersey.server.spi.ExternalRequestScope;

import org.glassfish.hk2.api.ServiceLocator;

/**
 * Weld specific request scope to align CDI request context with Jersey.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
@ApplicationScoped
@JerseyVetoed
public class CdiExternalRequestScope implements ExternalRequestScope<Object> {

    public static final ThreadLocal<ServiceLocator> actualServiceLocator = new ThreadLocal<>();

    @Override
    public ExternalRequestContext<Object> open(ServiceLocator serviceLocator) {
        actualServiceLocator.set(serviceLocator);
        return new ExternalRequestContext<>(null);
    }

    @Override
    public void resume(final ExternalRequestContext<Object> ctx, ServiceLocator serviceLocator) {
        actualServiceLocator.set(serviceLocator);
    }

    @Override
    public void suspend(final ExternalRequestContext<Object> ctx, ServiceLocator serviceLocator) {
        actualServiceLocator.remove();
    }

    @Override
    public void close() {
        actualServiceLocator.remove();
    }
}
