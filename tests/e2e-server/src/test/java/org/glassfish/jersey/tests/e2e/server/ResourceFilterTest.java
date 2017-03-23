/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.e2e.server;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.security.Principal;

import javax.ws.rs.GET;
import javax.ws.rs.NameBinding;
import javax.ws.rs.Path;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.ExceptionMapper;

import javax.annotation.Priority;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * JAX-RS name-bound filter tests.
 *
 * @author Martin Matula
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @author Miroslav.Fuksa
 * @see GloballyNameBoundResourceFilterTest
 */
public class ResourceFilterTest extends JerseyTest {

    @Override
    protected Application configure() {
        return new ResourceConfig(
                MyDynamicFeature.class,
                BasicTestsResource.class,
                NameBoundRequestFilter.class,
                NameBoundResponseFilter.class,
                PostMatchingFilter.class,
                // exception tests
                ExceptionTestsResource.class,
                ExceptionPreMatchRequestFilter.class,
                ExceptionPostMatchRequestFilter.class,
                ExceptionTestBoundResponseFilter.class,
                ExceptionTestGlobalResponseFilter.class,
                TestExceptionMapper.class,
                AbortResponseResource.class,
                AbortResponseFitler.class,
                AbortRequestFilter.class,
                ExceptionRequestFilter.class
        );
    }

    @Test
    public void testDynamicBinder() {
        test("dynamicBinder");
    }

    @Test
    public void testNameBoundRequest() {
        test("nameBoundRequest");
    }

    @Test
    public void testNameBoundResponse() {
        test("nameBoundResponse");
    }

    @Test
    public void testPostMatching() {
        test("postMatching");
    }

    // See JERSEY-1554
    @Test
    public void testGlobalPostMatchingNotInvokedOn404() {
        Response r = target("basic").path("not-found").request().get();
        assertEquals(404, r.getStatus());
        if (r.hasEntity()) {
            assertThat(r.readEntity(String.class), not(containsString("postMatching")));
        }
    }

    private void test(String name) {
        Response r = target("basic").path(name).request().get();
        assertEquals("Unexpected HTTP response status code.", 200, r.getStatus());
        assertTrue("Response does not have entity.", r.hasEntity());
        assertEquals("Unexpected response entity value.", name, r.readEntity(String.class));
    }

    @Path("/basic")
    public static class BasicTestsResource {

        @Path("dynamicBinder")
        @GET
        public String getDynamicBinder() {
            return "";
        }

        @Path("nameBoundRequest")
        @GET
        @NameBoundRequest
        public String getNameBoundRequest() {
            return "";
        }

        @Path("nameBoundResponse")
        @GET
        @NameBoundResponse
        public String getNameBoundResponse() {
            return "";
        }

        @Path("postMatching")
        @GET
        public String getPostMatching() {
            return "";
        }
    }

    @NameBinding
    @Retention(RetentionPolicy.RUNTIME)
    private static @interface NameBoundRequest {}

    @NameBinding
    @Retention(RetentionPolicy.RUNTIME)
    private static @interface NameBoundAbortResponse {}

