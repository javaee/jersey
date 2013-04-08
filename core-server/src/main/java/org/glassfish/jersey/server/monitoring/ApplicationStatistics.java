/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.server.monitoring;

import java.util.Date;
import java.util.Set;

import org.glassfish.jersey.server.ResourceConfig;

/**
 * Monitoring statistics and configuration of an application.
 * <p/>
 * Statistics retrieved from Jersey runtime might be mutable and thanks to it might provide inconsistent data
 * as not all statistics are updated in the same time. To retrieve the immutable and consistent
 * statistics data the method {@link #snapshot()} should be used.
 *
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 * @see MonitoringStatistics See MonitoringStatistics class for general details about statistics.
 */
public interface ApplicationStatistics {
    /**
     * Get the resource config.
     *
     * @return Resource config.
     */
    public ResourceConfig getResourceConfig();

    /**
     * Get the start time of the application.
     *
     * @return Time when an application initialization has been finished.
     */
    public Date getStartTime();

    /**
     * Get the time when application destroy (stop) has been finished.
     *
     * @return Destroy time.
     */
    public Date getDestroyTime();


    /**
     * Get resource classes registered by the user in the current application. The set contains only
     * user resource classes and not resource classes added by Jersey
     * or by {@link org.glassfish.jersey.server.model.ModelProcessor}.
     * <p/>
     * User resources are resources that
     * were explicitly registered by the configuration, discovered by the class path scanning or that
     * constructs explicitly registered {@link org.glassfish.jersey.server.model.Resource programmatic resource}.
     *
     * @return Resource user registered classes.
     */
    public Set<Class<?>> getRegisteredClasses();

    /**
     * Get resource instances registered by the user in the current application. The set contains only
     * user resources and not resources added by Jersey
     * or by {@link org.glassfish.jersey.server.model.ModelProcessor}.
     * <p/>
     * User resources are resources that
     * were explicitly registered by the configuration, discovered by the class path scanning or that
     * constructs explicitly registered {@link org.glassfish.jersey.server.model.Resource programmatic resource}.
     *
     * @return Resource instances registered by user.
     */
    public Set<Object> getRegisteredInstances();

    /**
     * Get registered providers available in the runtime. The registered providers
     * are providers like {@link org.glassfish.jersey.server.model.MethodList.Filter filters},
     * {@link javax.ws.rs.ext.ReaderInterceptor reader} and {@link javax.ws.rs.ext.WriterInterceptor writer}
     * interceptors which are explicitly registered by configuration, or annotated by
     * {@link javax.ws.rs.ext.Provider @Provider} or registered in META-INF/services. The
     * set does not include providers that are by default built in Jersey.
     *
     * @return Set of provider classes.
     */
    public Set<Class<?>> getProviders();

    /**
     * Get the immutable consistent snapshot of the monitoring statistics. Working with snapshots might
     * have negative performance impact as snapshot must be created but ensures consistency of data over time.
     * However, the usage of snapshot is encouraged to avoid working with inconsistent data. Not all statistics
     * must be updated in the same time on mutable version of statistics.
     *
     * @return Snapshot of application statistics.
     */
    public ApplicationStatistics snapshot();
}

