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
package org.glassfish.jersey.jdk.connector;

import java.net.CookiePolicy;
import java.util.Map;

import org.glassfish.jersey.internal.util.PropertiesClass;
import org.glassfish.jersey.internal.util.PropertiesHelper;

/**
 * Configuration options specific to {@link org.glassfish.jersey.jdk.connector.internal.JdkConnector}.
 *
 * @author Adam Lindenthal (adam.lindenthal at oracle.com)
 */
@PropertiesClass
public final class JdkConnectorProperties {

    /**
     * Configuration of the connector thread pool.
     * <p/>
     * An instance of {@link org.glassfish.jersey.jdk.connector.internal.ThreadPoolConfig} is expected.
     */
    public static final String WORKER_THREAD_POOL_CONFIG = "jersey.config.client.JdkConnectorProvider.workerThreadPoolConfig";

    /**
     * Container idle timeout in milliseconds ({@link Integer} value).
     * <p/>
     * When the timeout elapses, the shared thread pool will be destroyed.
     * <p/>
     * The default value is {@value #DEFAULT_CONNECTION_CLOSE_WAIT}
     */
    public static final String CONTAINER_IDLE_TIMEOUT = "jersey.config.client.JdkConnectorProvider.containerIdleTimeout";

    /**
     * A configurable property of HTTP parser. It defines the maximal acceptable size of HTTP response initial line,
     * each header and chunk header.
     * <p/>
     * The default value is {@value #DEFAULT_MAX_HEADER_SIZE}
     */
    public static final String MAX_HEADER_SIZE = "jersey.config.client.JdkConnectorProvider.maxHeaderSize";

    /**
     * The maximal number of redirects during single request.
     * <p/>
     * Value is expected to be positive {@link Integer}. Default value is {@value #DEFAULT_MAX_REDIRECTS}.
     * <p/>
     * HTTP redirection must be enabled by property {@link org.glassfish.jersey.client.ClientProperties#FOLLOW_REDIRECTS},
     * otherwise {@code MAX_HEADER_SIZE} is not applied.
     *
     * @see org.glassfish.jersey.client.ClientProperties#FOLLOW_REDIRECTS
     * @see org.glassfish.jersey.jdk.connector.internal.RedirectException
     */
    public static final String MAX_REDIRECTS = "jersey.config.client.JdkConnectorProvider.maxRedirects";

    /**
     * To set the cookie policy of this cookie manager.
     * <p/>
     * The default value is ACCEPT_ORIGINAL_SERVER.
     *
     * @see java.net.CookieManager
     */
    public static final String COOKIE_POLICY = "jersey.config.client.JdkConnectorProvider.cookiePolicy";

    /**
     * A maximal number of open connection to each destination. A destination is determined by the following triple:
     * <ul>
     * <li>host</li>
     * <li>port</li>
     * <li>protocol (HTTP/HTTPS)</li>
     * <ul/>
     * <p/>
     * The default value is {@value #DEFAULT_MAX_CONNECTIONS_PER_DESTINATION}
     */
    public static final String MAX_CONNECTIONS_PER_DESTINATION = "jersey.config.client.JdkConnectorProvider"
            + ".maxConnectionsPerDestination";

    /**
     * An amount of time in milliseconds ({@link Integer} value) during which an idle connection will be kept open.
     * <p/>
     * The default value is {@value #DEFAULT_CONNECTION_IDLE_TIMEOUT}
     */
    public static final String CONNECTION_IDLE_TIMEOUT = "jersey.config.client.JdkConnectorProvider.connectionIdleTimeout";

    /**
     * Default value for the {@link org.glassfish.jersey.client.ClientProperties#CHUNKED_ENCODING_SIZE} property.
     */
    public static final int DEFAULT_HTTP_CHUNK_SIZE = 4096;

    /**
     * Default value for the {@link #MAX_HEADER_SIZE} property.
     */
    public static final int DEFAULT_MAX_HEADER_SIZE = 8192;

    /**
     * Default value for the {@link #MAX_REDIRECTS} property.
     */
    public static final int DEFAULT_MAX_REDIRECTS = 5;

    /**
     * Default value for the {@link #COOKIE_POLICY} property.
     */
    public static final CookiePolicy DEFAULT_COOKIE_POLICY = CookiePolicy.ACCEPT_ORIGINAL_SERVER;

    /**
     * Default value for the {@link #MAX_CONNECTIONS_PER_DESTINATION} property.
     */
    public static final int DEFAULT_MAX_CONNECTIONS_PER_DESTINATION = 20;

    /**
     * Default value for the {@link #CONNECTION_IDLE_TIMEOUT} property.
     */
    public static final int DEFAULT_CONNECTION_IDLE_TIMEOUT = 1000000;

    /**
     * Default value for the {@link #CONTAINER_IDLE_TIMEOUT} property.
     */
    public static final int DEFAULT_CONNECTION_CLOSE_WAIT = 30_000;

    public static <T> T getValue(final Map<String, ?> properties, final String key, final Class<T> type) {
        return PropertiesHelper.getValue(properties, key, type, null);
    }

    public static <T> T getValue(final Map<String, ?> properties, final String key, T defaultValue, final Class<T> type) {
        return PropertiesHelper.getValue(properties, key, defaultValue, type, null);
    }

    /**
     * Prevents instantiation.
     */
    private JdkConnectorProperties() {
        throw new AssertionError("No instances allowed.");
    }
}
