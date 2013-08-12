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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientRequest;
import org.glassfish.jersey.client.ClientResponse;
import org.glassfish.jersey.client.filter.HttpDigestAuthFilter.DigestScheme;
import org.glassfish.jersey.client.spi.AsyncConnectorCallback;
import org.glassfish.jersey.client.spi.Connector;
import org.glassfish.jersey.internal.util.Base64;

import org.junit.Test;
import org.junit.Assert;

/**
 * @author raphael.jolivet@gmail.com
 * @author Stefan Katerkamp (stefan@katerkamp.de
 */
public class HttpDigestAuthFilterTest {

	@Test
	public void testParseHeaders1() throws Exception // no digest scheme
	{
		HttpDigestAuthFilter f = new HttpDigestAuthFilter("foo", "bar");
		Method method = HttpDigestAuthFilter.class.getDeclaredMethod("parseAuthHeaders", List.class);
		method.setAccessible(true);
		DigestScheme ds = (DigestScheme) method.invoke(f,
				Arrays.asList(new String[]{
			"basic toto=tutu",
			"basic toto=\"tutu\""
		}));

		Assert.assertNull(ds);
	}

	@Test
	public void testParseHeaders2() throws Exception // Two concurrent schemes
	{
		HttpDigestAuthFilter f = new HttpDigestAuthFilter("foo", "bar");
		Method method = HttpDigestAuthFilter.class.getDeclaredMethod("parseAuthHeaders", List.class);
		method.setAccessible(true);
		DigestScheme ds = (DigestScheme) method.invoke(f,
				Arrays.asList(new String[]{
			"Digest realm=\"tata\"",
			"basic  toto=\"tutu\""
		}));
		Assert.assertNotNull(ds);

		Assert.assertEquals("tata", ds.getRealm());
	}

	@Test
	public void testParseHeaders3() throws Exception // Complex case, with comma inside value
	{
		HttpDigestAuthFilter f = new HttpDigestAuthFilter("foo", "bar");
		Method method = HttpDigestAuthFilter.class.getDeclaredMethod("parseAuthHeaders", List.class);
		method.setAccessible(true);
		DigestScheme ds = (DigestScheme) method.invoke(f,
				Arrays.asList(new String[]{
			"digest realm=\"tata\",nonce=\"foo, bar\""
		}));

		Assert.assertNotNull(ds);
		Assert.assertEquals("tata", ds.getRealm());
		Assert.assertEquals("foo, bar", ds.getNonce());
	}

	@Test
	public void testParseHeaders4() throws Exception // Spaces
	{
		HttpDigestAuthFilter f = new HttpDigestAuthFilter("foo", "bar");
		Method method = HttpDigestAuthFilter.class.getDeclaredMethod("parseAuthHeaders", List.class);
		method.setAccessible(true);
		DigestScheme ds = (DigestScheme) method.invoke(f,
				Arrays.asList(new String[]{
			"    digest realm =   \"tata\"  ,  opaque=\"bar\" ,nonce=\"foo, bar\""
		}));

		Assert.assertNotNull(ds);
		Assert.assertEquals("tata", ds.getRealm());
		Assert.assertEquals("foo, bar", ds.getNonce());
		Assert.assertEquals("bar", ds.getOpaque());
	}

	@Test
	public void testParseHeaders5() throws Exception // Mix of quotes and  non-quotes
	{
		HttpDigestAuthFilter f = new HttpDigestAuthFilter("foo", "bar");
		Method method = HttpDigestAuthFilter.class.getDeclaredMethod("parseAuthHeaders", List.class);
		method.setAccessible(true);
		DigestScheme ds = (DigestScheme) method.invoke(f,
				Arrays.asList(new String[]{
			"    digest realm =   \"tata\"  ,  opaque =bar ,nonce=\"foo, bar\""
		}));

		Assert.assertNotNull(ds);
		Assert.assertEquals("tata", ds.getRealm());
		Assert.assertEquals("foo, bar", ds.getNonce());
		Assert.assertEquals("bar", ds.getOpaque());
	}

