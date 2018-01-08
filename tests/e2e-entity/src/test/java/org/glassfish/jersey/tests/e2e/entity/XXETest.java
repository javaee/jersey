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
package org.glassfish.jersey.tests.e2e.entity;

import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;

import javax.xml.bind.JAXBElement;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import static org.junit.Assert.assertEquals;

/**
 * Tests xml security.
 *
 * @author Paul Sandoz
 */
public class XXETest extends JerseyTest {

    private static final String DOCTYPE = "<!DOCTYPE foo [<!ENTITY xxe SYSTEM \"%s\">]>";
    private static final String XML = "<jaxbBean><value>&xxe;</value></jaxbBean>";

    private String getDocument() {
        final URL u = this.getClass().getResource("xxe.txt");
        return String.format(DOCTYPE, u.toString()) + XML;
    }

    private String getListDocument() {
        final URL u = this.getClass().getResource("xxe.txt");
        return String.format(DOCTYPE, u.toString()) + "<jaxbBeans>" + XML + XML + XML + "</jaxbBeans>";
    }

    @Path("/")
    @Consumes("application/xml")
    @Produces("application/xml")
    public static class EntityHolderResource {

        @Path("jaxb")
        @POST
        public String post(final JaxbBean s) {
            return s.value;
        }

        @Path("jaxbelement")
        @POST
        public String post(final JAXBElement<JaxbBeanType> s) {
            return s.getValue().value;
        }

        @Path("jaxb/list")
        @POST
        public String post(final List<JaxbBean> s) {
            return s.get(0).value;
        }

        @Path("sax")
        @POST
        public SAXSource postSax(final SAXSource s) {
            return s;
        }

        @Path("dom")
        @POST
        public String postDom(final DOMSource s) {
            final Document d = (Document) s.getNode();
            final Element e = (Element) d.getElementsByTagName("value").item(0);
            final Node n = e.getChildNodes().item(0);
            if (n.getNodeType() == Node.TEXT_NODE) {
                return n.getNodeValue();
            } else if (n.getNodeType() == Node.ENTITY_REFERENCE_NODE) {
                return "";
            } else {
                throw new WebApplicationException(400);
            }
        }

        @Path("stream")
        @POST
        public StreamSource postStream(final StreamSource s) {
            return s;
        }
    }

    @Override
    public ResourceConfig configure() {
        return new ResourceConfig(EntityHolderResource.class);
    }

    @Test
    public void testJAXBSecure() {
        final String s = target().path("jaxb").request("application/xml").post(Entity.entity(getDocument(),
                MediaType.APPLICATION_XML_TYPE), String.class);
        assertEquals("", s);
    }

    @Test
    public void testJAXBSecureWithThreads() throws Throwable {
        final int n = 4;
        final CountDownLatch latch = new CountDownLatch(n);

        final Runnable runnable = new Runnable() {
            public void run() {
                try {
                    final String s = target().path("jaxb").request("application/xml").post(Entity.entity(getDocument(),
                            MediaType.TEXT_PLAIN_TYPE), String.class);
                    assertEquals("", s);
                } finally {
                    latch.countDown();
                }
            }
        };

        final Set<Throwable> s = new HashSet<Throwable>();
        for (int i = 0; i < n; i++) {
            final Thread t = new Thread(runnable);
            t.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                public void uncaughtException(final Thread t, final Throwable ex) {
                    s.add(ex);
                }
            });
            t.start();
        }

        try {
            latch.await();
        } catch (final InterruptedException ignored) {
        }
    }

    @Test
    public void testJAXBElementSecure() {
        final String s = target().path("jaxbelement").request("application/xml").post(Entity.entity(getDocument(),
                MediaType.APPLICATION_XML_TYPE), String.class);
        assertEquals("", s);
    }

    @Ignore // TODO
    @Test
    public void testJAXBListSecure() {
        final String s = target().path("jaxb/list").request("application/xml").post(Entity.entity(getListDocument(),
                MediaType.APPLICATION_XML_TYPE), String.class);
        assertEquals("", s);
    }

    @Test
    public void testSAXSecure() {
        final JaxbBean b = target().path("sax").request("application/xml").post(Entity.entity(getDocument(),
                MediaType.APPLICATION_XML_TYPE), JaxbBean.class);
        assertEquals("", b.value);
    }

    @Test
    public void testDOMSecure() {
        final String s = target().path("dom").request("application/xml").post(Entity.entity(getDocument(),
                MediaType.APPLICATION_XML_TYPE), String.class);
        assertEquals("", s);
    }

    @Test
    public void testStreamSecure() {
        final JaxbBean b = target().path("stream").request("application/xml").post(Entity.entity(getDocument(),
                MediaType.APPLICATION_XML_TYPE), JaxbBean.class);
        assertEquals("", b.value);
    }

    // NOTE - this is a tes migrated from Jersey 1.x tests. The original test class contains also insecure "versions" of the
    // methods above configured via FeaturesAndProperties.FEATURE_DISABLE_XML_SECURITY. Those methods are ommited,
    // as Jersey 2 does not support such consturct.

}
