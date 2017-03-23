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

package org.glassfish.jersey.tests.e2e.json;

import java.io.StringReader;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonStructure;

import org.glassfish.jersey.jsonp.JsonProcessingFeature;
import org.glassfish.jersey.server.JSONP;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.glassfish.jersey.test.util.runner.ConcurrentRunner;

import org.junit.Test;
import org.junit.runner.RunWith;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author Michal Gajdos
 */
@RunWith(ConcurrentRunner.class)
public class JsonProcessingTest extends JerseyTest {

    private static final String JSON_OBJECT_STR = "{\"foo\":\"bar\"}";
    private static final String JSON_ARRAY_STR = "[" + JSON_OBJECT_STR + "," + JSON_OBJECT_STR + "]";
    private static final String JSON_ARRAY_VALUE_STR = "[null]";

    private static final JsonObject JSON_OBJECT = Json.createReader(new StringReader(JSON_OBJECT_STR)).readObject();
    private static final JsonArray JSON_ARRAY = Json.createReader(new StringReader(JSON_ARRAY_STR)).readArray();
    private static final JsonArray JSON_ARRAY_VALUE = Json.createReader(new StringReader(JSON_ARRAY_VALUE_STR)).readArray();

    @Path("/")
    public static class Resource {

        @POST
        @Path("jsonObject")
        public JsonObject postJsonObject(final JsonObject jsonObject) {
            return jsonObject;
        }

        @POST
        @Path("jsonStructure")
        public JsonStructure postJsonStructure(final JsonStructure jsonStructure) {
            return jsonStructure;
        }

        @POST
        @Path("jsonArray")
        public JsonArray postJsonArray(final JsonArray jsonArray) {
            return jsonArray;
        }

        @GET
        @JSONP
        @Path("jsonObjectWithPadding")
        @Produces("application/javascript")
        public JsonObject getJsonObjectWithPadding() {
            return JSON_OBJECT;
        }
    }

    @Override
    protected Application configure() {
        enable(TestProperties.DUMP_ENTITY);
        enable(TestProperties.LOG_TRAFFIC);

        return new ResourceConfig(Resource.class)
                // Make sure to disable auto-discovery (MOXy, BeanValidation, ...) and register ValidationFeature.
                .property(ServerProperties.FEATURE_AUTO_DISCOVERY_DISABLE, true)
                .register(JsonProcessingFeature.class);
    }

    @Test
    public void testJsonObject() throws Exception {
        final Response response = target("jsonObject").request(MediaType.APPLICATION_JSON).post(Entity.json(JSON_OBJECT));

        assertEquals(JSON_OBJECT, response.readEntity(JsonObject.class));
    }

    @Test
    public void testJsonObjectAsString() throws Exception {
        final Response response = target("jsonObject").request(MediaType.APPLICATION_JSON).post(Entity.json(JSON_OBJECT_STR));

        assertEquals(JSON_OBJECT, response.readEntity(JsonObject.class));
    }

    @Test
    public void testJsonObjectPlus() throws Exception {
        final Response response = target("jsonObject").request("application/foo+json").post(Entity.json(JSON_OBJECT));

        assertEquals(JSON_OBJECT, response.readEntity(JsonObject.class));
    }

    @Test
    public void testJsonObjectAsStringPlus() throws Exception {
        final Response response = target("jsonObject").request("application/foo+json").post(Entity.json(JSON_OBJECT_STR));

        assertEquals(JSON_OBJECT, response.readEntity(JsonObject.class));
    }

    @Test
    public void testJsonObjectWrongTarget() throws Exception {
        final Response response = target("jsonArray").request(MediaType.APPLICATION_JSON).post(Entity.json(JSON_OBJECT));

        assertEquals(500, response.getStatus());
    }

    @Test
    public void testJsonObjectAsStringWrongTarget() throws Exception {
        final Response response = target("jsonArray").request(MediaType.APPLICATION_JSON).post(Entity.json(JSON_OBJECT_STR));

        assertEquals(500, response.getStatus());
    }

    @Test
    public void testJsonObjectWrongEntity() throws Exception {
        final Response response = target("jsonObject").request(MediaType.APPLICATION_JSON).post(Entity.json(JSON_ARRAY));

        assertEquals(500, response.getStatus());
    }

    @Test
    public void testJsonObjectAsStringWrongEntity() throws Exception {
        final Response response = target("jsonObject").request(MediaType.APPLICATION_JSON).post(Entity.json(JSON_ARRAY_STR));

        assertEquals(500, response.getStatus());
    }

