/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.process.internal;

import java.util.*;

import javax.ws.rs.BindingPriority;
import javax.ws.rs.client.Configuration;

import org.glassfish.jersey.internal.inject.Providers;
import org.glassfish.jersey.internal.ServiceProviders;

import org.glassfish.hk2.Factory;
import org.glassfish.hk2.Services;

import org.jvnet.hk2.annotations.Inject;

/**
 * Abstract filter processor.
 *
 * @param <T> Parameter representing the filter type
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 * @author Santiago Pericas-Geertsen (santiago.pericasgeertsen at oracle.com)
 */
abstract class AbstractFilterProcessor<T> {

    /**
     * Ordering of filters based on their {@link BindingPriority binding priority}
     * values from lower to higher (ascending).
     */
    protected static final int FILTER_ORDER_ASCENDING = 1;
    /**
     * Ordering of filters based on their {@link BindingPriority binding priority}
     * values from higher to lower (descending).
     */
    protected static final int FILTER_ORDER_DESCENDING = -1;
    @Inject
    private Services services;
    @Inject
    protected Factory<JerseyFilterContext> filterContextFactory;
    //
    private final int ordering;

    /**
     * Initialize filter ordering to a default value, i.e.
     * {@link AbstractFilterProcessor#FILTER_ORDER_ASCENDING}
     */
    protected AbstractFilterProcessor() {
        this.ordering = FILTER_ORDER_ASCENDING;
    }

    /**
     * Initialize filter ordering to a custom value. The value can be either
     * {@link AbstractFilterProcessor#FILTER_ORDER_ASCENDING} or
     * {@link AbstractFilterProcessor#FILTER_ORDER_DESCENDING}.
     */
    protected AbstractFilterProcessor(int ordering) {
        this.ordering = ordering;
    }

    /**
     * Get the filter providers for the specific filter contract, sorted by their
     * {@link BindingPriority binding priority}. The filter ordering is determined
     *
     *
     * @param filterContract filter contract.
     * @return sorted list of filter contract providers.
     */
    protected final List<T> getFilters(final Class<T> filterContract) {
        ServiceProviders serviceProviders = services.forContract(ServiceProviders.class).get();
        return serviceProviders.getAll(filterContract, new Comparator<T>() {

            @Override
            public int compare(T t, T t1) {
                return (getPriority(t) - getPriority(t1)) * ordering;
            }

            private int getPriority(T t) {
                if (t.getClass().isAnnotationPresent(BindingPriority.class)) {
                    return t.getClass().getAnnotation(BindingPriority.class).value();
                } else {
                    return BindingPriority.USER;
                }
            }
        });
    }

    /**
     * Get the immutable bag of request-scoped configuration properties.
     *
     * @return immutable property map.
     */
    // TODO fix to work for both, client and server.
    protected final Map<String, Object> getProperties() {
        Configuration config = services.forContract(Configuration.class).get();
        return (config != null) ? config.getProperties() : null;
    }
}
