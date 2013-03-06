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
 */
public final class ClientProperties {
    /**
     * Automatic redirection. A value of {@code true} declares that the client will
     * automatically redirect to the URI declared in 3xx responses.
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
     * The value MUST be an instance convertible to {@link java.lang.Integer}.
     * A value of zero (0) is equivalent to an interval of infinity.
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
     * The value MUST be an instance convertible to {@link java.lang.Integer}.
     * A value of zero (0) is equivalent to an interval of infinity.
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
     * If the property is absent then chunked encoding will not be used.
     * A value &lt= 0 declares that chunked encoding will be used with
     * the default chunk size. A value &gt 0 declares that chunked encoding
     * will be used with the value as the declared chunk size.
     * <p />
     * A default value is not set.
     * <p />
     * The name of the configuration property is <tt>{@value}</tt>.
     */
    // TODO add support (ported from Jersey 1.x).
    public static final String CHUNKED_ENCODING_SIZE = "jersey.config.client.chunkedEncodingSize";

    /**
     * Automatic response buffering in case of an exception.
     *
     * A value of {@code true} declares that the client will automatically read &
     * buffer the response entity (if any) and close all resources associated with
     * the response.
     *
     * The value MUST be an instance convertible to {@link java.lang.Boolean}.
     * <p />
     * The default value is {@code true}.
     * <p />
     * The name of the configuration property is <tt>{@value}</tt>.
     */
    // TODO add support (ported from Jersey 1.x).
    public static final String BUFFER_RESPONSE_ENTITY_ON_EXCEPTION = "jersey.config.client.bufferResponseEntityOnException";

    /**
     * Asynchronous thread pool size.
     *
     * The value MUST be an instance of {@link java.lang.Integer}.
     * <p />
     * If the property is absent then thread pool used for async requests will
     * be initialized as default cached thread pool, which creates new thread
     * for every new request, see {@link java.util.concurrent.Executors}. When
     * a value &gt; 0 is provided, the created cached thread pool limited to that
     * number of threads will be utilized.
     * <p />
     * A default value is not set.
     * <p />
     * The name of the configuration property is <tt>{@value}</tt>.
     */
    // TODO add support (ported from Jersey 1.x).
    public static final String ASYNC_THREADPOOL_SIZE = "jersey.config.client.async.threadPoolSize";

    /**
     * If {@link org.glassfish.jersey.client.filter.EncodingFilter} is registered, this property indicates the value
     * of Content-Encoding property the filter should be adding.
     *
     * <p>The value MUST be an instance of {@link String}.</p>
     * <p>The default value is {@code null}.</p>
     * <p>The name of the configuration property is <tt>{@value}</tt>.</p>
     */
    public static final String USE_ENCODING = "jersey.config.client.useEncoding";

    /**
     * A value of {@code true} declares that the client will try to set unsupported HTTP method
     * to {@link java.net.HttpURLConnection} via reflection.
     * <p>
     * NOTE: Enabling this feature might cause security related warnings/errors and it might break when
     * other JDK implementation is used. <b>Use only when you know what you are doing.</b>
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
     * By default auto-discovery on client is automatically enabled if global property
     * {@value org.glassfish.jersey.CommonProperties#FEATURE_DISABLE_AUTO_DISCOVERY} is not disabled. If set then the client
     * property value overrides the global property value.
     * <p>
     * The default value is {@code false}.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     *
     * @see org.glassfish.jersey.CommonProperties#FEATURE_DISABLE_AUTO_DISCOVERY
     */
    public static final String FEATURE_DISABLE_AUTO_DISCOVERY = CommonProperties.FEATURE_DISABLE_AUTO_DISCOVERY + ".client";

    /**
     * If {@code true} then disable registration of Json Processing (JSR-353) feature on client.
     * <p>
     * By default Json Processing on client is automatically enabled if global property
     * {@value org.glassfish.jersey.CommonProperties#FEATURE_DISABLE_JSON_PROCESSING} is not disabled. If set then the client
     * property value overrides the global property value.
     * <p>
     * The default value is {@code false}.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     *
     * @see org.glassfish.jersey.CommonProperties#FEATURE_DISABLE_JSON_PROCESSING
     */
    public static final String FEATURE_DISABLE_JSON_PROCESSING = CommonProperties.FEATURE_DISABLE_JSON_PROCESSING + ".client";

    private ClientProperties() {
        // prevents instantiation
    }
}
