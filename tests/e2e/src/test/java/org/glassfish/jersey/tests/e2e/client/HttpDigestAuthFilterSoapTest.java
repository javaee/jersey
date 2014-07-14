/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2014 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.tests.e2e.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.transform.stream.StreamSource;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test Client to Server with DigestAuthentication and SOAPMessage Provider
 *
 * @author Stefan Katerkamp <stefan at katerkamp.de>
 */
public class HttpDigestAuthFilterSoapTest extends JerseyTest {

	private static final String DIGEST_TEST_LOGIN = "user";
	private static final String DIGEST_TEST_PASS = "password";
	private static final String DIGEST_TEST_NONCE = "eDePFNeJBAA=a874814ec55647862b66a747632603e5825acd39";
	private static final String DIGEST_TEST_REALM = "test";
	private static final String DIGEST_TEST_DOMAIN = "/auth-digest/";
	private static int ncExpected = 1;
	private static String messageBody = "";

	@Provider
	@Consumes("application/soap+xml")
	@Produces("application/soap+xml")
	public static class SOAP12Provider
			implements MessageBodyWriter<SOAPMessage>, MessageBodyReader<SOAPMessage> {

		@Override
		public boolean isWriteable(Class<?> aClass, Type type,
				Annotation[] annotations, MediaType mediaType) {
			return SOAPMessage.class.isAssignableFrom(aClass);
		}

		@Override
		public SOAPMessage readFrom(Class<SOAPMessage> soapEnvelopeClass, Type type,
				Annotation[] annotations, MediaType mediaType,
				MultivaluedMap<String, String> stringStringMultivaluedMap,
				InputStream inputStream) throws IOException, WebApplicationException {
			try {
				MessageFactory messageFactory = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
				StreamSource messageSource = new StreamSource(inputStream);
				SOAPMessage message = messageFactory.createMessage();
				SOAPPart soapPart = message.getSOAPPart();
				soapPart.setContent(messageSource);
				return message;
			} catch (SOAPException e) {
				StringWriter sw = new StringWriter();
				e.printStackTrace(new PrintWriter(sw));
				throw new IOException("SOAP Error. " + e.getMessage() + "\n" + sw.toString());
			}
		}

		@Override
		public long getSize(SOAPMessage soapMessage, Class<?> aClass, Type type,
				Annotation[] annotations, MediaType mediaType) {
			return -1;
		}

		@Override
		public void writeTo(SOAPMessage soapMessage, Class<?> aClass, Type type,
				Annotation[] annotations, MediaType mediaType,
				MultivaluedMap<String, Object> stringObjectMultivaluedMap,
				OutputStream outputStream) throws IOException, WebApplicationException {
			try {
				soapMessage.writeTo(outputStream);
			} catch (SOAPException e) {
				StringWriter sw = new StringWriter();
				e.printStackTrace(new PrintWriter(sw));
				throw new IOException("SOAP Error. " + e.getMessage() + "\n" + sw.toString());
			}
		}

		@Override
		public boolean isReadable(Class<?> aClass, Type type,
				Annotation[] annotations, MediaType mediaType) {
			return aClass.isAssignableFrom(SOAPMessage.class);
		}
	}

	@Override
	protected Application configure() {
		enable(TestProperties.LOG_TRAFFIC);
		enable(TestProperties.DUMP_ENTITY);
		return new ResourceConfig(Resource.class, SOAP12Provider.class);
	}

	@Path("/auth-digest")
	public static class Resource {

		@Context
		private HttpHeaders httpHeaders;
		@Context
		private UriInfo uriInfo;

		@POST
		@Path("int")
		public Response postInt() {
			return verifyIntegrity();
		}

