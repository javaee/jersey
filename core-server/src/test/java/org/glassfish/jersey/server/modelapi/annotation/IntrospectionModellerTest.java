/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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

import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.model.ResourceModelIssue;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

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

    public IntrospectionModellerTest() {
    }


    /**
     * Test of createResource method, of class IntrospectionModeller.
     */
    @Test
    public void testCreateResource() {
        Class<?> resourceClass = HelloWorldResource.class;
        Resource result = Resource.builder(resourceClass).build();
        final List<ResourceMethod> resourceMethods = result.getResourceMethods();
        assertEquals("Unexpected number of resource methods in the resource model.", 2, resourceMethods.size());

        ResourceMethod resourceMethod;
        resourceMethod = find(resourceMethods, "postA");
        assertEquals("Unexpected number of produced media types in the resource method model",
                3, resourceMethod.getProducedTypes().size());
        assertEquals("Unexpected number of consumed media types in the resource method model",
                2, resourceMethod.getConsumedTypes().size());

        resourceMethod = find(resourceMethods, "postB");
        assertEquals("Unexpected number of inherited produced media types in the resource method model",
                2, resourceMethod.getProducedTypes().size());
        assertEquals("Unexpected number of inherited consumed media types in the resource method model",
                3, resourceMethod.getConsumedTypes().size());
    }

    private ResourceMethod find(List<ResourceMethod> methods, String javaMethodName) {
        for (ResourceMethod method : methods) {
            if (method.getInvocable().getHandlingMethod().getName().equals(javaMethodName)) {
                return method;
            }
        }

        return null;
    }
}
