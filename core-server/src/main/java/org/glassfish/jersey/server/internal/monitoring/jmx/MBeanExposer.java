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

import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.ws.rs.ProcessingException;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.glassfish.jersey.server.internal.LocalizationMessages;
import org.glassfish.jersey.server.monitoring.ApplicationInfo;
import org.glassfish.jersey.server.monitoring.MonitoringStatistics;
import org.glassfish.jersey.server.monitoring.MonitoringStatisticsListener;
import org.glassfish.jersey.server.monitoring.ResourceStatistics;
import org.glassfish.jersey.server.spi.AbstractContainerLifecycleListener;
import org.glassfish.jersey.server.spi.Container;

import jersey.repackaged.com.google.common.collect.Maps;

/**
 * The main exposer class of Jersey JMX MBeans. The class creates MBeans and contains methods that
 * register and unregister MBeans.
 *
 * @author Miroslav Fuksa
 */
public class MBeanExposer extends AbstractContainerLifecycleListener implements MonitoringStatisticsListener {

    private static final Logger LOGGER = Logger.getLogger(MBeanExposer.class.getName());
    private static final String PROPERTY_SUBTYPE_GLOBAL = "Global";
    static final String PROPERTY_EXECUTION_TIMES_REQUESTS = "RequestTimes";
    static final String PROPERTY_EXECUTION_TIMES_METHODS = "MethodTimes";

    // MBeans
    private volatile ExecutionStatisticsDynamicBean requestMBean;
    private volatile ResponseMXBeanImpl responseMXBean;
    private volatile ResourcesMBeanGroup uriStatsGroup;
    private volatile ResourcesMBeanGroup resourceClassStatsGroup;
    private volatile ExceptionMapperMXBeanImpl exceptionMapperMXBean;
    private final AtomicBoolean destroyed = new AtomicBoolean(false);
    private final Object LOCK = new Object();

    /**
     * Name of domain that will prefix mbeans {@link ObjectName}. The code uses this
     * field also for synchronization purposes. If domain is {@code null}, no bean
     * has been registered yet.
     */
    private volatile String domain;

    @Inject
    private Provider<ApplicationInfo> applicationInfoProvider;


    private Map<String, ResourceStatistics> transformToStringKeys(Map<Class<?>, ResourceStatistics> stats) {
        Map<String, ResourceStatistics> newMap = Maps.newHashMap();
        for (Map.Entry<Class<?>, ResourceStatistics> entry : stats.entrySet()) {
            newMap.put(entry.getKey().getName(), entry.getValue());
        }
        return newMap;
    }

    /**
     * Convert the resource name to a valid {@link javax.management.ObjectName object name}.
     * @param name Resource name.
     * @param isUri {@code true} if the resource name is an URI.
     *
     * @return Converted valid object name.
     */
    static String convertToObjectName(String name, boolean isUri) {
        if (!isUri) {
            return name;
        }

        String str = name.replace("\\", "\\\\");
        str = str.replace("?", "\\?");
        str = str.replace("*", "\\*");

        return "\"" + str + "\"";
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
            synchronized (LOCK) {
                if (destroyed.get()) {
                    // already destroyed
                    return;
                }
                final ObjectName objectName = new ObjectName(name);
                if (mBeanServer.isRegistered(objectName)) {

                    LOGGER.log(Level.WARNING,
                            LocalizationMessages.WARNING_MONITORING_MBEANS_BEAN_ALREADY_REGISTERED(objectName));
                    mBeanServer.unregisterMBean(objectName);
                }
                mBeanServer.registerMBean(mbean, objectName);
            }
        } catch (JMException e) {
            throw new ProcessingException(LocalizationMessages.ERROR_MONITORING_MBEANS_REGISTRATION(name), e);
        }
    }

    private void unregisterJerseyMBeans(boolean destroy) {
        final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            synchronized (LOCK) {
                if (destroy) {
                    destroyed.set(true); // do not register new beans since now
                }

                if (domain == null) {
                    // No bean has been registered yet.
                    return;
                }
                final Set<ObjectName> names = mBeanServer.queryNames(new ObjectName(domain + ",*"), null);
                for (ObjectName name : names) {
                    mBeanServer.unregisterMBean(name);
                }
            }
        } catch (Exception e) {
            throw new ProcessingException(LocalizationMessages.ERROR_MONITORING_MBEANS_UNREGISTRATION_DESTROY(), e);
        }
    }

    @Override
    public void onStatistics(MonitoringStatistics statistics) {
        if (domain == null) {
            final String globalSubType = ",subType=" + PROPERTY_SUBTYPE_GLOBAL;

            final ApplicationInfo appStats = applicationInfoProvider.get();
            String appName = appStats.getResourceConfig().getApplicationName();
            if (appName == null) {
                appName = "App_" + Integer.toHexString(appStats.getResourceConfig().hashCode());
            }
            domain = "org.glassfish.jersey:type=" + appName;
            unregisterJerseyMBeans(false);

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

        requestMBean.updateExecutionStatistics(statistics.getRequestStatistics());
        uriStatsGroup.updateResourcesStatistics(statistics.getUriStatistics());
        responseMXBean.updateResponseStatistics(statistics.getResponseStatistics());
        exceptionMapperMXBean.updateExceptionMapperStatistics(statistics.getExceptionMapperStatistics());
        this.resourceClassStatsGroup.updateResourcesStatistics(transformToStringKeys(statistics.getResourceClassStatistics()));
    }

    @Override
    public void onShutdown(Container container) {
        unregisterJerseyMBeans(true);
    }
}
