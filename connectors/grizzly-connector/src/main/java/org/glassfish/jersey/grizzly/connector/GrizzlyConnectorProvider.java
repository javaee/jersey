/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2014 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.grizzly.connector;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Configurable;
import javax.ws.rs.core.Configuration;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientRequest;
import org.glassfish.jersey.client.Initializable;
import org.glassfish.jersey.client.spi.Connector;
import org.glassfish.jersey.client.spi.ConnectorProvider;
import org.glassfish.jersey.internal.util.Property;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.RequestBuilder;

/**
 * Connector provider for Jersey {@link Connector connectors} that utilize
 * Grizzly Asynchronous HTTP Client to send and receive HTTP request and responses.
 * <p>
 * The following connector configuration properties are supported:
 * <ul>
 * <li>{@link org.glassfish.jersey.client.ClientProperties#CONNECT_TIMEOUT}</li>
 * <li>{@link org.glassfish.jersey.client.ClientProperties#READ_TIMEOUT}</li>
 * <li>{@link org.glassfish.jersey.client.ClientProperties#REQUEST_ENTITY_PROCESSING}
 * - default value is {@link org.glassfish.jersey.client.RequestEntityProcessing#CHUNKED}</li>
 * <li>{@link org.glassfish.jersey.client.ClientProperties#PROXY_URI}</li>
 * <li>{@link org.glassfish.jersey.client.ClientProperties#PROXY_USERNAME}</li>
 * <li>{@link org.glassfish.jersey.client.ClientProperties#PROXY_PASSWORD}</li>
 * </ul>
 * </p>
 * <p>
 * Connector instances created via this connector provider use
 * {@link org.glassfish.jersey.client.RequestEntityProcessing#CHUNKED chunked encoding} as a default setting.
 * This can be overridden by the {@link org.glassfish.jersey.client.ClientProperties#REQUEST_ENTITY_PROCESSING}.
 * </p>
 * <p>
 * If a {@link org.glassfish.jersey.client.ClientResponse} is obtained and an entity is not read from the response then
 * {@link org.glassfish.jersey.client.ClientResponse#close()} MUST be called after processing the response to release
 * connection-based resources.
 * </p>
 * <p>
 * If a response entity is obtained that is an instance of {@link java.io.Closeable}  then the instance MUST
 * be closed after processing the entity to release connection-based resources.
 * <p/>
 * <p>
 * The following methods are currently supported: HEAD, GET, POST, PUT, DELETE, OPTIONS, PATCH and TRACE.
 * <p/>
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @since 2.5
 */
public class GrizzlyConnectorProvider implements ConnectorProvider {
    /**
     * A {@link GrizzlyConnectorProvider.RequestCustomizer request customizer} instance to be used to customize the
     * request.
     *
     * The value MUST be an instance implementing the  {@link GrizzlyConnectorProvider.RequestCustomizer} SPI.
     * <p>
     * A default value is not set (is {@code null}).
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     *
     * @see #register(Invocation.Builder, GrizzlyConnectorProvider.RequestCustomizer)
     * @see org.glassfish.jersey.grizzly.connector.GrizzlyConnectorProvider.RequestCustomizer
     */
    @Property
    static final String REQUEST_CUSTOMIZER = "jersey.config.grizzly.client.request.customizer";

    private final AsyncClientCustomizer asyncClientCustomizer;

