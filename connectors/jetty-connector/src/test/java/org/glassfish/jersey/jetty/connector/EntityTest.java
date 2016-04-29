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
package org.glassfish.jersey.jetty.connector;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import javax.xml.bind.annotation.XmlRootElement;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Tests the Http content negotiation.
 *
 * @author Arul Dhesiaseelan (aruld at acm.org)
 */
public class EntityTest extends JerseyTest {

    private static final Logger LOGGER = Logger.getLogger(EntityTest.class.getName());

    private static final String PATH = "test";

    @Path("/test")
    public static class EntityResource {

        @GET
        public Person get() {
            return new Person("John", "Doe");
        }

        @POST
        public Person post(Person entity) {
            return entity;
        }

    }

    @XmlRootElement
    public static class Person {

        private String firstName;
        private String lastName;

        public Person() {
        }

        public Person(String firstName, String lastName) {
            this.firstName = firstName;
            this.lastName = lastName;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        @Override
        public String toString() {
            return firstName + " " + lastName;
        }
    }

    @Override
    protected Application configure() {
        ResourceConfig config = new ResourceConfig(EntityResource.class, JacksonFeature.class);
        config.register(new LoggingFeature(LOGGER, LoggingFeature.Verbosity.PAYLOAD_ANY));
        return config;
    }

    @Override
    protected void configureClient(ClientConfig config) {
        config.connectorProvider(new JettyConnectorProvider())
                .register(JacksonFeature.class);
    }

    @Test
    public void testGet() {
        Response response = target(PATH).request(MediaType.APPLICATION_XML_TYPE).get();
        Person person = response.readEntity(Person.class);
        assertEquals("John Doe", person.toString());
        response = target(PATH).request(MediaType.APPLICATION_JSON_TYPE).get();
        person = response.readEntity(Person.class);
        assertEquals("John Doe", person.toString());
    }

    @Test
    public void testGetAsync() throws ExecutionException, InterruptedException {
        Response response = target(PATH).request(MediaType.APPLICATION_XML_TYPE).async().get().get();
        Person person = response.readEntity(Person.class);
        assertEquals("John Doe", person.toString());
        response = target(PATH).request(MediaType.APPLICATION_JSON_TYPE).async().get().get();
        person = response.readEntity(Person.class);
        assertEquals("John Doe", person.toString());
    }

    @Test
    public void testPost() {
        Response response = target(PATH).request(MediaType.APPLICATION_XML_TYPE).post(Entity.xml(new Person("John", "Doe")));
        Person person = response.readEntity(Person.class);
        assertEquals("John Doe", person.toString());
        response = target(PATH).request(MediaType.APPLICATION_JSON_TYPE).post(Entity.xml(new Person("John", "Doe")));
        person = response.readEntity(Person.class);
        assertEquals("John Doe", person.toString());
    }

    @Test
    public void testPostAsync() throws ExecutionException, InterruptedException, TimeoutException {
        Response response = target(PATH).request(MediaType.APPLICATION_XML_TYPE).async()
                .post(Entity.xml(new Person("John", "Doe"))).get();
        Person person = response.readEntity(Person.class);
        assertEquals("John Doe", person.toString());
        response = target(PATH).request(MediaType.APPLICATION_JSON_TYPE).async().post(Entity.xml(new Person("John", "Doe")))
                .get();
        person = response.readEntity(Person.class);
        assertEquals("John Doe", person.toString());
    }
}
