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

import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import javax.inject.Provider;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.monitoring.MonitoringStatistics;
import org.glassfish.jersey.server.monitoring.ResourceMethodStatistics;
import org.glassfish.jersey.server.monitoring.ResourceStatistics;
import org.glassfish.jersey.server.wadl.processor.WadlModelProcessor;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * This test verifies that {@link ResourceMethodStatistics} are not duplicated in
 * {@link MonitoringStatistics} when sub resource locators are used. Sub resources and their
 * methods should be mapped to currently existing {@link ResourceStatistics} and their
 * {@link ResourceMethodStatistics}.
 *
 * @author Miroslav Fuksa
 * @author Libor Kramolis (libor.kramolis at oracle.com)
 */
public class MonitoringStatisticsLocatorTest extends JerseyTest {

    @Override
    protected Application configure() {
        final ResourceConfig resourceConfig = new ResourceConfig(StatisticsResource.class, AnotherResource.class);
        resourceConfig.property(ServerProperties.MONITORING_STATISTICS_ENABLED, true);
        resourceConfig.property(ServerProperties.APPLICATION_NAME, "testApp");
        return resourceConfig;
    }

    @Path("resource")
    public static class StatisticsResource {

        @Context
        Provider<MonitoringStatistics> statistics;

        @GET
        public String getStats() throws InterruptedException {
            final MonitoringStatistics monitoringStatistics = statistics.get();
            final ResourceStatistics resourceStatistics = monitoringStatistics.getResourceClassStatistics()
                    .get(SubResource.class);
            if (resourceStatistics == null) {
                return "null";
            }

            String resp = "";

            for (final Map.Entry<ResourceMethod, ResourceMethodStatistics> entry
                    : resourceStatistics.getResourceMethodStatistics().entrySet()) {
                if (entry.getKey().getHttpMethod().equals("GET")) {
                    resp = resp + "getFound";
                }
            }
            return resp;
        }

        @GET
        @Path("uri")
        public String getUriStats() throws InterruptedException {
            final MonitoringStatistics monitoringStatistics = statistics.get();
            final ResourceStatistics resourceStatistics = monitoringStatistics.getUriStatistics()
                    .get("/resource/resource-locator");
            if (resourceStatistics == null) {
                return "null";
            }

            String resp = "";

            for (final Map.Entry<ResourceMethod, ResourceMethodStatistics> entry
                    : resourceStatistics.getResourceMethodStatistics().entrySet()) {
                if (entry.getKey().getHttpMethod().equals("GET")) {
                    resp = resp + "getFound";
                }
            }

            return resp;
        }

        @Path("resource-locator")
        public SubResource locator() {
            return new SubResource();
        }

        @Path("hello")
        @GET
        @Produces("text/plain")
        public String hello() {
            return "Hello!";
        }

        @GET
        @Path("resourceClassStatisticsWadlOptionsTest")
        public String getResourceClassStatisticsWadlOptionsTest() {
            return getResourceClassStatisticsTest(WadlModelProcessor.OptionsHandler.class.getName());
        }

        @GET
        @Path("resourceClassStatisticsGenericOptionsTest")
        public String getResourceClassStatisticsGenericOptionsTest() {
            return getResourceClassStatisticsTest(
                    "org.glassfish.jersey.server.wadl.processor.OptionsMethodProcessor$GenericOptionsInflector");
        }

        @GET
        @Path("resourceClassStatisticsPlainTextOptionsTest")
        public String getResourceClassStatisticsPlainTestOptionsTest() {
            return getResourceClassStatisticsTest(
                    "org.glassfish.jersey.server.wadl.processor.OptionsMethodProcessor$PlainTextOptionsInflector");
        }