    /**
     * A customization SPI for the async client instance underlying Grizzly connectors.
     * <p>
     * An implementation of async client customizer can be
     * registered in a {@code GrizzlyConnectorProvider}
     * {@link GrizzlyConnectorProvider#GrizzlyConnectorProvider(GrizzlyConnectorProvider.AsyncClientCustomizer) constructor}.
     * When a connector instance is then created, the customizer is invoked to update the
     * {@link com.ning.http.client.AsyncHttpClientConfig.Builder underlying async client configuration builder} before the actual
     * configuration instance is built and used to create the async client instance.
     * The customizer thus provides a way how to configure parts of the underlying async client SPI that are not directly
     * exposed in the {@code GrizzlyConnectorProvider} API.
     * </p>
     *
     * @see org.glassfish.jersey.grizzly.connector.GrizzlyConnectorProvider.RequestCustomizer
     * @since 2.10
     */
    public static interface AsyncClientCustomizer {
        /**
         * Customize the underlying asynchronous client configuration builder.
         * <p>
         * The configuration builder instance instance returned from the method will be subsequently used to build the
         * configuration object that configures both the {@link com.ning.http.client.providers.grizzly.GrizzlyAsyncHttpProvider}
         * Grizzly async client provider} as well as the underlying {@link com.ning.http.client.AsyncHttpClient async HTTP
         * client} instance itself.
         * </p>
         * <p>
         * Note that any JAX-RS and Jersey specific configuration updates on the configuration builder happen before this method
         * is invoked. As such, changes made to the configuration builder may override or cancel the effect of the JAX-RS and
         * Jersey specific configuration changes. As such any configuration changes should be made with care and implementers
         * should be aware of possible side effect of their changes.
         * </p>
         *
         * @param client        JAX-RS client for which the connector is being created.
         * @param config        JAX-RS configuration that was used to initialize connector's configuration.
         * @param configBuilder Async HTTP Client configuration builder that has been initialized based on the JAX-RS
         *                      configuration.
         * @return Async HTTP Client builder instance to be used to configure the underlying Grizzly provider and async HTTP
         * client instance. Typically, the method returns the same {@code configBuilder} instance that has been passed into
         * the method as an input parameter, but it is not required to do so.
         */
        public AsyncHttpClientConfig.Builder customize(final Client client,
                                                       final Configuration config,
                                                       final AsyncHttpClientConfig.Builder configBuilder);
    }

    /**
     * A customization SPI for the async client request instances.
     *
     * A request customizer can be used to configure Async HTTP Client specific details of the request, which are not directly
     * exposed via the JAX-RS, Jersey or Grizzly connector provider API.
     * <p>
     * Before a request is built and sent for execution, a registered request customizer
     * implementation can update the Async HTTP Client {@link com.ning.http.client.RequestBuilder request builder} used
     * to build the request instance ultimately sent for processing.
     * An instance of the request customizer can be either {@link #register(ClientConfig, RequestCustomizer) registered globally}
     * for all requests by registering the customizer in the Jersey client configuration, or it can be individually
     * {@link #register(Invocation.Builder, RequestCustomizer) registered per request}, by registering it into a specific
     * invocation builder instance. In case of a conflict when one instance is registered globally and another per request, the
     * per request registered customizer takes precedence and the global customizer will be ignored.
     * </p>
     *
     * @see org.glassfish.jersey.grizzly.connector.GrizzlyConnectorProvider.AsyncClientCustomizer
     * @see #register(org.glassfish.jersey.client.ClientConfig, GrizzlyConnectorProvider.RequestCustomizer)
     * @see #register(Invocation.Builder, GrizzlyConnectorProvider.RequestCustomizer)
     * @since 2.10
     */
    public static interface RequestCustomizer {
        /**
         * Customize the underlying Async HTTP Client request builder associated with a specific Jersey client request.
         * <p>
         * The request builder instance returned from the method will be subsequently used to build the actual Async HTTP Client
         * request instance sent for execution.
         * </p>
         * <p>
         * Note that any JAX-RS and Jersey specific request configuration updates on the request builder happen before this
         * method is invoked. As such, changes made to the request builder may override or cancel the effect of the JAX-RS and
         * Jersey specific request configuration changes. As such any request builder changes should be made with care and
         * implementers should be aware of possible side effect of their changes.
         * </p>
         *
         * @param requestContext Jersey client request instance for which the Async HTTP Client request is being built.
         * @param requestBuilder Async HTTP Client request builder for the Jersey request.
         * @return Async HTTP Client request builder instance that will be used to build the actual Async HTTP Client
         * request instance sent for execution. Typically, the method returns the same {@code requestBuilder} instance that
         * has been passed into the method as an input parameter, but it is not required to do so.
         */
        public RequestBuilder customize(final ClientRequest requestContext, final RequestBuilder requestBuilder);
    }

