/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

import javax.ws.rs.client.Client;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.ext.ClientFactory;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.GrizzlyHttpServerFactory;
import org.glassfish.jersey.media.json.JsonJacksonModule;
import org.glassfish.jersey.osgi.test.util.Helper;
import org.glassfish.jersey.server.ResourceConfig;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.BundleContext;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;


import static org.ops4j.pax.exam.CoreOptions.felix;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemPackage;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.repositories;

import static org.junit.Assert.assertTrue;



@RunWith(JUnit4TestRunner.class)
public class JsonTest {

    private static final int port = Helper.getEnvVariable("JERSEY_HTTP_PORT", 8080);

    private static final String CONTEXT = "/jersey";
    private static final URI baseUri = UriBuilder.fromUri("http://localhost").port(port).path(CONTEXT).build();

    @Inject
    protected BundleContext bundleContext;

    @Configuration
    public static Option[] configuration() {

        Option[] options = options(
//                systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("FINEST"),
                systemProperty("org.osgi.service.http.port").value(String.valueOf(port)),
                systemPackage("sun.misc"),
                // define maven repository
                repositories(
                "http://repo1.maven.org/maven2",
                "http://repository.apache.org/content/groups/snapshots-group",
                "http://repository.ops4j.org/maven2",
                "http://svn.apache.org/repos/asf/servicemix/m2-repo",
                "http://repository.springsource.com/maven/bundles/release",
                "http://repository.springsource.com/maven/bundles/external",
                "http://maven.java.net/content/repositories/snapshots"),

                // log
//                mavenBundle("org.ops4j.pax.logging", "pax-logging-api", "1.4"),
//                mavenBundle("org.ops4j.pax.logging", "pax-logging-service", "1.4"),

                // felix config admin
//                mavenBundle("org.apache.felix", "org.apache.felix.configadmin", "1.2.4"),

                // felix preference service
//                mavenBundle("org.apache.felix", "org.apache.felix.prefs","1.0.2"),

                // HTTP SPEC
                //mavenBundle("org.apache.geronimo.specs","geronimo-servlet_2.5_spec","1.1.2"),

                // Google Guava
                mavenBundle().groupId("com.googlecode.guava-osgi").artifactId("guava-osgi").versionAsInProject(),

                wrappedBundle(mavenBundle().groupId("javax.mail").artifactId("mail").versionAsInProject()),
                wrappedBundle(mavenBundle().groupId("javax.activation").artifactId("activation").versionAsInProject()),

                // HK2
                mavenBundle().groupId("org.glassfish.hk2").artifactId("hk2-api").versionAsInProject(),
                mavenBundle().groupId("org.glassfish.hk2").artifactId("osgi-resource-locator").versionAsInProject(),
                mavenBundle().groupId("org.glassfish.hk2").artifactId("auto-depends").versionAsInProject(),
                mavenBundle().groupId("org.glassfish.hk2.external").artifactId("javax.inject").versionAsInProject(),
                mavenBundle().groupId("org.glassfish.hk2.external").artifactId("asm-all-repackaged").versionAsInProject(),

                // JAX-RS API
                mavenBundle().groupId("javax.ws.rs").artifactId("javax.ws.rs-api").versionAsInProject(),

                // Jersey bundles
                mavenBundle().groupId("org.glassfish.jersey.core").artifactId("jersey-common").versionAsInProject(),
                mavenBundle().groupId("org.glassfish.jersey.core").artifactId("jersey-server").versionAsInProject(),
                mavenBundle().groupId("org.glassfish.jersey.core").artifactId("jersey-client").versionAsInProject(),
                mavenBundle().groupId("org.glassfish.jersey.containers").artifactId("jersey-container-grizzly2-http").versionAsInProject(),
                mavenBundle().groupId("org.glassfish.jersey.media").artifactId("jersey-media-json").versionAsInProject(),

                // Grizzly
                mavenBundle().groupId("org.glassfish.grizzly").artifactId("grizzly-http-server").versionAsInProject(),
                mavenBundle().groupId("org.glassfish.grizzly").artifactId("grizzly-rcm").versionAsInProject(),
                mavenBundle().groupId("org.glassfish.grizzly").artifactId("grizzly-http").versionAsInProject(),
                mavenBundle().groupId("org.glassfish.grizzly").artifactId("grizzly-framework").versionAsInProject(),
                mavenBundle().groupId("org.glassfish.gmbal").artifactId("gmbal-api-only").versionAsInProject(),
                mavenBundle().groupId("org.glassfish.external").artifactId("management-api").versionAsInProject(),

                // jersey-json deps
                mavenBundle().groupId("org.codehaus.jackson").artifactId("jackson-core-asl").versionAsInProject(),
                mavenBundle().groupId("org.codehaus.jackson").artifactId("jackson-mapper-asl").versionAsInProject(),
                mavenBundle().groupId("org.codehaus.jackson").artifactId("jackson-jaxrs").versionAsInProject(),
                mavenBundle().groupId("org.codehaus.jettison").artifactId("jettison").versionAsInProject(),

                // start felix framework
                felix());

        return options;
    }


    // TODO: the test is unreliable, the server returns null occasionally
    @Ignore
    @Test
    public void testJson() throws Exception {

        final ResourceConfig resourceConfig = new ResourceConfig(JsonResource.class).addModules(new JsonJacksonModule());
        final HttpServer server = GrizzlyHttpServerFactory.createHttpServer(baseUri, resourceConfig);

        Client c = ClientFactory.newClient();

        // TODO: enable JSON on the client side once JERSEY-1083 gets resolved
//        c.configuration().enable(new JsonFeature());

        final Response response = c.target(baseUri).path("/json").request(MediaType.APPLICATION_JSON).buildGet().invoke();
        String result = response.readEntity(String.class);
        System.out.println("RESULT = " + result);

        assertTrue(result.contains("Jim"));

        server.stop();
    }
}
