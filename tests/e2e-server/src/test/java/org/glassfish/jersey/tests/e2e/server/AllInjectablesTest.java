/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2017 Oracle and/or its affiliates. All rights reserved.
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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;

import javax.inject.Singleton;

import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.server.ExtendedUriInfo;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Paul Sandoz
 */
public class AllInjectablesTest extends JerseyTest {

    @Path("per-request")
    public static class PerRequestContextResource {
        @Context Application app;
        @Context ResourceContext rc;
        @Context Configuration config;
        @Context MessageBodyWorkers mbw;
        @Context HttpHeaders hs;
        @Context UriInfo ui;
        @Context ExtendedUriInfo eui;
        @Context Request r;
        @Context SecurityContext sc;
        @Context Providers p;

        @GET
        public String get() {
            assertNotNull(app);
            assertNotNull(rc);
            assertNotNull(config);
            assertNotNull(mbw);
            assertNotNull(hs);
            assertNotNull(ui);
            assertNotNull(eui);
            assertNotNull(r);
            assertNotNull(sc);
            assertNotNull(p);
            return "GET";
        }
    }

    @Path("per-request-constructor")
    public static class PerRequestContextConstructorParameterResource {
        public PerRequestContextConstructorParameterResource(@Context final Application app, @Context final ResourceContext rc,
                @Context final Configuration config, @Context final MessageBodyWorkers mbw, @Context final HttpHeaders hs,
                @Context final UriInfo ui, @Context final ExtendedUriInfo eui, @Context final Request r,
                @Context final SecurityContext sc, @Context final Providers p) {
            assertNotNull(app);
            assertNotNull(rc);
            assertNotNull(config);
            assertNotNull(mbw);
            assertNotNull(hs);
            assertNotNull(ui);
            assertNotNull(eui);
            assertNotNull(r);
            assertNotNull(sc);
            assertNotNull(p);
        }

        @GET
        public String get() {
            return "GET";
        }
    }

    @Path("per-request-method")
    public static class PerRequestContextMethodParameterResource {
        @GET
        public String get(@Context final Application app, @Context final ResourceContext rc, @Context final Configuration config,
                          @Context final MessageBodyWorkers mbw, @Context final HttpHeaders hs, @Context final UriInfo ui,
                          @Context final ExtendedUriInfo eui, @Context final Request r, @Context final SecurityContext sc,
                          @Context final Providers p) {
            assertNotNull(app);
            assertNotNull(rc);
            assertNotNull(config);
            assertNotNull(mbw);
            assertNotNull(hs);
            assertNotNull(ui);
            assertNotNull(eui);
            assertNotNull(r);
            assertNotNull(sc);
            assertNotNull(p);
            return "GET";
        }
    }

    @Path("singleton")
    @Singleton
    public static class SingletonContextResource {
        @Context Application app;
        @Context ResourceContext rc;
        @Context Configuration config;
        @Context MessageBodyWorkers mbw;
        @Context HttpHeaders hs;
        @Context UriInfo ui;
        @Context ExtendedUriInfo eui;
        @Context Request r;
        @Context SecurityContext sc;
        @Context Providers p;

        @GET
        public String get() {
            assertNotNull(app);
            assertNotNull(rc);
            assertNotNull(config);
            assertNotNull(mbw);
            assertNotNull(hs);
            assertNotNull(ui);
            assertNotNull(eui);
            assertNotNull(r);
            assertNotNull(sc);
            assertNotNull(p);
            return "GET";
        }
    }

    @Path("singleton-constructor")
    public static class SingletonContextConstructorParameterResource {
        public SingletonContextConstructorParameterResource(@Context final Application app, @Context final ResourceContext rc,
                @Context final Configuration config, @Context final MessageBodyWorkers mbw, @Context final HttpHeaders hs,
                @Context final UriInfo ui, @Context final ExtendedUriInfo eui, @Context final Request r,
                @Context final SecurityContext sc, @Context final Providers p) {
            assertNotNull(app);
            assertNotNull(rc);
            assertNotNull(config);
            assertNotNull(mbw);
            assertNotNull(hs);
            assertNotNull(ui);
            assertNotNull(eui);
            assertNotNull(r);
            assertNotNull(sc);
            assertNotNull(p);
        }

        @GET
        public String get() {
            return "GET";
        }
    }

    @Override
    public ResourceConfig configure() {
        return new ResourceConfig(PerRequestContextResource.class, PerRequestContextConstructorParameterResource.class,
                PerRequestContextMethodParameterResource.class, SingletonContextResource.class,
                SingletonContextConstructorParameterResource.class);
    }

    @Test
    public void testPerRequestInjected() throws IOException {
        assertEquals("GET", target().path("/per-request").request().get(String.class));
    }

    @Test
    public void testPerRequestConstructor() throws IOException {
        assertEquals("GET", target().path("/per-request-constructor").request().get(String.class));
    }

    @Test
    public void testPerRequestMethod() throws IOException {
        assertEquals("GET", target().path("/per-request-method").request().get(String.class));
    }

    @Test
    public void testSingleton() throws IOException {
        assertEquals("GET", target().path("/singleton").request().get(String.class));
    }

    @Test
    public void testSingletonConstructor() throws IOException {
        assertEquals("GET", target().path("/singleton-constructor").request().get(String.class));
    }

}
