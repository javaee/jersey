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

package org.glassfish.jersey.server.internal.inject;

import java.net.URI;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.*;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.ext.ExceptionMapper;

import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.RequestContextBuilder;
import org.glassfish.jersey.server.ParamException;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Not sure whether this is relevant anymore.
 *
 * @author Paul Sandoz
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
@SuppressWarnings("unchecked")
public class StringReaderTest extends AbstractTest {

    @Path("/")
    public static class BadDateResource {
        @GET
        public String doGet(@QueryParam("d") Date d) {
            return "DATE";
        }
    }

    @Test
    public void testBadDateResource() throws ExecutionException, InterruptedException {
        initiateWebApplication(BadDateResource.class);

        final ContainerResponse responseContext = getResponseContext(UriBuilder.fromPath("/").queryParam("d", "123").build().toString());

        assertEquals(404, responseContext.getStatus());
    }


    @Path("/")
    public static class BadEnumResource {
        public enum ABC {
            A, B, C;
        };

        @GET
        public String doGet(@QueryParam("d") ABC d) {
            return "ENUM";
        }
    }

    @Test
    public void testBadEnumResource() throws ExecutionException, InterruptedException {
        initiateWebApplication(BadEnumResource.class);

        final ContainerResponse responseContext = getResponseContext(UriBuilder.fromPath("/").queryParam("d", "123").build().toString());

        assertEquals(404, responseContext.getStatus());
    }

//    public static class URIStringReaderProvider implements StringReaderProvider<URI> {
//
//        public StringReader<URI> getStringReader(Class<?> type, Type genericType, Annotation[] annotations) {
//            if (type != URI.class) return null;
//
//            return new StringReader<URI>() {
//                public URI fromString(String value) {
//                    return URI.create(value);
//                }
//            };
//        }
//
//    }

    @Path("/")
    public static class BadURIResource {
        @GET
        public String doGet(@QueryParam("d") URI d) {
            return "URI";
        }
    }

//    @Test
//    public void testBadURIResource() {
//        initiateWebApplication(BadURIResource.class, URIStringReaderProvider.class);
//        ClientResponse cr = resource("/", false).queryParam("d", " 123 ").
//                get(ClientResponse.class);
//        assertEquals(404, cr.getStatus());
//    }

//    public static class ListOfStringReaderProvider implements StringReaderProvider<List<String>> {
//
//        @Override
//        public StringReader<List<String>> getStringReader(Class<?> type,
//                Type genericType, Annotation[] annotations) {
//            if (type != List.class) return null;
//
//            if (genericType instanceof ParameterizedType) {
//                ParameterizedType parameterizedType = (ParameterizedType)genericType;
//                if (parameterizedType.getActualTypeArguments().length != 1) return null;
//
//                if (parameterizedType.getActualTypeArguments()[0] != String.class) return null;
//            } else {
//                return null;
//            }
//
//            return new StringReader<List<String>>() {
//                @Override
//                public List<String> fromString(String value) {
//                    return Arrays.asList(value.split(","));
//                }
//            };
//        }
//    }

//    @Path("/")
//    public static class ListOfStringResource {
//        @GET
//        public String doGet(@QueryParam("l") List<List<String>> l) {
//            return l.toString();
//        }
//    }

//    @Test
//    public void testListOfStringReaderProvider() {
//        initiateWebApplication(ListOfStringResource.class, ListOfStringReaderProvider.class);
//        String s = resource("/", false).queryParam("l", "1,2,3").
//                get(String.class);
//
//        assertEquals(Collections.singletonList(Arrays.asList("1", "2", "3")).toString(), s);
//    }


    public static abstract class BaseExceptionMapper<T extends ParamException> implements ExceptionMapper<T> {
        public Response toResponse(T exception, String entity) {
            assertEquals("x", exception.getParameterName());
            if (exception.getParameterType() != PathParam.class) {
                assertEquals("default", exception.getDefaultStringValue());
            }
            return Response.fromResponse(exception.getResponse()).entity(entity).build();
        }
    }

    public static class ParamExceptionMapper extends BaseExceptionMapper<ParamException> {
        public Response toResponse(ParamException exception) {
            return toResponse(exception, "param");
        }
    }

