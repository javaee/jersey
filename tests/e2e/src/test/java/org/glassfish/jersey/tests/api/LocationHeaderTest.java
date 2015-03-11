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
package org.glassfish.jersey.tests.api;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import org.glassfish.jersey.server.ManagedAsync;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test if the location response header is correctly adjusted to contain an absolute URI.
 *
 * @author Adam Lindenthal (adam.lindenthal at oracle.com)
 */
public class LocationHeaderTest extends JerseyTest {

    private static final Logger LOGGER = Logger.getLogger(LocationHeaderTest.class.getName());
    static ExecutorService executor;

    private static AtomicBoolean executorComparisonFailed = new AtomicBoolean(false);
    private static AtomicBoolean interrupted = new AtomicBoolean(false);

    @Override
    protected ResourceConfig configure() {
        enable(TestProperties.LOG_TRAFFIC);
        return new ResourceConfig(
                LocationHeaderTest.ResponseTest.class,
                LocationManipulationDynamicBinding.class,
                AbortingPreMatchingRequestFilter.class,
                TestExceptionMapper.class
        );
    }

    /**
     * Prepare test infrastructure.
     *
     * In this case it prepares executor thread pool of size one and initializes the thread.
     * @throws Exception
     */
    @Override
    public void setUp() throws Exception {
        super.setUp();
        /* thread pool for custom executor async test */
        LocationHeaderTest.executor = Executors.newFixedThreadPool(1);

        // Force the thread to be eagerly instantiated - this prevents the instantiation later and ensures, that the thread
        // will not be a child thread of the request handling thread, so the thread-local baseUri variable will not be inherited.
        LocationHeaderTest.executor.submit(new Runnable() {
            @Override
            public void run() {
                LOGGER.info("Thread pool initialized.");
            }
        });
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
            URI uri = getUriBuilder().segment("location").build();
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
         * Resource method for the test with uri rewritten in the filter
         * @return test response with relative location uri
         */
        @GET
        @Path("locationWithFilter")
        public Response locationTestWithFilter() {
            // the logic is the same as for the basic case, just the resource method name has to differ because of binding
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
            URI uri = getUriBuilder().segment("foo").build();
            return Response.created(uri).entity("Return from locationTestWithInterceptor").type("text/plain").build();
        }

        /**
         * Resource method for the test with entity containing response
         * @return test response with relative uri and with entity
         */
        @GET
        @Path("locationWithBody")
        @Produces("text/plain")
        public Response locationTestWithBody() {
            URI uri = getUriBuilder().segment("locationWithBody").build();
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
            URI uri = getUriBuilder().segment("locationDirect").build();
            Response response = Response.created(uri).build();
            return response.getLocation().equals(uriInfo.getAbsolutePath());
        }

        /**
         * Asynchronous resource method for testing if the URI is absolutized also in case of asynchronous processing;
         *
         * The response is created in the separate thread. This tests, that the thread still has access to the request baseUri
         * thread-local variable in {@link org.glassfish.jersey.message.internal.OutboundJaxrsResponse.Builder}.
         */
        @GET
        @Path("locationAsync")
        public void locationAsync(@Suspended final AsyncResponse asyncResponse) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    URI uri = getUriBuilder().segment("locationAsync").build();
                    Response result = Response.created(uri).build();

                    URI location = result.getLocation();
                    if (uriInfo.getAbsolutePath().equals(location)) {
                        asyncResponse.resume(result);
                    } else {
                        asyncResponse.resume(Response.serverError().entity(location.toString()).build());
                    }

                }
            }).start();
        }

        /**
         * Resource method for async test with custom executor.
         *
         * It runs in a thread that was not created within the request scope, so it does not inherit the baseUri thread-local
         * variable value.
         * In this case, URI will not be absolutized until calling {@link AsyncResponse#resume(Object)}.
         */
        @GET
        @Path("executorAsync")
        @ManagedAsync
        public void executorAsync(@Suspended final AsyncResponse asyncResponse) {
            LocationHeaderTest.executor.submit(new Runnable() {
                @Override
                public void run() {
                    URI uri = getUriBuilder().segment("executorAsync").build();
                    Response result = Response.created(uri).build();
                    asyncResponse.resume(result);
                    if (!uriInfo.getAbsolutePath().equals(result.getLocation())) {
                        executorComparisonFailed.set(true);
                    }
                }
            });
        }

        /**
         * Placeholder for the suspended async responses;
         * For the current test a simple static field would be enough, but this is easily extensible;
         *
         * This is inspired by the {@link AsyncResponse} javadoc example
         */
        private static final BlockingQueue<AsyncResponse> suspended = new ArrayBlockingQueue<AsyncResponse>(5);

        /**
         * Start of the async test - stores the asynchronous response object
         */
        @GET
        @Path("locationAsyncStart")
        public void locationAsyncStart(@Suspended final AsyncResponse asyncResponse) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        suspended.put(asyncResponse);
                    } catch (InterruptedException e) {
                        asyncResponse.cancel();
                        Thread.currentThread().interrupt();
                        interrupted.set(true);
                    }
                }
            }).start();
        }

        /**
         * Finish of the async test - creates a response, checks the location header and resumes the asyncResponse
         * @return true if the URI was correctly absolutized, false if the URI is relative or differs from the expected URI
         */
        @GET
        @Path("locationAsyncFinish")
        public Boolean locationAsyncFinish() throws InterruptedException {
            AsyncResponse asyncResponse = suspended.poll(2000, TimeUnit.MILLISECONDS);

            URI uri = getUriBuilder().segment("locationAsyncFinish").build();
            Response result = Response.created(uri).build();
            boolean wasEqual = result.getLocation().equals(uriInfo.getAbsolutePath());

            asyncResponse.resume(result);
            return wasEqual;
        }

        /**
         * Resource method for testing URI absolutization after the abortion in the post-matching filter.
         * @return dummy response - this string should never be propagated to the client (the processing chain
         * will be aborted in the filter before this resource method is even called.
         * However, it is needed here, because the filter is bound to the resource method name.
         */
        @GET
        @Path("locationAborted")
        public String locationTestAborted() {
            assertTrue("The resource method locationTestAborted() should not have been called. The post-matching filter was "
                    + "not configured correctly. ", false);
            return "DUMMY_RESPONSE"; // this string should never reach the client (the resource method will not be called)
        }

        /**
         * Resource method for testing URI absolutization after the abortion in the pre-matching filter.
         * @return dummy response - this string should never be propagated to the client (the processing chain will be
         * executorComparisonFailed in the filter before this resource method is even called.
         * However, it is needed here, because the filter is bound to the resource method name.
         */
        @GET
        @Path("locationAbortedPreMatching")
        public String locationTestPreMatchingAborted() {
            assertTrue("The resource method locationTestPreMatchingAborted() should not have been called. The pre-matching "
                    + "filter was not configured correctly. ", false);
            return "DUMMY_RESPONSE"; // this string should never reach the client (the resource method will not be called)
        }

        /**
         * Resource method for the test of ResponseFilters in the sync case.
         * Returns response with a relative URI, which is than absolutized by Jersey.
         * Later the {@link UriCheckingResponseFilter} is triggered and checks the URI again - the check itself is done in the
         * filter.
         * Based on the result of the check, the filter returns the original status (201 - Created) or an
         * error status (500 - Internal Server Error) with an error message in the response body.
         */
        @GET
        @Path("responseFilterSync")
        public Response responseFilterSync() {
            return Response.created(getUriBuilder().segment("responseFilterSync").build()).build();
        }

        /**
         * Resource method for the test of ResponseFilters in the async case. It runs in the separate thread created on request.
         *
         * Returns response with a relative URI, which is than absolutized by Jersey.
         * Later the {@link UriCheckingResponseFilter} is triggered and checks the URI again - the check itself is done in the
         * filter.
         * Based on the result of the check, the filter returns the original status (201 - Created) or an
         * error status (500 - Internal Server Error) with an error message in the response body.
         */
        @GET
        @Path("responseFilterAsync")
        public void responseFilterAsync(@Suspended final AsyncResponse asyncResponse) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Response result = Response.created(getUriBuilder().segment("responseFilterAsync").build()).build();
                    asyncResponse.resume(result);
                }
            }).start();
        }

        /**
         * Resource method for the test of ResponseFilters in the async/executor case. It runs in a thread created out of the
         * request scope.
         *
         * Returns response with a relative URI, which is than absolutized by Jersey.
         * Later the {@link UriCheckingResponseFilter} is triggered and checks the URI again - the check itself is done in the
         * filter.
         * Based on the result of the check, the filter returns the original status (201 - Created) or an
         * error status (500 - Internal Server Error) with an error message in the response body.
         */
        @GET
        @Path("responseFilterAsyncExecutor")
        public void responseFilterAsyncExecutor(@Suspended final AsyncResponse asyncResponse) {
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    Response result = Response.created(getUriBuilder().segment("responseFilterAsyncExecutor").build()).build();
                    asyncResponse.resume(result);
                }
            });
        }

        /**
         * Resource method for testing for testing the URI absolutization in the exception mapper in the synchronous case.
         *
         * Method always throws {@link WebApplicationException}, which is defined to be handled by {@link TestExceptionMapper}.
         * The exception mapper then creates the response with relative URI and response is routed
         * into {@link UriCheckingResponseFilter}, which checks if th URI was correctly absolutized.
         * @return does not return any response
         */
        @GET
        @Path("exceptionMapperSync")
        public Response exceptionMapperSync() {
            throw new WebApplicationException();
        }

        /**
         * Resource method for testing for testing the URI absolutization in the exception mapper in the asynchronous case.
         * New thread is started for the resource method processing.
         *
         * Method always "throws" {@link WebApplicationException} (in case of async methods,
         * the exceptions are not thrown directly, but passed to {@link AsyncResponse#resume(Throwable)}),
         * which is defined to be handled by {@link TestExceptionMapper}.         *
         * The exception mapper then creates the response with relative URI and response is routed
         * into {@link UriCheckingResponseFilter}, which checks if th URI was correctly absolutized.
         */
        @GET
        @Path("exceptionMapperAsync")
        public void exceptionMapperAsync(@Suspended final AsyncResponse asyncResponse) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    asyncResponse.resume(new WebApplicationException());
                }
            }).start();
        }

        /**
         * Resource method for testing for testing the URI absolutization in the exception mapper in the asynchronous case.
         * A thread from executor thread pool (created out of request scope) is used for processing the resource method.
         *
         * Method always "throws" {@link WebApplicationException} (in case of async methods,
         * the exceptions are not thrown directly, but passed to {@link AsyncResponse#resume(Throwable)}),
         * which is defined to be handled by {@link TestExceptionMapper}.         *
         * The exception mapper then creates the response with relative URI and response is routed
         * into {@link UriCheckingResponseFilter}, which checks if th URI was correctly absolutized.
         */
        @GET
        @Path("exceptionMapperExecutor")
        public void exceptionMapperExecutor(@Suspended final AsyncResponse asyncResponse) {
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    asyncResponse.resume(new WebApplicationException());
                }
            });
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
     * Test the URI created in the post-matching request filter.
     */
    @Test
    public void testAbortFilter() {
        checkResponseFilter("ResponseTest/locationAborted", "uriAfterAbortion/SUCCESS");
    }

    /**
     * Test the URI created in the pre-matching request filter.
     */
    @Test
    public void testAbortPreMatchingFilter() {
        checkResource("ResponseTest/locationAbortedPreMatching", "uriAfterPreMatchingAbortion/SUCCESS");
    }

    /**
     * Basic test; resource methods returns relative uri, test expects uri to be absolute
     */
    @Test
    public void testConvertRelativeUriToAbsolute() {
        checkResource("ResponseTest/location");
    }

    /**
     * Test with entity; most of the HTTP 201 Created responses do not contain any body, just headers.
     * This test ensures, that the uri conversion works even in case when entity is present.
     */
    @Test
    public void testAbsoluteUriWithEntity() {
        Response response = checkResource("ResponseTest/locationWithBody");
        assertNotNull(response.getEntity());
    }

    /**
     * Test with URI Rewritten in the container response filter;
     * Filters do have access to the response headers and can manipulate the location uri so that it contains a relative address.
     * This test incorporates a filter which replaces the uri with a relative one. However we expect to have absolute uri at
     * the end of the chain.
     */
    @Test
    public void testAbsoluteUriWithFilter() {
        checkResource("ResponseTest/locationWithFilter", "ResponseTest/UriChangedByFilter");
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
        checkResource("ResponseTest/locationWithInterceptor", "ResponseTest/UriChangedByInterceptor");
    }

    /**
     * Test with null location;
     * Ensures, that the null location is processed correctly.
     */
    @Test
    public void testNullLocation() {
        final Response response = target().path("ResponseTest/locationNull").request(MediaType.TEXT_PLAIN).get(Response.class);
        String location = response.getHeaderString(HttpHeaders.LOCATION);
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

    /**
     * Basic asynchronous testcase; checks if the URI is correctly absolutized also within a separate thread during
     * async processing
     */
    @Test
    public void testAsync() {
        String expectedUri = getBaseUri() + "ResponseTest/locationAsync";
        final Response response = target().path("ResponseTest/locationAsync").request().get(Response.class);

        String msg = String.format("Comparison failed in the resource method. \nExpected: %1$s\nActual: %2$s",
                expectedUri, response.readEntity(String.class));
        assertNotEquals(msg, response.getStatus(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());

        String location = response.getHeaderString(HttpHeaders.LOCATION);
        LOGGER.info("Location resolved from response > " + location);
        assertEquals(expectedUri, location);
    }

    /**
     * Test with a thread from thread-pool (created out of request scope)
     */
    @Test
    public void testExecutorAsync() {
        final Response response = target().path("ResponseTest/executorAsync").request().get(Response.class);
        String location = response.getHeaderString(HttpHeaders.LOCATION);
        LOGGER.info("Location resolved from response > " + location);
        assertFalse("The comparison failed in the resource method.", executorComparisonFailed.get());
        assertEquals(getBaseUri() + "ResponseTest/executorAsync", location);
    }

    /**
     * Asynchronous testcase split over two distinct requests
     */
    @Test
    public void testSeparatedAsync() throws ExecutionException, InterruptedException {
        Future<Response> futureResponse = target().path("ResponseTest/locationAsyncStart").request().async().get();
        Boolean result = target().path("ResponseTest/locationAsyncFinish").request().get(Boolean.class);
        assertFalse("Thread was interrupted on inserting into blocking queue.", interrupted.get());
        assertTrue(result);

        Response response = futureResponse.get();
        String location = response.getHeaderString(HttpHeaders.LOCATION);
        assertEquals(getBaseUri() + "ResponseTest/locationAsyncFinish", location);
    }

    /**
     * Test, that uri is correct in the response filter (synchronous).
     */
    @Test
    public void testResponseFilterSync() {
        checkResponseFilter("ResponseTest/responseFilterSync");
    }

    /**
     * Test, that uri is correct in the response filter (asynchronous).
     */
    @Test
    public void testResponseFilterAsync() {
        checkResponseFilter("ResponseTest/responseFilterAsync");
    }

    /**
     * Test, that uri is correct in the response filter (asynchronous/executor).
     */
    @Test
    public void testResponseFilterAsyncExecutor() {
        checkResponseFilter("ResponseTest/responseFilterAsyncExecutor");
    }

    /**
     * Test, that uri is correct in the response filter when created in the exception mapper (synchronous).
     */
    @Test
    public void testExceptionMapperSync() {
        checkResponseFilter("ResponseTest/exceptionMapperSync", "EXCEPTION_MAPPER");
    }

    /**
     * Test, that uri is correct in the response filter when created in the exception mapper (asynchronous).
     */
    @Test
    public void testExceptionMapperAsync() {
        checkResponseFilter("ResponseTest/exceptionMapperAsync", "EXCEPTION_MAPPER");
    }

    /**
     * Test, that uri is correct in the response filter when created in the exception mapper (asynchronous/executor).
     */
    @Test
    public void testExceptionMapperExecutor() {
        checkResponseFilter("ResponseTest/exceptionMapperExecutor", "EXCEPTION_MAPPER");
    }

    private Response checkResource(String resourcePath) {
        return checkResource(resourcePath, resourcePath);
    }

    private Response checkResource(String resourcePath, String expectedRelativeUri) {
        final Response response = target().path(resourcePath).request(MediaType.TEXT_PLAIN).get(Response.class);
        String location = response.getHeaderString(HttpHeaders.LOCATION);
        LOGGER.info("Location resolved from response > " + location);
        assertEquals(getBaseUri() + expectedRelativeUri, location);
        return response;
    }

    private void checkResponseFilter(String resourcePath) {
        checkResponseFilter(resourcePath, resourcePath);
    }

    private void checkResponseFilter(String resourcePath, String expectedRelativeUri) {
        final Response response = target().path(resourcePath).request().get(Response.class);
        assertNotEquals("Message from response filter: " + response.readEntity(String.class),
                response.getStatus(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        assertEquals(getBaseUri() + expectedRelativeUri, response.getLocation().toString());
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
     * Response filter - check if the URI is absolute. If it is not correctly absolutized,
     * it changes the response to 500 - Internal Server Error and sets the error message into the response body.
     */
    public static class UriCheckingResponseFilter implements ContainerResponseFilter {

        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
            URI location = responseContext.getLocation();
            if (!location.isAbsolute()) {
                responseContext.setStatus(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
                responseContext.setEntity("Response location was not absolute in UriCheckingFilter. Location value: " + location);
            }
        }
    }

    /**
     * Request Filter which aborts the current request calling ContainerRequestContext.abortWith().
     *
     * The returned response is passed to Response.created() and immediately tested for absolutization.
     * This is necessary, as the relative URI would be absolutized later anyway and would reach the calling test method as
     * an absolute URI and there would be no way to determine where the URI conversion was done.
     *
     * The result of the test is propagated back to the test method by separate URI values in case of a success or a failure.
     */
    public static class AbortingRequestFilter implements ContainerRequestFilter {

        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
            LOGGER.info("Aborting request in the request filter. Returning status created.");
            String successRelativeUri = "uriAfterAbortion/SUCCESS";
            Response response = Response.created(URI.create(successRelativeUri)).build();

            if (!response.getLocation().toString().equals(requestContext.getUriInfo().getBaseUri() + successRelativeUri)) {
                response = Response.created(URI.create("uriAfterAbortion/FAILURE")).build();
            }
            requestContext.abortWith(response);
        }
    }

    /**
     * Request Filter which aborts the current request calling ContainerRequestContext.abortWith().
     *
     * The returned response is passed to Response.created() and immediately tested for absolutization.
     * This is necessary, as the relative URI would be absolutized later anyway and would reach the calling test method as
     * an absolute URI and there would be no way to determine where the URI conversion was done.
     *
     * The result of the test is propagated back to the test method by separate URI values in case of a success or a failure.
     */
    @PreMatching
    public static class AbortingPreMatchingRequestFilter implements ContainerRequestFilter {

        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
            if (requestContext.getUriInfo().getAbsolutePath().toString().endsWith("locationAbortedPreMatching")) {
                LOGGER.info("Aborting request in the request filter. Returning status created.");
                String successRelativeUri = "uriAfterPreMatchingAbortion/SUCCESS";
                Response response = Response.created(URI.create(successRelativeUri)).build();
                if (!response.getLocation().toString().equals(requestContext.getUriInfo().getBaseUri() + successRelativeUri)) {
                    response = Response.created(URI.create("uriAfterPreMatchingAbortion/FAILURE")).build();
                }
                requestContext.abortWith(response);
            }
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
     * Exception mapper which creates a test response with a relative URI.
     */
    public static class TestExceptionMapper implements ExceptionMapper<WebApplicationException> {

        @Override
        public Response toResponse(WebApplicationException exception) {
            return Response.created(URI.create("EXCEPTION_MAPPER")).build();
        }
    }

    /**
     * Registers the filter and interceptor and binds it to the resource methods of interest.
     */
    public static class LocationManipulationDynamicBinding implements DynamicFeature {

        @Override
        public void configure(ResourceInfo resourceInfo, FeatureContext context) {
            if (ResponseTest.class.equals(resourceInfo.getResourceClass())) {
                String methodName = resourceInfo.getResourceMethod().getName();
                if (methodName.contains("locationTestWithFilter")) {
                    context.register(LocationManipulationFilter.class);
                    LOGGER.info("LocationManipulationFilter registered.");
                }
                if (methodName.contains("locationTestWithInterceptor")) {
                    context.register(LocationManipulationInterceptor.class);
                    LOGGER.info("LocationManipulationInterceptor registered.");
                }
                if (methodName.contains("locationTestAborted")) {
                    context.register(AbortingRequestFilter.class);
                    LOGGER.info("AbortingRequestFilter registered.");
                }
                if (methodName.contains("responseFilterSync")
                        || methodName.contains("responseFilterAsync")
                        || methodName.contains("locationTestAborted")
                        || methodName.contains("exceptionMapperSync")
                        || methodName.contains("exceptionMapperAsync")
                        || methodName.contains("exceptionMapperExecutor")) {
                    context.register(UriCheckingResponseFilter.class);
                    LOGGER.info("UriCheckingResponseFilter registered.");
                }

            }
        }
    }
}



