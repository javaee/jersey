/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.hk2.api.ServiceLocator;

/**
 * {@link org.glassfish.hk2.api.ServiceLocator HK2 locator} manager designed for Jersey
 * {@link javax.enterprise.inject.spi.Extension CDI extension}. This SPI is designed to support deployments that can contain
 * more than one Jersey/HK2 managed CDI {@link org.glassfish.jersey.server.spi.ComponentProvider component provider}
 * (more HK2 service locator) but only single CDI extension instance (e.g. EAR with multiple WARs). Each CDI component provider
 * instance acknowledges the manager about new service locator and manager is supposed to return the effective service locator
 * for the current context (based on the Servlet context, for example).
 *
 * @author Michal Gajdos
 * @since 2.17
 */
public interface Hk2LocatorManager {

    /**
     * Register a new {@link org.glassfish.hk2.api.ServiceLocator service locator} with this manager.
     *
     * @param locator locator to be registered.
     */
    public void registerLocator(ServiceLocator locator);

    /**
     * Obtain the effective {@link org.glassfish.hk2.api.ServiceLocator service locator}. The implementations are supposed to
     * decide which of the registered service locators is the currently effective locator. The decision can be based, for
     * example, on current Servlet context (if the application is deployed on Servlet container).
     *
     * @return currently effective service locator.
     */
    public ServiceLocator getEffectiveLocator();
}
