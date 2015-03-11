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

package org.glassfish.jersey.client.oauth2;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Feature;

/**
 * Main class to build the Authorization Flow instances and {@link javax.ws.rs.core.Feature client filter feature} that
 * can supports performing of authenticated OAuth requests.
 * <p><b>Authorization flow</b></p>
 * For more information about authorization flow, see {@link OAuth2CodeGrantFlow}.
 * <p><b>Client feature</b></p>
 * <p>
 * Use method {@link #feature(String)} to build the feature. OAuth2 client filter feature registers
 * the support for performing authenticated requests to the
 * Service Provider. The feature uses an access token to initialize
 * the internal {@link javax.ws.rs.container.ContainerRequestFilter filter}
 * which will add {@code Authorization} http header containing OAuth 2 authorization information (based
 * on {@code bearer} tokens).
 * </p>
 *
 * <p>
 * The internal filter can be controlled by properties put into
 * the {@link javax.ws.rs.client.ClientRequestContext client request}
 * using {@link javax.ws.rs.client.ClientRequestContext#setProperty(String, Object)} method. The property key
 * is defined in this class as a static variables
 * ({@link OAuth2ClientSupport#OAUTH2_PROPERTY_ACCESS_TOKEN} (see its javadoc for usage).
 * Using the property a specific
 * access token can be defined for each request.
 * </p>
 * Example of using specific access token for one request:
 * <pre>
 * final Response response = client.target("foo").request()
 *           .property(OAUTH2_PROPERTY_ACCESS_TOKEN, "6ab45ab465e46f54d771a").get();
 * </pre>

 *
 * @author Miroslav Fuksa
 * @since 2.3
 */
public final class OAuth2ClientSupport {
    /**
     * Key of the property that can be attached to the
     * {@link javax.ws.rs.client.ClientRequestContext client request} using
     * {@link javax.ws.rs.client.ClientRequestContext#setProperty(String, Object)} and that
     * defines access token that should be used when generating OAuth {@code Authorization}
     * http header. The property will override the setting of the internal
     * {@link javax.ws.rs.client.ClientRequestFilter filter} for the current request only. This property
     * can be used only when {@link javax.ws.rs.core.Feature OAauth 2 filter feature} is
     * registered into the {@link javax.ws.rs.client.Client}
     * instance.
     * <p>
     * The value of the property must be a {@link String}.
     * </p>
     */
    public static final String OAUTH2_PROPERTY_ACCESS_TOKEN = "jersey.config.client.oauth2.access.token";

    /**
     * Build the {@link Feature client filter feature} from the {@code accessToken} that will add
     * {@code Authorization} http header to the request with the OAuth authorization information.
     *
     *
     * @param accessToken Access token to be used in the authorization header or {@code null}
     *                    if no default access token should be defined. In this case the token
     *                    will have to be set for each request using {@link #OAUTH2_PROPERTY_ACCESS_TOKEN}
     *                    property.
     * @return Client feature.
     */
    public static Feature feature(String accessToken) {
        return new OAuth2ClientFeature(accessToken);
    }

    /**
     * Get the builder of the {@link OAuth2CodeGrantFlow Authorization Code Grant Flow}.
     *
     * @param clientIdentifier Client identifier (id of application that wants to be approved). Issued by the
     *                         Service Provider.
     * @param authorizationUri The URI to which the user should be redirected to authorize our application.
     *                         The URI points to the
     *                          authorization server and is defined by the Service Provider.
     * @param accessTokenUri The access token URI on which the access token can be requested. The URI points to the
     *                          authorization server and is defined by the Service Provider.
     * @return builder of the {@link OAuth2CodeGrantFlow Authorization Code Grant Flow}.
     */
    public static OAuth2CodeGrantFlow.Builder authorizationCodeGrantFlowBuilder(ClientIdentifier clientIdentifier,
                                                                                String authorizationUri,
                                                                                String accessTokenUri) {
        return new AuthCodeGrantImpl.Builder(clientIdentifier, authorizationUri, accessTokenUri);
    }

    /**
     * Get a builder that can be directly used to perform Authorization Code Grant flow defined by
     * Google.
     *
     * @param clientIdentifier Client identifier (id of application that wants to be approved). Issued by the
     *                         Service Provider.
     * @param redirectURI URI to which the user (resource owner) should be redirected after he/she
     *                    grants access to our application or {@code null} if the application
     *                    does not support redirection (eg. is not a web server).
     * @param scope The api to which an access is requested (eg. Google tasks).
     * @return Google builder instance.
     */
    public static OAuth2FlowGoogleBuilder googleFlowBuilder(ClientIdentifier clientIdentifier,
                                                            String redirectURI, String scope) {

        return new OAuth2FlowGoogleBuilder()
                .authorizationUri("https://accounts.google.com/o/oauth2/auth")
                .accessTokenUri("https://accounts.google.com/o/oauth2/token")
                .redirectUri(redirectURI == null ? OAuth2Parameters.REDIRECT_URI_UNDEFINED : redirectURI)
                .clientIdentifier(clientIdentifier)
                .scope(scope);
    }

    /**
     * Get a builder that can be directly used to perform Authorization Code Grant flow defined by
     * Facebook.
     *
     * @param clientIdentifier Client identifier (id of application that wants to be approved). Issued by the
     *                         Service Provider.
     * @param redirectURI URI to which the user (resource owner) should be redirected after he/she
     *                    grants access to our application or {@code null} if the application
     *                    does not support redirection (eg. is not a web server).
     * </p>
     * @return Builder instance.
     */
    public static OAuth2CodeGrantFlow.Builder facebookFlowBuilder(ClientIdentifier clientIdentifier,
                                                                  String redirectURI) {
        return OAuth2FlowFacebookBuilder.getFacebookAuthorizationBuilder(clientIdentifier, redirectURI,
                ClientBuilder.newClient());
    }

    /**
     * Prevent instantiation.
     */
    private OAuth2ClientSupport() {
    }
}

