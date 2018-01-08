/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2017 Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.io.StringReader;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import static org.junit.Assert.assertEquals;

/**
 * Tests, that Jersey returns wildcard mediaType in case no response representation was specified.
 *
 * @author Adam Lindenthal (adam.lindenthal at oracle.com)
 */
public class WadlEmptyMediaTypeTest extends JerseyTest {

    @Path("test")
    public static class WadlEmptyMediaTypeTestResource {
        @Path("getEmpty")
        @GET
        public String getEmpty() {
            return "No @Produces annotation";
        }

        @Path("getText")
        @Produces("text/plain")
        @GET
        public String getText() {
            return "Produces text/plain";
        }
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(WadlEmptyMediaTypeTestResource.class);
    }

    @Test
    public void testOverride() throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
        WebTarget target = target("/application.wadl");
        String wadl = target.request().get(String.class);

        InputSource is = new InputSource(new StringReader(wadl));
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);

        XPath xpath = XPathFactory.newInstance().newXPath();
        String val = xpath.evaluate("//method[@id='getEmpty']/response/representation/@mediaType", document);
        assertEquals("*/*", val);

        val = xpath.evaluate("//method[@id='getText']/response/representation/@mediaType", document);
        assertEquals("text/plain", val);
    }
}
