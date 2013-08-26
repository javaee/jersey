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

package org.glassfish.jersey.tests.e2e.server.wadl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
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
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.glassfish.jersey.tests.e2e.entity.JaxbBean;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.sun.research.ws.wadl.Resources;

import junit.framework.Assert;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * @author mh124079
 * @author Michal Gajdos (michal.gajdos at oracle.com)
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 * @author Libor Kramolis (libor.kramolis at oracle.com)
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({WadlResourceTest.Wadl1Test.class, WadlResourceTest.Wadl2Test.class, WadlResourceTest.Wadl3Test.class,
        WadlResourceTest.Wadl5Test.class, WadlResourceTest.Wadl7Test.class, WadlResourceTest.Wadl8Test.class,
        WadlResourceTest.Wadl9Test.class})
public class WadlResourceTest {
    private static Document extractWadlAsDocument(Response response) throws ParserConfigurationException, SAXException,
            IOException {
//        final Response response = webTarget.request().get();
        Assert.assertEquals(200, response.getStatus());
        File tmpFile = response.readEntity(File.class);
        DocumentBuilderFactory bf = DocumentBuilderFactory.newInstance();
        bf.setNamespaceAware(true);
        bf.setValidating(false);
        if (!SaxHelper.isXdkDocumentBuilderFactory(bf)) {
            bf.setXIncludeAware(false);
        }
        DocumentBuilder b = bf.newDocumentBuilder();
        Document d = b.parse(tmpFile);
        Wadl5Test.printSource(new DOMSource(d));
        return d;
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
            final Response response = target("root/foo").request(MediaTypes.WADL).options();
            Assert.assertEquals(200, response.getStatus());
        }

        @Test
        public void testPathTemplateInSubResourceMethod2() throws ParserConfigurationException, SAXException, IOException,
                XPathExpressionException {
            final Response response = target("root").request(MediaTypes.WADL).options();
            Assert.assertEquals(200, response.getStatus());
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
            public String createWidget(String bar, @MatrixParam("test") String test) {
                return bar;
            }

            @PUT
            @Path("{id}")
            @Consumes("application/xml")
            public void updateWidget(String bar, @PathParam("id") int id) {
            }

            @GET
            @Path("{id}")
            @Produces({"application/xml", "application/json"})
            public String getWidget(@PathParam("id") int id) {
                return null;
            }

            @DELETE
            @Path("{id}")
            public void deleteWidget(@PathParam("id") int id) {
            }

            @Path("{id}/verbose")
            public ExtraResource getVerbose(@PathParam("id") int id) {
                return new ExtraResource();
            }
        }

        @Test
        public void testDisableWadl() throws ExecutionException, InterruptedException {
            ResourceConfig rc = new ResourceConfig(WidgetsResource.class, ExtraResource.class);
            rc.property(ServerProperties.WADL_FEATURE_DISABLE, true);

            ApplicationHandler applicationHandler = new ApplicationHandler(rc);

            final ContainerResponse containerResponse = applicationHandler.apply(new ContainerRequest(
                    URI.create("/"), URI.create("/application.wadl"),
                    "GET", null, new MapPropertiesDelegate())).get();

            assertEquals(404, containerResponse.getStatus());
        }

        @Test
        public void testEnableWadl() throws ExecutionException, InterruptedException {
            ResourceConfig rc = new ResourceConfig(WidgetsResource.class, ExtraResource.class);
            rc.property(ServerProperties.WADL_FEATURE_DISABLE, false);

            ApplicationHandler applicationHandler = new ApplicationHandler(rc);

            final ContainerResponse containerResponse = applicationHandler.apply(new ContainerRequest(
                    URI.create("/"), URI.create("/application.wadl"),
                    "GET", null, new MapPropertiesDelegate())).get();

            assertEquals(200, containerResponse.getStatus());
        }

