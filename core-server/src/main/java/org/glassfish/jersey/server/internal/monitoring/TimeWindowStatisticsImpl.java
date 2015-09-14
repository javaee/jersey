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
import java.util.concurrent.TimeUnit;

import org.glassfish.jersey.server.monitoring.TimeWindowStatistics;

/**
 * Immutable {@link TimeWindowStatistics Time window statistics} that uses backing {@link SlidingWindowTimeReservoir} for its
 * {@code Builder} implementation.
 *
 * @author Miroslav Fuksa
 * @author Stepan Vavra (stepan.vavra at oracle.com)
 */
final class TimeWindowStatisticsImpl implements TimeWindowStatistics {

    /**
     * Builder of time window statistics.
     */
    static class Builder<V> {

        /**
         * Total interval for which these statistics are calculated (eg. last 15 seconds, last one minute) converted to ms
         */
        private final long interval;

        private final TimeReservoir<V> timeReservoir;

        /**
         * Create new time window statistics builder instance.
         *
         * @param timeReservoir statistically representative reservoir of long values data stream in time.
         */
        Builder(TimeReservoir<V> timeReservoir) {
            interval = timeReservoir.interval(TimeUnit.MILLISECONDS);
            this.timeReservoir = timeReservoir;
        }

        /**
         * Add request execution.
         *
         * @param requestTime Time of execution.
         * @param duration    Duration of request processing.
         */
        void addRequest(final long requestTime, final V duration) {
            timeReservoir.update(duration, requestTime, TimeUnit.MILLISECONDS);
        }

        /**
         * Build the time window statistics instance.
         *
         * @return New instance of statistics.
         */
        TimeWindowStatisticsImpl build() {
            return build(System.currentTimeMillis());
        }

        /**
         * Build the time window statistics instance.
         *
         * @param currentTime Current time as a reference to which the statistics should be built.
         * @return New instance of statistics.
         */
        TimeWindowStatisticsImpl build(final long currentTime) {
            final UniformTimeSnapshot durationReservoirSnapshot = timeReservoir
                    .getSnapshot(currentTime, TimeUnit.MILLISECONDS);

            // if nothing was collected, return a single empty stat instance
            if (durationReservoirSnapshot.size() == 0) {
                return getOrCreateEmptyStats(interval);
            }

            return new TimeWindowStatisticsImpl(interval, durationReservoirSnapshot);

        }

        private TimeWindowStatisticsImpl getOrCreateEmptyStats(final long interval) {
            if (!EMPTY.containsKey(interval)) {
                EMPTY.putIfAbsent(interval, new TimeWindowStatisticsImpl(interval, 0, -1, -1, -1, 0));
            }
            return EMPTY.get(interval);
        }

        public long getInterval() {
            return interval;
        }
    }

    private static final ConcurrentHashMap<Long, TimeWindowStatisticsImpl> EMPTY = new ConcurrentHashMap<>(6);

    static {
        EMPTY.putIfAbsent(0L, new TimeWindowStatisticsImpl(0, 0, 0, 0, 0, 0));
    }

    private final long interval;

    private final long minimumDuration;
    private final long maximumDuration;
    private final long averageDuration;

    private final long totalCount;
    private final double requestsPerSecond;

    private TimeWindowStatisticsImpl(final long interval, final double requestsPerSecond, final long minimumDuration,
                                     final long maximumDuration, final long averageDuration, final long totalCount) {
        this.interval = interval;
        this.requestsPerSecond = requestsPerSecond;
        this.minimumDuration = minimumDuration;
        this.maximumDuration = maximumDuration;
        this.averageDuration = averageDuration;
        this.totalCount = totalCount;
    }

    private TimeWindowStatisticsImpl(final long interval, final UniformTimeSnapshot snapshot) {
        this(interval, snapshot.getRate(TimeUnit.SECONDS), snapshot.getMin(), snapshot.getMax(), (long) snapshot.getMean(),
                snapshot.size());
    }

    @Override
    public long getTimeWindow() {
        return interval;
    }

    @Override
    public double getRequestsPerSecond() {
        return requestsPerSecond;
    }

    @Override
    public long getMinimumDuration() {
        return minimumDuration;
    }

    @Override
    public long getMaximumDuration() {
        return maximumDuration;
    }

    @Override
    public long getRequestCount() {
        return totalCount;
    }

    @Override
    public TimeWindowStatistics snapshot() {
        // TimeWindowStatisticsImpl is immutable; the Builder is mutable
        return this;
    }

    @Override
    public long getAverageDuration() {
        return averageDuration;
    }
}