        private String getResourceClassStatisticsTest(final String resourceClassName) {
            final ResourceStatistics resourceMethodStatistics = findResourceClassStatistics(statistics.get(), resourceClassName);

            boolean resourceHelloOptions = false;
            boolean anotherHelloOptions = false;
            boolean anotherXmlOptions = false;
            for (final Map.Entry<ResourceMethod, ResourceMethodStatistics> entry : resourceMethodStatistics
                    .getResourceMethodStatistics().entrySet()) {
                final ResourceMethod resourceMethod = entry.getKey();
                final String fullPath = getFullPath(resourceMethod);
                if ("/resource/hello".equals(fullPath)) {
                    resourceHelloOptions = true;
                } else if ("/another/hello".equals(fullPath)) {
                    anotherHelloOptions = true;
                } else if ("/another/xml".equals(fullPath)) {
                    anotherXmlOptions = true;
                }
            }
            if (resourceHelloOptions && anotherHelloOptions && anotherXmlOptions) {
                return "OK";
            } else {
                return "FAIL: /resource/hello=" + resourceHelloOptions + "; /another/hello=" + anotherHelloOptions
                        + "; /another/xml=" + anotherXmlOptions;
            }
        }

        @GET
        @Path("uriStatisticsResourceHelloTest")
        public String getUriStatisticsResourceHelloTest() {
            return getUriStatisticsTest("/resource/hello");
        }

        @GET
        @Path("uriStatisticsAnotherHelloTest")
        public String getUriStatisticsAnotherHelloTest() {
            return getUriStatisticsTest("/another/hello");
        }

        @GET
        @Path("uriStatisticsAnotherXmlTest")
        public String getUriStatisticsAnotherXmlTest() {
            return getUriStatisticsTest("/another/xml");
        }

        private String getUriStatisticsTest(final String uri) {
            boolean plainTextOptions = false;
            boolean wadlOptions = false;
            boolean genericOptions = false;
            final ResourceStatistics resourceStatistics = statistics.get().getUriStatistics().get(uri);

            for (final Map.Entry<ResourceMethod, ResourceMethodStatistics> entry : resourceStatistics
                    .getResourceMethodStatistics().entrySet()) {
                if (entry.getKey().getHttpMethod().equals("OPTIONS")) {
                    final ResourceMethod resourceMethod = entry.getKey();
                    final String producedTypes = resourceMethod.getProducedTypes().toString();
                    if ("[text/plain]".equals(producedTypes)) {
                        plainTextOptions = true;
                    } else if ("[application/vnd.sun.wadl+xml]".equals(producedTypes)) {
                        wadlOptions = true;
                    } else if ("[*/*]".equals(producedTypes)) {
                        genericOptions = true;
                    }
                }
            }
            if (plainTextOptions && wadlOptions && genericOptions) {
                return "OK";
            } else {
                return "FAIL: [text/plain]=" + plainTextOptions + "; [application/vnd.sun.wadl+xml]=" + wadlOptions
                        + "; [*/*]=" + genericOptions;
            }
        }

        private ResourceStatistics findResourceClassStatistics(final MonitoringStatistics monitoringStatistics,
                                                               final String resourceClassName) {
            for (final Map.Entry<Class<?>, ResourceStatistics> entry : monitoringStatistics.getResourceClassStatistics()
                    .entrySet()) {
                final Class<?> key = entry.getKey();
                final String clazz = key.getName();

                if (clazz.equals(resourceClassName)) {
                    return entry.getValue();
                }
            }
            return null;
        }

        private static String getFullPath(final ResourceMethod resourceMethod) {
            final StringBuilder fullPath = new StringBuilder();
            if (resourceMethod != null) {
                prefixPath(fullPath, resourceMethod.getParent());
            }
            return fullPath.toString();
        }

        private static void prefixPath(final StringBuilder fullPath, final Resource parent) {
            if (parent != null) {
                String path = parent.getPath();
                if (path.startsWith("/")) {
                    path = path.substring(1);
                }
                fullPath.insert(0, "/" + path);
                prefixPath(fullPath, parent.getParent());
            }
        }

    }

    public static class SubResource {

        @GET
        public String get() {
            return "get";
        }

        @Path("sub")
        public SubResource subLocator() {
            return new SubResource();
        }

    }

    @Path("/another")
    public static class AnotherResource {

