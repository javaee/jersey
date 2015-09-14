/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved.
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
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 * Copyright 2010-2013 Coda Hale and Yammer, Inc., 2014-2015 Dropwizard Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.glassfish.jersey.server.internal.monitoring;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.glassfish.jersey.server.internal.monitoring.ReservoirConstants.COLLISION_BUFFER;
import static org.glassfish.jersey.server.internal.monitoring.ReservoirConstants.COLLISION_BUFFER_POWER;

/**
 * An abstract {@link TimeReservoir} implementation backed by a sliding window that stores only the measurements made in the last
 * {@code N} seconds (or other startTime unit) and allows an update with data that happened in past (which is what makes it
 * different from Dropwizard's Metrics SlidingTimeWindowReservoir.
 * <p/>
 * The snapshot this reservoir returns has limitations as mentioned in {@link TimeReservoir}.
 * <p/>
 * This reservoir is capable to store up to 2^{@link ReservoirConstants#COLLISION_BUFFER_POWER}, that is 256, in a granularity of
 * nanoseconds. In other words, up to 256 values that occurred at the same nanosecond can be stored in this reservoir. For
 * particular nanosecond, if the collision buffer exceeds, newly added values are thrown away.
 *
 * @param <V> The type of values to store in this sliding window reservoir
 * @author Stepan Vavra (stepan.vavra at oracle.com)
 * @see <pre><a href="https://github.com/dropwizard/metrics/blob/master/metrics-core/src/main/java/io/dropwizard/metrics
 * /SlidingTimeWindowReservoir.java">Dropwizard's
 * Metrics SlidingTimeWindowReservoir</a></pre>
 */
abstract class AbstractSlidingWindowTimeReservoir<V> implements TimeReservoir<V> {

    private final ConcurrentNavigableMap<Long, V> measurements;
    private final long window;
    private final AtomicLong greatestTick;
    private final AtomicLong updateCount;
    private final AtomicLong startTick;
    private final AtomicInteger trimOff;
    private final SlidingWindowTrimmer<V> trimmer;
    private final long interval;
    private final TimeUnit intervalUnit;

    /**
     * Creates a new {@link SlidingWindowTimeReservoir} with the start time and window of startTime.
     *
     * @param window        The window of startTime
     * @param windowUnit    The unit of {@code window}
     * @param startTime     The start time from which this reservoir calculates measurements
     * @param startTimeUnit The start time unit
     */
    public AbstractSlidingWindowTimeReservoir(final long window,
                                              final TimeUnit windowUnit,
                                              final long startTime,
                                              final TimeUnit startTimeUnit) {
        this(window, windowUnit, startTime, startTimeUnit, null);
    }

    /**
     * Creates a new base sliding time window reservoir with the start time and a specified time window.
     *
     * @param window        The window of startTime.
     * @param windowUnit    The unit of {@code window}.
     * @param startTime     The start time from which this reservoir calculates measurements.
     * @param startTimeUnit The start time unit.
     * @param trimmer       The trimmer to use for trimming, if {@code null}, default trimmer is used.
     */
    @SuppressWarnings("unchecked")
    public AbstractSlidingWindowTimeReservoir(final long window,
                                              final TimeUnit windowUnit,
                                              final long startTime,
                                              final TimeUnit startTimeUnit,
                                              final SlidingWindowTrimmer<V> trimmer) {
        this.trimmer = trimmer != null ? trimmer : (SlidingWindowTrimmer<V>) DefaultSlidingWindowTrimmerHolder.INSTANCE;
        this.measurements = new ConcurrentSkipListMap<>();
        this.interval = window;
        this.intervalUnit = windowUnit;
        this.window = windowUnit.toNanos(window) << COLLISION_BUFFER_POWER;
        this.startTick = new AtomicLong(tick(startTime, startTimeUnit));
        this.greatestTick = new AtomicLong(startTick.get());
        this.updateCount = new AtomicLong(0);
        this.trimOff = new AtomicInteger(0);

        this.trimmer.setTimeReservoir(this);
    }

    @Override
    public int size(long time, TimeUnit timeUnit) {
        conditionallyUpdateGreatestTick(tick(time, timeUnit));
        trim();
        return measurements.size();
    }

    @Override
    public void update(V value, long time, TimeUnit timeUnit) {
        if (updateCount.incrementAndGet() % ReservoirConstants.TRIM_THRESHOLD == 0) {
            trim();
        }

        long tick = tick(time, timeUnit);
        for (int i = 0; i < COLLISION_BUFFER; ++i) {
            if (measurements.putIfAbsent(tick, value) == null) {
                conditionallyUpdateGreatestTick(tick);
                return;
            }
            // increase the tick, there should be up to COLLISION_BUFFER empty slots
            // where to put the value for given 'time'
            // if empty slot is not found, throw it away as we're getting inaccurate statistics anyway
            tick++;
        }
    }

