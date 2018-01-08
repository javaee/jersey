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

package org.glassfish.jersey.tests.e2e.server;

import java.io.IOException;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.internal.inject.PerLookup;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

/**
 * Test resources which also acts as providers.
 *
 * @author Miroslav Fuksa
 */
public class SingletonProvidersResourcesTest extends JerseyTest {

    @Override
    protected Application configure() {
        ResourceConfig resourceConfig = new ResourceConfig(ResourceSingleton.class, ResourceNotSingleton.class);

        final Resource.Builder resourceBuilder = Resource.builder();
        resourceBuilder.name("programmatic").path("programmatic").addMethod("GET")
                .handledBy(ResourceProgrammaticNotSingleton.class);
        resourceConfig.registerResources(resourceBuilder.build());

        return resourceConfig;
    }

    @Test
    public void testResourceAsFilter() {
        String str = target().path("singleton").request().header("singleton", "singleton").get(String.class);
        assertTrue(str, str.startsWith("true/"));
        String str2 = target().path("singleton").request().header("singleton", "singleton").get(String.class);
        assertTrue(str2, str2.startsWith("true/"));
        assertEquals(str, str2);
    }

    @Test
    public void testResourceAsFilterAnnotatedPerLookup() {
        String str = target().path("perlookup").request().header("not-singleton", "not-singleton").get(String.class);
        assertTrue(str.startsWith("false/"));
        String str2 = target().path("perlookup").request().header("not-singleton", "not-singleton").get(String.class);
        assertTrue(str2.startsWith("false/"));
        assertNotSame(str, str2);
    }

    @Test
    public void testResourceProgrammatic() {
        String str = target().path("programmatic").request().header("programmatic", "programmatic").get(String.class);
        assertTrue(str.startsWith("false/"));
        String str2 = target().path("programmatic").request().header("programmatic", "programmatic").get(String.class);
        assertTrue(str2.startsWith("false/"));
        assertNotSame(str, str2);
    }

    // this should be singleton, it means the same instance for the usage as a filter and as an resource
    @Path("singleton")
    public static class ResourceSingleton implements ContainerRequestFilter {

        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
            if (requestContext.getHeaders().containsKey("singleton")) {
                requestContext.getHeaders().add("filter-class", this.toString());
            }
        }

        @GET
        public String get(@HeaderParam("filter-class") String filterClass) {
            return String.valueOf(String.valueOf(this.toString().equals(filterClass)) + "/" + this.toString() + ":"
                    + filterClass);
        }
    }

    // this should NOT be singleton, because it is annotated as per lookup
    @Path("perlookup")
    @PerLookup
    public static class ResourceNotSingleton implements ContainerRequestFilter {

        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
            if (requestContext.getHeaders().containsKey("not-singleton")) {
                requestContext.getHeaders().add("filter-class", this.toString());
            }
        }

        @GET
        public String get(@HeaderParam("filter-class") String filterClass) {
            return String.valueOf(String.valueOf(this.toString().equals(filterClass)) + "/" + this.toString() + ":"
                    + filterClass);
        }
    }

    // should not be a singleton as this is only programmatic resource and is not registered as provider
    public static class ResourceProgrammaticNotSingleton implements ContainerRequestFilter,
                                                                    Inflector<Request, Response> {

        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
            if (requestContext.getHeaders().containsKey("programmatic")) {
                requestContext.getHeaders().add("filter-class", this.toString());
            }
        }

        @Override                            //JerseyContainerRequestContext
        public Response apply(Request request) {
            String filterClass = ((ContainerRequestContext) request).getHeaders().getFirst("filter-class");
            return Response.ok(String.valueOf(String.valueOf(this.toString().equals(filterClass)) + "/" + this.toString() + ":"
                    + filterClass)).build();
        }
    }

}
