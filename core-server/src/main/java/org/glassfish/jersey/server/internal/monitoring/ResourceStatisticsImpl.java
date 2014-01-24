/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2014 Oracle and/or its affiliates. All rights reserved.
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

import java.util.Collections;
import java.util.Map;

import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.monitoring.ExecutionStatistics;
import org.glassfish.jersey.server.monitoring.ResourceMethodStatistics;
import org.glassfish.jersey.server.monitoring.ResourceStatistics;

import jersey.repackaged.com.google.common.collect.Maps;

/**
 * Resource statistics implementation.
 *
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 */
class ResourceStatisticsImpl implements ResourceStatistics {


    /**
     * Builder of resource statistics instances.
     */
    static class Builder {
        private final ExecutionStatisticsImpl.Builder resourceExecutionStatisticsBuilder;
        private final ExecutionStatisticsImpl.Builder requestExecutionStatisticsBuilder;
        private final Map<ResourceMethod, ResourceMethodStatisticsImpl.Builder> methodsBuilders = Maps.newHashMap();
        private final Map<String, ResourceMethodStatisticsImpl.Builder> stringToMethodsBuilders = Maps.newHashMap();

        /**
         * Create a new builder.
         *
         * @param resource Resource for which the instance is created.
         */
        Builder(Resource resource) {
            this();
            for (ResourceMethod method : resource.getResourceMethods()) {
                getOrCreate(method);
            }
        }

        /**
         * Create a new builder.
         */
        Builder() {
            this.resourceExecutionStatisticsBuilder = new ExecutionStatisticsImpl.Builder();
            this.requestExecutionStatisticsBuilder = new ExecutionStatisticsImpl.Builder();
        }

        /**
         * Build a new instance of {@link ResourceStatisticsImpl}.
         * @return New instance of resource statistics.
         */
        ResourceStatisticsImpl build() {
            final Map<ResourceMethod, ResourceMethodStatistics> resourceMethods = Maps.newHashMap();
            for (Map.Entry<ResourceMethod, ResourceMethodStatisticsImpl.Builder> methodEntry : methodsBuilders.entrySet()) {
                resourceMethods.put(methodEntry.getKey(), methodEntry.getValue().build());
            }

            return new ResourceStatisticsImpl(
                    Collections.unmodifiableMap(resourceMethods),
                    resourceExecutionStatisticsBuilder.build(),
                    requestExecutionStatisticsBuilder.build());
        }

        /**
         * Add execution of a resource method in the resource.
         *
         * @param resourceMethod Resource method executed.
         * @param methodStartTime Time of execution of the resource method.
         * @param methodDuration Time spent on execution of resource method itself.
         * @param requestStartTime Time when the request matching to the executed resource method has been received
         *                         by Jersey.
         * @param requestDuration Time of whole request processing (from receiving the request until writing the response).
         */
        void addExecution(ResourceMethod resourceMethod, long methodStartTime, long methodDuration,
                          long requestStartTime, long requestDuration) {
            resourceExecutionStatisticsBuilder.addExecution(methodStartTime, methodDuration);
            requestExecutionStatisticsBuilder.addExecution(requestStartTime, requestDuration);


            final ResourceMethodStatisticsImpl.Builder builder = getOrCreate(resourceMethod);
            builder.addResourceMethodExecution(methodStartTime, methodDuration, requestStartTime, requestDuration);
        }

        /**
         * Add a resource method to the statistics.
         *
         * @param resourceMethod Resource method.
         */
        void addMethod(ResourceMethod resourceMethod) {
            getOrCreate(resourceMethod);

        }

        private ResourceMethodStatisticsImpl.Builder getOrCreate(ResourceMethod resourceMethod) {
            final String methodUniqueId = MonitoringUtils.getMethodUniqueId(resourceMethod);
            ResourceMethodStatisticsImpl.Builder methodBuilder = stringToMethodsBuilders.get(methodUniqueId);

            if (methodBuilder == null) {
                methodBuilder = new ResourceMethodStatisticsImpl.Builder(resourceMethod);
                methodsBuilders.put(resourceMethod, methodBuilder);
                stringToMethodsBuilders.put(methodUniqueId, methodBuilder);
            }
            return methodBuilder;
        }


    }

    private final Map<ResourceMethod, ResourceMethodStatistics> resourceMethods;
    private final ExecutionStatistics resourceExecutionStatistics;
    private final ExecutionStatistics requestExecutionStatistics;


    private ResourceStatisticsImpl(Map<ResourceMethod, ResourceMethodStatistics> resourceMethods,
                                   ExecutionStatistics resourceExecutionStatistics, ExecutionStatistics requestExecutionStatistics) {
        this.resourceMethods = resourceMethods;
        this.resourceExecutionStatistics = resourceExecutionStatistics;
        this.requestExecutionStatistics = requestExecutionStatistics;
    }

    @Override
    public ExecutionStatistics getResourceMethodExecutionStatistics() {
        return resourceExecutionStatistics;
    }

    @Override
    public ExecutionStatistics getRequestExecutionStatistics() {
        return requestExecutionStatistics;
    }

    @Override
    public Map<ResourceMethod, ResourceMethodStatistics> getResourceMethodStatistics() {
        return resourceMethods;
    }

    @Override
    public ResourceStatistics snapshot() {
        // snapshot functionality not yet implemented
        return this;
    }
}