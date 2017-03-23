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
package org.glassfish.jersey.tests.e2e.client;

import java.io.IOException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.client.ResponseProcessingException;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class ClientFilterTest extends JerseyTest {

    @Path("/test")
    public static class FooResource {

        @GET
        public String get(@Context HttpHeaders headers) {
            return "GET "
                    + headers.getHeaderString(HttpHeaders.COOKIE)
                    + headers.getHeaderString(HttpHeaders.CACHE_CONTROL);
        }
    }

    public static class CustomRequestFilter implements ClientRequestFilter {

        @Override
        public void filter(ClientRequestContext clientRequestContext) throws IOException {
            clientRequestContext.abortWith(Response.ok("OK!", MediaType.TEXT_PLAIN_TYPE).build());
        }
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(FooResource.class);
    }

    @Test
    public void filterAbortTest() {
        final WebTarget target = target();
        target.register(CustomRequestFilter.class);
        final String entity = target.request().get(String.class);

        assertEquals("OK!", entity);
    }

    public static class HeaderProvidersRequestFilter implements ClientRequestFilter {

        @Override
        public void filter(ClientRequestContext clientRequestContext) throws IOException {
            final CacheControl cacheControl = new CacheControl();
            cacheControl.setPrivate(true);
            clientRequestContext.getHeaders().putSingle(HttpHeaders.CACHE_CONTROL, cacheControl);
            clientRequestContext.getHeaders().putSingle(HttpHeaders.COOKIE, new Cookie("cookie", "cookie-value"));
        }
    }

    @Test
    public void filterHeaderProvidersTest() {
        final WebTarget target = target();
        target.register(HeaderProvidersRequestFilter.class).register(LoggingFeature.class);
        final Response response = target.path("test").request().get(Response.class);
        final String entity = response.readEntity(String.class);

        assertTrue(entity.contains("GET"));
        assertTrue(entity.contains("private"));
        assertTrue(entity.contains("cookie-value"));
    }

    public static class IOExceptionResponseFilter implements ClientResponseFilter {

        @Override
        public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws
                IOException {
            throw new IOException(IOExceptionResponseFilter.class.getName());

        }
    }

    @Test
    public void ioExceptionResponseFilterTest() {
        final WebTarget target = target();
        target.register(IOExceptionResponseFilter.class).register(LoggingFeature.class);

        boolean caught = false;

        try {
            target.path("test").request().get(Response.class);
        } catch (ResponseProcessingException e) {
            caught = true;
            assertNotNull(e.getCause());
            assertEquals(IOException.class, e.getCause().getClass());
            assertEquals(IOExceptionResponseFilter.class.getName(), e.getCause().getMessage());
        }

        assertTrue(caught);
    }
}
