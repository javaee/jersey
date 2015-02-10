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

import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.internal.monitoring.ApplicationInfoListener;
import org.glassfish.jersey.server.internal.monitoring.MonitoringEventListener;
import org.glassfish.jersey.server.internal.monitoring.MonitoringFeature;
import org.glassfish.jersey.server.internal.monitoring.jmx.MBeanExposer;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test of registration of {@link MonitoringFeature}.
 * @author Miroslav Fuksa
 * @author Libor Kramolis (libor.kramolis at oracle.com)
 */
public class MonitoringFeatureTest {

    @Test
    public void testStatisticsEnabled() {
        final ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.register(MonitoringFeature.class);
        final ApplicationHandler applicationHandler = new ApplicationHandler(resourceConfig);

        final ResourceConfig config = applicationHandler.getConfiguration();
        Assert.assertTrue(config.isRegistered(ApplicationInfoListener.class));
        Assert.assertTrue(config.isRegistered(MonitoringEventListener.class));
        Assert.assertFalse(config.isRegistered(MBeanExposer.class));
    }

    @Test
    public void testMonitoringDisabled() {
        final ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.register(MonitoringFeature.class);
        resourceConfig.property(ServerProperties.MONITORING_ENABLED, false);
        final ApplicationHandler applicationHandler = new ApplicationHandler(resourceConfig);

        final ResourceConfig config = applicationHandler.getConfiguration();
        Assert.assertFalse(config.isRegistered(ApplicationInfoListener.class));
        Assert.assertFalse(config.isRegistered(MonitoringEventListener.class));
        Assert.assertFalse(config.isRegistered(MBeanExposer.class));
    }

    @Test
    public void testStatisticsDisabled() {
        final ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.register(MonitoringFeature.class);
        resourceConfig.property(ServerProperties.MONITORING_STATISTICS_ENABLED, false);
        final ApplicationHandler applicationHandler = new ApplicationHandler(resourceConfig);

        final ResourceConfig config = applicationHandler.getConfiguration();
        Assert.assertTrue(config.isRegistered(ApplicationInfoListener.class));
        Assert.assertFalse(config.isRegistered(MonitoringEventListener.class));
        Assert.assertFalse(config.isRegistered(MBeanExposer.class));
    }

    @Test
    public void testMonitoringEnabledByAutodiscovery() {
        final ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.property(ServerProperties.MONITORING_ENABLED, true);
        final ApplicationHandler applicationHandler = new ApplicationHandler(resourceConfig);

        final ResourceConfig config = applicationHandler.getConfiguration();
        Assert.assertTrue(config.isRegistered(ApplicationInfoListener.class));
        Assert.assertTrue(config.isRegistered(MonitoringEventListener.class));
        Assert.assertFalse(config.isRegistered(MBeanExposer.class));
    }

    @Test
    public void testMonitoringEnabledStatisticsDisabledByAutodiscovery() {
        final ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.property(ServerProperties.MONITORING_ENABLED, true);
        resourceConfig.property(ServerProperties.MONITORING_STATISTICS_ENABLED, false);
        final ApplicationHandler applicationHandler = new ApplicationHandler(resourceConfig);

        final ResourceConfig config = applicationHandler.getConfiguration();
        Assert.assertTrue(config.isRegistered(ApplicationInfoListener.class));
        Assert.assertFalse(config.isRegistered(MonitoringEventListener.class));
        Assert.assertFalse(config.isRegistered(MBeanExposer.class));
    }

    @Test
    public void testStatisticsEnabledByAutodiscovery() {
        final ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.property(ServerProperties.MONITORING_STATISTICS_ENABLED, true);
        final ApplicationHandler applicationHandler = new ApplicationHandler(resourceConfig);

        final ResourceConfig config = applicationHandler.getConfiguration();
        Assert.assertTrue(config.isRegistered(ApplicationInfoListener.class));
        Assert.assertTrue(config.isRegistered(MonitoringEventListener.class));
        Assert.assertFalse(config.isRegistered(MBeanExposer.class));
    }