        @Path("hello")
        @GET
        @Produces("text/plain")
        public String sayHello() {
            return "Hello, again.";
        }

        @Path("xml")
        @GET
        @Produces(MediaType.TEXT_XML)
        public String sayXMLHello() {
            return "<?xml version=\"1.0\"?><hello>World!</hello>";
        }
    }

    @Test
    public void test() throws InterruptedException {
        Response response = target().path("resource").request().get();
        assertEquals(200, response.getStatus());
        assertEquals("null", response.readEntity(String.class));

        response = target().path("resource/resource-locator").request().get();
        assertEquals(200, response.getStatus());
        assertEquals("get", response.readEntity(String.class));

        response = target().path("resource/resource-locator").request().get();
        assertEquals(200, response.getStatus());
        assertEquals("get", response.readEntity(String.class));

        response = target().path("resource/resource-locator/sub").request().get();
        assertEquals(200, response.getStatus());
        assertEquals("get", response.readEntity(String.class));

        response = target().path("resource/hello").request().get();
        assertEquals(200, response.getStatus());
        assertEquals("Hello!", response.readEntity(String.class));

        response = target().path("another/hello").request().get();
        assertEquals(200, response.getStatus());
        assertEquals("Hello, again.", response.readEntity(String.class));

        response = target().path("another/xml").request().get();
        assertEquals(200, response.getStatus());
        assertEquals("<?xml version=\"1.0\"?><hello>World!</hello>", response.readEntity(String.class));

        Thread.sleep(600);

        response = target().path("resource").request().get();
        assertEquals(200, response.getStatus());
        assertEquals("getFound", response.readEntity(String.class));

        response = target().path("resource/uri").request().get();
        assertEquals(200, response.getStatus());
        assertEquals("getFound", response.readEntity(String.class));
    }

    @Test
    public void testResourceClassStatisticsWadlOptions() {
        final Response response = target().path("resource/resourceClassStatisticsWadlOptionsTest").request().get();
        assertEquals(200, response.getStatus());
        assertEquals("OK", response.readEntity(String.class));
    }

    @Test
    public void testResourceClassStatisticsGenericOptions() {
        final Response response = target().path("resource/resourceClassStatisticsGenericOptionsTest").request().get();
        assertEquals(200, response.getStatus());
        assertEquals("OK", response.readEntity(String.class));
    }

    @Test
    public void testResourceClassStatisticsPlainTextOptions() {
        final Response response = target().path("resource/resourceClassStatisticsPlainTextOptionsTest").request().get();
        assertEquals(200, response.getStatus());
        assertEquals("OK", response.readEntity(String.class));
    }

    @Test
    public void testUriStatisticsResourceHello() throws InterruptedException {
        Response response = target().path("resource/hello").request().get();
        assertEquals(200, response.getStatus());
        assertEquals("Hello!", response.readEntity(String.class));

        Thread.sleep(600);

        response = target().path("resource/uriStatisticsResourceHelloTest").request().get();
        assertEquals(200, response.getStatus());
        assertEquals("OK", response.readEntity(String.class));
    }

    @Test
    public void testUriStatisticsAnotherHello() throws InterruptedException {
        Response response = target().path("another/hello").request().get();
        assertEquals(200, response.getStatus());
        assertEquals("Hello, again.", response.readEntity(String.class));

        Thread.sleep(600);

        response = target().path("resource/uriStatisticsAnotherHelloTest").request().get();
        assertEquals(200, response.getStatus());
        assertEquals("OK", response.readEntity(String.class));
    }

    @Test
    public void testUriStatisticsAnotherXml() throws InterruptedException {
        Response response = target().path("another/xml").request().get();
        assertEquals(200, response.getStatus());
        assertEquals("<?xml version=\"1.0\"?><hello>World!</hello>", response.readEntity(String.class));

        Thread.sleep(600);

        response = target().path("resource/uriStatisticsAnotherXmlTest").request().get();
        assertEquals(200, response.getStatus());
        assertEquals("OK", response.readEntity(String.class));
    }
}
