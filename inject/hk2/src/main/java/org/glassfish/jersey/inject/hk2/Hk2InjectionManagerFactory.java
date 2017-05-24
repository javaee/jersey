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

package org.glassfish.jersey.inject.hk2;

import java.security.AccessController;

import javax.annotation.Priority;

import org.glassfish.jersey.internal.inject.Bindings;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.InjectionManagerFactory;
import org.glassfish.jersey.internal.util.PropertiesHelper;

/**
 * Factory which is able to create {@link InjectionManager} instance using service loading and automatically initialize injection
 * manager using {@code parent} or immediately registers binder.
 */
@Priority(10)
public class Hk2InjectionManagerFactory implements InjectionManagerFactory {

    /**
     * Hk2 Injection manager strategy.
     * <p>
     * Value can be supplied only via java properties, which would typically be done using '-D' parameter,
     * for example: {@code java -Dorg.glassfish.jersey.hk2.injection.manager.strategy=delayed ...}
     * <p>
     * Valid values are "immediate" and "delayed" and values are case-insensitive.
     * <p>
     * Default value is "immediate".
     */
    public static final String HK2_INJECTION_MANAGER_STRATEGY = "org.glassfish.jersey.hk2.injection.manager.strategy";

    private enum Hk2InjectionManagerStrategy {

        /**
         * @see ImmediateHk2InjectionManager
         */
        IMMEDIATE {
            @Override
            InjectionManager createInjectionManager(final Object parent) {
                return new ImmediateHk2InjectionManager(parent);
            }
        },
        /**
         * @see DelayedHk2InjectionManager
         */
        DELAYED {
            @Override
            InjectionManager createInjectionManager(final Object parent) {
                return new DelayedHk2InjectionManager(parent);
            }
        };

        abstract InjectionManager createInjectionManager(Object parent);
    }

    @Override
    public InjectionManager create(Object parent) {
        return initInjectionManager(getStrategy().createInjectionManager(parent));
    }

    /**
     * Check HK2 Strategy property {@link #HK2_INJECTION_MANAGER_STRATEGY} and returns {@code true} if the current HK2 Strategy is
     * "immediate".
     *
     * @return {@code true} if the current HK2 Strategy is "immediate".
     */
    public static boolean isImmediateStrategy() {
        return getStrategy() == Hk2InjectionManagerStrategy.IMMEDIATE;
    }

    private static Hk2InjectionManagerStrategy getStrategy() {
        String value = AccessController.doPrivileged(PropertiesHelper.getSystemProperty(HK2_INJECTION_MANAGER_STRATEGY));
        if (value == null || value.isEmpty()) {
            return Hk2InjectionManagerStrategy.IMMEDIATE;
        }

        if ("immediate".equalsIgnoreCase(value)) {
            return Hk2InjectionManagerStrategy.IMMEDIATE;
        } else if ("delayed".equalsIgnoreCase(value)) {
            return Hk2InjectionManagerStrategy.DELAYED;
        } else {
            throw new IllegalStateException("Illegal value of " + HK2_INJECTION_MANAGER_STRATEGY
                                            + ". Expected \"immediate\" or \"delayed\", the actual value is: " + value);
        }
    }

    private InjectionManager initInjectionManager(InjectionManager injectionManager) {
        injectionManager.register(Bindings.service(injectionManager).to(InjectionManager.class));
        return injectionManager;
    }
}
