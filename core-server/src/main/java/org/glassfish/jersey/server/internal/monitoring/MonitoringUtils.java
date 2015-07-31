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

import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.monitoring.ExecutionStatistics;
import org.glassfish.jersey.server.monitoring.TimeWindowStatistics;

/**
 * Monitoring helper class that contains utility methods used in
 * Monitoring.
 *
 * @author Miroslav Fuksa
 */
public final class MonitoringUtils {

    /**
     * Request rate limit (per second) below which statistics can be considered as cacheable.
     */
    private static final double CACHEABLE_REQUEST_RATE_LIMIT = 0.001;

    /**
     * Get the method unique string ID. The ID is constructed from method attributes separated
     * by pipe '|'. The attributes are used in the following order:
     * method-produces|method-consumes|http-method|method-path|method-java-name
     * <p>
     *     If any of the attributes is not defined, "null" is used for such an attribute.
     * <p/>
     *
     * @param method Resource method.
     * @return String constructed from resource method parameters.
     */
    public static String getMethodUniqueId(final ResourceMethod method) {
        final String path = method.getParent() != null ? createPath(method.getParent()) : "null";

        return method.getProducedTypes().toString() + "|"
                + method.getConsumedTypes().toString() + "|"
                + method.getHttpMethod() + "|"
                + path + "|"
                + method.getInvocable().getHandlingMethod().getName();
    }

    private static String createPath(Resource resource) {
        return appendPath(resource, new StringBuilder()).toString();
    }

    private static StringBuilder appendPath(Resource resource, StringBuilder path) {
        return resource.getParent() == null ? path.append(resource.getPath())
                : appendPath(resource.getParent(), path).append(".").append(resource.getPath());
    }

    /**
     * Indicates whether the global, resource, resource method statistics containing the give execution statistics can
     * be cached.
     *
     * @param stats execution statistics to be examined.
     * @return {@code true} if the statistics can be cached, {@code false} otherwise.
     */
    static boolean isCacheable(final ExecutionStatistics stats) {
        for (final TimeWindowStatistics window : stats.getTimeWindowStatistics().values()) {
            if (window.getRequestsPerSecond() >= CACHEABLE_REQUEST_RATE_LIMIT) {
                return false;
            }
        }
        return true;
    }

    /**
     * Prevent instantiation.
     */
    private MonitoringUtils() {
    }
}
