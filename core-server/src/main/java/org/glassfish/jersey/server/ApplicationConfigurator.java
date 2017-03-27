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

import java.util.Collection;
import java.util.Collections;

import javax.ws.rs.core.Application;

import javax.inject.Singleton;

import org.glassfish.jersey.internal.BootstrapBag;
import org.glassfish.jersey.internal.BootstrapConfigurator;
import org.glassfish.jersey.internal.inject.Bindings;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.util.collection.Value;
import org.glassfish.jersey.server.spi.ComponentProvider;

/**
 * Configurator which initializes and register {@link Application} instance into {@link InjectionManager} and
 * {@link BootstrapBag}.
 *
 * @author Petr Bouda (petr.bouda at oracle.com)
 */
class ApplicationConfigurator implements BootstrapConfigurator {

    private Application application;
    private Class<? extends Application> applicationClass;

    /**
     * Initialize {@link Application} from provided instance.
     *
     * @param application application instance.
     */
    ApplicationConfigurator(Application application) {
        this.application = application;
    }

    /**
     * Initialize {@link Application} from provided class.
     *
     * @param applicationClass application class.
     */
    ApplicationConfigurator(Class<? extends Application> applicationClass) {
        this.applicationClass = applicationClass;
    }

    @Override
    public void init(InjectionManager injectionManager, BootstrapBag bootstrapBag) {
        ServerBootstrapBag serverBag = (ServerBootstrapBag) bootstrapBag;
        Application resultApplication;

        // ApplicationConfigurer was created with an Application instance.
        if (application != null) {
            if (application instanceof ResourceConfig) {
                ResourceConfig rc = (ResourceConfig) application;
                if (rc.getApplicationClass() != null) {
                    rc.setApplication(createApplication(
                            injectionManager, rc.getApplicationClass(), serverBag.getComponentProviders()));
                }
            }
            resultApplication = application;

        // ApplicationConfigurer was created with an Application class.
        } else {
            resultApplication = createApplication(injectionManager, applicationClass, serverBag.getComponentProviders());
        }

        serverBag.setApplication(resultApplication);
        injectionManager.register(Bindings.service(resultApplication).to(Application.class));
    }

    private static Application createApplication(
            InjectionManager injectionManager,
            Class<? extends Application> applicationClass,
            Value<Collection<ComponentProvider>> componentProvidersValue) {
        // need to handle ResourceConfig and Application separately as invoking forContract() on these
        // will trigger the factories which we don't want at this point
        if (applicationClass == ResourceConfig.class) {
            return new ResourceConfig();
        } else if (applicationClass == Application.class) {
            return new Application();
        } else {
            Collection<ComponentProvider> componentProviders = componentProvidersValue.get();
            boolean appClassBound = false;
            for (ComponentProvider cp : componentProviders) {
                if (cp.bind(applicationClass, Collections.emptySet())) {
                    appClassBound = true;
                    break;
                }
            }
            if (!appClassBound) {
                if (applicationClass.isAnnotationPresent(Singleton.class)) {
                    injectionManager.register(Bindings.serviceAsContract(applicationClass).in(Singleton.class));
                    appClassBound = true;
                }
            }
            final Application app = appClassBound
                    ? injectionManager.getInstance(applicationClass)
                    : injectionManager.createAndInitialize(applicationClass);
            if (app instanceof ResourceConfig) {
                final ResourceConfig _rc = (ResourceConfig) app;
                final Class<? extends Application> innerAppClass = _rc.getApplicationClass();
                if (innerAppClass != null) {
                    Application innerApp = createApplication(injectionManager, innerAppClass, componentProvidersValue);
                    _rc.setApplication(innerApp);
                }
            }
            return app;
        }
    }
}
