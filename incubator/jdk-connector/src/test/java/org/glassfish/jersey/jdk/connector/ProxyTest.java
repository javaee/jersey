/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.jdk.connector;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Application;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.internal.util.Base64;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Petr Janouch (petr.janouch at oracle.com)
 */
public class ProxyTest extends JerseyTest {

    private static final Charset CHARACTER_SET = Charset.forName("iso-8859-1");

    private static final String PROXY_HOST = "localhost";
    private static final int PROXY_PORT = 8321;
    private static final String PROXY_USER_NAME = "petr";
    private static final String PROXY_PASSWORD = "my secret password";

    @Test
    public void testConnect() throws IOException {
        doTest(Proxy.Authentication.NONE);
    }

    @Test
    public void testBasicAuthentication() throws IOException {
        doTest(Proxy.Authentication.BASIC);
    }

    @Test
    public void testDigestAuthentication() throws IOException {
        doTest(Proxy.Authentication.DIGEST);
    }

    private void doTest(Proxy.Authentication authentication) throws IOException {
        Proxy proxy = new Proxy(authentication);
        try {
            proxy.start();
            javax.ws.rs.core.Response response = target("resource").request().get();
            assertEquals(200, response.getStatus());
            assertEquals("OK", response.readEntity(String.class));
            assertTrue(proxy.getProxyHit());
        } finally {
            proxy.stop();
        }
    }

    @Test
    public void authenticationFailTest() throws IOException {
        Proxy proxy = new Proxy(Proxy.Authentication.BASIC);
        try {
            proxy.start();
            proxy.setAuthernticationFail(true);
            try {
                target("resource").request().get();
                fail();
            } catch (Exception e) {
                assertEquals(ProxyAuthenticationException.class, e.getCause().getCause().getClass());
            }

            assertTrue(proxy.getProxyHit());
        } finally {
            proxy.stop();
        }
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(Resource.class);
    }

    @Override
    protected void configureClient(final ClientConfig config) {
        config.property(JdkConnectorProvider.MAX_CONNECTIONS_PER_DESTINATION, 1);
        config.property(ClientProperties.PROXY_URI, "http://" + PROXY_HOST + ":" + PROXY_PORT);
        config.property(ClientProperties.PROXY_USERNAME, PROXY_USER_NAME);
        config.property(ClientProperties.PROXY_PASSWORD, PROXY_PASSWORD);
        config.connectorProvider(new JdkConnectorProvider());
    }

    @Path("/resource")
    public static class Resource {

        @GET
        public String get() {
            return "OK";
        }
    }

    private static class Proxy {

        private final HttpServer server = HttpServer.createSimpleServer("/", PROXY_HOST, PROXY_PORT);
        private volatile String destinationUri = null;
        private final Authentication authentication;
        private volatile boolean proxyHit = false;
        private volatile boolean authenticationFail = false;

        Proxy(Authentication authentication) {
            this.authentication = authentication;
        }

        boolean getProxyHit() {
            return proxyHit;
        }

        void setAuthernticationFail(boolean authenticationFail) {
            this.authenticationFail = authenticationFail;
        }

        void start() throws IOException {
            server.getServerConfiguration().addHttpHandler(new HttpHandler() {
                public void service(Request request, Response response) throws Exception {
                    if (request.getMethod().getMethodString().equals("CONNECT")) {
                        proxyHit = true;

                        String authorizationHeader = request.getHeader("Proxy-Authorization");

                        if (authentication != Authentication.NONE && authorizationHeader == null) {
                            // if we need authentication and receive CONNECT with no Proxy-authorization header, send 407
                            send407(request, response);
                            return;
                        }

                        if (authenticationFail) {
                            send407(request, response);
                            return;
                        }

                        if (authentication == Authentication.BASIC) {
                            if (!verifyBasicAuthorizationHeader(response, authorizationHeader)) {
                                return;
                            }

                            // if success continue

                        } else if (authentication == Authentication.DIGEST) {
                            if (!verifyDigestAuthorizationHeader(response, authorizationHeader)) {
                                return;
                            }

                            // if success continue
                        }

                        // check that both Host header and URI contain host:port
                        String requestURI = request.getRequestURI();
                        String host = request.getHeader("Host");
                        if (!requestURI.equals(host)) {
                            response.setStatus(400);
                            System.out.println("Request URI: " + requestURI);
                            System.out.println("Host header: " + host);
                            return;
                        }

                        // save the destination where a normal proxy would open a connection
                        destinationUri = "http://" + requestURI;
                        response.setStatus(200);
                        hackGrizzlyConnect(request, response);
                        return;
                    }

                    handleTrafficAfterConnect(request, response);
                }
            });

            server.start();
        }

