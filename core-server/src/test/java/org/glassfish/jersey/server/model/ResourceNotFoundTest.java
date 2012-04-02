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

package org.glassfish.jersey.server.model;

import org.glassfish.jersey.message.internal.Requests;
import org.glassfish.jersey.server.JerseyApplication;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.process.Inflector;

import static org.junit.Assert.assertEquals;

/**
 * test for JERSEY-938
 *
 * @author Jakub Podlesak
 */
public class ResourceNotFoundTest {

    JerseyApplication application;

    private JerseyApplication.Builder createApplicationBuilder(Class<?>... classes) {
        final ResourceConfig resourceConfig = new ResourceConfig(classes);
        return JerseyApplication.builder(resourceConfig);
    }


    public static class MyInflector implements Inflector<Request, Response> {
        @Override
        public Response apply(Request data) {
            return Response.ok("dynamic", MediaType.TEXT_PLAIN).build();
        }
    }

    @Path("/foo")
    public static class FooResource {
        @Produces("text/plain")
        @GET
        public String getFoo() {
            return "foo";
        }

        @Path("bar")
        @Produces("text/plain")
        @GET
        public String getBar() {
            return "bar";
        }
    }

    @Test
    public void testExistingDeclarativeResources() throws Exception {
        JerseyApplication app = createApplicationBuilder(FooResource.class).build();

        Response response;

        response = app.apply(Requests.from("/foo","GET").accept("text/plain").build()).get();
        assertEquals(200, response.getStatus());
        assertEquals("foo", response.readEntity(String.class));

        response = app.apply(Requests.from("/foo/bar","GET").accept("text/plain").build()).get();
        assertEquals(200, response.getStatus());
        assertEquals("bar", response.readEntity(String.class));
    }

    @Test
    public void testMissingDeclarativeResources() throws Exception {
        JerseyApplication app = createApplicationBuilder(FooResource.class).build();

        Response response;

        response = app.apply(Requests.from("/foe","GET").accept("text/plain").build()).get();
        assertEquals(404, response.getStatus());

        response = app.apply(Requests.from("/fooe","GET").accept("text/plain").build()).get();
        assertEquals(404, response.getStatus());

        response = app.apply(Requests.from("/foo/baz","GET").accept("text/plain").build()).get();
        assertEquals(404, response.getStatus());

        response = app.apply(Requests.from("/foo/bar/baz","GET").accept("text/plain").build()).get();
        assertEquals(404, response.getStatus());
    }

    private JerseyApplication createMixedApp() {
        JerseyApplication.Builder appBuilder = createApplicationBuilder(FooResource.class);
        appBuilder.bind("/dynamic").method("GET").to(new MyInflector());
        appBuilder.bind("/foo/dynamic").method("GET").to(new MyInflector());
        JerseyApplication app = appBuilder.build();
        return app;
    }

    @Test
    public void testExistingMixedResources() throws Exception {

        JerseyApplication app = createMixedApp();

        Response response;

        response = app.apply(Requests.from("/foo","GET").accept("text/plain").build()).get();
        assertEquals(200, response.getStatus());
        assertEquals("foo", response.readEntity(String.class));

        response = app.apply(Requests.from("/dynamic","GET").accept("text/plain").build()).get();
        assertEquals(200, response.getStatus());
        assertEquals("dynamic", response.readEntity(String.class));

        response = app.apply(Requests.from("/foo/bar","GET").accept("text/plain").build()).get();
        assertEquals(200, response.getStatus());
        assertEquals("bar", response.readEntity(String.class));

        response = app.apply(Requests.from("/foo/dynamic","GET").accept("text/plain").build()).get();
        assertEquals(200, response.getStatus());
        assertEquals("dynamic", response.readEntity(String.class));
    }


    @Test
    public void testMissingMixedResources() throws Exception {

        JerseyApplication app = createMixedApp();

        Response response;

        response = app.apply(Requests.from("/foe","GET").accept("text/plain").build()).get();
        assertEquals(404, response.getStatus());

        response = app.apply(Requests.from("/fooe","GET").accept("text/plain").build()).get();
        assertEquals(404, response.getStatus());

        response = app.apply(Requests.from("/dynamical","GET").accept("text/plain").build()).get();
        assertEquals(404, response.getStatus());

        response = app.apply(Requests.from("/foo/baz","GET").accept("text/plain").build()).get();
        assertEquals(404, response.getStatus());

        response = app.apply(Requests.from("/foo/bar/baz","GET").accept("text/plain").build()).get();
        assertEquals(404, response.getStatus());

        response = app.apply(Requests.from("/foo/dynamic/baz","GET").accept("text/plain").build()).get();
        assertEquals(404, response.getStatus());
    }
}
