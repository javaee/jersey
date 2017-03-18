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

import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.jersey.internal.BootstrapBag;
import org.glassfish.jersey.internal.BootstrapConfigurator;
import org.glassfish.jersey.internal.ServiceFinder;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.server.internal.LocalizationMessages;
import org.glassfish.jersey.server.spi.ComponentProvider;
import org.glassfish.jersey.server.spi.ExternalRequestContext;
import org.glassfish.jersey.server.spi.ExternalRequestScope;

/**
 * Configurator which initializes and register {@link ExternalRequestScope} instance into {@link InjectionManager}.
 *
 * @author Petr Bouda (petr.bouda at oracle.com)
 */
class ExternalRequestScopeConfigurator implements BootstrapConfigurator {

    private static final Logger LOGGER = Logger.getLogger(ExternalRequestScopeConfigurator.class.getName());

    @Override
    public void init(InjectionManager injectionManager, BootstrapBag bootstrapBag) {
        ServerBootstrapBag serverBag = (ServerBootstrapBag) bootstrapBag;

        Class<ExternalRequestScope>[] extScopes = ServiceFinder.find(ExternalRequestScope.class, true).toClassArray();
        boolean extScopeBound = false;

        if (extScopes.length == 1) {
            for (ComponentProvider p : serverBag.getComponentProviders().get()) {
                if (p.bind(extScopes[0], Collections.singleton(ExternalRequestScope.class))) {
                    extScopeBound = true;
                    break;
                }
            }
        } else if (extScopes.length > 1) {
            if (LOGGER.isLoggable(Level.WARNING)) {
                StringBuilder scopeList = new StringBuilder("\n");
                for (Class<ExternalRequestScope> ers : extScopes) {
                    scopeList.append("   ").append(ers.getTypeParameters()[0]).append('\n');
                }
                LOGGER.warning(LocalizationMessages.WARNING_TOO_MANY_EXTERNAL_REQ_SCOPES(scopeList.toString()));
            }
        }

        if (!extScopeBound) {
            injectionManager.register(new NoopExternalRequestScopeBinder());
        }
    }

    private static class NoopExternalRequestScopeBinder extends AbstractBinder {

        @Override
        protected void configure() {
            bind(NOOP_EXTERNAL_REQ_SCOPE).to(ExternalRequestScope.class);
        }
    }

    private static final ExternalRequestScope<Object> NOOP_EXTERNAL_REQ_SCOPE = new ExternalRequestScope<Object>() {

        @Override
        public ExternalRequestContext<Object> open(InjectionManager injectionManager) {
            return null;
        }

        @Override
        public void close() {
        }

        @Override
        public void suspend(ExternalRequestContext<Object> o, InjectionManager injectionManager) {
        }

        @Override
        public void resume(ExternalRequestContext<Object> o, InjectionManager injectionManager) {
        }
    };
}
