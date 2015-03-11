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

package org.glassfish.jersey.server;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceTestUtils;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test merging of resources and child resources.
 *
 * @author Miroslav Fuksa
 *
 */
public class ResourceMergeTest {

    @Path("a")
    public static class ResourceA {
        @GET
        public String get() {
            return "get";
        }


        @GET
        @Path("child")
        public String childGet() {
            return "child-get";
        }


        @Path("child")
        public ResourceB getLocator() {
            return new ResourceB();
        }

        @GET
        @Path("child2")
        public String child2Get() {
            return "child2-get";
        }
    }


    @Path("a")
    public static class ResourceB {

        @POST
        public String post() {
            return "post";
        }

        @POST
        @Path("child")
        public String childPost() {
            return "child-post";
        }
    }

    @Path("different-path")
    public static class ResourceC {

        @POST
        public String post() {
            return "post";
        }

        @PUT
        @Path("child")
        public String childPut() {
            return "child-put";
        }

        @Path("locator")
        public ResourceA locator() {
            return new ResourceA();
        }
    }


    @Test
    public void testResourceMerge() {
        final List<Resource> rootResources = createRootResources();
        assertEquals(2, rootResources.size());

        final Resource resourceC = ResourceTestUtils.getResource(rootResources, "different-path");
        ResourceTestUtils.containsExactMethods(resourceC, false, "POST");

        final Resource resourceAB = ResourceTestUtils.getResource(rootResources, "a");
        ResourceTestUtils.containsExactMethods(resourceAB, false, "POST", "GET");
    }

    private List<Resource> createRootResources() {
        final Resource resourceA = Resource.from(ResourceA.class);
        final Resource resourceB = Resource.from(ResourceB.class);
        final ResourceBag.Builder builder = new ResourceBag.Builder();
        builder.registerProgrammaticResource(resourceA);
        builder.registerProgrammaticResource(resourceB);
        builder.registerProgrammaticResource(Resource.from(ResourceC.class));
        final ResourceBag bag = builder.build();
        return bag.getRootResources();
    }


    @Test
    public void testChildResourceMerge() {
        final List<Resource> rootResources = createRootResources();
        assertEquals(2, rootResources.size());
        final Resource resourceAB = ResourceTestUtils.getResource(rootResources, "a");
        assertEquals(2, resourceAB.getChildResources().size());
        final Resource child = ResourceTestUtils.getResource(resourceAB.getChildResources(), "child");
        final Resource child2 = ResourceTestUtils.getResource(resourceAB.getChildResources(), "child2");

        ResourceTestUtils.containsExactMethods(child, true, "GET", "POST");
        ResourceTestUtils.containsExactMethods(child2, false, "GET");


        final Resource resourceC = ResourceTestUtils.getResource(rootResources, "different-path");
        final List<Resource> childResourcesC = resourceC.getChildResources();
        assertEquals(2, childResourcesC.size());
        final Resource childC1 = ResourceTestUtils.getResource(childResourcesC, "child");
        ResourceTestUtils.containsExactMethods(childC1, false, "PUT");

        final Resource childC2 = ResourceTestUtils.getResource(childResourcesC, "locator");
        ResourceTestUtils.containsExactMethods(childC2, true);

        child.getResourceMethods().size();
    }

    public static class MyInflector implements Inflector<ContainerRequestContext, Object> {


        @Override
        public Object apply(ContainerRequestContext requestContext) {
            return null;
        }
    }


    @Test
    public void programmaticTest() {
        final List<Resource> rootResources = getResourcesFromProgrammatic();

        assertEquals(1, rootResources.size());
        final Resource root = ResourceTestUtils.getResource(rootResources, "root");
        final List<Resource> childResources = root.getChildResources();
        assertEquals(2, childResources.size());
        final Resource child = ResourceTestUtils.getResource(childResources, "child");
        ResourceTestUtils.containsExactMethods(child, true, "GET", "POST", "DELETE");
        final Resource child2 = ResourceTestUtils.getResource(childResources, "child2");
        ResourceTestUtils.containsExactMethods(child2, false, "PUT");
    }

