/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.server.internal.inject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Uri;
import javax.ws.rs.client.WebTarget;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.RequestContextBuilder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class UriTest extends AbstractTest {

    @Path("test")
    public static class Resource1 {

        @Uri("http://oracle.com") WebTarget webTarget1;

        @GET
        @Path("1")
        public String doGet1() {
            return webTarget1.getUri().toString();
        }

        @GET
        @Path("2")
        public String doGet2(@Uri("http://oracle.com") WebTarget webTarget2) {
            return webTarget2.getUri().toString();
        }
    }

    @Path("test")
    public static class Resource2 {

        @Uri("http://oracle.com/{param}") WebTarget webTarget1;

        @GET
        @Path("1")
        public String doGet1() {
            return webTarget1.getUri() == null ? "null" : webTarget1.getUri().toString();
        }

        @GET
        @Path("{param}")
        public String doGet2(@Uri("http://oracle.com/{param}") WebTarget webTarget2) {
            return webTarget2.getUri().toString();
        }
    }

    @Path("test")
    public static class Resource3 {

        @Uri("{param}") WebTarget webTarget1;

        @GET
        @Path("1")
        public String doGet1() {
            return webTarget1.getUri() == null ? "null" : webTarget1.getUri().toString();
        }

        @GET
        @Path("{param}")
        public String doGet2(@Uri("{param}") WebTarget webTarget2) {
            return webTarget2.getUri().toString();
        }
    }

    @Path("test")
    public static class Resource4 {

        @Uri("http://oracle.com") WebTarget webTarget1;

        @GET
        @Path("1")
        public String doGet1() {
            return (String) webTarget1.configuration().getProperties().get("test-property");
        }

        @GET
        @Path("2")
        public String doGet2(@Uri("http://oracle.com") WebTarget webTarget2) {
            return (String) webTarget2.configuration().getProperties().get("test-property");
        }

    }


    @Test
    public void testGet1() throws ExecutionException, InterruptedException {
        initiateWebApplication(Resource1.class);

        final ContainerResponse response = apply(
                RequestContextBuilder.from("/test/1", "GET").
                        build()
        );

        assertEquals("http://oracle.com", response.getEntity());
    }

    @Test
    public void testGet2() throws ExecutionException, InterruptedException {
        initiateWebApplication(Resource1.class);

        final ContainerResponse response = apply(
                RequestContextBuilder.from("/test/2", "GET").
                        build()
        );

        assertEquals("http://oracle.com", response.getEntity());
    }


    @Test
    public void testGetParam1() throws ExecutionException, InterruptedException {
        initiateWebApplication(Resource2.class);

        final ContainerResponse response = apply(
                RequestContextBuilder.from("/test/1", "GET").
                        build()
        );

        assertEquals(null, response.getEntity());
    }

    @Test
    public void testGetParam2() throws ExecutionException, InterruptedException {
        initiateWebApplication(Resource2.class);

        final ContainerResponse response = apply(
                RequestContextBuilder.from("/test/parameter", "GET").
                        build()
        );

        assertEquals("http://oracle.com/parameter", response.getEntity());
    }


    @Test
    public void testGetRelative1() throws ExecutionException, InterruptedException {
        initiateWebApplication(Resource3.class);

        final ContainerResponse response = apply(
                RequestContextBuilder.from("/test/1", "GET").
                        build()
        );

        assertEquals(null, response.getEntity());
    }

    @Test
    public void testGetRelative2() throws ExecutionException, InterruptedException {
        initiateWebApplication(Resource3.class);

        final ContainerResponse response = apply(
                RequestContextBuilder.from("/test/parameter", "GET").
                        build()
        );

        assertEquals("/parameter", response.getEntity());
    }

    @Test
    public void testConfiguredInjection1() throws ExecutionException, InterruptedException {
        final ResourceConfig resourceConfig = new ResourceConfig(Resource4.class);
        final ClientConfig clientConfig = new ClientConfig();
        clientConfig.setProperty("test-property", "test-value");
        Map<String, ClientConfig> clientConfigMap = new HashMap<String, ClientConfig>();
        clientConfigMap.put("http://oracle.com", clientConfig);
        resourceConfig.setProperty(ServerProperties.WEBTARGET_CONFIGURATION, clientConfigMap);

        initiateWebApplication(resourceConfig);

        final ContainerResponse response = apply(
                RequestContextBuilder.from("/test/1", "GET").
                        build()
        );

        assertEquals("test-value", response.getEntity());
    }

    @Test
    public void testConfiguredInjection2() throws ExecutionException, InterruptedException {
        final ResourceConfig resourceConfig = new ResourceConfig(Resource4.class);
        final ClientConfig clientConfig = new ClientConfig();
        clientConfig.setProperty("test-property", "test-value");
        Map<String, ClientConfig> clientConfigMap = new HashMap<String, ClientConfig>();
        clientConfigMap.put("http://oracle.com", clientConfig);
        resourceConfig.setProperty(ServerProperties.WEBTARGET_CONFIGURATION, clientConfigMap);

        initiateWebApplication(resourceConfig);

        final ContainerResponse response = apply(
                RequestContextBuilder.from("/test/2", "GET").
                        build()
        );

        assertEquals("test-value", response.getEntity());
    }
}
