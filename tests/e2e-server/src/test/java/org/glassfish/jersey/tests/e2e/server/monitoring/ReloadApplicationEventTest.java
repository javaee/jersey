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
import javax.ws.rs.core.Response;

import javax.inject.Singleton;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.grizzly.GrizzlyTestContainerFactory;
import org.glassfish.jersey.test.jdkhttp.JdkHttpServerTestContainerFactory;
import org.glassfish.jersey.test.jetty.JettyTestContainerFactory;
import org.glassfish.jersey.test.simple.SimpleTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * This test tests the lifecycle of the application in accordance to the monitoring
 * and {@link ContainerLifecycleListener container events}. Among others, it checks that
 * Monitoring MBeans are correctly exposed and destroyed when application is reloaded. Uses different
 * containers to test it.
 *
 * @author Miroslav Fuksa
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({ReloadApplicationEventTest.GrizzlyTestCase.class, ReloadApplicationEventTest.JdkServerTestCase.class,
        ReloadApplicationEventTest.SimpleHttpServerTestCase.class})
public class ReloadApplicationEventTest extends JerseyTest {

    public static final String ORIGINAL = "original";
    public static final String RELOADED = "reloaded";

    public static class GrizzlyTestCase extends ParentTest {

        @Override
        protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
            return new GrizzlyTestContainerFactory();
        }
    }

    public static class JdkServerTestCase extends ParentTest {

        @Override
        protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
            return new JdkHttpServerTestContainerFactory();
        }
    }

    /**
     * Works only with Java 7
     */
    public static class JettyServerTestCase extends ParentTest {

        @Override
        protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
            return new JettyTestContainerFactory();
        }
    }

    public static class SimpleHttpServerTestCase extends ParentTest {

        @Override
        protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
            return new SimpleTestContainerFactory();
        }
    }

    public static class ParentTest extends JerseyTest {

        @Override
        public void setUp() throws Exception {
            super.setUp();
        }

        @Override
        protected Application configure() {
            OriginalResult.reset();
            ReloadedResult.reset();
            OriginalResult originalResult = new OriginalResult();
            final ResourceConfig resourceConfig = getResourceConfig()
                    .setApplicationName(ORIGINAL)
                    .register(new TestResource(originalResult))
                    .register(new AppEventListener(originalResult));

            return resourceConfig;
        }

        private static ResourceConfig getResourceConfig() {
            final ResourceConfig resourceConfig = new ResourceConfig();
            resourceConfig.property(ServerProperties.MONITORING_STATISTICS_MBEANS_ENABLED, true);
            return resourceConfig;
        }

        public static interface TestResultTracker {

            public void reloaded();

            public void shutdown();

            public void startup();

            /**
             * {@link ApplicationEvent.Type#RELOAD_FINISHED} called.
             */
            public void reloadedEvent();

            /**
             * {@link ApplicationEvent.Type#INITIALIZATION_FINISHED} called.
             */
            public void initEvent();
        }

        public static class OriginalResult implements TestResultTracker {

            public static boolean reloadedCalled;
            public static boolean shutdownCalled;
            public static boolean startupCalled;
            public static boolean reloadedEventCalled;
            public static boolean initEventCalled;

            public static void reset() {
                reloadedCalled = false;
                shutdownCalled = false;
                startupCalled = false;
                reloadedEventCalled = false;
                initEventCalled = false;
            }

            @Override
            public void reloaded() {
                reloadedCalled = true;
            }

            @Override
            public void shutdown() {
                shutdownCalled = true;
            }

            @Override
            public void startup() {
                startupCalled = true;
            }

            @Override
            public void reloadedEvent() {
                reloadedEventCalled = true;
            }

            @Override
            public void initEvent() {
                initEventCalled = true;
            }

        }

        public static class ReloadedResult implements TestResultTracker {

            public static boolean shutdownCalled;
            public static boolean reloadedCalled;
            public static boolean reloadedEventCalled;
            public static boolean initEventCalled;
            public static boolean startupCalled;

            public static void reset() {
                reloadedCalled = false;
                shutdownCalled = false;
                startupCalled = false;
                reloadedEventCalled = false;
                initEventCalled = false;
            }

            @Override
            public void reloaded() {
                reloadedCalled = true;

            }

            @Override
            public void shutdown() {
                shutdownCalled = true;
            }

            @Override
            public void startup() {
                startupCalled = true;
            }

            @Override
            public void reloadedEvent() {
                reloadedEventCalled = true;
            }

            @Override
            public void initEvent() {
                initEventCalled = true;
            }

        }

        @Path("resource")
        @Singleton
        public static class TestResource implements ContainerLifecycleListener {

            private volatile Container container;

            private final TestResultTracker testResultTracker;

            public TestResource(TestResultTracker testResultTracker) {
                this.testResultTracker = testResultTracker;
            }

            @GET
            public String get() {
                container.reload(getResourceConfig()
                        .setApplicationName(RELOADED)
                        .register(new TestResource(new ReloadedResult()))
                        .register(new AppEventListener(new ReloadedResult())));
                return "get";
            }

            @Override
            public void onStartup(Container container) {
                this.container = container;
                testResultTracker.startup();
            }

            @Override
            public void onReload(Container container) {
                testResultTracker.reloaded();
            }

            @Override
            public void onShutdown(Container container) {
                testResultTracker.shutdown();
            }
        }

        public static class AppEventListener implements ApplicationEventListener {

            private final TestResultTracker resultTracker;

            public AppEventListener(TestResultTracker resultTracker) {
                this.resultTracker = resultTracker;
            }

            @Override
            public void onEvent(ApplicationEvent event) {
                switch (event.getType()) {
                    case INITIALIZATION_FINISHED:
                        resultTracker.initEvent();
                        break;
                    case RELOAD_FINISHED:
                        resultTracker.reloadedEvent();
                        break;
                }
            }

            @Override
            public RequestEventListener onRequest(RequestEvent requestEvent) {
                return null;
            }
        }

        /**
         * Tests that monitoring and container events are correctly called when application is created and redeployed.
         * It also checks that MBeans are exposed and deregistered when the application is undeployed. Test contains
         * waits and timeouts as monitoring events are processed asynchronously and it might take some time
         * until MBeans are registered. The test deploys original application, then reload is initiated and
         * another application is deployed.
         *
         * @throws MalformedObjectNameException
         * @throws AttributeNotFoundException
         * @throws MBeanException
         * @throws ReflectionException
         * @throws InstanceNotFoundException
         * @throws InterruptedException
         */
        @Test
        public void testApplicationEvents() throws MalformedObjectNameException, AttributeNotFoundException,
                MBeanException, ReflectionException, InstanceNotFoundException, InterruptedException {
            // wait to expose MBeans in the ORIGINAL application
            Thread.sleep(700);

            // before reload
            assertTrue(OriginalResult.startupCalled);
            assertTrue(OriginalResult.initEventCalled);
            assertFalse(OriginalResult.shutdownCalled);

            assertFalse(ReloadedResult.reloadedCalled);
            assertFalse(ReloadedResult.startupCalled);
            assertFalse(ReloadedResult.initEventCalled);

            checkMBeanRegistration(ORIGINAL, true);
            checkMBeanRegistration(RELOADED, false);

            // now cause the reload:
            final Response response = target().path("resource").request().get();
            assertEquals(200, response.getStatus());

            int cnt = 0;
            while ((!ReloadedResult.initEventCalled) && (cnt++ < 30)) {
                Thread.sleep(200);
            }
            assertTrue("Timeout: application was not reloaded in time.", ReloadedResult.initEventCalled);
            // wait again some time until events are processed and mbeans are invoked
            Thread.sleep(700);

            // after reload
            assertFalse(OriginalResult.reloadedCalled);
            assertFalse(OriginalResult.reloadedEventCalled);
            assertTrue(OriginalResult.shutdownCalled);
            assertTrue(ReloadedResult.startupCalled);
            assertTrue(ReloadedResult.initEventCalled);
            assertTrue(ReloadedResult.reloadedCalled);
            assertTrue(ReloadedResult.reloadedEventCalled);

            checkMBeanRegistration(ORIGINAL, false);
            checkMBeanRegistration(RELOADED, true);
        }

        private void checkMBeanRegistration(String appName, boolean shouldBeRegistered)
                throws MalformedObjectNameException,
                MBeanException, AttributeNotFoundException, InstanceNotFoundException, ReflectionException,
                InterruptedException {

            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            final ObjectName name = new ObjectName(
                    "org.glassfish.jersey:type=" + appName + ",subType=Global,global=Configuration");
            boolean registered = mBeanServer.isRegistered(name);

            int time = 0;
            while (shouldBeRegistered && !registered && time < 4000) {
                // wait until MBeans are asynchronously exposed
                int waitTime = 300;
                time += waitTime;
                Thread.sleep(waitTime);
                registered = mBeanServer.isRegistered(name);
            }
            Assert.assertEquals(shouldBeRegistered, registered);
            if (registered) {
                final String str = (String) mBeanServer.getAttribute(name, "ApplicationName");
                Assert.assertEquals(appName, str);
            }
        }
    }

}
