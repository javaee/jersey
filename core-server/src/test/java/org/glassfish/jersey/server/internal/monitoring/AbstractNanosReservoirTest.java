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

import static org.junit.Assert.assertEquals;

/**
 * @author Stepan Vavra (stepan.vavra at oracle.com)
 */
public class AbstractNanosReservoirTest {

    protected static final double DELTA = 0.0001;
    protected static final int COLLISION_BUFFER = 256;

    protected void reservoirUpdateInNanos(TimeReservoir reservoir, long value, long time) {
        reservoir.update(value, time, TimeUnit.NANOSECONDS);
    }

    protected void checkInNanos(final TimeReservoir reservoir,
                                final long snapshotTime,
                                final int expectedSize,
                                final int expectedMin,
                                final int expectedMax,
                                final double expectedMean) {
        checkInNanos(reservoir, snapshotTime, expectedSize, expectedMin, expectedMax, expectedMean,
                reservoir.getSnapshot(snapshotTime, TimeUnit.NANOSECONDS).getTimeInterval(TimeUnit.NANOSECONDS));
    }

    /**
     * Checks whether the snapshot of given reservoir exhibits with expected measurements.
     *
     * @param reservoir        The reservoir to assert.
     * @param snapshotTime     The time for which to get the snapshot
     * @param expectedSize     Expected size of the snapshot
     * @param expectedMin      Expected minimum
     * @param expectedMax      Expected maximum
     * @param expectedMean     Expected mean
     * @param expectedInterval Expected interval
     */
    protected void checkInNanos(final TimeReservoir reservoir,
                                final long snapshotTime,
                                final long expectedSize,
                                final long expectedMin,
                                final long expectedMax,
                                final double expectedMean, final long expectedInterval) {
        final UniformTimeSnapshot snapshot = reservoir.getSnapshot(snapshotTime, TimeUnit.NANOSECONDS);

        assertEquals("Total count does not match!", expectedSize, snapshot.size());
        assertEquals("Min exec time does not match!", expectedMin, snapshot.getMin());
        assertEquals("Max exec time does not match!", expectedMax, snapshot.getMax());
        assertEquals("Average exec time does not match!", expectedMean, snapshot.getMean(), DELTA);
        assertEquals("Expected interval does not match!", expectedInterval, snapshot.getTimeInterval(TimeUnit.NANOSECONDS));
    }

}
