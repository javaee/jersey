/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.ws.rs.BindingPriority;
import javax.ws.rs.GET;
import javax.ws.rs.NameBinding;
import javax.ws.rs.Path;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import deprecated.javax.ws.rs.DynamicBinder;
import deprecated.javax.ws.rs.PostMatching;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * @author Martin Matula (martin.matula at oracle.com)
 */
public class ResourceFilterTest extends JerseyTest {
    @Override
    protected Application configure() {
        return new ResourceConfig(
                MyDynamicBinder.class,
                MyResource.class,
                NameBoundRequestFilter.class,
                NameBoundResponseFilter.class,
                PostMatchingFilter.class
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

    private void test(String name) {
        Response r = target(name).request().get();
        assertEquals(200, r.getStatus());
        assertEquals(name, r.readEntity(String.class));
    }

    @Path("/")
    public static class MyResource {
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
    private static @interface NameBoundResponse {}

    @NameBoundRequest
    @BindingPriority(1)
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

    @PostMatching
    public static class PostMatchingFilter implements ContainerRequestFilter {
        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
            requestContext.abortWith(Response.ok("postMatching", MediaType.TEXT_PLAIN_TYPE).build());
        }
    }

    @BindingPriority(1)
    private static class DbFilter implements ContainerRequestFilter {
        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
            requestContext.abortWith(Response.ok("dynamicBinder", MediaType.TEXT_PLAIN_TYPE).build());
        }
    }

    public static class MyDynamicBinder implements DynamicBinder<DbFilter> {
        private final DbFilter filter = new DbFilter();

        @Override
        public DbFilter getBoundProvider(ResourceInfo resourceInfo) {
            if ("getDynamicBinder".equals(resourceInfo.getResourceMethod().getName())) {
                return filter;
            }
            return null;
        }
    }
}
