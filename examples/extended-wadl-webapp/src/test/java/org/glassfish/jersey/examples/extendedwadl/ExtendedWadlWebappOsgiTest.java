/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import javax.inject.Inject;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.glassfish.jersey.examples.extendedwadl.resources.ItemResource;
import org.glassfish.jersey.examples.extendedwadl.resources.ItemsResource;
import org.glassfish.jersey.examples.extendedwadl.resources.MyApplication;
import org.glassfish.jersey.examples.extendedwadl.util.Examples;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.internal.util.PropertiesHelper;
import org.glassfish.jersey.internal.util.SimpleNamespaceResolver;
import org.glassfish.jersey.message.internal.MediaTypes;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.wadl.internal.WadlUtils;
import org.glassfish.jersey.test.TestProperties;

import org.glassfish.grizzly.http.server.HttpServer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.w3c.dom.Document;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.CoreOptions.systemPackage;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.tinybundles.core.TinyBundles.bundle;

/**
 * @author Naresh
 * @author Miroslav Fuksa
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 * @author Adam Lindenthal (adam.lindenthal at oracle.com)
 */
@RunWith(PaxExam.class)
public class ExtendedWadlWebappOsgiTest {

    @Inject
    BundleContext bundleContext;

    private static final Logger LOGGER = Logger.getLogger(ExtendedWadlWebappOsgiTest.class.getName());

    // we want to re-use the port number as set for Jersey test container to avoid CT port number clashes
    private static final String testContainerPort = System.getProperty(TestProperties.CONTAINER_PORT);
    private static final int testPort = testContainerPort == null
            ? TestProperties.DEFAULT_CONTAINER_PORT : Integer.parseInt(testContainerPort);

    private static final URI baseUri = UriBuilder
            .fromUri("http://localhost")
            .port(testPort)
            .path("extended-wadl-webapp").build();

    @Configuration
    public static Option[] configuration() {
        List<Option> options = Arrays.asList(options(
                // systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("FINEST"),
                systemProperty("org.osgi.framework.system.packages.extra").value("javax.annotation"),

                // javax.annotation must go first!
                mavenBundle().groupId("javax.annotation").artifactId("javax.annotation-api").versionAsInProject(),

                junitBundles(),

                mavenBundle("org.ops4j.pax.url", "pax-url-mvn"),

                // HK2
                mavenBundle().groupId("org.glassfish.hk2").artifactId("hk2-api").versionAsInProject(),
                mavenBundle().groupId("org.glassfish.hk2").artifactId("osgi-resource-locator").versionAsInProject(),
                mavenBundle().groupId("org.glassfish.hk2").artifactId("hk2-locator").versionAsInProject(),
                mavenBundle().groupId("org.glassfish.hk2").artifactId("hk2-utils").versionAsInProject(),
                mavenBundle().groupId("org.glassfish.hk2.external").artifactId("javax.inject").versionAsInProject(),
                mavenBundle().groupId("org.glassfish.hk2.external").artifactId("aopalliance-repackaged").versionAsInProject(),
                mavenBundle().groupId("org.javassist").artifactId("javassist").versionAsInProject(),

                // JAX-RS API
                mavenBundle().groupId("javax.ws.rs").artifactId("javax.ws.rs-api").versionAsInProject(),

                // Jersey bundles
                mavenBundle().groupId("org.glassfish.jersey.core").artifactId("jersey-common").versionAsInProject(),
                mavenBundle().groupId("org.glassfish.jersey.media").artifactId("jersey-media-jaxb").versionAsInProject(),
                mavenBundle().groupId("org.glassfish.jersey.core").artifactId("jersey-server").versionAsInProject(),
                mavenBundle().groupId("org.glassfish.jersey.core").artifactId("jersey-client").versionAsInProject(),
                mavenBundle().groupId("org.glassfish.jersey.inject").artifactId("jersey-hk2").versionAsInProject(),

                // jettison
                mavenBundle().groupId("org.codehaus.jettison").artifactId("jettison").versionAsInProject(),

                // validation
                mavenBundle().groupId("javax.validation").artifactId("validation-api").versionAsInProject(),

                // Grizzly
                systemPackage("sun.misc"),       // required by grizzly-framework
                mavenBundle().groupId("org.glassfish.grizzly").artifactId("grizzly-framework").versionAsInProject(),
                mavenBundle().groupId("org.glassfish.grizzly").artifactId("grizzly-http").versionAsInProject(),
                mavenBundle().groupId("org.glassfish.grizzly").artifactId("grizzly-http-server").versionAsInProject(),

                // Jersey Grizzly
                mavenBundle().groupId("org.glassfish.jersey.containers").artifactId("jersey-container-grizzly2-http")
                        .versionAsInProject(),

                // tinybundles + required dependencies
                mavenBundle().groupId("org.ops4j.pax.tinybundles").artifactId("tinybundles").versionAsInProject(),
                mavenBundle().groupId("biz.aQute.bnd").artifactId("bndlib").versionAsInProject(),

                // create ad-hoc bundle
                provision(
                        bundle()
                                .add(MyApplication.class)
                                .add(ItemResource.class)
                                .add(ItemsResource.class)
                                .add(Examples.class)
                                .add(SampleWadlGeneratorConfig.class)
                                .add(Item.class)
                                .add(Items.class)
                                .add(ObjectFactory.class)
                                .add("application-doc.xml", ClassLoader.getSystemResourceAsStream("application-doc.xml"))
                                .add("application-grammars.xml",
                                        ClassLoader.getSystemResourceAsStream("application-grammars.xml"))
                                .add("resourcedoc.xml", ClassLoader.getSystemResourceAsStream("resourcedoc.xml"))
                                .set("Export-Package",
                                        MyApplication.class.getPackage().getName() + "," + SampleWadlGeneratorConfig.class
                                                .getPackage().getName())
                                .set("DynamicImport-Package", "*")
                                .set("Bundle-SymbolicName", "webapp").build())
        ));
        final String localRepository = AccessController.doPrivileged(PropertiesHelper.getSystemProperty("localRepository"));
        if (localRepository != null) {
            options = new ArrayList<>(options);
            options.add(systemProperty("org.ops4j.pax.url.mvn.localRepository").value(localRepository));
        }
        return options.toArray(new Option[options.size()]);
    }

