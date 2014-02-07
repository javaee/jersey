/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2014 Oracle and/or its affiliates. All rights reserved.
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

import java.net.URI;
import java.util.List;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.RequestContextBuilder;
import org.glassfish.jersey.server.ResourceConfig;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * Test of programmatic resource method additions.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class ProgrammaticResourceMethodsTest {

    @Test
    public void testGet() throws Exception {
        final ResourceConfig rc = new ResourceConfig();
        final Resource.Builder resourceBuilder = Resource.builder("test");
        resourceBuilder.addMethod("GET").handledBy(new Inflector<ContainerRequestContext, Response>() {

            @Override
            public Response apply(ContainerRequestContext request) {
                return Response.ok().build();
            }
        });
        rc.registerResources(resourceBuilder.build());
        final ApplicationHandler application = new ApplicationHandler(rc);

        checkReturnedStatus(RequestContextBuilder.from("/test", "GET").build(), application);
    }

    @Test
    public void testHead() throws Exception {
        final ResourceConfig rc = new ResourceConfig();
        final Resource.Builder resourceBuilder = Resource.builder("test");
        resourceBuilder.addMethod("HEAD").handledBy(new Inflector<ContainerRequestContext, Response>() {

            @Override
            public Response apply(ContainerRequestContext request) {
                return Response.ok().build();
            }
        });
        rc.registerResources(resourceBuilder.build());
        final ApplicationHandler application = new ApplicationHandler(rc);

        checkReturnedStatus(RequestContextBuilder.from("/test", "HEAD").build(), application);
    }

    @Test
    public void testOptions() throws Exception {
        final ResourceConfig rc = new ResourceConfig();
        final Resource.Builder resourceBuilder = Resource.builder("test");
        resourceBuilder.addMethod("OPTIONS").handledBy(new Inflector<ContainerRequestContext, Response>() {

            @Override
            public Response apply(ContainerRequestContext request) {
                return Response.ok().build();
            }
        });
        rc.registerResources(resourceBuilder.build());
        final ApplicationHandler application = new ApplicationHandler(rc);

        checkReturnedStatus(RequestContextBuilder.from("/test", "OPTIONS").build(), application);
    }

    @Test
    public void testMultiple() throws Exception {
        Inflector<ContainerRequestContext, Response> inflector = new Inflector<ContainerRequestContext, Response>() {

            @Override
            public Response apply(ContainerRequestContext request) {
                return Response.ok().build();
            }
        };

        final ResourceConfig rc = new ResourceConfig();
        final Resource.Builder resourceBuilder = Resource.builder("test");

        resourceBuilder.addMethod("GET").handledBy(inflector);
        resourceBuilder.addMethod("OPTIONS").handledBy(inflector);
        resourceBuilder.addMethod("HEAD").handledBy(inflector);

        rc.registerResources(resourceBuilder.build());
        final ApplicationHandler application = new ApplicationHandler(rc);

        checkReturnedStatus(RequestContextBuilder.from("/test", "GET").build(), application);
        checkReturnedStatus(RequestContextBuilder.from("/test", "HEAD").build(), application);
        checkReturnedStatus(RequestContextBuilder.from("/test", "OPTIONS").build(), application);
    }

    @Test
    public void testTwoBindersSamePath() throws Exception {
        final ResourceConfig rc = new ResourceConfig();
        final Resource.Builder resourceBuilder = Resource.builder("/");
        final Resource.Builder childTest1Builder = resourceBuilder.addChildResource("test1");
        childTest1Builder.addMethod("GET").handledBy(new Inflector<ContainerRequestContext, Response>() {

            @Override
            public Response apply(ContainerRequestContext request) {
                return Response.created(URI.create("/foo")).build();
            }
        });
        Inflector<ContainerRequestContext, Response> inflector1 = new Inflector<ContainerRequestContext, Response>() {

            @Override
            public Response apply(ContainerRequestContext request) {
                return Response.accepted().build();
            }
        };
        final Resource.Builder childTest2Builder = resourceBuilder.addChildResource("test2");
        childTest2Builder.addMethod("GET").handledBy(inflector1);
        childTest2Builder.addMethod("HEAD").handledBy(inflector1);
        Inflector<ContainerRequestContext, Response> inflector2 = new Inflector<ContainerRequestContext, Response>() {

            @Override
            public Response apply(ContainerRequestContext request) {
                return Response.status(203).build();
            }
        };
        childTest1Builder.addMethod("OPTIONS").handledBy(inflector2);
        childTest1Builder.addMethod("HEAD").handledBy(inflector2);
        final Resource resource = resourceBuilder.build();
        rc.registerResources(resource);
        final ApplicationHandler application = new ApplicationHandler(rc);

        checkReturnedStatusEquals(201, RequestContextBuilder.from("/test1", "GET").build(), application);
//        checkReturnedStatusEquals(203, Requests.from("/test1", "HEAD").build(), application);
//        checkReturnedStatusEquals(203, Requests.from("/test1", "OPTIONS").build(), application);

//        checkReturnedStatusEquals(202, Requests.from("/test2", "GET").build(), application);
//        checkReturnedStatusEquals(202, Requests.from("/test2", "HEAD").build(), application);
//        checkReturnedStatusEquals(202, Requests.from("/test2", "OPTIONS").build(), application);
    }

    @Test
    public void testConsumesProduces() {
        final Resource.Builder builder = Resource.builder("root");
        builder.addMethod("POST").handledBy(new Inflector
                <ContainerRequestContext, Object>() {

            @Override
            public Object apply(ContainerRequestContext requestContext) {
                return null;
            }
        }).consumes(MediaType.APPLICATION_XML).consumes("text/html").consumes("text/plain",
                "application/json").produces(MediaType.TEXT_HTML_TYPE,
                MediaType.APPLICATION_JSON_TYPE);
        final Resource res = builder.build();
        final ResourceMethod method = res.getResourceMethods().get(0);
        final List<MediaType> consumedTypes = method.getConsumedTypes();
        assertEquals(4, consumedTypes.size());
        final List<MediaType> producedTypes = method.getProducedTypes();
        assertEquals(2, producedTypes.size());
        assertTrue(consumedTypes.contains(MediaType.APPLICATION_XML_TYPE));
        assertTrue(consumedTypes.contains(MediaType.TEXT_HTML_TYPE));
        assertTrue(consumedTypes.contains(MediaType.TEXT_PLAIN_TYPE));
        assertTrue(consumedTypes.contains(MediaType.APPLICATION_JSON_TYPE));
        assertTrue(producedTypes.contains(MediaType.TEXT_HTML_TYPE));
        assertTrue(producedTypes.contains(MediaType.APPLICATION_JSON_TYPE));
    }

    private void checkReturnedStatus(ContainerRequest req, ApplicationHandler app) throws Exception {
        checkReturnedStatusEquals(200, req, app);
    }

    private void checkReturnedStatusEquals(int expectedStatus, ContainerRequest req, ApplicationHandler app)
            throws Exception {
        final int responseStatus = app.apply(req).get().getStatus();
        assertEquals(expectedStatus, responseStatus);
    }
}
