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

package org.glassfish.jersey.tests.e2e.server.monitoring;

import java.util.Map;

import javax.inject.Provider;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.monitoring.MonitoringStatistics;
import org.glassfish.jersey.server.monitoring.ResourceMethodStatistics;
import org.glassfish.jersey.server.monitoring.ResourceStatistics;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Assert;
import org.junit.Test;

/**
 * This test verifies that {@link ResourceMethodStatistics} are not duplicated in
 * {@link MonitoringStatistics} when sub resource locators are used. Sub resources and their
 * methods should be mapped to currently existing {@link ResourceStatistics} and their
 * {@link ResourceMethodStatistics}.
 *
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 */
public class MonitoringStatisticsLocatorTest extends JerseyTest {

    @Override
    protected Application configure() {
        final ResourceConfig resourceConfig = new ResourceConfig(StatisticsResource.class);
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

            for (Map.Entry<ResourceMethod, ResourceMethodStatistics> entry
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


            for (Map.Entry<ResourceMethod, ResourceMethodStatistics> entry
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

    @Test
    public void test() throws InterruptedException {
        Response response = target().path("resource").request().get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("null", response.readEntity(String.class));

        response = target().path("resource/resource-locator").request().get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("get", response.readEntity(String.class));


        response = target().path("resource/resource-locator").request().get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("get", response.readEntity(String.class));


        response = target().path("resource/resource-locator/sub").request().get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("get", response.readEntity(String.class));

        Thread.sleep(600);

        response = target().path("resource").request().get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("getFound", response.readEntity(String.class));

        response = target().path("resource/uri").request().get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("getFound", response.readEntity(String.class));

    }
}