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
package org.glassfish.jersey.tests.e2e.server.wadl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import javax.inject.Named;
import javax.xml.XMLConstants;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.glassfish.jersey.internal.MapPropertiesDelegate;
import org.glassfish.jersey.internal.util.SaxHelper;
import org.glassfish.jersey.internal.util.SimpleNamespaceResolver;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.message.internal.MediaTypes;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.wadl.WadlApplicationContext;
import org.glassfish.jersey.server.wadl.WadlGenerator;
import org.glassfish.jersey.server.wadl.config.WadlGeneratorConfig;
import org.glassfish.jersey.server.wadl.config.WadlGeneratorDescription;
import org.glassfish.jersey.server.wadl.internal.WadlGeneratorImpl;
import org.glassfish.jersey.server.wadl.internal.WadlUtils;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;

import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.ElementQualifier;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.examples.RecursiveElementNameAndTextQualifier;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableMap;

import com.sun.research.ws.wadl.Method;
import com.sun.research.ws.wadl.Param;
import com.sun.research.ws.wadl.Request;
import com.sun.research.ws.wadl.Resource;
import com.sun.research.ws.wadl.Resources;

/**
 * WADL use case tests.
 *
 * @author Marc Hadley
 * @author Miroslav Fuksa
 * @author Michal Gajdos
 * @author Libor Kramolis (libor.kramolis at oracle.com)
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
        WadlResourceTest.Wadl1Test.class,
        WadlResourceTest.Wadl2Test.class,
        WadlResourceTest.Wadl3Test.class,
        WadlResourceTest.Wadl4Test.class,
        WadlResourceTest.Wadl5Test.class,
        WadlResourceTest.Wadl6Test.class,
        WadlResourceTest.Wadl7Test.class,
        WadlResourceTest.Wadl8Test.class,
        WadlResourceTest.Wadl9Test.class,
        WadlResourceTest.Wadl10Test.class,
        WadlResourceTest.Wadl11Test.class,
})
public class WadlResourceTest {

    /**
     * Extracts WADL as {@link Document} from given {@link Response}.
     *
     * @param response The response to extract {@code Document} from.
     * @return The extracted {@code Document}.
     * @throws ParserConfigurationException In case of parser configuration issues.
     * @throws SAXException                 In case of parsing issues.
     * @throws IOException                  In case of IO error.
     */
    static Document extractWadlAsDocument(final Response response) throws ParserConfigurationException, SAXException,
            IOException {
        assertEquals(200, response.getStatus());
        final File tmpFile = response.readEntity(File.class);
        final DocumentBuilderFactory bf = DocumentBuilderFactory.newInstance();
        bf.setNamespaceAware(true);
        bf.setValidating(false);
        if (!SaxHelper.isXdkDocumentBuilderFactory(bf)) {
            bf.setXIncludeAware(false);
        }
        final DocumentBuilder b = bf.newDocumentBuilder();
        final Document d = b.parse(tmpFile);
        Wadl5Test.printSource(new DOMSource(d));
        return d;
    }

    private static String nodeAsString(final Object resourceNode) throws TransformerException {
        StringWriter writer = new StringWriter();
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.transform(new DOMSource((Node) resourceNode), new StreamResult(writer));
        return writer.toString();
    }

    public static class Wadl7Test extends JerseyTest {

        @Override
        protected Application configure() {
            return new ResourceConfig(TestRootResource.class);
        }

        @Path("root")
        public static class TestRootResource {

            @Path("{template}")
            @GET
            public String getSub() {
                return "get";
            }

            @GET
            public String get() {
                return "getroot";
            }
        }

        @Test
        public void testPathTemplateInSubResourceMethod() throws ParserConfigurationException, SAXException, IOException,
                XPathExpressionException {
            final Response response = target("root/foo").request(MediaTypes.WADL_TYPE).options();
            assertEquals(200, response.getStatus());
        }

        @Test
        public void testPathTemplateInSubResourceMethod2() throws ParserConfigurationException, SAXException, IOException,
                XPathExpressionException {
            final Response response = target("root").request(MediaTypes.WADL_TYPE).options();
            assertEquals(200, response.getStatus());
        }
    }

    public static class Wadl5Test extends JerseyTest {

        @Override
        protected Application configure() {
            return new ResourceConfig(WidgetsResource.class, ExtraResource.class);
        }

        @Path("foo")
        public static class ExtraResource {

            @GET
            @Produces("application/xml")
            public String getRep() {
                return null;
            }
        }

        @Path("widgets")
        public static class WidgetsResource {

            @GET
            @Produces({"application/xml", "application/json"})
            public String getWidgets() {
                return null;
            }

            @POST
            @Consumes({"application/xml"})
            @Produces({"application/xml", "application/json"})
            public String createWidget(final String bar, @MatrixParam("test") final String test) {
                return bar;
            }

            @PUT
            @Path("{id}")
            @Consumes("application/xml")
            public void updateWidget(final String bar, @PathParam("id") final int id) {
            }

            @GET
            @Path("{id}")
            @Produces({"application/xml", "application/json"})
            public String getWidget(@PathParam("id") final int id) {
                return null;
            }

            @DELETE
            @Path("{id}")
            public void deleteWidget(@PathParam("id") final int id) {
            }

            @Path("{id}/verbose")
            public ExtraResource getVerbose(@PathParam("id") final int id) {
                return new ExtraResource();
            }
        }

        @Test
        public void testDisableWadl() throws ExecutionException, InterruptedException {
            final ResourceConfig rc = new ResourceConfig(WidgetsResource.class, ExtraResource.class);
            rc.property(ServerProperties.WADL_FEATURE_DISABLE, true);

            final ApplicationHandler applicationHandler = new ApplicationHandler(rc);

            final ContainerResponse containerResponse = applicationHandler.apply(new ContainerRequest(
                    URI.create("/"), URI.create("/application.wadl"),
                    "GET", null, new MapPropertiesDelegate())).get();

            assertEquals(404, containerResponse.getStatus());
        }

        @Test
        public void testEnableWadl() throws ExecutionException, InterruptedException {
            final ResourceConfig rc = new ResourceConfig(WidgetsResource.class, ExtraResource.class);
            rc.property(ServerProperties.WADL_FEATURE_DISABLE, false);

            final ApplicationHandler applicationHandler = new ApplicationHandler(rc);

            final ContainerResponse containerResponse = applicationHandler.apply(new ContainerRequest(
                    URI.create("/"), URI.create("/application.wadl"),
                    "GET", null, new MapPropertiesDelegate())).get();

            assertEquals(200, containerResponse.getStatus());
        }

        public static void printSource(final Source source) {
            try {
                System.out.println("---------------------");
                final Transformer trans = TransformerFactory.newInstance().newTransformer();
                final Properties oprops = new Properties();
                oprops.put(OutputKeys.OMIT_XML_DECLARATION, "yes");
                oprops.put(OutputKeys.INDENT, "yes");
                oprops.put(OutputKeys.METHOD, "xml");
                trans.setOutputProperties(oprops);
                trans.transform(source, new StreamResult(System.out));
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }

        public static String getXmlSchemaNamespacePrefix(final Node elementNode) {
            final NamedNodeMap attributes = elementNode.getAttributes();

            for (int i = 0; i < attributes.getLength(); i++) {
                final Node item = attributes.item(i);
                if (XMLConstants.W3C_XML_SCHEMA_NS_URI.equals(item.getNodeValue())) {
                    return item.getLocalName();
                }
            }

            return "xs";
        }

        /**
         * Test WADL generation.
         *
         * @throws Exception in case of unexpected test failure.
         */
        @Test
        public void testGetWadl() throws Exception {
            final File tmpFile = target("application.wadl").queryParam(WadlUtils.DETAILED_WADL_QUERY_PARAM, "true")
                    .request().get(File.class);

            final DocumentBuilderFactory bf = DocumentBuilderFactory.newInstance();
            bf.setNamespaceAware(true);
            bf.setValidating(false);
            if (!SaxHelper.isXdkDocumentBuilderFactory(bf)) {
                bf.setXIncludeAware(false);
            }
            final DocumentBuilder b = bf.newDocumentBuilder();
            final Document d = b.parse(tmpFile);
            printSource(new DOMSource(d));
            final XPath xp = XPathFactory.newInstance().newXPath();
            xp.setNamespaceContext(new SimpleNamespaceResolver("wadl", "http://wadl.dev.java.net/2009/02"));
            // check base URI
            String val = (String) xp.evaluate("/wadl:application/wadl:resources/@base", d, XPathConstants.STRING);
            assertEquals(val, getBaseUri().toString());
            // check total number of resources is 4
            val = (String) xp.evaluate("count(//wadl:resource)", d, XPathConstants.STRING);
            assertEquals("6", val);
            // check only once resource with for {id}
            val = (String) xp.evaluate("count(//wadl:resource[@path='{id}'])", d, XPathConstants.STRING);
            assertEquals("1", val);
            // check only once resource with for {id}/verbose
            val = (String) xp.evaluate("count(//wadl:resource[@path='{id}/verbose'])", d, XPathConstants.STRING);
            assertEquals("1", val);
            // check only once resource with for widgets
            val = (String) xp.evaluate("count(//wadl:resource[@path='widgets'])", d, XPathConstants.STRING);
            assertEquals("1", val);
            // check 3 methods for {id}
            val = (String) xp.evaluate("count(//wadl:resource[@path='{id}']/wadl:method)", d, XPathConstants.STRING);
            assertEquals("6", val);
            // check 2 methods for widgets
            val = (String) xp.evaluate("count(//wadl:resource[@path='widgets']/wadl:method)", d, XPathConstants.STRING);
            assertEquals("5", val);
            // check 1 matrix param on resource
            val = (String) xp.evaluate("count(//wadl:resource[@path='widgets']/wadl:param[@name='test'])", d,
                    XPathConstants.STRING);
            assertEquals("1", val);
            // check type of {id} is int
            String prefix = getXmlSchemaNamespacePrefix(
                    (Node) xp.evaluate("//wadl:resource[@path='{id}']/wadl:param[@name='id']", d, XPathConstants.NODE));
            val = (String) xp.evaluate("//wadl:resource[@path='{id}']/wadl:param[@name='id']/@type", d, XPathConstants.STRING);
            assertEquals(val, (prefix == null ? "" : prefix + ":") + "int");
            // check number of output representations is two
            val = (String) xp.evaluate("count(//wadl:resource[@path='widgets']/wadl:method[@name='GET']/wadl:response/wadl"
                    + ":representation)", d, XPathConstants.STRING);
            assertEquals("2", val);
            // check number of output representations is one
            val = (String) xp.evaluate("count(//wadl:resource[@path='widgets']/wadl:method[@name='POST']/wadl:request/wadl"
                    + ":representation)", d, XPathConstants.STRING);
            assertEquals("1", val);
            // check type of {id}/verbose is int
            prefix = getXmlSchemaNamespacePrefix(
                    (Node) xp.evaluate("//wadl:resource[@path='{id}/verbose']/wadl:param[@name='id']", d, XPathConstants.NODE));
            val = (String) xp.evaluate("//wadl:resource[@path='{id}/verbose']/wadl:param[@name='id']/@type", d,
                    XPathConstants.STRING);
            assertEquals(val, (prefix == null ? "" : prefix + ":") + "int");
        }

        @Test
        public void testLastModifiedGET() {
            final WebTarget target = target("/application.wadl");

            final Response r = target.queryParam(WadlUtils.DETAILED_WADL_QUERY_PARAM, "true").request().get(Response.class);
            assertTrue(r.getHeaders().containsKey("Last-modified"));
        }

        @Test
        public void testLastModifiedOPTIONS() {
            final WebTarget target = target("/widgets/3/verbose");

            final Response r = target.queryParam(WadlUtils.DETAILED_WADL_QUERY_PARAM, "true").request(MediaTypes.WADL_TYPE)
                    .options();
            System.out.println(r.readEntity(String.class));
            assertTrue(r.getHeaders().containsKey("Last-modified"));
        }

        @Test
        public void testOptionsResourceWadl() throws ParserConfigurationException, XPathExpressionException, IOException,
                SAXException {
            // test WidgetsResource
            Response response = target("/widgets").queryParam(WadlUtils.DETAILED_WADL_QUERY_PARAM, "true")
                    .request(MediaTypes.WADL_TYPE).options();
            assertEquals(200, response.getStatus());
            File tmpFile = response.readEntity(File.class);
            final DocumentBuilderFactory bf = DocumentBuilderFactory.newInstance();
            bf.setNamespaceAware(true);
            bf.setValidating(false);
            if (!SaxHelper.isXdkDocumentBuilderFactory(bf)) {
                bf.setXIncludeAware(false);
            }
            DocumentBuilder b = bf.newDocumentBuilder();
            Document d = b.parse(tmpFile);
            printSource(new DOMSource(d));
            final XPath xp = XPathFactory.newInstance().newXPath();
            xp.setNamespaceContext(new SimpleNamespaceResolver("wadl", "http://wadl.dev.java.net/2009/02"));

            // check base URI
            String val = (String) xp.evaluate("/wadl:application/wadl:resources/@base", d, XPathConstants.STRING);
            assertEquals(val, getBaseUri().toString());
            // check total number of resources is 3 (no ExtraResource details included)
            val = (String) xp.evaluate("count(//wadl:resource)", d, XPathConstants.STRING);
            assertEquals("3", val);
            // check only once resource with for {id}
            val = (String) xp.evaluate("count(//wadl:resource[@path='{id}'])", d, XPathConstants.STRING);
            assertEquals("1", val);
            // check only once resource with for {id}/verbose
            val = (String) xp.evaluate("count(//wadl:resource[@path='{id}/verbose'])", d, XPathConstants.STRING);
            assertEquals("1", val);
            // check only once resource with for widgets
            val = (String) xp.evaluate("count(//wadl:resource[@path='widgets'])", d, XPathConstants.STRING);
            assertEquals("1", val);
            // check 3 methods for {id}
            val = (String) xp.evaluate("count(//wadl:resource[@path='{id}']/wadl:method)", d, XPathConstants.STRING);
            assertEquals("6", val);
            // check 2 methods for widgets
            val = (String) xp.evaluate("count(//wadl:resource[@path='widgets']/wadl:method)", d, XPathConstants.STRING);
            assertEquals("5", val);
            // check type of {id} is int
            final String prefix = getXmlSchemaNamespacePrefix(
                    (Node) xp.evaluate("//wadl:resource[@path='{id}']/wadl:param[@name='id']", d, XPathConstants.NODE));
            val = (String) xp.evaluate("//wadl:resource[@path='{id}']/wadl:param[@name='id']/@type", d, XPathConstants.STRING);
            assertEquals(val, (prefix == null ? "" : prefix + ":") + "int");
            // check number of output representations is two
            val = (String) xp.evaluate("count(//wadl:resource[@path='widgets']/wadl:method[@name='GET']/wadl:response/wadl"
                    + ":representation)", d, XPathConstants.STRING);
            assertEquals("2", val);
            // check number of output representations is one
            val = (String) xp.evaluate("count(//wadl:resource[@path='widgets']/wadl:method[@name='POST']/wadl:request/wadl"
                    + ":representation)", d, XPathConstants.STRING);
            assertEquals("1", val);

            response = target("/foo").queryParam(WadlUtils.DETAILED_WADL_QUERY_PARAM, "true")
                    .request(MediaTypes.WADL_TYPE).options();
            assertEquals(200, response.getStatus());
            tmpFile = response.readEntity(File.class);
            b = bf.newDocumentBuilder();
            d = b.parse(tmpFile);
            printSource(new DOMSource(d));
            // check base URI
            val = (String) xp.evaluate("/wadl:application/wadl:resources/@base", d, XPathConstants.STRING);
            assertEquals(val, getBaseUri().toString());
            // check total number of resources is 1 (no ExtraResource details included)
            val = (String) xp.evaluate("count(//wadl:resource)", d, XPathConstants.STRING);
            assertEquals("1", val);
            // check only once resource with path foo
            val = (String) xp.evaluate("count(//wadl:resource[@path='foo'])", d, XPathConstants.STRING);
            assertEquals("1", val);
            // check 1 methods for foo
            val = (String) xp.evaluate("count(//wadl:resource[@path='foo']/wadl:method)", d, XPathConstants.STRING);
            assertEquals("4", val);
        }

        @Test
        public void testOptionsLocatorWadl() throws ParserConfigurationException, SAXException, IOException,
                XPathExpressionException {

            // test WidgetsResource
            final File tmpFile = target("/widgets/3/verbose").queryParam(WadlUtils.DETAILED_WADL_QUERY_PARAM, "true")
                    .request(MediaTypes.WADL_TYPE).options(File.class);
            final DocumentBuilderFactory bf = DocumentBuilderFactory.newInstance();
            bf.setNamespaceAware(true);
            bf.setValidating(false);
            if (!SaxHelper.isXdkDocumentBuilderFactory(bf)) {
                bf.setXIncludeAware(false);
            }
            final DocumentBuilder b = bf.newDocumentBuilder();
            final Document d = b.parse(tmpFile);
            printSource(new DOMSource(d));
            final XPath xp = XPathFactory.newInstance().newXPath();
            xp.setNamespaceContext(new SimpleNamespaceResolver("wadl", "http://wadl.dev.java.net/2009/02"));
            // check base URI
            String val = (String) xp.evaluate("/wadl:application/wadl:resources/@base", d, XPathConstants.STRING);
            assertEquals(val, getBaseUri().toString());
            // check total number of resources is 1 (no ExtraResource details included)
            val = (String) xp.evaluate("count(//wadl:resource)", d, XPathConstants.STRING);
            assertEquals(val, "1");
            // check only once resource with path
            val = (String) xp.evaluate("count(//wadl:resource[@path='widgets/3/verbose'])", d, XPathConstants.STRING);
            assertEquals(val, "1");
            // check 1 methods
            val = (String) xp.evaluate("count(//wadl:resource[@path='widgets/3/verbose']/wadl:method)", d, XPathConstants.STRING);
            assertEquals("4", val);
        }

        @Test
        public void testOptionsSubResourceWadl() throws ParserConfigurationException, SAXException, IOException,
                XPathExpressionException {
            // test WidgetsResource
            final File tmpFile = target("/widgets/3").queryParam(WadlUtils.DETAILED_WADL_QUERY_PARAM, "true")
                    .request(MediaTypes.WADL_TYPE).options(File.class);
            final DocumentBuilderFactory bf = DocumentBuilderFactory.newInstance();
            bf.setNamespaceAware(true);
            bf.setValidating(false);
            if (!SaxHelper.isXdkDocumentBuilderFactory(bf)) {
                bf.setXIncludeAware(false);
            }
            final DocumentBuilder b = bf.newDocumentBuilder();
            final Document d = b.parse(tmpFile);
            printSource(new DOMSource(d));
            final XPath xp = XPathFactory.newInstance().newXPath();
            xp.setNamespaceContext(new SimpleNamespaceResolver("wadl", "http://wadl.dev.java.net/2009/02"));
            String val = (String) xp.evaluate("/wadl:application/wadl:resources/@base", d, XPathConstants.STRING);
            assertEquals(val, getBaseUri().toString());
            // check total number of resources is 1
            val = (String) xp.evaluate("count(//wadl:resource)", d, XPathConstants.STRING);
            assertEquals("1", val);
            // check only one resource with for {id}
            val = (String) xp.evaluate("count(//wadl:resource[@path='widgets/3'])", d, XPathConstants.STRING);
            assertEquals("1", val);
            // check 3 methods
            val = (String) xp.evaluate("count(//wadl:resource[@path='widgets/3']/wadl:method)", d, XPathConstants.STRING);
            assertEquals("6", val);
        }

        public static class MyWadlGeneratorConfig extends WadlGeneratorConfig {

            @Override
            public List<WadlGeneratorDescription> configure() {
                return generator(MyWadlGenerator.class).descriptions();
            }

            public static class MyWadlGenerator extends WadlGeneratorImpl {

                public static final String CUSTOM_RESOURCES_BASE_URI = "http://myBaseUri";

                @Override
                public Resources createResources() {
                    final Resources resources = super.createResources();
                    resources.setBase(CUSTOM_RESOURCES_BASE_URI);

                    return resources;
                }

                @Override
                public void setWadlGeneratorDelegate(final WadlGenerator delegate) {
                    // nothing
                }
            }
        }

        /**
         * Test overriding WADL's /application/resources/@base attribute.
         *
         * @throws Exception in case of unexpected test failure.
         */
        @Test
        public void testCustomWadlResourcesBaseUri() throws Exception {
            final ResourceConfig rc = new ResourceConfig(WidgetsResource.class, ExtraResource.class);
            rc.property(ServerProperties.WADL_GENERATOR_CONFIG, MyWadlGeneratorConfig.class.getName());

            final ApplicationHandler applicationHandler = new ApplicationHandler(rc);

            final ContainerResponse containerResponse = applicationHandler.apply(new ContainerRequest(
                    URI.create("/"), URI.create("/application.wadl"),
                    "GET", null, new MapPropertiesDelegate())).get();

            final DocumentBuilderFactory bf = DocumentBuilderFactory.newInstance();
            bf.setNamespaceAware(true);
            bf.setValidating(false);
            if (!SaxHelper.isXdkDocumentBuilderFactory(bf)) {
                bf.setXIncludeAware(false);
            }
            final DocumentBuilder b = bf.newDocumentBuilder();

            ((ByteArrayInputStream) containerResponse.getEntity()).reset();

            final Document d = b.parse((InputStream) containerResponse.getEntity());
            printSource(new DOMSource(d));
            final XPath xp = XPathFactory.newInstance().newXPath();
            xp.setNamespaceContext(new SimpleNamespaceResolver("wadl", "http://wadl.dev.java.net/2009/02"));
            // check base URI
            final String val = (String) xp.evaluate("/wadl:application/wadl:resources/@base", d, XPathConstants.STRING);
            assertEquals(val, MyWadlGeneratorConfig.MyWadlGenerator.CUSTOM_RESOURCES_BASE_URI);
        }

        @Path("emptyproduces")
        public static class EmptyProducesTestResource {

            @PUT
            @Produces({})
            public Response put(final String str) {
                return Response.ok().build();
            }

            @POST
            @Path("/sub")
            @Produces({})
            public Response post(final String str) {
                return Response.ok().build();
            }
        }

        @Test
        public void testEmptyProduces() throws Exception {
            final ResourceConfig rc = new ResourceConfig(EmptyProducesTestResource.class);
            rc.property(ServerProperties.WADL_FEATURE_DISABLE, false);

            final ApplicationHandler applicationHandler = new ApplicationHandler(rc);

            final ContainerResponse containerResponse = applicationHandler.apply(new ContainerRequest(
                    URI.create("/"), URI.create("/application.wadl"),
                    "GET", null, new MapPropertiesDelegate())).get();

            assertEquals(200, containerResponse.getStatus());

            ((ByteArrayInputStream) containerResponse.getEntity()).reset();

            final DocumentBuilderFactory bf = DocumentBuilderFactory.newInstance();
            bf.setNamespaceAware(true);
            bf.setValidating(false);
            if (!SaxHelper.isXdkDocumentBuilderFactory(bf)) {
                bf.setXIncludeAware(false);
            }
            final DocumentBuilder b = bf.newDocumentBuilder();
            final Document d = b.parse((InputStream) containerResponse.getEntity());
            printSource(new DOMSource(d));
            final XPath xp = XPathFactory.newInstance().newXPath();
            xp.setNamespaceContext(new SimpleNamespaceResolver("wadl", "http://wadl.dev.java.net/2009/02"));

            final NodeList responseElements = (NodeList) xp.evaluate(
                    "/wadl:application/wadl:resources[@path!='application.wadl']//wadl:method/wadl:response", d,
                    XPathConstants.NODESET);

            for (int i = 0; i < responseElements.getLength(); i++) {
                final Node item = responseElements.item(i);

                assertEquals(0, item.getChildNodes().getLength());
                assertEquals(0, item.getAttributes().getLength());
            }
        }
    }

    public static class Wadl1Test extends JerseyTest {

        @Override
        protected Application configure() {
            return new ResourceConfig(RootResource.class);
        }

        @Path("root")
        public static class RootResource {

            @Path("loc")
            public Object getSub() {
                return new SubResource();
            }

            @Path("switch")
            @POST
            public void switchMethod(@Context final WadlApplicationContext wadlApplicationContext) {
                wadlApplicationContext.setWadlGenerationEnabled(!wadlApplicationContext.isWadlGenerationEnabled());

            }
        }

        public static class SubResource {

            @Path("loc")
            public Object getSub() {
                return new SubResource();
            }

            @GET
            @Produces("text/plain")
            public String hello() {
                return "Hello World !";
            }

            @GET
            @Path("sub")
            @Produces("text/plain")
            public String helloSub() {
                return "Hello World !";
            }
        }

        @Test
        public void testRecursive() throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
            _testRecursiveWadl("root/loc");
        }

        @Test
        public void mytest() throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
            final Response response = target().path("root/test").request().get();
            System.out.println(response.readEntity(String.class));
        }

        @Test
        public void testRecursive2() throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
            _testRecursiveWadl("root/loc/loc");
        }

        private void _testRecursiveWadl(final String path) throws ParserConfigurationException, SAXException, IOException,
                XPathExpressionException {
            final Document d = extractWadlAsDocument(target(path).request(MediaTypes.WADL_TYPE).options());

            final XPath xp = XPathFactory.newInstance().newXPath();
            xp.setNamespaceContext(new SimpleNamespaceResolver("wadl", "http://wadl.dev.java.net/2009/02"));
            String val = (String) xp.evaluate("/wadl:application/wadl:resources/@base", d, XPathConstants.STRING);
            assertEquals(val, getBaseUri().toString());

            // check only one resource with for 'root/loc'
            val = (String) xp.evaluate("count(//wadl:resource[@path='" + path + "'])", d, XPathConstants.STRING);
            assertEquals(val, "1");
        }

    }

    public static class Wadl2Test extends JerseyTest {

        @Override
        protected Application configure() {
            return new ResourceConfig(RootResource1.class, RootResource2.class);
        }

        @Path("root1")
        public static class RootResource1 {

            @Path("loc")
            public Wadl1Test.SubResource getSub() {
                return new Wadl1Test.SubResource();
            }
        }

        @Path("root2")
        public static class RootResource2 {

            @Path("loc")
            public Wadl1Test.SubResource getSub() {
                return new Wadl1Test.SubResource();
            }
        }

        @Test
        public void testRecursive2() throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
            final Document d = extractWadlAsDocument(target("/application.wadl")
                    .queryParam(WadlUtils.DETAILED_WADL_QUERY_PARAM, "true").request().get());

            final XPath xp = XPathFactory.newInstance().newXPath();
            xp.setNamespaceContext(new SimpleNamespaceResolver("wadl", "http://wadl.dev.java.net/2009/02"));
            String val = (String) xp.evaluate("/wadl:application/wadl:resources/@base", d, XPathConstants.STRING);
            assertEquals(val, getBaseUri().toString());
            // check only one resource with for 'root/loc'
            val = (String) xp.evaluate("count(//wadl:resource[@path='loc'])", d, XPathConstants.STRING);
            assertEquals("4", val);
            // check for method with id of hello
            val = (String) xp.evaluate("count(//wadl:resource[@path='loc']/wadl:method[@id='hello'])", d, XPathConstants.STRING);
            assertEquals("2", val);
        }
    }

    public static class Wadl3Test extends JerseyTest {

        @Override
        protected Application configure() {
            return new ResourceConfig(FormResource.class);
        }

        @Path("form")
        public static class FormResource {

            @POST
            @Consumes("application/x-www-form-urlencoded")
            public void post(
                    @FormParam("a") final String a,
                    @FormParam("b") final String b,
                    @FormParam("c") final JaxbBean c,
                    @FormParam("c") final FormDataContentDisposition cdc,
                    final Form form) {
            }

        }

        @Test
        public void testFormParam() throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
            final Document d = extractWadlAsDocument(target("/application.wadl").request().get());
            final XPath xp = XPathFactory.newInstance().newXPath();
            xp.setNamespaceContext(new SimpleNamespaceResolver("wadl", "http://wadl.dev.java.net/2009/02"));

            final String requestPath = "//wadl:resource[@path='form']/wadl:method[@name='POST']/wadl:request";
            final String representationPath = requestPath + "/wadl:representation";

            // check number of request params is zero
            int count = ((Double) xp.evaluate("count(" + requestPath + "/wadl:param)", d, XPathConstants.NUMBER)).intValue();
            assertEquals(0, count);

            // check number of request representations is one
            count = ((Double) xp.evaluate("count(" + representationPath + ")", d, XPathConstants.NUMBER)).intValue();
            assertEquals(1, count);

            // check number of request representation params is three
            count = ((Double) xp.evaluate("count(" + representationPath + "/wadl:param)", d, XPathConstants.NUMBER)).intValue();
            assertEquals(3, count);

            // check the style of the request representation param is 'query'
            String val = (String) xp.evaluate(representationPath + "/wadl:param[@name='a']/@style", d, XPathConstants.STRING);
            assertEquals("query", val);
            val = (String) xp.evaluate(representationPath + "/wadl:param[@name='b']/@style", d, XPathConstants.STRING);
            assertEquals("query", val);

        }
    }

    public static class Wadl4Test extends JerseyTest {

        @Override
        protected Application configure() {
            return new ResourceConfig(FieldParamResource.class);
        }

        @Path("fieldParam/{pp}")
        public static class FieldParamResource {

            @HeaderParam("hp")
            String hp;
            @MatrixParam("mp")
            String mp;
            @PathParam("pp")
            String pp;

            private String q;

            @QueryParam("q")
            public void setQ(final String q) {
                this.q = q;
            }

            // these should not be included in WADL
            @Context
            UriInfo uriInfo;
            @Named("fakeParam")
            String fakeParam;

            @GET
            @Produces("text/plain")
            public String get() {
                return pp;
            }

        }

        @Test
        public void testFieldParam() throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
            final String path = "fieldParam";
            _testFieldAndSetterParam(target("/application.wadl").request().get(), path);
        }

        private static void _testFieldAndSetterParam(final Response response, final String path)
                throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {

            final Document d = extractWadlAsDocument(response);
            final XPath xp = XPathFactory.newInstance().newXPath();
            xp.setNamespaceContext(new SimpleNamespaceResolver("wadl", "http://wadl.dev.java.net/2009/02"));

            final String resourcePath = String.format("//wadl:resource[@path='%s/{pp}']", path);
            final String methodPath = resourcePath + "/wadl:method[@name='GET']";

            // check number of resource methods is one
            final int methodCount = ((Double) xp.evaluate("count(" + methodPath + ")", d, XPathConstants.NUMBER)).intValue();
            assertThat("Unexpected number of methods on path '" + methodPath + "'", methodCount, equalTo(1));

            final Map<String, String> paramStyles = new HashMap<>();
            paramStyles.put("mp", "matrix");
            paramStyles.put("pp", "template");

            // check the number of resource params
            final String resourceParamsCountXPath = String.format("count(%s/wadl:param)", resourcePath);
            final int resourceParamsCount = ((Double) xp.evaluate(resourceParamsCountXPath, d, XPathConstants.NUMBER)).intValue();
            assertThat("Number of resource parameters does not match.", resourceParamsCount, equalTo(2));

            for (final Map.Entry<String, String> param : paramStyles.entrySet()) {

                final String pName = param.getKey();
                final String pStyle = param.getValue();

                final String paramXPath = String.format("%s/wadl:param[@name='%s']", resourcePath, pName);

                // check number of params is one
                final int pc = ((Double) xp.evaluate("count(" + paramXPath + ")", d, XPathConstants.NUMBER)).intValue();
                assertThat("Number of " + pStyle + " parameters '" + pName + "' does not match.", pc, equalTo(1));

                // check the style of the param
                final String style = (String) xp.evaluate(paramXPath + "/@style", d, XPathConstants.STRING);
                assertThat("Parameter '" + pName + "' style does not match.", pStyle, equalTo(style));
            }

            paramStyles.clear();
            paramStyles.put("hp", "header");
            paramStyles.put("q", "query");

            // check the number of request params
            final String requestParamsCountXPath = String.format("count(%s/wadl:request/wadl:param)", methodPath);
            final int requestParamsCount = ((Double) xp.evaluate(requestParamsCountXPath, d, XPathConstants.NUMBER)).intValue();
            assertThat("Number of request parameters does not match.", requestParamsCount, equalTo(2));

            for (final Map.Entry<String, String> param : paramStyles.entrySet()) {

                final String pName = param.getKey();
                final String pStyle = param.getValue();

                final String paramXPath = String.format("%s/wadl:request/wadl:param[@name='%s']", methodPath, pName);

                // check that the number of params is one
                final int pc = ((Double) xp.evaluate("count(" + paramXPath + ")", d, XPathConstants.NUMBER)).intValue();
                assertThat("Number of " + pStyle + " parameters '" + pName + "' does not match.", pc, equalTo(1));

                // check the style of the param
                final String style = (String) xp.evaluate(paramXPath + "/@style", d, XPathConstants.STRING);
                assertThat("Parameter '" + pName + "' style does not match.", pStyle, equalTo(style));
            }
        }
    }

    /**
     * Tests OPTIONS method on a resource method annotated with @Path and containing a leading '/'.
     */
    public static class Wadl6Test extends JerseyTest {

        @Path("wadl6test")
        public static class Resource {

            @GET
            @Path("foo1")
            public String foo1() {
                return "foo1";
            }

            @GET
            @Path("/foo2")
            public String foo2() {
                return "foo2";
            }

            @GET
            @Path("foo3/")
            public String foo3() {
                return "foo3";
            }

            @GET
            @Path("/foo4/")
            public String foo4() {
                return "foo4";
            }
        }

        @Override
        protected Application configure() {
            enable(TestProperties.LOG_TRAFFIC);
            enable(TestProperties.DUMP_ENTITY);

            return new ResourceConfig(Resource.class);
        }

        @Test
        public void testGetWithPathAndLeadingSlash() throws Exception {
            for (int i = 1; i < 5; i++) {
                final String[] paths = {
                        "foo" + i,
                        "/foo" + i,
                        "foo" + i + '/',
                        "/foo" + i + '/'
                };

                for (final String path : paths) {
                    final Response response = target("wadl6test")
                            .path(path)
                            .request("application/vnd.sun.wadl+xml")
                            .options();
                    assertEquals(200, response.getStatus());
                    final String document = response.readEntity(String.class);

                    // check that the resulting document contains a method element with id="fooX"
                    assertTrue(document.replaceAll("\n", " ").matches(".*<method[^>]+id=\"foo" + i + "\"[^>]*>.*"));
                }
            }
        }
    }

    public static class Wadl8Test extends JerseyTest {

        @Override
        protected Application configure() {
            return new ResourceConfig(ResourceA.class, ResourceB.class, ResourceSpecific.class);
        }

        @Path("{a}")
        public static class ResourceA {

            @GET
            public String getA() {
                return "a";
            }
        }

        @Path("{b}")
        public static class ResourceB {

            @POST
            public String postB(final String str) {
                return "b";
            }
        }

        @Path("resource")
        public static class ResourceSpecific {

            @GET
            @Path("{templateA}")
            public String getTemplateA() {
                return "template-a";
            }

            @POST
            @Path("{templateB}")
            public String postTemplateB(final String str) {
                return "template-b";
            }
        }

        @Test
        @Ignore("JERSEY-1670: WADL Options invoked on resources with same template returns only methods from one of them.")
        // TODO: fix
        public void testWadlForAmbiguousResourceTemplates()
                throws IOException, SAXException, ParserConfigurationException, XPathExpressionException {
            final Response response = target().path("foo").request(MediaTypes.WADL_TYPE).options();
            final Document d = extractWadlAsDocument(response);
            final XPath xp = XPathFactory.newInstance().newXPath();
            xp.setNamespaceContext(new SimpleNamespaceResolver("wadl", "http://wadl.dev.java.net/2009/02"));

            String result = (String) xp.evaluate("//wadl:resource/wadl:method[@name='GET']/@id", d, XPathConstants.STRING);
            assertEquals("getA", result);

            result = (String) xp.evaluate("//wadl:resource/wadl:method[@name='POST']/@id", d, XPathConstants.STRING);
            assertEquals("postB", result);
        }

        @Test
        @Ignore("JERSEY-1670: WADL Options invoked on resources with same template returns only methods from one of them.")
        // TODO: fix
        public void testWadlForAmbiguousChildResourceTemplates()
                throws IOException, SAXException, ParserConfigurationException, XPathExpressionException {
            final Response response = target().path("resource/bar").request(MediaTypes.WADL_TYPE).options();

            final Document d = extractWadlAsDocument(response);
            final XPath xp = XPathFactory.newInstance().newXPath();
            xp.setNamespaceContext(new SimpleNamespaceResolver("wadl", "http://wadl.dev.java.net/2009/02"));

            String result = (String) xp.evaluate("//wadl:resource/wadl:method[@name='GET']/@id", d, XPathConstants.STRING);
            assertEquals("getTemplateA", result);

            result = (String) xp.evaluate("//wadl:resource/wadl:method[@name='POST']/@id", d, XPathConstants.STRING);
            assertEquals("postTemplateB", result);
        }
    }

    /**
     * Tests usage of property {@link ServerProperties#METAINF_SERVICES_LOOKUP_DISABLE}. Wadl is registered automatically every
     * time and can be turned off only via {@link ServerProperties#WADL_FEATURE_DISABLE}.
     */
    public static class Wadl9Test extends JerseyTest {

        @Path("wadl9test")
        public static class Resource {

            @GET
            public String foo() {
                return "foo";
            }

        } // class Resource

        @Override
        protected Application configure() {
            final ResourceConfig resourceConfig = new ResourceConfig(Resource.class);
            resourceConfig.property(ServerProperties.METAINF_SERVICES_LOOKUP_DISABLE, true);

            return resourceConfig;
        }

        @Test
        public void testWadl() throws Exception {
            final Response response = target("/application.wadl").request().get();

            assertThat(response.getStatus(), is(200));
            assertThat(response.hasEntity(), is(true));
        }

    } // class Wadl9Test

    /**
     * Tests whether boolean getters have been generated with "is" prefix.
     */
    public static class Wadl10Test extends JerseyTest {

        @Path("wadl10test")
        public static class Resource {

            @GET
            public Boolean foo(@QueryParam("q") final Boolean q) {
                return q;
            }

        } // class Resource

        @Override
        protected Application configure() {
            return new ResourceConfig(Resource.class);
        }

        @Test
        public void testWadl() throws Exception {
            final Response response = target("/application.wadl").request().get();

            assertThat(response.getStatus(), is(200));
            assertThat(response.hasEntity(), is(true));

            final Method method = (Method) response.readEntity(com.sun.research.ws.wadl.Application.class) // wadl
                    .getResources().get(0).getResource().get(0) // resource
                    .getMethodOrResource().get(0); // method
            final Param param = method.getRequest().getParam().get(0); // param

            // not interested in returned value, only whether we can compile.
            assertThat(param.isRequired(), notNullValue());
            assertThat(param.isRepeating(), notNullValue());
        }

    } // class Wadl10Test

    /**
     * Tests whether unknown annotation affects a WADL correctness.
     */
    public static class Wadl11Test extends JerseyTest {

        @Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.TYPE})
        @Retention(RetentionPolicy.RUNTIME)
        @Inherited
        public @interface UnknownAnnotation {
        }

        @Path("annotated")
        @Consumes("application/json")
        @UnknownAnnotation
        public static class Annotated {

            @Path("subresource")
            @POST
            @Produces("application/json")
            @UnknownAnnotation
            public Entity myMethod1(@UnknownAnnotation final Entity entity) {
                return entity;
            }

            @Path("subresource")
            @POST
            @Produces("application/xml")
            public Entity myMethod2(@UnknownAnnotation final Entity entity) {
                return entity;
            }

        }

        @Path("not-annotated")
        @Consumes("application/json")
        public static class Plain {

            @Path("subresource")
            @POST
            @Produces("application/json")
            public Entity myMethod1(final Entity entity) {
                return entity;
            }

            @Path("subresource")
            @POST
            @Produces("application/xml")
            public Entity myMethod2(final Entity entity) {
                return entity;
            }

        }

        @Override
        protected Application configure() {
            return new ResourceConfig(Annotated.class, Plain.class);
        }

        @Test
        public void testWadlIsComplete() throws Exception {
            final Response response = target("/application.wadl").request().get();

            assertThat(response.getStatus(), is(200));
            assertThat(response.hasEntity(), is(true));

            final com.sun.research.ws.wadl.Application application =
                    response.readEntity(com.sun.research.ws.wadl.Application.class);

            // "annotated/subresource"
            final Resource resource =
                    (Resource) application.getResources().get(0).getResource().get(0).getMethodOrResource().get(0);

            assertThatMethodContainsRR(resource, "myMethod1", 0);
            assertThatMethodContainsRR(resource, "myMethod2", 1);

        }

        private static void assertThatMethodContainsRR(final Resource resource, final String methodName, final int methodIndex) {
            final Method method = (Method) resource.getMethodOrResource().get(methodIndex);

            assertThat(method.getId(), equalTo(methodName));

            final Request request = method.getRequest();
            final List<com.sun.research.ws.wadl.Response> response = method.getResponse();

            assertThat(request, notNullValue());
            assertThat(response.isEmpty(), is(false));
        }

        @Test
        public void testWadlIsSameForAnnotatedAndNot() throws Exception {

            final Response response = target("/application.wadl").request().get();

            final Document document = extractWadlAsDocument(response);

            final XPath xp = XPathFactory.newInstance().newXPath();
            final SimpleNamespaceResolver nsContext = new SimpleNamespaceResolver("wadl", "http://wadl.dev.java.net/2009/02");
            xp.setNamespaceContext(nsContext);

            final Diff diff = XMLUnit.compareXML(
                    nodeAsString(
                            xp.evaluate("//wadl:resource[@path='annotated']/wadl:resource", document,
                                    XPathConstants.NODE)),
                    nodeAsString(
                            xp.evaluate("//wadl:resource[@path='not-annotated']/wadl:resource", document,
                                    XPathConstants.NODE))
            );
            XMLUnit.setXpathNamespaceContext(
                    new SimpleNamespaceContext(ImmutableMap.of("wadl", "http://wadl.dev.java.net/2009/02")));
            final ElementQualifier elementQualifier = new RecursiveElementNameAndTextQualifier();
            diff.overrideElementQualifier(elementQualifier);
            XMLAssert.assertXMLEqual(diff, true);

        }

    } // class Wadl11Test


    @XmlRootElement(name = "jaxbBean")
    public class JaxbBean {

        public String value;

        public JaxbBean() {
        }

        public JaxbBean(String str) {
            value = str;
        }

        public boolean equals(Object o) {
            if (!(o instanceof JaxbBean)) {
                return false;
            }
            return ((JaxbBean) o).value.equals(value);
        }

        public String toString() {
            return "JAXBClass: " + value;
        }
    }
}
