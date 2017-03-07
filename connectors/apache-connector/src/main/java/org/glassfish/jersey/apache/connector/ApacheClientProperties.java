/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2017 Oracle and/or its affiliates. All rights reserved.
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

import java.util.Map;

import org.glassfish.jersey.internal.util.PropertiesClass;
import org.glassfish.jersey.internal.util.PropertiesHelper;

/**
 * Configuration options specific to the Client API that utilizes {@link ApacheConnectorProvider}.
 *
 * @author jorgeluisw@mac.com
 * @author Paul Sandoz
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 * @author Arul Dhesiaseelan (aruld at acm.org)
 */
@PropertiesClass
public final class ApacheClientProperties {

    /**
     * The credential provider that should be used to retrieve
     * credentials from a user. Credentials needed for proxy authentication
     * are stored here as well.
     * <p/>
     * The value MUST be an instance of {@link org.apache.http.client.CredentialsProvider}.
     * <p/>
     * If the property is absent a default provider will be used.
     * <p/>
     * The name of the configuration property is <tt>{@value}</tt>.
     */
    public static final String CREDENTIALS_PROVIDER = "jersey.config.apache.client.credentialsProvider";

    /**
     * A value of {@code false} indicates the client should handle cookies
     * automatically using HttpClient's default cookie policy. A value
     * of {@code true} will cause the client to ignore all cookies.
     * <p/>
     * The value MUST be an instance of {@link java.lang.Boolean}.
     * <p/>
     * The default value is {@code false}.
     * <p/>
     * The name of the configuration property is <tt>{@value}</tt>.
     */
    public static final String DISABLE_COOKIES = "jersey.config.apache.client.handleCookies";

    /**
     * A value of {@code true} indicates that a client should send an
     * authentication request even before the server gives a 401
     * response.
     * <p>
     * This property may only be set prior to constructing Apache connector using {@link ApacheConnectorProvider}.
     * <p/>
     * The value MUST be an instance of {@link java.lang.Boolean}.
     * <p/>
     * The default value is {@code false}.
     * <p/>
     * The name of the configuration property is <tt>{@value}</tt>.
     */
    public static final String PREEMPTIVE_BASIC_AUTHENTICATION = "jersey.config.apache.client.preemptiveBasicAuthentication";

    /**
     * Connection Manager which will be used to create {@link org.apache.http.client.HttpClient}.
     * <p/>
     * The value MUST be an instance of {@link org.apache.http.conn.HttpClientConnectionManager}.
     * <p/>
     * If the property is absent a default Connection Manager will be used
     * ({@link org.apache.http.impl.conn.BasicHttpClientConnectionManager}).
     * If you want to use this client in multi-threaded environment, be sure you override default value with
     * {@link org.apache.http.impl.conn.PoolingHttpClientConnectionManager} instance.
     * <p/>
     * The name of the configuration property is <tt>{@value}</tt>.
     */
    public static final String CONNECTION_MANAGER = "jersey.config.apache.client.connectionManager";

    /**
     * A value of {@code true} indicates that configured connection manager should be shared
     * among multiple Jersey {@link org.glassfish.jersey.client.ClientRuntime} instances. It means that closing
     * a particular {@link org.glassfish.jersey.client.ClientRuntime} instance does not shut down the underlying
     * connection manager automatically. In such case, the connection manager life-cycle
     * should be fully managed by the application code. To release all allocated resources,
     * caller code should especially ensure {@link org.apache.http.conn.HttpClientConnectionManager#shutdown()} gets
     * invoked eventually.
     * <p>
     * This property may only be set prior to constructing Apache connector using {@link ApacheConnectorProvider}.
     * <p/>
     * The value MUST be an instance of {@link java.lang.Boolean}.
     * <p/>
     * The default value is {@code false}.
     * <p/>
     * The name of the configuration property is <tt>{@value}</tt>.
     *
     * @since 2.18
     */
    public static final String CONNECTION_MANAGER_SHARED = "jersey.config.apache.client.connectionManagerShared";

    /**
     * Request configuration for the {@link org.apache.http.client.HttpClient}.
     * Http parameters which will be used to create {@link org.apache.http.client.HttpClient}.
     * <p/>
     * The value MUST be an instance of {@link org.apache.http.client.config.RequestConfig}.
     * <p/>
     * If the property is absent default request configuration will be used.
     * <p/>
     * The name of the configuration property is <tt>{@value}</tt>.
     *
     * @since 2.5
     */
    public static final String REQUEST_CONFIG = "jersey.config.apache.client.requestConfig";

    /**
     * HttpRequestRetryHandler which will be used to create {@link org.apache.http.client.HttpClient}.
     * <p/>
     * The value MUST be an instance of {@link org.apache.http.client.HttpRequestRetryHandler}.
     * <p/>
     * If the property is absent a default retry handler will be used
     * ({@link org.apache.http.impl.client.DefaultHttpRequestRetryHandler}).
     * <p/>
     * The name of the configuration property is <tt>{@value}</tt>.
     */
    public static final String RETRY_HANDLER = "jersey.config.apache.client.retryHandler";

    /**
     * Get the value of the specified property.
     *
     * If the property is not set or the actual property value type is not compatible with the specified type, the method will
     * return {@code null}.
     *
     * @param properties    Map of properties to get the property value from.
     * @param key           Name of the property.
     * @param type          Type to retrieve the value as.
     * @param <T>           Type of the property value.
     * @return Value of the property or {@code null}.
     *
     * @since 2.8
     */
    public static <T> T getValue(final Map<String, ?> properties, final String key, final Class<T> type) {
        return PropertiesHelper.getValue(properties, key, type, null);
    }

    /**
     * Prevents instantiation.
     */
    private ApacheClientProperties() {
        throw new AssertionError("No instances allowed.");
    }
}
