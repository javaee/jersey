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

package org.glassfish.jersey.internal.inject;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Set;

/**
 * Implementation of the instance keeper which kept the instance of the class from
 * {@link InjectionManager} and the other information about this instance.
 *
 * @param <T> type of the kept instance.
 */
public class ServiceHolderImpl<T> implements ServiceHolder<T> {

    private final T service;

    private final Class<T> implementationClass;

    private final Set<Type> contractTypes;

    private final int rank;

    /**
     * Creates a new instance of the service holder which keeps the concrete instance and its additional information.
     *
     * @param service             service instance kept by this holder.
     * @param contractTypes       types which represent the given instance.
     */
    @SuppressWarnings("unchecked")
    public ServiceHolderImpl(T service, Set<Type> contractTypes) {
        this(service, (Class<T>) service.getClass(), contractTypes, 0);
    }

    /**
     * Creates a new instance of the service holder which keeps the concrete instance and its additional information.
     *
     * @param service             service instance kept by this holder.
     * @param implementationClass implementation class of the given instance.
     * @param contractTypes       types which represent the given instance.
     * @param rank                ranking of the given instance.
     */
    public ServiceHolderImpl(T service, Class<T> implementationClass, Set<Type> contractTypes, int rank) {
        this.service = service;
        this.implementationClass = implementationClass;
        this.contractTypes = contractTypes;
        this.rank = rank;
    }

    @Override
    public T getInstance() {
        return service;
    }

    @Override
    public Class<T> getImplementationClass() {
        return implementationClass;
    }

    @Override
    public Set<Type> getContractTypes() {
        return contractTypes;
    }

    @Override
    public int getRank() {
        return rank;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ServiceHolderImpl)) {
            return false;
        }
        ServiceHolderImpl<?> that = (ServiceHolderImpl<?>) o;
        return rank == that.rank
                && Objects.equals(service, that.service)
                && Objects.equals(implementationClass, that.implementationClass)
                && Objects.equals(contractTypes, that.contractTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(service, implementationClass, contractTypes, rank);
    }
}
