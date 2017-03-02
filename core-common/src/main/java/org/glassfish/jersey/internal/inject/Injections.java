/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.internal.inject;

import java.util.Iterator;
import java.util.ServiceLoader;

import javax.ws.rs.WebApplicationException;

import javax.inject.Provider;

import org.glassfish.jersey.hk2.HK2InjectionManager;

import org.glassfish.hk2.api.MultiException;

/**
 * Injection binding utility methods.
 *
 * @author Tom Beerbower
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class Injections {

    /**
     * Create a {@link InjectionManager}. In case the {@code name} is not specified, the locator
     * will be unnamed.
     *
     * @param name                 The name of this injection manager. Passing a {@code null}
     *                             name will result in a newly created injection manager with a
     *                             generated name.
     * @param parent               The parent of this injection manager. Services can be found in
     *                             the parent (and all grand-parents). May be {@code null}.
     *                             if the returned injection manager should not be parented.
     * @param binders              custom the {@link Binder binders}.
     * @return a injection manager with all the bindings.
     */
    public static InjectionManager createInjectionManager(String name, InjectionManager parent, Binder... binders) {
        return _injectionManager(name, parent, binders);
    }

    /**
     * Create a {@link InjectionManager}. In case the {@code name} is not specified, the locator
     * will be unnamed.
     *
     * @param binders custom the {@link Binder binders}.
     * @return a injection manager with all the bindings.
     */
    public static InjectionManager createInjectionManager(Binder... binders) {
        return _injectionManager(null, null, binders);
    }

    /**
     * Create a {@link InjectionManager}. In case the {@code name} is not specified, the locator
     * will be unnamed.
     *
     * @param name    The name of this injection manager. Passing a {@code null}
     *                name will result in a newly created injection manager with a
     *                generated name.
     * @param binders custom the {@link Binder binders}.
     * @return a injection manager with all the bindings.
     */
    public static InjectionManager createInjectionManager(String name, Binder... binders) {
        return _injectionManager(name, null, binders);
    }

    /**
     * Create an unnamed, parented {@link InjectionManager}. In case the {@code parent} injection manager
     * is not specified, the locator will not be parented.
     *
     * @param parent  The parent of this underlying DI locator. Services can be found in
     *                the parent (and all grand-parents). May be {@code null}.
     *                if the returned BeanManager should not be parented.
     * @param binders custom the {@link Binder binders}.
     * @return an injection manager with all the bindings.
     */
    public static InjectionManager createInjectionManager(InjectionManager parent, Binder... binders) {
        return _injectionManager(null, parent, binders);
    }

    private static InjectionManager _injectionManager(String name, InjectionManager parent, Binder... binders) {
        Iterator<InjectionManager> iterator = ServiceLoader.load(InjectionManager.class).iterator();
        InjectionManager injectionManager;
        if (iterator.hasNext()) {
            injectionManager = iterator.next();
        } else {
            // TODO: Log that there is no explicitly configured InjectionManager, default is used.
            injectionManager = new HK2InjectionManager();
        }

        injectionManager.initialize(name, parent, binders);
        return injectionManager;
    }

    /**
     * Get the class by contract or create and inject a new instance.
     *
     * @param <T>             instance type.
     * @param injectionManager DI injection manager.
     * @param clazz           class of the instance to be provider.
     * @return instance of the class either provided as a service or created and injected  by HK2.
     */
    public static <T> T getOrCreate(InjectionManager injectionManager, final Class<T> clazz) {
        try {
            final T component = injectionManager.getInstance(clazz);
            return component == null ? injectionManager.createAndInitialize(clazz) : component;
            // TODO: not really MultiException.
        } catch (final MultiException e) {

            // Look for WebApplicationException and return it if found. MultiException is thrown when *Param field is
            // annotated and value cannot be provided (for example fromString(String) method can throw unchecked
            // exception.
            //
            // see InvalidParamTest
            // see JERSEY-1117
            for (final Throwable t : e.getErrors()) {
                if (WebApplicationException.class.isAssignableFrom(t.getClass())) {
                    throw (WebApplicationException) t;
                }
            }

            throw e;
        }
    }

    /**
     * Get a provider for a contract.
     *
     * @param <T>              instance type.
     * @param injectionManager injection manager.
     * @param clazz            class of the instance to be provider.
     * @return provider of contract class.
     */
    public static <T> Provider<T> getProvider(final InjectionManager injectionManager, final Class<T> clazz) {
        return () -> injectionManager.getInstance(clazz);
    }
}
