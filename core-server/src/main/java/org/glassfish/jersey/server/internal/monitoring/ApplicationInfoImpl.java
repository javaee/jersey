/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.server.internal.monitoring;

import java.util.Date;
import java.util.Set;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.monitoring.ApplicationInfo;

/**
 * Application statistics.
 *
 * @author Miroslav Fuksa
 */
final class ApplicationInfoImpl implements ApplicationInfo {

    private final ResourceConfig resourceConfig;
    private final Date startTime;
    private final Set<Class<?>> registeredClasses;
    private final Set<Object> registeredInstances;
    private final Set<Class<?>> providers;

    /**
     * Create a new application statistics instance.
     *
     * @param resourceConfig Resource config of the application being monitored.
     * @param startTime Start time of the application (when initialization was finished).
     * @param registeredClasses Registered resource classes.
     * @param registeredInstances Registered resource instances.
     * @param providers Registered providers.
     */
    ApplicationInfoImpl(final ResourceConfig resourceConfig, final Date startTime, final Set<Class<?>> registeredClasses,
                        final Set<Object> registeredInstances, final Set<Class<?>> providers) {
        this.resourceConfig = resourceConfig;
        this.startTime = startTime;

        this.registeredClasses = registeredClasses;
        this.registeredInstances = registeredInstances;
        this.providers = providers;
    }

    @Override
    public ResourceConfig getResourceConfig() {
        return resourceConfig;
    }

    @Override
    public Date getStartTime() {
        return startTime;
    }

    @Override
    public Set<Class<?>> getRegisteredClasses() {
        return registeredClasses;
    }

    @Override
    public Set<Object> getRegisteredInstances() {
        return registeredInstances;
    }

    @Override
    public Set<Class<?>> getProviders() {
        return providers;
    }

    @Override
    public ApplicationInfo snapshot() {
        // snapshot functionality not yet implemented
        return this;
    }

}