	/*
	 Test max cache size of 0 
	  
	 @Test
	 public void testGet() {
	 HttpDigestAuthFilter filter = new HttpDigestAuthFilter("Uzivatelske jmeno", "Heslo");
	 ClientConfig clientConfig = new ClientConfig(filter).connector(new TestConnector());
	 Client client = ClientBuilder.newClient(clientConfig);
	 Invocation.Builder invBuilder = client.target(UriBuilder.fromUri("/").build()).request();
	 Response response = invBuilder.get();

	 String authHeader = response.getHeaderString(HttpHeaders.AUTHORIZATION);
	 Assert.assertEquals("Basic " + Base64.encodeAsString("Uzivatelske jmeno:Heslo"), authHeader);
	 }

	 @Test
	 public void testBlankUsernamePassword() {
	 HttpDigestAuthFilter filter = new HttpDigestAuthFilter(null, (String) null);
	 ClientConfig clientConfig = new ClientConfig(filter).connector(new TestConnector());
	 Client client = ClientBuilder.newClient(clientConfig);
	 Invocation.Builder invBuilder = client.target(UriBuilder.fromUri("/").build()).request();
	 Response response = invBuilder.get();

	 String authHeader = response.getHeaderString(HttpHeaders.AUTHORIZATION);
	 Assert.assertEquals("Basic " + Base64.encodeAsString(":"), authHeader);
	 }

	 private static class TestConnector implements Connector {

	 @Override
	 public ClientResponse apply(ClientRequest requestContext) {
	 final ClientResponse responseContext;
	 responseContext = new ClientResponse(Response.Status.OK, requestContext);

	 final String authHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
	 if (authHeader != null) {
	 responseContext.header(HttpHeaders.AUTHORIZATION, authHeader);
	 }
	 return responseContext;
	 }

	 @Override
	 public Future<?> apply(ClientRequest clientRequest, AsyncConnectorCallback callback) {
	 throw new UnsupportedOperationException("Asynchronous execution not supported.");
	 }

	 @Override
	 public void close() {
	 // do nothing
	 }

	 @Override
	 public String getName() {
	 return null;
	 }
	 }
	 * 
	 
+ * Test of validation of resources as an end-to-end test.
+ *
+public class AmbigousResourceMethodTest extends JerseyTest {
+
+    @Override
+    protected Application configure() {
+        return new ResourceConfig(TestResource.class);
+    }
+
+    @Test
+    public void testRequestToAmbiguousResourceClass() {
+        final String simpleName = TestResource.class.getSimpleName();
+
+        Response response = 
target().path("test").request(MediaType.TEXT_PLAIN).get();
+        assertEquals(200, response.getStatus());
+        assertEquals(simpleName + simpleName, 
response.readEntity(String.class));
+
+        response = 
target().path("test").request(MediaType.TEXT_HTML_TYPE).get();
+        assertEquals(200, response.getStatus());
+        assertEquals(simpleName, response.readEntity(String.class));
+
+        response = 
target().path("test").request(MediaType.TEXT_HTML_TYPE).post(Entity.entity("aaaa",
 MediaType.TEXT_PLAIN_TYPE));
+        assertEquals(200, response.getStatus());
+        assertEquals(simpleName + simpleName, 
response.readEntity(String.class));
+
+        response = 
target().path("test").request(MediaType.TEXT_HTML_TYPE).post(Entity.entity("aaaa",
 MediaType.TEXT_HTML_TYPE));
+        assertEquals(200, response.getStatus());
+        assertEquals(simpleName, response.readEntity(String.class));
+    }
+
+     * Test ambiguous resource class.
+    @Path("test")
+    public static class TestResource {
+        @POST
+        public String sub() {
+            return getClass().getSimpleName();
+        }
+
+        @POST
+        @Consumes(MediaType.TEXT_PLAIN)
+        public String subsub() {
+            return sub() + sub();
+        }
+
+        @GET
+        public String get() {
+            return sub();
+        }
+
+        @GET
+        @Produces(MediaType.TEXT_PLAIN)
+        public String getget() {
+            return subsub();
+        }
+    }
+}
	 * 
	 * 
	 * siehe 
	 * /tests/e2e/src/test/java/org/glassfish/jersey/tests/e2e/client

	 */
}
