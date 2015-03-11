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

package org.glassfish.jersey.server.internal.monitoring.jmx;

import java.util.Map;

import org.glassfish.jersey.server.monitoring.ExceptionMapperMXBean;
import org.glassfish.jersey.server.monitoring.ExceptionMapperStatistics;

import jersey.repackaged.com.google.common.collect.Maps;

/**
 * MXBean implementing a {@link org.glassfish.jersey.server.monitoring.ExceptionMapperMXBean} mxbean interface.
 *
 * @author Miroslav Fuksa
 */
public class ExceptionMapperMXBeanImpl implements ExceptionMapperMXBean {
    private volatile ExceptionMapperStatistics mapperStatistics;
    private volatile Map<String, Long> mapperExcecutions = Maps.newHashMap();

    /**
     * Create a new MXBean and register it into mbean server using {@code mBeanExposer}.
     *
     * @param mapperStatistics Exception mapper statistics that should be exposed.
     * @param mBeanExposer Mbean exposer.
     * @param parentName Object name prefix of the parent mbeans.
     */
    public ExceptionMapperMXBeanImpl(ExceptionMapperStatistics mapperStatistics,
                                     MBeanExposer mBeanExposer, String parentName) {
        mBeanExposer.registerMBean(this, parentName + ",exceptions=ExceptionMapper");
        updateExceptionMapperStatistics(mapperStatistics);
    }

    /**
     * Update the MXBean with new statistics.
     *
     * @param mapperStatistics New exception mapper statistics.
     */
    public void updateExceptionMapperStatistics(ExceptionMapperStatistics mapperStatistics) {
        this.mapperStatistics = mapperStatistics;

        for (Map.Entry<Class<?>, Long> entry : mapperStatistics.getExceptionMapperExecutions().entrySet()) {
            mapperExcecutions.put(entry.getKey().getName(), entry.getValue());
        }
    }

    @Override
    public Map<String, Long> getExceptionMapperCount() {
        return mapperExcecutions;
    }

    @Override
    public long getSuccessfulMappings() {
        return mapperStatistics.getSuccessfulMappings();
    }

    @Override
    public long getUnsuccessfulMappings() {
        return mapperStatistics.getUnsuccessfulMappings();
    }

    @Override
    public long getTotalMappings() {
        return mapperStatistics.getTotalMappings();
    }


}
