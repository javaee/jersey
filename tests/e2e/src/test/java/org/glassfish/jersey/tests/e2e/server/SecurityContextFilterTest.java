/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.tests.e2e.server;

import static junit.framework.Assert.assertEquals;

import java.io.IOException;
import java.security.Principal;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.FilterContext;
import javax.ws.rs.ext.PreMatchRequestFilter;
import javax.ws.rs.ext.Provider;

import org.glassfish.jersey.internal.util.collection.Ref;
import org.glassfish.jersey.server.JerseyApplication;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Assert;
import org.junit.Test;
import org.jvnet.hk2.annotations.Inject;

/**
 * End to end test class for testing security context in the Filter and
 * resource.
 *
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 */
public class SecurityContextFilterTest extends JerseyTest {

    private static final String PRINCIPAL_NAME = "test_principal_setByFilter";
    private static final String SKIP_FILTER = "skipFilter";
    private static final String PRINCIPAL_IS_NULL = "principalIsNull";

    @Override
    protected JerseyApplication configure() {
        return JerseyApplication.builder(new ResourceConfig(SecurityContextFilter.class, Resource.class)).build();
    }

    @Provider
    public static class SecurityContextFilter implements PreMatchRequestFilter {

        @Inject
        Ref<SecurityContext> securityContextRef;
        @Context
        SecurityContext securityContext;

        @Override
        public void preMatchFilter(FilterContext context) throws IOException {
            // test injections
            Assert.assertNotNull(securityContext);
            Assert.assertEquals(securityContextRef.get(), securityContext);


            String header = context.getRequest().getHeaders().getHeader(SKIP_FILTER);
            if ("true".equals(header)) {
                return;
            }

            // test injections
            Assert.assertNotNull(securityContext);
            Assert.assertEquals(securityContextRef.get(), securityContext);

            // set new Security Context
            securityContextRef.set(new SecurityContext() {

                @Override
                public boolean isUserInRole(String role) {
                    return false;
                }

                @Override
                public boolean isSecure() {
                    return false;
                }

                @Override
                public Principal getUserPrincipal() {
                    return new Principal() {

                        @Override
                        public String getName() {
                            return PRINCIPAL_NAME;
                        }
                    };
                }

                @Override
                public String getAuthenticationScheme() {
                    return null;
                }
            });
        }
    }

    /**
     * Tests SecurityContext in filter.
     *
     * @throws Exception Thrown when request processing fails in the
     * application.
     */
    @Test
    public void testSecurityContextInjectionFilter() throws Exception {
        Response response = target().path("test").request().get();
        assertEquals(200, response.getStatus());
        String entity = response.readEntity(String.class);
        assertEquals(PRINCIPAL_NAME, entity);
    }

    /**
     * Tests SecurityContext in filter.
     *
     * @throws Exception Thrown when request processing fails in the
     * application.
     */
    @Test
    public void testContainerSecurityContext() throws Exception {
        Response response = target().path("test").request().header(SKIP_FILTER, "true").get();
        assertEquals(200, response.getStatus());
        String entity = response.readEntity(String.class);
        Assert.assertTrue(!entity.equals(PRINCIPAL_NAME));
    }

    @Path("test")
    /**
     * Test resource class.
     */
    public static class Resource {

        /**
         * Test resource method.
         *
         * @param securityCtx security context.
         * @return String response with principal name.
         */
        @GET
        public String getPrincipal(@Context SecurityContext securityCtx) {
            Assert.assertNotNull(securityCtx);
            Principal userPrincipal = securityCtx.getUserPrincipal();
            return userPrincipal == null ? PRINCIPAL_IS_NULL : userPrincipal.getName();
        }
    }
}
