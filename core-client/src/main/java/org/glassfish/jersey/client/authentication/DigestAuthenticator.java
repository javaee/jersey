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

package org.glassfish.jersey.client.authentication;

import java.io.IOException;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.internal.LocalizationMessages;
import org.glassfish.jersey.message.MessageUtils;
import org.glassfish.jersey.uri.UriComponent;

/**
 * Implementation of Digest Http Authentication method (RFC 2617).
 *
 * @author raphael.jolivet@gmail.com
 * @author Stefan Katerkamp (stefan@katerkamp.de)
 * @author Miroslav Fuksa
 */
final class DigestAuthenticator {

    private static final char[] HEX_ARRAY = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    private static final Pattern KEY_VALUE_PAIR_PATTERN = Pattern.compile("(\\w+)\\s*=\\s*(\"([^\"]+)\"|(\\w+))\\s*,?\\s*");
    private static final int CLIENT_NONCE_BYTE_COUNT = 4;

    private final SecureRandom randomGenerator;
    private final HttpAuthenticationFilter.Credentials credentials;

    private final Map<URI, DigestScheme> digestCache;

    /**
     * Create a new instance initialized from credentials and configuration.
     *
     * @param credentials Credentials. Can be {@code null} if there are no default credentials.
     * @param limit       Maximum number of URIs that should be kept in the cache containing URIs and their
     *                    {@link org.glassfish.jersey.client.authentication.DigestAuthenticator.DigestScheme}.
     */
    DigestAuthenticator(final HttpAuthenticationFilter.Credentials credentials, final int limit) {
        this.credentials = credentials;

        digestCache = Collections.synchronizedMap(new LinkedHashMap<URI, DigestScheme>(limit) {
            // use id as it is an anonymous inner class with changed behaviour
            private static final long serialVersionUID = 2546245625L;

            @Override
            protected boolean removeEldestEntry(final Map.Entry eldest) {
                return size() > limit;
            }
        });

        try {
            randomGenerator = SecureRandom.getInstance("SHA1PRNG");
        } catch (final NoSuchAlgorithmException e) {
            throw new RequestAuthenticationException(LocalizationMessages.ERROR_DIGEST_FILTER_GENERATOR(), e);
        }
    }

    /**
     * Process request and add authentication information if possible.
     *
     * @param request Request context.
     * @return {@code true} if authentication information was added.
     * @throws IOException When error with encryption occurs.
     */
    boolean filterRequest(final ClientRequestContext request) throws IOException {
        final DigestScheme digestScheme = digestCache.get(request.getUri());
        if (digestScheme != null) {
            final HttpAuthenticationFilter.Credentials cred = HttpAuthenticationFilter.getCredentials(request,
                    this.credentials, HttpAuthenticationFilter.Type.DIGEST);
            if (cred != null) {
                request.getHeaders().add(HttpHeaders.AUTHORIZATION, createNextAuthToken(digestScheme, request, cred));
                return true;
            }
        }
        return false;
    }

    /**
     * Process response and repeat the request if digest authentication is requested. When request is repeated
     * the response will be modified to contain new response information.
     *
     * @param request  Request context.
     * @param response Response context (will be updated with newest response data if the request was repeated).
     * @return {@code true} if response does not require authentication or if authentication is required,
     * new request was done with digest authentication information and authentication was successful.
     * @throws IOException When error with encryption occurs.
     */
    public boolean filterResponse(final ClientRequestContext request, final ClientResponseContext response) throws IOException {

        if (Response.Status.fromStatusCode(response.getStatus()) == Response.Status.UNAUTHORIZED) {

            final DigestScheme digestScheme = parseAuthHeaders(response.getHeaders().get(HttpHeaders.WWW_AUTHENTICATE));
            if (digestScheme == null) {
                return false;
            }

            // assemble authentication request and resend it
            final HttpAuthenticationFilter.Credentials cred = HttpAuthenticationFilter.getCredentials(request,
                    this.credentials, HttpAuthenticationFilter.Type.DIGEST);
            if (cred == null) {

                throw new ResponseAuthenticationException(null, LocalizationMessages.AUTHENTICATION_CREDENTIALS_MISSING_DIGEST());
            }

            final boolean success = HttpAuthenticationFilter.repeatRequest(request, response, createNextAuthToken(digestScheme,
                    request, cred));
            if (success) {
                digestCache.put(request.getUri(), digestScheme);
            } else {
                digestCache.remove(request.getUri());
            }
            return success;
        }
        return true;
    }