    @Test
    public void testStatisticsDisabledByAutodiscovery() {
        final ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.property(ServerProperties.MONITORING_STATISTICS_ENABLED, false);
        final ApplicationHandler applicationHandler = new ApplicationHandler(resourceConfig);

        final ResourceConfig config = applicationHandler.getConfiguration();
        Assert.assertFalse(config.isRegistered(ApplicationInfoListener.class));
        Assert.assertFalse(config.isRegistered(MonitoringEventListener.class));
        Assert.assertFalse(config.isRegistered(MBeanExposer.class));
    }


    @Test
    public void testStatisticsEnabledMbeansEnabledByInstance() {
        final ResourceConfig resourceConfig = new ResourceConfig();
        final MonitoringFeature monitoringFeature = new MonitoringFeature();
        monitoringFeature.setmBeansEnabled(true);
        resourceConfig.register(monitoringFeature);
        final ApplicationHandler applicationHandler = new ApplicationHandler(resourceConfig);

        final ResourceConfig config = applicationHandler.getConfiguration();
        Assert.assertTrue(config.isRegistered(ApplicationInfoListener.class));
        Assert.assertTrue(config.isRegistered(MonitoringEventListener.class));
        Assert.assertTrue(config.isRegistered(MBeanExposer.class));
    }

    @Test
    public void testStatisticsEnabledMbeansEnabledByInstance2() {
        final ResourceConfig resourceConfig = new ResourceConfig();
        final MonitoringFeature monitoringFeature = new MonitoringFeature();
        monitoringFeature.setmBeansEnabled(true);
        resourceConfig.register(monitoringFeature);
        resourceConfig.property(ServerProperties.MONITORING_STATISTICS_MBEANS_ENABLED, false);
        final ApplicationHandler applicationHandler = new ApplicationHandler(resourceConfig);

        final ResourceConfig config = applicationHandler.getConfiguration();
        Assert.assertTrue(config.isRegistered(ApplicationInfoListener.class));
        Assert.assertTrue(config.isRegistered(MonitoringEventListener.class));
        Assert.assertFalse(config.isRegistered(MBeanExposer.class));
    }

    @Test
    public void testAllDisabled() {
        final ResourceConfig resourceConfig = new ResourceConfig();
        final MonitoringFeature monitoringFeature = new MonitoringFeature();
        monitoringFeature.setmBeansEnabled(true);
        resourceConfig.register(monitoringFeature);
        resourceConfig.property(ServerProperties.MONITORING_STATISTICS_MBEANS_ENABLED, false);
        resourceConfig.property(ServerProperties.MONITORING_STATISTICS_ENABLED, false);
        final ApplicationHandler applicationHandler = new ApplicationHandler(resourceConfig);

        final ResourceConfig config = applicationHandler.getConfiguration();
        Assert.assertTrue(config.isRegistered(ApplicationInfoListener.class));
        Assert.assertFalse(config.isRegistered(MonitoringEventListener.class));
        Assert.assertFalse(config.isRegistered(MBeanExposer.class));
    }

    @Test
    public void testAllDisabled2() {
        final ResourceConfig resourceConfig = new ResourceConfig();
        final MonitoringFeature monitoringFeature = new MonitoringFeature();
        resourceConfig.register(monitoringFeature);
        resourceConfig.property(ServerProperties.MONITORING_STATISTICS_MBEANS_ENABLED, false);
        resourceConfig.property(ServerProperties.MONITORING_STATISTICS_ENABLED, false);
        final ApplicationHandler applicationHandler = new ApplicationHandler(resourceConfig);

        final ResourceConfig config = applicationHandler.getConfiguration();
        Assert.assertTrue(config.isRegistered(ApplicationInfoListener.class));
        Assert.assertFalse(config.isRegistered(MonitoringEventListener.class));
        Assert.assertFalse(config.isRegistered(MBeanExposer.class));
    }

