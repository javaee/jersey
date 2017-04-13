/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.e2e.client.connector;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.PATCH;
import javax.ws.rs.Path;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.spi.ConnectorProvider;
import org.glassfish.jersey.grizzly.connector.GrizzlyConnectorProvider;
import org.glassfish.jersey.jdk.connector.JdkConnectorProvider;
import org.glassfish.jersey.jetty.connector.JettyConnectorProvider;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.netty.connector.NettyConnectorProvider;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import static org.junit.Assert.assertEquals;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
@RunWith(Parameterized.class)
public class HttpPatchTest extends JerseyTest {

    private static final Logger LOGGER = Logger.getLogger(RequestHeaderModificationsTest.class.getName());

    @Parameterized.Parameters(name = "{index}: {0}")
    public static List<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                // {HttpUrlConnectorProvider.class}, // cannot process PATCH without additional configuration
                {GrizzlyConnectorProvider.class},
                {JettyConnectorProvider.class}, // unstable.
                {ApacheConnectorProvider.class},
                {GrizzlyConnectorProvider.class},
                {NettyConnectorProvider.class},
                {JdkConnectorProvider.class},
                });
    }

    private final ConnectorProvider connectorProvider;

    public HttpPatchTest(Class<? extends ConnectorProvider> connectorProviderClass)
            throws IllegalAccessException, InstantiationException {
        this.connectorProvider = connectorProviderClass.newInstance();
    }

    @Override
    protected Application configure() {
        set(TestProperties.RECORD_LOG_LEVEL, Level.WARNING.intValue());
        enable(TestProperties.LOG_TRAFFIC);
        return new ResourceConfig(PatchResource.class)
                .register(new LoggingFeature(LOGGER, LoggingFeature.Verbosity.HEADERS_ONLY));
    }

    @Override
    protected void configureClient(ClientConfig clientConfig) {
        clientConfig.connectorProvider(connectorProvider);
    }

    @Test
    public void testPatchResponse() throws Exception {
        Response response = target().request().patch(Entity.text("patch"));

        assertEquals(200, response.getStatus());
        assertEquals("patch", response.readEntity(String.class));
    }

    @Test
    public void testPatchEntity() throws Exception {
        String response = target().request().patch(Entity.text("patch"), String.class);

        assertEquals("patch", response);
    }

    @Test
    public void testPatchGenericType() throws Exception {
        String response = target().request().patch(Entity.text("patch"), new GenericType<String>() {
        });

        assertEquals("patch", response);
    }

    @Test
    public void testAsyncPatchResponse() throws Exception {
        Future<Response> response = target().request().async().patch(Entity.text("patch"));

        assertEquals(200, response.get().getStatus());
        assertEquals("patch", response.get().readEntity(String.class));
    }

    @Test
    public void testAsyncPatchEntity() throws Exception {
        Future<String> response = target().request().async().patch(Entity.text("patch"), String.class);

        assertEquals("patch", response.get());
    }

    @Test
    public void testAsyncPatchGenericType() throws Exception {
        Future<String> response = target().request().async().patch(Entity.text("patch"), new GenericType<String>() {
        });

        assertEquals("patch", response.get());
    }

    @Test
    public void testRxPatchResponse() throws Exception {
        CompletionStage<Response> response = target().request().rx().patch(Entity.text("patch"));

        assertEquals(200, response.toCompletableFuture().get().getStatus());
        assertEquals("patch", response.toCompletableFuture().get().readEntity(String.class));
    }

    @Test
    public void testRxPatchEntity() throws Exception {
        CompletionStage<String> response = target().request().rx().patch(Entity.text("patch"), String.class);

        assertEquals("patch", response.toCompletableFuture().get());
    }

    @Test
    public void testRxPatchGenericType() throws Exception {
        CompletionStage<String> response = target().request().rx()
                                                   .patch(Entity.text("patch"), new GenericType<String>() {
                                                   });

        assertEquals("patch", response.toCompletableFuture().get());
    }

    @Path("/")
    public static class PatchResource {

        @PATCH
        public String patch(String entity) {

            System.out.println("SERVER: patch request received.");

            return entity;
        }
    }
}
