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
package org.glassfish.jersey.server;

import java.io.IOException;
import java.security.Principal;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;

import org.glassfish.jersey.internal.util.collection.Ref;

import org.jvnet.hk2.annotations.Inject;

import org.junit.Assert;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * Test class for testing security context in the Filter and resource.
 *
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 */
public class SecurityContextTest {

    private static final String PRINCIPAL_NAME = "SetByFilter";
    private static final String SKIP_FILTER = "skipFilter";
    private static final String PRINCIPAL_IS_NULL = "principalIsNull";

    private static class SecurityContextFilter implements ContainerRequestFilter {

        @Inject
        Ref<SecurityContext> securityContextRef;
        @Context
        SecurityContext securityContext;

        @Override
        public void filter(ContainerRequestContext rc) throws IOException {
            // test injections
            Assert.assertNotNull(securityContext);
            Assert.assertEquals(securityContextRef.get(), securityContext);
            Assert.assertTrue(securityContext.getUserPrincipal() == null);

            String header = rc.getHeaders().getFirst(SKIP_FILTER);
            if ("true".equals(header)) {
                return;
            }



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
     * @throws Exception Thrown when request processing fails in the applicaton.
     */
    @Test
    public void testSecurityContextInjectionFilter() throws Exception {
        final ResourceConfig resourceConfig = new ResourceConfig(Resource.class, SecurityContextFilter.class);
        final ApplicationHandler application = new ApplicationHandler(resourceConfig);

        JerseyContainerResponseContext response = application.apply(RequestContextBuilder.from("/test", "GET").build()).get();
        assertEquals(response.getStatus(), 200);
        assertEquals(response.getEntity(), PRINCIPAL_NAME);
    }

    /**
     * Tests SecurityContext in filter.
     *
     * @throws Exception Thrown when request processing fails in the
     * application.
     */
    @Test
    public void testDefaultSecurityContext() throws Exception {
        final ResourceConfig resourceConfig = new ResourceConfig(Resource.class, SecurityContextFilter.class);
        final ApplicationHandler application = new ApplicationHandler(resourceConfig);

        JerseyContainerResponseContext response =
                application.apply(RequestContextBuilder.from("/test", "GET").header(SKIP_FILTER, "true").build()).get();
        assertEquals(200, response.getStatus());
        Object entity = response.getEntity();
        Assert.assertTrue(!PRINCIPAL_NAME.equals(entity));
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
        public String getSomething(@Context SecurityContext securityCtx) {
            Assert.assertNotNull(securityCtx);
            Principal userPrincipal = securityCtx.getUserPrincipal();
            return userPrincipal == null ? PRINCIPAL_IS_NULL : userPrincipal.getName();
        }
    }
}