        public static void printSource(Source source) {
            try {
                System.out.println("---------------------");
                Transformer trans = TransformerFactory.newInstance().newTransformer();
                Properties oprops = new Properties();
                oprops.put(OutputKeys.OMIT_XML_DECLARATION, "yes");
                oprops.put(OutputKeys.INDENT, "yes");
                oprops.put(OutputKeys.METHOD, "xml");
                trans.setOutputProperties(oprops);
                trans.transform(source, new StreamResult(System.out));
            } catch (Exception e) {
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
//            File tmpFile = target("application.wadl").request().get(File.class);
            File tmpFile = target("application.wadl").request().get(File.class);
            final String str = target("application.wadl").request().get(String.class);

            DocumentBuilderFactory bf = DocumentBuilderFactory.newInstance();
            bf.setNamespaceAware(true);
            bf.setValidating(false);
            if (!SaxHelper.isXdkDocumentBuilderFactory(bf)) {
                bf.setXIncludeAware(false);
            }
            DocumentBuilder b = bf.newDocumentBuilder();
            Document d = b.parse(tmpFile);
            printSource(new DOMSource(d));
            XPath xp = XPathFactory.newInstance().newXPath();
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
            val = (String) xp.evaluate("count(//wadl:resource[@path='widgets']/wadl:method[@name='GET']/wadl:response/wadl" +
                    ":representation)", d, XPathConstants.STRING);
            assertEquals("2", val);
            // check number of output representations is one
            val = (String) xp.evaluate("count(//wadl:resource[@path='widgets']/wadl:method[@name='POST']/wadl:request/wadl" +
                    ":representation)", d, XPathConstants.STRING);
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

            Response r = target.request().get(Response.class);
            assertTrue(r.getHeaders().containsKey("Last-modified"));
        }

        @Test
        public void testLastModifiedOPTIONS() {
            final WebTarget target = target("/widgets/3/verbose");

            Response r = target.request(MediaTypes.WADL).options();
            System.out.println(r.readEntity(String.class));
            assertTrue(r.getHeaders().containsKey("Last-modified"));
        }

        @Test
        public void testOptionsResourceWadl() throws ParserConfigurationException, XPathExpressionException, IOException,
                SAXException {
            // test WidgetsResource
            Response response = target("/widgets").request(MediaTypes.WADL).options();
            Assert.assertEquals(200, response.getStatus());
//            System.out.println(response.readEntity(String.class));
            File tmpFile = response.readEntity(File.class);
            DocumentBuilderFactory bf = DocumentBuilderFactory.newInstance();
            bf.setNamespaceAware(true);
            bf.setValidating(false);
            if (!SaxHelper.isXdkDocumentBuilderFactory(bf)) {
                bf.setXIncludeAware(false);
            }
            DocumentBuilder b = bf.newDocumentBuilder();
            Document d = b.parse(tmpFile);
            printSource(new DOMSource(d));
            XPath xp = XPathFactory.newInstance().newXPath();
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
            String prefix = getXmlSchemaNamespacePrefix(
                    (Node) xp.evaluate("//wadl:resource[@path='{id}']/wadl:param[@name='id']", d, XPathConstants.NODE));
            val = (String) xp.evaluate("//wadl:resource[@path='{id}']/wadl:param[@name='id']/@type", d, XPathConstants.STRING);
            assertEquals(val, (prefix == null ? "" : prefix + ":") + "int");
            // check number of output representations is two
            val = (String) xp.evaluate("count(//wadl:resource[@path='widgets']/wadl:method[@name='GET']/wadl:response/wadl" +
                    ":representation)", d, XPathConstants.STRING);
            assertEquals("2", val);
            // check number of output representations is one
            val = (String) xp.evaluate("count(//wadl:resource[@path='widgets']/wadl:method[@name='POST']/wadl:request/wadl" +
                    ":representation)", d, XPathConstants.STRING);
            assertEquals("1", val);

            response = target("/foo").request(MediaTypes.WADL).options();
            Assert.assertEquals(200, response.getStatus());
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
            File tmpFile = target("/widgets/3/verbose").request(MediaTypes.WADL).options(File.class);
            DocumentBuilderFactory bf = DocumentBuilderFactory.newInstance();
            bf.setNamespaceAware(true);
            bf.setValidating(false);
            if (!SaxHelper.isXdkDocumentBuilderFactory(bf)) {
                bf.setXIncludeAware(false);
            }
            DocumentBuilder b = bf.newDocumentBuilder();
            Document d = b.parse(tmpFile);
            printSource(new DOMSource(d));
            XPath xp = XPathFactory.newInstance().newXPath();
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
            File tmpFile = target("/widgets/3").request(MediaTypes.WADL).options(File.class);
            DocumentBuilderFactory bf = DocumentBuilderFactory.newInstance();
            bf.setNamespaceAware(true);
            bf.setValidating(false);
            if (!SaxHelper.isXdkDocumentBuilderFactory(bf)) {
                bf.setXIncludeAware(false);
            }
            DocumentBuilder b = bf.newDocumentBuilder();
            Document d = b.parse(tmpFile);
            printSource(new DOMSource(d));
            XPath xp = XPathFactory.newInstance().newXPath();
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

        // TODO: migrate rest of tests
        //    @Path("root")
//    public static class RootResource {
//        @Path("loc")
//        public Object getSub() {
//            return new SubResource();
//        }
//
//        @Path("switch")
//        @POST
//        public void switchMethod(@Context WadlApplicationContext wadlApplicationContext) {
//            wadlApplicationContext.setWadlGenerationEnabled(!wadlApplicationContext.isWadlGenerationEnabled());
//
//        }
//    }
//
//    public static class SubResource {
//        @Path("loc")
//        public Object getSub() {
//            return new SubResource();
//        }
//
//        @GET
//        @Produces("text/plain")
//        public String hello() {
//            return "Hello World !";
//        }
//
//        @GET
//        @Path("sub")
//        @Produces("text/plain")
//        public String helloSub() {
//            return "Hello World !";
//        }
//    }
//
//    public void testRecursive() throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
//        initiateWebApplication(RootResource.class);
//        WebResource r = resource("/root/loc");
//
//        // test WidgetsResource
//        File tmpFile = r.accept(MediaTypes.WADL).options(File.class);
//        DocumentBuilderFactory bf = DocumentBuilderFactory.newInstance();
//        bf.setNamespaceAware(true);
//        bf.setValidating(false);
//        if (!SaxHelper.isXdkDocumentBuilderFactory(bf)) {
//            bf.setXIncludeAware(false);
//        }
//        DocumentBuilder b = bf.newDocumentBuilder();
//        Document d = b.parse(tmpFile);
//        printSource(new DOMSource(d));
//        XPath xp = XPathFactory.newInstance().newXPath();
//        xp.setNamespaceContext(new NSResolver("wadl", "http://wadl.dev.java.net/2009/02"));
//        String val = (String)xp.evaluate("/wadl:application/wadl:resources/@base", d, XPathConstants.STRING);
//        assertEquals(val,BASE_URI.toString());
//        // check only one resource with for 'root/loc'
//        val = (String)xp.evaluate("count(//wadl:resource[@path='root/loc'])", d, XPathConstants.STRING);
//        assertEquals(val,"1");
//
//        r = resource("/root/loc/loc");
//
//        // test WidgetsResource
//        tmpFile = r.accept(MediaTypes.WADL).options(File.class);
//        bf = DocumentBuilderFactory.newInstance();
//        bf.setNamespaceAware(true);
//        bf.setValidating(false);
//        if (!SaxHelper.isXdkDocumentBuilderFactory(bf)) {
//            bf.setXIncludeAware(false);
//        }
//        b = bf.newDocumentBuilder();
//        d = b.parse(tmpFile);
//        printSource(new DOMSource(d));
//        xp = XPathFactory.newInstance().newXPath();
//        xp.setNamespaceContext(new NSResolver("wadl", "http://wadl.dev.java.net/2009/02"));
//        val = (String)xp.evaluate("/wadl:application/wadl:resources/@base", d, XPathConstants.STRING);
//        assertEquals(val,BASE_URI.toString());
//        // check only one resource with for 'root/loc'
//        val = (String)xp.evaluate("count(//wadl:resource[@path='root/loc/loc'])", d, XPathConstants.STRING);
//        assertEquals(val,"1");
//
//    }
//
//    @Path("root1")
//    public static class RootResource1 {
//        @Path("loc")
//        public SubResource getSub() {
//            return new SubResource();
//        }
//    }
//
//    @Path("root2")
//    public static class RootResource2 {
//        @Path("loc")
//        public SubResource getSub() {
//            return new SubResource();
//        }
//    }
//
//    public void testRecursive2() throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
//        initiateWebApplication(RootResource1.class, RootResource2.class);
//        WebResource r = resource("/application.wadl");
//
//        File tmpFile = r.get(File.class);
//        DocumentBuilderFactory bf = DocumentBuilderFactory.newInstance();
//        bf.setNamespaceAware(true);
//        bf.setValidating(false);
//        if (!SaxHelper.isXdkDocumentBuilderFactory(bf)) {
//            bf.setXIncludeAware(false);
//        }
//        DocumentBuilder b = bf.newDocumentBuilder();
//        Document d = b.parse(tmpFile);
//        printSource(new DOMSource(d));
//
//        XPath xp = XPathFactory.newInstance().newXPath();
//        xp.setNamespaceContext(new NSResolver("wadl", "http://wadl.dev.java.net/2009/02"));
//        String val = (String)xp.evaluate("/wadl:application/wadl:resources/@base", d, XPathConstants.STRING);
//        assertEquals(val,BASE_URI.toString());
//        // check only one resource with for 'root/loc'
//        val = (String)xp.evaluate("count(//wadl:resource[@path='loc'])", d, XPathConstants.STRING);
//        assertEquals("4", val);
//        // check for method with id of hello
//        val = (String)xp.evaluate("count(//wadl:resource[@path='loc']/wadl:method[@id='hello'])", d, XPathConstants.STRING);
//        assertEquals("2", val);
//    }
//
//
//    @Path("form")
//    public static class FormResource {
//
//        @POST
//        @Consumes( "application/x-www-form-urlencoded" )
//        public void post(
//                @FormParam( "a" ) String a,
//                @FormParam( "b" ) String b,
//                @FormParam( "c" ) JAXBBean c,
//                @FormParam( "c" ) FormDataContentDisposition cdc,
//                Form form ) {
//        }
//
//    }
//
//    public void testFormParam() throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
//        initiateWebApplication(FormResource.class);
//        WebResource r = resource("/application.wadl");
//
//        File tmpFile = r.get(File.class);
//        DocumentBuilderFactory bf = DocumentBuilderFactory.newInstance();
//        bf.setNamespaceAware(true);
//        bf.setValidating(false);
//        if (!SaxHelper.isXdkDocumentBuilderFactory(bf)) {
//            bf.setXIncludeAware(false);
//        }
//        DocumentBuilder b = bf.newDocumentBuilder();
//        Document d = b.parse(tmpFile);
//        printSource(new DOMSource(d));
//        XPath xp = XPathFactory.newInstance().newXPath();
//        xp.setNamespaceContext(new NSResolver("wadl", "http://wadl.dev.java.net/2009/02"));
//
//        final String requestPath = "//wadl:resource[@path='form']/wadl:method[@name='POST']/wadl:request";
//        final String representationPath = requestPath + "/wadl:representation";
//
//        // check number of request params is zero
//        int count = ( (Double)xp.evaluate("count(" + requestPath + "/wadl:param)", d, XPathConstants.NUMBER) ).intValue();
//        assertEquals( 0, count );
//
//        // check number of request representations is one
//        count = ( (Double)xp.evaluate("count(" + representationPath + ")", d, XPathConstants.NUMBER) ).intValue();
//        assertEquals( 1, count );
//
//        // check number of request representation params is three
//        count = ( (Double)xp.evaluate("count(" + representationPath + "/wadl:param)", d, XPathConstants.NUMBER) ).intValue();
//        assertEquals( 3, count );
//
//        // check the style of the request representation param is 'query'
//        String val = (String)xp.evaluate( representationPath + "/wadl:param[@name='a']/@style", d, XPathConstants.STRING);
//        assertEquals( "query", val );
//        val = (String)xp.evaluate( representationPath + "/wadl:param[@name='b']/@style", d, XPathConstants.STRING);
//        assertEquals( "query", val );
//
//    }
//
//
//    @Path("fieldParam/{pp}")
//    public static class FieldParamResource {
//
//        @HeaderParam("hp") String hp;
//        @MatrixParam("mp") String mp;
//        @PathParam("pp") String pp;
//        @QueryParam("q") String q;
//
//        @GET
//        @Produces("text/plain" )
//        public String get() {
//            return pp;
//        }
//
//    }
//
//    public void testFieldParam() throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
//        _testFieldAndSetterParam(FieldParamResource.class, "fieldParam");
//    }
//
//    @Path("setterParam/{pp}")
//    public static class SetterParamResource {
//
//        @HeaderParam("hp")
//        public void setHp(String hp) {};
//
//        @MatrixParam("mp")
//        public void setMp(String mp) {};
//
//        @PathParam("pp")
//        public void setPP(String pp) {};
//
//        @QueryParam("q")
//        public void setQ(String q) {};
//
//        @GET
//        @Produces("text/plain" )
//        public String get() {
//            return "nonsense";
//        }
//
//    }
//
//    public void testSetterParam() throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
//        _testFieldAndSetterParam(SetterParamResource.class, "setterParam");
//    }
//
//    public void testEnableDisableRuntime() {
//        initiateWebApplication(RootResource.class);
//        WebResource r = resource("/", false);
//        r.addFilter(new LoggingFilter());
//
//        ClientResponse response = r.path("application.wadl").get(ClientResponse.class);
//        assertTrue(response.getStatus() == 200);
//
//        response = r.path("root").options(ClientResponse.class);
//        assertTrue(response.getStatus() == 200);
//
//        r.path("root/switch").post();
//
//        response = r.path("application.wadl").get(ClientResponse.class);
//        assertTrue(response.getStatus() == 404);
//
//        response = r.path("root").options(ClientResponse.class);
//        assertTrue(response.getStatus() == 204);
//
//        r.path("root/switch").post();
//
//        response = r.path("application.wadl").get(ClientResponse.class);
//        assertTrue(response.getStatus() == 200);
//
//        response = r.path("root").options(ClientResponse.class);
//        assertTrue(response.getStatus() == 200);
//    }
//
//    private void _testFieldAndSetterParam(Class resourceClass, String path) throws ParserConfigurationException,
// SAXException, IOException, XPathExpressionException {
//        initiateWebApplication(resourceClass);
//        WebResource r = resource("/application.wadl");
//
//        File tmpFile = r.get(File.class);
//        DocumentBuilderFactory bf = DocumentBuilderFactory.newInstance();
//        bf.setNamespaceAware(true);
//        bf.setValidating(false);
//        if (!SaxHelper.isXdkDocumentBuilderFactory(bf)) {
//            bf.setXIncludeAware(false);
//        }
//        DocumentBuilder b = bf.newDocumentBuilder();
//        Document d = b.parse(tmpFile);
//        printSource(new DOMSource(d));
//        XPath xp = XPathFactory.newInstance().newXPath();
//        xp.setNamespaceContext(new NSResolver("wadl", "http://wadl.dev.java.net/2009/02"));
//
//        final String resourcePath = String.format("//wadl:resource[@path='%s/{pp}']", path);
//        final String methodPath = resourcePath + "/wadl:method[@name='GET']";
//
//        // check number of resource methods is one
//        int methodCount = ( (Double)xp.evaluate("count(" + methodPath + ")", d, XPathConstants.NUMBER) ).intValue();
//        assertEquals(1, methodCount );
//
//        Map<String, String> paramStyles = new HashMap<String, String>();
//
//        paramStyles.put("hp", "header");
//        paramStyles.put("mp", "matrix");
//        paramStyles.put("pp", "template");
//        paramStyles.put("q", "query");
//
//        for(Map.Entry<String, String> param : paramStyles.entrySet()) {
//
//            String pName = param.getKey();
//            String pStyle = param.getValue();
//
//            String paramXPath = String.format("%s/wadl:param[@name='%s']", resourcePath, pName);
//
//            // check number of params is one
//            int pc = ( (Double)xp.evaluate("count(" + paramXPath + ")", d, XPathConstants.NUMBER) ).intValue();
//            assertEquals(1, pc );
//
//            // check the style of the param
//            String style = (String)xp.evaluate(paramXPath + "/@style", d, XPathConstants.STRING);
//            assertEquals(pStyle, style );
//        }
//    }
//
//
        public static class MyWadlGeneratorConfig extends WadlGeneratorConfig {

            @Override
            public List<WadlGeneratorDescription> configure() {
                return generator(MyWadlGenerator.class).descriptions();
            }

            public static class MyWadlGenerator extends WadlGeneratorImpl {
                public static final String CUSTOM_RESOURCES_BASE_URI = "http://myBaseUri";

                @Override
                public Resources createResources() {
                    Resources resources = super.createResources();
                    resources.setBase(CUSTOM_RESOURCES_BASE_URI);

                    return resources;
                }

                @Override
                public void setWadlGeneratorDelegate(WadlGenerator delegate) {
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
            ResourceConfig rc = new ResourceConfig(WidgetsResource.class, ExtraResource.class);
            rc.property(ServerProperties.WADL_GENERATOR_CONFIG, MyWadlGeneratorConfig.class.getName());

            ApplicationHandler applicationHandler = new ApplicationHandler(rc);

            final ContainerResponse containerResponse = applicationHandler.apply(new ContainerRequest(
                    URI.create("/"), URI.create("/application.wadl"),
                    "GET", null, new MapPropertiesDelegate())).get();

            DocumentBuilderFactory bf = DocumentBuilderFactory.newInstance();
            bf.setNamespaceAware(true);
            bf.setValidating(false);
            if (!SaxHelper.isXdkDocumentBuilderFactory(bf)) {
                bf.setXIncludeAware(false);
            }
            DocumentBuilder b = bf.newDocumentBuilder();

            ((ByteArrayInputStream) containerResponse.getEntity()).reset();

            Document d = b.parse((InputStream) containerResponse.getEntity());
            printSource(new DOMSource(d));
            XPath xp = XPathFactory.newInstance().newXPath();
            xp.setNamespaceContext(new SimpleNamespaceResolver("wadl", "http://wadl.dev.java.net/2009/02"));
            // check base URI
            String val = (String) xp.evaluate("/wadl:application/wadl:resources/@base", d, XPathConstants.STRING);
            assertEquals(val, MyWadlGeneratorConfig.MyWadlGenerator.CUSTOM_RESOURCES_BASE_URI);
        }

        //
//    @Path("jresponse")
//    public static class JResponseTestResource {
//        @GET
//        @Produces("text/plain")
//        public JResponse<List<String>> getClichedMessage() {
//            // Return some cliched textual content
//            return JResponse.<List<String>>status(200).entity(new ArrayList() {
//                {
//                    add("Hello world!");
//                }
//            }).build();
//        }
//    }
//
//    public void testJresponse() throws Exception {
//        ResourceConfig rc = new DefaultResourceConfig(JResponseTestResource.class);
//        initiateWebApplication(rc);
//
//        WebResource r = resource("/application.wadl");
//
//        assertTrue(r.get(String.class).length() > 0);
//    }
//
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
            ResourceConfig rc = new ResourceConfig(EmptyProducesTestResource.class);
            rc.property(ServerProperties.WADL_FEATURE_DISABLE, false);

            ApplicationHandler applicationHandler = new ApplicationHandler(rc);

            final ContainerResponse containerResponse = applicationHandler.apply(new ContainerRequest(
                    URI.create("/"), URI.create("/application.wadl"),
                    "GET", null, new MapPropertiesDelegate())).get();

            assertEquals(200, containerResponse.getStatus());

            ((ByteArrayInputStream) containerResponse.getEntity()).reset();

            DocumentBuilderFactory bf = DocumentBuilderFactory.newInstance();
            bf.setNamespaceAware(true);
            bf.setValidating(false);
            if (!SaxHelper.isXdkDocumentBuilderFactory(bf)) {
                bf.setXIncludeAware(false);
            }
            DocumentBuilder b = bf.newDocumentBuilder();
            Document d = b.parse((InputStream) containerResponse.getEntity());
            printSource(new DOMSource(d));
            XPath xp = XPathFactory.newInstance().newXPath();
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
            public void switchMethod(@Context WadlApplicationContext wadlApplicationContext) {
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

        private void _testRecursiveWadl(String path) throws ParserConfigurationException, SAXException, IOException,
                XPathExpressionException {
            WebTarget webTarget = target(path);
            // test WidgetsResource
            Document d = extractWadlAsDocument(target(path).request(MediaTypes.WADL).options());

            XPath xp = XPathFactory.newInstance().newXPath();
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
            Document d = extractWadlAsDocument(target("/application.wadl").request().get());

            XPath xp = XPathFactory.newInstance().newXPath();
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
                    @FormParam("a") String a,
                    @FormParam("b") String b,
                    @FormParam("c") JaxbBean c,
                    @FormParam("c") FormDataContentDisposition cdc,
                    Form form) {
            }

        }

        @Test
        public void testFormParam() throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
            Document d = extractWadlAsDocument(target("/application.wadl").request().get());
            XPath xp = XPathFactory.newInstance().newXPath();
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

    @Ignore // TODO - fails -> fix it and unignore
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
            @QueryParam("q")
            String q;

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

        private static void _testFieldAndSetterParam(Response response, String path) throws ParserConfigurationException,
                SAXException, IOException, XPathExpressionException {

            Document d = extractWadlAsDocument(response);
            XPath xp = XPathFactory.newInstance().newXPath();
            xp.setNamespaceContext(new SimpleNamespaceResolver("wadl", "http://wadl.dev.java.net/2009/02"));

            final String resourcePath = String.format("//wadl:resource[@path='%s/{pp}']", path);
            final String methodPath = resourcePath + "/wadl:method[@name='GET']";

            // check number of resource methods is one
            int methodCount = ((Double) xp.evaluate("count(" + methodPath + ")", d, XPathConstants.NUMBER)).intValue();
            assertEquals(1, methodCount);

            Map<String, String> paramStyles = new HashMap<String, String>();

            paramStyles.put("hp", "header");
            paramStyles.put("mp", "matrix");
            paramStyles.put("pp", "template");
            paramStyles.put("q", "query");

            for (Map.Entry<String, String> param : paramStyles.entrySet()) {

                String pName = param.getKey();
                String pStyle = param.getValue();

                String paramXPath = String.format("%s/wadl:param[@name='%s']", resourcePath, pName);

                // check number of params is one
                int pc = ((Double) xp.evaluate("count(" + paramXPath + ")", d, XPathConstants.NUMBER)).intValue();
                assertEquals(1, pc);

                // check the style of the param
                String style = (String) xp.evaluate(paramXPath + "/@style", d, XPathConstants.STRING);
                assertEquals(pStyle, style);
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
                    final Response response = target("wadl6test").
                            path(path).
                            request("application/vnd.sun.wadl+xml").
                            options();
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
            public String postB(String str) {
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
            public String postTemplateB(String str) {
                return "template-b";
            }
        }

        @Test
        @Ignore("WADL Options invoked on resources with same template returns only methods from one of these resources")
        // TODO: fix
        public void testWadlForAmbiguousResourceTemplates() throws IOException, SAXException, ParserConfigurationException,
                XPathExpressionException {
            final Response response = target().path("foo").request(MediaTypes.WADL).options();
            Document d = extractWadlAsDocument(response);
            XPath xp = XPathFactory.newInstance().newXPath();
            xp.setNamespaceContext(new SimpleNamespaceResolver("wadl", "http://wadl.dev.java.net/2009/02"));

            String result = (String) xp.evaluate("//wadl:resource/wadl:method[@name='GET']/@id", d, XPathConstants.STRING);
            Assert.assertEquals("getA", result);


            result = (String) xp.evaluate("//wadl:resource/wadl:method[@name='POST']/@id", d, XPathConstants.STRING);
            Assert.assertEquals("postB", result);
        }

        @Test
        @Ignore("WADL Options invoked on resources with same template returns only methods from one of these resources")
        // TODO: fix
        public void testWadlForAmbiguousChildResourceTemplates() throws IOException, SAXException, ParserConfigurationException,
                XPathExpressionException {
            final Response response = target().path("resource/bar").request(MediaTypes.WADL).options();

            Document d = extractWadlAsDocument(response);
            XPath xp = XPathFactory.newInstance().newXPath();
            xp.setNamespaceContext(new SimpleNamespaceResolver("wadl", "http://wadl.dev.java.net/2009/02"));

            String result = (String) xp.evaluate("//wadl:resource/wadl:method[@name='GET']/@id", d, XPathConstants.STRING);
            Assert.assertEquals("getTemplateA", result);

            result = (String) xp.evaluate("//wadl:resource/wadl:method[@name='POST']/@id", d, XPathConstants.STRING);
            Assert.assertEquals("postTemplateB", result);
        }
    }


    /**
     * Tests usage of property {@link ServerProperties#METAINF_SERVICES_LOOKUP_DISABLE}.
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
            ResourceConfig resourceConfig = new ResourceConfig(Resource.class);
            resourceConfig.property(ServerProperties.METAINF_SERVICES_LOOKUP_DISABLE, true);

            return resourceConfig;
        }

        @Test
        public void testEmptyWadlResult() throws Exception {
            Response response = target("/application.wadl").request().get();
            assertTrue(response.getStatus() == 404);
        }

    } // class Wadl9Test

}
