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

package org.glassfish.jersey.client.oauth1;

/**
 * The main class to build the support for OAuth 1 into the Jersey client.
 * <p>
 * The support for OAuth is divided into two parts:
 * <list>
 *     <li><b>Authorization Flow:</b> process of acquiring the user
 *     approval for accessing user's resources on the Service Provider. The authorization process is managed
 *     by an implementation of {@link OAuth1AuthorizationFlow} interface. The result of the process is
 *     an {@link AccessToken}.</li>
 *
 *     <li><b>Authenticated Requests:</b> requests done by a {@link javax.ws.rs.client.Client client} are
 *     enhanced by an {@code Authorization} http header that contains OAuth1 authorization information
 *     based on the {@code AccessToken} received from Authorization flow. This support is provided by
 *     {@link javax.ws.rs.core.Feature oauth 1 filter feature} that is registered into client configuration.
 *     </li>
 * </list>
 * This class contains static method that allows to build both OAuth1 features (authorization flow and client feature).
 * </p>
 *
 * @author Miroslav Fuksa
 * @since 2.3
 */
public final class OAuth1ClientSupport {

    /**
     * Key of the property that can be attached to the
     * {@link javax.ws.rs.client.ClientRequestContext client request} using
     * {@link javax.ws.rs.client.ClientRequestContext#setProperty(String, Object)} and that
     * defines {@link org.glassfish.jersey.client.oauth1.ConsumerCredentials consumer credentials} that should be used when generating OAuth {@code Authorization}
     * http header. The property will override the setting of the internal
     * {@link javax.ws.rs.client.ClientRequestFilter filter} for the current request only.
     * <p>
     * The value of the property must be {@link org.glassfish.jersey.client.oauth1.ConsumerCredentials} instance.
     * </p>
     */
    public static final String OAUTH_PROPERTY_CONSUMER_CREDENTIALS = "jersey.config.client.oauth1.consumer.credentials";

    /**
     * Key of the property that can be attached to the
     * {@link javax.ws.rs.client.ClientRequestContext client request} using
     * {@link javax.ws.rs.client.ClientRequestContext#setProperty(String, Object)} and that
     * defines {@link org.glassfish.jersey.client.oauth1.AccessToken access token} that should be used when generating OAuth {@code Authorization}
     * http header. The property will override the setting of the internal
     * {@link javax.ws.rs.client.ClientRequestFilter filter} for the current request only.
     * <p>
     * The value of the property must be {@link org.glassfish.jersey.client.oauth1.AccessToken} instance.
     * </p>
     */
    public static final String OAUTH_PROPERTY_ACCESS_TOKEN = "jersey.config.client.oauth1.access.token";

    /**
     * Key of the property that can be attached to the
     * {@link javax.ws.rs.client.ClientRequestContext client request} using
     * {@link javax.ws.rs.client.ClientRequestContext#setProperty(String, Object)} and that
     * defines {@link org.glassfish.jersey.oauth1.signature.OAuth1Parameters}
     * that should be used when generating OAuth {@code Authorization}
     * http header. The property will override the setting of the internal
     * {@link javax.ws.rs.client.ClientRequestFilter filter} for the current request only.
     * <p>
     * The value of the property must be {@link org.glassfish.jersey.oauth1.signature.OAuth1Parameters} instance.
     * </p>
     * <p>
     * This property is for advanced usage and should not be used if not needed as it can make the filter
     * configuration inconsistent for
     * the request and can produce unwanted results.
     * </p>
     * <p>
     * This property should be used only for configuring an instance of {@link OAuth1ClientFeature OAuth feature}, not the
     * {@link OAuth1AuthorizationFlow Authorization flow}.
     * </p>
     */
    public static final String OAUTH_PROPERTY_OAUTH_PARAMETERS = "jersey.config.client.oauth1.parameters";

    /**
     * Key of the property that can be attached to the
     * {@link javax.ws.rs.client.ClientRequestContext client request} using
     * {@link javax.ws.rs.client.ClientRequestContext#setProperty(String, Object)} and that
     * defines {@link org.glassfish.jersey.oauth1.signature.OAuth1Secrets}
     * that should be used when generating OAuth {@code Authorization}
     * http header. The property will override the setting of the internal
     * {@link javax.ws.rs.client.ClientRequestFilter filter} for the current request only.
     * <p>
     * The value of the property must be {@link org.glassfish.jersey.oauth1.signature.OAuth1Secrets} instance.
     * </p>
     * <p>
     * This
     * property is for advanced usage and should not be used if not needed as it can make the filter
     * configuration inconsistent for
     * the request and can produce unwanted results.
     * </p>
     * <p>
     * This property should be used only for configuring an instance of {@link OAuth1ClientFeature OAuth feature}, not the
     * {@link OAuth1AuthorizationFlow Authorization flow}.
     * </p>
     */
    public static final String OAUTH_PROPERTY_OAUTH_SECRETS = "jersey.config.client.oauth1.secrets";

    /**
     * Get a new builder of OAuth1 client support.
     *
     * @param consumerCredentials Consumer credentials issued by the service provider for the application that
     *                            wants to access data.
     * @return Builder instance.
     */
    public static OAuth1Builder builder(ConsumerCredentials consumerCredentials) {
        return new OAuth1BuilderImpl(consumerCredentials);
    }

    /**
     * Prevent instantiation.
     */
    private OAuth1ClientSupport() {
    }
}
