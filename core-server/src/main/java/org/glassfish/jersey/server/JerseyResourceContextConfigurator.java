/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Oracle and/or its affiliates. All rights reserved.
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

import java.util.function.Consumer;
import java.util.function.Function;

import javax.ws.rs.container.ResourceContext;

import org.glassfish.jersey.internal.BootstrapBag;
import org.glassfish.jersey.internal.BootstrapConfigurator;
import org.glassfish.jersey.internal.inject.Binding;
import org.glassfish.jersey.internal.inject.Bindings;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.Injections;
import org.glassfish.jersey.internal.inject.InstanceBinding;
import org.glassfish.jersey.server.internal.JerseyResourceContext;

/**
 * Configurator which initializes and register {@link JerseyResourceContext} instance into {@link InjectionManager} and
 * {@link BootstrapBag}.
 *
 * @author Petr Bouda (petr.bouda at oracle.com)
 */
class JerseyResourceContextConfigurator implements BootstrapConfigurator {

    @Override
    public void init(InjectionManager injectionManager, BootstrapBag bootstrapBag) {
        ServerBootstrapBag serverBag = (ServerBootstrapBag) bootstrapBag;
        Consumer<Binding> registerBinding = injectionManager::register;
        Function<Class<?>, ?> getOrCreateInstance = clazz -> Injections.getOrCreate(injectionManager, clazz);
        Consumer<Object> injectInstance = injectionManager::inject;

        // Initialize and register Resource Context
        JerseyResourceContext resourceContext = new JerseyResourceContext(getOrCreateInstance, injectInstance, registerBinding);
        InstanceBinding<JerseyResourceContext> resourceContextBinding =
                Bindings.service(resourceContext)
                        .to(ResourceContext.class)
                        .to(ExtendedResourceContext.class);
        injectionManager.register(resourceContextBinding);
        serverBag.setResourceContext(resourceContext);
    }

    @Override
    public void postInit(InjectionManager injectionManager, BootstrapBag bootstrapBag) {
    }
}
