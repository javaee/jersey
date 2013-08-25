/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.junit.Test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests if the location response header is correctly adjusted to contain an absolute URI
 *
 * @author Adam Lindenthal (adam.lindenthal at oracle.com)
 */
public class LocationHeaderTest extends JerseyTest {
    private static final Logger LOGGER = Logger.getLogger(LocationHeaderTest.class.getName());

    @Override
    protected ResourceConfig configure() {
        enable(TestProperties.LOG_TRAFFIC);
        return new ResourceConfig(
                LocationHeaderTest.ResponseTest.class,
                LocationManipulationDynamicBinding.class
        );
    }

    /**
     * Test JAX-RS resource
     */
    @Path(value = "/ResponseTest")
    public static class ResponseTest {

        /**
         * Resource method for the basic uri conversion test
         * @return test response with relative location uri
         */
        @GET
        @Path("location")
        public Response locationTest() {
            UriBuilder uriBuilder = UriBuilder.fromResource(ResponseTest.class);
            URI uri = uriBuilder.build("-test-");
            LOGGER.info("URI Created in the resource method > " + uri);
            Response.ResponseBuilder responseBuilder = Response.created(uri);
            return responseBuilder.build();
        }

        /**
         * Resource method for the test with null location
         * @return test response with null location uri
         */
        @GET
        @Path("locationNull")
        public Response locationTestNull() {
            Response.ResponseBuilder responseBuilder = Response.created(null);
            return responseBuilder.build();
        }

        /**
         * Resource method for the test with uri rewritten in the filter
         * @return test response with relative location uri
         */
        @GET
        @Path("locationWithFilter")
        public Response locationTestWithFilter() {
            return locationTest();
        }

        /**
         * Resource method for the test with uri rewritten in the interceptor
         * @return test response with relative location uri and with body
         * (write interceptors are not triggered for entity-less responses)
         */
        @GET
        @Path("locationWithInterceptor")
        public Response locationTestWithInterceptor() {
            UriBuilder uriBuilder = UriBuilder.fromResource(ResponseTest.class);
            URI uri = uriBuilder.build("-test-");
            LOGGER.info("3> " + uri);
            Response.ResponseBuilder responseBuilder = Response.created(uri);
            responseBuilder = responseBuilder.entity("Return from locationWithBody").type("text/plain");
            return responseBuilder.build();
        }

        /**
         * Resource method for the test with entity containing response
         * @return test response with relative uri and with entity
         */
        @GET
        @Path("locationWithBody")
        @Produces("text/plain")
        public Response locationTestWithBody() {
            UriBuilder uriBuilder = UriBuilder.fromResource(ResponseTest.class);
            uriBuilder = uriBuilder.segment("{id}");
            URI uri = uriBuilder.build("-test-");
            Response.ResponseBuilder responseBuilder = Response.created(uri);
            responseBuilder = responseBuilder.entity("Return from locationWithBody").type("text/plain");
            return responseBuilder.build();
        }
    }

    /**
     * Basic test; resource methods returns relative uri, test expects uri to be absolute
     */
    @Test
    public void testConvertRelativeUriToAbsolute() {
        final Response response = target().path("ResponseTest/location").request(MediaType.TEXT_PLAIN).get(Response.class);
        String location = response.getHeaderString(HttpHeaders.LOCATION);
        LOGGER.info("Location resolved from response > " + location);
        assertTrue("Location header should be absolute URI", location.startsWith("http://localhost"));
    }

    /**
     * Test with entity; most of the HTTP 201 Created responses do not contain any body, just headers.
     * This test ensures, that the uri conversion works even in case when entity is present.
     */
    @Test
    public void testAbsoluteUriWithEntity() {
        final Response response = target().path("ResponseTest/locationWithBody").request().get(Response.class);
        String location = response.getHeaderString(HttpHeaders.LOCATION);
        LOGGER.info("Location resolved from response > " + location);
        assertTrue("Location header should be absolute URI", location.startsWith("http://localhost"));
        Object entity = response.getEntity();
        assertNotNull(entity);
    }

