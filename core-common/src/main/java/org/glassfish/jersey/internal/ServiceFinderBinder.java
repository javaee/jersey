/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2014 Oracle and/or its affiliates. All rights reserved.
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

import java.util.Map;

import javax.ws.rs.RuntimeType;

import org.glassfish.jersey.CommonProperties;

import org.glassfish.hk2.utilities.binding.AbstractBinder;

/**
 * Simple ServiceFinder injection binder.
 *
 * Looks for all implementations of a given contract using {@link ServiceFinder}
 * and registers found instances to {@link org.glassfish.hk2.api.ServiceLocator}.
 *
 * @param <T> contract type.
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 * @author Libor Kramolis (libor.kramolis at oracle.com)
 */
public class ServiceFinderBinder<T> extends AbstractBinder {

    private final Class<T> contract;

    private final Map<String, Object> applicationProperties;

    private final RuntimeType runtimeType;

    /**
     * Create a new service finder injection binder.
     *
     * @param contract contract of the service providers bound by this binder.
     * @param applicationProperties map containing application properties. May be {@code null}.
     * @param runtimeType runtime (client or server) where the service finder binder is used.
     */
    public ServiceFinderBinder(Class<T> contract, Map<String, Object> applicationProperties, RuntimeType runtimeType) {
        this.contract = contract;
        this.applicationProperties = applicationProperties;
        this.runtimeType = runtimeType;
    }

    @Override
    protected void configure() {
        final boolean METAINF_SERVICES_LOOKUP_DISABLE_DEFAULT = false;
        boolean disableMetainfServicesLookup = METAINF_SERVICES_LOOKUP_DISABLE_DEFAULT;
        if (applicationProperties != null) {
            disableMetainfServicesLookup = CommonProperties.getValue(applicationProperties, runtimeType,
                    CommonProperties.METAINF_SERVICES_LOOKUP_DISABLE, METAINF_SERVICES_LOOKUP_DISABLE_DEFAULT, Boolean.class);
        }
        if (!disableMetainfServicesLookup) {
            for (Class<T> t : ServiceFinder.find(contract, true).toClassArray()) {
                bind(t).to(contract);
            }
        }
    }
}
