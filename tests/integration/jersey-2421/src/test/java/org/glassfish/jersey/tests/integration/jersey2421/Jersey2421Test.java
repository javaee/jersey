/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.integration.jersey2421;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.Future;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientRequest;
import org.glassfish.jersey.client.ClientResponse;
import org.glassfish.jersey.client.spi.AsyncConnectorCallback;
import org.glassfish.jersey.client.spi.Connector;
import org.glassfish.jersey.client.spi.ConnectorProvider;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.message.internal.NullOutputStream;
import org.glassfish.jersey.message.internal.OutboundMessageContext;

import org.junit.Test;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Reproducer tests for JERSEY-2421.
 *
 * @author Michal Gajdos
 */
public class Jersey2421Test {

    private static class TestConnector implements Connector, ConnectorProvider {

        @Override
        public ClientResponse apply(final ClientRequest request) {
            try {
                request.setStreamProvider(new OutboundMessageContext.StreamProvider() {
                    @Override
                    public OutputStream getOutputStream(final int contentLength) throws IOException {
                        return new NullOutputStream();
                    }
                });
                request.writeEntity();

                if (request.getHeaderString("Content-Type").contains("boundary")) {
                    return new ClientResponse(Response.Status.OK, request);
                }
            } catch (final IOException ioe) {
                // NOOP
            }
            return new ClientResponse(Response.Status.BAD_REQUEST, request);
        }

        @Override
        public Future<?> apply(final ClientRequest request, final AsyncConnectorCallback callback) {
            return null;
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public void close() {
        }

        @Override
        public Connector getConnector(final Client client, final Configuration runtimeConfig) {
            return this;
        }
    }

    /**
     * Test that multipart feature works on the client-side - custom connector checks presence of {@code boundary} parameter in
     * the {@code Content-Type} header (the header is added to the request in MBW).
     */
    @Test
    public void testMultiPartFeatureOnClient() throws Exception {
        final Client client = ClientBuilder.newClient(new ClientConfig().connectorProvider(new TestConnector()))
                .register(MultiPartFeature.class);

        final MultiPart entity = new FormDataMultiPart()
                .bodyPart(new FormDataBodyPart(FormDataContentDisposition.name("part").build(), "CONTENT"));

        final Response response = client.target("http://localhost").request()
                .post(Entity.entity(entity, MediaType.MULTIPART_FORM_DATA_TYPE));

        assertThat(response.getStatus(), is(200));
    }

    /**
     * Test that classes from jersey-server module cannot be loaded.
     */
    @Test(expected = ClassNotFoundException.class)
    public void testLoadJerseyServerClass() throws Exception {
        Class.forName("org.glassfish.jersey.server.ResourceConfig");
    }
}
