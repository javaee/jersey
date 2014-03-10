/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2014 Oracle and/or its affiliates. All rights reserved.
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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Link;
import javax.ws.rs.ext.RuntimeDelegate;

import org.glassfish.jersey.internal.TestRuntimeDelegate;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Tests for LinkTest class.
 *
 * @author Santiago Pericas-Geertsen (Santiago.PericasGeertsen at oracle.com)
 */
public class JerseyLinkTest {

    @Path("/myresource")
    static class MyResource {

        @GET
        @Produces("text/plain")
        public String self() {
            return "myself";
        }

        @GET
        @Produces("application/xml")
        public String notSelf() {
            return "<xml>notSelf</xml>";
        }
    }

    @Before
    public void setUp() throws Exception {
        RuntimeDelegate.setInstance(new TestRuntimeDelegate());
    }

    @After
    public void tearDown() throws Exception {
        RuntimeDelegate.setInstance(null);
    }

    @Test
    public void testGetUri() {
        URI u = URI.create("http://example.org/app/link1");
        Link l1 = Link.fromUri("http://example.org/app/link1").param("foo1", "bar1").param("foo2", "bar2").build();
        assertEquals(l1.getUri(), u);
        assertEquals(l1.getUriBuilder().build(), u);
    }

    @Test
    public void testToString() {
        Link link = Link.fromUri("http://example.org/app/link1").rel("self").build();
        assertEquals("<http://example.org/app/link1>; rel=\"self\"", link.toString());
    }

    @Test
    public void testGetters() {
        Link link = Link.fromUri("http://example.org/app/link1").rel("self").type("text/plain").build();
        assertEquals(URI.create("http://example.org/app/link1"), link.getUri());
        assertEquals("self", link.getRel());
        assertEquals(null, link.getTitle());
        assertEquals("text/plain", link.getType());
        assertEquals(2, link.getParams().size());
    }

    /**
     * Regression test for JERSEY-1378 fix.
     */
    @Test
    public void buildRelativeLinkTest() {
        assertEquals(URI.create("aa%20bb"), Link.fromUri("aa bb").build().getUri());
    }

    /**
     * Reproducer for JERSEY-2387: IAE expected on unresolved URI template parameters.
     */
    @Test
    public void testLinkBuilderWithUnresolvedTemplates() {
        Link.Builder linkBuilder;
        try {
            linkBuilder = Link.fromUri("scheme://authority/{x1}/{x2}/{x3}");
            linkBuilder.build("p");
            fail("IllegalArgumentException is expected to be thrown from Link.Builder when there are unresolved templates.");
        } catch (IllegalArgumentException expected) {
            // exception expected, move on...
        }
        try {
            linkBuilder = Link.fromUri("scheme://authority/{x1}/{x2}/{x3}");
            linkBuilder.build();
            fail("IllegalArgumentException is expected to be thrown from Link.Builder when there are unresolved templates.");
        } catch (IllegalArgumentException expected) {
            // exception expected, move on...
        }
    }
}
