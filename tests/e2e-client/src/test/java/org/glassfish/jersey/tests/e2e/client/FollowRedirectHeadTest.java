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

package org.glassfish.jersey.tests.e2e.client;

import java.net.URI;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests that configuration of {@link ClientProperties#FOLLOW_REDIRECTS} works when HEAD method is used.
 *
 * @author Paul Sandoz
 * @author Miroslav Fuksa
 */
public class FollowRedirectHeadTest extends JerseyTest {

    @Path("resource")
    public static class Resource {

        @Path("redirect")
        @GET
        public Response redirect() {
            return Response.status(303).location(URI.create("resource/final")).build();
        }

        @Path("final")
        @GET
        public Response afterRedirection() {
            return Response.ok("final-entity").build();
        }
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(Resource.class, LoggingFeature.class);
    }

    private WebTarget getTarget(boolean followRedirect) {
        Client client = ClientBuilder.newClient(new ClientConfig().property(ClientProperties.FOLLOW_REDIRECTS,
                followRedirect));
        return client.target(getBaseUri()).path("resource/redirect");
    }

    @Test
    public void testDontFollowRedirectHead() throws Exception {
        Response response = getTarget(false).request().head();
        Assert.assertEquals(303, response.getStatus());
        Assert.assertTrue(response.getLocation().toString().endsWith("/final"));
    }

    @Test
    public void testDontFollowRedirectGet() throws Exception {
        Response response = getTarget(false).request().get();
        Assert.assertEquals(303, response.getStatus());
        Assert.assertTrue(response.getLocation().toString().endsWith("/final"));
    }

    @Test
    public void testFollowRedirectHead() throws Exception {
        Response response = getTarget(true).request().head();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertFalse(response.hasEntity());
    }

    @Test
    public void testFollowRedirectGet() throws Exception {
        Response response = getTarget(true).request().get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("final-entity", response.readEntity(String.class));
    }
}
