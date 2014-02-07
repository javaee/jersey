/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2014 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.message.internal;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Set;

import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Link;
import javax.ws.rs.ext.RuntimeDelegate;

import org.glassfish.jersey.internal.TestRuntimeDelegate;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * {@link org.glassfish.jersey.message.internal.InboundMessageContext} test.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class InboundMessageContextTest {

    public InboundMessageContextTest() {
        RuntimeDelegate.setInstance(new TestRuntimeDelegate());
    }

    @Test
    public void testNoLength() {
        InboundMessageContext r = new InboundMessageContext();
        assertEquals(-1, r.getLength());
    }

    @Test
    public void testRequestCookies() throws URISyntaxException {
        InboundMessageContext r = new InboundMessageContext();
        r.header(HttpHeaders.COOKIE, "oreo=chocolate");
        r.header(HttpHeaders.COOKIE, "nilla=vanilla");
        assertEquals(r.getRequestCookies().size(), 2);
        assertTrue(r.getRequestCookies().containsKey("oreo"));
        assertTrue(r.getRequestCookies().containsKey("nilla"));
        CookieProvider cp = new CookieProvider();
        assertTrue(r.getRequestCookies().containsValue(cp.fromString("oreo=chocolate")));
        assertTrue(r.getRequestCookies().containsValue(cp.fromString("nilla=vanilla")));
    }

    @Test
    public void testDate() throws URISyntaxException, ParseException {
        InboundMessageContext r = new InboundMessageContext();
        r.header(HttpHeaders.DATE, "Tue, 29 Jan 2002 22:14:02 -0500");
        SimpleDateFormat f = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
        Date date = f.parse("Tue, 29 Jan 2002 22:14:02 -0500");
        assertEquals(r.getDate(), date);
    }

    @Test
    public void testHeader() throws URISyntaxException, ParseException {
        InboundMessageContext r = new InboundMessageContext();
        r.header(HttpHeaders.ACCEPT, "application/xml, text/plain");
        r.header(HttpHeaders.ACCEPT, "application/json");
        r.header("FOO", "");
        assertTrue(r.getHeaderString(HttpHeaders.ACCEPT).contains("application/xml"));
        assertTrue(r.getHeaderString(HttpHeaders.ACCEPT).contains("text/plain"));
        assertTrue(r.getHeaderString(HttpHeaders.ACCEPT).contains("application/json"));
        assertEquals(r.getHeaderString("FOO").length(), 0);
        assertNull(r.getHeaderString("BAR"));
    }

    @Test
    public void testHeaderMap() throws URISyntaxException, ParseException {
        InboundMessageContext r = new InboundMessageContext();
        r.header(HttpHeaders.ACCEPT, "application/xml, text/plain");
        r.header(HttpHeaders.ACCEPT, "application/json");
        r.header("Allow", "GET, PUT");
        r.header("Allow", "POST");
        assertTrue(r.getHeaders().containsKey(HttpHeaders.ACCEPT));
        assertTrue(r.getHeaders().containsKey("Allow"));
        assertTrue(r.getHeaders().get(HttpHeaders.ACCEPT).contains("application/json"));
        assertTrue(r.getHeaders().get("Allow").contains("POST"));
    }

    @Test
    public void testAllowedMethods() throws URISyntaxException {
        InboundMessageContext r = new InboundMessageContext();
        r.header("Allow", "GET, PUT");
        r.header("Allow", "POST");
        assertEquals(3, r.getAllowedMethods().size());
        assertTrue(r.getAllowedMethods().contains("GET"));
        assertTrue(r.getAllowedMethods().contains("PUT"));
        assertTrue(r.getAllowedMethods().contains("POST"));
        assertFalse(r.getAllowedMethods().contains("DELETE"));
    }

    @Test
    public void testResponseCookies() throws URISyntaxException {
        InboundMessageContext r = new InboundMessageContext();
        r.header(HttpHeaders.SET_COOKIE, "oreo=chocolate");
        r.header(HttpHeaders.SET_COOKIE, "nilla=vanilla");
        assertEquals(2, r.getResponseCookies().size());
        assertTrue(r.getResponseCookies().containsKey("oreo"));
        assertTrue(r.getResponseCookies().containsKey("nilla"));
    }

    @Test
    public void testEntityTag() throws URISyntaxException {
        InboundMessageContext r = new InboundMessageContext();
        r.header(HttpHeaders.ETAG, "\"tag\"");
        assertEquals(EntityTag.valueOf("\"tag\""), r.getEntityTag());
    }

    @Test
    public void testLastModified() throws URISyntaxException, ParseException {
        InboundMessageContext r = new InboundMessageContext();
        r.header(HttpHeaders.LAST_MODIFIED, "Tue, 29 Jan 2002 22:14:02 -0500");
        SimpleDateFormat f = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
        Date date = f.parse("Tue, 29 Jan 2002 22:14:02 -0500");
        assertEquals(date, r.getLastModified());
    }

    @Test
    public void testLocation() throws URISyntaxException {
        InboundMessageContext r = new InboundMessageContext();
        r.header(HttpHeaders.LOCATION, "http://example.org/app");
        assertEquals(URI.create("http://example.org/app"), r.getLocation());
    }

    @Test
    public void testGetLinks() {
        InboundMessageContext r = new InboundMessageContext();
        Link link1 = Link.fromUri("http://example.org/app/link1").param("produces", "application/json").param("method",
                "GET").rel("self").build();
        Link link2 = Link.fromUri("http://example.org/app/link2").param("produces", "application/xml").param("method",
                "PUT").rel("self").build();
        r.header("Link", link1.toString());
        r.header("Link", link2.toString());
        assertEquals(2, r.getLinks().size());
        assertTrue(r.getLinks().contains(link1));
        assertTrue(r.getLinks().contains(link2));
    }

    @Test
    public void testGetLink() {
        InboundMessageContext r = new InboundMessageContext();
        Link link1 = Link.fromUri("http://example.org/app/link1").param("produces", "application/json").param("method",
                "GET").rel("self").build();
        Link link2 = Link.fromUri("http://example.org/app/link2").param("produces", "application/xml").param("method",
                "PUT").rel("update").build();
        Link link3 = Link.fromUri("http://example.org/app/link2").param("produces", "application/xml").param("method",
                "POST").rel("update").build();
        r.header("Link", link1.toString());
        r.header("Link", link2.toString());
        r.header("Link", link3.toString());
        assertTrue(r.getLink("self").equals(link1));
        assertTrue(r.getLink("update").equals(link2) || r.getLink("update").equals(link3));
    }


    @Test
    public void testGetAllowedMethods() {
        InboundMessageContext r = new InboundMessageContext();
        r.header(HttpHeaders.ALLOW, "a,B,CcC,dDd");
        final Set<String> allowedMethods = r.getAllowedMethods();
        assertEquals(4, allowedMethods.size());
        assertTrue(allowedMethods.contains("A"));
        assertTrue(allowedMethods.contains("B"));
        assertTrue(allowedMethods.contains("CCC"));
        assertTrue(allowedMethods.contains("DDD"));
    }
}
