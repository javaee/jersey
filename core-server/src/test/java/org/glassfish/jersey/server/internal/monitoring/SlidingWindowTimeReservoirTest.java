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

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Tests of {@link SlidingWindowTimeReservoir}.
 *
 * @author Stepan Vavra (stepan.vavra at oracle.com)
 */
public class SlidingWindowTimeReservoirTest extends AbstractNanosReservoirTest {

    @Test
    public void testMultipleRequestsAtTheSameTimeZeroTime() {
        testMultipleRequestsAtTheSameTime(0);
    }

    @Test
    public void testMultipleRequestsAtTheSameTimeSystemTime() {
        testMultipleRequestsAtTheSameTime(System.nanoTime());
    }

    private void testMultipleRequestsAtTheSameTime(final long now) {
        final SlidingWindowTimeReservoir reservoir = slidingWindowTimeReservoir(now);

        // put multiple requests at the beginning so that even the COLLISION_BUFFER bounds is tested
        reservoirUpdateInNanos(reservoir, 10, now);
        reservoirUpdateInNanos(reservoir, 20, now);
        reservoirUpdateInNanos(reservoir, 30, now);
        reservoirUpdateInNanos(reservoir, 40, now);

        reservoirUpdateInNanos(reservoir, 50, now + 1);
        // put multiple requests in the middle of the window
        reservoirUpdateInNanos(reservoir, 60, now + 5);
        reservoirUpdateInNanos(reservoir, 70, now + 5);
        checkInNanos(reservoir, now + 5, 7, 10, 70, 40, 5);

        // put multiple requests at the end of the window
        reservoirUpdateInNanos(reservoir, 80, now + 10);
        reservoirUpdateInNanos(reservoir, 90, now + 10);
        checkInNanos(reservoir, now + 10, 9, 10, 90, 50);

        // at 'now + 11' all the requests from 'now' should be gone
        checkInNanos(reservoir, now + 11, 5, 50, 90, 70);
        // at 'now + 12' the '50' value should be gone as well
        checkInNanos(reservoir, now + 12, 4, 60, 90, 75);
    }

    @Test
    public void testFirstUpdateOlderThanStartTimeZeroTime() {
        testFirstUpdateOlderThanStartTime(0);
    }

    @Test
    public void testFirstUpdateOlderThanStartTimeSystemTime() {
        testFirstUpdateOlderThanStartTime(System.nanoTime());
    }

    @Test
    public void testFirstUpdateOlderThanStartTimeMinTime() {
        testFirstUpdateOlderThanStartTime(Long.MIN_VALUE);
    }

    private void testFirstUpdateOlderThanStartTime(final long now) {
        final SlidingWindowTimeReservoir reservoir = slidingWindowTimeReservoir(now);

        // put multiple requests at the beginning so that even the COLLISION_BUFFER bounds is tested
        reservoirUpdateInNanos(reservoir, 10, now - 5);
        reservoirUpdateInNanos(reservoir, 20, now - 4);
        checkInNanos(reservoir, now, 2, 10, 20, 15, 5);

        // put multiple requests at the end of the window
        reservoirUpdateInNanos(reservoir, 30, now);
        checkInNanos(reservoir, now, 3, 10, 30, 20, 5);

        reservoirUpdateInNanos(reservoir, 40, now + 1);
        checkInNanos(reservoir, now + 1, 4, 10, 40, 25, 6);

        checkInNanos(reservoir, now + 5, 4, 10, 40, 25, 10);
        checkInNanos(reservoir, now + 6, 3, 20, 40, 30, 10);
        checkInNanos(reservoir, now + 7, 2, 30, 40, 35, 10);
        checkInNanos(reservoir, now + 10, 2, 30, 40, 35, 10);
        checkInNanos(reservoir, now + 11, 1, 40, 40, 40, 10);
        checkInNanos(reservoir, now + 12, 0, 0, 0, 0, 10);

    }

    protected SlidingWindowTimeReservoir slidingWindowTimeReservoir(final long now) {
        return new SlidingWindowTimeReservoir(10, TimeUnit.NANOSECONDS, now,
                TimeUnit.NANOSECONDS);
    }

