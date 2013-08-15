/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.glassfish.jersey.internal.inject.HttpHeadersInjectee;
import org.glassfish.jersey.internal.inject.RequestInjectee;
import org.glassfish.jersey.internal.inject.SecurityContextInjectee;
import org.glassfish.jersey.internal.inject.UriInfoInjectee;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.RequestContextBuilder;

import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Test if request scoped injection points are injected without using
 * dynamic proxies.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public class ProxyInjectablesTest extends AbstractTest {

    private final static Map<Class<?>, Class<?>> InjecteeMap = new HashMap<Class<?>, Class<?>>() {
        {
            put(UriInfo.class, UriInfoInjectee.class);
            put(HttpHeaders.class, HttpHeadersInjectee.class);
            put(Request.class, RequestInjectee.class);
            put(SecurityContext.class, SecurityContextInjectee.class);
        }
    };

    private ContainerResponse resource(String uri) throws Exception {
        return apply(RequestContextBuilder.from(uri, "GET").build());
    }

    @Path("/")
    public static class PerRequestContextInjectedResource {

        @Context UriInfo ui;

        @Context HttpHeaders hs;

        @Context Request r;

        @Context SecurityContext sc;

        @GET
        public String get() {
            assertEquals(InjecteeMap.get(UriInfo.class), ui.getClass());
            assertEquals(InjecteeMap.get(HttpHeaders.class), hs.getClass());
            assertEquals(InjecteeMap.get(Request.class), r.getClass());
            assertEquals(InjecteeMap.get(SecurityContext.class), sc.getClass());
            return "GET";
        }
    }

    @Path("/")
    public static class PerRequestInjectInjectedResource {

        @Inject UriInfo ui;

        @Inject HttpHeaders hs;

        @Inject Request r;

        @Inject SecurityContext sc;

        @GET
        public String get() {
            assertEquals(InjecteeMap.get(UriInfo.class), ui.getClass());
            assertEquals(InjecteeMap.get(HttpHeaders.class), hs.getClass());
            assertEquals(InjecteeMap.get(Request.class), r.getClass());
            assertEquals(InjecteeMap.get(SecurityContext.class), sc.getClass());
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
            assertEquals(InjecteeMap.get(UriInfo.class), ui.getClass());
            assertEquals(InjecteeMap.get(HttpHeaders.class), hs.getClass());
            assertEquals(InjecteeMap.get(Request.class), r.getClass());
            assertEquals(InjecteeMap.get(SecurityContext.class), sc.getClass());
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
            assertEquals(InjecteeMap.get(UriInfo.class), ui.getClass());
            assertEquals(InjecteeMap.get(HttpHeaders.class), hs.getClass());
            assertEquals(InjecteeMap.get(Request.class), r.getClass());
            assertEquals(InjecteeMap.get(SecurityContext.class), sc.getClass());
            return "GET";
        }
    }

    @Test
    // TODO: this needs JERSEY-1967 fix
    @Ignore
    public void testPerRequestContextInjected() throws Exception {
        initiateWebApplication(PerRequestContextInjectedResource.class);

        assertEquals("GET", resource("/").getEntity());
    }

    @Test
    public void testPerRequestInjectInjected() throws Exception {
        initiateWebApplication(PerRequestInjectInjectedResource.class);

        assertEquals("GET", resource("/").getEntity());
    }

    @Test
    // TODO: this needs JERSEY-1967 fix
    @Ignore
    public void testPerRequestMethodParameterInjected() throws Exception {
        initiateWebApplication(PerRequestContextMethodParameterResource.class);

        assertEquals("GET", resource("/").getEntity());
    }
}
