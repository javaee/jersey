/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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
import java.util.Collection;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

/**
 * Client filter providing HTTP Digest Authentication
 *
 * @author raphael.jolivet@gmail.com
 * @author Stefan Katerkamp (stefan@katerkamp.de)
 */
@Provider
public class HttpDigestAuthFilter implements ClientRequestFilter, ClientResponseFilter {

	private static final Logger logger = Logger.getLogger(HttpDigestAuthFilter.class.getName());
	private static final Charset CHARACTER_SET = Charset.forName("iso-8859-1");
	static private final int CLIENT_NONCE_BYTE_COUNT = 4;
	static private final SecureRandom randomGenerator;

	static {
		try {
			randomGenerator = SecureRandom.getInstance("SHA1PRNG");
		} catch (Exception e) {
			throw new Error(e);
		}
	}
	private final String digest_username;
	private final byte[] digest_password;
	private String digest_nonce;
	private String digest_realm;
	private String digest_opaque;
	private ALGORITHM digest_algorithm;
	private QOP digest_qop = QOP.UNSPECIFIED;
	private int digest_nc = 1;
	boolean authorizationPhase = false;
	MultivaluedMap<String, Object> origHeaderMap = null;

	/**
	 * Creates a new HTTP Basic Authentication filter using provided username
	 * and password credentials.
	 *
	 * @param username
	 * @param password
	 */
	public HttpDigestAuthFilter(String username, String password) {
		this(username, (password != null) ? password.getBytes(CHARACTER_SET) : new byte[0]);
	}

	/**
	 * Creates a new HTTP Basic Authentication filter using provided username
	 * and password credentials. This constructor allows to avoid storing plain
	 * password value in a String variable.
	 *
	 * @param username
	 * @param password
	 */
	public HttpDigestAuthFilter(String username, byte[] password) {
		if (username == null) {
			username = "";
		}
		if (password == null) {
			password = new byte[0];
		}
		this.digest_username = username;
		this.digest_password = password;
	}

	/**
	 * Filter, which gets called before request is sent over the wire
	 *
	 * @param requestContext
	 * @throws IOException
	 */
	@Override
	public void filter(ClientRequestContext requestContext) throws IOException {

		origHeaderMap = requestContext.getHeaders();

		if (parseHeaders(requestContext.getHeaders().get(HttpHeaders.AUTHORIZATION)) != null) {
			authorizationPhase = true;
		} else if (this.digest_nonce != null) {
			String authLine = nextDigestAuthenticationToken(requestContext);
			requestContext.getHeaders().add(HttpHeaders.AUTHORIZATION, authLine);
		}

		if (logger.isLoggable(Level.FINEST)) {
			if (requestContext.getHeaderString(HttpHeaders.AUTHORIZATION) != null) {
				logger.log(Level.FINEST, "Client Request: {0}", requestContext.getHeaderString(HttpHeaders.AUTHORIZATION));
			}
		}
	}

	/**
	 * Filter, which gets called when response comes back from wire
	 */
	@Override
	public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {

		if (logger.isLoggable(Level.FINEST)) {
			if (responseContext.getHeaderString(HttpHeaders.WWW_AUTHENTICATE) != null) {
				logger.log(Level.FINEST, "Server Response: {0} {1}", new Object[]{responseContext.getStatus(),
					responseContext.getHeaderString(HttpHeaders.WWW_AUTHENTICATE)});
			}
		}

		if (Response.Status.fromStatusCode(responseContext.getStatus()) == Status.UNAUTHORIZED && !authorizationPhase) {

			HashMap<String, String> map = parseHeaders(responseContext.getHeaders().get(HttpHeaders.WWW_AUTHENTICATE));
			if (map == null) {
				return;
			}
			digest_realm = map.get("realm");
			digest_nonce = map.get("nonce");
			digest_opaque = map.get("opaque");
			digest_algorithm = ALGORITHM.parse(map.get("algorithm"));
			digest_qop = QOP.parse(map.get("qop"));

			String staleStr = map.get("stale");
			boolean stale = (staleStr != null) && staleStr.toLowerCase().equals("true");

			if (stale || !authorizationPhase) {

				authorizationPhase = true;

				// assemble athentication request
				Client client = requestContext.getClient();
				String method = requestContext.getMethod();
				MediaType mediaType = requestContext.getMediaType();
				URI luri = requestContext.getUri();
				WebTarget wt = client.target(luri);
				Builder builder = wt.request(mediaType);
				builder.headers(origHeaderMap);

				// send request
				Response serverResponse2 = null;
				switch (method) {
					case "GET":
						serverResponse2 = builder.get();
						break;
					case "POST":
						serverResponse2 = builder.post((Entity) requestContext.getEntity()); //@todo check
						break;
					default:
						throw new IOException("Method not implemented: " + method);
				}
				if (serverResponse2 == null) {
					return;
				}

				// merge response 2 into original response
				if (serverResponse2.hasEntity()) {
					String entity = serverResponse2.readEntity(String.class);
					responseContext.setEntityStream(new ByteArrayInputStream(entity.getBytes(CHARACTER_SET)));
				}
				MultivaluedMap<String, String> headers = responseContext.getHeaders();
				headers.clear();
				headers.putAll(serverResponse2.getStringHeaders());
				responseContext.setStatus(serverResponse2.getStatus());
			}
		}
	}

