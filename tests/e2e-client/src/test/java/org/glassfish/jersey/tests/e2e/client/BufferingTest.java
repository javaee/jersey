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

package org.glassfish.jersey.tests.e2e.client;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.RequestEntityProcessing;
import org.glassfish.jersey.grizzly.connector.GrizzlyConnectorProvider;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests chunk encoding and possibility of buffering the entity.
 *
 * @author Miroslav Fuksa
 */
public class BufferingTest extends JerseyTest {

    @Override
    protected Application configure() {
        return new ResourceConfig(MyResource.class, LoggingFeature.class);
    }

    @Path("resource")
    public static class MyResource {
        @POST
        public String getBuffered(@HeaderParam("content-length") String contentLenght,
                                  @HeaderParam("transfer-encoding") String transferEncoding) {
            if (transferEncoding != null && transferEncoding.equals("chunked")) {
                return "chunked";
            }
            return contentLenght;
        }
    }

    @Test
    public void testApacheConnector() {
        testWithBuffering(getApacheConnectorConfig());
        testWithChunkEncodingWithoutPropertyDefinition(getApacheConnectorConfig());
        testWithChunkEncodingWithPropertyDefinition(getApacheConnectorConfig());
        testWithChunkEncodingPerRequest(getApacheConnectorConfig());
        testDefaultOption(getApacheConnectorConfig(), RequestEntityProcessing.CHUNKED);
    }

    @Test
    public void testGrizzlyConnector() {
        testWithBuffering(getGrizzlyConnectorConfig());
        testWithChunkEncodingWithoutPropertyDefinition(getGrizzlyConnectorConfig());
        testWithChunkEncodingWithPropertyDefinition(getGrizzlyConnectorConfig());
        testWithChunkEncodingPerRequest(getGrizzlyConnectorConfig());
        testDefaultOption(getGrizzlyConnectorConfig(), RequestEntityProcessing.CHUNKED);
    }

    @Test
    public void testHttpUrlConnector() {
        testWithBuffering(getHttpUrlConnectorConfig());
        testWithChunkEncodingWithoutPropertyDefinition(getHttpUrlConnectorConfig());
        testWithChunkEncodingWithPropertyDefinition(getHttpUrlConnectorConfig());
        testWithChunkEncodingPerRequest(getHttpUrlConnectorConfig());
        testDefaultOption(getHttpUrlConnectorConfig(), RequestEntityProcessing.BUFFERED);
    }

    private ClientConfig getApacheConnectorConfig() {
        return new ClientConfig().connectorProvider(new ApacheConnectorProvider());
    }

    private ClientConfig getGrizzlyConnectorConfig() {
        return new ClientConfig().connectorProvider(new GrizzlyConnectorProvider());
    }

    private ClientConfig getHttpUrlConnectorConfig() {
        return new ClientConfig();
    }

    private void testDefaultOption(ClientConfig cc, RequestEntityProcessing mode) {
        String entity = getVeryLongString();
        makeRequest(cc, entity, mode == RequestEntityProcessing.BUFFERED ? String.valueOf(entity.length())
                : "chunked");
    }

    private void testWithBuffering(ClientConfig cc) {
        cc.property(ClientProperties.REQUEST_ENTITY_PROCESSING, RequestEntityProcessing.BUFFERED);
        String entity = getVeryLongString();

        makeRequest(cc, entity, String.valueOf(entity.length()));
    }

    private void makeRequest(ClientConfig cc, String entity, String expected) {
        Client client = ClientBuilder.newClient(cc);
        WebTarget target = client.target(UriBuilder.fromUri(getBaseUri()).path("resource").build());

        Response response = target.request().post(Entity.entity(entity, MediaType.TEXT_PLAIN_TYPE));
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(expected, response.readEntity(String.class));
    }

    private void testWithChunkEncodingWithPropertyDefinition(ClientConfig cc) {
        cc.property(ClientProperties.REQUEST_ENTITY_PROCESSING, RequestEntityProcessing.CHUNKED);
        cc.property(ClientProperties.CHUNKED_ENCODING_SIZE, 3000);
        String entity = getVeryLongString();

        makeRequest(cc, entity, "chunked");
    }

    private void testWithChunkEncodingWithoutPropertyDefinition(ClientConfig cc) {
        cc.property(ClientProperties.REQUEST_ENTITY_PROCESSING, RequestEntityProcessing.CHUNKED);
        String entity = getVeryLongString();

        makeRequest(cc, entity, "chunked");
    }


    /**
     * Tests that {@link org.glassfish.jersey.client.ClientProperties#REQUEST_ENTITY_PROCESSING} can be defined
     * per request with different values.
     */
    private void testWithChunkEncodingPerRequest(ClientConfig cc) {
        cc.property(ClientProperties.REQUEST_ENTITY_PROCESSING, RequestEntityProcessing.CHUNKED);

        cc.property(ClientProperties.CHUNKED_ENCODING_SIZE, 3000);
        Client client = ClientBuilder.newClient(cc);
        WebTarget target = client.target(UriBuilder.fromUri(getBaseUri()).path("resource").build());

        String entity = getVeryLongString();
        Response response = target.request().post(Entity.entity(entity, MediaType.TEXT_PLAIN_TYPE));
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("chunked", response.readEntity(String.class));

        response = target.property(ClientProperties.REQUEST_ENTITY_PROCESSING, RequestEntityProcessing.BUFFERED)
                .request().post(Entity.entity(entity, MediaType.TEXT_PLAIN_TYPE));
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(String.valueOf(entity.length()), response.readEntity(String.class));
    }

    public String getVeryLongString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1; i++) {
            sb.append("helllllloooooooooooooooooooooooooooooouuuuuuuuuuu.");
        }
        return sb.toString();
    }
}
