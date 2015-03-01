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

import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Feature;

/**
 * The interface that defines OAuth 2 Authorization Code Grant Flow.
 * <p>
 * The implementation of this interface is capable of performing of the user
 * authorization defined in the OAuth2 specification as "Authorization Code Grant Flow" (OAuth 2 spec defines more
 * Authorization Flows). The result of the authorization
 * is the {@link TokenResult}. The implementation starts the authorization process by construction of a redirect URI
 * to which the user should
 * be redirected (the URI points to authorization consent page hosted by Service Provider). The user
 * grants an access using this page. Service Provider redirects the user back to the
 * our server and the authorization process is finished using the same instance of the interface implementation.
 * </p>
 * <p>
 * To perform the authorization follow these steps:
 * <list>
 * <li>Get the instance of this interface using {@link OAuth2ClientSupport}.</li>
 * <li>Call {@link #start()} method. The method returns redirection uri as a String.</li>
 * <li>Redirect the user to the redirect URI returned from the {@code start} method. If your application deployment
 * does not allow redirection (for example the app is a console application), then provide the redirection URI
 * to the user in other ways.</li>
 * <li>User should authorize your application on the redirect URI.</li>
 * <li>After authorization the Authorization Server redirects the user back to the URI specified
 * by {@link OAuth2CodeGrantFlow.Builder#redirectUri(String)} and provide the {@code code} and {@code state} as
 * a request query parameter. Extract these parameter from the request. If your deployment does not support
 * redirection (your app is not a web server) then Authorization Server will provide the user with
 * {@code code} in other ways (for example display on the html page). You need to get
 * this code from the user. The {@code state} parameter is added to the redirect URI in the {@code start} method and
 * and the same parameter should be returned from the authorization response as a protection against CSRF attacks.</li>
 * <li>Use the {@code code} and {@code state} to finish the authorization process by calling the method
 * {@link #finish(String, String)} supplying the {@code code} and the {@code state} parameter. The method will internally request
 * the access token from the Authorization Server and return it.</li>
 * <li>You can use access token from {@code TokenResult} together with {@link ClientIdentifier} to
 * perform the authenticated requests to the Service Provider. You can also call
 * methods {@link #getAuthorizedClient()} to get {@link Client client} already configured with support
 * for authentication from consumer credentials and access token received during authorization process.
 * </li>
 * </list>
 * </p>
 * <p>
 * Important note: one instance of the interface can be used only for one authorization process. The methods
 * must be called exactly in the order specified by the list above. Therefore the instance is also not
 * thread safe and no concurrent access is expected.
 * </p>
 * <p>
 * Instance must be stored between method calls (between {@code start} and {@code finish})
 * for one user authorization process as the instance keeps
 * internal state of the authorization process.
 * </p>
 *
 * @author Miroslav Fuksa
 * @since 2.3
 */
public interface OAuth2CodeGrantFlow {

    /**
     * Start the authorization process and return redirection URI on which the user should give a consent
     * for our application to access resources.
     *
     * @return URI to which user should be redirected.
     */
    String start();

    /**
     * Finish the authorization process and return the {@link TokenResult}. The method must be called on the
     * same instance after the {@link #start()} method was called and user granted access to this application.
     * <p>
     * The method makes a request to the Authorization Server in order to exchange {@code code} for access token.
     * </p>
     * @param code Code received from the user authorization process.
     * @param state State received from the user authorization response.
     * @return Token result.
     */

    TokenResult finish(String code, String state);

    /**
     * Refresh the access token using a refresh token. This method can be called on newly created instance or on
     * instance on which the authorization flow was already performed.
     *
     * @param refreshToken Refresh token.
     * @return Token result.
     */
    TokenResult refreshAccessToken(String refreshToken);

    /**
     * Return the client configured for performing authorized requests to the Service Provider. The
     * authorization process must be successfully finished by instance by calling methods {@link #start()} and
     * {@link #finish(String, String)}.
     *
     * @return Client configured to add correct {@code Authorization} header to requests.
     */
    public Client getAuthorizedClient();

    /**
     * Return the {@link Feature oauth filter feature} that can be used to configure
     * {@link Client client} instances to perform authenticated requests to the Service Provider.
     * <p>
     * The
     * authorization process must be successfully finished by instance by calling methods {@link #start()} and
     * {@link #finish(String, String)}.
     * </p>
     *
     * @return oauth filter feature configured with received {@code AccessToken}.
     */
    public Feature getOAuth2Feature();

    /**
     * Phase of the Authorization Code Grant Flow.
     */
    enum Phase {
        /**
         * Authorization phase. The phase when user is redirected to the authorization server to authorize
         * the application.
         */
        AUTHORIZATION {
            @Override
            public void property(String key, String value, Map<String, String> authorizationProps,
                                 Map<String, String> accessTokenProps, Map<String, String> refreshTokenProps) {
                nonNullProperty(key, value, authorizationProps);
            }
        },
        /**
         * Requesting the access token phase.
         */
        ACCESS_TOKEN_REQUEST {
            @Override
            public void property(String key,
                                 String value,
                                 Map<String, String> authorizationProps,
                                 Map<String, String> accessTokenProps,
                                 Map<String, String> refreshTokenProps) {
                nonNullProperty(key, value, accessTokenProps);
            }
        },
        /**
         * Refreshing the access token phase.
         */
        REFRESH_ACCESS_TOKEN {
            @Override
            public void property(String key,
                                 String value,
                                 Map<String, String> authorizationProps,
                                 Map<String, String> accessTokenProps,
                                 Map<String, String> refreshTokenProps) {
                nonNullProperty(key, value, refreshTokenProps);
            }
        },
        /**
         * All phases.
         */
        ALL {
            @Override
            public void property(String key,
                                 String value,
                                 Map<String, String> authorizationProps,
                                 Map<String, String> accessTokenProps,
                                 Map<String, String> refreshTokenProps) {
                nonNullProperty(key, value, authorizationProps);
                nonNullProperty(key, value, accessTokenProps);
                nonNullProperty(key, value, refreshTokenProps);
            }
        };

