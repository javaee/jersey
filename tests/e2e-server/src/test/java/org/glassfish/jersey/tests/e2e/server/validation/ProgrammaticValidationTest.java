/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.e2e.server.validation;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.client.Entity;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Bean Validation tests for programmatically created resources.
 *
 * @author Michal Gajdos
 */
public class ProgrammaticValidationTest extends JerseyTest {

    @Override
    protected Application configure() {
        final Set<Resource> resources = new HashSet<>();

        Resource.Builder resourceBuilder = Resource.builder("instance");
        resourceBuilder
                .addMethod("POST")
                .handledBy(new ValidationInflector());
        resources.add(resourceBuilder.build());

        resourceBuilder = Resource.builder("class");
        resourceBuilder
                .addMethod("POST")
                .handledBy(ValidationInflector.class);
        resources.add(resourceBuilder.build());

        try {
            resourceBuilder = Resource.builder("methodInstanceClass");
            resourceBuilder
                    .addMethod("POST")
                    .handledBy(new ValidationInflector(), ValidationInflector.class.getMethod("get",
                            ContainerRequestContext.class));
            resources.add(resourceBuilder.build());
        } catch (NoSuchMethodException e) {
            throw new RuntimeException();
        }

        try {
            resourceBuilder = Resource.builder("methodClassClass");
            resourceBuilder
                    .addMethod("POST")
                    .handledBy(ValidationInflector.class,
                            ValidationInflector.class.getMethod("get", ContainerRequestContext.class));
            resources.add(resourceBuilder.build());
        } catch (NoSuchMethodException e) {
            throw new RuntimeException();
        }

        try {
            resourceBuilder = Resource.builder("methodInstanceInterface");
            resourceBuilder
                    .addMethod("POST")
                    .handledBy(new ValidationInflector(), Inflector.class.getMethod("apply", Object.class));
            resources.add(resourceBuilder.build());
        } catch (NoSuchMethodException e) {
            throw new RuntimeException();
        }

        try {
            resourceBuilder = Resource.builder("methodClassInterface");
            resourceBuilder
                    .addMethod("POST")
                    .handledBy(ValidationInflector.class, Inflector.class.getMethod("apply", Object.class));
            resources.add(resourceBuilder.build());
        } catch (NoSuchMethodException e) {
            throw new RuntimeException();
        }

        return new ResourceConfig().register(LoggingFeature.class).registerResources(resources);
    }

    @Test
    public void testInflectorInstance() throws Exception {
        final Response response = target("instance").request().post(Entity.entity("value", MediaType.TEXT_PLAIN_TYPE));

        assertEquals(200, response.getStatus());
        assertEquals("value", response.readEntity(String.class));
    }

    @Test
    public void testInflectorInstanceNegative() throws Exception {
        final Response response = target("instance").request().post(Entity.entity(null, MediaType.TEXT_PLAIN_TYPE));

        assertEquals(500, response.getStatus());
    }

    @Test
    public void testInflectorClass() throws Exception {
        final Response response = target("class").request().post(Entity.entity("value", MediaType.TEXT_PLAIN_TYPE));

        assertEquals(200, response.getStatus());
        assertEquals("value", response.readEntity(String.class));
    }

    @Test
    public void testInflectorClassNegative() throws Exception {
        final Response response = target("class").request().post(Entity.entity(null, MediaType.TEXT_PLAIN_TYPE));

        assertEquals(500, response.getStatus());
    }

    @Test
    public void testInflectorMethodInstanceClass() throws Exception {
        final Response response = target("methodInstanceClass").request().post(Entity.entity("value", MediaType.TEXT_PLAIN_TYPE));

        assertEquals(200, response.getStatus());
        assertEquals("value", response.readEntity(String.class));
    }

    @Test
    public void testInflectorMethodInstanceClassNegative() throws Exception {
        final Response response = target("methodInstanceClass").request().post(Entity.entity(null, MediaType.TEXT_PLAIN_TYPE));

        assertEquals(500, response.getStatus());
    }

    @Test
    public void testInflectorMethodClassClass() throws Exception {
        final Response response = target("methodClassClass").request().post(Entity.entity("value", MediaType.TEXT_PLAIN_TYPE));

        assertEquals(200, response.getStatus());
        assertEquals("value", response.readEntity(String.class));
    }

    @Test
    public void testInflectorMethodClassClassNegative() throws Exception {
        final Response response = target("methodClassClass").request().post(Entity.entity(null, MediaType.TEXT_PLAIN_TYPE));

        assertEquals(500, response.getStatus());
    }

    @Test
    public void testInflectorMethodInstanceInterface() throws Exception {
        final Response response = target("methodInstanceInterface").request()
                .post(Entity.entity("value", MediaType.TEXT_PLAIN_TYPE));

        assertEquals(200, response.getStatus());
        assertEquals("value", response.readEntity(String.class));
    }

    @Test
    public void testInflectorMethodInstanceInterfaceNegative() throws Exception {
        final Response response = target("methodInstanceInterface").request()
                .post(Entity.entity(null, MediaType.TEXT_PLAIN_TYPE));

        assertEquals(500, response.getStatus());
    }

    @Test
    public void testInflectorMethodClassInterface() throws Exception {
        final Response response = target("methodClassInterface").request()
                .post(Entity.entity("value", MediaType.TEXT_PLAIN_TYPE));

        assertEquals(200, response.getStatus());
        assertEquals("value", response.readEntity(String.class));
    }

    @Test
    public void testInflectorMethodClassInterfaceNegative() throws Exception {
        final Response response = target("methodClassInterface").request().post(Entity.entity(null, MediaType.TEXT_PLAIN_TYPE));

        assertEquals(500, response.getStatus());
    }
}
