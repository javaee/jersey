/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotAllowedException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.NotSupportedException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.RedirectionException;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.ClientResponse;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Test for WebApplicationException handling on both server and client side.
 *
 * @author Paul Sandoz (paul.sandoz at oracle.com)
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public class ExceptionTest extends JerseyTest {

    @Override
    protected Application configure() {
        return new ResourceConfig(ExceptionDrivenResource.class, ResponseDrivenResource.class);
    }

    @Override
    protected void configureClient(ClientConfig clientConfig) {
        clientConfig.setProperty(ClientProperties.FOLLOW_REDIRECTS, false);
    }

    static final URI testUri = UriBuilder.fromUri("http://jersey.java.net").build();

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    static Map<String, WebApplicationException> ExceptionMAP = new HashMap<String, WebApplicationException>() {{
        put("301", new RedirectionException(Response.Status.MOVED_PERMANENTLY, testUri));
        put("302", new RedirectionException(Response.Status.FOUND, testUri));
        put("303", new RedirectionException(Response.Status.SEE_OTHER, testUri));
        put("307", new RedirectionException(Response.Status.TEMPORARY_REDIRECT, testUri));
        put("400", new BadRequestException());
        put("401", new NotAuthorizedException("challenge"));
        put("402", new ClientErrorException(402));
        put("404", new NotFoundException());
        put("405", new NotAllowedException("OPTIONS"));
        put("406", new NotAcceptableException());
        put("415", new NotSupportedException());
        put("500", new InternalServerErrorException());
        put("501", new ServerErrorException(501));
        put("503", new ServiceUnavailableException());
    }};

    static Map<String, Response> ResponseMAP = new HashMap<String, Response>() {{
        put("301", Response.status(301).location(testUri).build());
        put("302", Response.status(302).location(testUri).build());
        put("303", Response.seeOther(testUri).build());
        put("307", Response.temporaryRedirect(testUri).build());
        put("400", Response.status(400).build());
        put("401", Response.status(401).build());
        put("402", Response.status(402).build());
        put("404", Response.status(404).build());
        put("405", Response.status(405).allow("OPTIONS").build());
        put("406", Response.status(406).build());
        put("415", Response.status(415).build());
        put("500", Response.serverError().build());
        put("501", Response.status(501).build());
        put("503", Response.status(503).build());
    }};

    @Path("exceptionDriven")
    public static class ExceptionDrivenResource {

        @GET
        @Path("{status}")
        public String get(@PathParam("status") String status) {
            throw ExceptionMAP.get(status);
        }
    }

    @Path("responseDriven")
    public static class ResponseDrivenResource {

        @GET
        @Path("{status}")
        public Response get(@PathParam("status") String status) {
            return ResponseMAP.get(status);
        }
    }

    private void _testStatusCode(final String status) {
        _testStatusCodeViaException("exceptionDriven", status);
        _testStatusCodeDirectly("exceptionDriven", status);
        _testStatusCodeViaException("responseDriven", status);
        _testStatusCodeDirectly("responseDriven", status);
    }

    private void _testStatusCodeViaException(final String prefix, final String status) {

        final int statusCode = Integer.parseInt(status);

        try {

            target().path(prefix).path(status).request().get(ClientResponse.class);
            fail("An exception expected");
        } catch (WebApplicationException ex) {
            //noinspection ThrowableResultOfMethodCallIgnored
            assertEquals(ExceptionMAP.get(status).getClass(), ex.getClass());

            final Response response = ex.getResponse();
            assertEquals(statusCode, response.getStatus());

            if (is3xxCode(statusCode)) {
                assertNotNull(response.getLocation());
            }
        }
    }

    private void _testStatusCodeDirectly(final String prefix, final String status) {
        final int statusCode = Integer.parseInt(status);
        final Response response = target().path(prefix).path(status).request().get();
        assertEquals(statusCode, response.getStatus());
        if (is3xxCode(statusCode)) {
            assertNotNull(response.getLocation());
        }
}


    @Test
    public void testAllStatusCodes() {
        for (String status : ExceptionMAP.keySet()) {
            _testStatusCode(status);
        }
    }

    private boolean is3xxCode(final int statusCode) {
        return 299 < statusCode && statusCode < 400;
    }
}