    /**
     * Create new Grizzly Async HTTP Client connector provider.
     */
    public GrizzlyConnectorProvider() {
        this.asyncClientCustomizer = null;
    }

    /**
     * Create new Grizzly Async HTTP Client connector provider with a custom client configuration customizer.
     *
     * @param asyncClientCustomizer Async HTTP Client configuration customizer.
     * @since 2.10
     */
    public GrizzlyConnectorProvider(final AsyncClientCustomizer asyncClientCustomizer) {
        this.asyncClientCustomizer = asyncClientCustomizer;
    }

    @Override
    public Connector getConnector(Client client, Configuration config) {
        return new GrizzlyConnector(client, config, asyncClientCustomizer);
    }

    /**
     * Retrieve the underlying Grizzly {@link AsyncHttpClient} instance from
     * {@link org.glassfish.jersey.client.JerseyClient} or {@link org.glassfish.jersey.client.JerseyWebTarget}
     * configured to use {@code GrizzlyConnectorProvider}.
     *
     * @param component {@code JerseyClient} or {@code JerseyWebTarget} instance that is configured to use
     *                  {@code GrizzlyConnectorProvider}.
     * @return underlying Grizzly {@code AsyncHttpClient} instance.
     *
     * @throws java.lang.IllegalArgumentException in case the {@code component} is neither {@code JerseyClient}
     *                                            nor {@code JerseyWebTarget} instance or in case the component
     *                                            is not configured to use a {@code GrizzlyConnectorProvider}.
     * @since 2.8
     */
    public static AsyncHttpClient getHttpClient(Configurable<?> component) {
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

        if (connector instanceof GrizzlyConnector) {
            return ((GrizzlyConnector) connector).getGrizzlyClient();
        }

        throw new IllegalArgumentException(LocalizationMessages.EXPECTED_CONNECTOR_PROVIDER_NOT_USED());
    }

    /**
     * Register a request customizer for a single request.
     *
     * A registered customizer will be used to customize the underlying Async HTTP Client request builder.
     * <p>
     * Invoking this method on an instance that is not configured to use Grizzly Async HTTP Client
     * connector does not have any effect.
     * </p>
     *
     * @param builder    JAX-RS request invocation builder.
     * @param customizer request customizer to be registered.
     * @return updated Jersey client config with the Grizzly
     * {@link org.glassfish.jersey.grizzly.connector.GrizzlyConnectorProvider.RequestCustomizer} attached.
     */
    public static Invocation.Builder register(Invocation.Builder builder, RequestCustomizer customizer) {
        return builder.property(REQUEST_CUSTOMIZER, customizer);
    }

    /**
     * Register a request customizer for a all requests executed by a client instance configured with this client config.
     *
     * A registered customizer will be used to customize underlying Async HTTP Client request builders for all requests created
     * using the Jersey client instance configured with this client config.
     * <p>
     * Invoking this method on an instance that is not configured to use Grizzly Async HTTP Client
     * connector does not have any effect.
     * </p>
     *
     * @param config     Jersey client configuration.
     * @param customizer Async HTTP Client configuration customizer.
     * @return updated JAX-RS client invocation builder with the Grizzly
     * {@link org.glassfish.jersey.grizzly.connector.GrizzlyConnectorProvider.RequestCustomizer RequestCustomizer} attached.
     */
    public static ClientConfig register(ClientConfig config, RequestCustomizer customizer) {
        return config.property(REQUEST_CUSTOMIZER, customizer);
    }
}
