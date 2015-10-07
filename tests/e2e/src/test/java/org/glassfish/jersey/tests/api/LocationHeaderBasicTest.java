/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.tests.api;

import java.net.URI;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.glassfish.jersey.test.util.runner.ConcurrentRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test if the location relative URI is correctly resolved within basic cases.
 *
 * @author Adam Lindenthal (adam.lindenthal at oracle.com)
 */
@RunWith(ConcurrentRunner.class)
public class LocationHeaderBasicTest extends JerseyTest {

    private static final Logger LOGGER = Logger.getLogger(LocationHeaderBasicTest.class.getName());

    @Override
    protected ResourceConfig configure() {
        enable(TestProperties.LOG_TRAFFIC);
        return new ResourceConfig(ResponseTest.class);
    }

    @Override
    protected void configureClient(final ClientConfig config) {
        super.configureClient(config);
        config.property(ClientProperties.FOLLOW_REDIRECTS, false);
    }

    /**
     * Test JAX-RS resource
     */
    @Path(value = "/ResponseTest")
    public static class ResponseTest {

        /* injected request URI for assertions in the resource methods */
        @Context
        private UriInfo uriInfo;

        /**
         * Resource method for the basic uri conversion test
         * @return test response with relative location uri
         */
        @GET
        @Path("location")
        public Response locationTest() {
            final URI uri = URI.create("location");
            LOGGER.info("URI Created in the resource method > " + uri);
            return Response.created(uri).build();
        }

        /**
         * Resource method for the test with null location
         * @return test response with null location uri
         */
        @GET
        @Path("locationNull")
        public Response locationTestNull() {
            return Response.created(null).build();
        }

        /**
         * Resource method for the test with entity containing response
         * @return test response with relative uri and with entity
         */
        @GET
        @Path("locationWithBody")
        @Produces("text/plain")
        public Response locationTestWithBody() {
            final URI uri = URI.create("locationWithBody");
            return Response.created(uri).entity("Return from locationWithBody").type("text/plain").build();
        }

        /**
         * Resource method for direct test - location header is checked immediately after calling Response.created() and
         * the result is returned as a boolean response instead of returning the ({@link Response}) type and checking the
         * header in the calling test method. This isolates the influence of absolutization routine performed in the
         * ({@link org.glassfish.jersey.server.ServerRuntime} before closing the stream.
         *
         * @return true if URI is absolutized correctly, false if the URI remains relative (or does not match the expected one).
         */
        @GET
        @Path("locationDirect")
        @Produces("text/plain")
        public Boolean locationDirectTest() {
            final URI uri = getUriBuilder().segment("locationDirect").build();
            final Response response = Response.created(uri).build();
            return response.getLocation().equals(uriInfo.getAbsolutePath());
        }




        /**
         * Resource method for testing correct baseUri and request overwrite in the prematching filter.
         * Should never be called by the test, as {@link ResponseTest#redirectedUri()} should be called instead.
         */
        @GET
        @Path("filterChangedBaseUri")
        public Response locationWithChangedBaseUri() {
            fail("Method should not expected to be called, as prematching filter should have changed the request uri.");
            return Response.created(URI.create("new")).build();
        }

        /**
         * Not called by the test directly, but after prematching filter redirect from
         * {@link ResponseTest#locationWithChangedBaseUri()}.
         *
         * @return {@code 201 Created} response with location resolved against new baseUri.
         */
        @GET
        @Path("newUri")
        public Response redirectedUri() {
            return Response.created(URI.create("newRedirected")).build();
        }

        /**
         * Resource method for testing relative URI resolution in case of {@code seeOther} response.
         * @return {@code 303 See Other} response with relative URI
         */
        @POST
        @Path("seeOther")
        @Consumes("text/plain")
        public Response seeOther() {
            return Response.seeOther(URI.create("other")).build();
        }

        /**
         * Resource method for testing relative URI resolution in case of {@code seeOther} response.
         * @return {@code 303 See Other} response with relative URI
         */
        @GET
        @Path("seeOtherLeading")
        public Response seeOtherWithLeadingSlash() {
            return Response.seeOther(URI.create("/other")).build();
        }

        /**
         * Resource method for testing relative URI resolution in case of {@code seeOther} response.
         * @return {@code 303 See Other} response with relative URI
         */
        @GET
        @Path("seeOtherTrailing")
        public Response seeOtherWithTrailingSlash() {
            return Response.seeOther(URI.create("other/")).build();
        }

        /**
         * Resource method for testing relative URI resolution in case of {@code temporaryRedirect} response.
         * @return {@code 307 Temporary Redirect} response with relative URI
         */
        @GET
        @Path("temporaryRedirect")
        public Response temporaryRedirect() {
            return Response.temporaryRedirect(URI.create("redirect")).build();
        }

