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
    public static final String FOLLOW_REDIRECTS = "jersey.config.client.followRedirects";

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
     * The value MUST be an instance convertible to {@link java.lang.Integer}.
     * <p />
     * The property defines the size of the chunk in bytes. The property does not enable
     * chunked encoding (it is controlled by {@link #REQUEST_ENTITY_PROCESSING} property).
     * <p />
     * A default value is not set and is {@link org.glassfish.jersey.client.spi.Connector connector}
     * implementation-specific.
     * <p />
     * The name of the configuration property is <tt>{@value}</tt>.
     */
    public static final String CHUNKED_ENCODING_SIZE = "jersey.config.client.chunkedEncodingSize";

    /**
     * Asynchronous thread pool size.
     *
     * The value MUST be an instance of {@link java.lang.Integer}.
     * <p>
     * If the property is absent then thread pool used for async requests will
     * be initialized as default cached thread pool, which creates new thread
     * for every new request, see {@link java.util.concurrent.Executors}. When a
     * value &gt;&nbsp;0 is provided, the created cached thread pool limited to that
     * number of threads will be utilized. Zero or negative values will be ignored.
     * </p>
     * <p>
     * Note that the property is ignored if a custom {@link org.glassfish.jersey.spi.RequestExecutorProvider}
     * is configured in the client runtime.
     * </p>
     * <p>
     * A default value is not set.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     */
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
     * @see org.glassfish.jersey.CommonProperties#JSON_PROCESSING_FEATURE_DISABLE
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
     * @see org.glassfish.jersey.CommonProperties#METAINF_SERVICES_LOOKUP_DISABLE
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
     * {@link org.glassfish.jersey.client.authentication.HttpAuthenticationFeature#digest()}  digest filter}.
     * Cache contains authentication
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

    // TODO Need to implement support for PROXY-* properties in other connectors
    /**
     * The property defines a URI of a HTTP proxy the client connector should use.
     * <p>
     * If the port component of the URI is absent then a default port of {@code 8080} is assumed.
     * If the property absent then no proxy will be utilized.
     * </p>
     * <p>The value MUST be an instance of {@link String}.</p>
     * <p>The default value is {@code null}.</p>
     * <p>The name of the configuration property is <tt>{@value}</tt>.</p>
     *
     * @since 2.5
     */
    public static final String PROXY_URI = "jersey.config.client.proxy.uri";

    /**
     * The property defines a user name which will be used for HTTP proxy authentication.
     * <p>
     * The property is ignored if no {@link #PROXY_URI HTTP proxy URI} has been set.
     * If the property absent then no proxy authentication will be utilized.
     * </p>
     * <p>The value MUST be an instance of {@link String}.</p>
     * <p>The default value is {@code null}.</p>
     * <p>The name of the configuration property is <tt>{@value}</tt>.</p>
     *
     * @since 2.5
     */
    public static final String PROXY_USERNAME = "jersey.config.client.proxy.username";

    /**
     * The property defines a user password which will be used for HTTP proxy authentication.
     * <p>
     * The property is ignored if no {@link #PROXY_URI HTTP proxy URI} has been set.
     * If the property absent then no proxy authentication will be utilized.
     * </p>
     * <p>The value MUST be an instance of {@link String}.</p>
     * <p>The default value is {@code null}.</p>
     * <p>The name of the configuration property is <tt>{@value}</tt>.</p>
     *
     * @since 2.5
     */
    public static final String PROXY_PASSWORD = "jersey.config.client.proxy.password";
    /**
     * The property specified how the entity should be serialized to the output stream by the
     * {@link org.glassfish.jersey.client.spi.Connector connector}; if the buffering
     * should be used or the entity is streamed in chunked encoding.
     * <p>
     * The value MUST be an instance of {@link String} or an enum value {@link RequestEntityProcessing} in the case
     * of programmatic definition of the property. Allowed values are:
     * <ul>
     *     <li><b>{@code BUFFERED}</b>: the entity will be buffered and content length will be send in Content-length header.</li>
     *     <li><b>{@code CHUNKED}</b>: chunked encoding will be used and entity will be streamed.</li>
     * </ul>
     * </p>
     * <p>
     * Default value is {@code CHUNKED}. However, due to limitations some connectors can define different
     * default value (usually if the chunked encoding cannot be properly supported on the {@code Connector}).
     * This detail should be specified in the javadoc of particular connector. For example, {@link HttpUrlConnector}
     * use buffering as the default mode.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     * @since 2.5
     */
    public static final String REQUEST_ENTITY_PROCESSING = "jersey.config.client.request.entity.processing";

    private ClientProperties() {
        // prevents instantiation
    }
}
