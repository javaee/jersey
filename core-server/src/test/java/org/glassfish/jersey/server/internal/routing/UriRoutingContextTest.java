/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.server.internal.routing;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriInfo;

import org.glassfish.jersey.server.RequestContextBuilder;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class UriRoutingContextTest {

    public UriRoutingContextTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    private UriRoutingContext createContext(String requestUri, String method) {
        return new UriRoutingContext(RequestContextBuilder.from(requestUri, method).build());
    }

    private UriRoutingContext createContext(String appBaseUri, String requestUri, String method) {
        return new UriRoutingContext(RequestContextBuilder.from(appBaseUri, requestUri, method).build());
    }

    @Test
    public void testGetAbsolutePath() throws URISyntaxException {
        UriRoutingContext context;

        context = createContext("http://example.org/app/", "http://example.org/app/resource?foo1=bar1&foo2=bar2", "GET");
        assertEquals(URI.create("http://example.org/app/resource"), context.getAbsolutePath());

        context = createContext("http://example.org/app/", "http://example.org/app/resource%20decoded?foo1=bar1", "GET");
        assertEquals(URI.create("http://example.org/app/resource%20decoded"), context.getAbsolutePath());
    }

    @Test
    public void testGetPath() throws URISyntaxException {
        UriRoutingContext context;

        context = createContext("http://example.org/my%20app/resource?foo1=bar1&foo2=bar2", "GET");
        assertEquals("my app/resource", context.getPath());

        context = createContext("http://example.org/my%20app/", "http://example.org/my%20app/resource?foo1=bar1&foo2=bar2",
                "GET");
        assertEquals("resource", context.getPath());

        context = createContext("http://example.org/my%20app/",
                "http://example.org/my%20app/resource?foo1=bar1&foo2=bar2", "GET");
        assertEquals("resource", context.getPath());
    }

    @Test
    public void testGetDecodedPath() throws URISyntaxException {
        UriRoutingContext ctx = createContext("http://example.org/my%20app/resource?foo1=bar1&foo2=bar2", "GET");
        assertEquals("my%20app/resource", ctx.getPath(false));
        assertEquals("my app/resource", ctx.getPath(true));
    }

    @Test
    public void testGetPathBuilder() throws URISyntaxException {
        UriRoutingContext ctx = createContext("http://example.org/my%20app/",
                "http://example.org/my%20app/resource?foo1=bar1&foo2=bar2", "GET");
        assertEquals(URI.create("http://example.org/my%20app/resource"), ctx.getAbsolutePathBuilder().build());
    }

    @Test
    public void testGetPathSegments() throws URISyntaxException {
        List<PathSegment> lps = createContext("http://example.org/app/",
                "http://example.org/app/my%20resource/my%20subresource", "GET").getPathSegments();
        assertEquals(2, lps.size());
        assertEquals("my resource", lps.get(0).getPath());
        assertEquals("my subresource", lps.get(1).getPath());

        try {
            lps.remove(0);
            fail("UnsupportedOperationException expected - returned list should not be modifiable.");
        } catch (UnsupportedOperationException ex) {
            // passed
        }
    }

    @Test
    public void testGetPathSegments2() throws URISyntaxException {
        List<PathSegment> lps = createContext("http://example.org/app/",
                "http://example.org/app/my%20resource/my%20subresource", "GET").getPathSegments(false);
        assertEquals(2, lps.size());
        assertEquals("my%20resource", lps.get(0).getPath());
        assertEquals("my%20subresource", lps.get(1).getPath());

        try {
            lps.remove(0);
            fail("UnsupportedOperationException expected - returned list should not be modifiable.");
        } catch (UnsupportedOperationException ex) {
            // passed
        }
    }

    @Test
    public void testQueryParams() throws URISyntaxException {
        MultivaluedMap<String, String> map =
                createContext("http://example.org/app/resource?foo1=bar1&foo2=bar2", "GET").getQueryParameters();
        assertEquals(2, map.size());
        assertEquals("bar1", map.getFirst("foo1"));
        assertEquals("bar2", map.getFirst("foo2"));

        try {
            map.remove("foo1");
            fail("UnsupportedOperationException expected - returned list should not be modifiable.");
        } catch (UnsupportedOperationException ex) {
            // passed
        }
    }

    @Test
    public void testQueryParamsDecoded() throws URISyntaxException {
        MultivaluedMap<String, String> map =
                createContext("http://example.org/app/resource?foo1=%7Bbar1%7D&foo2=%7Bbar2%7D", "GET").getQueryParameters(true);
        assertEquals(2, map.size());
        assertEquals("{bar1}", map.getFirst("foo1"));
        assertEquals("{bar2}", map.getFirst("foo2"));
        try {
            map.remove("foo1");
            fail("UnsupportedOperationException expected - returned list should not be modifiable.");
        } catch (UnsupportedOperationException ex) {
            // passed
        }
    }

    /**
     * Migrated Jersey 1.x {@code com.sun.jersey.impl.PathSegmentsHttpRequestTest}.
     */
    @Test
    public void testGetPathSegmentsGeneral() {
        final UriInfo ui = createContext("/p1;x=1;y=1/p2;x=2;y=2/p3;x=3;y=3", "GET");

        List<PathSegment> segments = ui.getPathSegments();
        assertEquals(3, segments.size());

        final Iterator<PathSegment> psi = segments.iterator();
        PathSegment segment;

        segment = psi.next();
        assertEquals("p1", segment.getPath());
        MultivaluedMap<String, String> m = segment.getMatrixParameters();
        assertEquals("1", m.getFirst("x"));
        assertEquals("1", m.getFirst("y"));

        segment = psi.next();
        assertEquals("p2", segment.getPath());
        m = segment.getMatrixParameters();
        assertEquals("2", m.getFirst("x"));
        assertEquals("2", m.getFirst("y"));

        segment = psi.next();
        assertEquals("p3", segment.getPath());
        m = segment.getMatrixParameters();
        assertEquals("3", m.getFirst("x"));
        assertEquals("3", m.getFirst("y"));
    }

    /**
     * Migrated Jersey 1.x {@code com.sun.jersey.impl.PathSegmentsHttpRequestTest}.
     */
    @Test
    public void testGetPathSegmentsMultipleSlash() {
        final UriInfo ui = createContext("/p//p//p//", "GET");
        List<PathSegment> segments = ui.getPathSegments();
        assertEquals(7, segments.size());

        final Iterator<PathSegment> psi = segments.iterator();
        PathSegment segment;

        segment = psi.next();
        assertEquals("p", segment.getPath());
        assertEquals(0, segment.getMatrixParameters().size());

        segment = psi.next();
        assertEquals("", segment.getPath());
        assertEquals(0, segment.getMatrixParameters().size());

        segment = psi.next();
        assertEquals("p", segment.getPath());
        assertEquals(0, segment.getMatrixParameters().size());

        segment = psi.next();
        assertEquals("", segment.getPath());
        assertEquals(0, segment.getMatrixParameters().size());

        segment = psi.next();
        assertEquals("p", segment.getPath());
        assertEquals(0, segment.getMatrixParameters().size());

        segment = psi.next();
        assertEquals("", segment.getPath());
        assertEquals(0, segment.getMatrixParameters().size());

        segment = psi.next();
        assertEquals("", segment.getPath());
        assertEquals(0, segment.getMatrixParameters().size());
    }

    /**
     * Migrated Jersey 1.x {@code com.sun.jersey.impl.PathSegmentsHttpRequestTest}.
     */
    @Test
    public void testGetPathSegmentsMultipleMatrix() {
        final UriInfo ui = createContext("/p;x=1;x=2;x=3", "GET");
        List<PathSegment> segments = ui.getPathSegments();
        assertEquals(1, segments.size());

        final Iterator<PathSegment> psi = segments.iterator();
        PathSegment segment;

        segment = psi.next();
        MultivaluedMap<String, String> m = segment.getMatrixParameters();
        List<String> values = m.get("x");
        for (int i = 0; i < m.size(); i++) {
            assertEquals(Integer.valueOf(i + 1).toString(), values.get(i));
        }
    }

    /**
     * Migrated Jersey 1.x {@code com.sun.jersey.impl.PathSegmentsHttpRequestTest}.
     */
    @Test
    public void testGetPathSegmentsMultipleSlashmulitpleMatrix() {
        final UriInfo ui = createContext("/;x=1;y=1/;x=2;y=2/;x=3;y=3", "GET");
        List<PathSegment> segments = ui.getPathSegments();
        assertEquals(3, segments.size());

        final Iterator<PathSegment> psi = segments.iterator();
        PathSegment segment;

        segment = psi.next();
        MultivaluedMap<String, String> m = segment.getMatrixParameters();
        assertEquals("1", m.getFirst("x"));
        assertEquals("1", m.getFirst("y"));

        segment = psi.next();
        m = segment.getMatrixParameters();
        assertEquals("2", m.getFirst("x"));
        assertEquals("2", m.getFirst("y"));

        segment = psi.next();
        m = segment.getMatrixParameters();
        assertEquals("3", m.getFirst("x"));
        assertEquals("3", m.getFirst("y"));
    }

    /**
     * Migrated Jersey 1.x {@code com.sun.jersey.impl.QueryParametersHttpRequestTest}.
     */
    @Test
    public void testGetQueryParametersGeneral() throws Exception {
        final UriInfo ui = createContext("/widgets/10?verbose=true&item=1&item=2", "GET");
        MultivaluedMap<String, String> p = ui.getQueryParameters();
        assertEquals(p.get("verbose").size(), 1);
        assertEquals(p.getFirst("verbose"), "true");
        assertEquals(p.get("item").size(), 2);
        assertEquals(p.getFirst("item"), "1");
        assertEquals(p.get("foo"), null);
        assertEquals(p.getFirst("foo"), null);
    }

    /**
     * Migrated Jersey 1.x {@code com.sun.jersey.impl.QueryParametersHttpRequestTest}.
     */
    @Test
    public void testGetQueryParametersEmpty() throws Exception {
        final UriInfo ui = createContext("/widgets/10", "GET");
        MultivaluedMap<String, String> p = ui.getQueryParameters();
        assertEquals(p.size(), 0);
    }

    /**
     * Migrated Jersey 1.x {@code com.sun.jersey.impl.QueryParametersHttpRequestTest}.
     */
    @Test
    public void testGetQueryParametersSingleAmpersand() throws Exception {
        final UriInfo ui = createContext("/widgets/10?&", "GET");
        MultivaluedMap<String, String> p = ui.getQueryParameters();
        assertEquals(p.size(), 0);
    }

    /**
     * Migrated Jersey 1.x {@code com.sun.jersey.impl.QueryParametersHttpRequestTest}.
     */
    @Test
    public void testGetQueryParametersMultipleAmpersand() throws Exception {
        final UriInfo ui = createContext("/widgets/10?&&%20=%20&&&", "GET");
        MultivaluedMap<String, String> p = ui.getQueryParameters();
        assertEquals(p.size(), 1);
    }

    /**
     * Migrated Jersey 1.x {@code com.sun.jersey.impl.QueryParametersHttpRequestTest}.
     */
    @Test
    public void testGetQueryParametersInterspersedAmpersand() throws Exception {
        final UriInfo ui = createContext("/widgets/10?a=1&&b=2", "GET");
        MultivaluedMap<String, String> p = ui.getQueryParameters();
        assertEquals(p.size(), 2);
    }

    /**
     * Migrated Jersey 1.x {@code com.sun.jersey.impl.QueryParametersHttpRequestTest}.
     */
    @Test
    public void testGetQueryParametersEmptyValues() throws Exception {
        final UriInfo ui = createContext("/widgets/10?one&two&three", "GET");
        MultivaluedMap<String, String> p = ui.getQueryParameters();

        assertEquals(p.getFirst("one"), "");
        assertEquals(p.getFirst("two"), "");
        assertEquals(p.getFirst("three"), "");
    }

    /**
     * Migrated Jersey 1.x {@code com.sun.jersey.impl.QueryParametersHttpRequestTest}.
     */
    @Test
    public void testGetQueryParametersMultipleEmptyValues() throws Exception {
        final UriInfo ui = createContext("/widgets/10?one&one&one", "GET");
        MultivaluedMap<String, String> p = ui.getQueryParameters();

        assertEquals(p.get("one").size(), 3);
        assertEquals(p.get("one").get(0), "");
        assertEquals(p.get("one").get(1), "");
        assertEquals(p.get("one").get(2), "");
    }

    /**
     * Migrated Jersey 1.x {@code com.sun.jersey.impl.QueryParametersHttpRequestTest}.
     */
    @Test
    public void testGetQueryParametersWhiteSpace() throws Exception {
        final UriInfo ui = createContext("/widgets/10?x+=+1%20&%20y+=+2", "GET");
        MultivaluedMap<String, String> p = ui.getQueryParameters();

        assertEquals(" 1 ", p.getFirst("x "));
        assertEquals(" 2", p.getFirst(" y "));
    }

    /**
     * Migrated Jersey 1.x {@code com.sun.jersey.impl.QueryParametersHttpRequestTest}.
     */
    @Test
    public void testGetQueryParametersDecoded() throws Exception {
        UriInfo ui;
        MultivaluedMap<String, String> p;

        ui = createContext("/widgets/10?x+=+1%20&%20y+=+2", "GET");
        p = ui.getQueryParameters();
        assertEquals(" 1 ", p.getFirst("x "));
        assertEquals(" 2", p.getFirst(" y "));

        ui = createContext("/widgets/10?x=1&y=1+%2B+2", "GET");
        p = ui.getQueryParameters(true);
        assertEquals("1", p.getFirst("x"));
        assertEquals("1 + 2", p.getFirst("y"));

        ui = createContext("/widgets/10?x=1&y=1+%26+2", "GET");
        p = ui.getQueryParameters(true);
        assertEquals("1", p.getFirst("x"));
        assertEquals("1 & 2", p.getFirst("y"));

        ui = createContext("/widgets/10?x=1&y=1+%7C%7C+2", "GET");
        p = ui.getQueryParameters(true);
        assertEquals("1", p.getFirst("x"));
        assertEquals("1 || 2", p.getFirst("y"));
    }

    /**
     * Migrated Jersey 1.x {@code com.sun.jersey.impl.QueryParametersHttpRequestTest}.
     */
    @Test
    public void testGetQueryParametersEncoded() throws Exception {
        final UriInfo ui = createContext("/widgets/10?x+=+1%20&%20y+=+2", "GET");
        MultivaluedMap<String, String> p = ui.getQueryParameters(false);

        assertEquals("+1%20", p.getFirst("x "));
        assertEquals("+2", p.getFirst(" y "));
    }
}
