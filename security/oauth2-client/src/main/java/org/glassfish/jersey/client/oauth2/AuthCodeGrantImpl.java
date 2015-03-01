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

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.ReaderInterceptor;

import javax.inject.Inject;
import javax.inject.Provider;

import org.glassfish.jersey.client.oauth2.internal.LocalizationMessages;
import org.glassfish.jersey.internal.PropertiesDelegate;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.message.MessageBodyWorkers;

/**
 * Default implementation of {@link OAuth2CodeGrantFlow}.
 *
 * @author Miroslav Fuksa
 * @since 2.3
 */
class AuthCodeGrantImpl implements OAuth2CodeGrantFlow {

    /**
     * Builder implementation.
     */
    static class Builder implements OAuth2CodeGrantFlow.Builder {

        private String accessTokenUri;
        private String refreshTokenUri;
        private String authorizationUri;
        private String callbackUri;
        private ClientIdentifier clientIdentifier;
        private Client client;
        private String scope;
        private Map<String, String> authorizationProperties = new HashMap<>();
        private Map<String, String> accessTokenProperties = new HashMap<>();
        private Map<String, String> refreshTokenProperties = new HashMap<>();

        /**
         * Create a new builder.
         */
        public Builder() {
        }

        /**
         * Create a new builder with defined URIs and client id.
         */
        public Builder(final ClientIdentifier clientIdentifier, final String authorizationUri, final String accessTokenUri) {
            this();
            this.accessTokenUri = accessTokenUri;
            this.authorizationUri = authorizationUri;
            this.clientIdentifier = clientIdentifier;
        }

        /**
         * Create a new builder with defined URIs and client id and callback uri.
         */
        public Builder(final ClientIdentifier clientIdentifier, final String authorizationUri, final String accessTokenUri,
                       final String callbackUri) {
            this();
            this.accessTokenUri = accessTokenUri;
            this.authorizationUri = authorizationUri;
            this.callbackUri = callbackUri;
            this.clientIdentifier = clientIdentifier;
        }

        @Override
        public Builder accessTokenUri(final String accessTokenUri) {
            this.accessTokenUri = accessTokenUri;
            return this;
        }

        @Override
        public Builder authorizationUri(final String authorizationUri) {
            this.authorizationUri = authorizationUri;
            return this;
        }

        @Override
        public Builder redirectUri(final String redirectUri) {
            this.callbackUri = redirectUri;
            return this;
        }

        @Override
        public Builder clientIdentifier(final ClientIdentifier clientIdentifier) {
            this.clientIdentifier = clientIdentifier;
            return this;
        }

        @Override
        public Builder scope(final String scope) {
            this.scope = scope;
            return this;
        }

        @Override
        public Builder client(final Client client) {
            this.client = client;
            return this;
        }

        @Override
        public Builder refreshTokenUri(final String refreshTokenUri) {
            this.refreshTokenUri = refreshTokenUri;
            return this;
        }

        @Override
        public Builder property(final OAuth2CodeGrantFlow.Phase phase, final String key, final String value) {
            phase.property(key, value, authorizationProperties, accessTokenProperties, refreshTokenProperties);
            return this;
        }

        String getAccessTokenUri() {
            return accessTokenUri;
        }

        String getRefreshTokenUri() {
            return refreshTokenUri;
        }

        String getAuthorizationUri() {
            return authorizationUri;
        }

        String getScope() {
            return scope;
        }

        String getCallbackUri() {
            return callbackUri;
        }

        ClientIdentifier getClientIdentifier() {
            return clientIdentifier;
        }

        Client getClient() {
            return client;
        }

        Map<String, String> getAuthorizationProperties() {
            return authorizationProperties;
        }

        Map<String, String> getAccessTokenProperties() {
            return accessTokenProperties;
        }

        Map<String, String> getRefreshTokenProperties() {
            return refreshTokenProperties;
        }

        @Override
        public AuthCodeGrantImpl build() {
            return new AuthCodeGrantImpl(authorizationUri, accessTokenUri,
                    callbackUri, refreshTokenUri,
                    clientIdentifier,
                    scope, client, authorizationProperties, accessTokenProperties, refreshTokenProperties);
        }
    }

    private AuthCodeGrantImpl(final String authorizationUri, final String accessTokenUri, final String redirectUri,
                              final String refreshTokenUri,
                              final ClientIdentifier clientIdentifier,
                              final String scope, final Client client, final Map<String, String> authorizationProperties,
                              final Map<String, String> accessTokenProperties,
                              final Map<String, String> refreshTokenProperties) {
        this.accessTokenUri = accessTokenUri;
        this.authorizationUri = authorizationUri;

        this.authorizationProperties = authorizationProperties;
        this.accessTokenProperties = accessTokenProperties;
        this.refreshTokenProperties = refreshTokenProperties;

        if (refreshTokenUri != null) {
            this.refreshTokenUri = refreshTokenUri;
        } else {
            this.refreshTokenUri = accessTokenUri;
        }

        this.clientIdentifier = clientIdentifier;
        this.client = configureClient(client);

        initDefaultProperties(redirectUri, scope);
    }

    private Client configureClient(Client client) {
        if (client == null) {
            client = ClientBuilder.newClient();
        }

        final Configuration config = client.getConfiguration();
        if (!config.isRegistered(AuthCodeGrantImpl.DefaultTokenMessageBodyReader.class)) {
            client.register(AuthCodeGrantImpl.DefaultTokenMessageBodyReader.class);
        }
        if (!config.isRegistered(JacksonFeature.class)) {
            client.register(JacksonFeature.class);
        }

        return client;
    }

