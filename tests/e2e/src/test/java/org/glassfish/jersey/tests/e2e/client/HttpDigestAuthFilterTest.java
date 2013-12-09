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
package org.glassfish.jersey.tests.e2e.client;

import java.lang.reflect.Method;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.filter.HttpDigestAuthFilter;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 * @author Stefan Katerkamp <stefan@katerkamp.de>
 */
public class HttpDigestAuthFilterTest extends JerseyTest {

    private static final Logger logger = Logger.getLogger(HttpDigestAuthFilterTest.class.getName());
    private static final String DIGEST_TEST_LOGIN = "user";
    private static final String DIGEST_TEST_PASS = "password";
    private static final String DIGEST_TEST_INVALIDPASS = "nopass";
    // Digest string expected for OK auth:
    // Digest realm="test", nonce="eDePFNeJBAA=a874814ec55647862b66a747632603e5825acd39",
    //   algorithm=MD5, domain="/auth-digest/", qop="auth"
    private static final String DIGEST_TEST_NONCE = "eDePFNeJBAA=a874814ec55647862b66a747632603e5825acd39";
    private static final String DIGEST_TEST_REALM = "test";
    private static final String DIGEST_TEST_DOMAIN = "/auth-digest/";
    private static int ncExpected = 1;

    @Override
    protected Application configure() {
        enable(TestProperties.LOG_TRAFFIC);
        enable(TestProperties.DUMP_ENTITY);
        return new ResourceConfig(Resource.class);
    }

    @Path("/auth-digest")
    public static class Resource {

        private static Method md5 = null;
        @Context
        private HttpHeaders httpHeaders;
        @Context
        private UriInfo uriInfo;

        @GET
        public Response get1() {
            return verify();
        }


        @GET
        @Path("ěščřžýáíé")
        public Response getEncoding() {
            return verify();
        }

        private Response verify() {
            if (httpHeaders.getRequestHeader(HttpHeaders.AUTHORIZATION) == null) {
                // the first request has no authorization header, tell filter its 401
                // and send filter back seed for the new to be built header
                ResponseBuilder responseBuilder = Response.status(Response.Status.UNAUTHORIZED);
                responseBuilder = responseBuilder.header(HttpHeaders.WWW_AUTHENTICATE,
                        "Digest realm=\"" + DIGEST_TEST_REALM + "\", "
                                + "nonce=\"" + DIGEST_TEST_NONCE + "\", "
                                + "algorithm=MD5, "
                                + "domain=\"" + DIGEST_TEST_DOMAIN + "\", qop=\"auth\"");
                return responseBuilder.build();
            } else {
                // the filter takes the seed and adds the header
                List<String> authList = httpHeaders.getRequestHeader(HttpHeaders.AUTHORIZATION);
                if (authList.size() != 1) {
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
                }
                String authHeader = authList.get(0);

                String ha1 = md5(DIGEST_TEST_LOGIN, DIGEST_TEST_REALM, DIGEST_TEST_PASS);
                String ha2 = md5("GET", uriInfo.getRequestUri().getRawPath());
                String response = md5(
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

        /**
         * Colon separated value MD5 hash. Call md5 method of the filter.
         *
         * @param tokens one or more strings
         * @return M5 hash string
         */
        static String md5(String... tokens) {
            HttpDigestAuthFilter f = new HttpDigestAuthFilter("foo", "bar");
            String md5String = null;
            try {
                if (md5 == null) {
                    md5 = HttpDigestAuthFilter.class.getDeclaredMethod("md5", String[].class);
                    md5.setAccessible(true);
                }
                md5String = (String) md5.invoke(f, new Object[]{tokens});
            } catch (Exception ex) {
                logger.log(Level.SEVERE, null, ex);
            }
            return md5String;

        }

        /**
         * Get a value of the Digest Auth Header.
         *
         * @param authHeader digest auth header string
         * @param keyName key of the value to retrieve
         * @return value string
         */
        static String getDigestAuthHeaderValue(String authHeader, String keyName) {
            int i1 = authHeader.indexOf(keyName);

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
    public void testHttpDigestAuthFilter() {
        final String path = "auth-digest";
        testRequest(path);
    }

    @Test
    public void testHttpDigestAuthFilterWithEncodedUri() {
        final String path = "auth-digest/ěščřžýáíé";
        testRequest(path);
    }

    private void testRequest(String path) {
        ClientConfig jerseyConfig = new ClientConfig();

        Client client = ClientBuilder.newClient(jerseyConfig);
        client = client.register(new HttpDigestAuthFilter(DIGEST_TEST_LOGIN, DIGEST_TEST_PASS));

        WebTarget resource = client.target(getBaseUri()).path(path);

        ncExpected = 1;
        Response r1 = resource.request().get();
        Assert.assertEquals(Response.Status.fromStatusCode(r1.getStatus()), Response.Status.OK);
    }


    @Test
    public void testPreemptive() {
        ClientConfig jerseyConfig = new ClientConfig();

        Client client = ClientBuilder.newClient(jerseyConfig);
        client = client.register(new HttpDigestAuthFilter(DIGEST_TEST_LOGIN, DIGEST_TEST_PASS));

        WebTarget resource = client.target(getBaseUri()).path("auth-digest");

        ncExpected = 1;
        Response r1 = resource.request().get();
        Assert.assertEquals(Response.Status.fromStatusCode(r1.getStatus()), Response.Status.OK);

        ncExpected = 2;
        Response r2 = resource.request().get();
        Assert.assertEquals(Response.Status.fromStatusCode(r1.getStatus()), Response.Status.OK);

        ncExpected = 3;
        Response r3 = resource.request().get();
        Assert.assertEquals(Response.Status.fromStatusCode(r1.getStatus()), Response.Status.OK);

    }

    @Test
    public void testAuthentication() {
        ClientConfig jerseyConfig = new ClientConfig();

        Client client = ClientBuilder.newClient(jerseyConfig);
        client = client.register(new HttpDigestAuthFilter(DIGEST_TEST_LOGIN, DIGEST_TEST_INVALIDPASS));

        WebTarget resource = client.target(getBaseUri()).path("auth-digest");

        ncExpected = 1;
        Response r1 = resource.request().get();
        Assert.assertEquals(Response.Status.fromStatusCode(r1.getStatus()), Response.Status.UNAUTHORIZED);
    }
}
