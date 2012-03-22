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

import org.glassfish.jersey.message.internal.Requests;
import org.glassfish.jersey.server.Application;
import org.glassfish.jersey.server.ResourceConfig;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Paul Sandoz
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public class OptionsTest {

    @Target({ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @HttpMethod("patch")
    public @interface PATCH {
    }

    private Application app;

    private void initiateWebApplication(Class<?>... rc) {
        final ResourceConfig resourceConfig = ResourceConfig.builder().addClasses(rc).build();

        app = Application.builder(resourceConfig).build();
    }

    @Path("/")
    static public class ResourceNoOptions {
        @GET
        public String get() {
            return "GET";
        }

        @PUT
        public String put(String e) {
            return "PUT";
        }

        @POST
        public String post(String e) {
            return "POST";
        }

        @DELETE
        public void delete() {
        }

        @PATCH
        public String patch(String e) {
            return "PATCH";
        }
    }

    @Test
    public void testNoOptions() throws Exception {
        initiateWebApplication(ResourceNoOptions.class);

        Response response = app.apply(Requests.from("/", "OPTIONS").build()).get();
        assertEquals(200, response.getStatus());
        _checkAllowContent(response.getHeaders().getHeader("Allow").toString());

        final MediaType mediaType = MediaType.APPLICATION_XML_TYPE;
        response = app.apply(Requests.from("/", "OPTIONS").accept(mediaType).build()).get();
        assertEquals(200, response.getStatus());
        assertEquals(mediaType, response.getHeaders().getMediaType());
        assertEquals(0, response.getHeaders().getLength());
        _checkAllowContent(response.getHeaders().getHeader("Allow").toString());
    }

    private void _checkAllowContent(String allow) {
        assertTrue(allow.contains("OPTIONS"));
        assertTrue(allow.contains("GET"));
        assertTrue(allow.contains("PUT"));
        assertTrue(allow.contains("POST"));
        assertTrue(allow.contains("DELETE"));
        assertTrue(allow.contains("PATCH"));
    }

    @Path("/")
    static public class ResourceWithOptions {

        @OPTIONS
        public Response options() {
            return Response.ok("OPTIONS").
                    header("Allow", "OPTIONS, GET, PUT, POST, DELETE, PATCH").
                    header("X-TEST", "OVERRIDE").build();
        }

        @GET
        public String get() {
            return "GET";
        }

        @PUT
        public String put(String e) {
            return "PUT";
        }

        @POST
        public String post(String e) {
            return "POST";
        }

        @DELETE
        public void delete() {
        }

        @PATCH
        public String patch(String e) {
            return "PATCH";
        }
    }

    @Test
    public void testWithOptions() throws Exception {
        initiateWebApplication(ResourceWithOptions.class);

        Response response = app.apply(Requests.from("/", "OPTIONS").build()).get();

        assertEquals(200, response.getStatus());

        String allow = response.getHeaders().getHeader("Allow").toString();
        assertTrue(allow.contains("OPTIONS"));
        assertTrue(allow.contains("GET"));
        assertTrue(allow.contains("PUT"));
        assertTrue(allow.contains("POST"));
        assertTrue(allow.contains("DELETE"));
        assertTrue(allow.contains("PATCH"));

        assertEquals("OVERRIDE", response.getHeaders().getHeader("X-TEST"));
    }
}
