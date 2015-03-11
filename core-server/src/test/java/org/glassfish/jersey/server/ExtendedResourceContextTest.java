/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.server;

import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;

import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceModel;
import org.glassfish.jersey.server.model.ResourceTestUtils;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Test (@link ExtendedResourceContext extended resource context}.
 *
 * @author Miroslav Fuksa
 */
public class ExtendedResourceContextTest {

    private static Resource getResource(List<Resource> resources, String path) {
        for (Resource resource : resources) {
            if (resource.getPath().equals(path)) {
                return resource;
            }
        }
        fail("Resource with path '" + path + "' is not in the list of resources " + resources + "!");
        return null;
    }

    @Path("a")
    public static class ResourceA {
        @Context
        ExtendedResourceContext resourceContext;

        @GET
        public String get() {
            return "get";
        }

        @GET
        @Path("child")
        public String childGet() {
            return "child-get";
        }

        @GET
        @Path("model")
        public String model() {
            final ResourceModel resourceModel = resourceContext.getResourceModel();
            final List<Resource> resources = resourceModel.getRootResources();
            final Resource a = getResource(resources, "a");
            ResourceTestUtils.containsMethod(a, "GET");
            ResourceTestUtils.containsMethod(a, "POST");

            final Resource b = getResource(resources, "b");
            ResourceTestUtils.containsMethod(b, "GET");

            final Resource q = getResource(resources, "b");
            ResourceTestUtils.containsMethod(q, "GET");


            assertEquals(3, resources.size());

            return "ok";
        }

    }

    @Path("a")
    public static class ResourceASecond {
        @POST
        public String post(String post) {
            return "post";
        }
    }

    @Path("b")
    public static class ResourceB {
        @GET
        public String get() {
            return "get";
        }

        @GET
        @Path("child")
        public String childGet() {
            return "child-get";
        }
    }


    @Test
    public void testExtendedResourceContext() throws ExecutionException, InterruptedException {
        ApplicationHandler applicationHandler = new ApplicationHandler(new ResourceConfig(ResourceA.class, ResourceASecond.class,
                ResourceB.class));
        final ContainerResponse response = applicationHandler.apply(RequestContextBuilder.from("/a/model", "GET").build()).get();
        assertEquals("ok", response.getEntity());

    }


}
