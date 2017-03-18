/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.e2e.server.filter;

import java.security.Principal;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Priorities;
import javax.ws.rs.client.Entity;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import javax.annotation.Priority;
import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 *
 * @author Paul Sandoz
 * @author Martin Matula
 */
public class RolesAllowedTest extends JerseyTest {

    @PreMatching
    @Priority(Priorities.AUTHENTICATION)
    public static class SecurityFilter implements ContainerRequestFilter {

        public void filter(final ContainerRequestContext request) {
            final String user = request.getHeaders().getFirst("X-USER");
            request.setSecurityContext(new Authenticator(user));
        }

        private static class Authenticator implements SecurityContext {

            private final Principal principal;

            Authenticator(final String name) {
                principal = name == null
                        ? null
                        : new Principal() {
                            public String getName() {
                                return name;
                            }
                        };
            }

            public Principal getUserPrincipal() {
                return principal;
            }

            public boolean isUserInRole(final String role) {
                return role.equals(principal.getName()) || ("user".equals(role) && "admin".equals(principal.getName()));
            }

            public boolean isSecure() {
                return false;
            }

            public String getAuthenticationScheme() {
                return "";
            }
        }
    }

    @Path("/")
    @PermitAll
    public static class Resource {

        @RolesAllowed("user")
        @GET
        public String get() {
            return "GET";
        }

        @RolesAllowed("admin")
        @POST
        public String post(final String content) {
            return content;
        }

        @Path("sub")
        public SubResource getSubResource() {
            return new SubResource();
        }
    }

    @RolesAllowed("admin")
    public static class SubResource {

        @Path("deny-all")
        @DenyAll
        @GET
        public String denyAll() {
            return "GET";
        }

        @Path("permit-all")
        @PermitAll
        @GET
        public String permitAll() {
            return "GET";
        }
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(Resource.class, RolesAllowedDynamicFeature.class, SecurityFilter.class);
    }

    @Test
    public void testGetAsUser() {
        assertEquals("GET", target().request().header("X-USER", "user").get(String.class));
    }

    @Test
    public void testGetAsAdmin() {
        assertEquals("GET", target().request().header("X-USER", "admin").get(String.class));
    }

    @Test
    public void testPostAsUser() {
        final Response cr = target().request().header("X-USER", "user").post(Entity.text("POST"));
        assertEquals(403, cr.getStatus());
    }

    @Test
    public void testPostAsAdmin() {
        assertEquals("POST", target().request().header("X-USER", "admin").post(Entity.text("POST"), String.class));
    }

    @Test
    public void testDenyAll() {
        assertEquals(403, target("sub/deny-all").request().header("X-USER", "admin").get().getStatus());
    }

    @Test
    public void testPermitAll() {
        assertEquals("GET", target("sub/permit-all").request().header("X-USER", "xyz").get(String.class));
    }

    @Test
    public void testNotAuthorized() {
        assertThat("User should not be authorized.", target().request().get().getStatus(), is(403));
    }
}
