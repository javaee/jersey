/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.tests.e2e.container;

import java.net.URI;

import javax.ws.rs.Encoded;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;

import org.junit.Test;
import static org.junit.Assert.assertNotEquals;

import static junit.framework.TestCase.assertEquals;

/**
 * Test Jersey container implementation of URL resolving.
 * In this test there is no context path that means that
 * slashes in URL are part of Resource address and couldn't
 * be deleted.
 *
 * @author Petr Bouda (petr.bouda at oracle.com)
 */
public class LeadingSlashesTest extends JerseyContainerTest {

    public static final String CONTAINER_RESPONSE = "Container-Response";

    @Override
    protected Application configure() {
        ResourceConfig resourceConfig = new ResourceConfig(SimpleResource.class,
                EmptyResource.class,
                EmptyPathParamResource.class);

        resourceConfig.property(ServerProperties.REDUCE_CONTEXT_PATH_SLASHES_ENABLED, true);
        return resourceConfig;
    }

    @Path("simple")
    public static class SimpleResource {

        @GET
        public String encoded() {
            return CONTAINER_RESPONSE;
        }

    }

    @Path("/")
    public static class EmptyPathParamResource {

        @GET
        @Path("{bar:.*}/{baz:.*}/test")
        public String getHello(@PathParam("bar") final String bar, @PathParam("baz") final String baz) {
            return bar + "-" + baz;
        }

        @GET
        @Path("{bar:.*}/{baz:.*}/testParams")
        public String helloWithQueryParams(@PathParam("bar") final String bar, @PathParam("baz") final String baz,
                                           @QueryParam("bar") final String queryBar, @QueryParam("baz") final String queryBaz) {
            return "PATH PARAM: " + bar + "-" + baz + ", QUERY PARAM " + queryBar + "-" + queryBaz;
        }

        @GET
        @Path("{bar:.*}/{baz:.*}/encoded")
        public String getEncoded(@Encoded @QueryParam("query") String queryParam) {
            return queryParam.equals("%25dummy23%2Ba") + ":" + queryParam;
        }
    }

    @Path("/")
    public static class EmptyResource {

        @GET
        @Path("/test")
        public String getHello() {
            return CONTAINER_RESPONSE;
        }
    }

    @Test
    public void testSimpleSlashes() {
        Response result = call("/simple");
        assertEquals(CONTAINER_RESPONSE, result.readEntity(String.class));

        result = call("//simple");
        assertNotEquals(CONTAINER_RESPONSE, result.readEntity(String.class));
    }

    @Test
    public void testSlashesWithBeginningEmpty() {
        Response result = call("/test");
        assertEquals(CONTAINER_RESPONSE, result.readEntity(String.class));
    }

    @Test
    public void testSlashesWithBeginningEmptyPathParam() {
        Response result = call("///test");
        assertEquals("-", result.readEntity(String.class));
    }

    @Test
    public void testSlashesWithBeginningEmptyPathParamWithQueryParams() {
        URI hostPort = UriBuilder.fromUri("http://localhost/").port(getPort()).build();
        WebTarget target = client().target(hostPort).path("///testParams")
                .queryParam("bar", "Container")
                .queryParam("baz", "Response");

        Response result = target.request().get();
        assertEquals("PATH PARAM: -, QUERY PARAM Container-Response", result.readEntity(String.class));
    }

    @Test
    public void testEncodedQueryParams() {
        URI hostPort = UriBuilder.fromUri("http://localhost/").port(getPort()).build();
        WebTarget target = client().target(hostPort).path("///encoded")
                .queryParam("query", "%dummy23+a");

        Response response = target.request().get();
        assertEquals(200, response.getStatus());
        assertEquals("true:%25dummy23%2Ba", response.readEntity(String.class));
    }


    private Response call(String path) {
        URI hostPort = UriBuilder.fromUri("http://localhost/").port(getPort()).build();
        return client().target(hostPort).path(path).request().get();
    }

}
