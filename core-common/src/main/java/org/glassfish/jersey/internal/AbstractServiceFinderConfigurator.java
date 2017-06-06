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

package org.glassfish.jersey.internal;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.RuntimeType;

import org.glassfish.jersey.CommonProperties;
import org.glassfish.jersey.internal.inject.InjectionManager;

/**
 * Simple ServiceFinder configuration.
 *
 * Looks for all implementations of a given contract using {@link ServiceFinder} and registers found instances to
 * {@link InjectionManager}.
 *
 * @param <T> contract type.
 * @author Petr Bouda (petr.bouda at oracle.com)
 */
public abstract class AbstractServiceFinderConfigurator<T> implements BootstrapConfigurator {

    private final Class<T> contract;
    private final RuntimeType runtimeType;

    /**
     * Create a new configurator.
     *
     * @param contract    contract of the service providers bound by this binder.
     * @param runtimeType runtime (client or server) where the service finder binder is used.
     */
    protected AbstractServiceFinderConfigurator(Class<T> contract, RuntimeType runtimeType) {
        this.contract = contract;
        this.runtimeType = runtimeType;
    }

    /**
     * Load all particular implementations of the type {@code T} using {@link ServiceFinder}.
     *
     * @param applicationProperties map containing application properties. May be {@code null}
     * @return all registered classes of the type {@code T}.
     */
    protected List<Class<T>> loadImplementations(Map<String, Object> applicationProperties) {
        boolean METAINF_SERVICES_LOOKUP_DISABLE_DEFAULT = false;
        boolean disableMetaInfServicesLookup = METAINF_SERVICES_LOOKUP_DISABLE_DEFAULT;
        if (applicationProperties != null) {
            disableMetaInfServicesLookup = CommonProperties.getValue(applicationProperties, runtimeType,
                    CommonProperties.METAINF_SERVICES_LOOKUP_DISABLE, METAINF_SERVICES_LOOKUP_DISABLE_DEFAULT, Boolean.class);
        }
        if (!disableMetaInfServicesLookup) {
            return Stream.of(ServiceFinder.find(contract, true).toClassArray())
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
