/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package org.glassfish.jersey.client.spi;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientRequest;
import org.glassfish.jersey.client.ClientResponse;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Caching connector provider unit tests.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class CachingConnectorProviderTest {
    public static class ReferenceCountingNullConnector implements Connector, ConnectorProvider {

        private static final AtomicInteger counter = new AtomicInteger(0);

        @Override
        public ClientResponse apply(ClientRequest request) {
            throw new ProcessingException("test");
        }

        @Override
        public Future<?> apply(ClientRequest request, AsyncConnectorCallback callback) {
            throw new ProcessingException("test-async");
        }

        @Override
        public void close() {
            // do nothing
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public Connector getConnector(Client client, Configuration runtimeConfig) {
            counter.incrementAndGet();
            return this;
        }

        public int getCount() {
            return counter.get();
        }
    }

    @Test
    public void testCachingConnector() {
        final ReferenceCountingNullConnector connectorProvider = new ReferenceCountingNullConnector();
        final CachingConnectorProvider cachingConnectorProvider = new CachingConnectorProvider(connectorProvider);
        final ClientConfig configuration = new ClientConfig().connectorProvider(cachingConnectorProvider).getConfiguration();

        Client client1 = ClientBuilder.newClient(configuration);
        try {
            client1.target(UriBuilder.fromUri("/").build()).request().get();
        } catch (ProcessingException ce) {
            assertEquals("test", ce.getMessage());
            assertEquals(1, connectorProvider.getCount());
        }
        try {
            client1.target(UriBuilder.fromUri("/").build()).request().async().get();
        } catch (ProcessingException ce) {
            assertEquals("test-async", ce.getMessage());
            assertEquals(1, connectorProvider.getCount());
        }

        Client client2 = ClientBuilder.newClient(configuration);
        try {
            client2.target(UriBuilder.fromUri("/").build()).request().get();
        } catch (ProcessingException ce) {
            assertEquals("test", ce.getMessage());
            assertEquals(1, connectorProvider.getCount());
        }
        try {
            client2.target(UriBuilder.fromUri("/").build()).request().async().get();
        } catch (ProcessingException ce) {
            assertEquals("test-async", ce.getMessage());
            assertEquals(1, connectorProvider.getCount());
        }
    }
}
