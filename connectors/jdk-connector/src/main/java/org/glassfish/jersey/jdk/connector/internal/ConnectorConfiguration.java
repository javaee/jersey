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

package org.glassfish.jersey.jdk.connector.internal;

import java.net.CookiePolicy;
import java.net.URI;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Configuration;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import org.glassfish.jersey.SslConfigurator;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.jdk.connector.JdkConnectorProperties;

/**
 * A container for connector configuration to make it easier to move around.
 * <p/>
 * This is internal to the connector and is not used by the user for configuration.
 *
 * @author Petr Janouch (petr.janouch at oracle.com)
 */
class ConnectorConfiguration {

    private static final Logger LOGGER = Logger.getLogger(ConnectorConfiguration.class.getName());

    private final int chunkSize;
    private final boolean followRedirects;
    private final int maxRedirects;
    private final ThreadPoolConfig threadPoolConfig;
    private final int containerIdleTimeout;
    private final int maxHeaderSize;
    private final CookiePolicy cookiePolicy;
    private final int maxConnectionsPerDestination;
    private final int connectionIdleTimeout;
    private final SSLContext sslContext;
    private final HostnameVerifier hostnameVerifier;
    private final int responseTimeout;
    private final int connectTimeout;
    private final ProxyConfiguration proxyConfiguration;

