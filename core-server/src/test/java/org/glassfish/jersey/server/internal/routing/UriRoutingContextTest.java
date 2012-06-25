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
package org.glassfish.jersey.server.internal.routing;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;

import org.glassfish.jersey.internal.util.collection.Refs;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.RequestContextBuilder;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 *
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
        return new UriRoutingContext(
                Refs.<ContainerRequest>of(RequestContextBuilder.from(requestUri, method).build()));
    }

    private UriRoutingContext createContext(String appBaseUri, String requestUri, String method) {
        return new UriRoutingContext(
                Refs.<ContainerRequest>of(RequestContextBuilder.from(appBaseUri, requestUri, method).build()));
    }

    @Test
    public void testGetAbsolutePath() throws URISyntaxException {
        assertEquals(URI.create("http://example.org/app/resource"),
                createContext("http://example.org/app/resource?foo1=bar1&foo2=bar2", "GET").getAbsolutePath());
    }

    @Test
    public void testGetPath() throws URISyntaxException {
        assertEquals("/my app/resource",
                createContext("http://example.org/my%20app/resource?foo1=bar1&foo2=bar2", "GET").getAbsolutePath().getPath());
    }

    @Test
    public void testGetDecodedPath() throws URISyntaxException {
        UriRoutingContext ctx = createContext("http://example.org/my%20app/resource?foo1=bar1&foo2=bar2", "GET");
        assertEquals("/my%20app/resource", ctx.getPath(false));
        assertEquals("/my app/resource", ctx.getPath(true));
    }

    @Test
    public void testGetPathBuilder() throws URISyntaxException {
        UriRoutingContext ctx = createContext("http://example.org/my%20app/resource?foo1=bar1&foo2=bar2", "GET");
        assertEquals(URI.create("http://example.org/my%20app/resource"), ctx.getAbsolutePathBuilder().build());
    }

    @Test
    public void testGetPathSegments() throws URISyntaxException {
        List<PathSegment> lps =
                createContext("http://example.org/app/", "http://example.org/app/my%20resource/my%20subresource", "GET").getPathSegments();
        assertEquals(2, lps.size());
        assertEquals("my resource", lps.get(0).getPath());
        assertEquals("my subresource", lps.get(1).getPath());
    }

    @Test
    public void testGetPathSegments2() throws URISyntaxException {
        List<PathSegment> lps =
                createContext("http://example.org/app/", "http://example.org/app/my%20resource/my%20subresource", "GET").getPathSegments(false);
        assertEquals(2, lps.size());
        assertEquals("my%20resource", lps.get(0).getPath());
        assertEquals("my%20subresource", lps.get(1).getPath());
    }

    @Test
    public void testQueryParams() throws URISyntaxException {
        MultivaluedMap<String, String> map =
                createContext("http://example.org/app/resource?foo1=bar1&foo2=bar2", "GET").getQueryParameters();
        assertEquals("bar1", map.getFirst("foo1"));
        assertEquals("bar2", map.getFirst("foo2"));
    }

    @Test
    public void testQueryParamsDecoded() throws URISyntaxException {
        MultivaluedMap<String, String> map =
                createContext("http://example.org/app/resource?foo1=%7Bbar1%7D&foo2=%7Bbar2%7D", "GET").getQueryParameters(true);
        assertEquals("{bar1}", map.getFirst("foo1"));
        assertEquals("{bar2}", map.getFirst("foo2"));
    }
}
