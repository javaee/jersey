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
package org.glassfish.jersey.tests.e2e.server;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.ws.rs.GET;
import javax.ws.rs.NameBinding;
import javax.ws.rs.Path;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * JAX-RS global name-bound filter tests.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @see ResourceFilterTest
 */
public class GloballyNameBoundResourceFilterTest extends JerseyTest {

    public static final String TEST_REQUEST_HEADER = "test-request-header";

    @Override
    protected Application configure() {
        return new MyApplication();
    }

    @GloballyBound
    public static final class MyApplication extends ResourceConfig {
        public MyApplication() {
            super(
                    MyResource.class,
                    GloballyBoundRequestFilter.class,
                    GloballyBoundResponseFilter.class

            );
        }
    }

    @Test
    public void testGlobalyBoundPostMatching() {
        Response r = target("postMatching").request().get();
        assertThat(r.getStatus(), equalTo(200));
        assertThat(r.hasEntity(), is(true));
        assertThat(r.readEntity(String.class), equalTo("requestFilter-method-responseFilter"));
    }

    // See JERSEY-1554
    @Test
    public void testGlobalyBoundPostMatchingRequestFilterNotInvokedOn404() {
        Response r = target("notFound").request().get();
        assertEquals(404, r.getStatus());
        assertThat(r.hasEntity(), is(true));
        assertThat(r.readEntity(String.class), equalTo("responseFilter"));
    }

    @Path("/")
    public static class MyResource {

        @Path("postMatching")
        @GET
        public String getPostMatching(@Context HttpHeaders headers) {
            final String header = headers.getHeaderString(TEST_REQUEST_HEADER);
            return header + "-method";
        }
    }

    @NameBinding
    @Retention(RetentionPolicy.RUNTIME)
    private static @interface GloballyBound {
    }

    @GloballyBound
    public static class GloballyBoundRequestFilter implements ContainerRequestFilter {
        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
            requestContext.getHeaders().putSingle(TEST_REQUEST_HEADER, "requestFilter");
        }
    }

    @GloballyBound
    public static class GloballyBoundResponseFilter implements ContainerResponseFilter {
        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
            responseContext.setEntity(
                    responseContext.hasEntity() ? responseContext.getEntity() + "-responseFilter" : "responseFilter",
                    responseContext.getEntityAnnotations(),
                    responseContext.getMediaType());
        }
    }
}
