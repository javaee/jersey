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
import java.util.LinkedList;

/**
 * Aggregated value object stores aggregated measurements for provided set of data. The purpose of aggregation is to avoid high
 * memory and processor time requirements for the calculation of statistics.
 *
 * @author Stepan Vavra (stepan.vavra at oracle.com)
 */
class AggregatedValueObject {

    private final long max;
    private final long min;
    private final double mean;
    private final long count;

    private AggregatedValueObject(final long max, final long min, final double mean, final long count) {
        this.max = max;
        this.min = min;
        this.mean = mean;
        this.count = count;
    }

    /**
     * Creates aggregated value object for monitoring statistics based on the provided values. During the construction, the values
     * collection must not be modified.
     *
     * @param values The collection to create the aggregated statistics from.
     * @return Aggregated value object for provided arguments.
     */
    public static AggregatedValueObject createFromValues(Collection<Long> values) {
        if (values.isEmpty()) {
            // aggregated objects must be created for at least one value, additionally, prevent from division by zero in the mean
            throw new IllegalArgumentException("The values collection must not be empty");
        }

        long max = Long.MIN_VALUE;
        long min = Long.MAX_VALUE;
        long sum = 0;
        for (Long value : values) {
            max = Math.max(max, value);
            min = Math.min(min, value);
            sum += value;
        }

        return new AggregatedValueObject(max, min, (double) sum / values.size(), values.size());
    }

    /**
     * Creates aggregated value object for monitoring statistics based on the provided collection of values. During the
     * construction, the values collection must not be modified.
     *
     * @param values The collection to create the aggregated statistics from.
     * @return Aggregated value object for provided arguments.
     */
    public static AggregatedValueObject createFromMultiValues(Collection<? extends Collection<Long>> values) {
        final Collection<Long> mergedCollection = new LinkedList<>();
        for (Collection<Long> collection : values) {
            mergedCollection.addAll(collection);
        }
        return createFromValues(mergedCollection);
    }

    /**
     * @return The maximum value of the aggregated data
     */
    public long getMax() {
        return max;
    }

    /**
     * @return The minimum value of the aggregated data
     */
    public long getMin() {
        return min;
    }

    /**
     * @return The mean of the aggregated data
     */
    public double getMean() {
        return mean;
    }

    /**
     * @return The total number of the values this aggregated data provide information about
     */
    public long getCount() {
        return count;
    }
}
