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

/**
 * Class that contains definition od parameters used in OAuth2.
 *
 * @author Miroslav Fuksa
 * @since 2.3
 */
public final class OAuth2Parameters {
    /**
     * Parameter {@code client_id} that
     * corresponds to ({@link org.glassfish.jersey.client.oauth2.ClientIdentifier#getClientId()}).
     */
    public static final String CLIENT_ID = "client_id";

    /**
     * Parameter {@code client_secret} that
     * corresponds to ({@link ClientIdentifier#getClientSecret()}).
     */
    public static final String CLIENT_SECRET = "client_secret";

    /**
     * Parameter {@code response_type} used in the
     * authorization request. For Authorization Code Grant Flow the value is {@code code}.
     */
    public static final String RESPONSE_TYPE = "response_type";

    /**
     * Parameter {@code response_type} used in the
     * authorization request.
     */
    public static final String REDIRECT_URI = "redirect_uri";

    /**
     * Parameter {@code scope} that defines the scope to which an authorization is requested.
     * Space delimited format. Scope values are defined by the Service Provider.
     */
    public static final String SCOPE = "scope";
    /**
     * State parameter used in the authorization request and authorization
     * response to protect against CSRF attacks.
     */
    public static final String STATE = "state";

    /**
     * Parameter {@code refresh_token} contains Refresh Token (corresponds
     * to {@link org.glassfish.jersey.client.oauth2.TokenResult#getRefreshToken()}).
     */
    public static final String REFRESH_TOKEN = "refresh_token";
    /**
     * Authorization code
     */
    public static final String CODE = "code";
    public static final String REDIRECT_URI_UNDEFINED = "urn:ietf:wg:oauth:2.0:oob";

    /**
     *  Parameter {@code grant_type} used in the access token request.
     */
    public static enum GrantType {
        /**
         * Used to request an access token in the Authorization Code Grant Flow.
         * The parameter key defined by the OAuth2 protocol is equal
         * to the name of this enum value converted to lowercase.
         */
        AUTHORIZATION_CODE,
        /**
         * Used to refresh an access token in the Authorization Code Grant Flow.
         * The parameter key defined by the OAuth2 protocol is equal
         * to the name of this enum value converted to lowercase.
         */
        REFRESH_TOKEN,
        /**
         * Used in Resource Owner Password Credential Grant.
         * The parameter key defined by the OAuth2 protocol is equal
         * to the name of this enum value converted to lowercase.
         */
        PASSWORD,
        /**
         * Used in Client Credentials Flow.
         * The parameter key defined by the OAuth2 protocol is equal
         * to the name of this enum value converted to lowercase.
         */
        CLIENT_CREDENTIALS;

        /**
         * Parameter key name.
         */
        public static final String key = "grant_type";
    }

    /**
     * Prevent instantiation.
     */
    private OAuth2Parameters() {
    }
}
