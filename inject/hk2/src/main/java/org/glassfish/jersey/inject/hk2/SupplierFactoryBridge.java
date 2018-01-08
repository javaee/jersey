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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.glassfish.jersey.internal.inject.DisposableSupplier;

import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.reflection.ParameterizedTypeImpl;

/**
 * This class is able to find the {@link Supplier} of the particular type and use this {@code Supplier} to create a new
 * instance. If the {@code Supplier} is not found then {@code null} is returned.
 * <a>
 * If the found {@code Supplier} is a type of {@link DisposableSupplier} then this bridge can delegate
 * {@link Factory#dispose(Object)} invocation to {@link DisposableSupplier#dispose(Object)}.
 * <p>
 * It's recommended to register the instance of this class as a singleton and then the {@link #provide()} is called according to
 * a provided scope (for the created instance) during the binding process.
 *
 * @param <T> type which could be handled by {@code Supplier} and this bridge.
 */
public class SupplierFactoryBridge<T> implements Factory<T> {

    private ServiceLocator locator;
    private ParameterizedType beanType;
    private String beanName;
    private boolean disposable;

    // This bridge can create multiple instances using the method 'provide' therefore must map created suppliers because of
    // 'dispose' invocation later on.
    // TODO: Key as a WeakReference - prevent objects in scope which never dispose the objects such as PerLookup.
    private Map<Object, DisposableSupplier<T>> disposableSuppliers = new IdentityHashMap<>();

    /**
     * Constructor for a new bridge.
     *
     * @param locator    currently used locator, all factory invocations will be delegated to this locator.
     * @param beanType   generic type of a {@link Supplier} which is looked for in locator and on which the creation of
     *                   the new instance is delegated.
     * @param beanName   name of the bean that is provided by supplier.
     * @param disposable flag whether the bridge is set up for disposing the created object.
     */
    SupplierFactoryBridge(ServiceLocator locator, Type beanType, String beanName, boolean disposable) {
        this.locator = locator;
        this.beanType = new ParameterizedTypeImpl(Supplier.class, beanType);
        this.beanName = beanName;
        this.disposable = disposable;
    }

    @Override
    public T provide() {
        if (beanType != null) {
            Supplier<T> supplier = locator.getService(beanType, beanName);
            T instance = supplier.get();
            if (disposable) {
                disposableSuppliers.put(instance, (DisposableSupplier<T>) supplier);
            }
            return instance;
        } else {
            return null;
        }
    }

    @Override
    public void dispose(T instance) {
        if (disposable) {
            DisposableSupplier<T> disposableSupplier = disposableSuppliers.get(instance);
            disposableSupplier.dispose(instance);
            disposableSuppliers.remove(instance);
        }
    }
}