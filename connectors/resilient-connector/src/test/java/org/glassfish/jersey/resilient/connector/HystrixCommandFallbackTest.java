/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2016 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.resilient.connector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.ClientRequest;
import org.glassfish.jersey.client.ClientResponse;
import org.glassfish.jersey.grizzly.connector.GrizzlyConnectorProvider;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Test;

public class HystrixCommandFallbackTest extends JerseyTest {

    @Path("/test")
    public static class TimeoutResource {
        @GET
        @Path("timeout")
        public String getTimeout() {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "GET";
        }
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(TimeoutResource.class);
    }

    @Override
    protected void configureClient(ClientConfig config) {
        config.property(ClientProperties.READ_TIMEOUT, 1000);
        config.connectorProvider(new ResilientConnectorProvider(new GrizzlyConnectorProvider()));
    }

    @Test
    public void testFallback() {
        CachedResultFallback fallback =  new CachedResultFallback();
        // test Hystrix command
        Response response = target("test")
            .path("timeout")
            .request()
            .property(ResilientConnectorProvider.HYSTRIX_COMMAND_FALLBACK, fallback)
            .property(ClientProperties.READ_TIMEOUT, 1000)
            .get();

        assertEquals("Cached GET", response.readEntity(String.class));

        // test Hystrix Observable command
        try {
            response = target("test")
                    .path("timeout")
                    .request()
                    .property(ResilientConnectorProvider.HYSTRIX_COMMAND_FALLBACK, fallback)
                    .property(ClientProperties.READ_TIMEOUT, 1000)
                    .async()
                    .get()
                    .get();
        } catch (Exception e) {
            fail(e.getMessage());
        }
        assertEquals("Cached GET", response.readEntity(String.class));
    }

    private static class CachedResultFallback implements ResilientConnectorProvider.HystrixCommandFallback {

        @Override
        public ClientResponse execute(ClientRequest requestContext) {
            ClientResponse fallbackResponse = new ClientResponse(Response.Status.OK, requestContext);
            fallbackResponse.setEntityStream(new ByteArrayInputStream("Cached GET".getBytes()));
            return fallbackResponse;
        }
    }
}
