/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.jetty.connector;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Configurable;
import javax.ws.rs.core.Configuration;

import org.glassfish.jersey.client.Initializable;
import org.glassfish.jersey.client.spi.Connector;
import org.glassfish.jersey.client.spi.ConnectorProvider;

import org.eclipse.jetty.client.HttpClient;

/**
 * A {@link ConnectorProvider} for Jersey {@link Connector connector}
 * instances that utilize the Jetty HTTP Client to send and receive
 * HTTP request and responses.
 * <p>
 * The following connector configuration properties are supported:
 * <ul>
 * <li>{@link org.glassfish.jersey.client.ClientProperties#ASYNC_THREADPOOL_SIZE}</li>
 * <li>{@link org.glassfish.jersey.client.ClientProperties#CONNECT_TIMEOUT}</li>
 * <li>{@link org.glassfish.jersey.client.ClientProperties#FOLLOW_REDIRECTS}</li>
 * <li>{@link org.glassfish.jersey.client.ClientProperties#PROXY_URI}</li>
 * <li>{@link org.glassfish.jersey.client.ClientProperties#PROXY_USERNAME}</li>
 * <li>{@link org.glassfish.jersey.client.ClientProperties#PROXY_PASSWORD}</li>
 * <li>{@link org.glassfish.jersey.client.ClientProperties#PROXY_PASSWORD}</li>
 * <li>{@link JettyClientProperties#PREEMPTIVE_BASIC_AUTHENTICATION}</li>
 * <li>{@link JettyClientProperties#DISABLE_COOKIES}</li>
 * </ul>
 * </p>
 * <p>
 * This transport supports both synchronous and asynchronous processing of client requests.
 * The following methods are supported: GET, POST, PUT, DELETE, HEAD, OPTIONS, TRACE, CONNECT and MOVE.
 * </p>
 * <p>
 * Typical usage:
 * </p>
 * <pre>
 * {@code
 * ClientConfig config = new ClientConfig();
 * config.connectorProvider(new JettyConnectorProvider());
 * Client client = ClientBuilder.newClient(config);
 *
 * // async request
 * WebTarget target = client.target("http://localhost:8080");
 * Future<Response> future = target.path("resource").request().async().get();
 *
 * // wait for 3 seconds
 * Response response = future.get(3, TimeUnit.SECONDS);
 * String entity = response.readEntity(String.class);
 * client.close();
 * }
 * </pre>
 * <p>
 * Connector instances created via Jetty HTTP Client-based connector provider support only
 * {@link org.glassfish.jersey.client.RequestEntityProcessing#BUFFERED entity buffering}.
 * Defining the property {@link org.glassfish.jersey.client.ClientProperties#REQUEST_ENTITY_PROCESSING} has no
 * effect on Jetty HTTP Client-based connectors.
 * </p>
 *
 * @author Arul Dhesiaseelan (aruld at acm.org)
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @since 2.5
 */
public class JettyConnectorProvider implements ConnectorProvider {

    @Override
    public Connector getConnector(Client client, Configuration runtimeConfig) {
        return new JettyConnector(client, runtimeConfig);
    }

    /**
     * Retrieve the underlying Jetty {@link org.eclipse.jetty.client.HttpClient} instance from
     * {@link org.glassfish.jersey.client.JerseyClient} or {@link org.glassfish.jersey.client.JerseyWebTarget}
     * configured to use {@code JettyConnectorProvider}.
     *
     * @param component {@code JerseyClient} or {@code JerseyWebTarget} instance that is configured to use
     *                  {@code JettyConnectorProvider}.
     * @return underlying Jetty {@code HttpClient} instance.
     *
     * @throws java.lang.IllegalArgumentException in case the {@code component} is neither {@code JerseyClient}
     *                                            nor {@code JerseyWebTarget} instance or in case the component
     *                                            is not configured to use a {@code JettyConnectorProvider}.
     * @since 2.8
     */
    public static HttpClient getHttpClient(Configurable<?> component) {
        if (!(component instanceof Initializable)) {
            throw new IllegalArgumentException(
                    LocalizationMessages.INVALID_CONFIGURABLE_COMPONENT_TYPE(component.getClass().getName()));
        }

        final Initializable<?> initializable = (Initializable<?>) component;
        Connector connector = initializable.getConfiguration().getConnector();
        if (connector == null) {
            initializable.preInitialize();
            connector = initializable.getConfiguration().getConnector();
        }

        if (connector instanceof JettyConnector) {
            return ((JettyConnector) connector).getHttpClient();
        }

        throw new IllegalArgumentException(LocalizationMessages.EXPECTED_CONNECTOR_PROVIDER_NOT_USED());
    }
}
