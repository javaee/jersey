/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.server.model.internal;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.glassfish.jersey.internal.inject.Providers;
import org.glassfish.jersey.server.internal.LocalizationMessages;
import org.glassfish.jersey.server.model.Invocable;
import org.glassfish.jersey.server.spi.internal.ResourceMethodInvocationHandlerProvider;

import org.glassfish.hk2.api.ServiceLocator;

/**
 * An injectable {@link ResourceMethodInvocationHandlerProvider resource method
 * invocation handler provider} factory.
 * <p />
 * When invoked, the factory iterates over the registered custom {@link ResourceMethodInvocationHandlerProvider
 * resource method invocation handler providers} invoking their
 * {@link ResourceMethodInvocationHandlerProvider#create(org.glassfish.jersey.server.model.Invocable) createPatternFor(...)}
 * methods and returns the first non-null {@link InvocationHandler
 * invocation handler} instance retrieved from the providers. If no custom providers
 * are available, or if none of the providers returns a non-null invocation handler,
 * in such case a default invocation handler provided by the factory is returned.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
@Singleton
public final class ResourceMethodInvocationHandlerFactory implements ResourceMethodInvocationHandlerProvider {

    private static final InvocationHandler DEFAULT_HANDLER = new InvocationHandler() {

        @Override
        public Object invoke(Object target, Method method, Object[] args)
                throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
            return method.invoke(target, args);
        }
    };
    private static final Logger LOGGER = Logger.getLogger(ResourceMethodInvocationHandlerFactory.class.getName());
    private final Set<ResourceMethodInvocationHandlerProvider> providers;

    @Inject
    ResourceMethodInvocationHandlerFactory(ServiceLocator locator) {
        providers = Providers.getProviders(locator, ResourceMethodInvocationHandlerProvider.class);
    }

    // ResourceMethodInvocationHandlerProvider
    @Override
    public InvocationHandler create(Invocable resourceMethod) {
        for (ResourceMethodInvocationHandlerProvider provider : providers) {
            try {
                InvocationHandler handler = provider.create(resourceMethod);
                if (handler != null) {
                    return handler;
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, LocalizationMessages.ERROR_PROCESSING_METHOD(
                        resourceMethod,
                        provider.getClass().getName()), e);
            }
        }

        return DEFAULT_HANDLER;
    }
}
