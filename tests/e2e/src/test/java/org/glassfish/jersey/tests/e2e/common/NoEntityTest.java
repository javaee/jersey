/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2016 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.e2e.common;

import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.moxy.json.MoxyJsonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.simple.SimpleTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;

import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Tests that no content type is sent when an entity is not present.
 *
 * @author Miroslav Fuksa
 */
public class NoEntityTest extends JerseyTest {

    @Override
    protected Application configure() {
        ResourceConfig resourceConfig = new ResourceConfig(MyResource.class, MoxyJsonFeature.class);
        return resourceConfig;
    }

    @XmlRootElement
    public static class MyEntity {

        @XmlAttribute
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @Path("resource")
    public static class MyResource {

        @GET
        @Produces("application/json")
        @Path("no-entity")
        public Response getNoEntity() {
            return Response.status(204).build();
        }

        @GET
        @Produces("application/json")
        @Path("entity")
        public Response getEntity() {
            MyEntity myEntity = new MyEntity();
            myEntity.setName("hello");
            return Response.status(200).entity(myEntity).build();
        }

        @GET
        @Produces("text/plain")
        @Path("string")
        public Response getEmptyString() {

            return Response.status(204).entity("").build();
        }

    }

    @Override
    protected void configureClient(ClientConfig config) {
        config.register(
                new LoggingFeature(Logger.getLogger(NoEntityTest.class.toString()), LoggingFeature.Verbosity.PAYLOAD_ANY));
    }

    /**
     * Tests that returned media type is null when no entity is sent.
     */
    @Test
    public void testNoEntity() {
        Response response = target().path("resource/no-entity").request(MediaType.APPLICATION_JSON_TYPE).get();
        MyEntity myEntity = response.readEntity(MyEntity.class);
        assertNull(myEntity);
        assertEquals(204, response.getStatus());
        assertNull(response.getMediaType());
    }

    /**
     * Tests that correct media type is returned.
     */
    @Test
    public void testEntity() {
        Response response = target().path("resource/entity").request(MediaType.APPLICATION_JSON_TYPE).get();
        MyEntity myEntity = response.readEntity(MyEntity.class);
        assertEquals("hello", myEntity.getName());
        assertEquals(200, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
    }

    /**
     * Tests that entity is read as null when no entity is sent with 204 response status.
     * Currently this test throws an exception when trying to read the entity.
     * <p/>
     * Exception:
     * <p/>
     * org.glassfish.jersey.message.internal.MessageBodyProviderNotFoundException:
     * MessageBodyReader not found for media type=text/plain,
     * type=class org.glassfish.jersey.tests.e2e.common.NoEntityTest$MyEntity,
     * genericType=class org.glassfish.jersey.tests.e2e.common.NoEntityTest$MyEntity.
     * <p/>
     * https://java.net/jira/browse/JERSEY-1994
     */
    @Test
    @Ignore("see https://java.net/jira/browse/JERSEY-1994")
    public void testNoEntityString() {
        Response response = target().path("resource/string").request().get();
        MyEntity myEntity = response.readEntity(MyEntity.class);
        assertNull(myEntity);
        assertEquals(204, response.getStatus());
        assertEquals("text/plain", response.getMediaType());
    }

    @Override
    protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
        return new SimpleTestContainerFactory();
    }
}
