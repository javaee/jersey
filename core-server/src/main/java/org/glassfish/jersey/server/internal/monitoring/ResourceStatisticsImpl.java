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

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.monitoring.ExecutionStatistics;
import org.glassfish.jersey.server.monitoring.ResourceMethodStatistics;
import org.glassfish.jersey.server.monitoring.ResourceStatistics;

import jersey.repackaged.com.google.common.collect.Maps;

/**
 * Resource statistics implementation.
 *
 * @author Miroslav Fuksa
 */
final class ResourceStatisticsImpl implements ResourceStatistics {

    /**
     * Builder of resource statistics instances.
     */
    static class Builder {

        private final Set<ResourceMethodStatisticsImpl.Builder> methodsBuilders = new HashSet<>();
        private final ResourceMethodStatisticsImpl.Factory methodFactory;

        private ExecutionStatisticsImpl.Builder resourceExecutionStatisticsBuilder;
        private ExecutionStatisticsImpl.Builder requestExecutionStatisticsBuilder;

        private ResourceStatisticsImpl cached;

        /**
         * Create a new builder.
         *
         * @param resource Resource for which the instance is created.
         */
        Builder(final Resource resource, final ResourceMethodStatisticsImpl.Factory methodFactory) {
            this(methodFactory);

            for (final ResourceMethod method : resource.getResourceMethods()) {
                getOrCreate(method);
            }
        }

        /**
         * Create a new builder.
         */
        Builder(final ResourceMethodStatisticsImpl.Factory methodFactory) {
            this.methodFactory = methodFactory;
        }

        /**
         * Build a new instance of {@link ResourceStatisticsImpl}.
         *
         * @return New instance of resource statistics.
         */
        ResourceStatisticsImpl build() {
            if (cached != null) {
                return cached;
            }

            final Map<ResourceMethod, ResourceMethodStatistics> resourceMethods = Maps.newHashMap();
            for (final ResourceMethodStatisticsImpl.Builder builder : methodsBuilders) {
                final ResourceMethodStatisticsImpl stats = builder.build();
                resourceMethods.put(stats.getResourceMethod(), stats);
            }

            final ExecutionStatistics resourceStats = resourceExecutionStatisticsBuilder == null
                    ? ExecutionStatisticsImpl.EMPTY : resourceExecutionStatisticsBuilder.build();
            final ExecutionStatistics requestStats = requestExecutionStatisticsBuilder == null
                    ? ExecutionStatisticsImpl.EMPTY : requestExecutionStatisticsBuilder.build();

            final ResourceStatisticsImpl stats = new ResourceStatisticsImpl(Collections.unmodifiableMap(resourceMethods),
                    resourceStats, requestStats);

            if (MonitoringUtils.isCacheable(requestStats)) {
                cached = stats;
            }

            return stats;
        }

        /**
         * Add execution of a resource method in the resource.
         *
         * @param resourceMethod Resource method executed.
         * @param methodStartTime Time of execution of the resource method.
         * @param methodDuration Time spent on execution of resource method itself.
         * @param requestStartTime Time when the request matching to the executed resource method has been received
         * by Jersey.
         * @param requestDuration Time of whole request processing (from receiving the request until writing the response).
         */
        void addExecution(final ResourceMethod resourceMethod, final long methodStartTime, final long methodDuration,
                          final long requestStartTime, final long requestDuration) {
            cached = null;

            if (resourceExecutionStatisticsBuilder == null) {
                resourceExecutionStatisticsBuilder = new ExecutionStatisticsImpl.Builder();
            }
            resourceExecutionStatisticsBuilder.addExecution(methodStartTime, methodDuration);

            if (requestExecutionStatisticsBuilder == null) {
                requestExecutionStatisticsBuilder = new ExecutionStatisticsImpl.Builder();
            }
            requestExecutionStatisticsBuilder.addExecution(requestStartTime, requestDuration);

            addMethod(resourceMethod);
        }

        /**
         * Add a resource method to the statistics.
         *
         * @param resourceMethod Resource method.
         */
        void addMethod(final ResourceMethod resourceMethod) {
            getOrCreate(resourceMethod);
        }

        private ResourceMethodStatisticsImpl.Builder getOrCreate(final ResourceMethod resourceMethod) {
            final ResourceMethodStatisticsImpl.Builder methodStats = methodFactory.getOrCreate(resourceMethod);

            if (!methodsBuilders.contains(methodStats)) {
                methodsBuilders.add(methodStats);
            }
            return methodStats;
        }

    }

    private final Map<ResourceMethod, ResourceMethodStatistics> resourceMethods;
    private final ExecutionStatistics resourceExecutionStatistics;
    private final ExecutionStatistics requestExecutionStatistics;

    private ResourceStatisticsImpl(final Map<ResourceMethod, ResourceMethodStatistics> resourceMethods,
                                   final ExecutionStatistics resourceExecutionStatistics,
                                   final ExecutionStatistics requestExecutionStatistics) {
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