    /**
     * Test with URI Rewritten in the container response filter;
     * Filters do have access to the response headers and can manipulate the location uri so that it contains a relative address.
     * This test incorporates a filter which replaces the uri with a relative one. However we expect to have absolute uri at
     * the end of the chain.
     */
    @Test
    public void testAbsoluteUriWithFilter() {
        final Response response = target().path("ResponseTest/locationWithFilter")
                                          .request(MediaType.TEXT_PLAIN)
                                          .get(Response.class);
        String location = response.getHeaderString(HttpHeaders.LOCATION);
        LOGGER.info("Location resolved from response > " + location);
        assertTrue("Location header should be absolute URI", location.startsWith("http://localhost"));
        assertTrue("Location header value should be changed by the filter.", location.contains("UriChangedByFilter"));
    }

    /**
     * Test with URI Rewritten in the writer interceptor;
     * Interceptors do have access to the response headers and can manipulate the location uri so that it contains a relative
     * address.
     * This test incorporates an interceptor which replaces the uri with a relative one. However we expect to have absolute uri
     * at the end of the chain.
     */
    @Test
    public void testAbsoluteUriWithInterceptor() {
        final Response response = target().path("ResponseTest/locationWithInterceptor")
                                          .request(MediaType.TEXT_PLAIN)
                                          .get(Response.class);
        String location = response.getHeaderString(HttpHeaders.LOCATION);
        LOGGER.info("Location resolved from response > " + location);
        assertTrue("Location header should be absolute URI", location.startsWith("http://localhost"));
        assertTrue("Location header value should be changed by the interceptor.", location.contains("UriChangedByInterceptor"));
    }

    /**
     * Test with null location;
     * Ensures, that the null location is processed correctly.
     */
    @Test
    public void testNullLocation() {
        final Response response = target().path("ResponseTest/locationNull")
                .request(MediaType.TEXT_PLAIN)
                .get(Response.class);
        String location = response.getHeaderString(HttpHeaders.LOCATION);
        LOGGER.info("Location resolved from response > " + location);
        assertNull("Location header should be absolute URI", location);
    }

    /**
     * Response filter - replaces the Location header with a relative uri.
     */
    public static class LocationManipulationFilter implements ContainerResponseFilter {
        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
            MultivaluedMap<String, ? extends Object> headers = responseContext.getHeaders();
            List<URI> locations = (List<URI>) headers.get(HttpHeaders.LOCATION);
            locations.set(0, URI.create("ResponseTest/UriChangedByFilter"));
            LOGGER.info("LocationManipulationFilter applied.");
        }
    }

    /**
     * Writer interceptor - replaces the Location header with a relative uri.
     */
    public static class LocationManipulationInterceptor implements WriterInterceptor {
        @Override
        public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
            MultivaluedMap<String, ? extends Object> headers = context.getHeaders();
            List<URI> locations = (List<URI>) headers.get(HttpHeaders.LOCATION);
            locations.set(0, URI.create("ResponseTest/UriChangedByInterceptor"));
            LOGGER.info("LocationManipulationInterceptor applied.");
            context.proceed();
        }
    }

    /**
     * Registers the filter and interceptor and binds it to the resource methods of interest.
     */
    public static class LocationManipulationDynamicBinding implements DynamicFeature {
        @Override
        public void configure(ResourceInfo resourceInfo, FeatureContext context) {
            if (ResponseTest.class.equals(resourceInfo.getResourceClass())) {
                if (resourceInfo.getResourceMethod().getName().contains("locationTestWithFilter")) {
                    context.register(LocationManipulationFilter.class);
                    LOGGER.info("LocationManipulationFilter registered.");
                }
                if (resourceInfo.getResourceMethod().getName().contains("locationTestWithInterceptor")) {
                    context.register(LocationManipulationInterceptor.class);
                    LOGGER.info("LocationManipulationInterceptor registered.");
                }
            }
        }
    }
}



