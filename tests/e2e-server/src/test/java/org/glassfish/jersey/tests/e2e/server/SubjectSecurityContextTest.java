/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2017 Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.security.Principal;
import java.security.PrivilegedAction;

import javax.ws.rs.GET;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.Path;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.SubjectSecurityContext;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test subject based security context. Make sure resource
 * and sub-resource methods/locators are invoked
 * via {@link SubjectSecurityContext#doAsSubject(java.security.PrivilegedAction)} method.
 *
 * @author Martin Matula
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public class SubjectSecurityContextTest extends JerseyTest {

    // actual filter reference to keep track of the invocations
    SubjectSecurityContextSettingFilter subjectFilter;

    @Override
    protected Application configure() {
        return new ResourceConfig(Resource.class).registerInstances(subjectFilter = new SubjectSecurityContextSettingFilter());
    }

    @Path("/resource")
    public static class Resource {

        @GET
        public String resourceGet() {
            return "Resource GET";
        }

        @Path("subresource")
        public SubResource getSubResource() {
            return new SubResource();
        }

        @Path("subresource-wae")
        public SubResource getSubResourceEx() {
            throw new NotAcceptableException(Response.notAcceptable(null).entity("Not Acceptable SRL").build());
        }

        @Path("sub-get")
        @GET
        public String getSub() {
            return "Resource sub-GET";
        }

        @Path("sub-get-wae")
        @GET
        public String getSubEx() {
            throw new NotAcceptableException(Response.notAcceptable(null).entity("Not Acceptable Resource sub-GET").build());
        }
    }

    public static class SubResource {
        @GET
        public String subResourceGet() {
            return "SubResource GET";
        }

        @Path("wae")
        @GET
        public String subResourceGetEx() {
            throw new NotAcceptableException(Response.notAcceptable(null).entity("Not Acceptable SubResource GET").build());
        }
    }

    /**
     * Custom SubjectSecurityContext that keeps number of doAsSubject invocations.
     */
    public static class MySubjectSecurityContext implements SubjectSecurityContext {

        // no hits so far
        int hits = 0;

        @Override
        public Object doAsSubject(PrivilegedAction action) {
            hits++;
            return action.run();
        }

        @Override
        public Principal getUserPrincipal() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isUserInRole(String string) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isSecure() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getAuthenticationScheme() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    /**
     * Custom filter to set the custom subject security context on every request before the request get matched,
     * so that sub-resource locator invocations have a chance to see the subject security context set.
     */
    @PreMatching
    @Provider
    public static class SubjectSecurityContextSettingFilter implements ContainerRequestFilter {

        private final MySubjectSecurityContext securityContext = new MySubjectSecurityContext();

        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
            requestContext.setSecurityContext(securityContext);
        }

        /**
         * Provide actual doAsSubject hit count.
         *
         * @return number of doAsSubject invocations.
         */
        public int getHits() {
            return securityContext.hits;
        }
    }

    @Test
    public void testSubjectSecurityContext() {

        WebTarget r = target("/resource");

        assertThat(r.request().get(String.class), equalTo("Resource GET"));
        assertThat(subjectFilter.getHits(), equalTo(1)); // one resource method invoked

        assertThat(r.path("subresource").request().get(String.class), equalTo("SubResource GET"));
        assertThat(subjectFilter.getHits(), equalTo(3)); // + one sub-resource locator and one resource method invoked

        assertThat("Resource sub-GET", equalTo(r.path("sub-get").request().get(String.class)));
        assertThat(subjectFilter.getHits(), equalTo(4)); // + one sub-resource method invoked

        Response response;

        response = r.path("sub-get-wae").request().get();
        assertThat(response.getStatus(), equalTo(Response.Status.NOT_ACCEPTABLE.getStatusCode()));
        assertThat(response.readEntity(String.class), equalTo("Not Acceptable Resource sub-GET"));
        assertThat(subjectFilter.getHits(), equalTo(5)); // + one sub-resource method invoked

        response = r.path("subresource-wae").request().get();
        assertThat(response.getStatus(), equalTo(Response.Status.NOT_ACCEPTABLE.getStatusCode()));
        assertThat(response.readEntity(String.class), equalTo("Not Acceptable SRL"));
        assertThat(subjectFilter.getHits(), equalTo(6));

        response = r.path("subresource/wae").request().get();
        assertThat(response.getStatus(), equalTo(Response.Status.NOT_ACCEPTABLE.getStatusCode()));
        assertThat(response.readEntity(String.class), equalTo("Not Acceptable SubResource GET"));
        assertThat(subjectFilter.getHits(), equalTo(8)); // + one sub-resource locator and one resource method invoked
    }
}
