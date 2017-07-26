/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package org.glassfish.jersey.examples.exception;

import java.io.IOException;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.glassfish.jersey.examples.exception.Exceptions.MyException;
import org.glassfish.jersey.examples.exception.Exceptions.MySubException;
import org.glassfish.jersey.examples.exception.Exceptions.MySubSubException;
import org.glassfish.jersey.server.ContainerRequest;

/**
 * ExceptionResource class.
 *
 * @author Santiago.PericasGeertsen at oracle.com
 */
@Path("exception")
@Consumes("text/plain")
@Produces("text/plain")
public class ExceptionResource {

    @Provider
    static class MyResponseFilter implements ContainerResponseFilter {

        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
            System.out.println("MyResponseFilter.postFilter() enter");
            responseContext.setEntity(
                    responseContext.getEntity() + ":" + getClass().getSimpleName(), null, MediaType.TEXT_PLAIN_TYPE);
            System.out.println("MyResponseFilter.postFilter() exit");
        }
    }

    @Provider
    static class WebApplicationExceptionFilter implements ContainerRequestFilter, ContainerResponseFilter {

        @Override
        public void filter(ContainerRequestContext context) throws IOException {
            System.out.println("WebApplicationExceptionFilter.preFilter() enter");

            String path = ((ContainerRequest) context).getRequestUri().getPath();
            if (path.endsWith("request_exception") && context.hasEntity() && ((ContainerRequest) context)
                    .readEntity(String.class).equals("Request Exception")) {
                throw new WebApplicationException(Response.Status.OK);
            }
            System.out.println("WebApplicationExceptionFilter.preFilter() exit");
        }

        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
            System.out.println("WebApplicationExceptionFilter.postFilter() enter");
            if (responseContext.hasEntity() && responseContext.getEntity().equals("Response Exception")) {
                throw new WebApplicationException(Response.Status.OK);
            }
            System.out.println("WebApplicationExceptionFilter.postFilter() exit");
        }
    }

    @GET
    public String pingMe() {
        return "ping!";
    }

    @POST
    @Path("webapplication_entity")
    public String testWebApplicationExceptionEntity(String s) {
        String[] tokens = s.split(":");
        assert tokens.length == 2;
        int statusCode = Integer.valueOf(tokens[1]);
        Response r = Response.status(statusCode).entity(s).build();
        throw new WebApplicationException(r);
    }

    @POST
    @Path("webapplication_noentity")
    public String testWebApplicationExceptionNoEntity(String s) {
        String[] tokens = s.split(":");
        assert tokens.length == 2;
        int statusCode = Integer.valueOf(tokens[1]);
        Response r = Response.status(statusCode).build();
        throw new WebApplicationException(r);
    }

    @POST
    @Path("my")
    public String testMyException(String s) {
        String[] tokens = s.split(":");
        assert tokens.length == 2;
        int statusCode = Integer.valueOf(tokens[1]);
        Response r = Response.status(statusCode).build();
        throw new MyException(r);
    }

    @POST
    @Path("mysub")
    public String testMySubException(String s) {
        String[] tokens = s.split(":");
        assert tokens.length == 2;
        int statusCode = Integer.valueOf(tokens[1]);
        Response r = Response.status(statusCode).build();
        throw new MySubException(r);
    }

    @POST
    @Path("mysubsub")
    public String testMySubSubException(String s) {
        String[] tokens = s.split(":");
        assert tokens.length == 2;
        int statusCode = Integer.valueOf(tokens[1]);
        Response r = Response.status(statusCode).build();
        throw new MySubSubException(r);
    }

    @POST
    @Path("request_exception")
    public String exceptionInRequestFilter() {
        throw new InternalServerErrorException();        // should not reach here
    }

    @GET
    @Path("response_exception")
    public String exceptionInResponseFilter() {
        return "Response Exception";
    }
}
