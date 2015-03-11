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

package org.glassfish.jersey.oauth1.signature;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.glassfish.jersey.uri.UriComponent;

/**
 * A data structure class that represents OAuth protocol parameters.
 *
 * @author Hubert A. Le Van Gong <hubert.levangong at Sun.COM>
 * @author Paul C. Bryan <pbryan@sun.com>
 */
public class OAuth1Parameters extends HashMap<String, String> {

    /** Name of HTTP authorization header. */
    public static final String AUTHORIZATION_HEADER = "Authorization";

    /** OAuth scheme in Authorization header. */
    public static final String SCHEME = "OAuth";

    /** Name of parameter containing the protection realm. */
    public static final String REALM = "realm";

    /** Name of parameter containing the consumer key. */
    public static final String CONSUMER_KEY = "oauth_consumer_key";

    /** Name of parameter containing the access/request token. */
    public static final String TOKEN = "oauth_token";

    /** Name of parameter containing the signature method. */
    public static final String SIGNATURE_METHOD = "oauth_signature_method";

    /** Name of parameter containing the signature. */
    public static final String SIGNATURE = "oauth_signature";

    /** Name of parameter containing the timestamp. */
    public static final String TIMESTAMP = "oauth_timestamp";

    /** Name of parameter containing the nonce. */
    public static final String NONCE = "oauth_nonce";

    /** Name of parameter containing the protocol version. */
    public static final String VERSION = "oauth_version";

    /** Name of parameter containing the verifier code. */
    public static final String VERIFIER = "oauth_verifier";

    /** Name of parameter containing the callback URL. */
    public static final String CALLBACK = "oauth_callback";

    /** Name of parameter containing the token secret.
     * This parameter is never used in requests. It is part of a response to the request token and access token requests.
     */
    public static final String TOKEN_SECRET = "oauth_token_secret";

    /** Name of parameter containing the token secret.
     * This parameter is never used in requests. It is part of a response to the request token requests.
     */
    public static final String CALLBACK_CONFIRMED = "oauth_callback_confirmed";

    /**
     * Default value of the callback URI that should be used during Authorization flow
     * for Request Token request when the client is not capable of handling redirects (e.g. the client is
     * a mobile application).
     */
    public static final String NO_CALLBACK_URI_VALUE = "oob";

    /* Authorization scheme and delimiter. */
    private static final String SCHEME_SPACE = SCHEME + ' ';

    /**
     * Returns the protection realm for the request.
     */
    public String getRealm() {
        return get(REALM);
    }

    /**
     * Sets the protection realm for the request.
     */
    public void setRealm(String realm) {
        put(REALM, realm);
    }

    /**
     * Builder pattern method to return {@code OAuth1Parameters} after setting
     * protection realm.
     *
     * @param realm the protection realm for the request.
     * @return this parameters object.
     */
    public OAuth1Parameters realm(String realm) {
        setRealm(realm);
        return this;
    }

    /**
     * Returns the consumer key.
     */
    public String getConsumerKey() {
        return get(CONSUMER_KEY);
    }

    /**
     * Sets the consumer key.
     */
    public void setConsumerKey(String consumerKey) {
        put(CONSUMER_KEY, consumerKey);
    }

    /**
     * Builder pattern method to return {@code OAuth1Parameters} after setting
     * consumer key.
     *
     * @param consumerKey the consumer key.
     */
    public OAuth1Parameters consumerKey(String consumerKey) {
        setConsumerKey(consumerKey);
        return this;
    }

    /**
     * Returns the request or access token.
     */
    public String getToken() {
        return get(TOKEN);
    }

    @Override
    public String put(String key, String value) {
        return value == null ? remove(key) : super.put(key, value);
    }

    /**
     * Sets the request or access token.
     */
    public void setToken(String token) {
        if (token == null) {
            remove(TOKEN);
        } else {
            put(TOKEN, token);
        }
    }

    /**
     * Builder pattern method to return {@code OAuth1Parameters} after setting
     * token.
     *
     * @param token the access or request token.
     * @return this parameters object.
     */
    public OAuth1Parameters token(String token) {
        setToken(token);
        return this;
    }

