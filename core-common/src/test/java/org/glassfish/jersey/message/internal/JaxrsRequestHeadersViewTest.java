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

import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.RuntimeDelegate;

import org.glassfish.jersey.internal.TestRuntimeDelegate;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * JaxrsRequestViewTest class.
 *
 * @author Santiago Pericas-Geertsen (santiago.pericasgeertsen at oracle.com)
 */
public class JaxrsRequestHeadersViewTest {

    public JaxrsRequestHeadersViewTest() {
        RuntimeDelegate.setInstance(new TestRuntimeDelegate());
    }

    @Test
    public void testAcceptableMediaTypes() throws URISyntaxException {
        MutableRequest r = new MutableRequest("", "http://example.org/app/resource", "GET");
        r.header(HttpHeaders.ACCEPT, "application/xml, text/plain");
        r.header(HttpHeaders.ACCEPT, "application/json");
        JaxrsRequestHeadersView v =r.getJaxrsHeaders();
        assertEquals(v.getAcceptableMediaTypes().size(), 3);
        assertTrue(v.getAcceptableMediaTypes().contains(MediaType.APPLICATION_XML_TYPE));
        assertTrue(v.getAcceptableMediaTypes().contains(MediaType.TEXT_PLAIN_TYPE));
        assertTrue(v.getAcceptableMediaTypes().contains(MediaType.APPLICATION_JSON_TYPE));
    }

    @Test
    public void testAcceptableLanguages() throws URISyntaxException {
        MutableRequest r = new MutableRequest("", "http://example.org/app/resource", "GET");
        r.header(HttpHeaders.ACCEPT_LANGUAGE, "en-gb;q=0.8, en;q=0.7");
        r.header(HttpHeaders.ACCEPT_LANGUAGE, "de");
        JaxrsRequestHeadersView v =r.getJaxrsHeaders();
        assertEquals(v.getAcceptableLanguages().size(), 3);
        assertTrue(v.getAcceptableLanguages().contains(Locale.UK));
        assertTrue(v.getAcceptableLanguages().contains(Locale.ENGLISH));
        assertTrue(v.getAcceptableLanguages().contains(Locale.GERMAN));
    }

    @Test
    public void testCookies() throws URISyntaxException {
        MutableRequest r = new MutableRequest("", "http://example.org/app/resource", "GET");
        r.header(HttpHeaders.COOKIE, "oreo=chocolate");
        r.header(HttpHeaders.COOKIE, "nilla=vanilla");
        JaxrsRequestHeadersView v =r.getJaxrsHeaders();
        assertEquals(v.getCookies().size(), 2);
        assertTrue(v.getCookies().containsKey("oreo"));
        assertTrue(v.getCookies().containsKey("nilla"));
        CookieProvider cp = new CookieProvider();
        assertTrue(v.getCookies().containsValue(cp.fromString("oreo=chocolate")));
        assertTrue(v.getCookies().containsValue(cp.fromString("nilla=vanilla")));
    }

    @Test
    public void testDate() throws URISyntaxException, ParseException {
        MutableRequest r = new MutableRequest("", "http://example.org/app/resource", "GET");
        r.header(HttpHeaders.DATE, "Tue, 29 Jan 2002 22:14:02 -0500");
        JaxrsRequestHeadersView v =r.getJaxrsHeaders();
        SimpleDateFormat f = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
        Date date = f.parse("Tue, 29 Jan 2002 22:14:02 -0500");
        assertEquals(v.getDate(), date);
    }

    @Test
    public void testHeader() throws URISyntaxException, ParseException {
        MutableRequest r = new MutableRequest("", "http://example.org/app/resource", "GET");
        r.header(HttpHeaders.ACCEPT, "application/xml, text/plain");
        r.header(HttpHeaders.ACCEPT, "application/json");
        r.header("FOO", "");
        JaxrsRequestHeadersView v =r.getJaxrsHeaders();
        assertTrue(v.getHeaderString(HttpHeaders.ACCEPT).contains("application/xml"));
        assertTrue(v.getHeaderString(HttpHeaders.ACCEPT).contains("text/plain"));
        assertTrue(v.getHeaderString(HttpHeaders.ACCEPT).contains("application/json"));
        assertEquals(v.getHeaderString("FOO").length(), 0);
        assertNull(v.getHeaderString("BAR"));
    }

    @Test
    public void testHeaderMap() throws URISyntaxException, ParseException {
        MutableRequest r = new MutableRequest("", "http://example.org/app/resource", "GET");
        r.header(HttpHeaders.ACCEPT, "application/xml, text/plain");
        r.header(HttpHeaders.ACCEPT, "application/json");
        r.header("Allow", "GET, PUT");
        r.header("Allow", "POST");
        JaxrsRequestHeadersView v =r.getJaxrsHeaders();
        assertTrue(v.getRequestHeaders().containsKey(HttpHeaders.ACCEPT));
        assertTrue(v.getRequestHeaders().containsKey("Allow"));
        assertTrue(v.getRequestHeaders().get(HttpHeaders.ACCEPT).contains("application/json"));
        assertTrue(v.getRequestHeaders().get("Allow").contains("POST"));
    }

    @Test
    public void testHeaderValues() throws URISyntaxException, ParseException {
        MutableRequest r = new MutableRequest("", "http://example.org/app/resource", "GET");
        r.header(HttpHeaders.ACCEPT, "application/xml, text/plain");
        r.header(HttpHeaders.ACCEPT, "application/json");
        JaxrsRequestHeadersView v =r.getJaxrsHeaders();
        assertTrue(v.getRequestHeader(HttpHeaders.ACCEPT).contains("application/xml, text/plain"));
        assertTrue(v.getRequestHeader(HttpHeaders.ACCEPT).contains("application/json"));
    }

    @Test
    public void testContentLanguage() throws URISyntaxException, ParseException {
        MutableRequest r = new MutableRequest("", "http://example.org/app/resource", "GET");
        r.header(HttpHeaders.CONTENT_LANGUAGE, "en");
        JaxrsRequestHeadersView v =r.getJaxrsHeaders();
        assertEquals(v.getLanguage(), Locale.ENGLISH);
    }

    @Test
    public void testLength() throws URISyntaxException, ParseException {
        MutableRequest r = new MutableRequest("", "http://example.org/app/resource", "GET");
        r.header(HttpHeaders.CONTENT_LENGTH, "1024");
        JaxrsRequestHeadersView v =r.getJaxrsHeaders();
        assertEquals(v.getLength(), 1024);
    }

    @Test
    public void testMediaType() throws URISyntaxException, ParseException {
        MutableRequest r = new MutableRequest("", "http://example.org/app/resource", "GET");
        r.header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML);
        JaxrsRequestHeadersView v =r.getJaxrsHeaders();
        assertEquals(v.getMediaType(), MediaType.TEXT_HTML_TYPE);
    }
}
