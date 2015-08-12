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
 */
package org.glassfish.jersey.server.internal.monitoring;

import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Stepan Vavra (stepan.vavra at oracle.com)
 */
public class AggregatingTrimmerTest extends AbstractNanosReservoirTest {

    private final long startTime = System.nanoTime();
    private final TimeUnit startUnitTime = TimeUnit.NANOSECONDS;
    private final AggregatingTrimmer trimmer = new AggregatingTrimmer(startTime(), startUnitTime, 10, TimeUnit.NANOSECONDS);
    private final SlidingWindowTimeReservoir time10nsReservoir = new SlidingWindowTimeReservoir(10, TimeUnit.NANOSECONDS,
            startTime(), startUnitTime, trimmer);
    private final AggregatedSlidingWindowTimeReservoir aggregatedTime100nsReservoir = new
            AggregatedSlidingWindowTimeReservoir(100, TimeUnit.NANOSECONDS, startTime(), startUnitTime, trimmer);

    protected long startTime() {
        return startTime;
    }

    @Test
    public void simpleCheck() {

        time10nsReservoir.update(10L, startTime(), startUnitTime);
        time10nsReservoir.update(20L, startTime() + 50, startUnitTime);

        checkInNanos(aggregatedTime100nsReservoir, startTime() + 100, 2, 10, 20, 15);
    }

    @Test
    public void trimSlidingWindowBeforeAggregatedWindow() {

        time10nsReservoir.update(10L, startTime(), startUnitTime);
        time10nsReservoir.update(20L, startTime() + 50, startUnitTime);

        checkInNanos(time10nsReservoir, startTime() + 100, 0, 0, 0, 0);
        checkInNanos(aggregatedTime100nsReservoir, startTime() + 100, 2, 10, 20, 15);
    }

    @Test
    public void testAggregatingTrimmer() {

        time10nsReservoir.update(10L, startTime(), startUnitTime);
        time10nsReservoir.update(20L, startTime() + 50, startUnitTime);

        checkInNanos(time10nsReservoir, startTime() + 50, 1, 20, 20, 20);
        checkInNanos(aggregatedTime100nsReservoir, startTime() + 100, 2, 10, 20, 15);
    }

    @Test
    public void testAggregatingTrimmerDoubleValues() {
        checkInNanos(aggregatedTime100nsReservoir, startTime(), 0, 0, 0, 0);

        time10nsReservoir.update(1L, startTime(), startUnitTime);
        checkInNanos(aggregatedTime100nsReservoir, startTime() + 5, 1, 1, 1, 1);

        time10nsReservoir.update(2L, startTime() + 5, startUnitTime);
        checkInNanos(aggregatedTime100nsReservoir, startTime() + 5, 2, 1, 2, 1.5);

        time10nsReservoir.update(5L, startTime() + 11, startUnitTime);
        checkInNanos(aggregatedTime100nsReservoir, startTime() + 12, 3, 1, 5, 2.6666);
        checkInNanos(aggregatedTime100nsReservoir, startTime() + 30, 3, 1, 5, 2.6666);
        checkInNanos(aggregatedTime100nsReservoir, startTime() + 100, 3, 1, 5, 2.6666);
        checkInNanos(aggregatedTime100nsReservoir, startTime() + 101, 1, 5, 5, 5);
        checkInNanos(aggregatedTime100nsReservoir, startTime() + 110, 1, 5, 5, 5);
        checkInNanos(aggregatedTime100nsReservoir, startTime() + 111, 0, 0, 0, 0);
    }

    @Test
    public void testAggregatingTrimmerMultipleDoubleValuesInOneChunk() {
        // go to the first chunk
        time10nsReservoir.update(1L, startTime() + 5, startUnitTime);
        time10nsReservoir.update(2L, startTime() + 6, startUnitTime);

        // go to the second chunk
        time10nsReservoir.update(6L, startTime() + 11, startUnitTime);
        time10nsReservoir.update(3L, startTime() + 11, startUnitTime);
        time10nsReservoir.update(11L, startTime() + 14, startUnitTime);

        checkInNanos(aggregatedTime100nsReservoir, startTime() + 14, 5, 1, 11, 23d / 5);
        checkInNanos(aggregatedTime100nsReservoir, startTime() + 14, 5, 1, 11, 23d / 5);
        checkInNanos(aggregatedTime100nsReservoir, startTime() + 15, 5, 1, 11, 23d / 5);
        checkInNanos(aggregatedTime100nsReservoir, startTime() + 16, 5, 1, 11, 23d / 5);
        checkInNanos(aggregatedTime100nsReservoir, startTime() + 100, 5, 1, 11, 23d / 5);
        checkInNanos(aggregatedTime100nsReservoir, startTime() + 101, 3, 3, 11, 20d / 3);
        checkInNanos(aggregatedTime100nsReservoir, startTime() + 110, 3, 3, 11, 20d / 3);
        checkInNanos(aggregatedTime100nsReservoir, startTime() + 111, 0, 0, 0, 0);
    }

    @Test
    public void testLowerBoundFunction() {
        for (long chunkSize = 1; chunkSize < 15; ++chunkSize) {
            for (int power = 0; power < 8; ++power) {
                for (int startTime = -50; startTime < 50; ++startTime) {
                    for (int i = -50; i < 50; ++i) {
                        long lowerBound = AggregatingTrimmer.lowerBound(i, startTime, chunkSize, power);
                        Assert.assertTrue("Error occurred for: " + i + " .. lower bound: " + lowerBound + " .. power: " + power
                                        + " .. startTime: " + startTime,
                                lowerBound <= i);
                        Assert.assertTrue("Error occurred for: " + i + " .. lower bound: " + lowerBound + " .. power: " + power
                                        + " .. startTime: " + startTime,
                                i < lowerBound + chunkSize);
                    }
                }
            }
        }
    }
}
