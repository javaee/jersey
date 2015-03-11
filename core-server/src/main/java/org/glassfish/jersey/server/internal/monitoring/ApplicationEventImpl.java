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

import java.util.Set;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.ResourceModel;
import org.glassfish.jersey.server.monitoring.ApplicationEvent;

/**
 * Implementation of {@link ApplicationEvent application event}. Instances are immutable.
 *
 * @author Miroslav Fuksa
 */
public class ApplicationEventImpl implements ApplicationEvent {

    private final Type type;
    private final ResourceConfig resourceConfig;
    private final Set<Class<?>> providers;
    private final Set<Class<?>> registeredClasses;
    private final Set<Object> registeredInstances;
    private final ResourceModel resourceModel;

    /**
     * Create a new application event.
     * @param type Type of the event.
     * @param resourceConfig Resource config of the application.
     * @param registeredClasses Registered resource classes.
     * @param registeredInstances Registered resource instances.
     * @param resourceModel Resource model of the application (enhanced by
     *                      {@link org.glassfish.jersey.server.model.ModelProcessor model processors}).
     * @param providers Registered providers.
     */
    public ApplicationEventImpl(Type type, ResourceConfig resourceConfig,
                                Set<Class<?>> providers, Set<Class<?>> registeredClasses,
                                Set<Object> registeredInstances, ResourceModel resourceModel) {
        this.type = type;
        this.resourceConfig = resourceConfig;
        this.providers = providers;
        this.registeredClasses = registeredClasses;
        this.registeredInstances = registeredInstances;
        this.resourceModel = resourceModel;
    }

    @Override
    public ResourceConfig getResourceConfig() {
        return resourceConfig;
    }

    @Override
    public Type getType() {
        return type;
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
    public ResourceModel getResourceModel() {
        return resourceModel;
    }
}
