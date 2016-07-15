/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2016 Oracle and/or its affiliates. All rights reserved.
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

import java.util.Set;

import org.glassfish.hk2.api.ServiceLocator;

/**
 * Component provider interface to allow custom management of 3rd party
 * components life-cycle and dependency injection.
 * <p />
 * An implementation (a component-provider) identifies itself by placing a provider-configuration
 * file (if not already present), {@code org.glassfish.jersey.server.spi.ComponentProvider}
 * in the resource directory <tt>META-INF/services</tt>, and adding the fully
 * qualified service-provider-class of the implementation in the file.
 *
 * Jersey will not even try to inject component provider instances with Jersey artifacts.
 * The SPI providers should be designed so that no dependency injection is needed at the bind time phase.

 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public interface ComponentProvider {


    /**
     * Initializes the component provider with a reference to a HK2 service locator
     * instance, which will get used in the application to manage individual components.
     * Providers should keep a reference to the HK2 service locator for later use.
     * This method will be invoked prior to any bind method calls.
     * The service locator parameter will not be fully initialized at the time of invocation
     * and should be used as a reference only.
     *
     * @param locator an HK2 service locator.
     */
    void initialize(final ServiceLocator locator);

    /**
     * Jersey will invoke this method before binding of each component class internally
     * during initialization of it's HK2 service locator.
     *
     * If the component provider wants to bind the component class
     * itself, it must do so and return true. In that case, Jersey will not
     * bind the component and rely on the component provider in this regard.
     *
     * @param component a component (resource/provider) class.
     * @param providerContracts provider contracts implemented by given component.
     * @return true if the component class has been bound by the provider, false otherwise
     */
    boolean bind(final Class<?> component, Set<Class<?>> providerContracts);

    /**
     * Jersey will invoke this method after all component classes have been bound.
     *
     * If the component provider wants to do some actions after it has seen all component classes
     * registered with the application, this is the right place for the corresponding code.
     */
    void done();
}
