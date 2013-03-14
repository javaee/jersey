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

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.Charset;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.glassfish.jersey.examples.extendedwadl.resources.MyApplication;
import org.glassfish.jersey.internal.util.SimpleNamespaceResolver;
import org.glassfish.jersey.message.internal.MediaTypes;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import org.w3c.dom.Document;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import static junit.framework.Assert.assertEquals;

/**
 *
 * @author Naresh
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 */
public class ExtendedWadlWebappTest extends JerseyTest {

    @Override
    protected Application configure() {
        final ResourceConfig resourceConfig = new ResourceConfig(new MyApplication().getClasses());
        resourceConfig.property(ServerProperties.PROPERTY_WADL_GENERATOR_CONFIG, "org.glassfish.jersey.examples.extendedwadl" +
                ".SampleWadlGeneratorConfig");

        final Resource.Builder resourceBuilder = Resource.builder();
        resourceBuilder.name("resource-programmatic").path("programmatic").addMethod("GET")

                .handledBy(new ProgrammaticResource());
        resourceConfig.registerResources(resourceBuilder.build());
        return resourceConfig;
    }

    @Override
    protected URI getBaseUri() {
        return UriBuilder.fromUri(super.getBaseUri()).path("extended-wadl-webapp").build();
    }

    /**
     * Test checks that the WADL generated using the WadlGenerator api doesn't
     * contain the expected text.
     * @throws java.lang.Exception
     */
    @Test
    public void testExtendedWadl() throws Exception {
        String wadl = target().path("application.wadl").request(MediaTypes.WADL).get(String.class);

        System.out.println(wadl);
        assertTrue("Generated wadl is of null length", wadl.length() > 0);
        assertTrue("Generated wadl doesn't contain the expected text",
                wadl.contains("This is a paragraph"));

        assertFalse(wadl.contains("application.wadl/xsd0.xsd"));
    }

    @Test
    public void testWadlOptionsMethod() throws Exception {
        String wadl = target().path("items").request(MediaTypes.WADL).options(String.class);

        System.out.println(wadl);
        assertTrue("Generated wadl is of null length", wadl.length() > 0);
        assertTrue("Generated wadl doesn't contain the expected text",
                wadl.contains("This is a paragraph"));
        checkWadl(wadl, getBaseUri());
    }

    /**
     * Programmatic resource class javadoc.
     */
    private static class ProgrammaticResource implements Inflector<ContainerRequestContext, Response> {
        @Override
        public Response apply(ContainerRequestContext data) {
            return Response.ok("programmatic").build();
        }
    }

    private void checkWadl(String wadl, URI baseUri) throws Exception {
        DocumentBuilderFactory bf = DocumentBuilderFactory.newInstance();
        bf.setNamespaceAware(true);
        bf.setValidating(false);
//        if (!SaxHelper.isXdkDocumentBuilderFactory(bf)) {
//            bf.setXIncludeAware(false);
//        }
        DocumentBuilder b = bf.newDocumentBuilder();
        Document document = b.parse(new ByteArrayInputStream(wadl.getBytes(Charset.forName("UTF-8"))));
        XPath xp = XPathFactory.newInstance().newXPath();
        xp.setNamespaceContext(new SimpleNamespaceResolver("ns2", "http://wadl.dev.java.net/2009/02"));
        String val = (String) xp.evaluate("/ns2:application/ns2:resources/@base", document, XPathConstants.STRING);
        assertEquals(baseUri.toString(), val.endsWith("/") ? val.substring(0, val.length() - 1) : val);
        val = (String) xp.evaluate("count(//ns2:resource)", document, XPathConstants.STRING);
        assertEquals(val, "3");
        val = (String) xp.evaluate("count(//ns2:resource[@path='items'])", document, XPathConstants.STRING);
        assertEquals("1", val);
        val = (String) xp.evaluate("count(//ns2:resource[@path='{id}'])", document, XPathConstants.STRING);
        assertEquals("1", val);
        val = (String) xp.evaluate("count(//ns2:resource[@path='value/{value}'])", document, XPathConstants.STRING);
        assertEquals("1", val);

        val = (String) xp.evaluate("count(//ns2:resource[@path='{id}']/ns2:method)", document, XPathConstants.STRING);
        assertEquals("2", val);
        val = (String) xp.evaluate("count(//ns2:resource[@path='items']/ns2:method)", document, XPathConstants.STRING);
        assertEquals("4", val);
        val = (String) xp.evaluate("count(//ns2:resource[@path='value/{value}']/ns2:method)", document, XPathConstants.STRING);
        assertEquals("1", val);

        val = (String) xp.evaluate("count(//ns2:application/ns2:doc)", document, XPathConstants.STRING);
        assertEquals("3", val);
    }
}
