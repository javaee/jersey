/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.server.oauth1;

import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import org.glassfish.jersey.internal.util.collection.ImmutableMultivaluedMap;

/** Default in-memory implementation of OAuth1Provider. Stores consumers and tokens
 * in static hash maps. Provides some additional helper methods for consumer
 * and token management (registering new consumers, retrieving a list of all
 * registered consumers per owner, listing the authorized tokens per principal,
 * revoking tokens, etc.)
 *
 * @author Martin Matula
 * @author Miroslav Fuksa
 */
@Provider
public class DefaultOAuth1Provider implements OAuth1Provider {

    private static final ConcurrentHashMap<String, Consumer> consumerByConsumerKey = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Token> accessTokenByTokenString = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Token> requestTokenByTokenString = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, String> verifierByTokenString = new ConcurrentHashMap<>();

    @Override
    public Consumer getConsumer(final String consumerKey) {
        return consumerByConsumerKey.get(consumerKey);
    }

    /**
     * Register a new consumer.
     *
     * @param owner Identifier of the owner that registers the consumer (user ID or similar).
     * @param attributes Additional attributes (name-values pairs - to store additional
     * information about the consumer, such as name, URI, description, etc.)
     * @return {@link Consumer} object for the newly registered consumer.
     */
    public Consumer registerConsumer(final String owner, final MultivaluedMap<String, String> attributes) {
        return registerConsumer(owner, newUUIDString(), newUUIDString(), attributes);
    }

    /**
     * Register a new consumer configured with Consumer Key.
     *
     * @param owner Identifier of the owner that registers the consumer (user ID or similar).
     * @param key Consumer key.
     * @param secret Consumer key secret.
     * @param attributes Additional attributes (name-values pairs - to store additional
     * information about the consumer, such as name, URI, description, etc.)
     * @return {@link Consumer} object for the newly registered consumer.
     */
    public Consumer registerConsumer(final String owner,
                                     final String key,
                                     final String secret,
                                     final MultivaluedMap<String, String> attributes) {
        final Consumer c = new Consumer(key, secret, owner, attributes);
        consumerByConsumerKey.put(c.getKey(), c);
        return c;
    }

    /** Returns a set of consumers registered by a given owner.
     *
     * @param owner Identifier of the owner that registered the consumers to be retrieved.
     * @return consumers registered by the owner.
     */
    public Set<Consumer> getConsumers(final String owner) {
        final Set<Consumer> result = new HashSet<>();
        for (final Consumer consumer : consumerByConsumerKey.values()) {
            if (consumer.getOwner().equals(owner)) {
                result.add(consumer);
            }
        }
        return result;
    }

