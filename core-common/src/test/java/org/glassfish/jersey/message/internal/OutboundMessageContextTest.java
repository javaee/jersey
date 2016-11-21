/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2016 Oracle and/or its affiliates. All rights reserved.
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
import java.util.List;
import java.util.Locale;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.RuntimeDelegate;

import org.glassfish.jersey.internal.TestRuntimeDelegate;

import org.junit.Test;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * {@link OutboundMessageContext} test.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class OutboundMessageContextTest {

    public OutboundMessageContextTest() {
        RuntimeDelegate.setInstance(new TestRuntimeDelegate());
    }

    @Test
    public void testAcceptableMediaTypes() throws URISyntaxException {
        OutboundMessageContext r = new OutboundMessageContext();

        r.getHeaders().add(HttpHeaders.ACCEPT, "application/xml, text/plain");
        r.getHeaders().add(HttpHeaders.ACCEPT, "application/json");

        final List<MediaType> acceptableMediaTypes = r.getAcceptableMediaTypes();

        assertThat(acceptableMediaTypes.size(), equalTo(3));
        assertThat(acceptableMediaTypes,
                contains(MediaType.APPLICATION_XML_TYPE, MediaType.TEXT_PLAIN_TYPE, MediaType.APPLICATION_JSON_TYPE));
    }

    @Test
    public void testAcceptableLanguages() throws URISyntaxException {
        OutboundMessageContext r = new OutboundMessageContext();
        r.getHeaders().add(HttpHeaders.ACCEPT_LANGUAGE, "en-gb;q=0.8, en;q=0.7");
        r.getHeaders().add(HttpHeaders.ACCEPT_LANGUAGE, "de");
        assertEquals(r.getAcceptableLanguages().size(), 3);
        assertTrue(r.getAcceptableLanguages().contains(Locale.UK));
        assertTrue(r.getAcceptableLanguages().contains(Locale.ENGLISH));
        assertTrue(r.getAcceptableLanguages().contains(Locale.GERMAN));
    }

    @Test
    public void testRequestCookies() throws URISyntaxException {
        OutboundMessageContext r = new OutboundMessageContext();
        r.getHeaders().add(HttpHeaders.COOKIE, "oreo=chocolate");
        r.getHeaders().add(HttpHeaders.COOKIE, "nilla=vanilla");
        assertEquals(r.getRequestCookies().size(), 2);
        assertTrue(r.getRequestCookies().containsKey("oreo"));
        assertTrue(r.getRequestCookies().containsKey("nilla"));
        CookieProvider cp = new CookieProvider();
        assertTrue(r.getRequestCookies().containsValue(cp.fromString("oreo=chocolate")));
        assertTrue(r.getRequestCookies().containsValue(cp.fromString("nilla=vanilla")));
    }

    @Test
    public void testDate() throws URISyntaxException, ParseException {
        OutboundMessageContext r = new OutboundMessageContext();
        r.getHeaders().add(HttpHeaders.DATE, "Tue, 29 Jan 2002 22:14:02 -0500");
        SimpleDateFormat f = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
        Date date = f.parse("Tue, 29 Jan 2002 22:14:02 -0500");
        assertEquals(r.getDate(), date);
    }

    @Test
    public void testHeader() throws URISyntaxException, ParseException {
        OutboundMessageContext r = new OutboundMessageContext();
        r.getHeaders().add(HttpHeaders.ACCEPT, "application/xml, text/plain");
        r.getHeaders().add(HttpHeaders.ACCEPT, "application/json");
        r.getHeaders().add("FOO", "");
        assertTrue(r.getHeaderString(HttpHeaders.ACCEPT).contains("application/xml"));
        assertTrue(r.getHeaderString(HttpHeaders.ACCEPT).contains("text/plain"));
        assertTrue(r.getHeaderString(HttpHeaders.ACCEPT).contains("application/json"));
        assertEquals(r.getHeaderString("FOO").length(), 0);
        assertNull(r.getHeaderString("BAR"));
    }

    @Test
    public void testHeaderMap() throws URISyntaxException, ParseException {
        OutboundMessageContext r = new OutboundMessageContext();
        r.getHeaders().add(HttpHeaders.ACCEPT, "application/xml, text/plain");
        r.getHeaders().add(HttpHeaders.ACCEPT, "application/json");
        r.getHeaders().add("Allow", "GET, PUT");
        r.getHeaders().add("Allow", "POST");
        assertTrue(r.getHeaders().containsKey(HttpHeaders.ACCEPT));
        assertTrue(r.getHeaders().containsKey("Allow"));
        assertTrue(r.getHeaders().get(HttpHeaders.ACCEPT).contains("application/json"));
        assertTrue(r.getHeaders().get("Allow").contains("POST"));
    }

    @Test
    public void testAllowedMethods() throws URISyntaxException {
        OutboundMessageContext r = new OutboundMessageContext();
        r.getHeaders().add("Allow", "GET, PUT");
        r.getHeaders().add("Allow", "POST");
        assertEquals(3, r.getAllowedMethods().size());
        assertTrue(r.getAllowedMethods().contains("GET"));
        assertTrue(r.getAllowedMethods().contains("PUT"));
        assertTrue(r.getAllowedMethods().contains("POST"));
        assertFalse(r.getAllowedMethods().contains("DELETE"));
    }

    @Test
    public void testResponseCookies() throws URISyntaxException {
        OutboundMessageContext r = new OutboundMessageContext();
        r.getHeaders().add(HttpHeaders.SET_COOKIE, "oreo=chocolate");
        r.getHeaders().add(HttpHeaders.SET_COOKIE, "nilla=vanilla");
        assertEquals(2, r.getResponseCookies().size());
        assertTrue(r.getResponseCookies().containsKey("oreo"));
        assertTrue(r.getResponseCookies().containsKey("nilla"));
    }

    @Test
    public void testEntityTag() throws URISyntaxException {
        OutboundMessageContext r = new OutboundMessageContext();
        r.getHeaders().add(HttpHeaders.ETAG, "\"tag\"");
        assertEquals(EntityTag.valueOf("\"tag\""), r.getEntityTag());
    }

    @Test
    public void testLastModified() throws URISyntaxException, ParseException {
        OutboundMessageContext r = new OutboundMessageContext();
        r.getHeaders().add(HttpHeaders.LAST_MODIFIED, "Tue, 29 Jan 2002 22:14:02 -0500");
        SimpleDateFormat f = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
        Date date = f.parse("Tue, 29 Jan 2002 22:14:02 -0500");
        assertEquals(date, r.getLastModified());
    }

    @Test
    public void testLocation() throws URISyntaxException {
        OutboundMessageContext r = new OutboundMessageContext();
        r.getHeaders().add(HttpHeaders.LOCATION, "http://example.org/app");
        assertEquals(URI.create("http://example.org/app"), r.getLocation());
    }

    @Test
    public void testGetLinks() {
        OutboundMessageContext r = new OutboundMessageContext();
        Link link1 = Link.fromUri("http://example.org/app/link1").param("produces", "application/json").param("method", "GET").rel("self").build();
        Link link2 = Link.fromUri("http://example.org/app/link2").param("produces", "application/xml").param("method", "PUT").rel("self").build();
        r.getHeaders().add("Link", link1.toString());
        r.getHeaders().add("Link", link2.toString());
        assertEquals(2, r.getLinks().size());
        assertTrue(r.getLinks().contains(link1));
        assertTrue(r.getLinks().contains(link2));
    }

    @Test
    public void testGetLink() {
        OutboundMessageContext r = new OutboundMessageContext();
        Link link1 = Link.fromUri("http://example.org/app/link1").param("produces", "application/json").param("method", "GET").rel("self").build();
        Link link2 = Link.fromUri("http://example.org/app/link2").param("produces", "application/xml").param("method", "PUT").rel("update").build();
        Link link3 = Link.fromUri("http://example.org/app/link2").param("produces", "application/xml").param("method", "POST").rel("update").build();
        r.getHeaders().add("Link", link1.toString());
        r.getHeaders().add("Link", link2.toString());
        r.getHeaders().add("Link", link3.toString());
        assertTrue(r.getLink("self").equals(link1));
        assertTrue(r.getLink("update").equals(link2) || r.getLink("update").equals(link3));
    }

    @Test
    public void testGetLength() {
        OutboundMessageContext r = new OutboundMessageContext();
        r.getHeaders().add("Content-Length", 50);
        assertEquals(50, r.getLengthLong());
    }

    @Test
    public void testGetLength_tooLongForInt() {
        OutboundMessageContext r = new OutboundMessageContext();
        long length = Integer.MAX_VALUE + 5L;
        r.getHeaders().add("Content-Length", length);


        assertEquals(length, r.getLengthLong());

        // value is not a valid integer -> ProcessingException is thrown.
        try {
            r.getLength();
        } catch (ProcessingException e) {
            return;
        }

        fail();
    }
}