    private ResourceConfig createResourceConfig() {
        final ResourceConfig resourceConfig = new ResourceConfig(new MyApplication().getClasses());
        resourceConfig.property(ServerProperties.WADL_GENERATOR_CONFIG, SampleWadlGeneratorConfig.class.getName());

        return resourceConfig;
    }

    /**
     * Test checks that the WADL generated using the WadlGenerator api doesn't
     * contain the expected text.
     *
     * @throws java.lang.Exception in case of a test error.
     */
    @Test
    public void testExtendedWadl() throws Exception {

        // TODO - temporary workaround
        // This is a workaround related to issue JERSEY-2093; grizzly (1.9.5) needs to have the correct context
        // class loader set
        ClassLoader myClassLoader = this.getClass().getClassLoader();

        for (Bundle bundle : bundleContext.getBundles()) {
            if ("webapp".equals(bundle.getSymbolicName())) {
                myClassLoader = bundle.loadClass("org.glassfish.jersey.examples.extendedwadl.resources.MyApplication")
                        .getClassLoader();
                break;
            }
        }

        Thread.currentThread().setContextClassLoader(myClassLoader);
        // END of workaround - the entire block can be deleted after grizzly is updated to recent version

        // List all the OSGi bundles
        StringBuilder sb = new StringBuilder();
        sb.append("-- Bundle list -- \n");
        for (Bundle b : bundleContext.getBundles()) {
            sb.append(String.format("%1$5s", "[" + b.getBundleId() + "]")).append(" ")
                    .append(String.format("%1$-70s", b.getSymbolicName())).append(" | ")
                    .append(String.format("%1$-20s", b.getVersion())).append(" |");
            try {
                b.start();
                sb.append(" STARTED  | ");
            } catch (BundleException e) {
                sb.append(" *FAILED* | ").append(e.getMessage());
            }
            sb.append(b.getLocation()).append("\n");
        }
        sb.append("-- \n\n");
        LOGGER.fine(sb.toString());

        final ResourceConfig resourceConfig = createResourceConfig();
        final HttpServer server = GrizzlyHttpServerFactory.createHttpServer(baseUri, resourceConfig);
        final Client client = ClientBuilder.newClient();
        final Response response = client.target(baseUri).path("application.wadl").request(MediaTypes.WADL_TYPE).buildGet()
                .invoke();

        String wadl = response.readEntity(String.class);
        LOGGER.info("RESULT = " + wadl);

        assertTrue("Generated wadl is of null length", !wadl.isEmpty());
        assertTrue("Generated wadl doesn't contain the expected text",
                wadl.contains("This is a paragraph"));

        assertFalse(wadl.contains("application.wadl/xsd0.xsd"));

        server.shutdownNow();
    }