    ConnectorConfiguration(Client client, Configuration config) {
        final Map<String, Object> properties = config.getProperties();

        int proposedChunkSize = JdkConnectorProperties.getValue(properties, ClientProperties.CHUNKED_ENCODING_SIZE,
                JdkConnectorProperties.DEFAULT_HTTP_CHUNK_SIZE, Integer.class);
        if (proposedChunkSize < 0) {
            LOGGER.warning(LocalizationMessages.NEGATIVE_CHUNK_SIZE(proposedChunkSize,
                    JdkConnectorProperties.DEFAULT_HTTP_CHUNK_SIZE));
            proposedChunkSize = JdkConnectorProperties.DEFAULT_HTTP_CHUNK_SIZE;
        }

        chunkSize = proposedChunkSize;

        threadPoolConfig = JdkConnectorProperties.getValue(properties, JdkConnectorProperties.WORKER_THREAD_POOL_CONFIG,
                ThreadPoolConfig.defaultConfig(), ThreadPoolConfig.class);
        threadPoolConfig.setCorePoolSize(ClientProperties.getValue(properties, ClientProperties.ASYNC_THREADPOOL_SIZE,
                threadPoolConfig.getCorePoolSize(), Integer.class));
        containerIdleTimeout = JdkConnectorProperties.getValue(properties, JdkConnectorProperties.CONTAINER_IDLE_TIMEOUT,
                JdkConnectorProperties.DEFAULT_CONNECTION_CLOSE_WAIT, Integer.class);

        maxHeaderSize = JdkConnectorProperties.getValue(properties, JdkConnectorProperties.MAX_HEADER_SIZE,
                JdkConnectorProperties.DEFAULT_MAX_HEADER_SIZE, Integer.class);
        followRedirects = ClientProperties.getValue(properties, ClientProperties.FOLLOW_REDIRECTS, true, Boolean.class);

        cookiePolicy = JdkConnectorProperties.getValue(properties, JdkConnectorProperties.COOKIE_POLICY,
                JdkConnectorProperties.DEFAULT_COOKIE_POLICY, CookiePolicy.class);
        maxRedirects = JdkConnectorProperties.getValue(properties, JdkConnectorProperties.MAX_REDIRECTS,
                JdkConnectorProperties.DEFAULT_MAX_REDIRECTS, Integer.class);

        maxConnectionsPerDestination = JdkConnectorProperties.getValue(properties,
                JdkConnectorProperties.MAX_CONNECTIONS_PER_DESTINATION,
                JdkConnectorProperties.DEFAULT_MAX_CONNECTIONS_PER_DESTINATION, Integer.class);

        connectionIdleTimeout = JdkConnectorProperties
                .getValue(properties, JdkConnectorProperties.CONNECTION_IDLE_TIMEOUT,
                        JdkConnectorProperties.DEFAULT_CONNECTION_IDLE_TIMEOUT, Integer.class);

        responseTimeout = ClientProperties.getValue(properties, ClientProperties.READ_TIMEOUT, 0, Integer.class);

        connectTimeout = ClientProperties.getValue(properties, ClientProperties.CONNECT_TIMEOUT, 0, Integer.class);

        if (client.getSslContext() == null) {
            sslContext = SslConfigurator.getDefaultContext();
        } else {
            sslContext = client.getSslContext();
        }

        hostnameVerifier = client.getHostnameVerifier();

        proxyConfiguration = new ProxyConfiguration(properties);

        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.log(Level.FINEST, LocalizationMessages.CONNECTOR_CONFIGURATION(toString()));
        }
    }

    int getChunkSize() {
        return chunkSize;
    }

    boolean getFollowRedirects() {
        return followRedirects;
    }

    int getMaxRedirects() {
        return maxRedirects;
    }

    ThreadPoolConfig getThreadPoolConfig() {
        return threadPoolConfig;
    }

    int getContainerIdleTimeout() {
        return containerIdleTimeout;
    }

    int getMaxHeaderSize() {
        return maxHeaderSize;
    }

    CookiePolicy getCookiePolicy() {
        return cookiePolicy;
    }

    int getMaxConnectionsPerDestination() {
        return maxConnectionsPerDestination;
    }

    int getConnectionIdleTimeout() {
        return connectionIdleTimeout;
    }

    SSLContext getSslContext() {
        return sslContext;
    }

    HostnameVerifier getHostnameVerifier() {
        return hostnameVerifier;
    }

    int getResponseTimeout() {
        return responseTimeout;
    }

    int getConnectTimeout() {
        return connectTimeout;
    }

    public ProxyConfiguration getProxyConfiguration() {
        return proxyConfiguration;
    }

    @Override
    public String toString() {
        return "ConnectorConfiguration{"
                + ", chunkSize=" + chunkSize
                + ", followRedirects=" + followRedirects
                + ", maxRedirects=" + maxRedirects
                + ", threadPoolConfig=" + threadPoolConfig
                + ", containerIdleTimeout=" + containerIdleTimeout
                + ", maxHeaderSize=" + maxHeaderSize
                + ", cookiePolicy=" + cookiePolicy
                + ", maxConnectionsPerDestination=" + maxConnectionsPerDestination
                + ", connectionIdleTimeout=" + connectionIdleTimeout
                + ", sslContext=" + sslContext
                + ", hostnameVerifier=" + hostnameVerifier
                + ", responseTimeout=" + responseTimeout
                + ", connectTimeout=" + connectTimeout
                + ", proxyConfiguration=" + proxyConfiguration.toString()
                + '}';
    }

    static class ProxyConfiguration {

        private final boolean configured;
        private final String host;
        private final int port;
        private final String userName;
        private final String password;

        private ProxyConfiguration(Map<String, Object> properties) {
            String uriStr = ClientProperties.getValue(properties, ClientProperties.PROXY_URI, String.class);
            if (uriStr == null) {
                configured = false;
                host = null;
                port = -1;
                userName = null;
                password = null;
                return;
            }

            configured = true;
            URI proxyUri = URI.create(uriStr);
            host = proxyUri.getHost();

            if (proxyUri.getPort() == -1) {
                port = 8080;
            } else {
                port = proxyUri.getPort();
            }

            userName = JdkConnectorProperties.getValue(properties, ClientProperties.PROXY_USERNAME, String.class);
            password = JdkConnectorProperties.getValue(properties, ClientProperties.PROXY_PASSWORD, String.class);
        }

        boolean isConfigured() {
            return configured;
        }

        String getHost() {
            return host;
        }

        int getPort() {
            return port;
        }

        String getUserName() {
            return userName;
        }

        String getPassword() {
            return password;
        }
    }
}
