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

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.glassfish.jersey.server.monitoring.TimeWindowStatistics;

/**
 * {@link TimeWindowStatistics Time window statistics} implementation.
 *
 * @author Miroslav Fuksa
 */
final class TimeWindowStatisticsImpl implements TimeWindowStatistics {

    /**
     * Builder of time window statistics.
     */
    static class Builder {

        private static final int DEFAULT_UNITS_PER_INTERVAL = 100;
        private static final int MINIMUM_UNIT_SIZE = 1000;

        /**
         * Total interval for which these statistics are calculated (eg. last 15 seconds, last one minute) converted to ms
         */
        private final long interval;

        /**
         * size of one unit in ms.
         */
        private final long unit;

        /**
         * How many units are in one interval.
         */
        private final int unitsPerInterval;

        /**
         * Start time of measuring statistics.
         */
        private final long startTime;

        private final Queue<Unit> unitQueue;

        /**
         * Total request count measured in the interval window.
         * Summary of {@code count} all units in {@code unitQueue}.
         */
        private long totalCount;

        /**
         * Summary of all request duration in the time window.
         */
        private long totalDuration;

        /**
         * Same meaning as {@code interval} but is calculated including rounding error.
         * It is equal to {@code unit} * {@code unitsPerInterval}.
         */
        private final long intervalWithRoundError;

        // last unit = newest unit. Following statistics are not in the queue yet (no Unit yest)
        private long lastUnitEnd;
        private long lastUnitCount;
        private long lastUnitMin = -1;
        private long lastUnitMax = -1;
        private long lastUnitDuration = 0;

        /**
         * Oldest unit that is in the queue (first that will be removed).
         */
        private Unit oldestUnit;

        private static class Unit {

            private final long count;
            private final long minimumDuration;
            private final long maximumDuration;
            private final long duration;

            private Unit(final long count, final long minimumDuration, final long maximumDuration, final long duration) {
                this.count = count;
                this.minimumDuration = minimumDuration;
                this.maximumDuration = maximumDuration;
                this.duration = duration;
            }

            private static Unit EMPTY_UNIT = new Unit(0, -1, -1, 0);
        }

        /**
         * Create a new builder instance.
         *
         * @param timeWindowSize Size of time window.
         * @param timeUnit Time units of {@code timeWindowSize}.
         */
        Builder(final long timeWindowSize, final TimeUnit timeUnit) {
            this(timeWindowSize, timeUnit, System.currentTimeMillis());
        }

        /**
         * Create a new builder instance. A constructor is used mainly for testing purposes.
         *
         * @param timeWindowSize Size of time window.
         * @param timeUnit Time units of {@code timeWindowSize}.
         * @param now Current time.
         */
        Builder(final long timeWindowSize, final TimeUnit timeUnit, final long now) {
            startTime = now;
            this.interval = timeUnit.toMillis(timeWindowSize);
            if (interval == 0) {
                // unlimited timeWindowSize
                unit = 0;
                unitsPerInterval = 0;
                intervalWithRoundError = 0;
                unitQueue = null;
            } else {
                int n = DEFAULT_UNITS_PER_INTERVAL;
                long u = interval / n;
                if (u < 1000) {
                    n = (int) interval / 1000;
                    u = interval / n;
                }
                this.unit = u;
                this.unitsPerInterval = n;
                intervalWithRoundError = unit * unitsPerInterval;
                // TODO change to array
                this.unitQueue = new LinkedList<>();

                lastUnitEnd = startTime + unit;
            }
        }

        /**
         * Add request execution.
         *
         * @param requestTime Time of execution.
         * @param duration Duration of request processing.
         */
        void addRequest(final long requestTime, final long duration) {
            closeLastUnitIfNeeded(requestTime);

            lastUnitCount++;
            lastUnitDuration += duration;

            if (duration < lastUnitMin || lastUnitMin == -1) {
                lastUnitMin = duration;
            }

            if (duration > lastUnitMax || lastUnitMax == -1) {
                lastUnitMax = duration;
            }
        }

