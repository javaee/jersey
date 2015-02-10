/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.e2e.common;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.client.Entity;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests HeaderValueException.
 *
 * @author Miroslav Fuksa
 */
public class HeaderValueExceptionTest extends JerseyTest {

    @Override
    protected Application configure() {
        return new ResourceConfig(ResponseFilter.class, TestResource.class);
    }

    @Test
    public void testInboundHeaderThrowsException() throws ExecutionException, InterruptedException {
        final Response response = target("resource/inbound").request()
                .header(HttpHeaders.DATE, "foo")
                .post(Entity.entity("inbound", MediaType.TEXT_PLAIN_TYPE));
        Assert.assertEquals(400, response.getStatus());
    }

    @Test
    public void testOutboundHeaderThrowsException() throws ExecutionException, InterruptedException {
        final Response response = target("resource/outbound").request()
                .post(Entity.entity("outbound", MediaType.TEXT_PLAIN_TYPE));
        Assert.assertEquals(500, response.getStatus());
    }

    @Test
    public void testOutboundResponseHeaderThrowsException() throws ExecutionException, InterruptedException {
        final Response response = target("resource/outbound-Response").request()
                .post(Entity.entity("outbound", MediaType.TEXT_PLAIN_TYPE));
        Assert.assertEquals(500, response.getStatus());
    }


    public static class ResponseFilter implements ContainerResponseFilter {

        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
            // this call should throw HeaderValueException which will be converted to HTTP 500 response
            responseContext.getDate();
        }
    }

    @Path("resource")
    public static class TestResource {
        @POST
        @Path("inbound")
        public String postInbound(String entity, @Context HttpHeaders headers) {
            // this call should throw HeaderValueException which will be converted to HTTP 400 response
            headers.getDate();
            return entity;
        }

        @POST
        @Path("outbound")
        public Response postOutbound(String entity) {
            return Response.ok().entity(entity).header(HttpHeaders.DATE, "bar").build();
        }

        @POST
        @Path("outbound-Response")
        public Response postOutboundResponse(String entity) {
            final Response response = Response.ok(entity).header(HttpHeaders.DATE, "foo").build();
            // this call should throw HeaderValueException which will be converted to HTTP 500 response
            response.getDate();
            return response;
        }
    }
}
