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

package org.glassfish.jersey.model.internal;

import java.util.Map;

import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.FeatureContext;

import org.glassfish.jersey.internal.inject.ServiceLocatorSupplier;

import org.glassfish.hk2.api.ServiceLocator;

/**
 * Wrapper of {@link javax.ws.rs.core.FeatureContext} that can supply instance of
 * {@link org.glassfish.hk2.api.ServiceLocator service locator}.
 *
 * @author Miroslav Fuksa
 */
public class FeatureContextWrapper implements FeatureContext, ServiceLocatorSupplier {

    private final FeatureContext context;
    private final ServiceLocator serviceLocator;

    /**
     * Create a new instance of wrapper.
     *
     * @param context Feature context instance that should be wrapped.
     * @param serviceLocator Service locator.
     */
    public FeatureContextWrapper(FeatureContext context, ServiceLocator serviceLocator) {
        this.context = context;
        this.serviceLocator = serviceLocator;
    }

    @Override
    public Configuration getConfiguration() {
        return context.getConfiguration();
    }

    @Override
    public FeatureContext property(String name, Object value) {
        return context.property(name, value);
    }

    @Override
    public FeatureContext register(Class<?> componentClass) {
        return context.register(componentClass);
    }

    @Override
    public FeatureContext register(Class<?> componentClass, int priority) {
        return context.register(componentClass, priority);
    }

    @Override
    public FeatureContext register(Class<?> componentClass, Class<?>... contracts) {
        return context.register(componentClass, contracts);
    }

    @Override
    public FeatureContext register(Class<?> componentClass, Map<Class<?>, Integer> contracts) {
        return context.register(componentClass, contracts);
    }

    @Override
    public FeatureContext register(Object component) {
        return context.register(component);
    }

    @Override
    public FeatureContext register(Object component, int priority) {
        return context.register(component, priority);
    }

    @Override
    public FeatureContext register(Object component, Class<?>... contracts) {
        return context.register(component, contracts);
    }

    @Override
    public FeatureContext register(Object component, Map<Class<?>, Integer> contracts) {
        return context.register(component, contracts);
    }

    @Override
    public ServiceLocator getServiceLocator() {
        return serviceLocator;
    }
}