    @NameBoundRequest
    @Priority(1)
    public static class NameBoundRequestFilter implements ContainerRequestFilter {

        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
            requestContext.abortWith(Response.ok("nameBoundRequest", MediaType.TEXT_PLAIN_TYPE).build());
        }
    }

    @NameBoundResponse
    public static class NameBoundResponseFilter implements ContainerResponseFilter {

        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
            responseContext.setEntity("nameBoundResponse", responseContext.getEntityAnnotations(), MediaType.TEXT_PLAIN_TYPE);
        }
    }

    public static class PostMatchingFilter implements ContainerRequestFilter {

        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
            if (requestContext.getUriInfo().getPath().startsWith("basic")) {
                requestContext.abortWith(Response.ok("postMatching", MediaType.TEXT_PLAIN_TYPE).build());
            }
        }
    }

    @Priority(1)
    @PreMatching
    private static class DbFilter implements ContainerRequestFilter {

        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
            requestContext.abortWith(Response.ok("dynamicBinder", MediaType.TEXT_PLAIN_TYPE).build());
        }
    }

    public static class MyDynamicFeature implements DynamicFeature {

        private final DbFilter filter = new DbFilter();

        @Override
        public void configure(final ResourceInfo resourceInfo, final FeatureContext context) {
            if ("getDynamicBinder".equals(resourceInfo.getResourceMethod().getName())) {
                context.register(filter);
            }
        }
    }

    @Path("/exception")
    public static class ExceptionTestsResource {

        @Path("matched")
        @GET
        @ExceptionTestBound
        public String getPostMatching() {
            return "method";
        }
    }

    @PreMatching
    public static class ExceptionPreMatchRequestFilter implements ContainerRequestFilter {

        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
            if ("exception/not-found".equals(requestContext.getUriInfo().getPath())) {
                throw new TestException("globalRequest");
            }
        }
    }

    @NameBinding
    @Retention(RetentionPolicy.RUNTIME)
    private static @interface ExceptionTestBound {}

    @ExceptionTestBound
    public static class ExceptionPostMatchRequestFilter implements ContainerRequestFilter {

        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
            throw new TestException("nameBoundRequest");
        }
    }

    @ExceptionTestBound
    @Priority(10)
    public static class ExceptionTestBoundResponseFilter implements ContainerResponseFilter {

        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
            responseContext.setEntity(
                    (responseContext.hasEntity()) ? responseContext.getEntity() + "-nameBoundResponse" : "nameBoundResponse",
                    responseContext.getEntityAnnotations(),
                    MediaType.TEXT_PLAIN_TYPE);
        }
    }

    @Priority(1)
    public static class ExceptionTestGlobalResponseFilter implements ContainerResponseFilter {

        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
            if (!requestContext.getUriInfo().getPath().startsWith("exception")) {
                return;
            }

            responseContext.setEntity(
                    (responseContext.hasEntity()) ? responseContext.getEntity() + "-globalResponse" : "globalResponse",
                    responseContext.getEntityAnnotations(),
                    MediaType.TEXT_PLAIN_TYPE);
        }
    }

    public static class TestException extends RuntimeException {

        public TestException(String message) {
            super(message);
        }
    }

    public static class TestExceptionMapper implements ExceptionMapper<TestException> {

        public static final String POSTFIX = "-mappedTestException";

        @Override
        public Response toResponse(TestException exception) {
            return Response.ok(exception.getMessage() + POSTFIX).build();
        }
    }

    @Test
    public void testNameBoundResponseFilterNotInvokedOnPreMatchFilterException() {
        Response r = target("exception").path("not-found").request().get();
        assertEquals(200, r.getStatus());
        assertTrue(r.hasEntity());
        assertEquals("globalRequest-mappedTestException-globalResponse", r.readEntity(String.class));
    }

    @Test
    public void testNameBoundResponseFilterInvokedOnPostMatchFilterException() {
        Response r = target("exception").path("matched").request().get();
        assertEquals(200, r.getStatus());
        assertTrue(r.hasEntity());
        assertEquals("nameBoundRequest-mappedTestException-nameBoundResponse-globalResponse", r.readEntity(String.class));
    }

    @NameBoundAbortResponse
    private static class AbortResponseFitler implements ContainerResponseFilter {

        public static final String ABORT_FILTER_TEST_PASSED = "abort-filter-test-passed";

        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
            boolean passed = true;

            try {
                // checks that IllegalStateException is thrown when setting entity stream
                requestContext.setEntityStream(new InputStream() {
                    @Override
                    public int read() throws IOException {
                        return -1;
                    }
                });
                passed = false;
            } catch (IllegalStateException iae) {
                System.out.println(iae.getMessage());
            }

            try {
                // checks that IllegalStateException is thrown when setting security context
                requestContext.setSecurityContext(new SecurityContext() {
                    @Override
                    public Principal getUserPrincipal() {
                        return null;
                    }

                    @Override
                    public boolean isUserInRole(String role) {
                        return false;
                    }

                    @Override
                    public boolean isSecure() {
                        return false;
                    }

                    @Override
                    public String getAuthenticationScheme() {
                        return null;
                    }
                });
                passed = false;
            } catch (IllegalStateException iae) {
                System.out.println(iae.getMessage());
            }

            try {
                // checks that IllegalStateException is thrown when aborting request
                requestContext.abortWith(Response.serverError().build());
                passed = false;
            } catch (IllegalStateException iae) {
                System.out.println(iae.getMessage());
            }
            responseContext.getHeaders().add(ABORT_FILTER_TEST_PASSED, passed);
        }
    }

    @NameBoundAbortRequest
    private static class AbortRequestFilter implements ContainerRequestFilter {

        public static final String FILTER_ABORT_ENTITY = "filter-abort-entity";

        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
            requestContext.abortWith(Response.ok(FILTER_ABORT_ENTITY).build());
        }
    }

    @NameBoundExceptionInRequest
    private static class ExceptionRequestFilter implements ContainerRequestFilter {

        public static final String EXCEPTION_IN_REQUEST_FILTER = "exception-in-request-filter";

        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
            throw new TestException(EXCEPTION_IN_REQUEST_FILTER);
        }
    }

    @NameBinding
    @Retention(RetentionPolicy.RUNTIME)
    private static @interface NameBoundResponse {}

    @NameBinding
    @Retention(RetentionPolicy.RUNTIME)
    private static @interface NameBoundAbortRequest {}

    @NameBinding
    @Retention(RetentionPolicy.RUNTIME)
    private static @interface NameBoundExceptionInRequest {}

    @Path("abort")
    public static class AbortResponseResource {

        @Path("response")
        @GET
        @NameBoundAbortResponse
        public String get() {
            return "get";
        }

        @Path("abort-in-filter")
        @GET
        @NameBoundAbortResponse
        @NameBoundAbortRequest
        public String neverCalled() {
            return "This method will never be called. Request will be aborted in a request filter.";
        }

        @Path("exception")
        @GET
        @NameBoundAbortResponse
        @NameBoundExceptionInRequest
        public String exception() {
            return "This method will never be called. Exception will be thrown in a request filter.";
        }
    }

    @Test
    public void testAbortResponseInResponseFilter() {
        Response r = target("abort").path("response").request().get();
        assertEquals(200, r.getStatus());
        assertEquals("get", r.readEntity(String.class));
        assertNotNull(r.getHeaderString(AbortResponseFitler.ABORT_FILTER_TEST_PASSED));
        assertEquals("true", r.getHeaderString(AbortResponseFitler.ABORT_FILTER_TEST_PASSED));
    }

    @Test
    public void testAbortAbortedResponseInResponseFilter() {
        Response r = target("abort").path("abort-in-filter").request().get();
        assertEquals(200, r.getStatus());
        assertEquals(AbortRequestFilter.FILTER_ABORT_ENTITY, r.readEntity(String.class));
        assertNotNull(r.getHeaderString(AbortResponseFitler.ABORT_FILTER_TEST_PASSED));
        assertEquals("true", r.getHeaderString(AbortResponseFitler.ABORT_FILTER_TEST_PASSED));
    }

    @Test
    public void testAbortedResponseFromExceptionResponse() {
        Response r = target("abort").path("exception").request().get();
        assertEquals(200, r.getStatus());
        assertEquals(ExceptionRequestFilter.EXCEPTION_IN_REQUEST_FILTER + TestExceptionMapper.POSTFIX,
                r.readEntity(String.class));
        assertNotNull(r.getHeaderString(AbortResponseFitler.ABORT_FILTER_TEST_PASSED));
        assertEquals("true", r.getHeaderString(AbortResponseFitler.ABORT_FILTER_TEST_PASSED));
    }
}
