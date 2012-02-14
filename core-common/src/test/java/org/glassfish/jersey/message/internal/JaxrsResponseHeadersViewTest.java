/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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

import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.ResponseHeaders;
import javax.ws.rs.ext.RuntimeDelegate;

import org.glassfish.jersey.internal.TestRuntimeDelegate;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * JaxrsResponseViewTest class.
 *
 * @author Santiago Pericas-Geertsen (santiago.pericasgeertsen at oracle.com)
 */
public class JaxrsResponseHeadersViewTest {

    private final MutableRequest request;

    public JaxrsResponseHeadersViewTest() {
        RuntimeDelegate.setInstance(new TestRuntimeDelegate());
        request = new MutableRequest("http://example.org/app/",
                "http://example.org/app/resource?foo1=bar1&foo2=bar2", "GET");
    }

    @Test
    public void testMediaType() throws URISyntaxException, ParseException {
        MutableResponse mr = new MutableResponse(Status.OK, request.workers());
        mr.header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML);
        ResponseHeaders r = mr.getJaxrsHeaders();
        assertEquals(r.getMediaType(), MediaType.TEXT_HTML_TYPE);
    }

    @Test
    public void testLength() throws URISyntaxException, ParseException {
        MutableResponse mr = new MutableResponse(Status.OK, request.workers());
        mr.header(HttpHeaders.CONTENT_LENGTH, "1024");
        ResponseHeaders r = mr.getJaxrsHeaders();
        assertEquals(r.getLength(), 1024);
    }

    @Test
    public void testContentLanguage() throws URISyntaxException, ParseException {
        MutableResponse mr = new MutableResponse(Status.OK, request.workers());
        mr.header(HttpHeaders.CONTENT_LANGUAGE, "en");
        ResponseHeaders r = mr.getJaxrsHeaders();
        assertEquals(r.getLanguage(), Locale.ENGLISH);
    }

    @Test
    public void testHeaderValues() throws URISyntaxException, ParseException {
        MutableResponse mr = new MutableResponse(Status.OK, request.workers());
        mr.header(HttpHeaders.ACCEPT, "application/xml, text/plain");
        mr.header(HttpHeaders.ACCEPT, "application/json");
        ResponseHeaders r = mr.getJaxrsHeaders();
        assertTrue(r.getHeaderValues(HttpHeaders.ACCEPT).contains("application/xml, text/plain"));
        assertTrue(r.getHeaderValues(HttpHeaders.ACCEPT).contains("application/json"));
    }

    @Test
    public void testHeaderMap() throws URISyntaxException, ParseException {
        MutableResponse mr = new MutableResponse(Status.OK, request.workers());
        mr.header(HttpHeaders.ACCEPT, "application/xml, text/plain");
        mr.header(HttpHeaders.ACCEPT, "application/json");
        mr.header("Allow", "GET, PUT");
        mr.header("Allow", "POST");
        ResponseHeaders r = mr.getJaxrsHeaders();
        assertTrue(r.asMap().containsKey(HttpHeaders.ACCEPT));
        assertTrue(r.asMap().containsKey("Allow"));
        assertTrue(r.asMap().get(HttpHeaders.ACCEPT).contains("application/json"));
        assertTrue(r.asMap().get("Allow").contains("POST"));
    }

    @Test
    public void testHeader() throws URISyntaxException, ParseException {
        MutableResponse mr = new MutableResponse(Status.OK, request.workers());
        mr.header(HttpHeaders.ACCEPT, "application/xml, text/plain");
        mr.header(HttpHeaders.ACCEPT, "application/json");
        mr.header("FOO", "");
        ResponseHeaders r = mr.getJaxrsHeaders();
        assertTrue(r.getHeader(HttpHeaders.ACCEPT).contains("application/xml"));
        assertTrue(r.getHeader(HttpHeaders.ACCEPT).contains("text/plain"));
        assertTrue(r.getHeader(HttpHeaders.ACCEPT).contains("application/json"));
        assertEquals(r.getHeader("FOO").length(), 0);
        assertNull(r.getHeader("BAR"));
    }

    @Test
    public void testDate() throws URISyntaxException, ParseException {
        MutableResponse mr = new MutableResponse(Status.OK, request.workers());
        mr.header(HttpHeaders.DATE, "Tue, 29 Jan 2002 22:14:02 -0500");
        ResponseHeaders r = mr.getJaxrsHeaders();
        SimpleDateFormat f = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss Z");
        Date date = f.parse("Tue, 29 Jan 2002 22:14:02 -0500");
        assertEquals(r.getDate(), date);
    }

    public void testAllowedMethods() throws URISyntaxException {
        MutableResponse mr = new MutableResponse(Status.OK, request.workers());
        mr.header("Allow", "GET, PUT");
        mr.header("Allow", "POST");
        ResponseHeaders r = mr.getJaxrsHeaders();
        assertEquals(r.getAllowedMethods().size(), 3);
        assertTrue(r.getAllowedMethods().contains("GET"));
        assertTrue(r.getAllowedMethods().contains("PUT"));
        assertTrue(r.getAllowedMethods().contains("POST"));
        assertFalse(r.getAllowedMethods().contains("DELETE"));
    }

    @Test
    public void testCookies() throws URISyntaxException {
        MutableResponse mr = new MutableResponse(Status.OK, request.workers());
        mr.header(HttpHeaders.COOKIE, "oreo=chocolate");
        mr.header(HttpHeaders.COOKIE, "nilla=vanilla");
        ResponseHeaders r = mr.getJaxrsHeaders();
        assertEquals(r.getCookies().size(), 2);
        assertTrue(r.getCookies().containsKey("oreo"));
        assertTrue(r.getCookies().containsKey("nilla"));
    }

    @Test
    public void testEntityTag() throws URISyntaxException {
        MutableResponse mr = new MutableResponse(Status.OK, request.workers());
        mr.header(HttpHeaders.ETAG, "\"tag\"");
        ResponseHeaders r = mr.getJaxrsHeaders();
        assertEquals(r.getEntityTag(), EntityTag.valueOf("\"tag\""));
    }

    @Test
    public void testLastModified() throws URISyntaxException, ParseException {
        MutableResponse mr = new MutableResponse(Status.OK, request.workers());
        mr.header(HttpHeaders.LAST_MODIFIED, "Tue, 29 Jan 2002 22:14:02 -0500");
        ResponseHeaders r = mr.getJaxrsHeaders();
        SimpleDateFormat f = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss Z");
        Date date = f.parse("Tue, 29 Jan 2002 22:14:02 -0500");
        assertEquals(r.getLastModified(), date);
    }

    @Test
    public void testLocation() throws URISyntaxException {
        MutableResponse mr = new MutableResponse(Status.OK, request.workers());
        mr.header(HttpHeaders.LOCATION, "http://example.org/app");
        ResponseHeaders r = mr.getJaxrsHeaders();
        assertEquals(r.getLocation(), URI.create("http://example.org/app"));
    }

    @Test
    public void testGetLinks() {
        MutableResponse mr = new MutableResponse(Status.OK, request.workers());
        Link link1 = Link.fromUri("http://example.org/app/link1").param("produces", "application/json").param("method", "GET").rel("self").build();
        Link link2 = Link.fromUri("http://example.org/app/link2").param("produces", "application/xml").param("method", "PUT").rel("self").build();
        mr.header("Link", link1.toString());
        mr.header("Link", link2.toString());
        ResponseHeaders r = mr.getJaxrsHeaders();
        assertEquals(r.getLinks().size(), 2);
        assertTrue(r.getLinks().contains(link1));
        assertTrue(r.getLinks().contains(link2));
    }

    @Test
    public void testGetLink() {
        MutableResponse mr = new MutableResponse(Status.OK, request.workers());
        Link link1 = Link.fromUri("http://example.org/app/link1").param("produces", "application/json").param("method", "GET").rel("self").build();
        Link link2 = Link.fromUri("http://example.org/app/link2").param("produces", "application/xml").param("method", "PUT").rel("update").build();
        Link link3 = Link.fromUri("http://example.org/app/link2").param("produces", "application/xml").param("method", "POST").rel("update").build();
        mr.header("Link", link1.toString());
        mr.header("Link", link2.toString());
        mr.header("Link", link3.toString());
        ResponseHeaders r = mr.getJaxrsHeaders();
        assertTrue(r.getLink("self").equals(link1));
        assertTrue(r.getLink("update").equals(link2) || r.getLink("update").equals(link3));
    }
}
