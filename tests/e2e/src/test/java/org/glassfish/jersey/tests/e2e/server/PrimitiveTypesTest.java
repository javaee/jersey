/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;

import junit.framework.Assert;

/**
 * Tests primitive types as entity.
 *
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 */
public class PrimitiveTypesTest extends JerseyTest {
    @Override
    protected ResourceConfig configure() {
        return new ResourceConfig(Resource.class);
    }

    @Test
    public void testInteger() {
        WebTarget web = target().path("test");
        Response response = web.path("Integer").request().post(Entity.entity(new Integer(5), MediaType.TEXT_PLAIN_TYPE));
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(new Integer(6), response.readEntity(Integer.class));
    }

    @Test
    public void testPrimitiveInt() {
        WebTarget web = target().path("test");
        Response response = web.path("int").request().post(Entity.entity(5, MediaType.TEXT_PLAIN_TYPE));
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(new Integer(6), response.readEntity(Integer.class));
    }

    @Test
    public void testPrimitiveIntNull() {
        WebTarget web = target().path("test");
        Response response = web.path("int").request().post(Entity.entity("", MediaType.TEXT_PLAIN_TYPE));
        Assert.assertEquals(400, response.getStatus());
    }


    @Test
    public void testLong() {
        WebTarget web = target().path("test");
        Response response = web.path("Long").request().post(Entity.entity(new Long(5), MediaType.TEXT_PLAIN_TYPE));
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(new Long(6), response.readEntity(Long.class));
    }

    @Test
    public void testPrimitiveLong() {
        WebTarget web = target().path("test");
        Response response = web.path("long").request().post(Entity.entity(5l, MediaType.TEXT_PLAIN_TYPE));
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(6l, (long) response.readEntity(long.class));
    }

    @Test
    public void testPrimitiveLongNull() {
        WebTarget web = target().path("test");
        Response response = web.path("long").request().post(Entity.entity("", MediaType.TEXT_PLAIN_TYPE));
        Assert.assertEquals(400, response.getStatus());
    }


    @Test
    public void testShort() {
        WebTarget web = target().path("test");
        Response response = web.path("Short").request().post(Entity.entity(new Short((short) 5), MediaType.TEXT_PLAIN_TYPE));
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(new Short((short) 6), response.readEntity(Short.class));
    }

    @Test
    public void testPrimitiveShort() {
        WebTarget web = target().path("test");
        Response response = web.path("short").request().post(Entity.entity((short) 5, MediaType.TEXT_PLAIN_TYPE));
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals((short) 6, (short) response.readEntity(short.class));
    }

    @Test
    public void testPrimitiveShortNull() {
        WebTarget web = target().path("test");
        Response response = web.path("short").request().post(Entity.entity("", MediaType.TEXT_PLAIN_TYPE));
        Assert.assertEquals(400, response.getStatus());
    }

    @Test
    public void testByte() {
        WebTarget web = target().path("test");
        Response response = web.path("Byte").request().post(Entity.entity(new Byte((byte) 5), MediaType.TEXT_PLAIN_TYPE));
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(new Byte((byte) 6), response.readEntity(Byte.class));
    }

    @Test
    public void testPrimitiveByte() {
        WebTarget web = target().path("test");
        Response response = web.path("byte").request().post(Entity.entity((byte) 5, MediaType.TEXT_PLAIN_TYPE));
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals((byte) 6, (byte) response.readEntity(byte.class));
    }

    @Test
    public void testPrimitiveByteNull() {
        WebTarget web = target().path("test");
        Response response = web.path("byte").request().post(Entity.entity("", MediaType.TEXT_PLAIN_TYPE));
        Assert.assertEquals(400, response.getStatus());
    }

    @Test
    public void testFloat() {
        WebTarget web = target().path("test");
        Response response = web.path("Float").request().post(Entity.entity(new Float(5), MediaType.TEXT_PLAIN_TYPE));
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(new Float(6), response.readEntity(Float.class));
    }

    @Test
    public void testPrimitiveFloat() {
        WebTarget web = target().path("test");
        Response response = web.path("float").request().post(Entity.entity(5f, MediaType.TEXT_PLAIN_TYPE));
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(6f, response.readEntity(float.class));
    }