    /** Returns a list of access tokens authorized with the supplied principal name.
     *
     * @param principalName Principal name for which to retrieve the authorized tokens.
     * @return authorized access tokens.
     */
    public Set<Token> getAccessTokens(final String principalName) {
        final Set<Token> tokens = new HashSet<>();
        for (final Token token : accessTokenByTokenString.values()) {
            if (principalName.equals(token.getPrincipal().getName())) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    /** Authorizes a request token for given principal and roles and returns
     * verifier.
     *
     * @param token Request token to authorize.
     * @param userPrincipal User principal to authorize the token for.
     * @param roles Set of roles to authorize the token for.
     * @return OAuth verifier value for exchanging this token for an access token.
     */
    public String authorizeToken(final Token token, final Principal userPrincipal, final Set<String> roles) {
        final Token authorized = token.authorize(userPrincipal, roles);
        requestTokenByTokenString.put(token.getToken(), authorized);
        final String verifier = newUUIDString();
        verifierByTokenString.put(token.getToken(), verifier);
        return verifier;
    }

    /** Checks if the supplied token is authorized for a given principal name
     * and if so, revokes the authorization.
     *
     * @param token Access token to revoke the authorization for.
     * @param principalName Principal name the token is currently authorized for.
     */
    public void revokeAccessToken(final String token, final String principalName) {
        final Token t = (Token) getAccessToken(token);
        if (t != null && t.getPrincipal().getName().equals(principalName)) {
            accessTokenByTokenString.remove(token);
        }
    }

    /** Generates a new non-guessable random string (used for token/customer
     * strings, secrets and verifier.
     *
     * @return Random UUID string.
     */
    protected String newUUIDString() {
        final String tmp = UUID.randomUUID().toString();
        return tmp.replaceAll("-", "");
    }

    @Override
    public Token getRequestToken(final String token) {
        return requestTokenByTokenString.get(token);
    }

    @Override
    public OAuth1Token newRequestToken(final String consumerKey,
                                       final String callbackUrl,
                                       final Map<String, List<String>> attributes) {
        final Token rt = new Token(newUUIDString(), newUUIDString(), consumerKey, callbackUrl, attributes);
        requestTokenByTokenString.put(rt.getToken(), rt);
        return rt;
    }

    @Override
    public OAuth1Token newAccessToken(final OAuth1Token requestToken, final String verifier) {
        if (verifier == null || requestToken == null || !verifier.equals(verifierByTokenString.remove(requestToken.getToken()))) {
            return null;
        }
        final Token token = requestTokenByTokenString.remove(requestToken.getToken());
        if (token == null) {
            return null;
        }
        final Token at = new Token(newUUIDString(), newUUIDString(), token);
        accessTokenByTokenString.put(at.getToken(), at);
        return at;
    }

    public void addAccessToken(final String token,
                               final String secret,
                               final String consumerKey,
                               final String callbackUrl,
                               final Principal principal,
                               final Set<String> roles,
                               final MultivaluedMap<String, String> attributes) {
        final Token accessToken = new Token(token, secret, consumerKey, callbackUrl, principal, roles, attributes);

        accessTokenByTokenString.put(accessToken.getToken(), accessToken);
    }

    @Override
    public OAuth1Token getAccessToken(final String token) {
        return accessTokenByTokenString.get(token);
    }

    /** Simple read-only implementation of {@link OAuth1Consumer}.
     */
    public static class Consumer implements OAuth1Consumer {

        private final String key;
        private final String secret;
        private final String owner;
        private final MultivaluedMap<String, String> attributes;

        private Consumer(final String key, final String secret, final String owner, final Map<String, List<String>> attributes) {
            this.key = key;
            this.secret = secret;
            this.owner = owner;
            this.attributes = getImmutableMap(attributes);
        }

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public String getSecret() {
            return secret;
        }

        /** Returns identifier of owner of this consumer - i.e. who registered
         * the consumer.
         *
         * @return consumer owner
         */
        public String getOwner() {
            return owner;
        }

        /** Returns additional attributes associated with the consumer (e.g. name,
         * URI, description, etc.)
         *
         * @return name-values pairs of additional attributes
         */
        public MultivaluedMap<String, String> getAttributes() {
            return attributes;
        }

        @Override
        public Principal getPrincipal() {
            return null;
        }

        @Override
        public boolean isInRole(final String role) {
            return false;
        }
    }

    private static MultivaluedMap<String, String> getImmutableMap(final Map<String, List<String>> map) {
        final MultivaluedHashMap<String, String> newMap = new MultivaluedHashMap<>();
        for (final Map.Entry<String, List<String>> entry : map.entrySet()) {
            newMap.put(entry.getKey(), entry.getValue());
        }
        return newMap;
    }

    /** Simple immutable implementation of {@link OAuth1Token}.
     *
     */
    public class Token implements OAuth1Token {

        private final String token;
        private final String secret;
        private final String consumerKey;
        private final String callbackUrl;
        private final Principal principal;
        private final Set<String> roles;
        private final MultivaluedMap<String, String> attribs;

        protected Token(final String token, final String secret, final String consumerKey, final String callbackUrl,
                        final Principal principal, final Set<String> roles, final MultivaluedMap<String, String> attributes) {
            this.token = token;
            this.secret = secret;
            this.consumerKey = consumerKey;
            this.callbackUrl = callbackUrl;
            this.principal = principal;
            this.roles = roles;
            this.attribs = attributes;
        }

        public Token(final String token,
                     final String secret,
                     final String consumerKey,
                     final String callbackUrl,
                     final Map<String, List<String>> attributes) {
            this(token, secret, consumerKey, callbackUrl, null, Collections.<String>emptySet(),
                    new ImmutableMultivaluedMap<>(getImmutableMap(attributes)));
        }

        public Token(final String token, final String secret, final Token requestToken) {
            this(token, secret, requestToken.getConsumer().getKey(), null,
                    requestToken.principal, requestToken.roles, ImmutableMultivaluedMap.<String, String>empty());
        }

        @Override
        public String getToken() {
            return token;
        }

        @Override
        public String getSecret() {
            return secret;
        }

        @Override
        public OAuth1Consumer getConsumer() {
            return DefaultOAuth1Provider.this.getConsumer(consumerKey);
        }

        @Override
        public MultivaluedMap<String, String> getAttributes() {
            return attribs;
        }

        @Override
        public Principal getPrincipal() {
            return principal;
        }

        @Override
        public boolean isInRole(final String role) {
            return roles.contains(role);
        }

        /** Returns callback URL for this token (applicable just to request tokens)
         *
         * @return callback url
         */
        public String getCallbackUrl() {
            return callbackUrl;
        }

        /** Authorizes this token - i.e. generates a clone with principal and roles set
         * to the passed values.
         *
         * @param principal Principal to add to the token.
         * @param roles Roles to add to the token.
         * @return Cloned token with the principal and roles set.
         */
        protected Token authorize(final Principal principal, final Set<String> roles) {
            return new Token(token, secret, consumerKey, callbackUrl, principal,
                    roles == null ? Collections.<String>emptySet() : new HashSet<>(roles), attribs);
        }
    }
}
