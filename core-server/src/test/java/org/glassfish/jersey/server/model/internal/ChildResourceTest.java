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
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package org.glassfish.jersey.server.model.internal;

import java.util.concurrent.ExecutionException;

import javax.ws.rs.container.ContainerRequestContext;

import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.RequestContextBuilder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.uri.PathPattern;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * @author Miroslav Fuksa
 *
 */
public class ChildResourceTest {

    @Test
    public void testRootResource() throws ExecutionException, InterruptedException {
        ApplicationHandler applicationHandler = createApplication();
        final ContainerResponse response = applicationHandler.apply(RequestContextBuilder.from("/root", "GET").build()).get();
        assertEquals(200, response.getStatus());
        assertEquals("root-get", response.getEntity());
    }

    @Test
    public void testChildResource() throws ExecutionException, InterruptedException {
        ApplicationHandler applicationHandler = createApplication();
        final ContainerResponse response = applicationHandler.apply(RequestContextBuilder.from("/root/child",
                "GET").build()).get();
        assertEquals(200, response.getStatus());
        assertEquals("sub-get", response.getEntity());
    }

    @Test
    public void testAnotherChildResource() throws ExecutionException, InterruptedException {
        ApplicationHandler applicationHandler = createApplication();
        final ContainerResponse response = applicationHandler.apply(RequestContextBuilder.from("/root/another-child",
                "GET").build()).get();
        assertEquals(200, response.getStatus());
        assertEquals("another-child-get", response.getEntity());
    }

    private ApplicationHandler createApplication() {
        final Resource.Builder rootBuilder = Resource.builder("root");

        rootBuilder.addMethod("GET").handledBy(new Inflector<ContainerRequestContext, String>() {
            @Override
            public String apply(ContainerRequestContext requestContext) {
                return "root-get";
            }
        });

        rootBuilder.addChildResource("child").addMethod("GET").handledBy(new Inflector<ContainerRequestContext, String>() {
            @Override
            public String apply(ContainerRequestContext requestContext) {
                return "sub-get";
            }
        }).build();

        Resource.Builder anotherChildBuilder = Resource.builder("another-child");
        anotherChildBuilder.addMethod("GET").handledBy(new Inflector<ContainerRequestContext, String>() {
            @Override
            public String apply(ContainerRequestContext requestContext) {
                return "another-child-get";
            }
        });
        rootBuilder.addChildResource(anotherChildBuilder.build());



        Resource resource = rootBuilder.build();
        ResourceConfig resourceConfig = new ResourceConfig().registerResources(resource);

        return new ApplicationHandler(resourceConfig);
    }


    @Test
    public void test() {
        process("http://localhost/{adas}/aa/f");
        process("http://localhost/{aaa}/aa/f");


    }

    private void process(String str) {
        PathPattern pattern = new PathPattern(str);

        System.out.println("template: " + pattern.getTemplate().toString());
        System.out.println("pattern: " + pattern.toString());
    }


}
