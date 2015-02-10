/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2015 Oracle and/or its affiliates. All rights reserved.
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

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.CookieParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotAllowedException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.NotSupportedException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.RedirectionException;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;

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
 * Exception throwing and handling related tests; such as
 * {@link WebApplicationException} handling on both server and client side,
 * proper exception throwing etc.
 *
 * @author Paul Sandoz
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public class ExceptionTest extends JerseyTest {

    @Override
    protected Application configure() {
        return new ResourceConfig(
                ExceptionDrivenResource.class,
                ResponseDrivenResource.class,
                // JERSEY-1532
                NonWaeThrowingConverter.class,
                ParamConverterThrowingNonWaeFieldTestResource1.class,
                ParamConverterThrowingNonWaeFieldTestResource2.class,
                ParamConverterThrowingNonWaeFieldTestResource3.class,
                ParamConverterThrowingNonWaeFieldTestResource4.class,
                ParamConverterThrowingNonWaeFieldTestResource5.class,
                ParamConverterThrowingNonWaeFieldTestResource6.class,
                ParamConverterThrowingNonWaeFieldTestResource7.class,
                ParamConverterThrowingNonWaeFieldTestResource8.class,
                ParamConverterThrowingNonWaeFieldTestResource9.class,
                ParamConverterThrowingNonWaeFieldTestResource10.class,
                ParamConverterThrowingNonWaeMethodTestResource.class,
                WaeThrowingConverter.class,
                ParamConverterThrowingWaeFieldTestResource1.class,
                ParamConverterThrowingWaeFieldTestResource2.class,
                ParamConverterThrowingWaeFieldTestResource3.class,
                ParamConverterThrowingWaeFieldTestResource4.class,
                ParamConverterThrowingWaeFieldTestResource5.class,
                ParamConverterThrowingWaeFieldTestResource6.class,
                ParamConverterThrowingWaeFieldTestResource7.class,
                ParamConverterThrowingWaeFieldTestResource8.class,
                ParamConverterThrowingWaeFieldTestResource9.class,
                ParamConverterThrowingWaeFieldTestResource10.class,
                ParamConverterThrowingWaeMethodTestResource.class
        );
    }

    @Override
    protected void configureClient(ClientConfig config) {
        config.property(ClientProperties.FOLLOW_REDIRECTS, false);
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

    /**
     * BEGIN: JERSEY-1532 reproducer code:
     *
     * From JAX-RS 2.0 spec, sect. 3.2:
     * ================================
     * A WebApplicationException thrown during construction of field or property values using 3 or 4 above
     * is processed directly as described in Section 3.3.4. Other exceptions thrown during construction of
     * field or property values using 3 or 4 above are treated as client errors: if the field or property is
     * annotated with @MatrixParam, @QueryParam or @PathParam then an implementation MUST generate an instance
     * of NotFoundException (404 status) that wraps the thrown exception and no entity; if the field or property
     * is annotated with @HeaderParam or @CookieParam then an implementation MUST generate an instance of
     * BadRequestException (400 status) that wraps the thrown exception and no entity. Exceptions MUST be
     * processed as described in Section 3.3.4.
     */
    public static class NonWaeType {
    }

    public static class NonWaeException extends RuntimeException {
    }

    @ParamConverter.Lazy
    public static class NonWaeThrowingConverter implements ParamConverter<NonWaeType>, ParamConverterProvider {

        @Override
        public NonWaeType fromString(String value) {
            throw new NonWaeException();
        }

        @Override
        public String toString(NonWaeType value) {
            throw new NonWaeException();
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
            if (NonWaeType.class.isAssignableFrom(rawType)) {
                return (ParamConverter<T>) this;
            } else {
                return null;
            }
        }
    }

    @Path("param-converter/non-wae/field/default-matrix")
    public static class ParamConverterThrowingNonWaeFieldTestResource1 {
        @DefaultValue("value")
        @MatrixParam("missing")
        private NonWaeType field;

        @GET
        public Response get() {
            return Response.status(599).entity("This method should not be invoked").build();
        }
    }

    @Path("param-converter/non-wae/field/default-path")
    public static class ParamConverterThrowingNonWaeFieldTestResource2 {
        @DefaultValue("value")
        @PathParam("missing")
        private NonWaeType field;

        @GET
        public Response get() {
            return Response.status(599).entity("This method should not be invoked").build();
        }
    }

    @Path("param-converter/non-wae/field/default-query")
    public static class ParamConverterThrowingNonWaeFieldTestResource3 {
        @DefaultValue("value")
        @QueryParam("missing")
        private NonWaeType field;

        @GET
        public Response get() {
            return Response.status(599).entity("This method should not be invoked").build();
        }
    }

    @Path("param-converter/non-wae/field/default-header")
    public static class ParamConverterThrowingNonWaeFieldTestResource4 {
        @DefaultValue("value")
        @HeaderParam("missing")
        private NonWaeType field;

        @GET
        public Response get() {
            return Response.status(599).entity("This method should not be invoked").build();
        }
    }

    @Path("param-converter/non-wae/field/default-cookie")
    public static class ParamConverterThrowingNonWaeFieldTestResource5 {
        @DefaultValue("value")
        @CookieParam("missing")
        private NonWaeType field;

        @GET
        public Response get() {
            return Response.status(599).entity("This method should not be invoked").build();
        }
    }

    @Path("param-converter/non-wae/field/matrix")
    public static class ParamConverterThrowingNonWaeFieldTestResource6 {
        @MatrixParam("test")
        private NonWaeType field;

        @GET
        public Response get() {
            return Response.status(599).entity("This method should not be invoked").build();
        }
    }

    @Path("param-converter/non-wae/field/path/{test}")
    public static class ParamConverterThrowingNonWaeFieldTestResource7 {
        @PathParam("test")
        private NonWaeType field;

        @GET
        public Response get() {
            return Response.status(599).entity("This method should not be invoked").build();
        }
    }

    @Path("param-converter/non-wae/field/query")
    public static class ParamConverterThrowingNonWaeFieldTestResource8 {
        @QueryParam("test")
        private NonWaeType field;

        @GET
        public Response get() {
            return Response.status(599).entity("This method should not be invoked").build();
        }
    }

    @Path("param-converter/non-wae/field/header")
    public static class ParamConverterThrowingNonWaeFieldTestResource9 {
        @HeaderParam("test")
        private NonWaeType field;

        @GET
        public Response get() {
            return Response.status(599).entity("This method should not be invoked").build();
        }
    }

    @Path("param-converter/non-wae/field/cookie")
    public static class ParamConverterThrowingNonWaeFieldTestResource10 {
        @CookieParam("test")
        private NonWaeType field;

        @GET
        public Response get() {
            return Response.status(599).entity("This method should not be invoked").build();
        }
    }

    @Path("param-converter/non-wae/method")
    public static class ParamConverterThrowingNonWaeMethodTestResource {

        @GET
        @Path("default-matrix")
        public Response defaultMatrixTest(@DefaultValue("value") @MatrixParam("missing") NonWaeType param) {
            return Response.status(599).entity("This method should not be invoked").build();
        }

        @GET
        @Path("default-path")
        public Response defaultPathTest(@DefaultValue("value") @PathParam("missing") NonWaeType param) {
            return Response.status(599).entity("This method should not be invoked").build();
        }

        @GET
        @Path("default-query")
        public Response defaultQueryTest(@DefaultValue("value") @QueryParam("missing") NonWaeType param) {
            return Response.status(599).entity("This method should not be invoked").build();
        }

        @GET
        @Path("default-header")
        public Response defaultHeaderTest(@DefaultValue("value") @HeaderParam("missing") NonWaeType param) {
            return Response.status(599).entity("This method should not be invoked").build();
        }

        @GET
        @Path("default-cookie")
        public Response defaultCookieTest(@DefaultValue("value") @CookieParam("missing") NonWaeType param) {
            return Response.status(599).entity("This method should not be invoked").build();
        }

        @GET
        @Path("matrix")
        public Response matrixTest(@MatrixParam("test") NonWaeType param) {
            return Response.status(599).entity("This method should not be invoked").build();
        }

        @GET
        @Path("path/{test}")
        public Response pathTest(@PathParam("test") NonWaeType param) {
            return Response.status(599).entity("This method should not be invoked").build();
        }

        @GET
        @Path("query")
        public Response queryTest(@QueryParam("test") NonWaeType param) {
            return Response.status(599).entity("This method should not be invoked").build();
        }

        @GET
        @Path("header")
        public Response headerTest(@HeaderParam("test") NonWaeType param) {
            return Response.status(599).entity("This method should not be invoked").build();
        }

        @GET
        @Path("cookie")
        public Response cookieTest(@CookieParam("test") NonWaeType param) {
            return Response.status(599).entity("This method should not be invoked").build();
        }
    }

    @Test
    public void testNonWaeExceptionThrownFromParamConverter() {
        final WebTarget target = target("param-converter/non-wae/");

        testSingle("field-default-matrix", 404, target.path("field/default-matrix").request());
        testSingle("field-default-path", 404, target.path("field/default-path").request());
        testSingle("field-default-query", 404, target.path("field/default-query").request());
        testSingle("field-default-header", 400, target.path("field/default-header").request());
        testSingle("field-default-cookie", 400, target.path("field/default-cookie").request());

        testSingle("field-matrix", 404, target.path("field/matrix;test=value").request());
        testSingle("field-path", 404, target.path("field/path/value").request());
        testSingle("field-query", 404, target.path("field/query").queryParam("test", "value").request());
        testSingle("field-header", 400, target.path("field/header").request().header("test", "value"));
        testSingle("field-cookie", 400, target.path("field/cookie").request().cookie("test", "value"));

        testSingle("method-default-matrix", 404, target.path("method/default-matrix").request());
        testSingle("method-default-path", 404, target.path("method/default-path").request());
        testSingle("method-default-query", 404, target.path("method/default-query").request());
        testSingle("method-default-header", 400, target.path("method/default-header").request());
        testSingle("method-default-cookie", 400, target.path("method/default-cookie").request());

        testSingle("method-matrix", 404, target.path("method/matrix;test=value").request());
        testSingle("method-path", 404, target.path("method/path/value").request());
        testSingle("method-query", 404, target.path("method/query").queryParam("test", "value").request());
        testSingle("method-header", 400, target.path("method/header").request().header("test", "value"));
        testSingle("method-cookie", 400, target.path("method/cookie").request().cookie("test", "value"));
    }

    public static class WaeType {
    }

    @ParamConverter.Lazy
    public static class WaeThrowingConverter implements ParamConverter<WaeType>, ParamConverterProvider {

        @Override
        public WaeType fromString(String value) {
            throw new WebApplicationException(555);
        }

        @Override
        public String toString(WaeType value) {
            throw new WebApplicationException(555);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
            if (WaeType.class.isAssignableFrom(rawType)) {
                return (ParamConverter<T>) this;
            } else {
                return null;
            }
        }
    }

    @Path("param-converter/wae/field/default-matrix")
    public static class ParamConverterThrowingWaeFieldTestResource1 {
        @DefaultValue("value")
        @MatrixParam("missing")
        private WaeType field;

        @GET
        public Response get() {
            return Response.status(599).entity("This method should not be invoked").build();
        }
    }

    @Path("param-converter/wae/field/default-path")
    public static class ParamConverterThrowingWaeFieldTestResource2 {
        @DefaultValue("value")
        @PathParam("missing")
        private WaeType field;

        @GET
        public Response get() {
            return Response.status(599).entity("This method should not be invoked").build();
        }
    }

    @Path("param-converter/wae/field/default-query")
    public static class ParamConverterThrowingWaeFieldTestResource3 {
        @DefaultValue("value")
        @QueryParam("missing")
        private WaeType field;

        @GET
        public Response get() {
            return Response.status(599).entity("This method should not be invoked").build();
        }
    }

    @Path("param-converter/wae/field/default-header")
    public static class ParamConverterThrowingWaeFieldTestResource4 {
        @DefaultValue("value")
        @HeaderParam("missing")
        private WaeType field;

        @GET
        public Response get() {
            return Response.status(599).entity("This method should not be invoked").build();
        }
    }

    @Path("param-converter/wae/field/default-cookie")
    public static class ParamConverterThrowingWaeFieldTestResource5 {
        @DefaultValue("value")
        @CookieParam("missing")
        private WaeType field;

        @GET
        public Response get() {
            return Response.status(599).entity("This method should not be invoked").build();
        }
    }

    @Path("param-converter/wae/field/matrix")
    public static class ParamConverterThrowingWaeFieldTestResource6 {
        @MatrixParam("test")
        private WaeType field;

        @GET
        public Response get() {
            return Response.status(599).entity("This method should not be invoked").build();
        }
    }

    @Path("param-converter/wae/field/path/{test}")
    public static class ParamConverterThrowingWaeFieldTestResource7 {
        @PathParam("test")
        private WaeType field;

        @GET
        public Response get() {
            return Response.status(599).entity("This method should not be invoked").build();
        }
    }

    @Path("param-converter/wae/field/query")
    public static class ParamConverterThrowingWaeFieldTestResource8 {
        @QueryParam("test")
        private WaeType field;

        @GET
        public Response get() {
            return Response.status(599).entity("This method should not be invoked").build();
        }
    }

    @Path("param-converter/wae/field/header")
    public static class ParamConverterThrowingWaeFieldTestResource9 {
        @HeaderParam("test")
        private WaeType field;

        @GET
        public Response get() {
            return Response.status(599).entity("This method should not be invoked").build();
        }
    }

    @Path("param-converter/wae/field/cookie")
    public static class ParamConverterThrowingWaeFieldTestResource10 {
        @CookieParam("test")
        private WaeType field;

        @GET
        public Response get() {
            return Response.status(599).entity("This method should not be invoked").build();
        }
    }

    @Path("param-converter/wae/method")
    public static class ParamConverterThrowingWaeMethodTestResource {

        @GET
        @Path("default-matrix")
        public Response defaultMatrixTest(@DefaultValue("value") @MatrixParam("missing") WaeType param) {
            return Response.status(599).entity("This method should not be invoked").build();
        }

        @GET
        @Path("default-path")
        public Response defaultPathTest(@DefaultValue("value") @PathParam("missing") WaeType param) {
            return Response.status(599).entity("This method should not be invoked").build();
        }

        @GET
        @Path("default-query")
        public Response defaultQueryTest(@DefaultValue("value") @QueryParam("missing") WaeType param) {
            return Response.status(599).entity("This method should not be invoked").build();
        }

        @GET
        @Path("default-header")
        public Response defaultHeaderTest(@DefaultValue("value") @HeaderParam("missing") WaeType param) {
            return Response.status(599).entity("This method should not be invoked").build();
        }

        @GET
        @Path("default-cookie")
        public Response defaultCookieTest(@DefaultValue("value") @CookieParam("missing") WaeType param) {
            return Response.status(599).entity("This method should not be invoked").build();
        }

        @GET
        @Path("matrix")
        public Response matrixTest(@MatrixParam("test") WaeType param) {
            return Response.status(599).entity("This method should not be invoked").build();
        }

        @GET
        @Path("path/{test}")
        public Response pathTest(@PathParam("test") WaeType param) {
            return Response.status(599).entity("This method should not be invoked").build();
        }

        @GET
        @Path("query")
        public Response queryTest(@QueryParam("test") WaeType param) {
            return Response.status(599).entity("This method should not be invoked").build();
        }

        @GET
        @Path("header")
        public Response headerTest(@HeaderParam("test") WaeType param) {
            return Response.status(599).entity("This method should not be invoked").build();
        }

        @GET
        @Path("cookie")
        public Response cookieTest(@CookieParam("test") WaeType param) {
            return Response.status(599).entity("This method should not be invoked").build();
        }
    }

    @Test
    public void testWaeExceptionThrownFromParamConverter() {
        final WebTarget target = target("param-converter/wae/");

        testSingle("field-default-matrix", 555, target.path("field/default-matrix").request());
        testSingle("field-default-path", 555, target.path("field/default-path").request());
        testSingle("field-default-query", 555, target.path("field/default-query").request());
        testSingle("field-default-header", 555, target.path("field/default-header").request());
        testSingle("field-default-cookie", 555, target.path("field/default-cookie").request());

        testSingle("field-matrix", 555, target.path("field/matrix;test=value").request());
        testSingle("field-path", 555, target.path("field/path/value").request());
        testSingle("field-query", 555, target.path("field/query").queryParam("test", "value").request());
        testSingle("field-header", 555, target.path("field/header").request().header("test", "value"));
        testSingle("field-cookie", 555, target.path("field/cookie").request().cookie("test", "value"));

        testSingle("method-default-matrix", 555, target.path("method/default-matrix").request());
        testSingle("method-default-path", 555, target.path("method/default-path").request());
        testSingle("method-default-query", 555, target.path("method/default-query").request());
        testSingle("method-default-header", 555, target.path("method/default-header").request());
        testSingle("method-default-cookie", 555, target.path("method/default-cookie").request());

        testSingle("method-matrix", 555, target.path("method/matrix;test=value").request());
        testSingle("method-path", 555, target.path("method/path/value").request());
        testSingle("method-query", 555, target.path("method/query").queryParam("test", "value").request());
        testSingle("method-header", 555, target.path("method/header").request().header("test", "value"));
        testSingle("method-cookie", 555, target.path("method/cookie").request().cookie("test", "value"));
    }

    private void testSingle(String caseName, int expectedStatus, Invocation.Builder request) {
        final Response response = request.get();
        assertEquals("Test of an exception thrown during field/parameter injection [" + caseName + "] failed.",
                expectedStatus,
                response.getStatus());
    }
    /**
     * END: JERSEY-1532 reproducer code.
     */
}
