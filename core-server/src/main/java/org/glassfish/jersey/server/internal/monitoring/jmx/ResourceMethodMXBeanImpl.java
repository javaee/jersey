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

import org.glassfish.jersey.message.internal.MediaTypes;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.monitoring.ResourceMethodMXBean;
import org.glassfish.jersey.server.monitoring.ResourceMethodStatistics;

/**
 * MXBean implementing the {@link org.glassfish.jersey.server.monitoring.ResourceMethodMXBean} MXBean interface.
 * @author Miroslav Fuksa
 */
public class ResourceMethodMXBeanImpl implements ResourceMethodMXBean {
    private volatile ExecutionStatisticsDynamicBean methodExecutionStatisticsMxBean;
    private volatile ExecutionStatisticsDynamicBean requestExecutionStatisticsMxBean;
    private final String path;
    private final String name;
    private final ResourceMethod resourceMethod;
    private final String methodBeanName;

    /**
     * Create a new MXBean and expose it into mbean server using {@code mBeanExposer}.
     *
     * @param methodStatistics Statistics to be exposed by the MXBean.
     * @param uriResource {@code true} if the enclosing resource is identified by URI (and not by java
     *                                class name for example).
     * @param mBeanExposer MBean exposer.
     * @param parentName Name of the parent bean.
     * @param methodUniqueId method unique identifier in the enclosing resource
     */
    public ResourceMethodMXBeanImpl(ResourceMethodStatistics methodStatistics, boolean uriResource,
                                    MBeanExposer mBeanExposer, String parentName, String methodUniqueId) {

        // init mbean name
        this.resourceMethod = methodStatistics.getResourceMethod();
        final Class<?> handlerClass = resourceMethod.getInvocable().getHandler().getHandlerClass();
        final Class<?>[] paramTypes = resourceMethod.getInvocable().getHandlingMethod().getParameterTypes();
        this.name = resourceMethod.getInvocable().getHandlingMethod().getName();
        StringBuilder params = new StringBuilder();
        for (Class<?> type : paramTypes) {
            params.append(type.getSimpleName()).append(";");
        }
        if (params.length() > 0) {
            params.setLength(params.length() - 1);
        }

        if (uriResource) {
            path = "N/A";
        } else {
            path = resourceMethod.getParent().getParent() == null ? "" : resourceMethod.getParent().getPath();
        }

        final String hash = Integer.toHexString(methodUniqueId.hashCode());

        String beanName = resourceMethod.getHttpMethod() + "->";
        if (uriResource) {
            beanName += handlerClass.getSimpleName()
                    + "." + name + "(" + params.toString() + ")#" + hash;
        } else {
            beanName += name + "(" + params.toString() + ")#"
                    + hash;
        }
        this.methodBeanName = parentName + ",detail=methods,method=" + beanName;


        // register mbean
        mBeanExposer.registerMBean(this, methodBeanName);
        methodExecutionStatisticsMxBean = new ExecutionStatisticsDynamicBean(methodStatistics.getMethodStatistics(),
                mBeanExposer, methodBeanName, MBeanExposer.PROPERTY_EXECUTION_TIMES_METHODS);
        requestExecutionStatisticsMxBean = new ExecutionStatisticsDynamicBean(methodStatistics.getRequestStatistics(),
                mBeanExposer, methodBeanName, MBeanExposer.PROPERTY_EXECUTION_TIMES_REQUESTS);
    }

    /**
     * Update the statistics that are exposed by this MXBean.
     * @param resourceMethodStatisticsImpl New statistics.
     */
    public void updateResourceMethodStatistics(ResourceMethodStatistics resourceMethodStatisticsImpl) {
        this.methodExecutionStatisticsMxBean.updateExecutionStatistics(resourceMethodStatisticsImpl.getMethodStatistics());
        this.requestExecutionStatisticsMxBean.updateExecutionStatistics(resourceMethodStatisticsImpl.getRequestStatistics());
    }


    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getHttpMethod() {
        return resourceMethod.getHttpMethod();
    }

    @Override
    public String getDeclaringClassName() {
        return this.resourceMethod.getInvocable().getHandlingMethod().getDeclaringClass().getName();
    }

    @Override
    public String getConsumesMediaType() {
        return MediaTypes.convertToString(resourceMethod.getConsumedTypes());
    }

    @Override
    public String getProducesMediaType() {
        return MediaTypes.convertToString(resourceMethod.getProducedTypes());
    }

    @Override
    public String getMethodName() {
        return name;
    }
}