    @Test
    public void testExhaustiveRequestsAtTheSameTimeZeroTime() {
        testExhaustiveRequestsAtTheSameTime(0);
    }

    @Test
    public void testExhaustiveRequestsAtTheSameTimeSystemTime() {
        testExhaustiveRequestsAtTheSameTime(System.nanoTime());
    }

    @Test
    public void testExhaustiveRequestsAtTheSameTimeMaxTime() {
        testExhaustiveRequestsAtTheSameTime(Long.MAX_VALUE - 5);
    }

    /**
     * This test exhaustively verifies the sliding window time reservoir. Step by step, basically all its capabilities are
     * thoroughly tested.
     */
    private void testExhaustiveRequestsAtTheSameTime(final long now) {
        final SlidingWindowTimeReservoir reservoir = slidingWindowTimeReservoir(now);

        // put multiple requests at the beginning so that even the COLLISION_BUFFER bounds is tested
        for (int i = 0; i < COLLISION_BUFFER; ++i) {
            reservoirUpdateInNanos(reservoir, 10, now);
        }
        // add one more request which should not fit into the collision buffer and will be thrown away
        reservoirUpdateInNanos(reservoir, 999999, now);

        // check again at 'now + 5' before we add more values
        checkInNanos(reservoir, now + 5, COLLISION_BUFFER, 10, 10, 10, 5);

        // put multiple requests in the middle of the window
        for (int i = 0; i < COLLISION_BUFFER; ++i) {
            reservoirUpdateInNanos(reservoir, 10, now + 5);
        }
        // add one more request which should not fit into the collision buffer and will be thrown away
        reservoirUpdateInNanos(reservoir, 999999, now + 5);

        for (int i = 0; i <= 5; ++i) {
            // all the snapshots in past will return the same value as at 'now + 5'
            checkInNanos(reservoir, now + i, COLLISION_BUFFER * 2, 10, 10, 10, 5);
        }

        // add a value in past, at 'now + 1', this will also help us test that we trim the reservoir correctly
        reservoirUpdateInNanos(reservoir, 10, now + 1);

        for (int i = 0; i <= 5; ++i) {
            // all the snapshots in past will return the same value as at 'now + 5'
            checkInNanos(reservoir, now + i, COLLISION_BUFFER * 2 + 1, 10, 10, 10, 5);
        }

        // put multiple requests at the end of the window
        for (int i = 0; i < COLLISION_BUFFER; ++i) {
            reservoirUpdateInNanos(reservoir, 10, now + 10);
        }
        // add one more request which should not fit into the collision buffer and will be thrown away
        reservoirUpdateInNanos(reservoir, 999999, now + 10);

        checkInNanos(reservoir, now + 10, COLLISION_BUFFER * 3 + 1, 10, 10, 10);

        // at 'now + 11' all the requests from 'now' should be gone
        checkInNanos(reservoir, now + 11, COLLISION_BUFFER * 2 + 1, 10, 10, 10);

        // these values (from 'now') don't even fit into the reservoir, all the values will be thrown away
        for (int i = 0; i < COLLISION_BUFFER + 1; ++i) {
            reservoirUpdateInNanos(reservoir, 999999, now);
        }

        // check again at 'now + 11' to prove that values at 'now' weren't added at all
        checkInNanos(reservoir, now + 11, COLLISION_BUFFER * 2 + 1, 10, 10, 10);

        // at 'now + 12' the one additional request we added is gone
        checkInNanos(reservoir, now + 12, COLLISION_BUFFER * 2, 10, 10, 10);

        // at 'now + 15' it's the same as at 'now + 1'
        checkInNanos(reservoir, now + 15, COLLISION_BUFFER * 2, 10, 10, 10);

        // at 'now + 16' the values from 'now + 5' are gone
        checkInNanos(reservoir, now + 16, COLLISION_BUFFER, 10, 10, 10);

        // at 'now + 20' it's the the same as at 'now + 16'
        checkInNanos(reservoir, now + 20, COLLISION_BUFFER, 10, 10, 10);

        // at 'now + 21' all the requests are gone
        checkInNanos(reservoir, now + 21, 0, 0, 0, 0);
    }

}