    @Test
    public void testWadlOptionsMethod() throws Exception {
        // TODO - temporary workaround
        // This is a workaround related to issue JERSEY-2093; grizzly (1.9.5) needs to have the correct context
        // class loader set
        ClassLoader myClassLoader = this.getClass().getClassLoader();
        for (Bundle bundle : bundleContext.getBundles()) {
            if ("webapp".equals(bundle.getSymbolicName())) {
                myClassLoader = bundle.loadClass("org.glassfish.jersey.examples.extendedwadl.resources.MyApplication")
                        .getClassLoader();
                break;
            }
        }

        Thread.currentThread().setContextClassLoader(myClassLoader);
        // END of workaround; The entire block can be removed after grizzly is migrated to more recent version

        final ResourceConfig resourceConfig = createResourceConfig();
        final HttpServer server = GrizzlyHttpServerFactory.createHttpServer(baseUri, resourceConfig);
        final Client client = ClientBuilder.newClient();

        String wadl = client.target(baseUri).path("items").queryParam(WadlUtils.DETAILED_WADL_QUERY_PARAM, "true")
                .request(MediaTypes.WADL_TYPE).options(String.class);

        assertTrue("Generated wadl is of null length", !wadl.isEmpty());
        assertTrue("Generated wadl doesn't contain the expected text",
                wadl.contains("This is a paragraph"));

        checkWadl(wadl, baseUri);

        server.shutdownNow();
    }

    private void checkWadl(String wadl, URI baseUri) throws Exception {
        DocumentBuilderFactory bf = DocumentBuilderFactory.newInstance();
        bf.setNamespaceAware(true);
        bf.setValidating(false);
        DocumentBuilder b = bf.newDocumentBuilder();
        Document document = b.parse(new ByteArrayInputStream(wadl.getBytes(Charset.forName("UTF-8"))));
        XPath xp = XPathFactory.newInstance().newXPath();
        xp.setNamespaceContext(new SimpleNamespaceResolver("wadl", "http://wadl.dev.java.net/2009/02"));
        String val = (String) xp.evaluate("/wadl:application/wadl:resources/@base", document, XPathConstants.STRING);
        assertEquals(baseUri.toString(), val.endsWith("/") ? val.substring(0, val.length() - 1) : val);
        val = (String) xp.evaluate("count(//wadl:resource)", document, XPathConstants.STRING);
        assertEquals("Unexpected number of resource elements.", val, "4");
        val = (String) xp.evaluate("count(//wadl:resource[@path='items'])", document, XPathConstants.STRING);
        assertEquals("Unexpected number of resource elements with 'items' path.", "1", val);
        val = (String) xp.evaluate("count(//wadl:resource[@path='{id}'])", document, XPathConstants.STRING);
        assertEquals("Unexpected number of resource elements with '{id}' path.", "1", val);
        val = (String) xp.evaluate("count(//wadl:resource[@path='try-hard'])", document, XPathConstants.STRING);
        assertEquals("Unexpected number of resource elements with 'try-hard' path.", "1", val);
        val = (String) xp.evaluate("count(//wadl:resource[@path='value/{value}'])", document, XPathConstants.STRING);
        assertEquals("Unexpected number of resource elements with 'value/{value}' path.", "1", val);

        val = (String) xp.evaluate("count(//wadl:resource[@path='{id}']/wadl:method)", document, XPathConstants.STRING);
        assertEquals("Unexpected number of methods in resource element with '{id}' path.", "2", val);
        val = (String) xp.evaluate("count(//wadl:resource[@path='{id}']/wadl:method[@id='getItem']"
                        + "/wadl:doc[contains(., 'Typically returns the item if it exists.')])",
                document, XPathConstants.STRING);
        assertEquals("Unexpected documentation of getItem resource method at '{id}' path", "1", val);

        val = (String) xp.evaluate("count(//wadl:resource[@path='try-hard']/wadl:method)", document, XPathConstants.STRING);
        assertEquals("Unexpected number of methods in resource element with 'try-hard' path.", "1", val);
        val = (String) xp.evaluate("count(//wadl:resource[@path='try-hard']/wadl:method[@id='getItem']"
                        + "/wadl:doc[contains(., 'Tries hard to return the item if it exists.')])",
                document, XPathConstants.STRING);
        assertEquals("Unexpected documentation of getItem resource method at 'try-hard' path", "1", val);

        val = (String) xp.evaluate("count(//wadl:resource[@path='items']/wadl:method)", document, XPathConstants.STRING);
        assertEquals("Unexpected number of methods in resource element with 'items' path.", "4", val);
        val = (String) xp.evaluate("count(//wadl:resource[@path='value/{value}']/wadl:method)", document, XPathConstants.STRING);
        assertEquals("Unexpected number of methods in resource element with 'value/{value}' path.", "1", val);

        val = (String) xp.evaluate("count(//wadl:application/wadl:doc)", document, XPathConstants.STRING);
        assertEquals("Unexpected number of doc elements in application element.", "3", val);
    }
}
