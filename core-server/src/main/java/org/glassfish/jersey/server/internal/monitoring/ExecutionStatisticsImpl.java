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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.glassfish.jersey.server.monitoring.ExecutionStatistics;
import org.glassfish.jersey.server.monitoring.TimeWindowStatistics;

import jersey.repackaged.com.google.common.collect.Maps;

/**
 * Immutable Execution statistics.
 *
 * @author Miroslav Fuksa
 * @author Stepan Vavra (stepan.vavra at oracle.com)
 */
final class ExecutionStatisticsImpl implements ExecutionStatistics {

    /**
     * Builder of execution statistics.
     * <p/>
     * Must be thread-safe.
     */
    static class Builder {

        private volatile long lastStartTime;
        private final Map<Long, TimeWindowStatisticsImpl.Builder> intervalStatistics;

        /**
         * Create a new builder.
         */
        public Builder() {
            // create unmodifiable map to ensure that an iteration in the build() won't have multi-threading issues
            this.intervalStatistics = Collections.unmodifiableMap(new HashMap<Long, TimeWindowStatisticsImpl.Builder>(6) {
                {
                    addInterval(0, TimeUnit.MILLISECONDS, this);
                    addInterval(1, TimeUnit.SECONDS, this);
                    addInterval(15, TimeUnit.SECONDS, this);
                    addInterval(1, TimeUnit.MINUTES, this);
                    addInterval(15, TimeUnit.MINUTES, this);
                    addInterval(1, TimeUnit.HOURS, this);
                }
            });
        }

        private void addInterval(final long interval, final TimeUnit timeUnit, final Map map) {
            final long intervalInMillis = timeUnit.toMillis(interval);
            map.put(intervalInMillis, new TimeWindowStatisticsImpl.Builder(intervalInMillis,
                    TimeUnit.MILLISECONDS));
        }

        /**
         * Add execution of a target.
         *
         * @param startTime (Unix timestamp format)
         * @param duration  Duration of target execution in milliseconds.
         */
        void addExecution(final long startTime, final long duration) {
            for (final TimeWindowStatisticsImpl.Builder statBuilder : intervalStatistics.values()) {
                statBuilder.addRequest(startTime, duration);
            }

            this.lastStartTime = startTime;
        }

        /**
         * Build a new instance of execution statistics.
         *
         * @return new instance of execution statistics.
         */
        public ExecutionStatisticsImpl build() {
            final Map<Long, TimeWindowStatistics> newIntervalStatistics = Maps.newHashMap();
            for (final Map.Entry<Long, TimeWindowStatisticsImpl.Builder> builderEntry : intervalStatistics.entrySet()) {
                newIntervalStatistics.put(builderEntry.getKey(), builderEntry.getValue().build());
            }

            // cache when request rate is 0

            return new ExecutionStatisticsImpl(lastStartTime, newIntervalStatistics);
        }
    }

    static final ExecutionStatistics EMPTY = new Builder().build();

    private final long lastStartTime;
    private final Map<Long, TimeWindowStatistics> timeWindowStatistics;

    @Override
    public Date getLastStartTime() {
        return new Date(lastStartTime);
    }

    @Override
    public Map<Long, TimeWindowStatistics> getTimeWindowStatistics() {
        return timeWindowStatistics;
    }

    @Override
    public ExecutionStatistics snapshot() {
        // this object is immutable (TimeWindowStatistics are immutable as well)
        return this;
    }

    private ExecutionStatisticsImpl(final long lastStartTime, final Map<Long, TimeWindowStatistics> timeWindowStatistics) {
        this.lastStartTime = lastStartTime;
        this.timeWindowStatistics = Collections.unmodifiableMap(timeWindowStatistics);
    }

}
