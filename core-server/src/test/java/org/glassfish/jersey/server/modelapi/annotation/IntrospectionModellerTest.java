/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.server.modelapi.annotation;

import java.util.Collection;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import javax.inject.Inject;

import org.glassfish.jersey.server.model.Parameter;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;

import org.hamcrest.Matchers;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import jersey.repackaged.com.google.common.base.Function;
import jersey.repackaged.com.google.common.collect.Collections2;

/**
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public class IntrospectionModellerTest {

    static String data;

    @Path("/helloworld")
    @Produces(" a/b, c/d ")
    @Consumes({"e/f,g/h", " i/j"})
    public static class HelloWorldResource {

        @POST
        @Consumes(" a/b, c/d ")
        @Produces({"e/f,g/h", " i/j"})
        public String postA(final String data) {
            return data;
        }

        @POST
        public String postB(final String data) {
            return data;
        }
    }

    @Path("/other/{path}")
    public static class OtherResource {

        private String queryParam;

        public String getQueryParam() {
            return queryParam;
        }

        @QueryParam("q")
        public void setQueryParam(final String queryParam) {
            this.queryParam = queryParam;
        }

        @PathParam("path")
        private String pathParam;

        public String getPathParam() {
            return pathParam;
        }

        public void setPathParam(final String pathParam) {
            this.pathParam = pathParam;
        }

        @Context
        UriInfo uriInfo;

        @Inject
        public String annotatedButNotParam;

        @Inject
        public void setAnnotatedButNotParam(final String annotatedButNotParam) {
            this.annotatedButNotParam = annotatedButNotParam;
        }

        @GET
        public String get(@HeaderParam("h") String headerParam) {
            return headerParam + data;
        }

        @POST
        public String post(final String data) {
            return data;
        }
    }

    public IntrospectionModellerTest() {
    }

    @Test
    /**
     * Test of createResource method, of class IntrospectionModeller.
     */
    public void testCreateResource() {
        Class<?> resourceClass;
        Resource result;
        List<ResourceMethod> resourceMethods;
        ResourceMethod resourceMethod;

        // HelloWorldResource
        resourceClass = HelloWorldResource.class;

        result = Resource.builder(resourceClass).build();
        resourceMethods = result.getResourceMethods();
        assertEquals("Unexpected number of resource methods in the resource model.", 2, resourceMethods.size());

        resourceMethod = find(resourceMethods, "postA");
        assertEquals("Unexpected number of produced media types in the resource method model",
                3, resourceMethod.getProducedTypes().size());
        assertEquals("Unexpected number of consumed media types in the resource method model",
                2, resourceMethod.getConsumedTypes().size());
        assertEquals("Unexpected number of handler parameters",
                0, resourceMethod.getInvocable().getHandler().getParameters().size());

        resourceMethod = find(resourceMethods, "postB");
        assertEquals("Unexpected number of inherited produced media types in the resource method model",
                2, resourceMethod.getProducedTypes().size());
        assertEquals("Unexpected number of inherited consumed media types in the resource method model",
                3, resourceMethod.getConsumedTypes().size());
        assertEquals("Unexpected number of handler parameters",
                0, resourceMethod.getInvocable().getHandler().getParameters().size());

        // OtherResource
        resourceClass = OtherResource.class;

        result = Resource.builder(resourceClass).build();
        resourceMethods = result.getResourceMethods();
        assertEquals("Unexpected number of resource methods in the resource model.", 2, resourceMethods.size());

        resourceMethod = find(resourceMethods, "get");
        assertEquals("Unexpected number of produced media types in the resource method model",
                0, resourceMethod.getProducedTypes().size());
        assertEquals("Unexpected number of consumed media types in the resource method model",
                0, resourceMethod.getConsumedTypes().size());
        assertEquals("Unexpected number of handler parameters",
                5, resourceMethod.getInvocable().getHandler().getParameters().size());
        assertSources(resourceMethod.getInvocable().getHandler().getParameters(),
                Parameter.Source.CONTEXT,
                Parameter.Source.PATH,
                Parameter.Source.QUERY,
                Parameter.Source.UNKNOWN, // @Inject on field
                Parameter.Source.UNKNOWN);  // @Inject on setter

        resourceMethod = find(resourceMethods, "post");
        assertEquals("Unexpected number of inherited produced media types in the resource method model",
                0, resourceMethod.getProducedTypes().size());
        assertEquals("Unexpected number of inherited consumed media types in the resource method model",
                0, resourceMethod.getConsumedTypes().size());
        assertEquals("Unexpected number of handler parameters",
                5, resourceMethod.getInvocable().getHandler().getParameters().size());
        assertSources(resourceMethod.getInvocable().getHandler().getParameters(),
                Parameter.Source.CONTEXT,
                Parameter.Source.PATH,
                Parameter.Source.QUERY,
                Parameter.Source.UNKNOWN, // @Inject on field
                Parameter.Source.UNKNOWN);  // @Inject on setter
    }

    private ResourceMethod find(List<ResourceMethod> methods, String javaMethodName) {
        for (ResourceMethod method : methods) {
            if (method.getInvocable().getHandlingMethod().getName().equals(javaMethodName)) {
                return method;
            }
        }

        return null;
    }

    private void assertSources(Collection<Parameter> parameters, Parameter.Source... sources) {
        assertThat("Expected sources not found in the collection",
                Collections2.transform(parameters, new Function<Parameter, Parameter.Source>() {
                    @Override
                    public Parameter.Source apply(final Parameter parameter) {
                        return parameter.getSource();
                    }
                }),
                Matchers.containsInAnyOrder(sources)
        );
    }
}
