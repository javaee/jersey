/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015-2017 Oracle and/or its affiliates. All rights reserved.
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
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Objects;

import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NameBinding;
import javax.ws.rs.Path;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import javax.inject.Inject;

import org.glassfish.jersey.server.ExtendedUriInfo;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * {@link ExtendedUriInfo} e2e tests - testing e.g. getting matched resources, mapped throwable, etc.
 *
 * @author Michal Gajdos
 */
public class ExtendedUriInfoTest extends JerseyTest {

    @Override
    protected Application configure() {
        return new ResourceConfig(ThrowableResource.class, MappedThrowableResponseFilter.class, MappedExceptionMapper.class);
    }

    @NameBinding
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(value = RetentionPolicy.RUNTIME)
    public static @interface MappedThrowable {}

    @Path("mapped-throwable-test")
    @MappedThrowable
    public static class ThrowableResource {

        @GET
        @Path("unmapped")
        public Response unmapped() {
            throw new RuntimeException();
        }

        @GET
        @Path("mapped")
        public Response mapped() {
            throw new MappedException();
        }

        @GET
        @Path("webapp")
        public Response webapp() {
            throw new InternalServerErrorException();
        }

        @GET
        @Path("regular")
        public Response regular() {
            return Response.ok().build();
        }
    }

    public static class MappedException extends RuntimeException {
    }

    public static class MappedExceptionMapper implements ExceptionMapper<MappedException> {

        @Override
        public Response toResponse(final MappedException exception) {
            return Response.ok().build();
        }
    }

    @MappedThrowable
    public static class MappedThrowableResponseFilter implements ContainerResponseFilter {

        @Inject
        private ExtendedUriInfo uriInfo;

        @Override
        public void filter(final ContainerRequestContext requestContext, final ContainerResponseContext responseContext)
                throws IOException {
            responseContext.setEntity(Objects.toString(uriInfo.getMappedThrowable()));
        }
    }

    @Test
    public void testUnmappedThrowableValue() throws Exception {
        assertThat("Internal Server Error expected - response filter not invoked",
                target("mapped-throwable-test/unmapped").request().get().getStatus(),
                is(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()));
    }

    @Test
    public void testMappedThrowableValue() throws Exception {
        assertThat("MappedException expected in ExtendedUriInfo#getMappedThrowable",
                target("mapped-throwable-test/mapped").request().get().readEntity(String.class),
                is("org.glassfish.jersey.tests.e2e.server.ExtendedUriInfoTest$MappedException"));
    }

    @Test
    public void testWebAppThrowableValue() throws Exception {
        assertThat("InternalServerErrorException expected in ExtendedUriInfo#getMappedThrowable",
                target("mapped-throwable-test/webapp").request().get().readEntity(String.class),
                containsString("javax.ws.rs.InternalServerErrorException"));
    }

    @Test
    public void testRegularResourceValue() throws Exception {
        assertThat("null expected in ExtendedUriInfo#getMappedThrowable",
                target("mapped-throwable-test/regular").request().get().readEntity(String.class),
                is("null"));
    }
}
