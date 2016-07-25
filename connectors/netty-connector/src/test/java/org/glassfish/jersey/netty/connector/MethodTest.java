/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.netty.connector;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Tests the Http methods.
 *
 * @author Stepan Kopriva
 */
public class MethodTest extends JerseyTest {

    private static final String PATH = "test";

    @Path("/test")
    public static class HttpMethodResource {

        @GET
        public String get() {
            return "GET";
        }

        @POST
        public String post(String entity) {
            return entity;
        }

        @PUT
        public String put(String entity) {
            return entity;
        }

        @DELETE
        public String delete() {
            return "DELETE";
        }
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(HttpMethodResource.class);
    }

    @Override
    protected void configureClient(ClientConfig config) {
        config.connectorProvider(new NettyConnectorProvider());
    }

    @Test
    public void testGet() {
        Response response = target(PATH).request().get();
        assertEquals("GET", response.readEntity(String.class));
    }

    @Test
    public void testPost() {
        Response response = target(PATH).request().post(Entity.entity("POST", MediaType.TEXT_PLAIN));
        assertEquals("POST", response.readEntity(String.class));
    }

    @Test
    public void testPut() {
        Response response = target(PATH).request().put(Entity.entity("PUT", MediaType.TEXT_PLAIN));
        assertEquals("PUT", response.readEntity(String.class));
    }

    @Test
    public void testDelete() {
        Response response = target(PATH).request().delete();
        assertEquals("DELETE", response.readEntity(String.class));
    }
}
