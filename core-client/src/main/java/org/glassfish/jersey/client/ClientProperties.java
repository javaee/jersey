/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.client;

import org.glassfish.jersey.CommonProperties;

/**
 * Jersey client implementation configuration properties.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @author Libor Kramolis (libor.kramolis at oracle.com)
 */
public final class ClientProperties {

    /**
     * Automatic redirection. A value of {@code true} declares that the client
     * will automatically redirect to the URI declared in 3xx responses.
     *
     * The value MUST be an instance convertible to {@link java.lang.Boolean}.
     * <p />
     * The default value is {@code true}.
     * <p />
     * The name of the configuration property is <tt>{@value}</tt>.
     */
    public static final String FOLLOW_REDIRECTS =
            "jersey.config.client.followRedirects";
    /**
     * Read timeout interval, in milliseconds.
     *
     * The value MUST be an instance convertible to {@link java.lang.Integer}. A
     * value of zero (0) is equivalent to an interval of infinity.
     *
     * <p />
     * The default value is infinity (0).
     * <p />
     * The name of the configuration property is <tt>{@value}</tt>.
     */
    public static final String READ_TIMEOUT = "jersey.config.client.readTimeout";
    /**
     * Connect timeout interval, in milliseconds.
     *
     * The value MUST be an instance convertible to {@link java.lang.Integer}. A
     * value of zero (0) is equivalent to an interval of infinity.
     * <p />
     * The default value is infinity (0).
     * <p />
     * The name of the configuration property is <tt>{@value}</tt>.
     */
    public static final String CONNECT_TIMEOUT = "jersey.config.client.connectTimeout";
    /**
     * Chunked encoding size.
     *
     * The value MUST be an instance convertible to {@link java.lang.Integer}.
     * <p />
     * If the property is absent then chunked encoding will not be used. A value
     * &lt= 0 declares that chunked encoding will be used with the default chunk
     * size. A value &gt 0 declares that chunked encoding will be used with the
     * value as the declared chunk size.
     * <p />
     * Due to the bug in {@link java.net.HttpURLConnection} using chunk encoding
     * with {@link HttpUrlConnector} might cause unpredictable problems.
     * <p />
     * A default value is not set.
     * <p />
     * The name of the configuration property is <tt>{@value}</tt>.
     */
    public static final String CHUNKED_ENCODING_SIZE = "jersey.config.client.chunkedEncodingSize";

