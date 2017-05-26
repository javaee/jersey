/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015-2017 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.jdk.connector.internal;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generates a value of {@code Authorization} header of HTTP request for Digest Http Authentication scheme (RFC 2617).
 *
 * @author raphael.jolivet@gmail.com
 * @author Stefan Katerkamp (stefan@katerkamp.de)
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 * @author Ondrej Kosatka (ondrej.kosatka at oracle.com)
 */
class ProxyDigestAuthenticator {

    /**
     * Encoding used for authentication calculations.
     */
    private static final Charset CHARACTER_SET = Charset.forName("iso-8859-1");

    private static final Logger logger = Logger.getLogger(ProxyDigestAuthenticator.class.getName());

    private static final char[] HEX_ARRAY =
            {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    private static final Pattern KEY_VALUE_PAIR_PATTERN =
            Pattern.compile("(\\w+)\\s*=\\s*(\"([^\"]+)\"|(\\w+))\\s*,?\\s*");
    private static final int CLIENT_NONCE_BYTE_COUNT = 4;

    private SecureRandom randomGenerator;

    ProxyDigestAuthenticator() {
        try {
            randomGenerator = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            logger.config("No such algorithm to generate authorization digest http header." + e);
        }
    }

    String generateAuthorizationHeader(URI uri, String method, String authenticateHeader, String userName, String password)
            throws ProxyAuthenticationException {
        if (userName == null) {
            throw new ProxyAuthenticationException(LocalizationMessages.PROXY_USER_NAME_MISSING());
        }

        if (password == null) {
            throw new ProxyAuthenticationException(LocalizationMessages.PROXY_PASSWORD_MISSING());
        }
        DigestScheme digestScheme;
        try {
            digestScheme = parseAuthHeaders(authenticateHeader);
        } catch (IOException e) {
            throw new ProxyAuthenticationException(e.getMessage());
        }
        if (digestScheme == null) {
            throw new ProxyAuthenticationException(LocalizationMessages.PROXY_FAIL_AUTH_HEADER());
        }

        return createNextAuthToken(digestScheme, uri.toString(), method, userName, password);
    }

    /**
     * Parse digest header.
     *
     * @param authHeader value of {@code WWW-Authenticate} header
     * @return DigestScheme or {@code null} if no digest header exists.
     */
    private DigestScheme parseAuthHeaders(final String authHeader) throws IOException {

        if (authHeader == null) {
            return null;
        }

        String[] parts = authHeader.trim().split("\\s+", 2);

        if (parts.length != 2) {
            return null;
        }
        if (!parts[0].toLowerCase().equals("digest")) {
            return null;
        }

        String realm = null;
        String nonce = null;
        String opaque = null;
        QOP qop = QOP.UNSPECIFIED;
        Algorithm algorithm = Algorithm.UNSPECIFIED;
        boolean stale = false;

        Matcher match = KEY_VALUE_PAIR_PATTERN.matcher(parts[1]);
        while (match.find()) {
            // expect 4 groups (key)=("(val)" | (val))
            int nbGroups = match.groupCount();
            if (nbGroups != 4) {
                continue;
            }
            String key = match.group(1);
            String valNoQuotes = match.group(3);
            String valQuotes = match.group(4);
            String val = (valNoQuotes == null) ? valQuotes : valNoQuotes;
            if (key.equals("qop")) {
                qop = QOP.parse(val);
            } else if (key.equals("realm")) {
                realm = val;
            } else if (key.equals("nonce")) {
                nonce = val;
            } else if (key.equals("opaque")) {
                opaque = val;
            } else if (key.equals("stale")) {
                stale = Boolean.parseBoolean(val);
            } else if (key.equals("algorithm")) {
                algorithm = Algorithm.parse(val);
            }
        }
        return new DigestScheme(realm, nonce, opaque, qop, algorithm, stale);
    }

    /**
     * Creates digest string including counter.
     *
     * @param ds  DigestScheme instance
     * @param uri client request uri
     * @return digest authentication token string
     * @throws ProxyAuthenticationException if MD5 hash fails
     */
    private String createNextAuthToken(final DigestScheme ds, String uri, String method, String userName, String password) throws
            ProxyAuthenticationException {
        StringBuilder sb = new StringBuilder(100);
        sb.append("Digest ");
        append(sb, "username", userName);
        append(sb, "realm", ds.getRealm());
        append(sb, "nonce", ds.getNonce());
        append(sb, "opaque", ds.getOpaque());
        append(sb, "algorithm", ds.getAlgorithm().toString(), false);
        append(sb, "qop", ds.getQop().toString(), false);

        append(sb, "uri", uri);

        String ha1;
        if (ds.getAlgorithm().equals(Algorithm.MD5_SESS)) {
            ha1 = md5(md5(userName, ds.getRealm(), password));
        } else {
            ha1 = md5(userName, ds.getRealm(), password);
        }

        String ha2 = md5(method, uri);

        String response;
        if (ds.getQop().equals(QOP.UNSPECIFIED)) {
            response = md5(ha1, ds.getNonce(), ha2);
        } else {
            String cnonce = randomBytes(CLIENT_NONCE_BYTE_COUNT); // client nonce
            append(sb, "cnonce", cnonce);
            String nc = String.format("%08x", ds.incrementCounter()); // counter
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
    private static void append(StringBuilder sb, String key, String value, boolean useQuote) {

        if (value == null) {
            return;
        }
        if (sb.length() > 0) {
            if (sb.charAt(sb.length() - 1) != ' ') {
                sb.append(", ");
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
     * Append comma separated key=value token. The value gets enclosed in quotes.
     *
     * @param sb    string builder instance
     * @param key   key string
     * @param value value string
     */
    private static void append(StringBuilder sb, String key, String value) {
        append(sb, key, value, true);
    }

    /**
     * Convert bytes array to hex string.
     *
     * @param bytes array of bytes
     * @return hex string
     */
    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
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
     * @throws ProxyAuthenticationException if MD5 algorithm cannot be instantiated
     */
    private static String md5(String... tokens) throws ProxyAuthenticationException {
        StringBuilder sb = new StringBuilder(100);
        for (String token : tokens) {
            if (sb.length() > 0) {
                sb.append(':');
            }
            sb.append(token);
        }

        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
            throw new ProxyAuthenticationException(ex.getMessage());
        }
        md.update(sb.toString().getBytes(CHARACTER_SET), 0, sb.length());
        byte[] md5hash = md.digest();
        return bytesToHex(md5hash);
    }

    /**
     * Generate a random sequence of bytes and return its hex representation
     *
     * @param nbBytes number of bytes to generate
     * @return hex string
     */
    private String randomBytes(int nbBytes) {
        byte[] bytes = new byte[nbBytes];
        randomGenerator.nextBytes(bytes);
        return bytesToHex(bytes);
    }

    private enum QOP {

        UNSPECIFIED(null),
        AUTH("auth");

        private final String qop;

        QOP(String qop) {
            this.qop = qop;
        }

        @Override
        public String toString() {
            return qop;
        }

        public static QOP parse(String val) {
            if (val == null || val.isEmpty()) {
                return QOP.UNSPECIFIED;
            }
            if (val.contains("auth")) {
                return QOP.AUTH;
            }
            throw new UnsupportedOperationException(LocalizationMessages.PROXY_QOP_NO_SUPPORTED(val));
        }
    }

    enum Algorithm {

        UNSPECIFIED(null),
        MD5("MD5"),
        MD5_SESS("MD5-sess");
        private final String md;

        Algorithm(String md) {
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

        DigestScheme(String realm,
                     String nonce,
                     String opaque,
                     QOP qop,
                     Algorithm algorithm,
                     boolean stale) {
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
