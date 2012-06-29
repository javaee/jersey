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

package org.glassfish.jersey.server.internal.routing;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.glassfish.jersey.internal.inject.AbstractModule;
import org.glassfish.jersey.spi.Singleton;

import org.glassfish.hk2.DynamicBinderFactory;
import org.glassfish.hk2.Services;
import org.glassfish.hk2.inject.Injector;

import org.jvnet.hk2.annotations.Inject;

/**
 * Class used to bind singleton resources classes into HK2.
 *
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 */
public class SingletonResourceBinder {
    private Set<Class<?>> registeredClasses = Collections.newSetFromMap(new ConcurrentHashMap<Class<?>, Boolean>());
    private final Object lock = new Object();

    @Inject
    private Injector injector;
    @Inject
    private Services services;

    /**
     * Binds {@code resourceClass} into HK2 context as singleton.
     *
     * @param <T>           Type of the resource class
     * @param resourceClass Resource class which should be bound. If the class is not
     *                      annotated with {@link Singleton Singleton annotation} it
     *                      will be ignored by this method.
     */
    public <T> void bindResourceClassAsSingleton(Class<T> resourceClass) {
        if (registeredClasses.contains(resourceClass)) {
            return;
        }

        synchronized (lock) {
            if (registeredClasses.contains(resourceClass)) {
                return;
            }

            if (resourceClass.isAnnotationPresent(Singleton.class)) {
                final DynamicBinderFactory binderFactory = services.bindDynamically();
                T instance = injector.inject(resourceClass);
                binderFactory.bind(resourceClass).toInstance(instance);
                binderFactory.commit();
            }
            registeredClasses.add(resourceClass);
        }
    }

    /**
     * Module which registers {@link SingletonResourceBinder} into HK2.
     */
    public static class SingletonResourceBinderModule extends AbstractModule {

        @Override
        protected void configure() {
            bind().to(org.glassfish.jersey.server.internal.routing.SingletonResourceBinder.class).in(org.glassfish.hk2.scopes
                    .Singleton.class);
        }
    }
}
