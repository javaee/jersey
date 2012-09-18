/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.client;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.ClientFactory;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Martin Matula (martin.matula at oracle.com)
 */
public class JerseyWebTargetTest {
    private JerseyClient client;
    private JerseyWebTarget target;

    @Before
    public void setUp() {
        this.client = (JerseyClient) ClientFactory.newClient();
        this.target = client.target("/");
    }

    @Test
    public void testClose() {
        client.close();
        try {
            target.getUriBuilder();
            fail("IllegalStateException was expected.");
        } catch (IllegalStateException e) {
            // ignore
        }
        try {
            target.configuration();
            fail("IllegalStateException was expected.");
        } catch (IllegalStateException e) {
            // ignore
        }
    }

    @Test
    public void testPathParams() {
        URI uri;
        UriBuilder uriBuilder;

        uri = target.pathParam("a", "v").pathParam("a", null).getUri();
        assertEquals("/", uri.toString());

        uri = target.path("{a}").pathParam("a", "v").getUri();
        assertEquals("/v", uri.toString());

        uriBuilder = target.path("{a}").pathParam("a", "v").pathParam("a", null).getUriBuilder();
        assertEquals("/%7Ba%7D", uriBuilder.build().toString());

        final Map<String, Object> params = new HashMap<String, Object>(2);
        params.put("a", "w1");
        params.put("b", null);
        try {
            target.path("{a}").path("{b}").pathParam("a", "v1").pathParam("b", "v2")
                    .pathParams(params).getUri();
            fail("IllegalStateException expected - template variable 'b' should have no value.");
        } catch (IllegalStateException ex) {
            // expected
        }

        try {
            target.pathParams(null);
            fail("NullPointerException expected.");
        } catch (NullPointerException ex) {
            // expected
        }
    }

    @Test
    @Ignore("Un-ignore once JERSEY-1329 is fixed.")
    // TODO un-ignore
    public void testGetUriBuilder() {
        final Map<String, Object> params = new HashMap<String, Object>(2);
        params.put("a", "w1");
        params.put("b", null);
        UriBuilder uriBuilder = target.path("{a}").path("{b}").pathParam("a", "v1").pathParam("b", "v2")
                .pathParams(params).getUriBuilder();
        assertEquals("/w1/%7Bb%7D", uriBuilder.build().toString());
    }

    @Test
    public void testQueryParams() {
        URI uri;

        uri = target.path("a").queryParam("q", "v1", "v2").queryParam("q").getUri();
        assertEquals("/a", uri.toString());

        uri = target.path("a").queryParam("q", "v1", "v2").queryParam("q", (Object) null).getUri();
        assertEquals("/a", uri.toString());

        uri = target.path("a").queryParam("q", "v1", "v2").queryParam("q", (Object[]) null).getUri();
        assertEquals("/a", uri.toString());

        uri = target.path("a").queryParam("q", "v1", "v2").queryParam("q", new Object[]{}).getUri();
        assertEquals("/a", uri.toString());

        uri = target.path("a").queryParam("q", "v").getUri();
        assertEquals("/a?q=v", uri.toString());

        uri = target.path("a").queryParam("q1", "v1").queryParam("q2", "v2").queryParam("q1", (Object) null).getUri();
        assertEquals("/a?q2=v2", uri.toString());

        try {
            target.queryParam("q", "v1", null, "v2", null);
            fail("NullPointerException expected.");
        } catch (NullPointerException ex) {
            // expected
        }

        {
            final MultivaluedMap<String, Object> params = new MultivaluedHashMap<String, Object>(2);
            params.add("q1", "w1");
            params.add("q1", "w2");
            params.add("q2", null);
            uri = target.path("a").queryParam("q1", "v1").queryParam("q2", "v2").queryParams(params).getUri();
            assertEquals("/a?q1=v1&q1=w1&q1=w2", uri.toString());
        }

        try {
            target.queryParams(null);
            fail("NullPointerException expected.");
        } catch (NullPointerException ex) {
            // expected
        }

        try {
            // null-values supporting multivalued hash map
            final MultivaluedMap<String, Object> params = new MultivaluedHashMap<String, Object>(2) {
                @Override
                protected void addNull(List<Object> values) {
                    values.add(null);
                }

                @Override
                protected void addFirstNull(List<Object> values) {
                    values.add(null);
                }
            };
            params.add("q1", "w1");
            params.add("q1", null);
            params.add("q2", null);
            target.path("a").queryParam("q1", "v1").queryParam("q2", "v2").queryParams(params);
            fail("NullPointerException expected.");
        } catch (NullPointerException ex) {
            // expected
        }
    }

    @Test
    public void testMatrixParams() {
        URI uri;

        uri = target.path("a").matrixParam("q", "v1", "v2").matrixParam("q").getUri();
        assertEquals("/a", uri.toString());

        uri = target.path("a").matrixParam("q", "v1", "v2").matrixParam("q", (Object) null).getUri();
        assertEquals("/a", uri.toString());

        uri = target.path("a").matrixParam("q", "v1", "v2").matrixParam("q", (Object[]) null).getUri();
        assertEquals("/a", uri.toString());

        uri = target.path("a").matrixParam("q", "v1", "v2").matrixParam("q", new Object[]{}).getUri();
        assertEquals("/a", uri.toString());

        uri = target.path("a").matrixParam("q", "v").getUri();
        assertEquals("/a;q=v", uri.toString());

        uri = target.path("a").matrixParam("q1", "v1").matrixParam("q2", "v2").matrixParam("q1", (Object) null).getUri();
        assertEquals("/a;q2=v2", uri.toString());

        try {
            target.matrixParam("q", "v1", null, "v2", null);
            fail("NullPointerException expected.");
        } catch (NullPointerException ex) {
            // expected
        }
    }
}