        /**
         * Set property defined by {@code key} and {@code value} to the appropriate
         * property map based on this phase.
         * @param key Property key.
         * @param value Property value.
         * @param authorizationProps Properties used in construction of redirect URI.
         * @param accessTokenProps Properties (parameters) used in access token request.
         * @param refreshTokenProps Properties (parameters) used in request for refreshing the access token.
         */
        public abstract void property(String key, String value,
                                      Map<String, String> authorizationProps,
                                      Map<String, String> accessTokenProps,
                                      Map<String, String> refreshTokenProps);

        private static void nonNullProperty(String key, String value, Map<String, String> props) {
            if (value == null) {
                props.remove(key);
            } else {
                props.put(key, value);
            }
        }

    }

    /**
     * The builder of {@link OAuth2CodeGrantFlow}.
     *
     * @param <T> Type of the builder implementation. This parameter is used for convenience to allow
     *           better implementations of the builders implementing this interface (builder can return
     *           their own specific type instead of type defined by this interface only).
     */
    public interface Builder<T extends Builder> {

        /**
         * Set the access token URI on which the access token can be requested. The URI points to the
         * authorization server and is defined by the Service Provider.
         *
         * @param accessTokenUri  Access token URI.
         * @return Builder instance.
         */
        T accessTokenUri(String accessTokenUri);

        /**
         * Set the URI to which the user should be redirected to authorize our application. The URI points to the
         * authorization server and is defined by the Service Provider.
         *
         * @param authorizationUri Authorization URI.
         * @return Builder instance.
         */
        T authorizationUri(String authorizationUri);

        /**
         * Set the redirect URI to which the user (resource owner) should be redirected after he/she
         * grants access to our application. In most cases, the URI is under control of this application
         * and request done on this URI will be used to extract query parameter {@code code} and {@code state}
         * that will be used in
         * {@link OAuth2CodeGrantFlow#finish(String, String)} method.
         * <p>
         * If URI is not defined by this method, the default value {@code urn:ietf:wg:oauth:2.0:oob} will be used
         * in the Authorization
         * Flow which should cause that {@code code} will be passed to application in other way than request
         * redirection (for example shown to the user using html page).
         * </p>
         *
         * @param redirectUri URI that should receive authorization response from the Service Provider.
         * @return Builder instance.
         */
        T redirectUri(String redirectUri);

        /**
         * Set client identifier of the application that should be authorized.
         *
         * @param clientIdentifier Client identifier.
         * @return Builder instance.
         */
        T clientIdentifier(ClientIdentifier clientIdentifier);

        /**
         * Set a scope to which the application will get authorization grant. Values of this parameter
         * are defined by the Service Provider and defines usually subset of resource and operations available
         * in the Service Provider.
         * <p>
         * The parameter is optional but ServiceProvider might require it.
         * </p>
         *
         * @param scope Scope string.
         * @return Builder instance.
         */
        T scope(String scope);

        /**
         * Set the client that should be used internally by the {@code OAuth1AuthorizationFlow} to make requests to
         * Authorization Server. If this method is not called, it is up to the implementation to create or get
         * any private client instance to perform these requests. This method could be used mainly for
         * performance reasons to avoid creation of new client instances and have control about created client
         * instances used in the application.
         *
         * @param client Client instance.
         * @return Builder instance.
         */
        T client(Client client);

        /**
         * Set the refresh token URI on which the access token can be refreshed using a refresh token.
         * The URI points to the
         * authorization server and is defined by the Service Provider. If the URI is not defined by this method
         * it will be the same as URI defined in {@link #accessTokenUri(String)} (which is the default value
         * defined by the OAuth2 spec).
         * Some providers do not support
         * refreshing access tokens at all.
         *
         * @param refreshTokenUri  Refresh token URI.
         * @return Builder instance.
         */
        T refreshTokenUri(String refreshTokenUri);

        /**
         * Set property (parameter) that will be added to requests or URIs as a query parameters during
         * the Authorization Flow. Default parameters used during the Authorization Flow can be also
         * overridden by this method.
         *
         * @param phase Phase of the flow in which the properties (parameters) should be used. For example by using
         *              a {@link Phase#ACCESS_TOKEN_REQUEST}, the parameter will be added only to the http request
         *              for access token.
         * @param key Property key.
         * @param value Property value.
         * @return Builder instance.
         */
        T property(Phase phase, String key, String value);

        /**
         * Build the {@code OAuth2CodeGrantFlow} instance.
         *
         * @return New instance of {@code OAuth2CodeGrantFlow}.
         */
        OAuth2CodeGrantFlow build();
    }
}