    @Override
    public long interval(final TimeUnit timeUnit) {
        return timeUnit.convert(interval, intervalUnit);
    }

    private long conditionallyUpdateGreatestTick(final long tick) {
        while (true) {
            final long currentGreatestTick = greatestTick.get();
            if (tick <= currentGreatestTick) {
                // the tick is too small, return the greatest one
                return currentGreatestTick;
            }
            if (greatestTick.compareAndSet(currentGreatestTick, tick)) {
                // successfully updated greatestTick with the tick
                return tick;
            }
        }
    }

    /**
     * Updates the startTick in case that the sliding window was created AFTER the time of a value that updated this window.
     *
     * @param firstEntry The first entry of the windowed measurments
     */
    private void conditionallyUpdateStartTick(final Map.Entry<Long, V> firstEntry) {
        final Long firstEntryKey = firstEntry != null ? firstEntry.getKey() : null;
        if (firstEntryKey != null && firstEntryKey < startTick.get()) {
            while (true) {
                final long expectedStartTick = startTick.get();

                if (startTick.compareAndSet(expectedStartTick, firstEntryKey)) {
                    return;
                }
            }
        }
    }

    /**
     * Subclasses are required to instantiate {@link UniformTimeSnapshot} on their own.
     *
     * @param values           The values to create the snapshot from
     * @param timeInterval     The time interval this snapshot conforms to
     * @param timeIntervalUnit The interval unit of the time interval
     * @param time             The time of the request of the snapshot
     * @param timeUnit         The unit of the time of the snapshot request
     * @return The snapshot
     */
    abstract UniformTimeSnapshot snapshot(final Collection<V> values,
                                          final long timeInterval,
                                          final TimeUnit timeIntervalUnit,
                                          final long time,
                                          final TimeUnit timeUnit);

    @Override
    public UniformTimeSnapshot getSnapshot(long time, TimeUnit timeUnit) {
        trimOff.incrementAndGet();
        final long baselineTick = conditionallyUpdateGreatestTick(tick(time, timeUnit));
        try {
            // now, with the 'baselineTick' we can be sure that no trim will be performed
            // we just cannot guarantee that 'time' will correspond with the 'baselineTick' which is what the API warns about
            final ConcurrentNavigableMap<Long, V> windowMap = measurements
                    .subMap((roundTick(baselineTick)) - window, true, baselineTick, true);

            // if the first update came with value lower that the 'startTick' we need to extend the window size so that the
            // calculation depending on the actual measured interval is not unnecessary boosted
            conditionallyUpdateStartTick(windowMap.firstEntry());

            // calculate the actual measured interval
            final long measuredTickInterval = Math.min(baselineTick - startTick.get(), window);

            return snapshot(windowMap.values(), measuredTickInterval >> COLLISION_BUFFER_POWER,
                    TimeUnit.NANOSECONDS, time, timeUnit);
        } finally {
            trimOff.decrementAndGet();
            trim(baselineTick);
        }
    }

    private long tick(long time, TimeUnit timeUnit) {
        return timeUnit.toNanos(time) << COLLISION_BUFFER_POWER;
    }

    private void trim() {
        trim(greatestTick.get());
    }

    private void trim(final long baselineTick) {
        if (trimEnabled()) {
            final long key = roundTick(baselineTick) - window;
            trimmer.trim(measurements, key);
        }
    }

    private boolean trimEnabled() {
        return trimOff.get() == 0;
    }

    /**
     * The purpose of this method is to deal with the fact that data for the same nanosecond can be distributed in an interval
     * [0,256). By rounding the tick, we get the tick to which all the other ticks from the same interval belong.
     *
     * @param tick The tick
     * @return The rounded tick
     */
    private long roundTick(final long tick) {
        // tick / COLLISION_BUFFER * COLLISION_BUFFER
        return tick >> COLLISION_BUFFER_POWER << COLLISION_BUFFER_POWER;
    }

    /**
     * The holder of the lazy loaded instance of the default trimmer.
     */
    private static final class DefaultSlidingWindowTrimmerHolder {

        /**
         * The default instance of sliding window trimmer.
         */
        static final SlidingWindowTrimmer<Object> INSTANCE = new SlidingWindowTrimmer<Object>() {
            @Override
            public void trim(final ConcurrentNavigableMap<Long, Object> map, final long key) {
                map.headMap(key).clear();
            }

            @Override
            public void setTimeReservoir(final TimeReservoir<Object> reservoir) {
                // not used
            }
        };
    }
}
