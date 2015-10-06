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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Test if the location response header is resolved according to RFC7231 (in a JAX-RS 2.0 incompatible way) when
 * {@link ServerProperties#LOCATION_HEADER_RELATIVE_URI_RESOLUTION_RFC7231} property is set to {@code true} and
 * {@link ServerProperties#LOCATION_HEADER_RELATIVE_URI_RESOLUTION_DISABLED} to {@code false}
 *
 * @author Adam Lindenthal (adam.lindenthal at oracle.com)
 */
public class LocationHeaderWithIncompatibleFlagTest extends JerseyTest {

    private static final Logger LOGGER = Logger.getLogger(LocationHeaderWithIncompatibleFlagTest.class.getName());

    @Override
    protected ResourceConfig configure() {
        enable(TestProperties.LOG_TRAFFIC);
        final ResourceConfig rc = new ResourceConfig(ResponseTest.class);
        rc.property(ServerProperties.LOCATION_HEADER_RELATIVE_URI_RESOLUTION_RFC7231, Boolean.TRUE);
        return rc;
    }

    /**
     * Test JAX-RS resource
     */
    @Path(value = "test")
    public static class ResponseTest {
        /**
         * Resource method for the basic uri test
         * @return test response with relative location uri
         */
        @GET
        @Path("location")
        public Response locationTest() {
            final URI uri = URI.create("location");
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
         * Resource method for the test with location starting with single slash
         * @return test response with relative location uri starting with slash
         */
        @GET
        @Path("locationSlash")
        public Response locationTestSlash() {
            return Response.created(URI.create("/location")).build();
        }
    }

    /**
     * Test with relative location;
     * Ensures, that the location remains intact
     */
    @Test
    public void testLocation() {
        final Response response = target().path("test/location").request(MediaType.TEXT_PLAIN).get(Response.class);
        final String location = response.getHeaderString(HttpHeaders.LOCATION);
        LOGGER.info("Location resolved from response > " + location);
        assertEquals(getBaseUri().toString() + "test/location", location);
    }

    /**
     * Test with relative location with leading slash
     */
    @Test
    public void testLocationWithSlash() {
        final Response response = target().path("test/locationSlash").request(MediaType.TEXT_PLAIN).get(Response.class);
        final String location = response.getHeaderString(HttpHeaders.LOCATION);
        LOGGER.info("Location resolved from response > " + location);
        assertEquals(getBaseUri() + "location", location);
    }

    /**
     * Test with relative location with leading slash
     */
    @Test
    public void testNullLocation() {
        final Response response = target().path("test/locationNull").request(MediaType.TEXT_PLAIN).get(Response.class);
        final String location = response.getHeaderString(HttpHeaders.LOCATION);
        LOGGER.info("Location resolved from response > " + location);
        assertNull(location);
    }
}