    /**
     * Asynchronous thread pool size.
     *
     * The value MUST be an instance of {@link java.lang.Integer}.
     * <p />
     * If the property is absent then thread pool used for async requests will
     * be initialized as default cached thread pool, which creates new thread
     * for every new request, see {@link java.util.concurrent.Executors}. When a
     * value &gt; 0 is provided, the created cached thread pool limited to that
     * number of threads will be utilized.
     * <p />
     * A default value is not set.
     * <p />
     * The name of the configuration property is <tt>{@value}</tt>.
     */
    // TODO add support in default connector (ported from Jersey 1.x);
    public static final String ASYNC_THREADPOOL_SIZE = "jersey.config.client.async.threadPoolSize";
    /**
     * If {@link org.glassfish.jersey.client.filter.EncodingFilter} is
     * registered, this property indicates the value of Content-Encoding
     * property the filter should be adding.
     *
     * <p>The value MUST be an instance of {@link String}.</p>
     * <p>The default value is {@code null}.</p>
     * <p>The name of the configuration property is <tt>{@value}</tt>.</p>
     */
    public static final String USE_ENCODING = "jersey.config.client.useEncoding";
    /**
     * A value of {@code true} declares that the client will try to set
     * unsupported HTTP method to {@link java.net.HttpURLConnection} via
     * reflection.
     * <p>
     * NOTE: Enabling this feature might cause security related warnings/errors
     * and it might break when other JDK implementation is used. <b>Use only
     * when you know what you are doing.</b>
     * </p>
     * <p>The value MUST be an instance of {@link java.lang.Boolean}.</p>
     * <p>The default value is {@code false}.</p>
     * <p>The name of the configuration property is <tt>{@value}</tt>.</p>
     */
    public static final String HTTP_URL_CONNECTION_SET_METHOD_WORKAROUND =
            "jersey.config.client.httpUrlConnection.setMethodWorkaround";
    /**
     * If {@code true} then disable auto-discovery on the client.
     * <p>
     * By default auto-discovery on client is automatically enabled if global
     * property
     * {@value org.glassfish.jersey.CommonProperties#FEATURE_AUTO_DISCOVERY_DISABLE}
     * is not disabled. If set then the client property value overrides the
     * global property value.
     * <p>
     * The default value is {@code false}.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     *
     * @see org.glassfish.jersey.CommonProperties#FEATURE_AUTO_DISCOVERY_DISABLE
     */
    public static final String FEATURE_AUTO_DISCOVERY_DISABLE = CommonProperties.FEATURE_AUTO_DISCOVERY_DISABLE + ".client";
    /**
     * An integer value that defines the buffer size used to buffer client-side
     * request entity in order to determine its size and set the value of HTTP
     * <tt>{@value javax.ws.rs.core.HttpHeaders#CONTENT_LENGTH}</tt> header.
     * <p>
     * If the entity size exceeds the configured buffer size, the buffering
     * would be cancelled and the entity size would not be determined. Value
     * less or equal to zero disable the buffering of the entity at all.
     * </p>
     * This property can be used on the client side to override the outbound
     * message buffer size value - default or the global custom value set using
     * the
     * {@value org.glassfish.jersey.CommonProperties#OUTBOUND_CONTENT_LENGTH_BUFFER}
     * global property.
     * <p>
     * The default value is
     * <tt>{@value org.glassfish.jersey.message.internal.CommittingOutputStream#DEFAULT_BUFFER_SIZE}</tt>.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     *
     * @since 2.2
     */
    public static final String OUTBOUND_CONTENT_LENGTH_BUFFER = CommonProperties.OUTBOUND_CONTENT_LENGTH_BUFFER + ".client";
    /**
     * If {@code true} then disable configuration of Json Processing (JSR-353)
     * feature on client.
     * <p>
     * By default Json Processing on client is automatically enabled if global
     * property
     * {@value org.glassfish.jersey.CommonProperties#JSON_PROCESSING_FEATURE_DISABLE}
     * is not disabled. If set then the client property value overrides the
     * global property value.
     * <p>
     * The default value is {@code false}.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     *
     * @see
     * org.glassfish.jersey.CommonProperties#JSON_PROCESSING_FEATURE_DISABLE
     */
    public static final String JSON_PROCESSING_FEATURE_DISABLE = CommonProperties.JSON_PROCESSING_FEATURE_DISABLE + ".client";
    /**
     * If {@code true} then disable META-INF/services lookup on client.
     * <p>
     * By default Jersey lookups SPI implementations described by
     * META-INF/services/* files. Then you can register appropriate provider
     * classes by {@link javax.ws.rs.core.Application}.
     * </p>
     * <p>
     * The default value is {@code false}.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     *
     * @see
     * org.glassfish.jersey.CommonProperties#METAINF_SERVICES_LOOKUP_DISABLE
     */
    public static final String METAINF_SERVICES_LOOKUP_DISABLE = CommonProperties.METAINF_SERVICES_LOOKUP_DISABLE + ".client";
    /**
     * If {@code true} then disable configuration of MOXy Json feature on
     * client.
     * <p>
     * By default MOXy Json on client is automatically enabled if global
     * property
     * {@value org.glassfish.jersey.CommonProperties#MOXY_JSON_FEATURE_DISABLE}
     * is not disabled. If set then the client property value overrides the
     * global property value.
     * </p>
     * <p>
     * The default value is {@code false}.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     *
     * @see org.glassfish.jersey.CommonProperties#MOXY_JSON_FEATURE_DISABLE
     * @since 2.1
     */
    public static final String MOXY_JSON_FEATURE_DISABLE = CommonProperties.MOXY_JSON_FEATURE_DISABLE + ".client";
    /**
     * If {@code true}, the strict validation of HTTP specification compliance
     * will be suppressed.
     * <p>
     * By default, Jersey client runtime performs certain HTTP compliance checks
     * (such as which HTTP methods can facilitate non-empty request entities
     * etc.) in order to fail fast with an exception when user tries to
     * establish a communication non-compliant with HTTP specification. Users
     * who need to override these compliance checks and avoid the exceptions
     * being thrown by Jersey client runtime for some reason, can set this
     * property to {@code true}. As a result, the compliance issues will be
     * merely reported in a log and no exceptions will be thrown.
     * </p>
     * <p>
     * Note that the property suppresses the Jersey layer exceptions. Chances
     * are that the non-compliant behavior will cause different set of
     * exceptions being raised in the underlying I/O connector layer.
     * </p>
     * <p>
     * This property can be configured in a client runtime configuration or
     * directly on an individual request. In case of conflict, request-specific
     * property value takes precedence over value configured in the runtime
     * configuration.
     * </p>
     * <p>
     * The default value is {@code false}.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     *
     * @since 2.2
     */
    public static final String SUPPRESS_HTTP_COMPLIANCE_VALIDATION =
            "jersey.config.client.suppressHttpComplianceValidation";