    @Test
    public void testAllDisabled3() {
        final ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.property(ServerProperties.MONITORING_STATISTICS_MBEANS_ENABLED, false);
        resourceConfig.property(ServerProperties.MONITORING_STATISTICS_ENABLED, false);
        final ApplicationHandler applicationHandler = new ApplicationHandler(resourceConfig);

        final ResourceConfig config = applicationHandler.getConfiguration();
        Assert.assertFalse(config.isRegistered(ApplicationInfoListener.class));
        Assert.assertFalse(config.isRegistered(MonitoringEventListener.class));
        Assert.assertFalse(config.isRegistered(MBeanExposer.class));
    }

    @Test
    public void testAllEnabled() {
        final ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.property(ServerProperties.MONITORING_STATISTICS_MBEANS_ENABLED, true);
        resourceConfig.property(ServerProperties.MONITORING_STATISTICS_ENABLED, true);
        final ApplicationHandler applicationHandler = new ApplicationHandler(resourceConfig);

        final ResourceConfig config = applicationHandler.getConfiguration();
        Assert.assertTrue(config.isRegistered(ApplicationInfoListener.class));
        Assert.assertTrue(config.isRegistered(MonitoringEventListener.class));
        Assert.assertTrue(config.isRegistered(MBeanExposer.class));
    }

    @Test
    public void testAllEnabled2() {
        final ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.property(ServerProperties.MONITORING_STATISTICS_MBEANS_ENABLED, true);
        resourceConfig.property(ServerProperties.MONITORING_STATISTICS_ENABLED, true);
        final MonitoringFeature monitoringFeature = new MonitoringFeature();
        monitoringFeature.setmBeansEnabled(false);
        resourceConfig.register(monitoringFeature);
        final ApplicationHandler applicationHandler = new ApplicationHandler(resourceConfig);

        final ResourceConfig config = applicationHandler.getConfiguration();
        Assert.assertTrue(config.isRegistered(ApplicationInfoListener.class));
        Assert.assertTrue(config.isRegistered(MonitoringEventListener.class));
        Assert.assertTrue(config.isRegistered(MBeanExposer.class));
    }

    @Test
    public void testOnlyMBeansEnabled() {
        final ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.property(ServerProperties.MONITORING_STATISTICS_MBEANS_ENABLED, true);
        final ApplicationHandler applicationHandler = new ApplicationHandler(resourceConfig);

        final ResourceConfig config = applicationHandler.getConfiguration();
        Assert.assertTrue(config.isRegistered(ApplicationInfoListener.class));
        Assert.assertTrue(config.isRegistered(MonitoringEventListener.class));
        Assert.assertTrue(config.isRegistered(MBeanExposer.class));
    }

    @Test
    public void testOnlyMBeansEnabled2() {
        final ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.property(ServerProperties.MONITORING_STATISTICS_MBEANS_ENABLED, true);
        resourceConfig.property(ServerProperties.MONITORING_STATISTICS_ENABLED, false);
        final ApplicationHandler applicationHandler = new ApplicationHandler(resourceConfig);

        final ResourceConfig config = applicationHandler.getConfiguration();
        Assert.assertTrue(config.isRegistered(ApplicationInfoListener.class));
        Assert.assertTrue(config.isRegistered(MonitoringEventListener.class));
        Assert.assertTrue(config.isRegistered(MBeanExposer.class));
    }

    @Test
    public void testOnlyMBeansEnabled3() {
        final ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.property(ServerProperties.MONITORING_STATISTICS_MBEANS_ENABLED, true);
        resourceConfig.property(ServerProperties.MONITORING_STATISTICS_ENABLED, false);
        resourceConfig.register(new MonitoringFeature());
        final ApplicationHandler applicationHandler = new ApplicationHandler(resourceConfig);

        final ResourceConfig config = applicationHandler.getConfiguration();
        Assert.assertTrue(config.isRegistered(ApplicationInfoListener.class));
        Assert.assertTrue(config.isRegistered(MonitoringEventListener.class));
        Assert.assertTrue(config.isRegistered(MBeanExposer.class));
    }
}
