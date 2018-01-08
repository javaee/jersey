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

package org.glassfish.jersey.tests.e2e.client;

import java.util.concurrent.ExecutionException;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.client.AsyncInvoker;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.SyncInvoker;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test {@link GenericType} with {@link Response}.
 *
 * @author Miroslav Fuksa
 */
public class GenericResponseTest extends JerseyTest {

    @Override
    protected Application configure() {
        return new ResourceConfig(TestResource.class);
    }

    @Path("resource")
    public static class TestResource {

        @GET
        public String get() {
            return "get";
        }

        @POST
        public String post(String post) {
            return post;
        }
    }

    @Test
    public void testPost() {
        GenericType<Response> generic = new GenericType<Response>(Response.class);
        Entity entity = Entity.entity("entity", MediaType.WILDCARD_TYPE);

        WebTarget target = target("resource");
        SyncInvoker sync = target.request();

        Response response = sync.post(entity, generic);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("entity", response.readEntity(String.class));
    }

    @Test
    public void testAsyncPost() throws ExecutionException, InterruptedException {
        GenericType<Response> generic = new GenericType<Response>(Response.class);
        Entity entity = Entity.entity("entity", MediaType.WILDCARD_TYPE);

        WebTarget target = target("resource");
        final AsyncInvoker async = target.request().async();

        Response response = async.post(entity, generic).get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("entity", response.readEntity(String.class));
    }

    @Test
    public void testGet() {
        GenericType<Response> generic = new GenericType<Response>(Response.class);

        WebTarget target = target("resource");
        SyncInvoker sync = target.request();
        Response response = sync.get(generic);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("get", response.readEntity(String.class));
    }

    @Test
    public void testAsyncGet() throws ExecutionException, InterruptedException {
        GenericType<Response> generic = new GenericType<Response>(Response.class);

        WebTarget target = target("resource");
        final AsyncInvoker async = target.request().async();
        Response response = async.get(generic).get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("get", response.readEntity(String.class));
    }

    @Test
    public void testGetGenericString() {
        GenericType<String> generic = new GenericType<String>(String.class);

        WebTarget target = target("resource");
        SyncInvoker sync = target.request();
        final String entity = sync.get(generic);
        Assert.assertEquals("get", entity);
    }
}