    @Test
    public void testPrimitiveFloatNull() {
        WebTarget web = target().path("test");
        Response response = web.path("float").request().post(Entity.entity("", MediaType.TEXT_PLAIN_TYPE));
        Assert.assertEquals(400, response.getStatus());
    }

    @Test
    public void testDouble() {
        WebTarget web = target().path("test");
        Response response = web.path("Double").request().post(Entity.entity(new Double(5), MediaType.TEXT_PLAIN_TYPE));
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(new Double(6), response.readEntity(Double.class));
    }

    @Test
    public void testPrimitiveDouble() {
        WebTarget web = target().path("test");
        Response response = web.path("double").request().post(Entity.entity(5d, MediaType.TEXT_PLAIN_TYPE));
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(6d, response.readEntity(double.class));
    }

    @Test
    public void testPrimitiveDoubleNull() {
        WebTarget web = target().path("test");
        Response response = web.path("double").request().post(Entity.entity("", MediaType.TEXT_PLAIN_TYPE));
        Assert.assertEquals(400, response.getStatus());
    }

    @Test
    public void testCharacter() {
        WebTarget web = target().path("test");
        Response response = web.path("Character").request().post(Entity.entity(new Character('a'), MediaType.TEXT_PLAIN_TYPE));
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(new Character('b'), response.readEntity(Character.class));
    }

    @Test
    public void testPrimitiveCharacter() {
        WebTarget web = target().path("test");
        Response response = web.path("char").request().post(Entity.entity('a', MediaType.TEXT_PLAIN_TYPE));
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals('b', (char) response.readEntity(char.class));
    }

    @Test
    public void testPrimitiveCharacterNull() {
        WebTarget web = target().path("test");
        Response response = web.path("char").request().post(Entity.entity("", MediaType.TEXT_PLAIN_TYPE));
        Assert.assertEquals(400, response.getStatus());
    }

    @Test
    public void testString() {
        WebTarget web = target().path("test");
        Response response = web.path("String").request().post(Entity.entity("String", MediaType.TEXT_PLAIN_TYPE));
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("StringPOST", response.readEntity(String.class));
    }




    @Path("test")
    public static class Resource {
        @GET
        @Path("testik")
        public String test(@QueryParam("id") int id) {
            System.out.println(id);
            return String.valueOf(id);
        }

        @POST
        @Path("Integer")
        public Integer postInteger(Integer i) {
            return i + 1;
        }

        @POST
        @Path("int")
        public int postInt(int i) {
            return i + 1;
        }

        @POST
        @Path("Long")
        public Long postLong(Long l) {
            return l + 1;
        }

        @POST
        @Path("long")
        public long postLong(long l) {
            return l + 1;
        }

        @POST
        @Path("Short")
        public long postShort(Short s) {
            return s + 1;
        }


        @POST
        @Path("short")
        public long postPrimitiveShort(short s) {
            return s + 1;
        }


        @POST
        @Path("Byte")
        public Byte postByte(Byte b) {
            return new Byte((byte) (b + 1));
        }

        @POST
        @Path("byte")
        public byte postPrimitiveByte(byte b) {
            return (byte) (b + 1);
        }


        @POST
        @Path("Float")
        public Float postFloat(Float f) {
            return new Float(f + 1f);
        }


        @POST
        @Path("float")
        public float postPrimitiveFloat(float f) {
            return f + 1;
        }

        @POST
        @Path("Double")
        public Double postDouble(Double d) {
            return new Double(d + 1d);
        }


        @POST
        @Path("double")
        public double postPrimitiveDouble(double d) {
            return d + 1;
        }


        @POST
        @Path("Character")
        public Character postCharacter(Character c) {
            byte b = (byte) (c.charValue());
            b = (byte) (b + 1);
            return new Character((char) b);
        }

        @POST
        @Path("char")
        public char postPrimitiveChar(char c) {
            byte b = (byte) c;
            b = (byte) (b + 1);
            return (char) b;
        }


        @POST
        @Path("String")
        public String postString(String str) {
            return str + "POST";
        }
    }
}
