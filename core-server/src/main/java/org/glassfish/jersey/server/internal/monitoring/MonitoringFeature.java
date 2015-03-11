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

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.glassfish.jersey.internal.inject.ReferencingFactory;
import org.glassfish.jersey.internal.util.collection.Ref;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.internal.LocalizationMessages;
import org.glassfish.jersey.server.internal.monitoring.jmx.MBeanExposer;
import org.glassfish.jersey.server.monitoring.ApplicationInfo;
import org.glassfish.jersey.server.monitoring.MonitoringStatistics;
import org.glassfish.jersey.server.monitoring.MonitoringStatisticsListener;

import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.TypeLiteral;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

/**
 * Feature that enables calculating of {@link MonitoringStatistics monitoring statistics} and
 * optionally also enables exposure of monitoring MBeans.
 * <p>
 * Calculation of {@code MonitoringStatistics} is necessary in order to expose monitoring MBeans, so by default
 * this feature always enables calculation of {@code MonitoringStatistics}. Additionally, the feature can be
 * configured by setting {@code true} to {@link #setmBeansEnabled(boolean)} in order to enable exposure
 * of monitoring MBeans. The same can be achieved by configuration of a property
 * {@link org.glassfish.jersey.server.ServerProperties#MONITORING_STATISTICS_MBEANS_ENABLED} which
 * overrides the setting defined by the {@link #setmBeansEnabled(boolean)} method.
 * <p/>
 * <p>
 * The MonitoringStatistics can be controlled also by definition of a property
 * {@link org.glassfish.jersey.server.ServerProperties#MONITORING_STATISTICS_ENABLED} which overrides
 * the registration of this feature.
 * </p>
 * When auto-discovery is enabled then monitoring statistics and exposure of MBeans can be controlled only
 * by properties above without a need to explicitly register this feature.
 *
 * @see org.glassfish.jersey.server.ServerProperties#MONITORING_STATISTICS_ENABLED for more details.
 * @author Miroslav Fuksa
 */
public final class MonitoringFeature implements Feature {

    private static final Logger LOGGER = Logger.getLogger(MonitoringFeature.class.getName());

    private boolean monitoringEnabled = true;
    private boolean statisticsEnabled = true; // monitoring statistics are enabled only if monitoring is enabled
    private boolean mBeansEnabled; // monitoring mbeans are enabled only if monitoring statistics is enabled

    @Override
    public boolean configure(FeatureContext context) {
        final Boolean monitoringEnabledProperty = ServerProperties.getValue(context.getConfiguration().getProperties(),
                ServerProperties.MONITORING_ENABLED, null, Boolean.class);
        final Boolean statisticsEnabledProperty = ServerProperties.getValue(context.getConfiguration().getProperties(),
                ServerProperties.MONITORING_STATISTICS_ENABLED, null, Boolean.class);
        final Boolean mbeansEnabledProperty = ServerProperties.getValue(context.getConfiguration().getProperties(),
                ServerProperties.MONITORING_STATISTICS_MBEANS_ENABLED, null, Boolean.class);

        if (monitoringEnabledProperty != null) {
            monitoringEnabled = monitoringEnabledProperty;
            statisticsEnabled = monitoringEnabled; // monitoring statistics are enabled by default if monitoring is enabled
        }

        if (statisticsEnabledProperty != null) {
            monitoringEnabled = monitoringEnabled || statisticsEnabledProperty;
            statisticsEnabled = statisticsEnabledProperty;
        }

        if (mbeansEnabledProperty != null) {
            monitoringEnabled = monitoringEnabled || mbeansEnabledProperty;
            statisticsEnabled = statisticsEnabled || mbeansEnabledProperty;
            mBeansEnabled = mbeansEnabledProperty;
        }

        if (statisticsEnabledProperty != null && !statisticsEnabledProperty) {
            if (mbeansEnabledProperty != null && mBeansEnabled) {
                LOGGER.log(Level.WARNING,
                        LocalizationMessages.WARNING_MONITORING_FEATURE_ENABLED(ServerProperties.MONITORING_STATISTICS_ENABLED));
            } else {
                LOGGER.log(Level.WARNING,
                        LocalizationMessages.WARNING_MONITORING_FEATURE_DISABLED(ServerProperties.MONITORING_STATISTICS_ENABLED));
            }
        }

        if (monitoringEnabled) {
            context.register(ApplicationInfoListener.class);
            context.register(new AbstractBinder() {
                @Override
                protected void configure() {
                    bindFactory(ReferencingFactory.<ApplicationInfo>referenceFactory()).to(
                            new TypeLiteral<Ref<ApplicationInfo>>() {
                            }
                    ).in(Singleton.class);

                    bindFactory(ApplicationInfoInjectionFactory.class).to(
                            ApplicationInfo.class).in(PerLookup.class);
                }
            });
        }

        if (statisticsEnabled) {
            context.register(MonitoringEventListener.class);
            context.register(new AbstractBinder() {
                @Override
                protected void configure() {
                    bindFactory(ReferencingFactory.<MonitoringStatistics>referenceFactory()).to(
                            new TypeLiteral<Ref<MonitoringStatistics>>() {
                            }).in(Singleton.class);

                    bindFactory(StatisticsInjectionFactory.class).to(MonitoringStatistics.class).in(PerLookup.class);

                    bind(StatisticsListener.class).to(MonitoringStatisticsListener.class).in(Singleton.class);
                }
            });
        }

        if (mBeansEnabled) {
            // instance registration is needed here as MBeanExposer needs to be a singleton so that
            // one instance handles listening to events of MonitoringStatisticsListener and ContainerLifecycleListener
            context.register(new MBeanExposer());
        }

        return monitoringEnabled;
    }

    /**
     * Set whether the feature should also enable exposure of monitoring statistics MBeans.
     * The set value can be overwritten by the definition of the property
     * {@link org.glassfish.jersey.server.ServerProperties#MONITORING_STATISTICS_MBEANS_ENABLED}.
     *
     * @param mBeansEnabled {@code true} is monitoring MBeans should be exposed.
     */
    public void setmBeansEnabled(boolean mBeansEnabled) {
        this.mBeansEnabled = mBeansEnabled;
    }

    private static class ApplicationInfoInjectionFactory extends ReferencingFactory<ApplicationInfo> {

        /**
         * Create new referencing injection factory.
         *
         * @param referenceFactory reference provider backing the factory.
         */
        @Inject
        public ApplicationInfoInjectionFactory(Provider<Ref<ApplicationInfo>> referenceFactory) {
            super(referenceFactory);
        }

    }

    private static class StatisticsInjectionFactory extends ReferencingFactory<MonitoringStatistics> {

        /**
         * Create new referencing injection factory.
         *
         * @param referenceFactory reference provider backing the factory.
         */
        @Inject
        public StatisticsInjectionFactory(Provider<Ref<MonitoringStatistics>> referenceFactory) {
            super(referenceFactory);
        }

        @Override
        public MonitoringStatistics provide() {
            return super.provide();
        }

        @Override
        public void dispose(MonitoringStatistics monitoringStatistics) {
        }

    }

    private static class StatisticsListener implements MonitoringStatisticsListener {

        @Inject
        Provider<Ref<MonitoringStatistics>> statisticsFactory;

        @Override
        public void onStatistics(MonitoringStatistics statistics) {
            statisticsFactory.get().set(statistics);
        }
    }

}