    private void setDefaultProperty(final String key, final String value, final Map<String, String>... properties) {
        if (value == null) {
            return;
        }
        for (final Map<String, String> props : properties) {
            if (props.get(key) == null) {
                props.put(key, value);
            }

        }

    }

    private void initDefaultProperties(final String redirectUri, final String scope) {
        setDefaultProperty(OAuth2Parameters.RESPONSE_TYPE, "code", authorizationProperties);
        setDefaultProperty(OAuth2Parameters.CLIENT_ID, clientIdentifier.getClientId(), authorizationProperties,
                accessTokenProperties, refreshTokenProperties);
        setDefaultProperty(OAuth2Parameters.REDIRECT_URI, redirectUri == null
                ? OAuth2Parameters.REDIRECT_URI_UNDEFINED : redirectUri, authorizationProperties, accessTokenProperties);
        setDefaultProperty(OAuth2Parameters.STATE, UUID.randomUUID().toString(), authorizationProperties);
        setDefaultProperty(OAuth2Parameters.SCOPE, scope, authorizationProperties);

        setDefaultProperty(OAuth2Parameters.CLIENT_SECRET, clientIdentifier.getClientSecret(), accessTokenProperties,
                refreshTokenProperties);
        setDefaultProperty(OAuth2Parameters.GrantType.key, OAuth2Parameters.GrantType.AUTHORIZATION_CODE.name().toLowerCase(),
                accessTokenProperties);

        setDefaultProperty(OAuth2Parameters.GrantType.key, OAuth2Parameters.GrantType.REFRESH_TOKEN.name().toLowerCase(),
                refreshTokenProperties);
    }

    private final String accessTokenUri;
    private final String authorizationUri;
    private final String refreshTokenUri;
    private final ClientIdentifier clientIdentifier;

    private final Client client;

    private final Map<String, String> authorizationProperties;
    private final Map<String, String> accessTokenProperties;
    private final Map<String, String> refreshTokenProperties;

    private volatile TokenResult tokenResult;

    @Override
    public String start() {
        final UriBuilder uriBuilder = UriBuilder.fromUri(authorizationUri);
        for (final Map.Entry<String, String> entry : authorizationProperties.entrySet()) {
            uriBuilder.queryParam(entry.getKey(), entry.getValue());
        }
        return uriBuilder.build().toString();
    }

    @Override
    public TokenResult finish(final String authorizationCode, final String state) {
        if (!this.authorizationProperties.get(OAuth2Parameters.STATE).equals(state)) {
            throw new IllegalArgumentException(LocalizationMessages.ERROR_FLOW_WRONG_STATE());
        }

        accessTokenProperties.put(OAuth2Parameters.CODE, authorizationCode);
        final Form form = new Form();
        for (final Map.Entry<String, String> entry : accessTokenProperties.entrySet()) {
            form.param(entry.getKey(), entry.getValue());
        }

        final Response response = client.target(accessTokenUri)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));

        if (response.getStatus() != 200) {
            throw new ProcessingException(LocalizationMessages.ERROR_FLOW_REQUEST_ACCESS_TOKEN(response.getStatus()));
        }
        this.tokenResult = response.readEntity(TokenResult.class);
        return tokenResult;
    }

    @Override
    public TokenResult refreshAccessToken(final String refreshToken) {
        refreshTokenProperties.put(OAuth2Parameters.REFRESH_TOKEN, refreshToken);
        final Form form = new Form();
        for (final Map.Entry<String, String> entry : refreshTokenProperties.entrySet()) {
            form.param(entry.getKey(), entry.getValue());
        }

        final Response response = client.target(refreshTokenUri)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));

        if (response.getStatus() != 200) {
            throw new ProcessingException(LocalizationMessages.ERROR_FLOW_REQUEST_REFRESH_TOKEN(response.getStatus()));
        }

        this.tokenResult = response.readEntity(TokenResult.class);
        return tokenResult;
    }

    @Override
    public Client getAuthorizedClient() {
        return ClientBuilder.newClient().register(getOAuth2Feature());
    }

    @Override
    public Feature getOAuth2Feature() {
        if (this.tokenResult == null) {
            throw new IllegalStateException(LocalizationMessages.ERROR_FLOW_NOT_FINISHED());
        }
        return new OAuth2ClientFeature(tokenResult.getAccessToken());
    }

    static class DefaultTokenMessageBodyReader implements MessageBodyReader<TokenResult> {

        // Provider here prevents circular dependency error from HK2 (workers inject providers and this provider inject workers)
        @Inject
        private Provider<MessageBodyWorkers> workers;

        @Inject
        private Provider<PropertiesDelegate> propertiesDelegateProvider;

        private static Iterable<ReaderInterceptor> EMPTY_INTERCEPTORS = new ArrayList<>();

        @Override
        public boolean isReadable(final Class<?> type,
                                  final Type genericType,
                                  final Annotation[] annotations,
                                  final MediaType mediaType) {
            return type.equals(TokenResult.class);
        }

        @Override
        public TokenResult readFrom(final Class<TokenResult> type, final Type genericType, final Annotation[] annotations,
                                    final MediaType mediaType, final MultivaluedMap<String, String> httpHeaders,
                                    final InputStream entityStream) throws IOException, WebApplicationException {

            final GenericType<Map<String, Object>> mapType = new GenericType<Map<String, Object>>() {
            };

            final Map<String, Object> map = (Map<String, Object>) workers.get().readFrom(mapType.getRawType(),
                    mapType.getType(), annotations,
                    mediaType, httpHeaders,
                    propertiesDelegateProvider.get(),
                    entityStream, EMPTY_INTERCEPTORS, false);

            return new TokenResult(map);
        }
    }

}
