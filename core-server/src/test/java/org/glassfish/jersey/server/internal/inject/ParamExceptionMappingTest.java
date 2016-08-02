/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2016 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.server.internal.inject;

import java.net.URI;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.ext.ExceptionMapper;

import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.ParamException;
import org.glassfish.jersey.server.RequestContextBuilder;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Not sure whether this is relevant anymore.
 *
 * @author Paul Sandoz
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
@SuppressWarnings("unchecked")
public class ParamExceptionMappingTest extends AbstractTest {

    public abstract static class BaseExceptionMapper<T extends ParamException> implements ExceptionMapper<T> {

        public Response toResponse(T exception, String entity) {
            assertEquals("x", exception.getParameterName());

            // path param and form param can be integers in this test, thus different default value
            if (!exception.getParameterType().equals(PathParam.class)
                    && !exception.getParameterType().equals(FormParam.class)) {

                assertEquals("default", exception.getDefaultStringValue());
            } else {
                assertTrue(exception.getDefaultStringValue().equals("default") || exception.getDefaultStringValue().equals("1"));
            }
            return Response.fromResponse(exception.getResponse()).entity(entity).build();
        }
    }

    public static class ParamExceptionMapper extends BaseExceptionMapper<ParamException> {

        public Response toResponse(ParamException exception) {
            return toResponse(exception, "param");
        }
    }

    public static class UriExceptionMapper extends BaseExceptionMapper<ParamException.UriParamException> {

        public Response toResponse(ParamException.UriParamException exception) {
            return toResponse(exception, "uri");
        }
    }

    public static class PathExceptionMapper extends BaseExceptionMapper<ParamException.PathParamException> {

        public Response toResponse(ParamException.PathParamException exception) {
            return toResponse(exception, "path");
        }
    }

    public static class MatrixExceptionMapper extends BaseExceptionMapper<ParamException.MatrixParamException> {

        public Response toResponse(ParamException.MatrixParamException exception) {
            return toResponse(exception, "matrix");
        }
    }

    public static class QueryExceptionMapper extends BaseExceptionMapper<ParamException.QueryParamException> {

        public Response toResponse(ParamException.QueryParamException exception) {
            return toResponse(exception, "query");
        }
    }

    public static class
    CookieExceptionMapper extends BaseExceptionMapper<ParamException.CookieParamException> {

        public Response toResponse(ParamException.CookieParamException exception) {
            return toResponse(exception, "cookie");
        }
    }

    public static class HeaderExceptionMapper extends BaseExceptionMapper<ParamException.HeaderParamException> {

        public Response toResponse(ParamException.HeaderParamException exception) {
            return toResponse(exception, "header");
        }
    }

    public static class FormExceptionMapper extends BaseExceptionMapper<ParamException.FormParamException> {

        public Response toResponse(ParamException.FormParamException exception) {
            return toResponse(exception, "form");
        }
    }

    @Path("/")
    public static class ParamExceptionMapperResource {

        @Path("path/{x}")
        @GET
        public String getPath(@DefaultValue("1") @PathParam("x") int x) {
            return "";
        }

        @Path("matrix")
        @GET
        public String getMatrix(@DefaultValue("default") @MatrixParam("x") URI x) {
            return "";
        }

        @Path("query")
        @GET
        public String getQuery(@DefaultValue("default") @QueryParam("x") URI x) {
            return "";
        }

        @Path("cookie")
        @GET
        public String getCookie(@DefaultValue("default") @CookieParam("x") URI x) {
            return "";
        }

        @Path("header")
        @GET
        public String getHeader(@DefaultValue("default") @HeaderParam("x") URI x) {
            return "";
        }

        @Path("form")
        @POST
        @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
        public String postForm(@DefaultValue("default") @FormParam("x") URI x) {
            return "";
        }

        @Path("form-int")
        @POST
        @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
        public String postForm(@DefaultValue("1") @FormParam("x") int x) {
            return "";
        }
    }

