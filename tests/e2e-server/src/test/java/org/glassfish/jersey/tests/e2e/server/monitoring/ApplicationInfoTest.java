/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.e2e.server.monitoring;

import java.util.Arrays;
import java.util.List;

import javax.ws.rs.ConstrainedTo;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.Response;

import javax.annotation.Priority;
import javax.inject.Provider;

import org.glassfish.jersey.internal.spi.AutoDiscoverable;
import org.glassfish.jersey.internal.spi.ForcedAutoDiscoverable;
import org.glassfish.jersey.internal.util.PropertiesHelper;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.monitoring.ApplicationInfo;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * The test uses server properties {@link ServerProperties#MONITORING_STATISTICS_MBEANS_ENABLED},
 * {@link ServerProperties#MONITORING_STATISTICS_MBEANS_ENABLED},
 * {@link ServerProperties#MONITORING_STATISTICS_MBEANS_ENABLED}
 * and it also implements {@link ForcedAutoDiscoverable} and tests if it is possible to inject
 * {@link ApplicationInfo} in different circumstances.
 *
 * @author Libor Kramolis (libor.kramolis at oracle.com)
 */
@RunWith(Parameterized.class)
public class ApplicationInfoTest extends JerseyTest {

    private static final String FORCE_ENABLE = "FORCE_ENABLE";
    private static final String ENABLE_MONITORING = "ENABLE_MONITORING";
    private static final String ENABLE_MONITORING_STATISTICS = "ENABLE_MONITORING_STATISTICS";
    private static final String ENABLE_MONITORING_STATISTICS_MBEANS = "ENABLE_MONITORING_STATISTICS_MBEANS";

    @Parameterized.Parameters
    public static List<Object[]> testData() {
        return Arrays.asList(new Object[][] {
                //force, 3x AutoDiscoverable, 3x ResourceConfig,   response
                // no property set => 500
                {false, false, false, false, null, null, null, 500},
                // property set by ForcedAutoDiscoverable => 200
                {false, true, false, false, null, null, null, 200},
                {false, false, true, false, null, null, null, 200},
                {false, false, false, true, null, null, null, 200},
                // property disable by ResourceConfig => 500
                {false, true, false, false, false, false, false, 500},
                {false, false, true, false, false, false, false, 500},
                {false, false, false, true, false, false, false, 500},
                // property disable by ResourceConfig but forced by ForcedAutoDiscoverable => 200
                {true, true, false, false, false, false, false, 200},
                {true, false, true, false, false, false, false, 200},
                {true, false, false, true, false, false, false, 200}
        });
    }

    private int responseStatus;

    public ApplicationInfoTest(boolean forceEnable, boolean enableMonitoring,
                               boolean enableMonitoringStatistics, boolean enableMonitoringStatisticsMBeans,
                               Boolean monitoringEnabled, Boolean monitoringStatisticsEnabled,
                               Boolean monitoringStatisticsMBeansEnabled, int responseStatus) {
        super(createApplication(forceEnable, enableMonitoring,
                enableMonitoringStatistics, enableMonitoringStatisticsMBeans,
                monitoringEnabled, monitoringStatisticsEnabled,
                monitoringStatisticsMBeansEnabled));
        this.responseStatus = responseStatus;
    }

    private static Application createApplication(boolean forceEnable, boolean enableMonitoring,
                                                 boolean enableMonitoringStatistics, boolean enableMonitoringStatisticsMBeans,
                                                 Boolean monitoringEnabled, Boolean monitoringStatisticsEnabled,
                                                 Boolean monitoringStatisticsMBeansEnabled) {
        final ResourceConfig resourceConfig = new ResourceConfig(Resource.class);
        resourceConfig.property(ServerProperties.APPLICATION_NAME, "testApp");
        resourceConfig.property(FORCE_ENABLE, forceEnable);
        if (enableMonitoring) {
            resourceConfig.property(ENABLE_MONITORING, true);
        }
        if (enableMonitoringStatistics) {
            resourceConfig.property(ENABLE_MONITORING_STATISTICS, true);
        }
        if (enableMonitoringStatisticsMBeans) {
            resourceConfig.property(ENABLE_MONITORING_STATISTICS_MBEANS, true);
        }
        if (monitoringEnabled != null) {
            resourceConfig.property(ServerProperties.MONITORING_ENABLED, monitoringEnabled);
        }
        if (monitoringStatisticsEnabled != null) {
            resourceConfig.property(ServerProperties.MONITORING_STATISTICS_ENABLED, monitoringStatisticsEnabled);
        }
        if (monitoringStatisticsMBeansEnabled != null) {
            resourceConfig.property(ServerProperties.MONITORING_STATISTICS_MBEANS_ENABLED, monitoringStatisticsMBeansEnabled);
        }

        return resourceConfig;
    }

    @Test
    public void test() {
        final Response response = target().path("resource").request().get();
        Assert.assertEquals(responseStatus, response.getStatus());
        if (responseStatus == 200) {
            Assert.assertEquals("testApp", response.readEntity(String.class));
        }
    }

    @Path("resource")
    public static class Resource {

        @Context
        Provider<ApplicationInfo> applicationInfoProvider;

        @GET
        public String getAppName() {
            final ApplicationInfo applicationInfo = applicationInfoProvider.get();
            return applicationInfo.getResourceConfig().getApplicationName();
        }
    }

    @ConstrainedTo(RuntimeType.SERVER)
    @Priority(AutoDiscoverable.DEFAULT_PRIORITY - 1)
    public static class ForcedAutoDiscoverableImpl implements ForcedAutoDiscoverable {

        @Override
        public void configure(final FeatureContext context) {
            final boolean forceEnable = PropertiesHelper.isProperty(context.getConfiguration().getProperty(FORCE_ENABLE));
            if (PropertiesHelper.isProperty(context.getConfiguration().getProperty(ENABLE_MONITORING))) {
                enable(context, forceEnable, ServerProperties.MONITORING_ENABLED);
            }
            if (PropertiesHelper.isProperty(context.getConfiguration().getProperty(ENABLE_MONITORING_STATISTICS))) {
                enable(context, forceEnable, ServerProperties.MONITORING_STATISTICS_ENABLED);
            }
            if (PropertiesHelper.isProperty(context.getConfiguration().getProperty(ENABLE_MONITORING_STATISTICS_MBEANS))) {
                enable(context, forceEnable, ServerProperties.MONITORING_STATISTICS_MBEANS_ENABLED);
            }
        }

        private void enable(FeatureContext context, boolean forceEnable, String propertyName) {
            if (forceEnable) {
                context.property(propertyName, true);
            } else {
                if (context.getConfiguration().getProperty(propertyName) == null) {
                    context.property(propertyName, true);
                }
            }
        }

    }

}
