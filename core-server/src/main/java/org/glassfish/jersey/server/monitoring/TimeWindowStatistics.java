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

package org.glassfish.jersey.server.monitoring;

/**
 * Monitoring statistics of execution of any target (resource, resource method, application).
 * The main parameter of {@code TimeWindowStatistics} is the size of the time window. This is the time
 * for which the statistics are measured (for example for time window 1 hour, the statistics are evaluated
 * for last one hour and older statistics are dropped). The size of the time window can be retrieved by
 * {@link #getTimeWindow()}.
 * <p/>
 * Statistics retrieved from Jersey runtime might be mutable and thanks to it might provide inconsistent data
 * as not all statistics are updated in the same time. To retrieve the immutable and consistent
 * statistics data the method {@link #snapshot()} should be used.
 * <p/>
 *
 * @author Miroslav Fuksa
 * @see MonitoringStatistics See monitoring statistics for general details about statistics.
 */
public interface TimeWindowStatistics {

    /**
     * Returns the size of time window in milliseconds. Returned value denotes in how many last milliseconds
     * the statistics are evaluated.
     *
     * @return Time window in milliseconds.
     */
    public long getTimeWindow();

    /**
     * Returns average value of how many requests per second were received by application in the time window.
     *
     * @return Average of requests per second.
     */
    public double getRequestsPerSecond();

    /**
     * Returns the minimum duration (processing time) in milliseconds of the request processing measured
     * in the time window.
     * The time measures the
     * processing of the request since the start of request processing by Jersey until the response is
     * written or request processing fails and all resources for request processing are released.
     *
     * @return Minimum processing time of the request or -1 if no request has been processed.
     */
    public long getMinimumDuration();

    /**
     * Returns the maximum duration (processing time) in milliseconds of the request processing measured
     * in the time window.
     * processing of the request since the start of request processing by Jersey until the response is
     * written or request processing fails and all resources for request processing are released.
     *
     * @return Maximum processing time of the request or -1 if no request has been processed.
     */
    public long getMaximumDuration();

    /**
     * Returns the average duration (processing time) in milliseconds of the request processing measured
     * in the time window.
     * The time measures the
     * processing of the request since the start of request processing by Jersey until the response is
     * written or request processing fails and all resources for request processing are released.
     *
     * @return Average processing time of the request or -1 if no request has been processed.
     */
    public long getAverageDuration();

    /**
     * Returns the count of requests received measured in the time window.
     *
     * @return Count of requests that were handled by the application.
     */
    public long getRequestCount();

    /**
     * Get the immutable and consistent snapshot of the monitoring statistics. Working with snapshots might
     * have negative performance impact as snapshot must be created but ensures consistency of data over time.
     * However, the usage of snapshot is encouraged to avoid working with inconsistent data. Not all statistics
     * must be updated in the same time on mutable version of statistics.
     *
     * @return Snapshot of time window statistics.
     * @deprecated implementing class is immutable hence snapshot creation is not needed anymore
     */
    @Deprecated
    public TimeWindowStatistics snapshot();
}
