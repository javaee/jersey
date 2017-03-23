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

package org.glassfish.jersey.tests.e2e.client;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.Charset;
import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.GET;
import javax.ws.rs.NameBinding;
import javax.ws.rs.Path;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.internal.util.Base64;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.util.runner.ConcurrentRunner;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests {@link org.glassfish.jersey.client.authentication.HttpAuthenticationFeature}.
 *
 * @author Miroslav Fuksa
 */
@RunWith(ConcurrentRunner.class)
public class HttpAuthorizationTest extends JerseyTest {

    @NameBinding
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(value = RetentionPolicy.RUNTIME)
    public static @interface Digest {
    }

    @NameBinding
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(value = RetentionPolicy.RUNTIME)
    public static @interface Basic {
    }

    @NameBinding
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(value = RetentionPolicy.RUNTIME)
    public static @interface Alternating {
    }

    /**
     * Alternates between BASIC and DIGEST (each is used for 2 requests).
     */
    @Alternating
    public static class AlternatingDigestBasicFilter implements ContainerRequestFilter {

        int counter = 0;

        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
            if ((counter++ / 2) % 2 == 0) {
                new BasicFilter().filter(requestContext);
            } else {
                new DigestFilter().filter(requestContext);
            }
        }
    }

    @Digest
    public static class DigestFilter implements ContainerRequestFilter {

        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
            final String authorization = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);

            if (authorization != null && authorization.trim().toUpperCase().startsWith("DIGEST")) {
                final Matcher match = Pattern.compile("username=\"([^\"]+)\"").matcher(authorization);
                if (!match.find()) {
                    return;
                }
                final String username = match.group(1);

                requestContext.setSecurityContext(new SecurityContext() {
                    @Override
                    public Principal getUserPrincipal() {
                        return new Principal() {
                            @Override
                            public String getName() {
                                return username;
                            }
                        };
                    }

                    @Override
                    public boolean isUserInRole(String role) {
                        return false;
                    }

                    @Override
                    public boolean isSecure() {
                        return false;
                    }

                    @Override
                    public String getAuthenticationScheme() {
                        return "DIGEST";
                    }
                });
                return;
            }
            requestContext.abortWith(Response.status(401).header(HttpHeaders.WWW_AUTHENTICATE,
                    "Digest realm=\"my-realm\", domain=\"\", nonce=\"n9iv3MeSNkEfM3uJt2gnBUaWUbKAljxp\", algorithm=MD5, "
                            + "qop=\"auth\", stale=false")
                    .build());
        }
    }

    /**
     * Basic Auth: password must be the same as user name except first letter is capitalized.
     * Example: username "homer" ->  password "Homer"
     */
    @Basic
    public static class BasicFilter implements ContainerRequestFilter {

        static final Charset CHARACTER_SET = Charset.forName("iso-8859-1");
        public static final String AUTH_SCHEME_CASE = "Auth-Scheme-Case";

        @Override
        public void filter(ContainerRequestContext request) throws IOException {

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader != null && authHeader.trim().toUpperCase().startsWith("BASIC")) {
                String decoded = new String(Base64.decode(authHeader.substring(6).getBytes()), CHARACTER_SET);
                //                String decoded = Base64.decodeAsString(authHeader.substring(6));
                final String[] split = decoded.split(":");
                final String username = split[0];
                final String pwd = split[1];
                String capitalizedUserName = username.substring(0, 1).toUpperCase() + username.substring(1);
                if (capitalizedUserName.equals(pwd)) {
                    request.setSecurityContext(new SecurityContext() {
                        @Override
                        public Principal getUserPrincipal() {
                            return new Principal() {
                                @Override
                                public String getName() {
                                    return username;
                                }
                            };
                        }

                        @Override
                        public boolean isUserInRole(String role) {
                            return true;
                        }

                        @Override
                        public boolean isSecure() {
                            return false;
                        }

                        @Override
                        public String getAuthenticationScheme() {
                            return "BASIC";
                        }
                    });
                    return;
                }
            }
            final String authSchemeCase = request.getHeaderString(AUTH_SCHEME_CASE);

            final String authScheme;
            if ("uppercase".equals(authSchemeCase)) {
                authScheme = "BASIC";
            } else if ("lowercase".equals(authSchemeCase)) {
                authScheme = "basic";
            } else {
                authScheme = "Basic";
            }

            request.abortWith(Response.status(401).header(HttpHeaders.WWW_AUTHENTICATE, authScheme).build());
        }
    }

    @Override
    protected Application configure() {
        ResourceConfig resourceConfig = new ResourceConfig(MyResource.class);
        resourceConfig.register(LoggingFeature.class);
        resourceConfig.register(new BasicFilter());
        resourceConfig.register(new DigestFilter());
        resourceConfig.register(new AlternatingDigestBasicFilter());
        return resourceConfig;
    }

    @Path("resource")
    public static class MyResource {

        @Context
        SecurityContext securityContext;

        @GET
        public String unsecure() {
            return "unsecure";
        }

        @GET
        @Path("basic")
        @Basic
        public String basic() {
            return securityContext.getAuthenticationScheme() + ":" + securityContext.getUserPrincipal().getName();
        }

        @GET
        @Path("digest")
        @Digest
        public String digest() {
            return securityContext.getAuthenticationScheme() + ":" + securityContext.getUserPrincipal().getName();
        }

        @GET
        @Path("alternating")
        @Alternating
        public String alternating() {
            return securityContext.getAuthenticationScheme() + ":" + securityContext.getUserPrincipal().getName();
        }

    }

    @Test
    public void testBasicPreemptive() {
        Response response = target().path("resource").path("basic")
                .register(HttpAuthenticationFeature.basicBuilder().credentials("homer", "Homer").build())
                .request().get();
        check(response, 200, "BASIC:homer");
    }

    @Test
    public void testBasicNonPreemptive() {
        Response response = target().path("resource").path("basic")
                .register(HttpAuthenticationFeature.basicBuilder().nonPreemptive().credentials("homer", "Homer").build())
                .request().get();
        check(response, 200, "BASIC:homer");
    }

    @Test
    public void testBasicNonPreemptiveWithEmptyPassword() {
        final WebTarget target = target().path("resource")
                .register(HttpAuthenticationFeature.basicBuilder().nonPreemptive().build());
        Response response = target.request().get();
        check(response, 200, "unsecure");

        try {
            response = target().path("resource").path("basic")
                    .register(HttpAuthenticationFeature.basicBuilder().nonPreemptive().build())
                    .request().get();
            Assert.fail("should throw an exception as credentials are missing");
        } catch (Exception e) {
            // ok
        }

        response = target.path("basic").request().property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_BASIC_USERNAME, "bart")
                .property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_BASIC_PASSWORD, "Bart")
                .get();

        check(response, 200, "BASIC:bart");
    }

    @Test
    public void testUniversalBasic() {
        Response response = target().path("resource").path("basic")
                .register(HttpAuthenticationFeature.universalBuilder().credentials("homer", "Homer").build())
                .request().get();
        check(response, 200, "BASIC:homer");
    }

    /**
     * Reproducer for JERSEY-2941: BasicAuthenticator#filterResponseAndAuthenticate: auth-scheme checks should be case
     * insensitve.
     */
    @Test
    public void testUniversalBasicCaseSensitivity() {
        Response response;

        response = target().path("resource").path("basic")
                .register(HttpAuthenticationFeature.universalBuilder().credentials("homer", "Homer").build())
                .request()
                // no AUTH_SCHEME_CASE header = mixed case
                .get();
        check(response, 200, "BASIC:homer");

        response = target().path("resource").path("basic")
                .register(HttpAuthenticationFeature.universalBuilder().credentials("homer", "Homer").build())
                .request()
                .header(BasicFilter.AUTH_SCHEME_CASE, "lowercase")
                .get();
        check(response, 200, "BASIC:homer");

        response = target().path("resource").path("basic")
                .register(HttpAuthenticationFeature.universalBuilder().credentials("homer", "Homer").build())
                .request()
                .header(BasicFilter.AUTH_SCHEME_CASE, "uppercase")
                .get();
        check(response, 200, "BASIC:homer");
    }

    @Test
    public void testUniversalBasicWrongPassword() {
        Response response = target().path("resource").path("basic")
                .register(HttpAuthenticationFeature.universalBuilder().credentials("homer", "FOO").build())
                .request().get();
        check(response, 401);
    }

    @Test
    public void testBasicWithDifferentCredentials() {
        final WebTarget target = target().path("resource").path("basic")
                .register(HttpAuthenticationFeature.basicBuilder().credentials("marge", "Marge").build());

        _testBasicWithDifferentCredentials(target);
    }

    @Test
    public void testBasicUniversalWithDifferentCredentials() {
        final WebTarget target = target().path("resource").path("basic")
                .register(HttpAuthenticationFeature.universalBuilder().credentials("marge", "Marge").build());

        _testBasicWithDifferentCredentials(target);
    }

    public void _testBasicWithDifferentCredentials(WebTarget target) {
        Response response = target
                .request().get();
        check(response, 200, "BASIC:marge");

        response = target.request().property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_USERNAME, "bart")
                .property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_PASSWORD, "Bart")
                .get();

        check(response, 200, "BASIC:bart");

        response = target.request().property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_BASIC_USERNAME, "bart")
                .property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_BASIC_PASSWORD, "Bart")
                .get();

        check(response, 200, "BASIC:bart");

        response = target.request().property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_DIGEST_USERNAME, "bart")
                .property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_DIGEST_PASSWORD, "Bart")
                .get();

        check(response, 200, "BASIC:marge");
    }

    @Test
    public void testDigest() {
        Response response = target().path("resource").path("digest")
                .register(HttpAuthenticationFeature.digest("homer", "Homer"))
                .request().get();
        check(response, 200, "DIGEST:homer");
    }

    @Test
    public void testDigestWithPasswords() {
        final WebTarget target = target().path("resource").path("digest")
                .register(HttpAuthenticationFeature.digest("homer", "Homer"));
        _testDigestWithPasswords(target);
    }

    @Test
    public void testUniversalDigestWithPasswords() {
        final WebTarget target = target().path("resource").path("digest")
                .register(HttpAuthenticationFeature.universalBuilder().credentials("homer", "Homer").build());
        _testDigestWithPasswords(target);
    }

    public void _testDigestWithPasswords(WebTarget target) {
        Response response = target.request().get();
        check(response, 200, "DIGEST:homer");

        response = target.request().property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_DIGEST_USERNAME, "bart")
                .property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_DIGEST_PASSWORD, "Bart")
                .get();
        check(response, 200, "DIGEST:bart");

        response = target.request().property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_BASIC_USERNAME, "bart")
                .property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_BASIC_PASSWORD, "Bart")
                .get();
        check(response, 200, "DIGEST:homer");

        response = target.request().property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_USERNAME, "bart")
                .property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_PASSWORD, "Bart")
                .get();
        check(response, 200, "DIGEST:bart");
    }

    @Test
    public void testDigestWithEmptyDefaultPassword() {
        final WebTarget target = target().path("resource")
                .register(HttpAuthenticationFeature.digest());
        _testDigestWithEmptyDefaultPassword(target);
    }

    @Test
    public void testDigestUniversalWithEmptyDefaultPassword() {
        final WebTarget target = target().path("resource")
                .register(HttpAuthenticationFeature.universalBuilder().build());
        _testDigestWithEmptyDefaultPassword(target);
    }

    public void _testDigestWithEmptyDefaultPassword(WebTarget target) {
        Response response = target.request().get();
        check(response, 200, "unsecure");

        response = target.request().property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_DIGEST_USERNAME, "bart")
                .property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_DIGEST_PASSWORD, "Bart")
                .get();
        check(response, 200, "unsecure");

        response = target.path("digest").request().property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_DIGEST_USERNAME, "bart")
                .property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_DIGEST_PASSWORD, "Bart")
                .get();
        check(response, 200, "DIGEST:bart");

        try {
            target.path("digest").request()
                    .property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_BASIC_USERNAME, "bart")
                    .property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_BASIC_PASSWORD, "Bart")
                    .get();
            Assert.fail("should throw an exception as no credentials were supplied for digest auth");
        } catch (Exception e) {
            // ok
        }

        response = target.path("digest").request().property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_USERNAME, "bart")
                .property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_PASSWORD, "Bart")
                .get();
        check(response, 200, "DIGEST:bart");
    }

    private void check(Response response, int status, String entity) {
        Assert.assertEquals(status, response.getStatus());
        Assert.assertEquals(entity, response.readEntity(String.class));
    }

    private void check(Response response, int status) {
        Assert.assertEquals(status, response.getStatus());
    }

    @Test
    public void testDigestUniversalSimple() {
        Response response = target().path("resource").path("digest")
                .register(HttpAuthenticationFeature.universalBuilder().credentials("homer", "Homer").build())
                .request().get();
        check(response, 200, "DIGEST:homer");
    }

    @Test
    public void testDigestUniversalSimple2() {
        Response response = target().path("resource").path("digest")
                .register(HttpAuthenticationFeature.universalBuilder().credentialsForDigest("homer", "Homer").build())
                .request().get();
        check(response, 200, "DIGEST:homer");
    }

    @Test
    public void testDigestUniversalSimple3() {
        Response response = target().path("resource").path("digest")
                .register(HttpAuthenticationFeature.universalBuilder()
                        .credentialsForDigest("homer", "Homer")
                        .credentialsForBasic("foo", "bar")
                        .build())
                .request().get();
        check(response, 200, "DIGEST:homer");
    }

    @Test
    public void testDigestUniversalSimple4() {
        Response response = target().path("resource").path("digest")
                .register(HttpAuthenticationFeature.universal("homer", "Homer"))
                .request().get();
        check(response, 200, "DIGEST:homer");
    }

    @Test
    public void testUniversal() {
        final WebTarget target = target().path("resource")
                .register(HttpAuthenticationFeature.universal("homer", "Homer"));

        check(target.request().get(), 200, "unsecure");
        check(target.path("digest").request().get(), 200, "DIGEST:homer");
        check(target.path("basic").request().get(), 200, "BASIC:homer");
        check(target.path("basic").request().get(), 200, "BASIC:homer");
        check(target.path("digest").request().get(), 200, "DIGEST:homer");
        check(target.path("digest").request().get(), 200, "DIGEST:homer");
        check(target.path("digest").request().get(), 200, "DIGEST:homer");
        check(target.path("basic").request().get(), 200, "BASIC:homer");
        check(target.path("basic").request().property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_DIGEST_USERNAME, "bart")
                .property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_DIGEST_PASSWORD, "Bart").get(), 200, "BASIC:homer");

        check(target.path("digest").request().property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_DIGEST_USERNAME, "bart")
                .property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_DIGEST_PASSWORD, "Bart").get(), 200, "DIGEST:bart");

        check(target.path("digest").request().property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_USERNAME, "bart")
                .property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_PASSWORD, "Bart").get(), 200, "DIGEST:bart");

        check(target.path("alternating").request().get(), 200, "BASIC:homer");
        check(target.path("alternating").request().get(), 200, "DIGEST:homer");
        check(target.path("alternating").request().get(), 200, "BASIC:homer");
        check(target.path("basic").request().get(), 200, "BASIC:homer");
        check(target.path("alternating").request().property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_USERNAME, "bart")
                .property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_PASSWORD, "Bart").get(), 200, "DIGEST:bart");
        check(target.path("alternating").request().property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_USERNAME, "bart")
                .property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_PASSWORD, "Bart").get(), 200, "BASIC:bart");

    }

}
