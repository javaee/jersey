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

package org.glassfish.jersey.tests.e2e.client;

import java.io.IOException;
import java.util.Calendar;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.AsyncInvoker;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientFactory;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.spi.RequestExecutorsProvider;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import junit.framework.Assert;

/**
 * Tests client async invocation with different {@link RequestExecutorsProvider request executor providers}.
 *
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 */
public class ClientAsyncTest extends JerseyTest {
    @Override
    protected Application configure() {
        return new ResourceConfig(Resource.class);
    }

    @Test
    public void testAsync() throws ExecutionException, InterruptedException {
        ClientConfig jerseyConfig = new ClientConfig();
        jerseyConfig.register
                (AsyncProvider.class).register(ThreadInterceptor.class);
        Client client = ClientFactory.newClient(jerseyConfig);
        _testAsyncClient(client);
    }

    @Test
    public void testAsyncWithProvidersInstances() throws ExecutionException, InterruptedException {
        ClientConfig jerseyConfig = new ClientConfig();
        jerseyConfig.register
                (new AsyncProvider()).register(ThreadInterceptor.class);
        Client client = ClientFactory.newClient(jerseyConfig);
        _testAsyncClient(client);
    }


    @Test
    public void testSync() throws ExecutionException, InterruptedException {
        ClientConfig jerseyConfig = new ClientConfig();
        jerseyConfig.register
                (AsyncProvider.class).register(ThreadInterceptor.class);
        Client client = ClientFactory.newClient(jerseyConfig);
        _testSyncClient(client);
    }


    @Test
    public void testSyncWithProvidersInstances() throws ExecutionException, InterruptedException {
        ClientConfig jerseyConfig = new ClientConfig();
        jerseyConfig.register
                (new AsyncProvider()).register(ThreadInterceptor.class);
        Client client = ClientFactory.newClient(jerseyConfig);
        _testSyncClient(client);
    }


    private void _testAsyncClient(Client client) throws InterruptedException, ExecutionException {
        WebTarget target = client.target(getBaseUri()).path("resource");
        AsyncInvoker async = target.request().async();
        System.out.println(Calendar.getInstance().getTime().toString());
        Future<Response> future = async.post(Entity.entity("post", MediaType.TEXT_PLAIN_TYPE));
        System.out.println(Calendar.getInstance().getTime().toString());

        final Response response = future.get();
        final String entity = response.readEntity(String.class);
        Assert.assertEquals("AsyncRequest-post", entity);
    }

    private void _testSyncClient(Client client) {
        WebTarget target = client.target(getBaseUri()).path("resource");
        System.out.println(Calendar.getInstance().getTime().toString());
        final Response response = target.request().post(Entity.entity("post", MediaType.TEXT_PLAIN_TYPE));
        System.out.println(Calendar.getInstance().getTime().toString());

        final String entity = response.readEntity(String.class);
        Assert.assertNotSame("AsyncRequest-post", entity);
    }


    public static class AsyncProvider implements RequestExecutorsProvider {

        @Override
        public ExecutorService getRequestingExecutor() {
            return Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("AsyncRequest").build());
        }
    }


    public static class ThreadInterceptor implements WriterInterceptor {

        @Override
        public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
            context.setEntity(Thread.currentThread().getName() + "-" + context.getEntity());
            context.proceed();
        }
    }

    @Path("resource")
    public static class Resource {
        @POST
        public String post(String entity) {
            System.out.println("waiting 100 ms...");
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("waiting finished.");
            return entity;
        }
    }
}


