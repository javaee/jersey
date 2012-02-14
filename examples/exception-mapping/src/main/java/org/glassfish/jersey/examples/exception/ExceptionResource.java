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
package org.glassfish.jersey.examples.exception;

import java.io.IOException;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.FilterContext;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.RequestFilter;
import javax.ws.rs.ext.ResponseFilter;

import org.glassfish.jersey.examples.exception.Exceptions.MyException;
import org.glassfish.jersey.examples.exception.Exceptions.MySubException;
import org.glassfish.jersey.examples.exception.Exceptions.MySubSubException;

/**
 * ExceptionResource class.
 *
 * @author Santiago.PericasGeertsen at oracle.com
 */
@Path("exception")
public class ExceptionResource {

    @Provider
    static class MyResponseFilter implements ResponseFilter {

        @Override
        public void postFilter(FilterContext context) throws IOException {
            System.out.println("MyResponseFilter.postFilter() enter");
            Response response = context.getResponseBuilder().entity(
                    context.getResponse().readEntity(String.class) + ":" + getClass().getSimpleName()).build();
            context.setResponse(response);
            System.out.println("MyResponseFilter.postFilter() exit");
        }
    }

    @Provider
    static class WebApplicationExceptionFilter implements RequestFilter, ResponseFilter {

        @Override
        public void preFilter(FilterContext context) throws IOException {
            System.out.println("WebApplicationExceptionFilter.preFilter() enter");
            Request r = context.getRequest();
            if (r.hasEntity() && r.readEntity(String.class).equals("Request Exception")) {
                throw new WebApplicationException(Response.Status.OK);
            }
            System.out.println("WebApplicationExceptionFilter.preFilter() exit");

        }

        @Override
        public void postFilter(FilterContext context) throws IOException {
            System.out.println("WebApplicationExceptionFilter.postFilter() enter");
            Response r = context.getResponse();
            if (r.hasEntity() && r.readEntity(String.class).equals("Response Exception")) {
                throw new WebApplicationException(Response.Status.OK);
            }
            System.out.println("WebApplicationExceptionFilter.postFilter() exit");
        }
    }

    @GET
    @Produces("text/plain")
    public String pingMe() {
        return "ping!";
    }

    @POST
    @Consumes("text/plain")
    @Produces("text/plain")
    @Path("webapplication_entity")
    public String testWebApplicationExceptionEntity(String s) {
        String[] tokens = s.split(":");
        assert tokens.length == 2;
        int statusCode = Integer.valueOf(tokens[1]);
        Response r = Response.status(statusCode).entity(s).build();
        throw new WebApplicationException(r);
    }

    @POST
    @Consumes("text/plain")
    @Produces("text/plain")
    @Path("webapplication_noentity")
    public String testWebApplicationExceptionNoEntity(String s) {
        String[] tokens = s.split(":");
        assert tokens.length == 2;
        int statusCode = Integer.valueOf(tokens[1]);
        Response r = Response.status(statusCode).build();
        throw new WebApplicationException(r);
    }

    @POST
    @Consumes("text/plain")
    @Produces("text/plain")
    @Path("my")
    public String testMyException(String s) {
        String[] tokens = s.split(":");
        assert tokens.length == 2;
        int statusCode = Integer.valueOf(tokens[1]);
        Response r = Response.status(statusCode).build();
        throw new MyException(r);
    }

    @POST
    @Consumes("text/plain")
    @Produces("text/plain")
    @Path("mysub")
    public String testMySubException(String s) {
        String[] tokens = s.split(":");
        assert tokens.length == 2;
        int statusCode = Integer.valueOf(tokens[1]);
        Response r = Response.status(statusCode).build();
        throw new MySubException(r);
    }

    @POST
    @Consumes("text/plain")
    @Produces("text/plain")
    @Path("mysubsub")
    public String testMySubSubException(String s) {
        String[] tokens = s.split(":");
        assert tokens.length == 2;
        int statusCode = Integer.valueOf(tokens[1]);
        Response r = Response.status(statusCode).build();
        throw new MySubSubException(r);
    }

    @POST
    @Consumes("text/plain")
    @Produces("text/plain")
    @Path("request_exception")
    public String exceptionInRequestFilter() {
        throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);        // should not reach here
    }

    @GET
    @Produces("text/plain")
    @Path("response_exception")
    public String exceptionInResponseFilter() {
        return "response_exception";
    }
}
