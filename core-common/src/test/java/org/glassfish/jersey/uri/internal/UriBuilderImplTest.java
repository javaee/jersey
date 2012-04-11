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
package org.glassfish.jersey.uri.internal;

import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.core.UriBuilder;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class UriBuilderImplTest {

    public UriBuilderImplTest() {
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

    @Test
    public void testPathTemplateValueEncoding() throws URISyntaxException {
        String result;
        result = new UriBuilderImpl().uri(new URI("http://examples.jersey.java.net/")).userInfo("a/b").path("a/b").segment("a/b").build().toString();
        assertEquals("http://a%2Fb@examples.jersey.java.net/a/b/a%2Fb", result);

        result = new UriBuilderImpl().uri(new URI("http://examples.jersey.java.net/")).userInfo("{T1}").path("{T2}").segment("{T3}").build("a/b", "a/b", "a/b").toString();
        assertEquals("http://a%2Fb@examples.jersey.java.net/a/b/a/b", result);
        // TODO fix above to assertEquals("http://a%2Fb@examples.jersey.java.net/a%2Fb/a%2Fb", result);

        result = new UriBuilderImpl().uri(new URI("http://examples.jersey.java.net/")).userInfo("{T1}").path("{T2}").segment("{T2}").build("a@b", "a@b").toString();
        assertEquals("http://a%40b@examples.jersey.java.net/a@b/a@b", result);

        result = new UriBuilderImpl().uri(new URI("http://examples.jersey.java.net/")).userInfo("{T}").path("{T}").segment("{T}").build("a@b").toString();
        assertEquals("http://a%40b@examples.jersey.java.net/a%40b/a%40b", result);
        // TODO fix above to assertEquals("http://a%40b@examples.jersey.java.net/a@b/a@b", result);
    }

    @Test
    public void testReplaceMatrixParamWithNull() {
        UriBuilder builder = new UriBuilderImpl().matrixParam("matrix", "param1", "param2");
        builder.replaceMatrixParam("matrix", (Object[]) null);
        assertEquals(builder.build().toString(), "");
    }

    @Test
    public void testReplaceNullMatrixParam() {
        try {
            new UriBuilderImpl().replaceMatrixParam(null, "param");
        } catch (IllegalArgumentException e) {
            return;
        } catch (Exception e) {
            fail("Expected IllegalArgumentException but got " + e.toString());
        }
        fail("Expected IllegalArgumentException but no exception was thrown.");
    }

    // regression test for JERSEY-1081
    @Test
    public void testReplaceQueryParam() {
        URI uri = new UriBuilderImpl().path("http://localhost/").replaceQueryParam("foo", "test").build();
        assertEquals("http://localhost/?foo=test", uri.toString());
    }

    // regression test for JERSEY-1081
    @Test
    public void testReplaceQueryParamAndClone() {
        URI uri = new UriBuilderImpl().path("http://localhost/").replaceQueryParam("foo", "test").clone().build();
        assertEquals("http://localhost/?foo=test", uri.toString());
    }
}