    @Test
    public void testJsonObjectWrongMediaType() throws Exception {
        final Response response = target("jsonObject").request(MediaType.APPLICATION_OCTET_STREAM).post(Entity.json(JSON_OBJECT));

        assertEquals(500, response.getStatus());
    }

    @Test
    public void testJsonObjectAsStringWrongMediaType() throws Exception {
        final Response response = target("jsonObject").request(MediaType.APPLICATION_OCTET_STREAM)
                .post(Entity.json(JSON_OBJECT_STR));

        assertEquals(500, response.getStatus());
    }

    @Test
    public void testJsonArray() throws Exception {
        final Response response = target("jsonArray").request(MediaType.APPLICATION_JSON).post(Entity.json(JSON_ARRAY));

        assertEquals(JSON_ARRAY, response.readEntity(JsonArray.class));
    }

    @Test
    public void testJsonArrayAsString() throws Exception {
        final Response response = target("jsonArray").request(MediaType.APPLICATION_JSON).post(Entity.json(JSON_ARRAY_STR));

        assertEquals(JSON_ARRAY, response.readEntity(JsonArray.class));
    }

    @Test
    public void testJsonArrayPlus() throws Exception {
        final Response response = target("jsonArray").request("application/foo+json").post(Entity.json(JSON_ARRAY));

        assertEquals(JSON_ARRAY, response.readEntity(JsonArray.class));
    }

    @Test
    public void testJsonArrayAsStringPlus() throws Exception {
        final Response response = target("jsonArray").request("application/foo+json").post(Entity.json(JSON_ARRAY_STR));

        assertEquals(JSON_ARRAY, response.readEntity(JsonArray.class));
    }

    @Test
    public void testJsonArrayWrongTarget() throws Exception {
        final Response response = target("jsonObject").request(MediaType.APPLICATION_JSON).post(Entity.json(JSON_ARRAY));

        assertEquals(500, response.getStatus());
    }

    @Test
    public void testJsonArrayAsStringWrongTarget() throws Exception {
        final Response response = target("jsonObject").request(MediaType.APPLICATION_JSON).post(Entity.json(JSON_ARRAY_STR));

        assertEquals(500, response.getStatus());
    }

    @Test
    public void testJsonArrayWrongEntity() throws Exception {
        final Response response = target("jsonArray").request(MediaType.APPLICATION_JSON).post(Entity.json(JSON_OBJECT));

        assertEquals(500, response.getStatus());
    }

    @Test
    public void testJsonArrayAsStringWrongEntity() throws Exception {
        final Response response = target("jsonArray").request(MediaType.APPLICATION_JSON).post(Entity.json(JSON_OBJECT_STR));

        assertEquals(500, response.getStatus());
    }

    @Test
    public void testJsonArrayWrongMediaType() throws Exception {
        final Response response = target("jsonArray").request(MediaType.APPLICATION_OCTET_STREAM).post(Entity.json(JSON_ARRAY));

        assertEquals(500, response.getStatus());
    }

    @Test
    public void testJsonArraytAsStringWrongMediaType() throws Exception {
        final Response response = target("jsonArray").request(MediaType.APPLICATION_OCTET_STREAM)
                .post(Entity.json(JSON_ARRAY_STR));

        assertEquals(500, response.getStatus());
    }

    @Test
    public void testJsonArrayValueEntity() throws Exception {
        final Response response = target("jsonArray").request(MediaType.APPLICATION_JSON).post(Entity.json(JSON_ARRAY_VALUE));

        assertEquals(JSON_ARRAY_VALUE, response.readEntity(JsonArray.class));
    }

    @Test
    public void testJsonStructureArray() throws Exception {
        final Response response = target("jsonStructure").request(MediaType.APPLICATION_JSON).post(Entity.json(JSON_ARRAY));

        assertEquals(JSON_ARRAY, response.readEntity(JsonStructure.class));
    }

    @Test
    public void testJsonStructureObject() throws Exception {
        final Response response = target("jsonStructure").request(MediaType.APPLICATION_JSON).post(Entity.json(JSON_OBJECT));

        assertEquals(JSON_OBJECT, response.readEntity(JsonStructure.class));
    }

    @Test
    public void testJsonObjectWithPadding() throws Exception {
        final Response response = target("jsonObjectWithPadding").request("application/javascript").get();

        assertThat(response.getStatus(), is(200));
        assertThat(response.readEntity(String.class), is(JSONP.DEFAULT_CALLBACK + "(" + JSON_OBJECT_STR + ")"));
    }
}
