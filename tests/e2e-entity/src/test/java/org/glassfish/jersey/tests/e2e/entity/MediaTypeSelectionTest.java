/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2017 Oracle and/or its affiliates. All rights reserved.
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

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * @author Martin Matula
 */
public class MediaTypeSelectionTest extends AbstractTypeTester {
    @Path("form")
    public static class FormResource {
        @POST
        public Response post(MultivaluedMap<String, String> data) {
            return Response.ok(data, MediaType.APPLICATION_FORM_URLENCODED_TYPE).build();
        }
    }

    @Path("foo")
    public static class FooResource {
        @POST
        @Consumes("foo/*")
        @Produces("foo/*")
        public String foo(String foo) {
            return foo;
        }
    }

    @Path("text")
    public static class TextResource {
        @GET
        @Produces("text/*")
        public String getText() {
            return "text";
        }

        @GET
        @Produces("application/*")
        @Path("any")
        public String getAny() {
            return "text";
        }

        @POST
        @Produces("text/*")
        public Response post(String entity) {
            return Response.ok().entity("entity").build();
        }
    }

    @Path("wildcard")
    public static class WildCardResource {
        @POST
        public String wildCard(String wc) {
            return wc;
        }
    }

    @Path("jira/1518")
    public static class Issue1518Resource {
        @POST
        @Consumes("text/plain;qs=0.7")
        public String never() {
            throw new WebApplicationException(Response.Status.CONFLICT);
        }

        @POST
        @Consumes("text/*")
        public String text() {
            return "1518";
        }
    }

    // JERSEY-1518 reproducer test
    @Test
    public void testQsInConsumes() {
        Response r = target("jira/1518").request(MediaType.TEXT_PLAIN_TYPE).post(Entity.text("request"));
        assertEquals(200, r.getStatus());
        assertEquals(MediaType.TEXT_PLAIN_TYPE, r.getMediaType());
        assertEquals("1518", r.readEntity(String.class));
    }

    // JERSEY-1187 regression test
    @Test
    public void testExplicitMediaType() {
        Response r = target("form").request().post(Entity.form(new Form().param("a", "b")));
        assertEquals(MediaType.APPLICATION_FORM_URLENCODED_TYPE, r.getMediaType());
        assertEquals("b", r.readEntity(Form.class).asMap().getFirst("a"));
    }

    @Test
    public void testAmbiguousWildcard() {
        Response r = target("foo").request().post(Entity.entity("test", "foo/plain"));
        assertEquals(406, r.getStatus());
    }

    @Test
    public void testWildcardInSubType() {
        Response r = target("text").request("text/*").get();
        assertEquals(406, r.getStatus());
    }

    @Test
    public void testWildcardInSubTypePost() {
        Response r = target("text").request("text/*").post(Entity.entity("test", MediaType.TEXT_PLAIN_TYPE));
        assertEquals(406, r.getStatus());
    }

    @Test
    public void testWildcardInSubType2() {
        Response r = target("text").request("*/*").get();
        assertEquals(406, r.getStatus());
    }

    @Test
    public void testWildcardsInTypeAndSubType() {
        Response r = target("text/any").request("*/*").get();
        assertEquals(200, r.getStatus());
        assertEquals(MediaType.APPLICATION_OCTET_STREAM_TYPE, r.getMediaType());
    }

    @Test
    public void testNoAcceptHeader() {
        // This test is testing the situation when the client sends no Accept header to the server and it expects
        // APPLICATION_OCTET_STREAM_TYPE to be returned. But when no Accept header is defined by the client api the
        // HttpURLConnection (in HttpUrlConnector)  always put there some default Accept header (like */*, text/plain, ...).
        // To overwrite this behaviour we set Accept to empty String. This works fine as the server code handles empty
        // Accept header like no Accept header.
        final MultivaluedHashMap headers = new MultivaluedHashMap();
        headers.add("Accept", "");

        Response r = target("text/any").request().headers(headers).get();
        assertEquals(200, r.getStatus());
        assertEquals(MediaType.APPLICATION_OCTET_STREAM_TYPE, r.getMediaType());
    }

    @Test
    public void testSpecific() {
        Response r = target("foo").request("foo/plain").post(Entity.entity("test", "foo/plain"));
        assertEquals(MediaType.valueOf("foo/plain"), r.getMediaType());
        assertEquals("test", r.readEntity(String.class));
    }

    @Test
    public void testApplicationWildCard() {
        Response r = target("wildcard").request("application/*").post(Entity.text("test"));
        assertEquals(MediaType.APPLICATION_OCTET_STREAM_TYPE, r.getMediaType());
        assertEquals("test", r.readEntity(String.class));
    }
}
