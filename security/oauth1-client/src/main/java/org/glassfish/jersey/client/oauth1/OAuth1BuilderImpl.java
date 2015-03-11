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

import javax.ws.rs.core.Feature;

import org.glassfish.jersey.oauth1.signature.OAuth1Parameters;
import org.glassfish.jersey.oauth1.signature.OAuth1Secrets;

/**
 * OAuth 1 client builder default implementation.
 *
 * @author Miroslav Fuksa
 * @since 2.3
 */
class OAuth1BuilderImpl implements OAuth1Builder {

    private final OAuth1Parameters params;
    private final OAuth1Secrets secrets;
    private ConsumerCredentials consumerCredentials;

    /**
     * Create a new builder instance.
     *
     * @param consumerCredentials Consumer credentials.
     */
    OAuth1BuilderImpl(final ConsumerCredentials consumerCredentials) {
        this(new OAuth1Parameters(), new OAuth1Secrets(), consumerCredentials);
    }

    /**
     * Create a new builder instance.
     *
     * @param params Pre-configured oauth parameters.
     * @param secrets Pre-configured oauth secrets.
     */
    OAuth1BuilderImpl(final OAuth1Parameters params, final OAuth1Secrets secrets) {
        this(params, secrets, new ConsumerCredentials(params.getConsumerKey(), secrets.getConsumerSecret()));
    }

    private OAuth1BuilderImpl(final OAuth1Parameters params, final OAuth1Secrets secrets,
                              final ConsumerCredentials consumerCredentials) {
        this.params = params;
        this.secrets = secrets;

        // spec defines that when no callback uri is used (e.g. client is unable to receive callback
        // as it is a mobile application), the "oob" value should be used.
        if (this.params.getCallback() == null) {
            this.params.setCallback(OAuth1Parameters.NO_CALLBACK_URI_VALUE);
        }
        this.consumerCredentials = consumerCredentials;
    }

    @Override
    public OAuth1BuilderImpl signatureMethod(String signatureMethod) {
        params.setSignatureMethod(signatureMethod);
        return this;
    }

    @Override
    public OAuth1BuilderImpl realm(String realm) {
        params.setRealm(realm);
        return this;
    }

    @Override
    public OAuth1BuilderImpl timestamp(String timestamp) {
        params.setTimestamp(timestamp);
        return this;
    }

    @Override
    public OAuth1BuilderImpl nonce(String timestamp) {
        params.setNonce(timestamp);
        return this;
    }

    @Override
    public OAuth1BuilderImpl version(String timestamp) {
        params.setVersion(timestamp);
        return this;
    }

    @Override
    public FilterFeatureBuilder feature() {
        defineCredentialsParams();
        return new FilterBuilderImpl(params, secrets);
    }

    private void defineCredentialsParams() {
        params.setConsumerKey(consumerCredentials.getConsumerKey());
        secrets.setConsumerSecret(consumerCredentials.getConsumerSecret());
    }

    @Override
    public FlowBuilder authorizationFlow(String requestTokenUri, String accessTokenUri, String authorizationUri) {
        defineCredentialsParams();
        return new OAuth1AuthorizationFlowImpl.Builder(params, secrets, requestTokenUri, accessTokenUri, authorizationUri);
    }

    /**
     * OAuth 1 client filter feature builder default implementation.
     */
    static class FilterBuilderImpl implements FilterFeatureBuilder {

        private final OAuth1Parameters params;
        private final OAuth1Secrets secrets;
        private AccessToken accessToken;

        /**
         * Create a new builder instance.
         *
         * @param params Pre-configured oauth parameters.
         * @param secrets Pre-configured oauth secrets.
         */
        FilterBuilderImpl(OAuth1Parameters params, OAuth1Secrets secrets) {
            this.params = params;
            this.secrets = secrets;
        }

        @Override
        public FilterFeatureBuilder accessToken(AccessToken accessToken) {
            this.accessToken = accessToken;
            return this;
        }

        @Override
        public Feature build() {
            if (accessToken != null) {
                params.setToken(accessToken.getToken());
                secrets.setTokenSecret(accessToken.getAccessTokenSecret());
            }
            return new OAuth1ClientFeature(params, secrets);
        }
    }
}
