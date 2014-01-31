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
package org.glassfish.jersey.tests.e2e;

import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class ClientTest extends JerseyTest {

    @Path("helloworld")
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.TEXT_PLAIN)
    public static class HelloWorldResource {

        private static final String MESSAGE = "Hello world!";

        @GET
        public String getClichedMessage() {
            return MESSAGE;
        }
    }

    @Path("headers")
    @Produces(MediaType.TEXT_PLAIN)
    public static class HeadersTestResource {

        @POST
        @Path("content")
        public String contentHeaders(@HeaderParam("custom-header") final String customHeader,
                                     @Context final HttpHeaders headers, final String entity) {
            final StringBuilder sb = new StringBuilder(entity).append('\n');

            sb.append("custom-header:").append(customHeader).append('\n');

            for (final Map.Entry<String, List<String>> header : headers.getRequestHeaders().entrySet()) {
                sb.append(header.getKey()).append(':').append(header.getValue().toString()).append('\n');
            }

            return sb.toString();
        }
    }

    @Override
    protected ResourceConfig configure() {
        return new ResourceConfig(HelloWorldResource.class, HeadersTestResource.class);
    }

    @Test
    public void testAccesingHelloworldResource() {
        final WebTarget resource = target().path("helloworld");
        final Response r = resource.request().get();
        assertEquals(200, r.getStatus());

        final String responseMessage = resource.request().get(String.class);
        assertEquals(HelloWorldResource.MESSAGE, responseMessage);
    }

    @Test
    public void testAccesingMissingResource() {
        final WebTarget missingResource = target().path("missing");
        final Response r = missingResource.request().get();
        assertEquals(404, r.getStatus());


        try {
            missingResource.request().get(String.class);
        } catch (final WebApplicationException ex) {
            assertEquals(404, ex.getResponse().getStatus());
            return;
        }

        fail("Expected WebApplicationException has not been thrown.");
    }

    @Test
    // Inspired by JERSEY-1502
    public void testContextHeaders() {
        final WebTarget target = target().path("headers").path("content");

        Invocation.Builder ib;
        Invocation i;
        Response r;
        String reqHeaders;

        ib = target.request("*/*");
        ib.header("custom-header", "custom-value");
        ib.header("content-encoding", "deflate");
        i = ib.build("POST", Entity.entity("aaa", MediaType.WILDCARD_TYPE));
        r = i.invoke();

        reqHeaders = r.readEntity(String.class).toLowerCase();
        for (final String expected : new String[] {"custom-header:[custom-value]", "custom-header:custom-value"}) {
            assertTrue(String.format("Request headers do not contain expected '%s' entry:\n%s", expected, reqHeaders),
                    reqHeaders.contains(expected));
        }
        final String unexpected = "content-encoding";
        assertFalse(String.format("Request headers contains unexpected '%s' entry:\n%s", unexpected, reqHeaders),
                reqHeaders.contains(unexpected));

        ib = target.request("*/*");
        i = ib.build("POST",
                Entity.entity("aaa", Variant.mediaTypes(MediaType.WILDCARD_TYPE).encodings("deflate").build().get(0)));
        r = i.invoke();

        final String expected = "content-encoding:[deflate]";
        reqHeaders = r.readEntity(String.class).toLowerCase();
        assertTrue(String.format("Request headers do not contain expected '%s' entry:\n%s", expected, reqHeaders),
                reqHeaders.contains(expected));
    }
}