	/**
	 * Creates digest string including counter.
	 *
	 * @param requestContext
	 * @return
	 * @throws IOException
	 */
	private String nextDigestAuthenticationToken(ClientRequestContext requestContext) throws IOException {

		StringBuilder sb = new StringBuilder(100);
		sb.append("Digest ");
		append(sb, "username", digest_username);
		append(sb, "realm", digest_realm);
		append(sb, "nonce", digest_nonce);
		append(sb, "opaque", digest_opaque);
		append(sb, "algorithm", digest_algorithm.toString(), false);
		append(sb, "qop", digest_qop.toString(), false);

		String uri = requestContext.getUri().getPath();
		append(sb, "uri", uri);

		String ha1;
		if (digest_algorithm.equals(ALGORITHM.MD5_SESS)) {
			ha1 = md5(md5(digest_username, digest_realm, new String(digest_password)));
		} else {
			ha1 = md5(digest_username, digest_realm, new String(digest_password));
		}

		String ha2;
		if (digest_qop == QOP.AUTH_INT && requestContext.hasEntity()) {
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
		if (digest_qop.equals(QOP.UNSPECIFIED)) {
			response = md5(ha1, digest_nonce, ha2);
		} else {
			String cnonce = randomBytes(CLIENT_NONCE_BYTE_COUNT); // client nonce
			append(sb, "cnonce", cnonce);
			String nc = String.format("%08x", this.digest_nc); // counter
			this.digest_nc++;
			append(sb, "nc", nc, false);
			response = md5(ha1, digest_nonce, nc, cnonce, digest_qop.toString(), ha2);
		}
		append(sb, "response", response);

		return sb.toString();
	}

	/**
	 * Append comma plus key=value token
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

	static private void append(StringBuilder sb, String key, String value) {
		append(sb, key, value, true);
	}
	final protected static char[] hexArray = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

	/**
	 * @see
	 * http://stackoverflow.com/questions/9655181/convert-from-byte-array-to-hex-string-in-java
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
	 * @param tokens
	 * @return M5hash
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
	 */
	private static String randomBytes(int nbBytes) {
		byte[] bytes = new byte[nbBytes];
		randomGenerator.nextBytes(bytes);
		return bytesToHex(bytes);
	}
	static protected final Pattern KEY_VALUE_PAIR_PATTERN = Pattern.compile("(\\w+)\\s*=\\s*(\"([^\"]+)\"|(\\w+))\\s*,?\\s*");

	/**
	 * Parse digest header.
	 * @param lines
	 * @return  null if no digest header exists
	 */
	static public HashMap<String, String> parseHeaders(Collection<?> lines) {

		if (lines == null) {
			return null;
		}
		for (Object lineObject : lines) {

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

			Matcher match = KEY_VALUE_PAIR_PATTERN.matcher(parts[1]);
			HashMap<String, String> result = new HashMap<>();
			while (match.find()) {
				// expect 4 groups (key)=("(val)" | (val))
				int nbGroups = match.groupCount();
				if (nbGroups != 4) {
					continue;
				}
				String key = match.group(1);
				String valNoQuotes = match.group(3);
				String valQuotes = match.group(4);

				result.put(key, (valNoQuotes == null) ? valQuotes : valNoQuotes);
			}
			return result;
		}
		return null;
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
}
