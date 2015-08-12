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

/**
 * A statistical snapshot of a {@link UniformTimeSimpleSnapshot}.
 *
 * @author Stepan Vavra (stepan.vavra at oracle.com)
 * @author Dropwizard Team
 * @see <a href="https://github.com/dropwizard/metrics">https://github.com/dropwizard/metrics</a>
 */
class UniformTimeSimpleSnapshot extends AbstractTimeSnapshot {


    private final long max;
    private final long min;
    private final double mean;
    private final long count;

    /**
     * Constructs the snapshot which simply returns the provided data as arguments.
     *
     * @param max              The maximum.
     * @param min              The minimum.
     * @param mean             The mean.
     * @param count            The total count.
     * @param timeInterval     The time interval of this snapshot.
     * @param timeIntervalUnit The time interval unit.
     */
    public UniformTimeSimpleSnapshot(final long max,
                                     final long min,
                                     final double mean,
                                     final long count,
                                     final long timeInterval,
                                     final TimeUnit timeIntervalUnit) {
        super(timeInterval, timeIntervalUnit);
        this.max = max;
        this.min = min;
        this.mean = mean;
        this.count = count;
    }

    @Override
    public long size() {
        return count;
    }

    @Override
    public long getMax() {
        return max;
    }

    @Override
    public long getMin() {
        return min;
    }

    @Override
    public double getMean() {
        return mean;
    }
}
