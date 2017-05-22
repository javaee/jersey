/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.e2e.oauth;

import java.net.URI;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;

import javax.inject.Inject;

import org.glassfish.jersey.client.oauth1.AccessToken;
import org.glassfish.jersey.client.oauth1.ConsumerCredentials;
import org.glassfish.jersey.client.oauth1.OAuth1AuthorizationFlow;
import org.glassfish.jersey.client.oauth1.OAuth1ClientSupport;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.oauth1.DefaultOAuth1Provider;
import org.glassfish.jersey.server.oauth1.OAuth1Provider;
import org.glassfish.jersey.server.oauth1.OAuth1ServerFeature;
import org.glassfish.jersey.server.oauth1.OAuth1ServerProperties;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

import com.sun.security.auth.UserPrincipal;

/**
 * Tests client and server OAuth 1 functionality.
 *
 * @author Miroslav Fuksa
 */
public class OAuthClientServerTest extends JerseyTest {

    private static final String SECRET_CONSUMER_KEY = "secret-consumer-key";
    private static final String CONSUMER_KEY = "my-consumer-key";
    private static final String CONSUMER_NAME = "my-consumer";
    private static final String PROMETHEUS_TOKEN = "prometheus-token";
    private static final String PROMETHEUS_SECRET = "prometheus-secret";

    @Override
    protected Application configure() {

        final DefaultOAuth1Provider oAuthProvider = new DefaultOAuth1Provider();
        oAuthProvider.registerConsumer(CONSUMER_NAME, CONSUMER_KEY,
                SECRET_CONSUMER_KEY, new MultivaluedHashMap<String, String>());

        final Principal prometheusPrincipal = new Principal() {
            @Override
            public String getName() {
                return "prometheus";
            }
        };

        oAuthProvider.addAccessToken(PROMETHEUS_TOKEN, PROMETHEUS_SECRET, CONSUMER_KEY,
                "http://callback.url", prometheusPrincipal,
                Arrays.asList("admin", "user").stream().collect(Collectors.toSet()),
                new MultivaluedHashMap<String, String>());
        final OAuth1ServerFeature oAuth1ServerFeature = new OAuth1ServerFeature(oAuthProvider,
                "requestTokenSpecialUri", "accessTokenSpecialUri");
        final ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.register(oAuth1ServerFeature);
        resourceConfig.register(MyProtectedResource.class);
        resourceConfig.register(new LoggingFeature(Logger.getLogger(OAuthClientServerTest.class.getName()),
                LoggingFeature.Verbosity.PAYLOAD_ANY));
        resourceConfig.register(OAuthAuthorizationResource.class);
        resourceConfig.property(OAuth1ServerProperties.TIMESTAMP_UNIT, "SECONDS");
        resourceConfig.property(OAuth1ServerProperties.MAX_NONCE_CACHE_SIZE, 20);
        return resourceConfig;
    }

    @Path("resource")
    public static class MyProtectedResource {

        @Context
        private SecurityContext securityContext;

        @GET
        public String get() {
            return securityContext.getUserPrincipal().getName();
        }

        @Path("admin")
        @GET
        public boolean getFoo() {
            return securityContext.isUserInRole("admin");
        }
    }

    @Path("user-authorization")
    public static class OAuthAuthorizationResource {

        @Inject
        private OAuth1Provider provider;

        @GET
        public String mustBeGetMethod(@QueryParam("oauth_token") String token) {
            System.out.println("Token received from user: " + token);
            final DefaultOAuth1Provider defProvider = (DefaultOAuth1Provider) provider;
            assertEquals("http://consumer/callback/homer", defProvider.getRequestToken(token).getCallbackUrl());

            return defProvider.authorizeToken(
                    defProvider.getRequestToken(token),
                    new UserPrincipal("homer"),
                    Collections.singleton("user"));
        }
    }

