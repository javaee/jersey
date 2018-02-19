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
 * A statistical snapshot of a {@link UniformTimeSnapshot}.
 *
 * @author Stepan Vavra (stepan.vavra at oracle.com)
 * @author Dropwizard Team
 * @see <a href="https://github.com/dropwizard/metrics">https://github.com/dropwizard/metrics</a>
 */
public interface UniformTimeSnapshot {

    /**
     * Returns the number of values in the snapshot.
     *
     * @return the number of values
     */
    long size();

    /**
     * @return The maximum value in this snapshot
     */
    long getMax();

    /**
     * @return The minimum value in this snapshot
     */
    long getMin();

    /**
     * @return The mean of the values in this snapshot
     */
    double getMean();

    /**
     * The time interval for which this snapshot was created.
     *
     * @param timeUnit The time unit in which to return the time interval.
     * @return The time interval the snapshot was created at for the given time unit.
     */
    long getTimeInterval(TimeUnit timeUnit);

    /**
     * The rate of values in this snapshot for one given time unit.
     *
     * @param timeUnit The time unit at which to get the rate
     * @return The rate
     */
    double getRate(TimeUnit timeUnit);
}
