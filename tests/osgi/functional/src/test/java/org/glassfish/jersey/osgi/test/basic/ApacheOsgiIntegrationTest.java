/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.osgi.test.basic;

import java.net.URI;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.osgi.test.util.Helper;
import org.glassfish.jersey.server.ResourceConfig;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;

import static org.junit.Assert.assertEquals;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;

/**
 * @author Adam Lindenthal (adam.lindenthal at oracle.com)
 */
@RunWith(PaxExam.class)
public class ApacheOsgiIntegrationTest {

    private static final URI baseUri = UriBuilder.fromUri("http://localhost").port(Helper.getPort()).path("/jersey").build();

    @Configuration
    public static Option[] configuration() {
        final List<Option> options = Helper.getCommonOsgiOptions();
        options.addAll(Helper.expandedList(
                mavenBundle().groupId("org.ops4j.pax.logging").artifactId("pax-logging-api").versionAsInProject(),
                mavenBundle().groupId("org.apache.httpcomponents").artifactId("httpcore-osgi").versionAsInProject(),
                mavenBundle().groupId("org.apache.httpcomponents").artifactId("httpclient-osgi").versionAsInProject(),
                mavenBundle().groupId("org.glassfish.jersey.connectors").artifactId("jersey-apache-connector")
                        .versionAsInProject()

        ));
        return Helper.asArray(options);
    }

    @Path("/apacheOsgiTest")
    public static class ApacheOsgiTestResource {

        @GET
        public String getMe() {
            return "OK";
        }
    }

    @Test
    public void testSimpleResource() throws Exception {
        final ResourceConfig resourceConfig = new ResourceConfig(ApacheOsgiTestResource.class);
        final HttpServer server = GrizzlyHttpServerFactory.createHttpServer(baseUri, resourceConfig);

        final ClientConfig clientConfig = new ClientConfig();
        clientConfig.connectorProvider(new ApacheConnectorProvider());
        final Client c = ClientBuilder.newClient(clientConfig);

        final Response response = c.target(baseUri).path("/apacheOsgiTest").request().buildGet().invoke();

        final String result = response.readEntity(String.class);
        assertEquals("OK", result);
        server.shutdownNow();
    }
}