    /**
     * Tests client and server OAuth.
     * <p/>
     * Tests authorization flow including the request to a protected resource. The test uses {@link OAuth1AuthorizationFlow}
     * to perform user authorization and uses authorized client for requesting protected resource.
     * <p/>
     * The resource {@link OAuthAuthorizationResource} is used to perform user authorization (this is done
     * programmatically from the test). Finally, the Access Token is retrieved and used to request the
     * protected resource. In this resource the user principal is used to return the name of the user stored
     * in {@link SecurityContext}.
     */
    @Test
    public void testAuthorizationFlow() {
        String tempCredUri = UriBuilder.fromUri(getBaseUri()).path("requestTokenSpecialUri").build().toString();
        String accessTokenUri = UriBuilder.fromUri(getBaseUri()).path("accessTokenSpecialUri").build().toString();
        final String userAuthorizationUri = UriBuilder.fromUri(getBaseUri()).path("user-authorization").build().toString();
        final OAuth1AuthorizationFlow authFlow = OAuth1ClientSupport
                .builder(new ConsumerCredentials(CONSUMER_KEY, SECRET_CONSUMER_KEY))
                .authorizationFlow(tempCredUri, accessTokenUri, userAuthorizationUri)
                .callbackUri("http://consumer/callback/homer").build();

        final String authUri = authFlow.start();
        // authorize by a request to authorization URI
        final Response userAuthResponse = ClientBuilder.newClient().target(authUri).request().get();
        assertEquals(200, userAuthResponse.getStatus());
        final String verifier = userAuthResponse.readEntity(String.class);
        System.out.println("Verifier: " + verifier);

        authFlow.finish(verifier);
        final Client authorizedClient = authFlow.getAuthorizedClient();

        Response response = authorizedClient.target(getBaseUri()).path("resource")
                .request().get();
        assertEquals(200, response.getStatus());
        assertEquals("homer", response.readEntity(String.class));

        response = authorizedClient.target(getBaseUri()).path("resource").path("admin").request().get();
        assertEquals(200, response.getStatus());
        assertEquals(false, response.readEntity(boolean.class));
    }

    /**
     * Tests {@link org.glassfish.jersey.client.oauth1.OAuth1ClientFilter} already configured with Access Token for signature
     * purposes only.
     */
    @Test
    public void testRequestSigning() {
        final Feature filterFeature = OAuth1ClientSupport.builder(
                new ConsumerCredentials(CONSUMER_KEY, SECRET_CONSUMER_KEY)).feature()
                .accessToken(new AccessToken(PROMETHEUS_TOKEN, PROMETHEUS_SECRET)).build();
        final Client client = ClientBuilder.newBuilder()
                .register(filterFeature).build();
        final URI resourceUri = UriBuilder.fromUri(getBaseUri()).path("resource").build();
        final WebTarget target = client.target(resourceUri);
        Response response;
        for (int i = 0; i < 15; i++) {
            System.out.println("request: " + i);
            response = target.request().get();
            assertEquals(200, response.getStatus());
            assertEquals("prometheus", response.readEntity(String.class));
            i++;
            response = target.path("admin").request().get();
            assertEquals(200, response.getStatus());
            assertEquals(true, response.readEntity(boolean.class));
        }
    }

    /**
     * Tests configuration of the nonce cache on the server side.
     */
    @Test
    public void testRequestSigningWithExceedingCache() {
        final Feature filterFeature = OAuth1ClientSupport.builder(
                new ConsumerCredentials(CONSUMER_KEY, SECRET_CONSUMER_KEY)).feature()
                .accessToken(new AccessToken(PROMETHEUS_TOKEN, PROMETHEUS_SECRET)).build();
        final Client client = ClientBuilder.newBuilder()
                .register(filterFeature).build();
        final URI resourceUri = UriBuilder.fromUri(getBaseUri()).path("resource").build();
        final WebTarget target = client.target(resourceUri);
        Response response;
        for (int i = 0; i < 20; i++) {
            System.out.println("request: " + i);
            response = target.request().get();
            assertEquals(200, response.getStatus());
            assertEquals("prometheus", response.readEntity(String.class));
            i++;
            response = target.path("admin").request().get();
            assertEquals(200, response.getStatus());
            assertEquals(true, response.readEntity(boolean.class));
        }
        // now the nonce cache is full
        response = target.request().get();
        assertEquals(401, response.getStatus());
    }
}
