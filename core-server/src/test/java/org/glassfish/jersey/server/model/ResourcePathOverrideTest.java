/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2014 Oracle and/or its affiliates. All rights reserved.
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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;

import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.RequestContextBuilder;
import org.glassfish.jersey.server.ResourceConfig;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Test of resource path overriding via programmatic API.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class ResourcePathOverrideTest {

    private ApplicationHandler createApplication(ResourceConfig resourceConfig) {
        return new ApplicationHandler(resourceConfig);
    }

    /**
     * Test resource.
     */
    @Path("hello")
    @Produces("text/plain")
    public static class HelloResource {

        @GET
        public String sayHello() {
            return "Hello!";
        }
    }

    @Test
    public void testOverride() throws Exception {
        ResourceConfig resourceConfig = new ResourceConfig(HelloResource.class);


        Resource.Builder resourceBuilder = Resource.builder(HelloResource.class)
                .path("hello2");
        resourceBuilder.addChildResource("world").addMethod("GET").produces("text/plain")
                .handledBy(new Inflector<ContainerRequestContext, String>() {

                    @Override
                    public String apply(ContainerRequestContext request) {
                        return "Hello World!";
                    }
                });

        final Resource resource = resourceBuilder.build();
        resourceConfig.registerResources(resource);

        ApplicationHandler app = createApplication(resourceConfig);

        ContainerRequest request;
        request = RequestContextBuilder.from("/hello", "GET").build();
        assertEquals("Hello!", app.apply(request).get().getEntity());

        request = RequestContextBuilder.from("/hello2", "GET").build();
        assertEquals("Hello!", app.apply(request).get().getEntity());

        request = RequestContextBuilder.from("/hello2/world", "GET").build();
        final ContainerResponse response = app.apply(request).get();
        assertEquals(200, response.getStatus());
        assertEquals("Hello World!", response.getEntity());
    }
}
