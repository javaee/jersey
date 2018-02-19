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
 * Base implementation of {@code UniformTimeSnapshot}.
 *
 * @author Stepan Vavra (stepan.vavra at oracle.com)
 */
public abstract class AbstractTimeSnapshot implements UniformTimeSnapshot {

    private final long timeInterval;
    private final TimeUnit timeIntervalUnit;

    /**
     * Constructor to be used by subclasses overriding the base abstract uniform time snapshot class.
     *
     * @param timeInterval     The time interval of this snapshot.
     * @param timeIntervalUnit The time interval unit.
     */
    protected AbstractTimeSnapshot(final long timeInterval, final TimeUnit timeIntervalUnit) {
        this.timeInterval = timeInterval;
        this.timeIntervalUnit = timeIntervalUnit;
    }

    @Override
    public long getTimeInterval(TimeUnit timeUnit) {
        return timeUnit.convert(timeInterval, timeIntervalUnit);
    }

    @Override
    public double getRate(TimeUnit timeUnit) {
        final double rateInNanos = (double) size() / getTimeInterval(TimeUnit.NANOSECONDS);
        final long multiplier = TimeUnit.NANOSECONDS.convert(1, timeUnit);
        return rateInNanos * multiplier;
    }
}