        private void closeLastUnitIfNeeded(final long requestTime) {
            if (interval != 0) {
                if ((requestTime - lastUnitEnd) > interval + unit) {
                    resetQueue(requestTime);
                }
                if (lastUnitEnd < requestTime) {
                    // close the old unit
                    if (lastUnitCount > 0) {
                        add(new Unit(lastUnitCount, lastUnitMin, lastUnitMax, lastUnitDuration));
                    } else {
                        add(Unit.EMPTY_UNIT);
                    }
                    lastUnitEnd += unit;
                    resetLastUnit();

                    while (lastUnitEnd < requestTime) {
                        add(Unit.EMPTY_UNIT);
                        lastUnitEnd += unit;
                    }
                }
            }
        }

        private void resetLastUnit() {
            lastUnitCount = 0;
            lastUnitMin = -1;
            lastUnitMax = -1;
            lastUnitDuration = 0;
        }

        private void add(final Unit unit) {
            unitQueue.add(unit);

            // fill with empty until units
            if (unitQueue.size() > unitsPerInterval) {
                final Unit removedUnit = unitQueue.remove();
                totalCount -= removedUnit.count;
                totalDuration -= removedUnit.duration;
            }
            this.oldestUnit = unitQueue.element();
            totalCount += lastUnitCount;
            totalDuration += lastUnitDuration;
        }

        private void resetQueue(final long requestTime) {
            this.unitQueue.clear();
            lastUnitEnd = requestTime + unit;
            resetLastUnit();

            // fill with empty unit to keep result consistent
            for (int i = 0; i < unitsPerInterval; i++) {
                unitQueue.add(Unit.EMPTY_UNIT);
            }
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
            final long diff = currentTime - startTime;
            if (interval == 0) {
                if (diff < MINIMUM_UNIT_SIZE) {
                    return TimeWindowStatisticsImpl.EMPTY.get(0L);
                } else {
                    final double requestsPerSecond = (double) (1000 * lastUnitCount) / diff;
                    final long avg = lastUnitCount == 0 ? -1 : lastUnitDuration / lastUnitCount;

                    return lastUnitCount == 0
                            ? TimeWindowStatisticsImpl.EMPTY.get(0L)
                            : new TimeWindowStatisticsImpl(0, requestsPerSecond, lastUnitMin, lastUnitMax, avg, lastUnitCount);
                }
            }

            closeLastUnitIfNeeded(currentTime);
            long min = -1;
            long max = -1;
            final double requestsPerSecond;

            for (final Unit u : this.unitQueue) {
                min = getMin(min, u.minimumDuration);
                max = getMax(max, u.maximumDuration);
            }

            min = getMin(min, lastUnitMin);
            max = getMax(max, lastUnitMax);

            long adjustedTotalCount = totalCount + lastUnitCount;
            long adjustedTotalDuration = totalDuration + lastUnitDuration;

            final int size = unitQueue.size();
            if (size >= unitsPerInterval) {
                final double ratio = (currentTime - (lastUnitEnd - unit)) / ((double) unit);

                if (oldestUnit != null) {
                    adjustedTotalCount -= (long) (oldestUnit.count * ratio);
                    adjustedTotalDuration -= (long) (oldestUnit.duration * ratio);
                }

                // intervalWithRoundError is used instead of size * unit for performance reasons
                requestsPerSecond = (double) (1000 * adjustedTotalCount) / intervalWithRoundError;
            } else {
                requestsPerSecond = diff == 0 ? 0 : (double) (1000 * adjustedTotalCount) / (double) diff;
            }

            if (adjustedTotalCount == 0) {
                return getOrCreateEmptyStats(interval);
            } else {
                final long avg = adjustedTotalDuration / adjustedTotalCount;
                return new TimeWindowStatisticsImpl(interval, requestsPerSecond, min, max, avg, adjustedTotalCount);
            }
        }

        private TimeWindowStatisticsImpl getOrCreateEmptyStats(final long interval) {
            if (!EMPTY.containsKey(interval)) {
                EMPTY.putIfAbsent(interval, new TimeWindowStatisticsImpl(interval, 0, -1, -1, -1, 0));
            }
            return EMPTY.get(interval);
        }

        private long getMax(long globalMax, final long unitMax) {
            if ((unitMax > globalMax && unitMax != -1) || globalMax == -1) {
                globalMax = unitMax;
            }
            return globalMax;
        }

        private long getMin(long globalMin, final long unitMin) {
            if ((unitMin < globalMin && unitMin != -1) || globalMin == -1) {
                globalMin = unitMin;
            }
            return globalMin;
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
        // snapshot functionality not yet implemented
        return this;
    }

    @Override
    public long getAverageDuration() {
        return averageDuration;
    }
}
