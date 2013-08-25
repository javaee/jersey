/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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
 * Configuration options specific to the Client API that utilizes {@link ApacheConnector}.
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
     *
     * <p>The value MUST be an instance of {@link org.glassfish.jersey.SslConfigurator}.</p>
     *
     * <p>A default value is not set.</p>
     *
     * <p>The name of the configuration property is <tt>{@value}</tt>.</p>
     */
    public static final String SSL_CONFIG =
            "jersey.config.apache.client.ssl.sslConfig";

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
    public static final String CREDENTIALS_PROVIDER =
            "jersey.config.apache.client.credentialsProvider";

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
    public static final String DISABLE_COOKIES =
            "jersey.config.apache.client.handleCookies";

    /**
     * A value of {@code true} indicates that a client should send an
     * authentication request even before the server gives a 401
     * response.
     * <p/>
     * This property may only be set when constructing a {@link org.glassfish.jersey.apache.connector.ApacheConnector}
     * instance.
     * <p/>
     * The value MUST be an instance of {@link java.lang.Boolean}.
     * <p/>
     * The default value is {@code false}.
     * <p/>
     * The name of the configuration property is <tt>{@value}</tt>.
     */
    public static final String PREEMPTIVE_BASIC_AUTHENTICATION =
            "jersey.config.apache.client.preemptiveBasicAuthentication";

    /**
     * Connection Manager which will be used to create {@link org.apache.http.client.HttpClient}.
     * <p/>
     * The value MUST be an instance of {@link org.apache.http.conn.ClientConnectionManager}.
     * <p/>
     * If the property is absent a default Connection Manager will be used
     * ({@link org.apache.http.impl.conn.BasicClientConnectionManager}).
     * If you want to use this client in multi-threaded environment, be sure you override default value with
     * {@link org.apache.http.impl.conn.PoolingClientConnectionManager} instance.
     * <p/>
     * The name of the configuration property is <tt>{@value}</tt>.
     */
    public static final String CONNECTION_MANAGER =
            "jersey.config.apache.client.connectionManager";

    /**
     * Http parameters which will be used to create {@link org.apache.http.client.HttpClient}.
     * <p/>
     * The value MUST be an instance of {@link org.apache.http.params.HttpParams}.
     * <p/>
     * If the property is absent default http parameters will be used.
     * <p/>
     * The name of the configuration property is <tt>{@value}</tt>.
     */
    public static final String HTTP_PARAMS =
            "jersey.config.apache.client.httpParams";

    /**
     * A value of a URI to configure the proxy host and proxy port to proxy
     * HTTP requests and responses. If the port component of the URI is absent
     * then a default port of {@code 8080} will be selected.
     * <p/>
     * The value MUST be an instance of {@link String} or {@link java.net.URI}.
     * <p/>
     * If the property absent then no proxy will be utilized.
     * <p/>
     * The name of the configuration property is <tt>{@value}</tt>.
     */
    public static final String PROXY_URI =
            "jersey.config.apache.client.proxyUri";

    /**
     * User name which will be used for proxy authentication.
     * <p/>
     * The value MUST be an instance of {@link String}.
     * <p/>
     * If the property absent then no proxy authentication will be utilized.
     * <p/>
     * The name of the configuration property is <tt>{@value}</tt>.
     */
    public static final String PROXY_USERNAME =
            "jersey.config.apache.client.proxyUsername";

    /**
     * Password which will be used for proxy authentication.
     * <p/>
     * The value MUST be an instance of {@link String}.
     * <p/>
     * If the property absent then no proxy authentication will be utilized.
     * <p/>
     * The name of the configuration property is <tt>{@value}</tt>.
     */
    public static final String PROXY_PASSWORD =
            "jersey.config.apache.client.proxyPassword";

    private ApacheClientProperties() {
        // prevents instantiation
    }
}
