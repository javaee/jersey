/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2016 Oracle and/or its affiliates. All rights reserved.
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
import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * JERSEY-2345 Reproducer.
 * <p/>
 * If an invalid WWW-Authenticate header is sent from the server and HttpUrlConnector is used,
 * during processing in the Connector an Exception is thrown (HttpUrlConnection creates an instance of AuthenticateHeader class
 * which tries to parse the header value during construction. If a scheme is missing (header value is one word),
 * the constructor throws an exception.
 * <p/>
 * This cannot be "fixed" in Jersey.
 *
 * @author Adam Lindenthal (adam.lindenthal at oracle.com)
 */
public class AbortingFilterTest extends JerseyTest {
    private static Logger logger = Logger.getLogger(AbortingFilterTest.class.getName());

    @Path("simple")
    public static class SimpleTestResource {
        @GET
        public Response simpleTest() {
            return Response.ok().build();
        }

        @Path("response")
        @GET
        public Response simpleResponseTest() {
            return Response.ok().build();
        }

        @Path("direct")
        @GET
        public Response directResponseTest() {
            return Response.status(Response.Status.UNAUTHORIZED).header("WWW-Authenticate",
                    "oauth_problem=token_rejected").build();
        }
    }

    public static class AbortingFilter implements ContainerRequestFilter {
        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
            final Response.ResponseBuilder builder = Response.status(Response.Status.UNAUTHORIZED);
            builder.header("WWW-Authenticate", "oauth_problem=token_rejected");
            requestContext.abortWith(builder.build());
        }
    }

    public static class AbortingResponseFilter implements ContainerResponseFilter {
        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
            final Response.ResponseBuilder builder = Response.status(Response.Status.UNAUTHORIZED);
            builder.header("WWW-Authenticate", "oauth_problem=token_rejected");
            requestContext.abortWith(builder.build());

        }
    }

    @Override
    protected ResourceConfig configure() {
        enable(TestProperties.LOG_TRAFFIC);
        return new ResourceConfig(FilterDynamicBinding.class, SimpleTestResource.class, LoggingFeature.class);
    }

    /**
     * Try to abort response in the container response filter. However, it's too late and produces an error.
     */
    @Test
    public void testAbortingResponseFilter() {
        final Response response = target().path("simple/response").request().get();
        int status = response.getStatus();
        logger.info("Response status is: " + status);
        assertEquals(status, Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    /**
     * Try to abort response in a container request filter. With an invalid WWW-Authenticate header (according to HTTP specs)
     * and HttpUrlConnector, it throws an exception. This is an original scenario reported by jrh3k5 on java.net
     */
    @Test(expected = ProcessingException.class)
    public void testAbortingFilter() {
        final Response response = target().path("simple").request().get();
        int status = response.getStatus();
        logger.info("Response status is: " + status);
    }

    /**
     * The original reproted scenario with a different Connector.
     */
    @Test
    public void testAbortingFilterWithApacheConnector() {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.connectorProvider(new ApacheConnectorProvider());
        Client client = ClientBuilder.newClient(clientConfig);

        final Response response = client.target(getBaseUri()).path("/simple").request().get();
        int status = response.getStatus();
        logger.info("Response status is: " + status);
        assertEquals(status, Response.Status.UNAUTHORIZED.getStatusCode());
    }

    /**
     * This test shows, that the behaviour is not caused by the use of the filters, but by the response content itself.
     */
    @Test(expected = ProcessingException.class)
    public void testDirectResponse() {
        final Response response = target().path("simple/direct").request().get();
        int status = response.getStatus();
        logger.info("Response status is: " + status);
    }

    /**
     * Dynamically launch specific filters for specific resource methods
     */
    public static class FilterDynamicBinding implements DynamicFeature {
        @Override
        public void configure(ResourceInfo resourceInfo, FeatureContext context) {
            if (SimpleTestResource.class.equals(resourceInfo.getResourceClass())) {
                String methodName = resourceInfo.getResourceMethod().getName();
                if (methodName.contains("simpleResponseTest")) {
                    context.register(AbortingResponseFilter.class);
                    logger.info("Aborting will take place in ResponseFilter.");
                } else if (methodName.contains("simpleTest")) {
                    context.register(AbortingFilter.class);
                    logger.info("Aborting will take place in RequestFilter.");
                } else {
                    logger.info("Response will not be aborted.");
                }
            }
        }
    }
}