    private List<Resource> getResourcesFromProgrammatic() {
        final Resource.Builder root = Resource.builder("root");
        root.addChildResource("child").addMethod("GET").handledBy(new MyInflector());
        root.addChildResource("child").addMethod("POST").handledBy(new MyInflector());
        root.addChildResource("child2").addMethod("PUT").handledBy(new MyInflector());

        final Resource.Builder root2 = Resource.builder("root");
        root2.addChildResource("child").addMethod("DELETE").handledBy(new MyInflector());
        root2.addChildResource("child").addMethod((String) null).handledBy(new MyInflector());

        final ResourceBag.Builder builder = new ResourceBag.Builder();
        builder.registerProgrammaticResource(root.build());
        builder.registerProgrammaticResource(root2.build());
        final ResourceBag bag = builder.build();
        return bag.getRootResources();
    }


    @Test
    public void mergeTwoLocatorsTest() {
        final Resource.Builder root = Resource.builder("root");
        root.addChildResource("child").addMethod().handledBy(new MyInflector()).consumes(MediaType.APPLICATION_XML_TYPE);
        root.addChildResource("child").addMethod().handledBy(new MyInflector()).consumes(MediaType.APPLICATION_JSON_TYPE);
        final ResourceBag.Builder builder = new ResourceBag.Builder();
        try {
            builder.registerProgrammaticResource(root.build());
            final ResourceBag bag = builder.build();
            fail("Should fail - two locators on the same path.");
        } catch (Exception e) {
            // ok - should fail
        }
    }


    @Path("root/{a}")
    public static class ResourceTemplateA {
        @GET
        @Path("{q}")
        public String get() {
            return "get";
        }

        @PUT
        @Path("{q}")
        public String put() {
            return "put";
        }

        @POST
        @Path("{post}")
        public String post() {
            return "post";
        }


    }

    @Path("root/{b}")
    public static class ResourceTemplateB {
        @GET
        public String getB() {
            return "get-B";
        }
    }


    @Test
    public void testMergingOfTemplates() {
        final List<Resource> resources = createResources(ResourceTemplateA.class, ResourceTemplateB.class);
        testMergingTemplateResources(resources);
    }

    @Test
    public void testMergingOfTemplatesProgrammatic() {
        final List<Resource> resources = getResourcesTemplatesProgrammatic();
        testMergingTemplateResources(resources);
    }

    private void testMergingTemplateResources(List<Resource> resources) {
        assertEquals(2, resources.size());
        final Resource resB = ResourceTestUtils.getResource(resources, "root/{b}");
        ResourceTestUtils.containsExactMethods(resB, false, "GET");
        final Resource resA = ResourceTestUtils.getResource(resources, "root/{a}");

        assertTrue(resA.getResourceMethods().isEmpty());
        final List<Resource> childResources = resA.getChildResources();
        assertEquals(2, childResources.size());
        final Resource childQ = ResourceTestUtils.getResource(childResources, "{q}");
        ResourceTestUtils.containsExactMethods(childQ, false, "GET", "PUT");
        final Resource childPost = ResourceTestUtils.getResource(childResources, "{post}");
        ResourceTestUtils.containsExactMethods(childPost, false, "POST");
    }

    private List<Resource> createResources(Class<?>... resourceClass) {
        final ResourceBag.Builder builder = new ResourceBag.Builder();
        for (Class<?> clazz : resourceClass) {
            final Resource resource = Resource.from(clazz);
            builder.registerProgrammaticResource(resource);
        }
        final ResourceBag bag = builder.build();
        return bag.getRootResources();
    }

    private List<Resource> getResourcesTemplatesProgrammatic() {
        final Resource.Builder root = Resource.builder("root/{a}");
        root.addChildResource("{q}").addMethod("GET").handledBy(new MyInflector());
        root.addChildResource("{q}").addMethod("PUT").handledBy(new MyInflector());
        root.addChildResource("{post}").addMethod("POST").handledBy(new MyInflector());

        final Resource.Builder root2 = Resource.builder("root/{b}");
        root2.addMethod("GET").handledBy(new MyInflector());

        final ResourceBag.Builder builder = new ResourceBag.Builder();
        builder.registerProgrammaticResource(root.build());
        builder.registerProgrammaticResource(root2.build());
        final ResourceBag bag = builder.build();
        return bag.getRootResources();
    }

}



