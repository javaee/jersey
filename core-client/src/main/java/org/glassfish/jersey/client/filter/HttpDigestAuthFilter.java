/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.client.filter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.internal.util.PropertiesHelper;

/**
 * Client filter providing HTTP Digest Authentication with preemptive
 * authentication support.
 *
 * @author raphael.jolivet@gmail.com
 * @author Stefan Katerkamp (stefan@katerkamp.de)
 */
@Provider
public class HttpDigestAuthFilter implements ClientRequestFilter, ClientResponseFilter {

	@Inject
	private Configuration config;
	private static final Logger logger = Logger.getLogger(HttpDigestAuthFilter.class.getName());
	private static final Charset CHARACTER_SET = Charset.forName("iso-8859-1");
	private static final char[] hexArray = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
	private static final Pattern KEY_VALUE_PAIR_PATTERN = Pattern.compile("(\\w+)\\s*=\\s*(\"([^\"]+)\"|(\\w+))\\s*,?\\s*");
	private static final SecureRandom randomGenerator;

	static {
		try {
			randomGenerator = SecureRandom.getInstance("SHA1PRNG");
		} catch (Exception e) {
			throw new Error(e);
		}
	}
	private static final int CLIENT_NONCE_BYTE_COUNT = 4;
	private String username;
	private byte[] password;
	private static final int MAXIMUM_DIGEST_CACHE_SIZE = 1000;
	private final Map<URI, DigestScheme> digestCache;

	/**
	 * Creates a new HTTP Basic Authentication filter using provided username
	 * and password credentials. MaxCacheSize must be greater or equal than 1
	 * and it must be greater than the maximum number of parallel requests using
	 * this filter.
	 *
	 * @param username user name
	 * @param password password
	 */
	public HttpDigestAuthFilter(String username, String password) {
		this(username, (password != null) ? password.getBytes(CHARACTER_SET) : new byte[0]);
	}

	/**
	 * Creates a new HTTP Basic Authentication filter using provided username
	 * and password credentials. This constructor allows to avoid storing plain
	 * password value in a String variable. MaxCacheSize must be greater or
	 * equal than 1 and it must be greater than the maximum number of parallel
	 * requests using this filter.
	 *
	 * @param username user name
	 * @param password password byte array
	 */
	public HttpDigestAuthFilter(String username, byte[] password) {
		this();

		if (username == null) {
			username = "";
		}
		if (password == null) {
			password = new byte[0];
		}
		this.username = username;
		this.password = password;
	}

	private HttpDigestAuthFilter() {
		int limit = MAXIMUM_DIGEST_CACHE_SIZE;
		if (config != null) {
			limit = PropertiesHelper.getValue(config.getProperties(),
					ClientProperties.DIGESTAUTH_URI_CACHE_SIZELIMIT, MAXIMUM_DIGEST_CACHE_SIZE);
			if (limit < 1) {
				limit = 1;
			}
		}

		final int mapSize = limit;
		digestCache = Collections.synchronizedMap(
				new LinkedHashMap<URI, DigestScheme>(mapSize) {
			// use id as it is an anonymous inner class with changed behaviour
			private static final long serialVersionUID = 2546245625L;

			@Override
			protected boolean removeEldestEntry(Map.Entry eldest) {
				return size() > mapSize;
			}
		});
	}

	@Override
	public void filter(ClientRequestContext requestContext) throws IOException {
		DigestScheme digestScheme = digestCache.get(requestContext.getUri());
		if (digestScheme != null && digestScheme.getNonce() != null) {
			String authLine = createNextAuthToken(digestScheme, requestContext); // increments nc
			requestContext.getHeaders().add(HttpHeaders.AUTHORIZATION, authLine);
		}

		if (logger.isLoggable(Level.FINEST)) {
			if (requestContext.getHeaderString(HttpHeaders.AUTHORIZATION) != null) {
				logger.log(Level.FINEST, "Client Request: {0}", requestContext.getHeaderString(HttpHeaders.AUTHORIZATION));
			}
		}
	}

	@Override
	public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {

		if (logger.isLoggable(Level.FINEST)) {
			if (responseContext.getHeaderString(HttpHeaders.WWW_AUTHENTICATE) != null) {
				logger.log(Level.FINEST, "Server Response: {0} {1}", new Object[]{responseContext.getStatus(),
					responseContext.getHeaderString(HttpHeaders.WWW_AUTHENTICATE)});
			}
		}

		if (Response.Status.fromStatusCode(responseContext.getStatus()) == Status.UNAUTHORIZED) {

			DigestScheme digestScheme = parseAuthHeaders(responseContext.getHeaders().get(HttpHeaders.WWW_AUTHENTICATE));
			if (digestScheme == null) {
				return;
			}

			if (digestScheme.isStale() || !digestCache.containsKey(requestContext.getUri())) {

				digestCache.put(requestContext.getUri(), digestScheme);

				// assemble authentication request and resend it
				Client client = requestContext.getClient();
				String method = requestContext.getMethod();
				MediaType mediaType = requestContext.getMediaType();
				URI luri = requestContext.getUri();

				WebTarget resourceTarget = client.target(luri);

				Invocation.Builder builder = resourceTarget.request(mediaType);
				builder.headers(requestContext.getHeaders());
				Invocation invocation = builder.build(method);

				Response nextResponse = invocation.invoke();

				if (nextResponse == null) {
					return;
				}
				if (nextResponse.hasEntity()) {
					String entity = nextResponse.readEntity(String.class);
					responseContext.setEntityStream(new ByteArrayInputStream(entity.getBytes(CHARACTER_SET)));
				}
				MultivaluedMap<String, String> headers = responseContext.getHeaders();
				headers.clear();
				headers.putAll(nextResponse.getStringHeaders());
				responseContext.setStatus(nextResponse.getStatus());
			}
		}
	}

