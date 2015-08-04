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
import java.util.HashMap;
import java.util.Map;

import org.glassfish.jersey.server.monitoring.ExceptionMapperStatistics;

import jersey.repackaged.com.google.common.collect.Maps;

/**
 * Exception mapper statistics.
 *
 * @author Miroslav Fuksa
 */
final class ExceptionMapperStatisticsImpl implements ExceptionMapperStatistics {

    /**
     * Builder of exception mapper statistics.
     * <p/>
     * This builder does not need to be threadsafe since it's called only from the jersey-background-task-scheduler.
     */
    static class Builder {

        private Map<Class<?>, Long> exceptionMapperExecutionCountMap = Maps.newHashMap();
        private long successfulMappings;
        private long unsuccessfulMappings;
        private long totalMappings;

        private ExceptionMapperStatisticsImpl cached;

        /**
         * Add mappings.
         *
         * @param success True if mappings were successful.
         * @param count Number of mappings.
         */
        void addMapping(final boolean success, final int count) {
            cached = null;

            totalMappings++;
            if (success) {
                successfulMappings += count;
            } else {
                unsuccessfulMappings += count;
            }
        }

        /**
         * Add an execution of exception mapper.
         *
         * @param mapper Exception mapper.
         * @param count Number of executions of the {@code mapper}.
         */
        void addExceptionMapperExecution(final Class<?> mapper, final int count) {
            cached = null;

            Long cnt = exceptionMapperExecutionCountMap.get(mapper);
            cnt = cnt == null ? count : cnt + count;
            exceptionMapperExecutionCountMap.put(mapper, cnt);
        }

        /**
         * Build an instance of exception mapper statistics.
         *
         * @return New instance of exception mapper statistics.
         */
        public ExceptionMapperStatisticsImpl build() {
            if (cached == null) {
                cached = new ExceptionMapperStatisticsImpl(new HashMap<>(this.exceptionMapperExecutionCountMap),
                        successfulMappings, unsuccessfulMappings, totalMappings);
            }

            return cached;
        }
    }

    private final Map<Class<?>, Long> exceptionMapperExecutionCount;
    private final long successfulMappings;
    private final long unsuccessfulMappings;
    private final long totalMappings;

    private ExceptionMapperStatisticsImpl(final Map<Class<?>, Long> exceptionMapperExecutionCount, final long successfulMappings,
                                          final long unsuccessfulMappings, final long totalMappings) {
        this.exceptionMapperExecutionCount = Collections.unmodifiableMap(exceptionMapperExecutionCount);
        this.successfulMappings = successfulMappings;
        this.unsuccessfulMappings = unsuccessfulMappings;
        this.totalMappings = totalMappings;
    }

    @Override
    public Map<Class<?>, Long> getExceptionMapperExecutions() {
        return exceptionMapperExecutionCount;
    }

    @Override
    public long getSuccessfulMappings() {
        return successfulMappings;
    }

    @Override
    public long getUnsuccessfulMappings() {
        return unsuccessfulMappings;
    }

    @Override
    public long getTotalMappings() {
        return totalMappings;
    }

    @Override
    public ExceptionMapperStatistics snapshot() {
        // snapshot functionality not yet implemented
        return this;
    }

}
