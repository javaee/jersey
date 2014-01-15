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
package org.glassfish.jersey.apache.connector;

/**
 * Configuration options specific to the Client API that utilizes {@link ApacheConnectorProvider}.
 *
 * @author jorgeluisw@mac.com
 * @author Paul Sandoz (paul.sandoz at oracle.com)
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 * @author Arul Dhesiaseelan (aruld at acm.org)
 */
public final class ApacheClientProperties {

    /**
     * Support for specifying SSL configuration for HTTPS connections.
     * Used only when making HTTPS requests.
     * <p/>
     * The value MUST be an instance of {@link org.glassfish.jersey.SslConfigurator}.
     * <p/>
     * A default value is not set.
     * <p/>
     * The name of the configuration property is <tt>{@value}</tt>.
     */
    public static final String SSL_CONFIG = "jersey.config.apache.client.ssl.sslConfig";

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
     * Request configuration for the {@link org.apache.http.client.HttpClient}.
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

    private ApacheClientProperties() {
        // prevents instantiation
    }
}
