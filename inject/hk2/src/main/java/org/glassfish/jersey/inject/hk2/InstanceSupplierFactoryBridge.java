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

import java.util.function.Supplier;

import org.glassfish.jersey.internal.inject.DisposableSupplier;

import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.ServiceLocator;

/**
 * This class is used as a bridge between {@link Factory HK2 Factory} and JDK {@link Supplier}. Using this class {@link Supplier}
 * is able to behave as a factory in service locator. The bridge just delegates all invocations to provided {@link ServiceLocator}
 * and therefore all operation should be in proper scope and context.
 * <p>
 * This bridge is dedicated to instance binding therefore underlying {@code supplier} is always only single instance.
 *
 * @param <T> type which could be handled by {@code Supplier} and this bridge.
 */
public class InstanceSupplierFactoryBridge<T> implements Factory<T> {

    private Supplier<T> supplier;
    private boolean disposable;

    /**
     * Constructor for a new bridge.
     *
     * @param supplier   type which will be looked for in locator.
     * @param disposable flag whether the bridge is set up for disposing the created object.
     */
    InstanceSupplierFactoryBridge(Supplier<T> supplier, boolean disposable) {
        this.supplier = supplier;
        this.disposable = disposable;
    }

    @Override
    public T provide() {
        return supplier.get();
    }

    @Override
    public void dispose(T instance) {
        if (disposable) {
            ((DisposableSupplier<T>) supplier).dispose(instance);
        }
    }
}
