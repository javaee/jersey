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
package org.glassfish.jersey.tests.e2e.server;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import javax.xml.bind.annotation.XmlRootElement;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.internal.HttpUrlConnector;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests determining media type of the response (especially that qs quality parameter is respected when
 * more media types are defined on the resource method).
 *
 * @author Miroslav Fuksa
 */
public class ContentNegotiationTest extends JerseyTest {

    @Path("persons")
    public static class MyResource {
        private static final Person[] LIST = new Person[] {
                new Person("Penny", 1),
                new Person("Howard", 2),
                new Person("Sheldon", 3)
        };

        @GET
        @Produces({"application/xml;qs=0.75", "application/json;qs=1.0"})
        public Person[] getList() {
            return LIST;
        }

        @GET
        @Produces({"application/json;qs=1", "application/xml;qs=0.75"})
        @Path("reordered")
        public Person[] getListReordered() {
            return LIST;
        }

        @GET
        @Produces({"application/json;qs=0.75", "application/xml;qs=1"})
        @Path("inverted")
        public Person[] getListInverted() {
            return LIST;
        }


        @GET
        @Produces({"application/xml;qs=0.75", "application/json;qs=0.9", "unknown/hello;qs=1.0"})
        @Path("unkownMT")
        public Person[] getListWithUnkownType() {
            return LIST;
        }

        @GET
        @Produces({"application/json", "application/xml", "text/plain"})
        @Path("shouldPickFirstJson")
        public Person[] getJsonArrayUnlessOtherwiseSpecified() {
            return LIST;
        }

        @GET
        @Produces({"application/xml", "text/plain", "application/json"})
        @Path("shouldPickFirstXml")
        public Person[] getXmlUnlessOtherwiseSpecified() {
            return LIST;
        }

        @GET
        @Produces("application/json;qs=0.75")
        @Path("twoMethodsOneEndpoint")
        public Person[] getJsonArray() {
            return LIST;
        }

        @GET
        @Produces("application/xml;qs=1")
        @Path("twoMethodsOneEndpoint")
        public Person[] getXml() {
            return LIST;
        }
    }

    @XmlRootElement
    public static class Person {
        private String name;
        private int age;

        public Person() {
        }

        public Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        @Override
        public String toString() {
            return name + "(" + age + ")";
        }
    }

    @Override
    protected void configureClient(ClientConfig config) {
        config.register(LoggingFeature.class);
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(MyResource.class);
    }

    /**
     * {@link HttpUrlConnector} by default adds some media types
     * to the Accept header if we don't specify them.
     */
    @Test
    public void testWithoutDefinedRequestedMediaType() {
        WebTarget target = target().path("/persons");
        Response response = target.request().get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
    }

    @Test
    public void testWithoutDefinedRequestedMediaTypeAndTwoMethods() {
        //We can not rely on method declaration ordering:
        //From Class javadoc: "The elements in the returned array are not sorted and are not in any particular order."
        //If there are same endpoints it is necessary to use quality parameter to ensure ordering.
        Response response = target().path("/persons/twoMethodsOneEndpoint").request().get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(MediaType.APPLICATION_XML_TYPE, response.getMediaType());
    }

    @Test
    public void testWithoutDefinedRequestedMediaTypeOrQualityModifiersJson() {
        Response response = target().path("/persons/shouldPickFirstJson").request().get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
    }

    @Test
    public void testWithoutDefinedRequestedMediaTypeOrQualityModifiersXml() {
        Response response = target().path("/persons/shouldPickFirstXml").request().get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(MediaType.APPLICATION_XML_TYPE, response.getMediaType());
    }

    @Test
    public void test() {
        WebTarget target = target().path("/persons");
        Response response = target.request(MediaType.WILDCARD).get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
    }

    @Test
    public void testInverted() {
        WebTarget target = target().path("/persons/inverted");
        Response response = target.request(MediaType.WILDCARD).get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(MediaType.APPLICATION_XML_TYPE, response.getMediaType());
    }

    @Test
    public void testInvertedWithJSONPreferredByClient() {
        WebTarget target = target().path("/persons/inverted");
        Response response = target.request("application/json;q=1.0", "application/xml;q=0.8").get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
    }

    @Test
    public void testReordered() {
        WebTarget target = target().path("/persons/reordered");
        Response response = target.request(MediaType.WILDCARD).get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
    }

    /**
     * Client and server prefers "unknown/hello" but there is no MBW on server to write such a type. Therefore
     * this type is ignored and "application/xml" is chosen (because it is the second preferred type by the client).
     */
    @Test
    public void testWithUnknownTypePreferredByClient() {
        WebTarget target = target().path("/persons/reordered");
        Response response = target.request("application/json;q=0.8", "application/xml;q=0.9",
                "unknown/hello;qs=1.0").get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(MediaType.APPLICATION_XML_TYPE, response.getMediaType());
    }
}
