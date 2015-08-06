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
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A {@link TimeReservoir} implementation backed by a sliding window that stores only the measurements made in the last {@code N}
 * seconds (or other startTime unit) and allows an update with data that happened in past (which is what makes it different from
 * Dropwizard's Metrics SlidingTimeWindowReservoir.
 * <p/>
 * The snapshot this reservoir returns has limitations as mentioned in {@link TimeReservoir}.
 * <p/>
 * This reservoir is capable to store up to 2^{@link #COLLISION_BUFFER_POWER}, that is 256, in a granularity of nanoseconds. In
 * other words, up to 256 values that occurred at the same nanosecond can be stored in this reservoir. For particular nanosecond,
 * if the collision buffer exceeds, newly added values are thrown away.
 *
 * @author Stepan Vavra (stepan.vavra at oracle.com)
 * @see <pre><a href="https://github.com/dropwizard/metrics/blob/master/metrics-core/src/main/java/io/dropwizard/metrics/SlidingTimeWindowReservoir.java">Dropwizrd's Metrics SlidingTimeWindowReservoir</a></pre>
 */
public class SlidingWindowTimeReservoir implements TimeReservoir {

    // allow for 2^that many duplicate ticks before throwing away measurements
    private static final int COLLISION_BUFFER_POWER = 8;
    private static final int COLLISION_BUFFER = 1 << COLLISION_BUFFER_POWER; // 256
    // only trim on updating once every N
    private static final int TRIM_THRESHOLD = 256;

    private final ConcurrentNavigableMap<Long, Long> measurements;
    private final long window;
    private final AtomicLong greatestTick;
    private final AtomicLong updateCount;
    private final long startTick;
    private final AtomicInteger trimOff;

    /**
     * Creates a new {@link SlidingWindowTimeReservoir} with the start time and window of startTime.
     *
     * @param window     the window of startTime
     * @param windowUnit the unit of {@code window}
     */
    public SlidingWindowTimeReservoir(long window, TimeUnit windowUnit, long startTime, TimeUnit startTimeUnit) {
        this.measurements = new ConcurrentSkipListMap<>();
        this.window = windowUnit.toNanos(window) << COLLISION_BUFFER_POWER;
        this.startTick = tick(startTime, startTimeUnit);
        this.greatestTick = new AtomicLong(startTick);
        this.updateCount = new AtomicLong(0);
        this.trimOff = new AtomicInteger(0);
    }

    @Override
    public int size(long time, TimeUnit timeUnit) {
        conditionallyUpdateGreatestTick(tick(time, timeUnit));
        trim();
        return measurements.size();
    }

    @Override
    public void update(long value, long time, TimeUnit timeUnit) {
        if (updateCount.incrementAndGet() % TRIM_THRESHOLD == 0) {
            trim();
        }

        long tick = tick(time, timeUnit);
        if (greatestTick.get() - window > tick) {
            // the value is too old that it doesn't even fit into the window
            return;
        }
        for (int i = 0; i < COLLISION_BUFFER; ++i) {
            if (measurements.putIfAbsent(tick, value) == null) {
                conditionallyUpdateGreatestTick(tick);
                break;
            }
            // increase the tick, there should be up to COLLISION_BUFFER empty slots
            // where to put the value for given 'time'
            // if empty slot is not found, throw it away as we're getting inaccurate statistics anyway
            tick++;
        }
    }

    private long conditionallyUpdateGreatestTick(final long tick) {
        for (;;) {
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

    @Override
    public UniformTimeSnapshot getSnapshot(long time, TimeUnit timeUnit) {
        trimOff.incrementAndGet();
        final long baselineTick = conditionallyUpdateGreatestTick(tick(time, timeUnit));
        try {
            // now, with the 'baselineTick' we can be sure that no trim will be performed
            // we just cannot guarantee that 'time' will correspond with the 'baselineTick' which is what the API warns about
            final long measuredTickInterval = Math.min(baselineTick - startTick, window);
            final Collection<Long> values = measurements
                    .subMap((roundTick(baselineTick)) - measuredTickInterval, true, baselineTick, true)
                    .values();
            return new UniformTimeSnapshot(values, measuredTickInterval >> COLLISION_BUFFER_POWER, TimeUnit.NANOSECONDS);
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
            measurements.headMap(roundTick(baselineTick) - window).clear();
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
}