    /**
     * Parse digest header.
     *
     * @param headers List of header strings
     * @return DigestScheme or {@code null} if no digest header exists.
     */
    private DigestScheme parseAuthHeaders(final List<?> headers) throws IOException {

        if (headers == null) {
            return null;
        }
        for (final Object lineObject : headers) {

            if (!(lineObject instanceof String)) {
                continue;
            }
            final String line = (String) lineObject;
            final String[] parts = line.trim().split("\\s+", 2);

            if (parts.length != 2) {
                continue;
            }
            if (!"digest".equals(parts[0].toLowerCase())) {
                continue;
            }

            String realm = null;
            String nonce = null;
            String opaque = null;
            QOP qop = QOP.UNSPECIFIED;
            Algorithm algorithm = Algorithm.UNSPECIFIED;
            boolean stale = false;

            final Matcher match = KEY_VALUE_PAIR_PATTERN.matcher(parts[1]);
            while (match.find()) {
                // expect 4 groups (key)=("(val)" | (val))
                final int nbGroups = match.groupCount();
                if (nbGroups != 4) {
                    continue;
                }
                final String key = match.group(1);
                final String valNoQuotes = match.group(3);
                final String valQuotes = match.group(4);
                final String val = (valNoQuotes == null) ? valQuotes : valNoQuotes;
                if ("qop".equals(key)) {
                    qop = QOP.parse(val);
                } else if ("realm".equals(key)) {
                    realm = val;
                } else if ("nonce".equals(key)) {
                    nonce = val;
                } else if ("opaque".equals(key)) {
                    opaque = val;
                } else if ("stale".equals(key)) {
                    stale = Boolean.parseBoolean(val);
                } else if ("algorithm".equals(key)) {
                    algorithm = Algorithm.parse(val);
                }
            }
            return new DigestScheme(realm, nonce, opaque, qop, algorithm, stale);
        }
        return null;
    }

    /**
     * Creates digest string including counter.
     *
     * @param ds             DigestScheme instance
     * @param requestContext client request context
     * @return digest authentication token string
     * @throws IOException
     */
    private String createNextAuthToken(final DigestScheme ds, final ClientRequestContext requestContext,
                                       final HttpAuthenticationFilter.Credentials credentials) throws IOException {
        final StringBuilder sb = new StringBuilder(100);
        sb.append("Digest ");
        append(sb, "username", credentials.getUsername());
        append(sb, "realm", ds.getRealm());
        append(sb, "nonce", ds.getNonce());
        append(sb, "opaque", ds.getOpaque());
        append(sb, "algorithm", ds.getAlgorithm().toString(), false);
        append(sb, "qop", ds.getQop().toString(), false);

        final String uri = UriComponent.fullRelativeUri(requestContext.getUri());
        append(sb, "uri", uri);

        final String ha1;
        if (ds.getAlgorithm() == Algorithm.MD5_SESS) {
            ha1 = md5(md5(credentials.getUsername(), ds.getRealm(),
                    new String(credentials.getPassword(), MessageUtils.getCharset(requestContext.getMediaType()))));
        } else {
            ha1 = md5(credentials.getUsername(), ds.getRealm(),
                    new String(credentials.getPassword(), MessageUtils.getCharset(requestContext.getMediaType())));
        }

        final String ha2 = md5(requestContext.getMethod(), uri);

        final String response;
        if (ds.getQop() == QOP.UNSPECIFIED) {
            response = md5(ha1, ds.getNonce(), ha2);
        } else {
            final String cnonce = randomBytes(CLIENT_NONCE_BYTE_COUNT); // client nonce
            append(sb, "cnonce", cnonce);
            final String nc = String.format("%08x", ds.incrementCounter()); // counter
            append(sb, "nc", nc, false);
            response = md5(ha1, ds.getNonce(), nc, cnonce, ds.getQop().toString(), ha2);
        }
        append(sb, "response", response);

        return sb.toString();
    }