    /**
     * Returns the signature method used to sign the request.
     */
    public String getSignatureMethod() {
        return get(SIGNATURE_METHOD);
    }

    /**
     * Sets the signature method used to sign the request.
     */
    public void setSignatureMethod(String signatureMethod) {
        put(SIGNATURE_METHOD, signatureMethod);
    }

    /**
     * Builder pattern method to return {@code OAuth1Parameters} after setting
     * signature method.
     *
     * @param signatureMethod the signature method used to sign the request.
     * @return this parameters object.
     */
    public OAuth1Parameters signatureMethod(String signatureMethod) {
        setSignatureMethod(signatureMethod);
        return this;
    }

    /**
     * Returns the signature for the request.
     */
    public String getSignature() {
        return get(SIGNATURE);
    }

    /**
     * Sets the signature for the request.
     */
    public void setSignature(String signature) {
        put(SIGNATURE, signature);
    }

    /**
     * Builder pattern method to return {@code OAuth1Parameters} after setting
     * signature.
     *
     * @param signature the signature for the request.
     * @return this parameters object.
     */
    public OAuth1Parameters signature(String signature) {
        setSignature(signature);
        return this;
    }

    /**
     * Returns the timestamp, a value expected to be a positive integer,
     * typically containing the number of seconds since January 1, 1970
     * 00:00:00 GMT (epoch).
     */
    public String getTimestamp() {
        return get(TIMESTAMP);
    }

    /**
     * Sets the timestamp. Its value is not validated, but should be a
     * positive integer, typically containing the number of seconds since
     * January 1, 1970 00:00:00 GMT (epoch).
     */
    public void setTimestamp(String timestamp) {
        put(TIMESTAMP, timestamp);
    }

    /**
     * Builder pattern method to return {@code OAuth1Parameters} after setting
     * timestamp.
     *
     * @param timestamp positive integer, typically number of seconds since epoch.
     * @return this parameters object.
     */
    public OAuth1Parameters timestamp(String timestamp) {
        setTimestamp(timestamp);
        return this;
    }

    /**
     * Sets the timestamp to the current time as number of seconds since epoch.
     */
    public void setTimestamp() {
        setTimestamp(Long.toString(System.currentTimeMillis() / 1000));
    }

    /**
     * Builder pattern method to return {@code OAuth1Parameters} after setting
     * timestamp to the current time.
     *
     * @return this parameters object.
     */
    public OAuth1Parameters timestamp() {
        setTimestamp();
        return this;
    }

    /**
     * Returns the nonce, a value that should be unique for a given
     * timestamp.
     */
    public String getNonce() {
        return get(NONCE);
    }

    /**
     * Sets the nonce, a value that should be unique for a given timestamp.
     */
    public void setNonce(String nonce) {
        put(NONCE, nonce);
    }

    /**
     * Builder pattern method to return {@code OAuth1Parameters} after setting
     * nonce.
     *
     * @param nonce a value that should be unique for a given timestamp.
     * @return this parameters object.
     */
    public OAuth1Parameters nonce(String nonce) {
        setNonce(nonce);
        return this;
    }

    /**
     * Sets the nonce to contain a randomly-generated UUID.
     */
    public void setNonce() {
        setNonce(UUID.randomUUID().toString());
    }

    /**
     * Builder pattern method to return {@code OAuth1Parameters} after setting
     * nonce to a randomly-generated UUID.
     *
     * @return this parameters object.
     */
    public OAuth1Parameters nonce() {
        setNonce();
        return this;
    }

    /**
     * Returns the protocol version.
     */
    public String getVersion() {
        return get(VERSION);
    }

    /**
     * Sets the protocol version.
     */
    public void setVersion(String version) {
        put(VERSION, version);
    }

    /**
     * Builder pattern method to return {@code OAuth1Parameters} after setting
     * version.
     *
     * @param version the protocol version.
     * @return this parameters object.
     */
    public OAuth1Parameters version(String version) {
        setVersion(version);
        return this;
    }

