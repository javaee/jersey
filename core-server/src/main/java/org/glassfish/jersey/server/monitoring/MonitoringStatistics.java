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

import java.util.Map;


/**
 * Monitoring statistics return statistic information about application run like number of requests received,
 * duration of request processing, number of successfully processed requests, statistical information about
 * execution of methods and resources, information about matching.
 * <p/>
 * Monitoring statistics is the main interface from which all statistic information can be retrieved. Statistics
 * can be retried in two ways: these can be injected or received from registered callback
 * interface {@link MonitoringStatisticsListener}. The following is the example of statistic injection:
 * <pre>
 *   &#064;Path("resource")
 *   public static class StatisticsTest {
 *       &#064;Inject
 *       Provider&lt;MonitoringStatistics&gt; statistics;
 *
 *       &#064;GET
 *       public long getTotalExceptionMappings() throws InterruptedException {
 *           final MonitoringStatistics monitoringStatistics = statistics.get();
 *           final long totalExceptionMappings = monitoringStatistics.getExceptionMapperStatistics().getTotalMappings();
 *
 *           return totalExceptionMappings;
 *       }
 *   }
 * </pre>
 * Note usage of {@link javax.inject.Provider} to retrieve statistics. Statistics change over time and this will
 * inject the latest statistics. In the case of singleton resources usage of {@code Provider} is the only way how
 * to inject statistics that are up to date.
 * <p/>
 * Retrieving statistics by {@code MonitoringStatisticsListener} is convenient in cases when there is a need
 * to take an action only when new statistics are calculated which occurs in not defined irregular intervals
 * (once per second for example).
 * <p/>
 * The contract does not mandate {@code MonitoringStatistics} to be immutable. Implementation of monitoring statistics
 * might be mutable, which means that an instance of {@code MonitoringStatistics}
 * might change its internal state over time. In order to get immutable snapshot of statistics
 * the method {@link #snapshot()} must be called to get a snapshot of the statistics that guarantees
 * that data to be immutable and consistent. Nested statistics interfaces contain also {@code snapshot} method which
 * can be used in the same way.
 * Note that a snapshot of {@code MonitoringStatistics} performs a deep snapshot of nested statistics object too, so there
 * is no need to call the {@code snapshot} method again on nested statistics components.
 * <p>
 * The implementation of this interface may be mutable and change it's state by an external event, however it is guaranteed
 * to be thread-safe.
 * </p>
 *
 * @author Miroslav Fuksa
 */
public interface MonitoringStatistics {
    /**
     * Get the statistics for each URI that is exposed in the application. Keys of returned map
     * are String URIs (for example "/bookstore/users/admin") and values are
     * {@link ResourceStatistics resource statistics} that contain information about
     * execution of resource methods available on the URI. The map contain URIs that are available in
     * application without URIs available in sub resource locators and URIs that are available trough sub
     * resource locators and were already matched by any request.
     *
     * @return Map with URI keys and resource statistics values.
     */
    public Map<String, ResourceStatistics> getUriStatistics();

    /**
     * Get the statistics for each resource {@link Class} that is deployed in the application. Keys of returned
     * map are classes of resources and values are {@link ResourceStatistics resource statistics}
     * that contain information about
     * execution of resource methods available in the resource class. Note that one resource class can serve
     * request matched to different URIs. By default the map will contain resource classes which are registered
     * in the resource model plus resource classes of sub resources returned from sub resource locators.
     *
     * @return Map with resource class keys and resource statistics values.
     */
    public Map<Class<?>, ResourceStatistics> getResourceClassStatistics();

    /**
     * Get the global application statistics of request execution. The statistics are not bound any specific resource or
     * resource method and contains information about all requests that application handles.
     *
     * @return Application request execution statistics.
     */
    public ExecutionStatistics getRequestStatistics();


    /**
     * Get global application response statistics. The statistics are not bound any specific resource or
     * resource method and contains information about all responses that application creates.
     *
     * @return Application response statistics.
     */
    public ResponseStatistics getResponseStatistics();

    /**
     * Get statistics about registered {@link javax.ws.rs.ext.ExceptionMapper exception mappers}.
     *
     * @return Exception mapper statistics.
     */
    public ExceptionMapperStatistics getExceptionMapperStatistics();

    /**
     * Get the immutable consistent snapshot of the monitoring statistics. Working with snapshots might
     * have negative performance impact as snapshot must be created but ensures consistency of data over time.
     * However, the usage of snapshot is encouraged to avoid working with inconsistent data. Not all statistics
     * must be updated in the same time on mutable version of statistics.
     *
     * @return Snapshot of monitoring statistics.
     * @deprecated implementing class is immutable hence snapshot creation is not needed anymore
     */
    @Deprecated
    public MonitoringStatistics snapshot();
}
