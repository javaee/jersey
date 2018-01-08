/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.apache.connector;

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;

import javax.inject.Singleton;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.server.ChunkedOutput;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * @author Petr Janouch (petr.janouch at oracle.com)
 */
public class StreamingTest extends JerseyTest {

    /**
     * Test that a data stream can be terminated from the client side.
     */
    @Test
    public void clientCloseTest() throws IOException {
        // start streaming
        InputStream inputStream = target().path("/streamingEndpoint").request().get(InputStream.class);

        WebTarget sendTarget = target().path("/streamingEndpoint/send");
        // trigger sending 'A' to the stream; OK is sent if everything on the server was OK
        assertEquals("OK", sendTarget.request().get().readEntity(String.class));
        // check 'A' has been sent
        assertEquals('A', inputStream.read());
        // closing the stream should tear down the connection
        inputStream.close();
        // trigger sending another 'A' to the stream; it should fail
        // (indicating that the streaming has been terminated on the server)
        assertEquals("NOK", sendTarget.request().get().readEntity(String.class));
    }

    @Override
    protected void configureClient(ClientConfig config) {
        config.connectorProvider(new ApacheConnectorProvider());
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(StreamingEndpoint.class);
    }

    @Singleton
    @Path("streamingEndpoint")
    public static class StreamingEndpoint {

        private final ChunkedOutput<String> output = new ChunkedOutput<>(String.class);

        @GET
        @Path("send")
        public String sendEvent() {
            try {
                output.write("A");
            } catch (IOException e) {
                return "NOK";
            }

            return "OK";
        }

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public ChunkedOutput<String> get() {
            return output;
        }
    }
}
