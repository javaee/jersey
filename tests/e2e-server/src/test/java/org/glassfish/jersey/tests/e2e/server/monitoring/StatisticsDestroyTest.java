/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2017 Oracle and/or its affiliates. All rights reserved.
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

import java.lang.management.ManagementFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.monitoring.DestroyListener;
import org.glassfish.jersey.server.monitoring.MonitoringStatistics;
import org.glassfish.jersey.server.monitoring.MonitoringStatisticsListener;
import org.glassfish.jersey.server.spi.AbstractContainerLifecycleListener;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.grizzly.GrizzlyTestContainerFactory;
import org.glassfish.jersey.test.jdkhttp.JdkHttpServerTestContainerFactory;
import org.glassfish.jersey.test.jetty.JettyTestContainerFactory;
import org.glassfish.jersey.test.simple.SimpleTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author Miroslav Fuksa
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({StatisticsDestroyTest.GrizzlyTestCase.class, StatisticsDestroyTest.JdkServerTestCase.class,
        StatisticsDestroyTest.SimpleHttpServerTestCase.class})
public class StatisticsDestroyTest {

    public static class ParentTest extends JerseyTest {
        @Override
        public Application configure() {
            StatisticsListener.reset();
            final ResourceConfig resourceConfig = new ResourceConfig(TestResource.class);
            resourceConfig.setApplicationName("myApplication");
            resourceConfig.property("very-important", "yes");
            resourceConfig.property("another-property", 48);
            resourceConfig.property(ServerProperties.MONITORING_STATISTICS_MBEANS_ENABLED, true);
            resourceConfig.register(StatisticsListener.class);
            return resourceConfig;
        }

        @Override
        @After
        public void tearDown() throws Exception {
            super.tearDown();
            assertTrue(StatisticsListener.ON_SHUTDOWN_CALLED);
            assertTrue(StatisticsListener.ON_DESTROY_CALLED);
            assertTrue(StatisticsListener.ON_STATISTICS_CALLED);

            final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            final ObjectName name = new ObjectName("org.glassfish.jersey:type=myApplication,subType=Global,global=Configuration");
            boolean registered = mBeanServer.isRegistered(name);
            int time = 0;
            while (registered && time < 4000) {
                // wait until MBeans are asynchronously exposed
                int waitTime = 300;
                time += waitTime;
                Thread.sleep(waitTime);
                registered = mBeanServer.isRegistered(name);
            }

            Assert.assertFalse("MBean should be already unregistered!", mBeanServer.isRegistered(name));
        }

        @Path("resource")
        public static class TestResource {
            @GET
            public String testGet() {
                return "get";
            }
        }

        @Test
        public void test() throws Exception {
            final String path = "resource";
            assertEquals(200, target().path(path).request().get().getStatus());
            final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            final ObjectName name = new ObjectName("org.glassfish.jersey:type=myApplication,subType=Global,global=Configuration");
            boolean registered = mBeanServer.isRegistered(name);

            // wait (events are processed asynchronously and it might take time to expose mbeans
            int time = 0;
            while (!registered && time < 4000) {
                // wait until MBeans are asynchronously exposed
                int waitTime = 300;
                time += waitTime;
                Thread.sleep(waitTime);
                registered = mBeanServer.isRegistered(name);
            }

            assertTrue("MBean should be already registered!", mBeanServer.isRegistered(name));
            final String str = (String) mBeanServer.getAttribute(name, "ApplicationName");
            Assert.assertEquals("myApplication", str);
        }
    }

    public static class GrizzlyTestCase extends ParentTest {
        @Override
        protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
            StatisticsListener.reset();
            return new GrizzlyTestContainerFactory();
        }
    }

    public static class JdkServerTestCase extends ParentTest {
        @Override
        protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
            StatisticsListener.reset();
            return new JdkHttpServerTestContainerFactory();
        }
    }

    /**
     * Works only with Java 7
     */
    public static class JettyServerTestCase extends ParentTest {
        @Override
        protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
            StatisticsListener.reset();
            return new JettyTestContainerFactory();
        }
    }

    public static class SimpleHttpServerTestCase extends ParentTest {
        @Override
        protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
            StatisticsListener.reset();
            return new SimpleTestContainerFactory();
        }
    }

    public static class StatisticsListener extends AbstractContainerLifecycleListener
            implements MonitoringStatisticsListener, DestroyListener {

        public static boolean ON_SHUTDOWN_CALLED = false;
        public static boolean ON_STATISTICS_CALLED = false;
        public static boolean ON_DESTROY_CALLED = false;

        public static void reset() {
            ON_SHUTDOWN_CALLED = false;
            ON_STATISTICS_CALLED = false;
            ON_DESTROY_CALLED = false;
        }

        @Override
        public void onStatistics(MonitoringStatistics statistics) {
            StatisticsListener.ON_STATISTICS_CALLED = true;
        }

        @Override
        public void onShutdown(Container container) {
            StatisticsListener.ON_SHUTDOWN_CALLED = true;
        }

        @Override
        public void onDestroy() {
            StatisticsListener.ON_DESTROY_CALLED = true;
        }
    }
}
