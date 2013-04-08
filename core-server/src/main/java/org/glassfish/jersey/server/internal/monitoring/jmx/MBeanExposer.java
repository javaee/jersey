/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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

import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.ws.rs.ProcessingException;

import org.glassfish.jersey.server.internal.LocalizationMessages;
import org.glassfish.jersey.server.monitoring.ApplicationStatistics;
import org.glassfish.jersey.server.monitoring.MonitoringStatistics;
import org.glassfish.jersey.server.monitoring.MonitoringStatisticsListener;
import org.glassfish.jersey.server.monitoring.ResourceStatistics;

import com.google.common.collect.Maps;

/**
 * The main exposer class of Jersey JMX MBeans. The class creates MBeans and contains methods that
 * register and unregister MBeans.
 *
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 */
public class MBeanExposer implements MonitoringStatisticsListener {

    private static final String PROPERTY_SUBTYPE_GLOBAL = "Global";
    static final String PROPERTY_EXECUTION_TIMES_REQUESTS = "RequestTimes";
    static final String PROPERTY_EXECUTION_TIMES_METHODS = "MethodTimes";
    // MBeans
    private volatile ExecutionStatisticsDynamicBean requestMBean;
    private volatile ResponseMXBeanImpl responseMXBean;
    private volatile ResourcesMBeanGroup uriStatsGroup;
    private volatile ResourcesMBeanGroup resourceClassStatsGroup;
    private volatile ExceptionMapperMXBeanImpl exceptionMapperMXBean;

    private final AtomicBoolean exposed = new AtomicBoolean(false);
    private volatile String domain;

    private static final Logger LOGGER = Logger.getLogger(MBeanExposer.class.getName());


    private Map<String, ResourceStatistics> transformToStringKeys(Map<Class<?>, ResourceStatistics> stats) {
        Map<String, ResourceStatistics> newMap = Maps.newHashMap();
        for (Map.Entry<Class<?>, ResourceStatistics> entry : stats.entrySet()) {
            newMap.put(entry.getKey().getName(), entry.getValue());
        }
        return newMap;
    }

    /**
     * Register the MBean with the given postfix name.
     *
     * @param mbean MBean to be registered.
     * @param namePostfix Postfix of the object name in the pattern ",[property]=[value]...". Example
     *                    ",subType=Requests,details=Execution"
     */
    void registerMBean(Object mbean, String namePostfix) {
        final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        final String name = domain + namePostfix;
        try {
            final ObjectName objectName = new ObjectName(name);
            if (mBeanServer.isRegistered(objectName)) {

                LOGGER.log(Level.WARNING,
                        LocalizationMessages.WARNING_MONITORING_MBEANS_BEAN_ALREADY_REGISTERED(objectName));
                mBeanServer.unregisterMBean(objectName);
            }

            mBeanServer.registerMBean(mbean, objectName);
        } catch (JMException e) {

            throw new ProcessingException(LocalizationMessages.ERROR_MONITORING_MBEANS_REGISTRATION(name), e);
        }
    }


    private void unregisterJerseyMBeans() {
        final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            final Set<ObjectName> names = mBeanServer.queryNames(new ObjectName(domain + ",*"), null);
            for (ObjectName name : names) {
                mBeanServer.unregisterMBean(name);
            }
        } catch (Exception e) {
            throw new ProcessingException(LocalizationMessages.ERROR_MONITORING_MBEANS_UNREGISTRATION_DESTROY(), e);
        }
    }

    @Override
    public void onStatistics(MonitoringStatistics statistics) {
        if (exposed.compareAndSet(false, true)) {
            final String globalSubType = ",subType=" + PROPERTY_SUBTYPE_GLOBAL;

            final ApplicationStatistics appStats = statistics.getApplicationStatistics();
            String appName = appStats.getResourceConfig().getApplicationName();
            if (appName == null) {
                appName = "App_" + Integer.toHexString(appStats.getResourceConfig().hashCode());
            }
            domain = "org.glassfish.jersey:type=" + appName;
            unregisterJerseyMBeans();

            uriStatsGroup = new ResourcesMBeanGroup(statistics.getUriStatistics(), true, this, ",subType=Uris");
            Map<String, ResourceStatistics> newMap = transformToStringKeys(statistics.getResourceClassStatistics());

            resourceClassStatsGroup = new ResourcesMBeanGroup(newMap, false, this, ",subType=Resources");

            responseMXBean = new ResponseMXBeanImpl();
            registerMBean(responseMXBean, globalSubType + ",global=Responses");

            requestMBean = new ExecutionStatisticsDynamicBean(statistics.getRequestStatistics(),
                    this, globalSubType, "AllRequestTimes");

            exceptionMapperMXBean = new ExceptionMapperMXBeanImpl(statistics.getExceptionMapperStatistics(), this,
                    globalSubType);

            new ApplicationMXBeanImpl(appStats, this, globalSubType);
        }

        if (statistics.getApplicationStatistics().getDestroyTime() != null) {
            unregisterJerseyMBeans();
        }

        requestMBean.updateExecutionStatistics(statistics.getRequestStatistics());
        uriStatsGroup.updateResourcesStatistics(statistics.getUriStatistics());
        responseMXBean.updateResponseStatistics(statistics.getResponseStatistics());
        exceptionMapperMXBean.updateExceptionMapperStatistics(statistics.getExceptionMapperStatistics());
        this.resourceClassStatsGroup.updateResourcesStatistics(transformToStringKeys(statistics.getResourceClassStatistics()));

    }


}
