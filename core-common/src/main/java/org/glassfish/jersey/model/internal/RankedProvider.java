/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2015 Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;
import javax.ws.rs.Priorities;

import javax.annotation.Priority;

import org.glassfish.jersey.model.ContractProvider;

/**
 * Jersey ranked provider model.
 *
 * @param <T> service provider contract Java type.
 * @author Michal Gajdos
 */
public class RankedProvider<T> {

    private final T provider;
    private final int rank;
    private final Set<Type> contractTypes;

    /**
     * Creates a new {@code RankedProvider} instance. The rank of the provider is obtained from the {@link javax.annotation.Priority}
     * annotation or is set to {@value javax.ws.rs.Priorities#USER} if the annotation is not present.
     *
     * @param provider service provider to create a {@code RankedProvider} instance from.
     */
    public RankedProvider(final T provider) {
        this.provider = provider;
        this.rank = computeRank(provider, ContractProvider.NO_PRIORITY);
        this.contractTypes = null;
    }

    /**
     * Creates a new {@code RankedProvider} instance for given {@code provider} with specific {@code rank} (> 0).
     *
     * @param provider service provider to create a {@code RankedProvider} instance from.
     * @param rank rank of this provider.
     */
    public RankedProvider(final T provider, final int rank) {
        this(provider, rank, null);
    }

    /**
     * Creates a new {@code RankedProvider} instance for given {@code provider} with specific {@code rank} (> 0).
     *
     * @param provider service provider to create a {@code RankedProvider} instance from.
     * @param rank rank of this provider.
     * @param contracts contracts implemented by the service provider
     */
    public RankedProvider(final T provider, final int rank, final Set<Type> contracts) {
        this.provider = provider;
        this.rank = computeRank(provider, rank);
        this.contractTypes = contracts;
    }

    private int computeRank(final T provider, final int rank) {
        if (rank > 0) {
            return rank;
        } else {
            if (provider.getClass().isAnnotationPresent(Priority.class)) {
                return provider.getClass().getAnnotation(Priority.class).value();
            } else {
                return Priorities.USER;
            }
        }
    }

    public T getProvider() {
        return provider;
    }

    public int getRank() {
        return rank;
    }

    /**
     * Get me set of implemented contracts.
     * Returns null if no contracts are implemented.
     *
     * @return set of contracts or null if no contracts have been implemented.
     */
    public Set<Type> getContractTypes() {
        return contractTypes;
    }

    @Override
    public String toString() {
        return provider.getClass().getName();
    }
}