	/**
	 * Parse digest header.
	 *
	 * @param headers List of header strings
	 * @return DigestScheme or null if no digest header exists
	 */
	private DigestScheme parseAuthHeaders(List<?> headers) throws IOException {

		if (headers == null) {
			return null;
		}
		for (Object lineObject : headers) {

			if (!(lineObject instanceof String)) {
				continue;
			}
			String line = (String) lineObject;
			String[] parts = line.trim().split("\\s+", 2);

			if (parts.length != 2) {
				continue;
			}
			if (!parts[0].toLowerCase().equals("digest")) {
				continue;
			}

			String realm = null;
			String nonce = null;
			String opaque = null;
			QOP qop = QOP.UNSPECIFIED;
			ALGORITHM algorithm = ALGORITHM.UNSPECIFIED;
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
					algorithm = ALGORITHM.parse(val);
				}
			}
			return new DigestScheme(realm, nonce, opaque, qop, algorithm, stale);
		}
		return null;
	}

	/**
	 * Creates digest string including counter.
	 *
	 * @param ds DigestScheme instance
	 * @param requestContext client request context
	 * @return digest authentication token string
	 * @throws IOException
	 */
	private String createNextAuthToken(DigestScheme ds, ClientRequestContext requestContext) throws IOException {

		StringBuilder sb = new StringBuilder(100);
		sb.append("Digest ");
		append(sb, "username", username);
		append(sb, "realm", ds.getRealm());
		append(sb, "nonce", ds.getNonce());
		append(sb, "opaque", ds.getOpaque());
		append(sb, "algorithm", ds.getAlgorithm().toString(), false);
		append(sb, "qop", ds.getQop().toString(), false);

		String uri = requestContext.getUri().getRawPath();
		append(sb, "uri", uri);

		String ha1;
		if (ds.getAlgorithm().equals(ALGORITHM.MD5_SESS)) {
			ha1 = md5(md5(username, ds.getRealm(), new String(password)));
		} else {
			ha1 = md5(username, ds.getRealm(), new String(password));
		}

		String ha2;
		if (ds.getQop() == QOP.AUTH_INT && requestContext.hasEntity()) {
			Object entity = requestContext.getEntity();
			if (entity instanceof String) {
				ha2 = md5(
						requestContext.getMethod(),
						uri,
						md5((String) entity));
			} else {
				throw new IOException("Entity of class " + entity.getClass().toString() + " not supported");
			}
		} else {
			ha2 = md5(requestContext.getMethod(), uri);
		}

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
	 * @param sb string builder instance
	 * @param key key string
	 * @param value value string
	 * @param useQuote true if value needs to be enclosed in quotes
	 */
	static private void append(StringBuilder sb, String key, String value, boolean useQuote) {

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
	 * @param sb string builder instance
	 * @param key key string
	 * @param value value string
	 */
	static private void append(StringBuilder sb, String key, String value) {
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
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
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
	private static String md5(String... tokens) throws IOException {
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
			throw new IOException(ex.getMessage());
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
	private static String randomBytes(int nbBytes) {
		byte[] bytes = new byte[nbBytes];
		randomGenerator.nextBytes(bytes);
		return bytesToHex(bytes);
	}

	private enum QOP {

		UNSPECIFIED(null),
		AUTH("auth"),
		AUTH_INT("auth-int");
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
			if (val.contains("auth-int")) {
				return QOP.AUTH_INT;
			}
			return QOP.AUTH;
		}
	}

	private enum ALGORITHM {

		UNSPECIFIED(null),
		MD5("md5"),
		MD5_SESS("md5-sess");
		private final String md;

		ALGORITHM(String md) {
			this.md = md;
		}

		@Override
		public String toString() {
			return md;
		}

		public static ALGORITHM parse(String val) {
			if (val == null || val.isEmpty()) {
				return ALGORITHM.UNSPECIFIED;
			}
			val = val.trim();
			if (val.contains("md5-sess")) {
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
		private final ALGORITHM algorithm;
		private final QOP qop;
		private final boolean stale;
		private volatile int nc;

		public DigestScheme(String realm,
				String nonce,
				String opaque,
				QOP qop,
				ALGORITHM algorithm,
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
			return nc++;
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

		public ALGORITHM getAlgorithm() {
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
