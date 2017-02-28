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

import org.glassfish.jersey.hk2.HK2InstanceManager;
import org.glassfish.jersey.spi.inject.Binder;
import org.glassfish.jersey.spi.inject.InstanceManager;

import org.glassfish.hk2.api.MultiException;

/**
 * Injection binding utility methods.
 *
 * @author Tom Beerbower
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class Injections {

    /**
     * Create a {@link InstanceManager}. In case the {@code name} is not specified, the locator
     * will be unnamed.
     *
     * @param name                 The name of this instance manager. Passing a {@code null}
     *                             name will result in a newly created instance manager with a
     *                             generated name.
     * @param parent               The parent of this instance manager. Services can be found in
     *                             the parent (and all grand-parents). May be {@code null}.
     *                             if the returned instance manager should not be parented.
     * @param binders              custom the {@link Binder binders}.
     * @return a instance manager with all the bindings.
     */
    public static InstanceManager createInstanceManager(String name, InstanceManager parent, Binder... binders) {
        return _instanceManager(name, parent, binders);
    }

    /**
     * Create a {@link InstanceManager}. In case the {@code name} is not specified, the locator
     * will be unnamed.
     *
     * @param binders custom the {@link Binder binders}.
     * @return a instance manager with all the bindings.
     */
    public static InstanceManager createInstanceManager(Binder... binders) {
        return _instanceManager(null, null, binders);
    }

    /**
     * Create a {@link InstanceManager}. In case the {@code name} is not specified, the locator
     * will be unnamed.
     *
     * @param name    The name of this instance manager. Passing a {@code null}
     *                name will result in a newly created instance manager with a
     *                generated name.
     * @param binders custom the {@link Binder binders}.
     * @return a instance manager with all the bindings.
     */
    public static InstanceManager createInstanceManager(String name, Binder... binders) {
        return _instanceManager(name, null, binders);
    }

    /**
     * Create an unnamed, parented {@link InstanceManager}. In case the {@code parent} instance manager
     * is not specified, the locator will not be parented.
     *
     * @param parent  The parent of this underlying DI locator. Services can be found in
     *                the parent (and all grand-parents). May be {@code null}.
     *                if the returned BeanManager should not be parented.
     * @param binders custom the {@link Binder binders}.
     * @return a instance manager with all the bindings.
     */
    public static InstanceManager createInstanceManager(InstanceManager parent, Binder... binders) {
        return _instanceManager(null, parent, binders);
    }

    private static InstanceManager _instanceManager(String name, InstanceManager parent, Binder... binders) {
        Iterator<InstanceManager> iterator = ServiceLoader.load(InstanceManager.class).iterator();
        InstanceManager instanceManager;
        if (iterator.hasNext()) {
            instanceManager = iterator.next();
        } else {
            // TODO: Log that there is no explicitly configured InstanceManager, default is used.
            instanceManager = new HK2InstanceManager();
        }

        instanceManager.initialize(name, parent, binders);
        return instanceManager;
    }

    /**
     * Get the class by contract or create and inject a new instance.
     *
     * @param <T>             instance type.
     * @param instanceManager DI instance manager.
     * @param clazz           class of the instance to be provider.
     * @return instance of the class either provided as a service or created and injected  by HK2.
     */
    public static <T> T getOrCreate(InstanceManager instanceManager, final Class<T> clazz) {
        try {
            final T component = instanceManager.getInstance(clazz);
            return component == null ? instanceManager.createAndInitialize(clazz) : component;
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
     * @param <T>             instance type.
     * @param instanceManager instance manager.
     * @param clazz           class of the instance to be provider.
     * @return provider of contract class.
     */
    public static <T> Provider<T> getProvider(final InstanceManager instanceManager, final Class<T> clazz) {
        return () -> instanceManager.getInstance(clazz);
    }
}
