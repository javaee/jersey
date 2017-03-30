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
package org.glassfish.jersey.resilient.connector;

import static javax.ws.rs.client.Entity.text;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientRequest;
import org.glassfish.jersey.grizzly.connector.GrizzlyConnectorProvider;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Test;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixObservableCommand;

public class HystrixCommandConfigProviderTest extends JerseyTest {

    @Path("/test")
    public static class EchoResource {
        @POST
        public Response post(@HeaderParam("X-Test-Hystrix-Command-Name") String testHystrixCommandNameHeader,
                           @HeaderParam("X-Test-Hystrix-Command-Type") String testHystrixCommandTypeHeader,
                           String entity) {
            return Response.ok("POSTed " + entity)
                    .header("X-Test-Hystrix-Command-Name", testHystrixCommandNameHeader)
                    .header("X-Test-Hystrix-Command-Type", testHystrixCommandTypeHeader)
                    .build();
        }
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(EchoResource.class);
    }

    @Override
    protected void configureClient(ClientConfig config) {
        config.connectorProvider(new ResilientConnectorProvider(new GrizzlyConnectorProvider()));
        config.property(ResilientConnectorProvider.HYSTRIX_COMMAND_CONFIG_PROVIDER,
                new ResilientConnectorProvider.HystrixCommandConfigProvider() {

            @Override
            public com.netflix.hystrix.HystrixObservableCommand.Setter observableCommandConfig(ClientRequest requestContext) {
                requestContext.getHeaders().add("X-Test-Hystrix-Command-Type", HystrixObservableCommand.class.getName());
                return com.netflix.hystrix.HystrixObservableCommand.Setter
                        .withGroupKey(HystrixCommandGroupKey.Factory.asKey("test"))
                        .andCommandKey(HystrixCommandKey.Factory.asKey(commandName(requestContext)));
            }

            @Override
            public String commandName(ClientRequest requestContext) {
                String command = requestContext.getUri().getPath() + "_" + requestContext.getMethod();
                requestContext.getHeaders().add("X-Test-Hystrix-Command-Name", command);
                return command;
            }

            @Override
            public com.netflix.hystrix.HystrixCommand.Setter commandConfig(ClientRequest requestContext) {
                requestContext.getHeaders().add("X-Test-Hystrix-Command-Type", HystrixCommand.class.getName());
                return com.netflix.hystrix.HystrixCommand.Setter
                        .withGroupKey(HystrixCommandGroupKey.Factory.asKey("test"))
                        .andCommandKey(HystrixCommandKey.Factory.asKey(commandName(requestContext)));
            }
        });
    }

    @Test
    public void testHystrixCommandConfigProvider() {
        Response response = target("test").request().post(text("Hello from Resilient connector"));
        assertEquals("POSTed Hello from Resilient connector", response.readEntity(String.class));
        assertEquals("/test_POST", response.getHeaderString("X-Test-Hystrix-Command-Name"));
        assertEquals("com.netflix.hystrix.HystrixCommand", response.getHeaderString("X-Test-Hystrix-Command-Type"));

        // test observable command
        try {
            response = target("test").request().async().post(text("Hello from Resilient connector")).get();
        } catch (Exception e) {
            fail(e.getMessage());
        }
        assertEquals("POSTed Hello from Resilient connector", response.readEntity(String.class));
        assertEquals("/test_POST", response.getHeaderString("X-Test-Hystrix-Command-Name"));
        assertEquals("com.netflix.hystrix.HystrixObservableCommand", response.getHeaderString("X-Test-Hystrix-Command-Type"));
    }
}
