/*
 * Copyright (c) 2015-2018 Oracle and/or its affiliates. All rights reserved.
 * Copyright 2010-2013 Coda Hale and Yammer, Inc.
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

package org.glassfish.jersey.server.internal.monitoring.core;

import java.util.concurrent.TimeUnit;

/**
 * A statistically representative reservoir of a data stream in time.
 * <p/>
 * Compared to Dropwizard Reservoir, this interface adds a possibility to work with data that is associated with a specific
 * time. It may not be possible; however, to obtain a snapshot or size at some moment in past due to performance optimizations.
 *
 * @author Stepan Vavra (stepan.vavra at oracle.com)
 * @see <a href="https://github.com/dropwizard/metrics">https://github.com/dropwizard/metrics</a>
 */
public interface TimeReservoir<V> {

    /**
     * Returns the number of values recorded at given time or newer. It may not be supported to return a size in past due to
     * performance optimizations.
     *
     * @param time     The time to get the size for
     * @param timeUnit Time unit of the provided time
     * @return the number of values recorded for given time or newer
     */
    int size(long time, TimeUnit timeUnit);

    /**
     * Adds a new recorded value to the reservoir bound to a given time.
     *
     * @param value    a new recorded value
     * @param time     The time the recorded value occurred at
     * @param timeUnit Time unit of the provided time
     */
    void update(V value, long time, TimeUnit timeUnit);

    /**
     * Returns a snapshot of the reservoir's values at given time or newer. It may not be supported to return a snapshot in past
     * due to performance optimizations.
     *
     * @param time     The time for which to get the snapshot
     * @param timeUnit Time unit of the provided time
     * @return a snapshot of the reservoir's values for given time or newer
     */
    UniformTimeSnapshot getSnapshot(long time, TimeUnit timeUnit);

    /**
     * The time interval this reservoir stores data of.
     *
     * @param timeUnit The time unit in which to get the interval
     * @return The time interval of this time reservoir
     */
    long interval(TimeUnit timeUnit);
}
