/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.server.filter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.RequestContextBuilder;
import org.glassfish.jersey.server.ResourceConfig;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Tests capability of URI modification during pre-matching filtering.
 *
 * @author Paul Sandoz
 * @author Libor Kramolis (libor.kramolis at oracle.com)
 */
public class UriModificationFilterTest {

    @Path("/a/b")
    public static class Resource {
        @GET
        public String get(@Context UriInfo ui, @QueryParam("filter") String f) {
            assertEquals("c", f);
            return ui.getRequestUri().toASCIIString();
        }
    }

    @PreMatching
    public static class UriModifyFilter implements ContainerRequestFilter {
        public void filter(ContainerRequestContext requestContext) throws IOException {
            UriBuilder ub = requestContext.getUriInfo().getBaseUriBuilder();

            List<PathSegment> pss = requestContext.getUriInfo().getPathSegments();

            for (int i = 0; i < pss.size() - 1; i++) {
                ub.segment(pss.get(i).getPath());
            }
            ub.queryParam("filter", pss.get(pss.size() - 1).getPath());

            requestContext.setRequestUri(requestContext.getUriInfo().getBaseUri(), ub.build());
        }
    }

    @Test
    public void testWithInstance() throws ExecutionException, InterruptedException {
        final ResourceConfig resourceConfig = new ResourceConfig(Resource.class)
                .register(UriModifyFilter.class);
        final ApplicationHandler application = new ApplicationHandler(resourceConfig);
        final ContainerResponse response = application.apply(RequestContextBuilder.from("/a/b/c", "GET").build()).get();

        assertEquals(200, response.getStatus());
        assertEquals("/a/b?filter=c", response.getEntity());
    }
}
