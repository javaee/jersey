/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015-2017 Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientLifecycleListener;
import org.glassfish.jersey.client.HttpUrlConnectorProvider;
import org.glassfish.jersey.client.JerseyClient;
import org.glassfish.jersey.client.spi.ConnectorProvider;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.grizzly.connector.GrizzlyConnectorProvider;
import org.glassfish.jersey.jetty.connector.JettyConnectorProvider;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * Ensure Jersey connectors do not leak resources
 * in case multiple client runtime instances are being created.
 *
 * On my laptop, 1000 iterations was sufficient to cause
 * a memory leak until JERSEY-2688 got fixed.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
@RunWith(Parameterized.class)
public class ShutdownHookMemoryLeakTest extends JerseyTest {

    private static final String PATH = "test";
    private static final int ITERATIONS = 1000;

    private final ConnectorProvider connectorProvider;

    public ShutdownHookMemoryLeakTest(final ConnectorProvider cp) {
        connectorProvider = cp;
    }


    @Parameterized.Parameters
    public static List<ConnectorProvider[]> connectionProviders() {
        return Arrays.asList(new ConnectorProvider[][] {
                {new GrizzlyConnectorProvider()},
                {new JettyConnectorProvider()},
                {new ApacheConnectorProvider()},
                {new HttpUrlConnectorProvider()}
        });
    }

    @Path(PATH)
    public static class TestResource {

        @GET
        public String get() {
            return "GET";
        }
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(TestResource.class);
    }

    @Override
    protected void configureClient(ClientConfig config) {
        config.connectorProvider(connectorProvider);
    }

    @Test
    @Ignore("Unstable, ignored for now")
    public void testClientDoesNotLeakResources() throws Exception {

        final AtomicInteger listenersInitialized = new AtomicInteger(0);
        final AtomicInteger listenersClosed = new AtomicInteger(0);

        for (int i = 0; i < ITERATIONS; i++) {
            final Response response = target(PATH).property("another", "runtime").register(new ClientLifecycleListener() {
                @Override
                public void onInit() {
                    listenersInitialized.incrementAndGet();
                }

                @Override
                public void onClose() {
                    listenersClosed.incrementAndGet();
                }
            }).register(LoggingFeature.class).request().get();
            assertEquals("GET", response.readEntity(String.class));
        }

        Collection shutdownHooks = getShutdownHooks(client());

        assertThat(String.format(
                    "%s: number of initialized listeners should be the same as number of total request count",
                        connectorProvider.getClass()),
                listenersInitialized.get(), is(ITERATIONS));

//      the following check is fragile, as GC could break it easily
//        assertThat(String.format(
//                "%s: number of closed listeners should correspond to the number of missing hooks",
//                        connectorProvider.getClass()),
//                listenersClosed.get(), is(ITERATIONS - shutdownHooks.size()));

        client().close();      // clean up the rest

        assertThat(String.format(
                        "%s: number of closed listeners should be the same as the number of total requests made",
                        connectorProvider.getClass()),
                listenersClosed.get(), is(ITERATIONS));
    }

    private Collection getShutdownHooks(javax.ws.rs.client.Client client) throws NoSuchFieldException, IllegalAccessException {
        JerseyClient jerseyClient = (JerseyClient) client;
        Field shutdownHooksField = JerseyClient.class.getDeclaredField("shutdownHooks");
        shutdownHooksField.setAccessible(true);
        return (Collection) shutdownHooksField.get(jerseyClient);
    }
}
