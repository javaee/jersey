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

package org.glassfish.jersey.server.internal.monitoring.jmx;

import java.util.Map;

import org.glassfish.jersey.server.internal.monitoring.MonitoringUtils;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.monitoring.ResourceMXBean;
import org.glassfish.jersey.server.monitoring.ResourceMethodStatistics;
import org.glassfish.jersey.server.monitoring.ResourceStatistics;

import jersey.repackaged.com.google.common.collect.Maps;

/**
 * MXBean implementing the {@link org.glassfish.jersey.server.monitoring.ResourceMethodMXBean} MXBean interface.
 *
 * @author Miroslav Fuksa
 */
public class ResourceMxBeanImpl implements ResourceMXBean {
    private final String name;
    private volatile ExecutionStatisticsDynamicBean methodsExecutionStatisticsBean;
    private volatile ExecutionStatisticsDynamicBean requestExecutionStatisticsBean;
    private final Map<String, ResourceMethodMXBeanImpl> resourceMethods = Maps.newHashMap();
    private final String resourcePropertyName;
    private final boolean uriResource;
    private final MBeanExposer mBeanExposer;

    /**
     * Create and register new MXBean into the mbean server using the {@code mBeanExposer}.
     *
     * @param resourceStatistics resource statistics that should be exposed by this and nested MXBeans.
     * @param name Name of the resource.
     * @param uriResource {@code true} if the resource is identified by URI (and not by java class name for example).
     * @param mBeanExposer MBean exposer.
     * @param parentName Name of the parent bean.
     */
    public ResourceMxBeanImpl(ResourceStatistics resourceStatistics, String name, boolean uriResource,
                              MBeanExposer mBeanExposer,
                              String parentName) {
        this.name = name;
        this.uriResource = uriResource;
        this.mBeanExposer = mBeanExposer;
        this.resourcePropertyName = parentName + ",resource=" + MBeanExposer.convertToObjectName(name, uriResource);
        mBeanExposer.registerMBean(this, resourcePropertyName);
        this.methodsExecutionStatisticsBean = new ExecutionStatisticsDynamicBean(
                resourceStatistics.getResourceMethodExecutionStatistics(), mBeanExposer, resourcePropertyName,
                MBeanExposer.PROPERTY_EXECUTION_TIMES_METHODS);
        this.requestExecutionStatisticsBean = new ExecutionStatisticsDynamicBean(
                resourceStatistics.getRequestExecutionStatistics(), mBeanExposer, resourcePropertyName,
                MBeanExposer.PROPERTY_EXECUTION_TIMES_REQUESTS);

        updateResourceStatistics(resourceStatistics);
    }

    /**
     * Update the statistics of this MXBean and of nested MXBeans.
     * @param resourceStatistics New resource statistics.
     */
    public void updateResourceStatistics(ResourceStatistics resourceStatistics) {
        this.methodsExecutionStatisticsBean.updateExecutionStatistics(resourceStatistics.getResourceMethodExecutionStatistics());
        this.requestExecutionStatisticsBean.updateExecutionStatistics(resourceStatistics.getRequestExecutionStatistics());

        for (Map.Entry<ResourceMethod, ResourceMethodStatistics> entry
                : resourceStatistics.getResourceMethodStatistics().entrySet()) {
            final ResourceMethodStatistics methodStats = entry.getValue();
            final ResourceMethod method = entry.getKey();

            final String methodId = MonitoringUtils.getMethodUniqueId(method);

            ResourceMethodMXBeanImpl methodMXBean = this.resourceMethods.get(methodId);
            if (methodMXBean == null) {
                methodMXBean = new ResourceMethodMXBeanImpl(methodStats, uriResource, mBeanExposer,
                        resourcePropertyName, methodId);
                resourceMethods.put(methodId, methodMXBean);
            }
            methodMXBean.updateResourceMethodStatistics(methodStats);
        }
    }

    @Override
    public String getName() {
        return this.name;
    }


}
