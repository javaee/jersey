/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.server.filter;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import javax.annotation.Priority;

import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.RequestContextBuilder;
import org.glassfish.jersey.server.ResourceConfig;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class ContainerResponseFilterOrderingTest {

    @Test
    public void testResponseFilter() throws ExecutionException, InterruptedException {
        ApplicationHandler handler = new ApplicationHandler(
                new ResourceConfig(Resource.class, ResponseFilter1.class, ResponseFilter2.class, ResponseFilter3.class));
        ContainerResponse res = handler.apply(RequestContextBuilder.from("", "/resource/", "GET").build()).get();
        assertEquals(200, res.getStatus());
    }

    @Provider
    @Priority(1)
    public static class ResponseFilter1 implements ContainerResponseFilter {

        public static Boolean called = false;

        @Override
        public void filter(ContainerRequestContext requestContext,
                           ContainerResponseContext responseContext) throws IOException {
            called = true;

            assertTrue(ResponseFilter3.called);
            assertTrue(ResponseFilter2.called);
        }
    }

    @Provider
    @Priority(2)
    public static class ResponseFilter2 implements ContainerResponseFilter {

        public static Boolean called = false;

        @Override
        public void filter(ContainerRequestContext requestContext,
                           ContainerResponseContext responseContext) throws IOException {
            called = true;

            assertTrue(ResponseFilter3.called);
            assertFalse(ResponseFilter1.called);
        }
    }

    @Provider
    @Priority(3)
    public static class ResponseFilter3 implements ContainerResponseFilter {

        public static Boolean called = false;

        @Override
        public void filter(ContainerRequestContext requestContext,
                           ContainerResponseContext responseContext) throws IOException {
            called = true;

            assertFalse(ResponseFilter1.called);
            assertFalse(ResponseFilter2.called);
        }
    }

    @Path("resource")
    public static class Resource {

        @GET
        public Response get() {
            return Response.ok().build();
        }
    }
}
