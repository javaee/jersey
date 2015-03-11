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
package org.glassfish.jersey.server.internal.process;

import java.io.IOException;
import java.security.Principal;

import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.RequestContextBuilder;
import org.glassfish.jersey.server.internal.inject.AbstractTest;
import org.glassfish.jersey.server.internal.routing.UriRoutingContext;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Test if request scoped injection points are injected without using
 * dynamic proxies.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public class ProxyInjectablesTest extends AbstractTest {

    public static final Class<RequestContextBuilder.TestContainerRequest> REQUEST_CLASS = RequestContextBuilder
            .TestContainerRequest.class;

    private ContainerResponse resource(String uri) throws Exception {
        return apply(RequestContextBuilder.from(uri, "GET").build());
    }

    @Provider
    public static class SecurityContextFilter implements ContainerRequestFilter {

        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
            requestContext.setSecurityContext(new MySecurityContext());
        }
    }

    private static class MySecurityContext implements SecurityContext {

        @Override
        public Principal getUserPrincipal() {
            return new Principal() {
                @Override
                public String getName() {
                    return "a";
                }
            };
        }

        @Override
        public boolean isUserInRole(String role) {
            return true;
        }

        @Override
        public boolean isSecure() {
            return false;
        }

        @Override
        public String getAuthenticationScheme() {
            return "BASIC";
        }
    }

    @Path("/")
    public static class PerRequestContextInjectedResource {

        @Context
        UriInfo ui;

        @Context
        HttpHeaders hs;

        @Context
        Request r;

        @Context
        SecurityContext sc;

        @GET
        public String get() {
            assertEquals(UriRoutingContext.class, ui.getClass());
            assertEquals(REQUEST_CLASS, hs.getClass());
            assertEquals(REQUEST_CLASS, r.getClass());
            assertEquals(SecurityContextInjectee.class, sc.getClass());
            assertEquals("a", sc.getUserPrincipal().getName());
            return "GET";
        }
    }

    @Path("/")
    @Singleton
    public static class SingletonInjectedResource {

        @Inject
        UriInfo ui;

        @Inject
        HttpHeaders hs;

        @Inject
        Request r;

        @Inject
        SecurityContext sc;

        @GET
        public String get() {
            assertNotEquals(UriRoutingContext.class, ui.getClass());
            assertNotEquals(REQUEST_CLASS, hs.getClass());
            assertNotEquals(REQUEST_CLASS, r.getClass());
            assertNotEquals(MySecurityContext.class, sc.getClass());
            assertEquals("a", sc.getUserPrincipal().getName());
            return "GET";
        }
    }

    @Path("/")
    public static class PerRequestContextMethodParameterResource {

        @GET
        public String get(
                @Context UriInfo ui,
                @Context HttpHeaders hs,
                @Context Request r,
                @Context SecurityContext sc) {
            assertEquals(UriRoutingContext.class, ui.getClass());
            assertEquals(REQUEST_CLASS, hs.getClass());
            assertEquals(REQUEST_CLASS, r.getClass());
            assertEquals(SecurityContextInjectee.class, sc.getClass());
            assertEquals("a", sc.getUserPrincipal().getName());
            return "GET";
        }
    }

    @Path("/")
    @Singleton
    public static class SingletonContextMethodParameterResource {

        @GET
        public String get(
                @Context UriInfo ui,
                @Context HttpHeaders hs,
                @Context Request r,
                @Context SecurityContext sc) {
            assertEquals(UriRoutingContext.class, ui.getClass());
            assertEquals(REQUEST_CLASS, hs.getClass());
            assertEquals(REQUEST_CLASS, r.getClass());
            assertEquals(SecurityContextInjectee.class, sc.getClass());
            assertEquals("a", sc.getUserPrincipal().getName());
            return "GET";
        }
    }

    @Test
    public void testPerRequestContextInjected() throws Exception {
        initiateWebApplication(PerRequestContextInjectedResource.class, SecurityContextFilter.class);

        assertEquals("GET", resource("/").getEntity());
    }

    @Test
    public void testPerRequestInjectInjected() throws Exception {
        initiateWebApplication(SingletonInjectedResource.class, SecurityContextFilter.class);

        assertEquals("GET", resource("/").getEntity());
    }

    @Test
    public void testPerRequestMethodParameterInjected() throws Exception {
        initiateWebApplication(PerRequestContextMethodParameterResource.class, SecurityContextFilter.class);

        assertEquals("GET", resource("/").getEntity());
    }

    @Test
    public void testSingletonParameterInjected() throws Exception {
        initiateWebApplication(SingletonContextMethodParameterResource.class, SecurityContextFilter.class);

        assertEquals("GET", resource("/").getEntity());
    }

    /**
     * Part of JERSEY-2386 reproducer. The request field
     * should not get injected as a dynamic proxy.
     */
    public static class MyFieldInjectedBean {

        @Context
        Request request;
    }

    /**
     * Part of JERSEY-2386 reproducer. The request field
     * should not get injected as a dynamic proxy.
     */
    public static class MyCtorInjectedBean {

        /**
         * This should get directly injected.
         */
        public MyCtorInjectedBean(@Context Request request) {
            this.request = request;
        }

        Request request;
    }

    /**
     * JERSEY-2386 reproducer. Bean parameter below must
     * get injected directly as well as its internal field.
     */
    @Path("/")
    public static class BeanParamInjectionResource {

        @GET
        @Path("field")
        public String getViaField(@BeanParam MyFieldInjectedBean bean) {
            assertEquals(MyFieldInjectedBean.class, bean.getClass());
            assertEquals(REQUEST_CLASS, bean.request.getClass());
            return "field";
        }

        @GET
        @Path("ctor")
        public String getViaCtor(@BeanParam MyCtorInjectedBean bean) {
            assertEquals(MyCtorInjectedBean.class, bean.getClass());
            assertEquals(REQUEST_CLASS, bean.request.getClass());
            return "ctor";
        }
    }

    /**
     * JERSEY-2386 reproducer. Make sure no dynamic proxy gets involved
     * when injecting into a bean parameter.
     */
    @Test
    public void testBeanParam() throws Exception {
        initiateWebApplication(BeanParamInjectionResource.class);

        assertEquals("field", resource("/field").getEntity());
        assertEquals("ctor", resource("/ctor").getEntity());
    }
}