		private Response verifyIntegrity() {
			if (httpHeaders.getRequestHeader(HttpHeaders.AUTHORIZATION) == null) {
				// the first request has no authorization header, tell filter its 401
				// and send filter back seed for the new to be built header
				ResponseBuilder responseBuilder = Response.status(Response.Status.UNAUTHORIZED);
				responseBuilder = responseBuilder.header(HttpHeaders.WWW_AUTHENTICATE,
						"Digest realm=\"" + DIGEST_TEST_REALM + "\", "
						+ "nonce=\"" + DIGEST_TEST_NONCE + "\", "
						+ "algorithm=MD5, "
						+ "domain=\"" + DIGEST_TEST_DOMAIN + "\", qop=\"auth-int\"");
				return responseBuilder.build();
			} else {
				// the filter takes the seed and adds the header
				final List<String> authList = httpHeaders.getRequestHeader(HttpHeaders.AUTHORIZATION);
				if (authList.size() != 1) {
					return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
				}
				final String authHeader = authList.get(0);

				final String ha1 = md5(DIGEST_TEST_LOGIN, DIGEST_TEST_REALM, DIGEST_TEST_PASS);
				final String ha2 = md5("POST", uriInfo.getRequestUri().getRawPath(), md5(messageBody));
				final String response = md5(
						ha1,
						DIGEST_TEST_NONCE,
						getDigestAuthHeaderValue(authHeader, "nc="),
						getDigestAuthHeaderValue(authHeader, "cnonce="),
						getDigestAuthHeaderValue(authHeader, "qop="),
						ha2);

				// this generates INTERNAL_SERVER_ERROR if not matching
				Assert.assertEquals(ncExpected, Integer.parseInt(getDigestAuthHeaderValue(authHeader, "nc=")));

				if (response.equals(getDigestAuthHeaderValue(authHeader, "response="))) {
					return Response.ok().build();
				} else {
					return Response.status(Response.Status.UNAUTHORIZED).build();
				}
			}
		}

		private static final Charset CHARACTER_SET = Charset.forName("UTF-8");

		/**
		 * Colon separated value MD5 hash. Call md5 method of the filter.
		 *
		 * @param tokens one or more strings
		 * @return M5 hash string
		 */
		static String md5(final String... tokens) {
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
				throw new ProcessingException(ex.getMessage());
			}
			md.update(sb.toString().getBytes(CHARACTER_SET), 0, sb.length());
			final byte[] md5hash = md.digest();
			return bytesToHex(md5hash);
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

		private static final char[] HEX_ARRAY = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

		/**
		 * Get a value of the Digest Auth Header.
		 *
		 * @param authHeader digest auth header string
		 * @param keyName key of the value to retrieve
		 * @return value string
		 */
		static String getDigestAuthHeaderValue(final String authHeader, final String keyName) {
			final int i1 = authHeader.indexOf(keyName);

			if (i1 == -1) {
				return null;
			}

			String value = authHeader.substring(
					authHeader.indexOf('=', i1) + 1,
					(authHeader.indexOf(',', i1) != -1
					? authHeader.indexOf(',', i1) : authHeader.length()));

			value = value.trim();
			if (value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"') {
				value = value.substring(1, value.length() - 1);
			}

			return value;
		}
	}

	@Test
	public void testHttpDigestAuthFilterIntegrity() throws SOAPException, IOException {
		final String path = "auth-digest/int";
		final ClientConfig jerseyConfig = new ClientConfig();

		ClientBuilder builder = ClientBuilder.newBuilder();
		builder = builder.withConfig(jerseyConfig);
		builder = builder.register(SOAP12Provider.class);
		Client client = builder.build();

		client = client.register(HttpAuthenticationFeature.digest(DIGEST_TEST_LOGIN, DIGEST_TEST_PASS));

		final WebTarget resource = client.target(getBaseUri()).path(path);

		ncExpected = 1;

		MessageFactory messageFactory = MessageFactory .newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
		SOAPMessage message = messageFactory.createMessage();
		SOAPPart soapPart = message.getSOAPPart();
		SOAPEnvelope envelope = soapPart.getEnvelope();
		SOAPBody body = envelope.getBody();
		body.addBodyElement(QName.valueOf("atest"));
		message.setProperty(SOAPMessage.WRITE_XML_DECLARATION, "true");
		message.saveChanges();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		message.writeTo(baos);
		baos.close();
		messageBody = baos.toString();

		Entity<SOAPMessage> esm = Entity.entity(message, "application/soap+xml");
		final Response response = resource.request("application/soap+xml").post(esm);
		SOAPMessage sm = response.readEntity(SOAPMessage.class);
		Assert.assertEquals(Response.Status.fromStatusCode(response.getStatus()), Response.Status.OK);
	}

}
