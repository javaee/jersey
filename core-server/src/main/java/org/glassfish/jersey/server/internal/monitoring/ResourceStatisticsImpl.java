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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.monitoring.ExecutionStatistics;
import org.glassfish.jersey.server.monitoring.ResourceMethodStatistics;
import org.glassfish.jersey.server.monitoring.ResourceStatistics;

import jersey.repackaged.com.google.common.collect.Maps;

/**
 * Immutable resource statistics implementation.
 *
 * @author Miroslav Fuksa
 * @author Stepan Vavra (stepan.vavra at oracle.com)
 */
final class ResourceStatisticsImpl implements ResourceStatistics {

    /**
     * Builder of resource statistics instances.
     * <p/>
     * Must be thread-safe.
     */
    static class Builder {

        private final ConcurrentMap<ResourceMethodStatisticsImpl.Builder, Boolean> methodsBuilders = new ConcurrentHashMap<>();
        private final ResourceMethodStatisticsImpl.Factory methodFactory;

        private final AtomicReference<ExecutionStatisticsImpl.Builder> resourceExecutionStatisticsBuilder = new
                AtomicReference<>();
        private final AtomicReference<ExecutionStatisticsImpl.Builder> requestExecutionStatisticsBuilder = new
                AtomicReference<>();

        private volatile ResourceStatisticsImpl cached;

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
         * <p/>
         * Note that this build method is called from various different threads.
         *
         * @return New instance of resource statistics.
         */
        ResourceStatisticsImpl build() {
            ResourceStatisticsImpl cachedReference = cached;
            if (cachedReference != null) {
                return cachedReference;
            }

            final Map<ResourceMethod, ResourceMethodStatistics> resourceMethods = Maps.newHashMap();
            for (final ResourceMethodStatisticsImpl.Builder builder : methodsBuilders.keySet()) {
                final ResourceMethodStatisticsImpl stats = builder.build();
                resourceMethods.put(stats.getResourceMethod(), stats);
            }

            final ExecutionStatistics resourceStats = resourceExecutionStatisticsBuilder.get() == null
                    ? ExecutionStatisticsImpl.EMPTY : resourceExecutionStatisticsBuilder.get().build();
            final ExecutionStatistics requestStats = requestExecutionStatisticsBuilder.get() == null
                    ? ExecutionStatisticsImpl.EMPTY : requestExecutionStatisticsBuilder.get().build();

            final ResourceStatisticsImpl stats = new ResourceStatisticsImpl(resourceMethods,
                    resourceStats, requestStats);

            if (MonitoringUtils.isCacheable(requestStats)) {
                cached = stats;
            }

            return stats;
        }

        /**
         * Add execution of a resource method in the resource.
         *
         * @param resourceMethod   Resource method executed.
         * @param methodStartTime  Time of execution of the resource method.
         * @param methodDuration   Time spent on execution of resource method itself.
         * @param requestStartTime Time when the request matching to the executed resource method has been received by Jersey.
         * @param requestDuration  Time of whole request processing (from receiving the request until writing the response).
         */
        void addExecution(final ResourceMethod resourceMethod, final long methodStartTime, final long methodDuration,
                          final long requestStartTime, final long requestDuration) {
            cached = null;

            if (resourceExecutionStatisticsBuilder.get() == null) {
                resourceExecutionStatisticsBuilder.compareAndSet(null, new ExecutionStatisticsImpl.Builder());
            }
            resourceExecutionStatisticsBuilder.get().addExecution(methodStartTime, methodDuration);

            if (requestExecutionStatisticsBuilder.get() == null) {
                requestExecutionStatisticsBuilder.compareAndSet(null, new ExecutionStatisticsImpl.Builder());
            }
            requestExecutionStatisticsBuilder.get().addExecution(requestStartTime, requestDuration);

            addMethod(resourceMethod);
        }

        /**
         * Add a resource method to the statistics.
         *
         * @param resourceMethod Resource method.
         */
        void addMethod(final ResourceMethod resourceMethod) {
            cached = null;

            getOrCreate(resourceMethod);
        }

        private ResourceMethodStatisticsImpl.Builder getOrCreate(final ResourceMethod resourceMethod) {
            final ResourceMethodStatisticsImpl.Builder methodStats = methodFactory.getOrCreate(resourceMethod);

            methodsBuilders.putIfAbsent(methodStats, Boolean.TRUE);
            return methodStats;
        }

    }

    private final Map<ResourceMethod, ResourceMethodStatistics> resourceMethods;
    private final ExecutionStatistics resourceExecutionStatistics;
    private final ExecutionStatistics requestExecutionStatistics;

    private ResourceStatisticsImpl(final Map<ResourceMethod, ResourceMethodStatistics> resourceMethods,
                                   final ExecutionStatistics resourceExecutionStatistics,
                                   final ExecutionStatistics requestExecutionStatistics) {
        this.resourceMethods = Collections.unmodifiableMap(resourceMethods);
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
        // this object is immutable and all the other accessible objects as well
        return this;
    }
}