    @Test
    public void testParamException() throws ExecutionException, InterruptedException {
        initiateWebApplication(ParamExceptionMapperResource.class,
                               PathExceptionMapper.class,
                               MatrixExceptionMapper.class,
                               QueryExceptionMapper.class,
                               CookieExceptionMapper.class,
                               HeaderExceptionMapper.class,
                               FormExceptionMapper.class);

        ContainerResponse responseContext = getResponseContext(UriBuilder.fromPath("/").path("path/ test").build().toString());
        assertEquals("path", responseContext.getEntity());

        responseContext = getResponseContext(UriBuilder.fromPath("/").path("matrix;x= 123").build().toString());
        assertEquals("matrix", responseContext.getEntity());

        responseContext = getResponseContext(UriBuilder.fromPath("/").path("query").queryParam("x", " 123").build().toString());
        assertEquals("query", responseContext.getEntity());

        responseContext = getResponseContext(UriBuilder.fromPath("/").path("cookie").build().toString(), new Cookie("x", " 123"));
        assertEquals("cookie", responseContext.getEntity());

        responseContext = apply(
                RequestContextBuilder.from("/header", "GET")
                        .header("x", " 123")
                        .build()
        );
        assertEquals("header", responseContext.getEntity());

        Form f = new Form();
        f.param("x", " 123");
        responseContext = apply(
                RequestContextBuilder.from("/form", "POST")
                        .type(MediaType.APPLICATION_FORM_URLENCODED_TYPE)
                        .entity(f)
                        .build()
        );
        assertEquals("form", responseContext.getEntity());
    }

    @Test
    public void testFormParamPrimitiveValidation() throws ExecutionException, InterruptedException {
        initiateWebApplication(ParamExceptionMapperResource.class,
                               FormExceptionMapper.class);

        Form f = new Form();
        f.param("x", "http://oracle.com");
        ContainerResponseContext responseContext = apply(
                RequestContextBuilder.from("/form-int", "POST")
                                     .type(MediaType.APPLICATION_FORM_URLENCODED_TYPE)
                                     .entity(f)
                                     .build()
        );

        assertEquals("form", responseContext.getEntity());
    }

    @Test
    public void testGeneralParamException() throws ExecutionException, InterruptedException {
        initiateWebApplication(ParamExceptionMapperResource.class,
                ParamExceptionMapper.class);

        ContainerResponse responseContext = getResponseContext(UriBuilder.fromPath("/").path("path/ 123").build().toString());
        assertEquals("param", responseContext.getEntity());

        responseContext = getResponseContext(UriBuilder.fromPath("/").path("matrix;x= 123").build().toString());
        assertEquals("param", responseContext.getEntity());

        responseContext = getResponseContext(UriBuilder.fromPath("/").path("query").queryParam("x", " 123").build().toString());
        assertEquals("param", responseContext.getEntity());

        responseContext = getResponseContext(UriBuilder.fromPath("/").path("cookie").build().toString(), new Cookie("x", " 123"));
        assertEquals("param", responseContext.getEntity());

        responseContext = apply(
                RequestContextBuilder.from("/header", "GET")
                        .header("x", " 123")
                        .build()
        );
        assertEquals("param", responseContext.getEntity());

        Form f = new Form();
        f.param("x", " 123");
        responseContext = apply(
                RequestContextBuilder.from("/form", "POST")
                        .type(MediaType.APPLICATION_FORM_URLENCODED_TYPE)
                        .entity(f)
                        .build()
        );
        assertEquals("param", responseContext.getEntity());
    }

    @Test
    public void testURIParamException() throws ExecutionException, InterruptedException {
        initiateWebApplication(ParamExceptionMapperResource.class,
                UriExceptionMapper.class);

        ContainerResponse responseContext = getResponseContext(UriBuilder.fromPath("/").path("path/ 123").build().toString());
        assertEquals("uri", responseContext.getEntity());

        responseContext = getResponseContext(UriBuilder.fromPath("/").path("matrix;x= 123").build().toString());
        assertEquals("uri", responseContext.getEntity());

        responseContext = getResponseContext(UriBuilder.fromPath("/").path("query").queryParam("x", " 123").build().toString());
        assertEquals("uri", responseContext.getEntity());
    }

}
