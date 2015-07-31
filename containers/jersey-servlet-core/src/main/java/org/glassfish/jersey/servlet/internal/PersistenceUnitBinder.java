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

package org.glassfish.jersey.servlet.internal;

import java.lang.reflect.Proxy;
import java.util.Enumeration;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import javax.servlet.ServletConfig;

import org.glassfish.jersey.server.ContainerException;

import org.glassfish.hk2.api.Injectee;
import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.TypeLiteral;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

import jersey.repackaged.com.google.common.collect.Maps;

/**
 * {@link PersistenceUnit Persistence unit} injection binder.
 *
 * @author Michal Gajdos
 */
public class PersistenceUnitBinder extends AbstractBinder {

    /**
     * Prefix of the persistence unit init param.
     */
    public static final String PERSISTENCE_UNIT_PREFIX = "unit:";

    @Singleton
    private static class PersistenceUnitInjectionResolver implements InjectionResolver<PersistenceUnit> {

        private final Map<String, String> persistenceUnits = Maps.newHashMap();

        @Inject
        private PersistenceUnitInjectionResolver(final ServiceLocator locator) {
            // Look for persistence units.
            final ServletConfig servletConfig = locator.getService(ServletConfig.class);

            for (final Enumeration parameterNames = servletConfig.getInitParameterNames(); parameterNames.hasMoreElements(); ) {
                final String key = (String) parameterNames.nextElement();

                if (key.startsWith(PERSISTENCE_UNIT_PREFIX)) {
                    persistenceUnits.put(key.substring(PERSISTENCE_UNIT_PREFIX.length()),
                            "java:comp/env/" + servletConfig.getInitParameter(key));
                }
            }
        }

        @Override
        public Object resolve(final Injectee injectee, final ServiceHandle<?> root) {
            if (!injectee.getRequiredType().equals(EntityManagerFactory.class)) {
                return null;
            }

            final PersistenceUnit annotation = injectee.getParent().getAnnotation(PersistenceUnit.class);
            final String unitName = annotation.unitName();

            if (!persistenceUnits.containsKey(unitName)) {
                throw new ContainerException(LocalizationMessages.PERSISTENCE_UNIT_NOT_CONFIGURED(unitName));
            }

            return Proxy.newProxyInstance(
                    this.getClass().getClassLoader(),
                    new Class[] {EntityManagerFactory.class},
                    new ThreadLocalNamedInvoker<EntityManagerFactory>(persistenceUnits.get(unitName)));
        }

        @Override
        public boolean isConstructorParameterIndicator() {
            return false;
        }

        @Override
        public boolean isMethodParameterIndicator() {
            return false;
        }
    }

    @Override
    protected void configure() {
        bind(PersistenceUnitInjectionResolver.class)
                .to(new TypeLiteral<InjectionResolver<PersistenceUnit>>() {})
                .in(Singleton.class);
    }
}
