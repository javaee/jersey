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

package org.glassfish.jersey.media.multipart.internal;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Entity;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ResourceConfig;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Test cases for inspecting an {@code FormDataMultiPart} entity in a {@code RequestFilter} and following injection of this
 * entity into a resource method.
 *
 * @author Paul Sandoz
 * @author Michal Gajdos
 */
public class FormDataMultiPartBufferTest extends MultiPartJerseyTest {

    @Override
    protected Application configure() {
        return ((ResourceConfig) super.configure()).registerInstances(new MyFilter());
    }

    @Override
    protected Set<Class<?>> getResourceClasses() {
        return Collections.singleton(ConsumesFormDataResource.class);
    }

    @Provider
    public static class MyFilter implements ContainerRequestFilter {

        @Override
        public void filter(ContainerRequestContext context) throws IOException {
            ((ContainerRequest) context).bufferEntity();

            // Read entity
            FormDataMultiPart multiPart = ((ContainerRequest) context).readEntity(FormDataMultiPart.class);

            assertEquals(3, multiPart.getBodyParts().size());
            assertNotNull(multiPart.getField("foo"));
            assertEquals("bar", multiPart.getField("foo").getValue());
            assertNotNull(multiPart.getField("baz"));
            assertEquals("bop", multiPart.getField("baz").getValue());

            assertNotNull(multiPart.getField("bean"));
            MultiPartBean bean = multiPart.getField("bean").getValueAs(MultiPartBean.class);
            assertEquals("myname", bean.getName());
            assertEquals("myvalue", bean.getValue());

            context.setProperty("filtered", "true");
        }
    }

    @Path("/ConsumesFormDataResource")
    public static class ConsumesFormDataResource {

        @PUT
        @Consumes("multipart/form-data")
        @Produces("text/plain")
        public Response get(@Context ContainerRequest request, FormDataMultiPart multiPart) {
            Object p = request.getProperty("filtered");
            assertNotNull(p);
            assertEquals("true", p);

            if (!(multiPart.getBodyParts().size() == 3)) {
                return Response
                        .ok("FAILED:  Number of body parts is " + multiPart.getBodyParts().size() + " instead of 3")
                        .build();
            }
            if (multiPart.getField("foo") == null) {
                return Response.ok("FAILED:  Missing field 'foo'").build();
            } else if (!"bar".equals(multiPart.getField("foo").getValue())) {
                return Response
                        .ok("FAILED:  Field 'foo' has value '" + multiPart.getField("foo").getValue() + "' instead of"
                                + " 'bar'")
                        .build();
            }
            if (multiPart.getField("baz") == null) {
                return Response.ok("FAILED:  Missing field 'baz'").build();
            } else if (!"bop".equals(multiPart.getField("baz").getValue())) {
                return Response
                        .ok("FAILED:  Field 'baz' has value '" + multiPart.getField("baz").getValue() + "' instead of 'bop'")
                        .build();
            }
            if (multiPart.getField("bean") == null) {
                return Response.ok("FAILED:  Missing field 'bean'").build();
            }
            MultiPartBean bean = multiPart.getField("bean").getValueAs(MultiPartBean.class);
            if (!bean.getName().equals("myname")) {
                return Response.ok("FAILED:  Second part name = " + bean.getName()).build();
            }
            if (!bean.getValue().equals("myvalue")) {
                return Response.ok("FAILED:  Second part value = " + bean.getValue()).build();
            }
            return Response.ok("SUCCESS:  All tests passed").build();
        }

    }

    @Test
    public void testConsumesFormDataResource() {
        MultiPartBean bean = new MultiPartBean("myname", "myvalue");
        FormDataMultiPart entity = new FormDataMultiPart()
            .field("foo", "bar")
            .field("baz", "bop")
            .field("bean", bean, new MediaType("x-application", "x-format"));

        String response = target()
                .path("ConsumesFormDataResource")
                .request("text/plain")
                .put(Entity.entity(entity, "multipart/form-data"), String.class);

        if (!response.startsWith("SUCCESS:")) {
            fail("Response is '" + response + "'");
        }
    }

}
