/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.examples.extendedwadl;

import java.net.URI;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientFactory;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.examples.extendedwadl.resources.ItemResource;
import org.glassfish.jersey.examples.extendedwadl.resources.ItemsResource;
import org.glassfish.jersey.examples.extendedwadl.resources.MyApplication;
import org.glassfish.jersey.examples.extendedwadl.util.Examples;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.message.internal.MediaTypes;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;

import org.glassfish.grizzly.http.server.HttpServer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.swissbox.tinybundles.core.TinyBundles;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.felix;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.CoreOptions.systemPackage;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.rawPaxRunnerOption;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.repositories;

import aQute.lib.osgi.Constants;

/**
 *
 * @author Naresh
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
@RunWith(JUnit4TestRunner.class)
public class ExtendedWadlWebappOsgiTest {

    private static final URI baseUri = UriBuilder.
            fromUri("http://localhost").
            port(8080).
            path("extended-wadl-webapp").build();

    @Configuration
    public static Option[] configuration() {
        return options(
                // systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("FINEST"),
                rawPaxRunnerOption("clean"),

                // define maven repositories
                repositories("http://repo1.maven.org/maven2",
                        "http://repository.apache.org/content/groups/snapshots-group",
                        "http://repository.ops4j.org/maven2",
                        "http://svn.apache.org/repos/asf/servicemix/m2-repo",
                        "http://repository.springsource.com/maven/bundles/release",
                        "http://repository.springsource.com/maven/bundles/external",
                        "http://maven.java.net/content/repositories/snapshots"),

                // log
                // mavenBundle("org.ops4j.pax.logging", "pax-logging-api", "1.4"),
                // mavenBundle("org.ops4j.pax.logging", "pax-logging-service", "1.4"),

                // felix config admin
                // mavenBundle("org.apache.felix", "org.apache.felix.configadmin", "1.2.4"),

                // felix preference service
                // mavenBundle("org.apache.felix", "org.apache.felix.prefs","1.0.2"),


                // Google Guava
                mavenBundle().groupId("com.google.guava").artifactId("guava").versionAsInProject(),

                // HK2
                mavenBundle().groupId("org.glassfish.hk2").artifactId("hk2-api").versionAsInProject(),
                mavenBundle().groupId("org.glassfish.hk2").artifactId("osgi-resource-locator").versionAsInProject(),
                mavenBundle().groupId("org.glassfish.hk2").artifactId("hk2-locator").versionAsInProject(),
                mavenBundle().groupId("org.glassfish.hk2").artifactId("hk2-utils").versionAsInProject(),
                mavenBundle().groupId("org.glassfish.hk2.external").artifactId("javax.inject").versionAsInProject(),
                mavenBundle().groupId("org.glassfish.hk2.external").artifactId("asm-all-repackaged").versionAsInProject(),
                mavenBundle().groupId("org.glassfish.hk2.external").artifactId("cglib").versionAsInProject(),

                // JAX-RS API
                mavenBundle().groupId("javax.ws.rs").artifactId("javax.ws.rs-api").versionAsInProject(),

                // Jersey bundles
                mavenBundle().groupId("org.glassfish.jersey.core").artifactId("jersey-common").versionAsInProject(),
                mavenBundle().groupId("org.glassfish.jersey.core").artifactId("jersey-server").versionAsInProject(),
                mavenBundle().groupId("org.glassfish.jersey.core").artifactId("jersey-client").versionAsInProject(),

                // javax.annotation
//                wrappedBundle(mavenBundle().groupId("javax.annotation").artifactId("jsr250-api").versionAsInProject()),

                // jettison
                mavenBundle().groupId("org.codehaus.jettison").artifactId("jettison").versionAsInProject(),

                // validation
                mavenBundle().groupId("javax.validation").artifactId("validation-api").versionAsInProject(),

                // Grizzly
                systemPackage("sun.misc"),
                mavenBundle().groupId("org.glassfish.grizzly").artifactId("grizzly-framework").versionAsInProject(),
                mavenBundle().groupId("org.glassfish.grizzly").artifactId("grizzly-rcm").versionAsInProject(),
                mavenBundle().groupId("org.glassfish.grizzly").artifactId("grizzly-http").versionAsInProject(),
                mavenBundle().groupId("org.glassfish.grizzly").artifactId("grizzly-http-server").versionAsInProject(),

                // Jersey Grizzly
                mavenBundle().groupId("org.glassfish.jersey.containers").artifactId("jersey-container-grizzly2-http")
                        .versionAsInProject(),
                provision(TinyBundles.newBundle()
                                        .add(MyApplication.class)
                                        .add(ItemResource.class)
                                        .add(ItemsResource.class)
                                        .add(Examples.class)
                                        .add(SampleWadlGeneratorConfig.class)
                                        .add(Item.class)
                                        .add(Items.class)
                                        .add(ObjectFactory.class)
                                        .add("application-doc.xml", ClassLoader.getSystemResourceAsStream("application-doc.xml"))
                                        .add("application-grammars.xml", ClassLoader.getSystemResourceAsStream("application-grammars.xml"))
                                        .add("resourcedoc.xml", ClassLoader.getSystemResourceAsStream("resourcedoc.xml"))
                                        .set(Constants.EXPORT_PACKAGE, MyApplication.class.getPackage().getName() + "," + SampleWadlGeneratorConfig.class.getPackage().getName())
                                        .set(Constants.IMPORT_PACKAGE, "*")
                                        .set(Constants.BUNDLE_SYMBOLICNAME, "webapp").build(TinyBundles.withBnd())),
                // start felix framework
                felix());
    }


    ResourceConfig createResourceConfig() {
        final ResourceConfig resourceConfig = new ResourceConfig(new MyApplication().getClasses());
        resourceConfig.setProperty(ServerProperties.PROPERTY_WADL_GENERATOR_CONFIG,
                SampleWadlGeneratorConfig.class.getName());
        return resourceConfig;
    }

    /**
     * Test checks that the WADL generated using the WadlGenerator api doesn't
     * contain the expected text.
     * @throws java.lang.Exception
     */
    @Test
    public void testExtendedWadl() throws Exception {

        final ResourceConfig resourceConfig = createResourceConfig();

        final HttpServer server = GrizzlyHttpServerFactory.createHttpServer(baseUri, resourceConfig);
        final Client client = ClientFactory.newClient();

        final Response response = client.target(baseUri).path("application.wadl").request(MediaTypes.WADL).buildGet().invoke();

        String wadl = response.readEntity(String.class);
        System.out.println("RESULT = " + wadl);

        assertTrue("Generated wadl is of null length", wadl.length() > 0);
        assertTrue("Generated wadl doesn't contain the expected text",
                wadl.contains("This is a paragraph"));

        assertFalse(wadl.contains("application.wadl/xsd0.xsd"));

        server.stop();
    }

    @Test
    public void testWadlOptionsMethod() throws Exception {

        final ResourceConfig resourceConfig = createResourceConfig();

        final HttpServer server = GrizzlyHttpServerFactory.createHttpServer(baseUri, resourceConfig);
        final Client client = ClientFactory.newClient();

        String wadl = client.target(baseUri).path("items").request(MediaTypes.WADL).options(String.class);

        assertTrue("Generated wadl is of null length", wadl.length() > 0);
        assertTrue("Generated wadl doesn't contain the expected text",
                wadl.contains("This is a paragraph"));

        server.stop();
    }
}
