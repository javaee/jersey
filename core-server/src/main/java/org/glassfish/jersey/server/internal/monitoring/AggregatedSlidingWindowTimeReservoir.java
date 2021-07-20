/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015-2018 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

import org.glassfish.jersey.server.internal.monitoring.core.AbstractSlidingWindowTimeReservoir;
import org.glassfish.jersey.server.internal.monitoring.core.UniformTimeSnapshot;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * Aggregated sliding window time reservoir stores aggregated measurements in a time window of given size. The resulting snapshot
 * provides precise data as far as the granularity of aggregating trimmer is not concerned. The granularity of the trimmer
 * determines the granularity of the data the snapshot provides. In other words, the aggregated value object is either included in
 * the resulting measurements or not depending whether it was trimmed or not.
 *
 * @author Stepan Vavra (stepan.vavra at oracle.com)
 */
class AggregatedSlidingWindowTimeReservoir extends AbstractSlidingWindowTimeReservoir<AggregatedValueObject> {

    private final AggregatingTrimmer notifier;

    /**
     * Creates an aggregated sliding window reservoir.
     *
     * @param window The time size of the window
     * @param windowUnit The unit of the window size
     * @param startTime The start time from when to calculate the statistics
     * @param startTimeUnit The unit of the start time
     * @param notifier The aggregating trimmer that produces the aggregated data
     */
    public AggregatedSlidingWindowTimeReservoir(
            final long window,
            final TimeUnit windowUnit,
            final long startTime,
            final TimeUnit startTimeUnit, final AggregatingTrimmer notifier) {
        super(window, windowUnit, startTime, startTimeUnit);
        this.notifier = notifier;
        notifier.register(this);
    }

    @Override
    protected UniformTimeSnapshot snapshot(final Collection<AggregatedValueObject> values,
                                           final long timeInterval,
                                           final TimeUnit timeIntervalUnit,
                                           final long time,
                                           final TimeUnit timeUnit) {
        final UniformTimeSnapshot notTrimmedMeasurementsSnapshot = notifier.getTimeReservoirNotifier()
                .getSnapshot(time, timeUnit);

        AggregatedValueObject[] arrayValues = new AggregatedValueObject[values.size()];
        arrayValues = values.toArray(arrayValues);
        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;
        long count = 0;
        double meanNumerator = 0;

        for (AggregatedValueObject value : arrayValues) {
            min = Math.min(min, value.getMin());
            max = Math.max(max, value.getMax());
            count += value.getCount();
            meanNumerator += value.getCount() * value.getMean();
        }
        if (notTrimmedMeasurementsSnapshot.size() > 0) {
            min = Math.min(min, notTrimmedMeasurementsSnapshot.getMin());
            max = Math.max(max, notTrimmedMeasurementsSnapshot.getMax());
            count += notTrimmedMeasurementsSnapshot.size();
            meanNumerator += notTrimmedMeasurementsSnapshot.size() * notTrimmedMeasurementsSnapshot.getMean();
        }

        if (count == 0) {
            return new UniformTimeSimpleSnapshot(0, 0, 0, 0, timeInterval, timeIntervalUnit);
        } else {
            return new UniformTimeSimpleSnapshot(max, min, meanNumerator / count, count, timeInterval, timeIntervalUnit);
        }
    }

}
