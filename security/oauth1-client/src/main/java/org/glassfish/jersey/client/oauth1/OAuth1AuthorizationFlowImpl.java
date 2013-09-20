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

package org.glassfish.jersey.client.oauth1;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.client.oauth1.internal.LocalizationMessages;
import org.glassfish.jersey.internal.util.collection.Value;
import org.glassfish.jersey.oauth1.signature.OAuth1Parameters;
import org.glassfish.jersey.oauth1.signature.OAuth1Secrets;

/**
 * Default implementation of {@link OAuth1AuthorizationFlow}. The instance is used
 * to perform authorization flows.
 *
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 * @since 2.3
 */
class OAuth1AuthorizationFlowImpl implements OAuth1AuthorizationFlow {

    /**
     * OAuth1AuthorizationFlowImpl builder.
     */
    static class Builder implements OAuth1Builder.FlowBuilder {

        private final ConsumerCredentials consumerCredentials;
        private String requestTokenUri;
        private String accessTokenUri;
        private String authorizationUri;
        private Client client;
        private String callbackUri;


        /**
         * Create a new builder.
         * @param consumerCredentials Consumer credentials.
         * @param requestTokenUri Request token uri.
         * @param accessTokenUri Access token uri.
         * @param authorizationUri Authorization uri.
         */
        public Builder(ConsumerCredentials consumerCredentials, String requestTokenUri, String accessTokenUri,
                       String authorizationUri) {
            this.consumerCredentials = consumerCredentials;
            this.requestTokenUri = requestTokenUri;
            this.accessTokenUri = accessTokenUri;
            this.authorizationUri = authorizationUri;
        }

        public Builder callbackUri(String callbackUri) {
            this.callbackUri = callbackUri;
            return this;
        }

        @Override
        public Builder client(Client client) {
            this.client = client;
            return this;
        }

        public OAuth1AuthorizationFlowImpl build() {
            return new OAuth1AuthorizationFlowImpl(consumerCredentials, requestTokenUri, accessTokenUri, authorizationUri,
                    callbackUri, client);
        }
    }


    /** The OAuth parameters to be used in generating signature. */
    private final OAuth1Parameters parameters;

    /** The OAuth secrets to be used in generating signature. */
    private final OAuth1Secrets secrets;

    private final String requestTokenUri;
    private final String accessTokenUri;
    private final String authorizationUri;


    private final Client client;
    private final ConsumerCredentials consumerCredentials;

    private volatile AccessToken accessToken;
    private final Value<Feature> oAuth1ClientFilterFeature = new Value<Feature>() {
        @Override
        public Feature get() {
            return OAuth1ClientSupport.builder(consumerCredentials).feature()
                    .accessToken(accessToken).build();
        }
    };


    private OAuth1AuthorizationFlowImpl(ConsumerCredentials consumerCredentials, String requestTokenUri,
                                        String accessTokenUri, String authorizationUri, String callbackUri,
                                        Client client) {
        this.parameters = new OAuth1Parameters().consumerKey(consumerCredentials.getConsumerKey());
        this.secrets = new OAuth1Secrets().consumerSecret(consumerCredentials.getConsumerSecret());
        this.consumerCredentials = consumerCredentials;
        this.requestTokenUri = requestTokenUri;
        this.accessTokenUri = accessTokenUri;
        this.authorizationUri = authorizationUri;


        if (client != null) {
            this.client = client;
        } else {
            this.client = ClientBuilder.newBuilder().build();
        }

        if (!this.client.getConfiguration().isRegistered(OAuth1ClientFeature.class)) {
            final Feature filterFeature = OAuth1ClientSupport.builder(consumerCredentials)
                    .feature().build();
            this.client.register(filterFeature);
        }

        if (callbackUri != null) {
            this.parameters.callback(callbackUri);
        }

        if (secrets.getConsumerSecret() == null || parameters.getConsumerKey() == null) {
            throw new IllegalStateException(LocalizationMessages.ERROR_CONFIGURATION_MISSING_CONSUMER());
        }
    }


    private Invocation.Builder addPropeties(Invocation.Builder invocationBuilder) {
        return invocationBuilder
                .property(OAuth1ClientSupport.OAUTH_PROPERTY_OAUTH_PARAMETERS, parameters)
                .property(OAuth1ClientSupport.OAUTH_PROPERTY_OAUTH_SECRETS, secrets);
    }


    public String start() {
        final Response response = addPropeties(client.target(requestTokenUri).request())
                .post(null);
        if (response.getStatus() != 200) {
            throw new RuntimeException(LocalizationMessages.ERROR_REQUEST_REQUEST_TOKEN(response.getStatus()));
        }
        final MultivaluedMap<String, String> formParams = response.readEntity(Form.class).asMap();
        parameters.token(formParams.getFirst(OAuth1Parameters.TOKEN));
        secrets.tokenSecret(formParams.getFirst(OAuth1Parameters.TOKEN_SECRET));

        return UriBuilder.fromUri(authorizationUri).queryParam(OAuth1Parameters.TOKEN, parameters.getToken())
                .build().toString();
    }

    public AccessToken finish(String verifier) {
        parameters.setVerifier(verifier);
        final Response response = addPropeties(client.target(accessTokenUri).request()).post(null);
        // accessToken request failed
        if (response.getStatus() >= 400) {
            throw new RuntimeException(LocalizationMessages.ERROR_REQUEST_ACCESS_TOKEN(response.getStatus()));
        }
        final Form form = response.readEntity(Form.class);
        String accessToken = form.asMap().getFirst(OAuth1Parameters.TOKEN);
        final String accessTokenSecret = form.asMap().getFirst(OAuth1Parameters.TOKEN_SECRET);

        if (accessToken == null) {
            throw new NotAuthorizedException(LocalizationMessages.ERROR_REQUEST_ACCESS_TOKEN_NULL());
        }

        parameters.token(accessToken);
        secrets.tokenSecret(accessTokenSecret);
        final AccessToken resultToken = new AccessToken(parameters.getToken(), secrets.getTokenSecret());
        this.accessToken = resultToken;
        return resultToken;
    }

    @Override
    public Client getAuthorizedClient() {
        return ClientBuilder.newClient().register(getOAuth1Feature());
    }

    @Override
    public Feature getOAuth1Feature() {
        if (this.accessToken == null) {
            throw new IllegalStateException(LocalizationMessages.ERROR_FLOW_NOT_FINISHED());
        }
        return oAuth1ClientFilterFeature.get();
    }
}