        private void send407(Request request, Response response) {
            response.setStatus(407);

            if (authentication == Authentication.BASIC) {
                response.setHeader("Proxy-Authenticate", "Basic");
            } else {
                response.setHeader("Proxy-Authenticate", "Digest realm=\"my-realm\", domain=\"\", "
                        + "nonce=\"n9iv3MeSNkEfM3uJt2gnBUaWUbKAljxp\", algorithm=MD5, \"\n"
                        + "                            + \"qop=\"auth\", stale=false");
            }
            hackGrizzlyConnect(request, response);
        }

        private boolean verifyBasicAuthorizationHeader(Response response, String authorizationHeader) {
            if (!authorizationHeader.startsWith("Basic")) {
                System.out.println(
                        "Authorization header during Basic authentication does not start with \"Basic\"");
                response.setStatus(400);
                return false;
            }
            String decoded = new String(Base64.decode(authorizationHeader.substring(6).getBytes()),
                    CHARACTER_SET);
            final String[] split = decoded.split(":");
            final String username = split[0];
            final String password = split[1];

            if (!username.equals(PROXY_USER_NAME)) {
                response.setStatus(400);
                System.out.println("Found unexpected username: " + username);
                return false;
            }

            if (!password.equals(PROXY_PASSWORD)) {
                response.setStatus(400);
                System.out.println("Found unexpected password: " + username);
                return false;
            }

            return true;
        }

        private boolean verifyDigestAuthorizationHeader(Response response, String authorizationHeader) {
            if (!authorizationHeader.startsWith("Digest")) {
                System.out.println(
                        "Authorization header during Digest authentication does not start with \"Digest\"");
                response.setStatus(400);
                return false;
            }

            final Matcher match = Pattern.compile("username=\"([^\"]+)\"").matcher(authorizationHeader);
            if (!match.find()) {
                return false;
            }
            final String username = match.group(1);
            if (!username.equals(PROXY_USER_NAME)) {
                response.setStatus(400);
                System.out.println("Found unexpected username: " + username);
                return false;
            }

            return true;
        }

        private void hackGrizzlyConnect(Request request, Response response) {
            // Grizzly does not like CONNECT method and sets keep alive to false
            // This hacks Grizzly, so it will keep the connection open
            response.getResponse().getProcessingState().setKeepAlive(true);
            response.getResponse().setContentLength(0);
            request.setMethod("GET");
        }

        private void handleTrafficAfterConnect(Request request, Response response) throws IOException {
            if (destinationUri == null) {
                // It seems that CONNECT has not been called
                System.out.println("Received non-CONNECT without receiving CONNECT first");
                response.setStatus(400);
                return;
            }

            // create a client and relay the request to the final destination
            ClientConfig clientConfig = new ClientConfig();
            clientConfig.connectorProvider(new JdkConnectorProvider());
            Client client = ClientBuilder.newClient(clientConfig);

            Invocation.Builder destinationRequest = client.target(destinationUri).path(request.getRequestURI()).request();
            for (String headerName : request.getHeaderNames()) {
                destinationRequest.header(headerName, request.getHeader(headerName));
            }

            javax.ws.rs.core.Response destinationResponse = destinationRequest
                    .method(request.getMethod().getMethodString());

            // translate the received response into the proxy response
            response.setStatus(destinationResponse.getStatus());
            OutputStream outputStream = response.getOutputStream();
            String body = destinationResponse.readEntity(String.class);
            outputStream.write(body.getBytes());
            client.close();
        }

        void stop() {
            server.shutdown();
        }

        private enum Authentication {
            NONE,
            BASIC,
            DIGEST
        }
    }
}