    /**
     * Sets the protocol version to the default value of 1.0.
     */
    public void setVersion() {
        setVersion("1.0");
    }

    /**
     * Builder pattern method to return {@code OAuth1Parameters} after setting
     * version to the default value of 1.0.
     *
     * @return this parameters object.
     */
    public OAuth1Parameters version() {
        setVersion();
        return this;
    }

    /**
     * Returns the verifier code.
     */
    public String getVerifier() {
        return get(VERIFIER);
    }

    /**
     * Sets the verifier code.
     */
    public void setVerifier(String verifier) {
        put(VERIFIER, verifier);
    }

    /**
     * Builder pattern method to return {@code OAuth1Parameters} after setting
     * verifier code.
     *
     * @param verifier the verifier code.
     * @return this parameters object.
     */
    public OAuth1Parameters verifier(String verifier) {
        setVerifier(verifier);
        return this;
    }

    /**
     * Returns the callback URL.
     */
    public String getCallback() {
        return get(CALLBACK);
    }

    /**
     * Sets the callback URL.
     */
    public void setCallback(String callback) {
        put(CALLBACK, callback);
    }

    /**
     * Builder pattern method to return {@code OAuth1Parameters} after setting
     * callback URL.
     *
     * @param callback the callback URL.
     * @return this parameters object.
     */
    public OAuth1Parameters callback(String callback) {
        setCallback(callback);
        return this;
    }

    /**
     * Removes (optional) quotation marks encapsulating parameter values in the
     * Authorization header and returns the result.
     */
    private static String dequote(String value) {
        int length = value.length();
        return ((length >= 2 && value.charAt(0) == '"' && value.charAt(length - 1) == '"')
                ? value.substring(1, length - 1) : value);
    }

    /**
     * Reads a request for OAuth parameters, and populates this object.
     *
     * @param request the request to read OAuth parameters from.
     * @return this parameters object.
     */
    public OAuth1Parameters readRequest(OAuth1Request request) {

        // read supported parameters from query string or request body (lowest preference)
        for (String param : request.getParameterNames()) {
            if (!param.startsWith("oauth_")) {
                continue;
            }
            List<String> values = request.getParameterValues(param);
            if (values == null) {
                continue;
            }
            Iterator<String> i = values.iterator();
            if (!i.hasNext()) {
                continue;
            }
            put(param, i.next());
        }

        // read all parameters from authorization header (highest preference)
        List<String> headers = request.getHeaderValues(AUTHORIZATION_HEADER);
        if (headers == null) {
            return this;
        }

        for (String header : headers) {
            if (!header.regionMatches(true, 0, SCHEME_SPACE, 0, SCHEME_SPACE.length())) {
                continue;
            }
            for (String param : header.substring(SCHEME_SPACE.length()).trim().split(",(?=(?:[^\"]*\"[^\"]*\")+$)")) {
                String[] nv = param.split("=", 2);
                if (nv.length != 2) {
                    continue;
                }
                put(UriComponent.decode(nv[0].trim(), UriComponent.Type.UNRESERVED),
                        UriComponent.decode(dequote(nv[1].trim()), UriComponent.Type.UNRESERVED));
            }
        }

        return this;
    }

    /**
     * Writes the OAuth parameters to a request, as an Authorization header.
     *
     * @param request the request to write OAuth parameters to.
     * @return this parameters object.
     */
    public OAuth1Parameters writeRequest(OAuth1Request request) {
        StringBuilder buf = new StringBuilder(SCHEME);
        boolean comma = false;
        for (String key : keySet()) {
            String value = get(key);
            if (value == null) {
                continue;
            }
            buf.append(comma ? ", " : " ").append(UriComponent.encode(key, UriComponent.Type.UNRESERVED));
            buf.append("=\"").append(UriComponent.encode(value, UriComponent.Type.UNRESERVED)).append('"');
            comma = true;
        }
        request.addHeaderValue(AUTHORIZATION_HEADER, buf.toString());
        return this;
    }

    @Override
    public OAuth1Parameters clone() {
        return (OAuth1Parameters) super.clone();
    }
}

