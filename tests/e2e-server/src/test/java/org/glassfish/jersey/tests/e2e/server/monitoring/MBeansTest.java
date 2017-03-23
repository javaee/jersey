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

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.monitoring.MonitoringStatistics;
import org.glassfish.jersey.server.monitoring.MonitoringStatisticsListener;
import org.glassfish.jersey.server.spi.AbstractContainerLifecycleListener;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Miroslav Fuksa
 */
public class MBeansTest extends JerseyTest {

    @Override
    protected Application configure() {
        final ResourceConfig resourceConfig = new ResourceConfig(TestResource.class, MyExceptionMapper.class);
        resourceConfig.setApplicationName("myApplication");
        resourceConfig.property("very-important", "yes");
        resourceConfig.property("another-property", 48);
        resourceConfig.property(ServerProperties.MONITORING_STATISTICS_MBEANS_ENABLED, true);
        resourceConfig.register(StatisticsListener.class);
        return resourceConfig;
    }

    public static class MyException extends RuntimeException {

        public MyException(String message) {
            super(message);
        }
    }

    public static class MyExceptionMapper implements ExceptionMapper<MyException> {

        @Override
        public Response toResponse(MyException exception) {
            return Response.ok("mapped").build();
        }
    }

    @Path("resource")
    public static class TestResource {

        @GET
        public String testGet() {
            return "get";
        }

        @GET
        @Path("test/{test: \\d+}")
        public String testGetPathPattern1() {
            return "testGetPathPattern1";
        }

        @GET
        @Path("test2/{test: hell?o}")
        public String testGetPathPattern2() {
            return "testGetPathPattern2";
        }

        @GET
        @Path("test3/{test: abc.* (a)(b)[a,c]?$[1-4]kkx|Y}")
        public String testGetPathPattern3() {
            return "testGetPathPattern2";
        }

        @GET
        @Path("test4/{test: [a,b]:r}")
        public String testGetPathPattern4() {
            return "testGetPathPattern2";
        }

        @POST
        public String testPost() {
            return "post";
        }

        @GET
        @Path("sub")
        public String testSubGet() {
            return "sub";
        }

        @GET
        @Path("exception")
        public String testException() {
            throw new MyException("test");
        }

        @POST
        @Path("sub2")
        @Produces("text/html")
        @Consumes("text/plain")
        public String testSu2bPost(String entity) {
            return "post";
        }

        @Path("locator")
        public SubResource getSubResource() {
            return new SubResource();
        }
    }

    public static class StatisticsListener extends AbstractContainerLifecycleListener implements MonitoringStatisticsListener {

        public static boolean ON_SHUTDOWN_CALLED = false;

        @Override
        public void onStatistics(MonitoringStatistics statistics) {
            // do nothing
        }

        @Override
        public void onShutdown(Container container) {
            StatisticsListener.ON_SHUTDOWN_CALLED = true;
        }
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        Assert.assertTrue(StatisticsListener.ON_SHUTDOWN_CALLED);

    }

    public static class SubResource {

        @GET
        @Path("in-subresource")
        public String get() {
            return "inSubResource";
        }

        @Path("locator")
        public SubResource getSubResource() {
            return new SubResource();
        }
    }

    @Test
    public void test() throws Exception {
        final String path = "resource";
        assertEquals(200, target().path(path).request().get().getStatus());
        assertEquals(200, target().path(path).request().post(Entity.entity("post",
                MediaType.TEXT_PLAIN_TYPE)).getStatus());
        assertEquals(200, target().path(path).request().post(Entity.entity("post",
                MediaType.TEXT_PLAIN_TYPE)).getStatus());
        assertEquals(200, target().path(path).request().post(Entity.entity("post",
                MediaType.TEXT_PLAIN_TYPE)).getStatus());
        assertEquals(200, target().path(path).request().post(Entity.entity("post",
                MediaType.TEXT_PLAIN_TYPE)).getStatus());
        assertEquals(200, target().path(path + "/sub2").request().post(Entity.entity("post",
                MediaType.TEXT_PLAIN_TYPE)).getStatus());
        final Response response = target().path(path + "/exception").request().get();
        assertEquals(200, response.getStatus());
        assertEquals("mapped", response.readEntity(String.class));

        assertEquals(200, target().path("resource/sub").request().get().getStatus());
        assertEquals(200, target().path("resource/sub").request().get().getStatus());
        assertEquals(200, target().path("resource/locator/in-subresource").request().get().getStatus());
        assertEquals(200, target().path("resource/locator/locator/in-subresource").request().get().getStatus());
        assertEquals(200, target().path("resource/locator/locator/locator/in-subresource").request().get().getStatus());
        assertEquals(404, target().path("resource/not-found-404").request().get().getStatus());

        // wait until statistics are propagated to mxbeans
        Thread.sleep(1500);

        final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        final ObjectName name = new ObjectName("org.glassfish.jersey:type=myApplication,subType=Global,global=Configuration");
        final String str = (String) mBeanServer.getAttribute(name, "ApplicationName");
        Assert.assertEquals("myApplication", str);

        checkResourceMBean("/resource");
        checkResourceMBean("/resource/sub");
        checkResourceMBean("/resource/locator");
        checkResourceMBean("/resource/exception");
        checkResourceMBean("/resource/test/{test: \\\\d+}");
        checkResourceMBean("/resource/test2/{test: hell\\?o}");
        checkResourceMBean("/resource/test3/{test: abc.\\* (a)(b)[a,c]\\?$[1-4]kkx|Y}");
        checkResourceMBean("/resource/test4/{test: [a,b]:r}");
    }

    private void checkResourceMBean(String name) throws MalformedObjectNameException {
        final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        final ObjectName objectName = new ObjectName(
                "org.glassfish.jersey:type=myApplication,subType=Uris,resource=\"" + name + "\"");
        ObjectInstance mbean = null;
        try {
            mbean = mBeanServer.getObjectInstance(objectName);
        } catch (InstanceNotFoundException e) {
            Assert.fail("Resource MBean name '" + name + "' not found.");
        }
        assertNotNull(mbean);
    }

    // this test runs the jersey environments, exposes mbeans and makes requests to
    // the deployed application. The test will never finished. This should be uncommented
    // only for development testing of mbeans in jconsole.
    // Steps: uncomment the test; run it; run jconsole and attach to the process of the tests
    //    @Test
    //    public void testNeverFinishesAndMustBeCommented() throws Exception {
    //        while (true) {
    //            test();
    //        }
    //    }
}