    /**
     * The property defines the size of digest cache in the
     * {@link org.glassfish.jersey.client.filter.HttpDigestAuthFilter}. Cache contains authentication
     * schemes for different request URIs.
     * <p\>
     * The value MUST be an instance of {@link java.lang.Integer} and it must be
     * higher or equal to 1.
     * </p>
     * <p>
     * The default value is {@code 1000}.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     *
     * @since 2.3
     */
    public static final String DIGESTAUTH_URI_CACHE_SIZELIMIT = "jersey.config.client.digestAuthUriCacheSizeLimit";

    /**
     * If {@code true}, {@link HttpUrlConnector} (if used) will assume the content length
     * from the value of {@value javax.ws.rs.core.HttpHeaders#CONTENT_LENGTH} request
     * header (if present).
     * <p>
     * When this property is enabled and the request has a valid non-zero content length
     * value specified in its {@value javax.ws.rs.core.HttpHeaders#CONTENT_LENGTH} request
     * header, that this value will be used as an input to the
     * {@link java.net.HttpURLConnection#setFixedLengthStreamingMode(int)}} method call
     * invoked on the underlying {@link java.net.HttpURLConnection connection}.
     * This will suppress the entity buffering in the @{code HttpURLConnection},
     * which is undesirable in certain scenarios, e.g. when streaming large entities.
     * </p>
     * <p>
     * Note that the content length value defined in the request header must exactly match
     * the real size of the entity. If the {@link javax.ws.rs.core.HttpHeaders#CONTENT_LENGTH} header
     * is explicitly specified in a request, this property will be ignored and the
     * request entity will be still buffered by the underlying @{code HttpURLConnection} infrastructure.
     * </p>
     * <p>
     * This property also overrides the behaviour enabled by the {@link #CHUNKED_ENCODING_SIZE} property.
     * Chunked encoding will only be used, if the size is not specified in the header of the request.
     * </p>
     * <p>
     * Note that this property only applies to client run-times that are configured to use the default
     * {@link HttpUrlConnector} as the client connector. The property is ignored by other connectors.
     * </p>
     * <p>
     * The default value is {@code false}.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     *
     * @since 2.4
     */
    public static final String HTTP_URL_CONNECTOR_FIX_LENGTH_STREAMING =
            "jersey.config.client.httpUrlConnector.fixLength.streaming";

    private ClientProperties() {
        // prevents instantiation
    }
}
