/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved.
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

import java.util.HashSet;
import java.util.Set;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.glassfish.hk2.api.ServiceLocator;

/**
 * Invokes {@link PreDestroy} methods on all registered objects, when the service locator is shut down.
 * <p/>
 * Some objects managed by Jersey are created using {@link ServiceLocator#createAndInitialize}. This means
 * that such objects are created, dependencies injected and methods annotated with {@link javax.annotation.PostConstruct}
 * invoked. Therefore methods annotated with {@link PreDestroy} should be invoked on such objects too, when they are destroyed.
 * <p/>
 * This service invokes {@link PreDestroy} on all registered objects when {@link ServiceLocator#shutdown()} is invoked
 * on the service locator where this service is registered. Therefore only classes with their lifecycle linked
 * to the service locator that created them should be registered here.
 *
 * @author Petr Janouch (petr.janouch at oracle.com)
 */
@Singleton
public class ManagedObjectsFinalizer {

    @Inject
    private ServiceLocator serviceLocator;

    private final Set<Object> managedObjects = new HashSet<Object>();

    /**
     * Register an object for invocation of its {@link PreDestroy} method.
     * It will be invoked when the service locator is shut down.
     *
     * @param object an object to be registered.
     */
    public void registerForPreDestroyCall(Object object) {
        managedObjects.add(object);
    }

    @PreDestroy
    public void preDestroy() {
        try {
            for (Object o : managedObjects) {
                serviceLocator.preDestroy(o);
            }

        } finally {
            managedObjects.clear();
        }
    }
}
