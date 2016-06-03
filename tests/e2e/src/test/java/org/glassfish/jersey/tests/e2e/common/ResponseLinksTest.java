/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2016 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.tests.e2e.common;

import java.net.URI;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Libor Kamolis (libor.kramolis at oracle.com)
 */
public class ResponseLinksTest extends JerseyTest {

    @Path("/test")
    public static class MyResource {

        @Context
        private UriInfo uriInfo;

        /**
         * Reproducer for JERSEY-2168
         */
        @Path("1")
        @GET
        @Produces({MediaType.APPLICATION_JSON})
        public Response getLink() {
            URI link = uriInfo.getAbsolutePathBuilder().queryParam("limit", 50).build();
            return Response.status(Response.Status.OK).link(link, "prev").build();
        }

        /**
         * Reproducer for JERSEY-2168
         */
        @Path("2")
        @GET
        @Produces({MediaType.APPLICATION_JSON})
        public Response getLinks() {
            Link link1 = Link.fromUri(uriInfo.getAbsolutePathBuilder().queryParam("limit", 50).build())
                    .rel("prev").build();
            Link link2 = Link.fromUri(
                    uriInfo.getAbsolutePathBuilder().queryParam("limit", 50).queryParam("action", "next").build()).rel("next")
                    .title("next page").build();
            return Response.status(Response.Status.OK).links(link1, link2).build();
        }
    }

    @Override
    protected Application configure() {
        ResourceConfig resourceConfig = new ResourceConfig(MyResource.class);
        resourceConfig.register(LoggingFeature.class);
        return resourceConfig;
    }

    @Override
    protected void configureClient(ClientConfig config) {
        config.register(LoggingFeature.class);
        super.configureClient(config);
    }

    /**
     * Reproducer for JERSEY-2168
     */
    @Test
    public void testGetLink() {
        WebTarget target = target("test");
        Response response = target.path("1").request(MediaType.APPLICATION_JSON).get();
        Set<Link> links = response.getLinks();
        Assert.assertEquals(1, links.size());
        Assert.assertNotNull(response.getLink("prev"));
        Assert.assertTrue(response.getLink("prev").getUri().toString().endsWith("1?limit=50"));
    }

    /**
     * Reproducer for JERSEY-2168
     */
    @Test
    public void testGetLinks() {
        WebTarget target = target("test");
        Response response = target.path("2").request(MediaType.APPLICATION_JSON).get();
        Set<Link> links = response.getLinks();
        Assert.assertEquals(2, links.size());
        Assert.assertNotNull(response.getLink("prev"));
        Assert.assertTrue(response.getLink("prev").getUri().toString().endsWith("2?limit=50"));
        Assert.assertNotNull(response.getLink("next"));
        Assert.assertEquals("next page", response.getLink("next").getTitle());
        Assert.assertTrue(response.getLink("next").getUri().toString().endsWith("2?limit=50&action=next"));
    }

}
