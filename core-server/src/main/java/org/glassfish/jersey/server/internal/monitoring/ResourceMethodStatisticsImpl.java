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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.monitoring.ExecutionStatistics;
import org.glassfish.jersey.server.monitoring.ResourceMethodStatistics;

/**
 * Immutable resource method statistics.
 *
 * @author Miroslav Fuksa
 * @author Stepan Vavra (stepan.vavra at oracle.com)
 */
final class ResourceMethodStatisticsImpl implements ResourceMethodStatistics {

    /**
     * Factory for creating and storing resource method statistics. One instance per resource method.
     * <p/>
     * Must be thread-safe.
     */
    static class Factory {

        private final ConcurrentMap<String, Builder> stringToMethodsBuilders = new ConcurrentHashMap<>();

        ResourceMethodStatisticsImpl.Builder getOrCreate(final ResourceMethod resourceMethod) {
            final String methodUniqueId = MonitoringUtils.getMethodUniqueId(resourceMethod);

            if (!stringToMethodsBuilders.containsKey(methodUniqueId)) {
                stringToMethodsBuilders.putIfAbsent(methodUniqueId, new ResourceMethodStatisticsImpl.Builder(resourceMethod));
            }
            return stringToMethodsBuilders.get(methodUniqueId);
        }
    }

    /**
     * Builder of resource method statistics.
     * <p/>
     * Must be thread-safe.
     */
    static class Builder {

        private final ResourceMethod resourceMethod;

        private final AtomicReference<ExecutionStatisticsImpl.Builder> resourceMethodExecutionStatisticsBuilder = new
                AtomicReference<>();
        private final AtomicReference<ExecutionStatisticsImpl.Builder> requestExecutionStatisticsBuilder = new
                AtomicReference<>();

        private volatile ResourceMethodStatisticsImpl cached;

        /**
         * Create a new builder instance.
         *
         * @param resourceMethod Resource method for which statistics are evaluated.
         */
        Builder(final ResourceMethod resourceMethod) {
            this.resourceMethod = resourceMethod;
        }

        /**
         * Build an instance of resource method statistics.
         *
         * @return New instance of resource method statistics.
         */
        ResourceMethodStatisticsImpl build() {
            ResourceMethodStatisticsImpl cachedLocalReference = cached;
            if (cachedLocalReference != null) {
                return cachedLocalReference;
            }

            final ExecutionStatistics methodStats = resourceMethodExecutionStatisticsBuilder.get() == null
                    ? ExecutionStatisticsImpl.EMPTY : resourceMethodExecutionStatisticsBuilder.get().build();
            final ExecutionStatistics requestStats = requestExecutionStatisticsBuilder.get() == null
                    ? ExecutionStatisticsImpl.EMPTY : requestExecutionStatisticsBuilder.get().build();

            final ResourceMethodStatisticsImpl stats = new ResourceMethodStatisticsImpl(resourceMethod, methodStats,
                    requestStats);

            if (MonitoringUtils.isCacheable(methodStats)) {
                // overwrite the cache regardless of whether it's null or not
                cached = stats;
            }

            return stats;
        }

        /**
         * Add execution of the resource method to the statistics.
         *
         * @param methodStartTime  Time spent on execution of resource method itself (Unix timestamp format).
         * @param methodDuration   Time of execution of the resource method.
         * @param requestStartTime Time of whole request processing (from receiving the request until writing the response). (Unix
         *                         timestamp format)
         * @param requestDuration  Time when the request matching to the executed resource method has been received by Jersey.
         */
        void addResourceMethodExecution(final long methodStartTime, final long methodDuration, final long requestStartTime,
                                        final long requestDuration) {
            cached = null;

            if (resourceMethodExecutionStatisticsBuilder.get() == null) {
                resourceMethodExecutionStatisticsBuilder.compareAndSet(null, new ExecutionStatisticsImpl.Builder());
            }
            resourceMethodExecutionStatisticsBuilder.get().addExecution(methodStartTime, methodDuration);

            if (requestExecutionStatisticsBuilder.get() == null) {
                requestExecutionStatisticsBuilder.compareAndSet(null, new ExecutionStatisticsImpl.Builder());
            }
            requestExecutionStatisticsBuilder.get().addExecution(requestStartTime, requestDuration);
        }
    }

    private final ExecutionStatistics resourceMethodExecutionStatistics;
    private final ExecutionStatistics requestExecutionStatistics;
    private final ResourceMethod resourceMethod;

    private ResourceMethodStatisticsImpl(final ResourceMethod resourceMethod,
                                         final ExecutionStatistics resourceMethodExecutionStatistics,
                                         final ExecutionStatistics requestExecutionStatistics) {
        this.resourceMethod = resourceMethod;

        this.resourceMethodExecutionStatistics = resourceMethodExecutionStatistics;
        this.requestExecutionStatistics = requestExecutionStatistics;
    }

    @Override
    public ExecutionStatistics getRequestStatistics() {
        return requestExecutionStatistics;
    }

    @Override
    public ExecutionStatistics getMethodStatistics() {
        return resourceMethodExecutionStatistics;
    }

    public ResourceMethod getResourceMethod() {
        return resourceMethod;
    }

    @Override
    public ResourceMethodStatistics snapshot() {
        // this object is immutable (not considering ResourceMethod which is not a monitoring object)
        return this;
    }
}