    /**
     * Append comma separated key=value token
     *
     * @param sb       string builder instance
     * @param key      key string
     * @param value    value string
     * @param useQuote true if value needs to be enclosed in quotes
     */
    private static void append(final StringBuilder sb, final String key, final String value, final boolean useQuote) {

        if (value == null) {
            return;
        }
        if (sb.length() > 0) {
            if (sb.charAt(sb.length() - 1) != ' ') {
                sb.append(',');
            }
        }
        sb.append(key);
        sb.append('=');
        if (useQuote) {
            sb.append('"');
        }
        sb.append(value);
        if (useQuote) {
            sb.append('"');
        }
    }

    /**
     * Append comma separated key=value token. The value gets enclosed in
     * quotes.
     *
     * @param sb    string builder instance
     * @param key   key string
     * @param value value string
     */
    private static void append(final StringBuilder sb, final String key, final String value) {
        append(sb, key, value, true);
    }

    /**
     * Convert bytes array to hex string.
     *
     * @param bytes array of bytes
     * @return hex string
     */
    private static String bytesToHex(final byte[] bytes) {
        final char[] hexChars = new char[bytes.length * 2];
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    /**
     * Colon separated value MD5 hash.
     *
     * @param tokens one or more strings
     * @return M5 hash string
     * @throws IOException
     */
    private static String md5(final String... tokens) throws IOException {
        final StringBuilder sb = new StringBuilder(100);
        for (final String token : tokens) {
            if (sb.length() > 0) {
                sb.append(':');
            }
            sb.append(token);
        }

        final MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (final NoSuchAlgorithmException ex) {
            throw new IOException(ex.getMessage());
        }
        md.update(sb.toString().getBytes(HttpAuthenticationFilter.CHARACTER_SET), 0, sb.length());
        final byte[] md5hash = md.digest();
        return bytesToHex(md5hash);
    }

    /**
     * Generate a random sequence of bytes and return its hex representation
     *
     * @param nbBytes number of bytes to generate
     * @return hex string
     */
    private String randomBytes(final int nbBytes) {
        final byte[] bytes = new byte[nbBytes];
        randomGenerator.nextBytes(bytes);
        return bytesToHex(bytes);
    }

    private enum QOP {

        UNSPECIFIED(null),
        AUTH("auth");

        private final String qop;

        QOP(final String qop) {
            this.qop = qop;
        }

        @Override
        public String toString() {
            return qop;
        }

        public static QOP parse(final String val) {
            if (val == null || val.isEmpty()) {
                return QOP.UNSPECIFIED;
            }
            if (val.contains("auth")) {
                return QOP.AUTH;
            }
            throw new UnsupportedOperationException(LocalizationMessages.DIGEST_FILTER_QOP_UNSUPPORTED(val));
        }
    }

    enum Algorithm {

        UNSPECIFIED(null),
        MD5("MD5"),
        MD5_SESS("MD5-sess");
        private final String md;

        Algorithm(final String md) {
            this.md = md;
        }

        @Override
        public String toString() {
            return md;
        }

        public static Algorithm parse(String val) {
            if (val == null || val.isEmpty()) {
                return Algorithm.UNSPECIFIED;
            }
            val = val.trim();
            if (val.contains(MD5_SESS.md) || val.contains(MD5_SESS.md.toLowerCase())) {
                return MD5_SESS;
            }
            return MD5;
        }
    }

    /**
     * Digest scheme POJO
     */
    final class DigestScheme {

        private final String realm;
        private final String nonce;
        private final String opaque;
        private final Algorithm algorithm;
        private final QOP qop;
        private final boolean stale;
        private volatile int nc;

        DigestScheme(final String realm, final String nonce, final String opaque, final QOP qop, final Algorithm algorithm,
                     final boolean stale) {
            this.realm = realm;
            this.nonce = nonce;
            this.opaque = opaque;
            this.qop = qop;
            this.algorithm = algorithm;
            this.stale = stale;
            this.nc = 0;
        }

        public int incrementCounter() {
            return ++nc;
        }

        public String getNonce() {
            return nonce;
        }

        public String getRealm() {
            return realm;
        }

        public String getOpaque() {
            return opaque;
        }

        public Algorithm getAlgorithm() {
            return algorithm;
        }

        public QOP getQop() {
            return qop;
        }

        public boolean isStale() {
            return stale;
        }

        public int getNc() {
            return nc;
        }
    }
}