    public static class URIExceptionMapper extends BaseExceptionMapper<ParamException.URIParamException> {
        public Response toResponse(ParamException.URIParamException exception) {
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
        public String getPath(@PathParam("x") URI x) {
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

        ContainerResponse responseContext = getResponseContext(UriBuilder.fromPath("/").path("path/ 123").build().toString());
        assertEquals("path", responseContext.getEntity());

        responseContext = getResponseContext(UriBuilder.fromPath("/").path("matrix;x= 123").build().toString());
        assertEquals("matrix", responseContext.getEntity());

        responseContext = getResponseContext(UriBuilder.fromPath("/").path("query").queryParam("x", " 123").build().toString());
        assertEquals("query", responseContext.getEntity());

        responseContext = getResponseContext(UriBuilder.fromPath("/").path("cookie").build().toString(), new Cookie("x", " 123"));
        assertEquals("cookie", responseContext.getEntity());

        responseContext = apply(
                RequestContextBuilder.from("/header", "GET").
                        header("x", " 123").
                        build()
        );
        assertEquals("header", responseContext.getEntity());

        Form f = new Form();
        f.param("x", " 123");
        responseContext = apply(
                RequestContextBuilder.from("/form", "POST").
                        type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).
                        entity(f).
                        build()
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
                RequestContextBuilder.from("/header", "GET").
                        header("x", " 123").
                        build()
        );
        assertEquals("param", responseContext.getEntity());

        Form f = new Form();
        f.param("x", " 123");
        responseContext = apply(
                RequestContextBuilder.from("/form", "POST").
                        type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).
                        entity(f).
                        build()
        );
        assertEquals("param", responseContext.getEntity());
    }

    @Test
    public void testURIParamException() throws ExecutionException, InterruptedException {
        initiateWebApplication(ParamExceptionMapperResource.class,
                URIExceptionMapper.class);

        ContainerResponse responseContext = getResponseContext(UriBuilder.fromPath("/").path("path/ 123").build().toString());
        assertEquals("uri", responseContext.getEntity());

        responseContext = getResponseContext(UriBuilder.fromPath("/").path("matrix;x= 123").build().toString());
        assertEquals("uri", responseContext.getEntity());

        responseContext = getResponseContext(UriBuilder.fromPath("/").path("query").queryParam("x", " 123").build().toString());
        assertEquals("uri", responseContext.getEntity());
    }


//    public static class ValidateParamReaderProvider implements StringReaderProvider<ValidateParam> {
//
//        public StringReader<ValidateParam> getStringReader(Class<?> type,
//                Type genericType, Annotation[] annotations) {
//            if (type != ValidateParam.class) return null;
//
//            return new StringReader<ValidateParam>() {
//                public ValidateParam fromString(String value) {
//                    return new ValidateParam(value);
//                }
//            };
//        }
//    }
//
//    public static class ValidateTrueParamReaderProvider implements StringReaderProvider<ValidateParam> {
//
//        @StringValueReader.ValidateDefaultValue
//        public static class NoValidateParamStringReader implements StringReader<ValidateParam> {
//            @Override
//            public ValidateParam fromString(String value) {
//                return new ValidateParam(value);
//            }
//        }
//
//        public StringReader<ValidateParam> getStringReader(Class<?> type,
//                Type genericType, Annotation[] annotations) {
//            if (type != ValidateParam.class) return null;
//
//            return new NoValidateParamStringReader();
//        }
//    }
//
//    public static class ValidateFalseParamReaderProvider implements StringReaderProvider<ValidateParam> {
//
//        @StringValueReader.ValidateDefaultValue(false)
//        public static class NoValidateParamStringReader implements StringReader<ValidateParam> {
//            @Override
//            public ValidateParam fromString(String value) {
//                return new ValidateParam(value);
//            }
//        }
//
//        public StringReader<ValidateParam> getStringReader(Class<?> type,
//                Type genericType, Annotation[] annotations) {
//            if (type != ValidateParam.class) return null;
//
//            return new NoValidateParamStringReader();
//        }
//    }
//
//    public static class ValidateParam {
//        public final String s;
//
//        public ValidateParam(String s) {
//            if (s.equals("invalid"))
//                throw new IllegalArgumentException();
//            this.s = s;
//        }
//    }
//
//    @Path("/")
//    public static class ValidateParamResource {
//        @GET
//        public String doGet(@DefaultValue("invalid") @QueryParam("x") ValidateParam d) {
//            return d.s;
//        }
//    }

//    private Errors.ErrorMessagesException catches(Closure c) {
//        return catches(c, Errors.ErrorMessagesException.class);
//    }
//
//    @Test
//    public void testValidateParam() {
//        List<Errors.ErrorMessage> messages = catches(new Closure() {
//            @Override
//            public void f() {
//                initiateWebApplication(ValidateParamResource.class, ValidateParamReaderProvider.class);
//            }
//        }).messages;
//
//        assertEquals(1, messages.size());
//    }
//
//    public void testValidateTrueParam() {
//        List<Errors.ErrorMessage> messages = catches(new Closure() {
//            @Override
//            public void f() {
//                initiateWebApplication(ValidateParamResource.class, ValidateTrueParamReaderProvider.class);
//            }
//        }).messages;
//
//        assertEquals(1, messages.size());
//    }
//
//    @Test
//    public void testNoValidateFalseParam() {
//        initiateWebApplication(ValidateParamResource.class, ValidateFalseParamReaderProvider.class);
//        ClientResponse cr = resource("/", false).queryParam("x", "valid").
//                get(ClientResponse.class);
//        assertEquals("valid", cr.getEntity(String.class));
//
//        cr = resource("/", false).queryParam("x", "invalid").
//                get(ClientResponse.class);
//        assertEquals(404, cr.getStatus());
//    }
//
//    static interface Closure {
//        void f();
//    }
}
