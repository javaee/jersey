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
package org.glassfish.jersey.server.model;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.RequestContextBuilder;
import org.glassfish.jersey.server.ResourceConfig;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Paul Sandoz
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public class OptionsSubResourceMethodTest {

    @Target({ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @HttpMethod("PATCH")
    public @interface PATCH {
    }

    private ApplicationHandler app;

    private void initiateWebApplication(Class<?>... classes) {
        app = new ApplicationHandler(new ResourceConfig(classes));
    }

    @Path("/")
    public static class ResourceNoOptions {

        @Path("sub")
        @GET
        public String get() {
            return "GET";
        }

        @Path("sub")
        @PUT
        public String put(String e) {
            return "PUT";
        }

        @Path("sub")
        @POST
        public String post(String e) {
            return "POST";
        }

        @Path("sub")
        @DELETE
        public void delete() {
        }

        @Path("sub")
        @PATCH
        public String patch(String e) {
            return "PATCH";
        }
    }

    @Test
    public void testNoOptions() throws Exception {
        initiateWebApplication(ResourceNoOptions.class);

        ContainerResponse response = app.apply(RequestContextBuilder.from("/sub", "OPTIONS").build()).get();

        assertEquals(200, response.getStatus());
        _checkAllowContent(response.getHeaderString("Allow"));

        response = app.apply(RequestContextBuilder.from("/sub", "OPTIONS").accept(MediaType.TEXT_HTML).build()).get();

        assertEquals(200, response.getStatus());
        _checkAllowContent(response.getHeaderString("Allow"));
    }

    private void _checkAllowContent(final String allow) {
        assertTrue(allow.contains("OPTIONS"));
        assertTrue(allow.contains("GET"));
        assertTrue(allow.contains("PUT"));
        assertTrue(allow.contains("POST"));
        assertTrue(allow.contains("DELETE"));
        assertTrue(allow.contains("PATCH"));

    }

    @Path("/")
    public static class ResourceWithOptions {

        @Path("sub")
        @OPTIONS
        public Response options() {
            return Response.ok("OPTIONS")
                    .header("Allow", "OPTIONS, GET, PUT, POST, DELETE, PATCH").build();
        }

        @Path("sub")
        @GET
        public String get() {
            return "GET";
        }

        @Path("sub")
        @PUT
        public String put(String e) {
            return "PUT";
        }

        @Path("sub")
        @POST
        public String post(String e) {
            return "POST";
        }

        @Path("sub")
        @DELETE
        public void delete() {
        }

        @Path("sub")
        @PATCH
        public String patch(String e) {
            return "PATCH";
        }
    }

    @Test
    public void testWithOptions() throws Exception {

        initiateWebApplication(ResourceWithOptions.class);

        ContainerResponse response = app.apply(RequestContextBuilder.from("/sub", "OPTIONS").build()).get();

        assertEquals(200, response.getStatus());

        String allow = response.getHeaderString("Allow");
        assertTrue(allow.contains("OPTIONS"));
        assertTrue(allow.contains("GET"));
        assertTrue(allow.contains("PUT"));
        assertTrue(allow.contains("POST"));
        assertTrue(allow.contains("DELETE"));
        assertTrue(allow.contains("PATCH"));
    }

    @Path("/")
    public static class ResourceNoOptionsDifferentSub {

        @Path("sub1")
        @GET
        public String getFoo() {
            return "FOO";
        }

        @Path("sub2")
        @PUT
        public String putBar() {
            return "BAR";
        }
    }

    @Test
    public void testNoOptionsDifferentSub() throws Exception {

        initiateWebApplication(ResourceNoOptionsDifferentSub.class);

        ContainerResponse response = app.apply(RequestContextBuilder.from("/sub1", "OPTIONS").build()).get();

        assertEquals(200, response.getStatus());

        String allow = response.getHeaderString("Allow");
        assertTrue(allow.contains("OPTIONS"));
        assertTrue(allow.contains("GET"));
        assertFalse(allow.contains("PUT"));

        response = app.apply(RequestContextBuilder.from("/sub2", "OPTIONS").build()).get();

        assertEquals(200, response.getStatus());

        allow = response.getHeaderString("Allow");
        assertTrue(allow.contains("OPTIONS"));
        assertTrue(allow.contains("PUT"));
        assertFalse(allow.contains("GET"));
    }
}
