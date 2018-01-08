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

package org.glassfish.jersey.ext.cdi1x.internal.spi;

import org.glassfish.jersey.internal.inject.InjectionManager;

/**
 * {@link InjectionManager injection manager} designed for Jersey
 * {@link javax.enterprise.inject.spi.Extension CDI extension}. This SPI is designed to support deployments that can contain
 * more than one Jersey/InjectionManager managed CDI {@link org.glassfish.jersey.server.spi.ComponentProvider component provider}
 * (more injection manager) but only single CDI extension instance (e.g. EAR with multiple WARs). Each CDI component provider
 * instance acknowledges the manager about new injection manager and manager is supposed to return the effective injection manager
 * for the current context (based on the Servlet context, for example).
 *
 * @author Michal Gajdos
 * @since 2.17
 */
public interface InjectionManagerStore {

    /**
     * Register a new {@link InjectionManager injection manager} with this manager.
     *
     * @param injectionManager injection manager to be registered.
     */
    public void registerInjectionManager(InjectionManager injectionManager);

    /**
     * Obtain the effective {@link InjectionManager injection manager}. The implementations are supposed to
     * decide which of the registered injection managers is the currently effective locator. The decision can be based, for
     * example, on current Servlet context (if the application is deployed on Servlet container).
     *
     * @return currently effective injection manager.
     */
    public InjectionManager getEffectiveInjectionManager();
}
