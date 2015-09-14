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

import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * Sliding window time reservoir implementation that stores data of type {@link Long}.
 *
 * @author Stepan Vavra (stepan.vavra at oracle.com)
 */
class SlidingWindowTimeReservoir extends AbstractSlidingWindowTimeReservoir<Long> {

    /**
     * Creates a new sliding window time reservoir with the start time, specified time window and a custom trimmer.
     *
     * @param window        The window of startTime.
     * @param windowUnit    The unit of {@code window}.
     * @param startTime     The start time from which this reservoir calculates measurements.
     * @param startTimeUnit The start time unit.
     * @param trimmer       The trimmer to use for trimming, if {@code null}, default trimmer is used.
     */
    public SlidingWindowTimeReservoir(final long window,
                                      final TimeUnit windowUnit,
                                      final long startTime,
                                      final TimeUnit startTimeUnit,
                                      final SlidingWindowTrimmer<Long> trimmer) {
        super(window, windowUnit, startTime, startTimeUnit, trimmer);
    }

    /**
     * Creates a new sliding window time reservoir with the start time, specified time window and a default trimmer.
     *
     * @param window        The window of startTime.
     * @param windowUnit    The unit of {@code window}.
     * @param startTime     The start time from which this reservoir calculates measurements.
     * @param startTimeUnit The start time unit.
     */
    public SlidingWindowTimeReservoir(final long window,
                                      final TimeUnit windowUnit,
                                      final long startTime,
                                      final TimeUnit startTimeUnit) {
        this(window, windowUnit, startTime, startTimeUnit, null);
    }

    @Override
    protected UniformTimeSnapshot snapshot(final Collection<Long> values,
                                           final long timeInterval,
                                           final TimeUnit timeIntervalUnit,
                                           final long time,
                                           final TimeUnit timeUnit) {
        return new UniformTimeValuesSnapshot(values, timeInterval, timeIntervalUnit);
    }
}
