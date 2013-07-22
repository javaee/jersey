/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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
import java.util.concurrent.TimeUnit;

import org.glassfish.jersey.server.monitoring.TimeWindowStatistics;

/**
 * {@link TimeWindowStatistics Time window statistics} implementation.
 *
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 */
public class TimeWindowStatisticsImpl implements TimeWindowStatistics {


    /**
     * Builder of time window statistics.
     */
    static class Builder {
        private static final int DEFAULT_UNITS_PER_INTERVAL = 100;
        private static final int MINIMUM_UNIT_SIZE = 1000;
        private final long interval;
        private final long unit;
        private final int unitsPerInterval;
        private final long startTime;
        private final Queue<Unit> unitQueue;
        private long totalCount;
        private long totalDuration;
        private final long intervalWithRoundError;

        private long lastUnitEnd;
        private long lastUnitCount;
        private long lastUnitMin = -1;
        private long lastUnitMax = -1;
        private long lastUnitDuration = 0;

        private static class Unit {
            private final long count;
            private final long minimumDuration;
            private final long maximumDuration;
            private final long duration;

            private Unit(long count, long minimumDuration, long maximumDuration, long duration) {
                this.count = count;
                this.minimumDuration = minimumDuration;
                this.maximumDuration = maximumDuration;
                this.duration = duration;
            }

            private static Unit EMPTY_UNIT = new Unit(0, -1, -1, 0);
        }

        /**
         * Create a new builder instance.
         * @param timeWindowSize Size of time window.
         * @param timeUnit Time units of {@code timeWindowSize}.
         */
        Builder(long timeWindowSize, TimeUnit timeUnit) {
            this(timeWindowSize, timeUnit, System.currentTimeMillis());
        }


        /**
         * Create a new builder instance. A constructor is used mainly for testing purposes.
         * @param timeWindowSize Size of time window.
         * @param timeUnit Time units of {@code timeWindowSize}.
         * @param now Current time.
         */
        Builder(long timeWindowSize, TimeUnit timeUnit, long now) {
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
                this.unitQueue = new LinkedList<Unit>();

                lastUnitEnd = startTime + unit;
            }
        }

        /**
         * Add request execution.
         * @param requestTime Time of execution.
         * @param duration Duration of request processing.
         */
        void addRequest(long requestTime, long duration) {
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

        private void closeLastUnitIfNeeded(long requestTime) {
            if (interval != 0) {
                if ((requestTime - lastUnitEnd) > interval + unit) {
                    resetQueue(requestTime);
                }
                if (lastUnitEnd < requestTime) {
                    // close the old unit
                    add(new Unit(lastUnitCount, lastUnitMin, lastUnitMax, lastUnitDuration));
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

        private void add(Unit unit) {
            unitQueue.add(unit);

            // fill with empty until units
            if (unitQueue.size() > unitsPerInterval) {
                final Unit removedUnit = unitQueue.remove();
                totalCount -= removedUnit.count;
                totalDuration -= removedUnit.duration;
            }
            totalCount += lastUnitCount;
            totalDuration += lastUnitDuration;
        }

        private void resetQueue(long requestTime) {
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
        TimeWindowStatisticsImpl build(long currentTime) {
            if (interval == 0) {
                final long diff = currentTime - startTime;
                if (diff < MINIMUM_UNIT_SIZE) {
                    return new TimeWindowStatisticsImpl(interval, 0, 0, 0, 0, 0);
                } else {
                    double requestsPerSecond = (double) (1000 * lastUnitCount) / diff;
                    long avg = lastUnitCount == 0 ? -1 : lastUnitDuration / lastUnitCount;
                    return new TimeWindowStatisticsImpl(interval, requestsPerSecond, lastUnitMin, lastUnitMax, avg, lastUnitCount);
                }
            }

            closeLastUnitIfNeeded(currentTime);
            long min = -1;
            long max = -1;
            double requestsPerSecond;

            for (final Unit u : this.unitQueue) {
                if ((u.minimumDuration < min && u.minimumDuration != -1) || min == -1) {
                    min = u.minimumDuration;
                }
                if ((u.maximumDuration > max && u.maximumDuration != -1) || max == -1) {
                    max = u.maximumDuration;
                }
            }

            final int size = unitQueue.size();
            if (size >= unitsPerInterval) {
                // intervalWithRoundError is used instead of size * unit for performance reasons
                requestsPerSecond = (double) (1000 * totalCount) / intervalWithRoundError;
            } else {
                requestsPerSecond = size == 0 ? 0d : (double) (1000 * totalCount) / (size * unit);
            }

            long avg = totalCount == 0 ? -1 : totalDuration / totalCount;
            return new TimeWindowStatisticsImpl(interval, requestsPerSecond, min, max, avg, totalCount);
        }

        public long getInterval() {
            return interval;
        }
    }

    private final long interval;
    private final double requestsPerSecond;

    private final long minimumDuration;
    private final long maximumDuration;
    private final long averageDuration;

    private long totalCount;


    private TimeWindowStatisticsImpl(long interval, double requestsPerSecond, long minimumDuration,
                                     long maximumDuration, long averageDuration, long totalCount) {
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