        /**
         * Resource method for testing relative URI resolution in case of {@code temporaryRedirect} response.
         * @return {@code 307 Temporary Redirect} response with relative URI
         */
        @GET
        @Path("temporaryRedirectLeading")
        public Response temporaryRedirectWithLeadingSlash() {
            return Response.temporaryRedirect(URI.create("/redirect")).build();
        }

        /**
         * Resource method for testing relative URI resolution in case of {@code temporaryRedirect} response.
         * @return {@code 307 Temporary Redirect} response with relative URI
         */
        @GET
        @Path("temporaryRedirectTrailing")
        public Response temporaryRedirectWithTrailingSlash() {
            return Response.temporaryRedirect(URI.create("redirect/")).build();
        }

        /** Return UriBuilder with base pre-set {@code /ResponseTest} uri segment for this resource.
         *
         * @return UriBuilder
         */
        private UriBuilder getUriBuilder() {
            return UriBuilder.fromResource(ResponseTest.class);
        }
    }

    /**
     * Basic test; resource methods returns relative uri, test expects uri to be absolute
     */
    @Test
    public void testConvertRelativeUriToAbsolute() {
        checkResource("ResponseTest/location", "location");
        // checkResource("ResponseTest/location");
    }

    /**
     * Test with entity; most of the HTTP 201 Created responses do not contain any body, just headers.
     * This test ensures, that the uri conversion works even in case when entity is present.
     */
    @Test
    public void testAbsoluteUriWithEntity() {
        final Response response = checkResource("ResponseTest/locationWithBody", "locationWithBody");
        assertNotNull(response.getEntity());
    }


    /**
     * Test with null location;
     * Ensures, that the null location is processed correctly.
     */
    @Test
    public void testNullLocation() {
        final Response response = target().path("ResponseTest/locationNull").request(MediaType.TEXT_PLAIN).get(Response.class);
        final String location = response.getHeaderString(HttpHeaders.LOCATION);
        LOGGER.info("Location resolved from response > " + location);
        assertNull("Location header should be absolute URI", location);
    }

    /**
     * Tests if the URI is absolutized in the Response directly after Response.Builder.created() is called
     */
    @Test
    public void testConversionDirectly() {
        final Boolean result = target().path("ResponseTest/locationDirect").request(MediaType.TEXT_PLAIN).get(Boolean.class);
        assertTrue(result);
    }

    @Test
    public void testSeeOther() {
        Response response = target().path("ResponseTest/seeOther").request()
                .post(Entity.entity("TEXT", MediaType.TEXT_PLAIN_TYPE));
        String location = response.getHeaderString(HttpHeaders.LOCATION);
        assertEquals(getBaseUri().toString() + "other", location);

        response = target().path("ResponseTest/seeOtherLeading").request(MediaType.TEXT_PLAIN).get(Response.class);
        location = response.getHeaderString(HttpHeaders.LOCATION);
        assertEquals(getBaseUri().toString() + "other", location);

        response = target().path("ResponseTest/seeOtherTrailing").request(MediaType.TEXT_PLAIN).get(Response.class);
        location = response.getHeaderString(HttpHeaders.LOCATION);
        assertEquals(getBaseUri().toString() + "other/", location);
    }

    @Test
    public void testTemporaryRedirect() {
        Response response = target().path("ResponseTest/temporaryRedirect").request(MediaType.TEXT_PLAIN).get(Response.class);
        String location = response.getHeaderString(HttpHeaders.LOCATION);
        assertEquals(getBaseUri().toString() + "redirect", location);

        response = target().path("ResponseTest/temporaryRedirectLeading").request(MediaType.TEXT_PLAIN).get(Response.class);
        location = response.getHeaderString(HttpHeaders.LOCATION);
        assertEquals(getBaseUri().toString() + "redirect", location);

        response = target().path("ResponseTest/temporaryRedirectTrailing").request(MediaType.TEXT_PLAIN).get(Response.class);
        location = response.getHeaderString(HttpHeaders.LOCATION);
        assertEquals(getBaseUri().toString() + "redirect/", location);
    }

    private Response checkResource(final String resourcePath, final String expectedRelativeUri) {
        final Response response = target().path(resourcePath).request(MediaType.TEXT_PLAIN).get(Response.class);
        final String location = response.getHeaderString(HttpHeaders.LOCATION);
        LOGGER.info("Location resolved from response > " + location);
        assertEquals(getBaseUri() + expectedRelativeUri, location);
        return response;
    }
}



