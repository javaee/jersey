/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2015 Oracle and/or its affiliates. All rights reserved.
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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.AsyncInvoker;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import javax.xml.bind.annotation.XmlRootElement;

import org.glassfish.jersey.client.ClientAsyncExecutor;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.spi.ThreadPoolExecutorProvider;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import jersey.repackaged.com.google.common.base.Function;
import jersey.repackaged.com.google.common.collect.Collections2;
import jersey.repackaged.com.google.common.util.concurrent.AbstractFuture;

import static javax.ws.rs.client.Entity.text;

/**
 * Tests sync and async client invocations.
 *
 * @author Miroslav Fuksa
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class BasicClientTest extends JerseyTest {

    @Override
    protected Application configure() {
        //        enable(TestProperties.LOG_TRAFFIC);
        //        enable(TestProperties.DUMP_ENTITY);
        return new ResourceConfig(Resource.class);
    }

    @Test
    public void testCustomExecutorsAsync() throws ExecutionException, InterruptedException {
        ClientConfig jerseyConfig = new ClientConfig();
        jerseyConfig.register(CustomExecutorProvider.class).register(ThreadInterceptor.class);
        Client client = ClientBuilder.newClient(jerseyConfig);
        runCustomExecutorTestAsync(client);
    }

    @Test
    public void testCustomExecutorsInstanceAsync() throws ExecutionException, InterruptedException {
        ClientConfig jerseyConfig = new ClientConfig();
        jerseyConfig.register(new CustomExecutorProvider()).register(ThreadInterceptor.class);
        Client client = ClientBuilder.newClient(jerseyConfig);
        runCustomExecutorTestAsync(client);
    }

    @Test
    public void testCustomExecutorsSync() throws ExecutionException, InterruptedException {
        ClientConfig jerseyConfig = new ClientConfig();
        jerseyConfig.register(CustomExecutorProvider.class).register(ThreadInterceptor.class);
        Client client = ClientBuilder.newClient(jerseyConfig);
        runCustomExecutorTestSync(client);
    }

    @Test
    public void testCustomExecutorsInstanceSync() throws ExecutionException, InterruptedException {
        ClientConfig jerseyConfig = new ClientConfig();
        jerseyConfig.register(new CustomExecutorProvider()).register(ThreadInterceptor.class);
        Client client = ClientBuilder.newClient(jerseyConfig);
        runCustomExecutorTestSync(client);
    }

    private void runCustomExecutorTestAsync(Client client) throws InterruptedException, ExecutionException {
        AsyncInvoker async = client.target(getBaseUri()).path("resource").request().async();

        Future<Response> f1 = async.post(text("post"));
        final Response response = f1.get();
        final String entity = response.readEntity(String.class);
        assertEquals("custom-async-request-post", entity);
    }

    private void runCustomExecutorTestSync(Client client) {
        WebTarget target = client.target(getBaseUri()).path("resource");
        final Response response = target.request().post(text("post"));

        final String entity = response.readEntity(String.class);
        assertNotSame("custom-async-request-post", entity);
    }

    @Test
    public void testAsyncClientInvocation() throws InterruptedException, ExecutionException {
        final WebTarget resource = target().path("resource");

        Future<Response> f1 = resource.request().async().post(text("post1"));
        final Response response = f1.get();
        assertEquals("post1", response.readEntity(String.class));

        Future<String> f2 = resource.request().async().post(text("post2"), String.class);
        assertEquals("post2", f2.get());

        Future<List<JaxbString>> f3 = resource.request().async().get(new GenericType<List<JaxbString>>() {
        });
        assertEquals(
                Arrays.asList("a", "b", "c").toString(),
                Collections2.transform(f3.get(), new Function<JaxbString, String>() {
                    @Override
                    public String apply(JaxbString input) {
                        return input.value;
                    }
                }).toString());

        final TestCallback<Response> c1 = new TestCallback<Response>() {

            @Override
            protected String process(Response result) {
                return result.readEntity(String.class);
            }
        };
        resource.request().async().post(text("post"), c1);
        assertEquals("post", c1.get());

        final TestCallback<String> c2 = new TestCallback<String>() {

            @Override
            protected String process(String result) {
                return result;
            }
        };
        resource.request().async().post(text("post"), c2);
        assertEquals("post", c2.get());

        final TestCallback<List<JaxbString>> c3 = new TestCallback<List<JaxbString>>() {

            @Override
            protected String process(List<JaxbString> result) {
                return Collections2.transform(result, new Function<JaxbString, String>() {
                    @Override
                    public String apply(JaxbString input) {
                        return input.value;
                    }
                }).toString();
            }
        };
        resource.request().async().get(c3);
        assertEquals(Arrays.asList("a", "b", "c").toString(), c3.get());
    }

    @Test
    public void testAsyncClientInvocationErrorResponse() throws InterruptedException, ExecutionException {
        final WebTarget errorResource = target().path("resource").path("error");

        Future<Response> f1 = errorResource.request().async().get();
        final Response response = f1.get();
        assertEquals(404, response.getStatus());
        assertEquals("error", response.readEntity(String.class));

        Future<String> f2 = errorResource.request().async().get(String.class);
        try {
            f2.get();
            fail("ExecutionException expected.");
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();
            assertTrue(WebApplicationException.class.isAssignableFrom(cause.getClass()));
            final Response r = ((WebApplicationException) cause).getResponse();
            assertEquals(404, r.getStatus());
            assertEquals("error", r.readEntity(String.class));
        }

        Future<List<String>> f3 = target().path("resource").path("errorlist").request()
                .async().get(new GenericType<List<String>>() {
                });
        try {
            f3.get();
            fail("ExecutionException expected.");
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();
            assertTrue(WebApplicationException.class.isAssignableFrom(cause.getClass()));
            final Response r = ((WebApplicationException) cause).getResponse();
            assertEquals(404, r.getStatus());
            assertEquals("error", r.readEntity(String.class));
        }

        final TestCallback<Response> c1 = new TestCallback<Response>() {

            @Override
            protected String process(Response result) {
                return result.readEntity(String.class);
            }
        };
        errorResource.request().async().get(c1);
        assertEquals("error", c1.get());

        final TestCallback<String> c2 = new TestCallback<String>() {

            @Override
            protected String process(String result) {
                return result;
            }
        };
        errorResource.request().async().get(c2);
        try {
            c2.get();
            fail("ExecutionException expected.");
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();
            assertTrue(WebApplicationException.class.isAssignableFrom(cause.getClass()));
            final Response r = ((WebApplicationException) cause).getResponse();
            assertEquals(404, r.getStatus());
            assertEquals("error", r.readEntity(String.class));
        }

        final TestCallback<List<JaxbString>> c3 = new TestCallback<List<JaxbString>>() {

            @Override
            protected String process(List<JaxbString> result) {
                return Collections2.transform(result, new Function<JaxbString, String>() {
                    @Override
                    public String apply(JaxbString input) {
                        return input.value;
                    }
                }).toString();
            }
        };
        target().path("resource").path("errorlist").request().async().get(c3);
        try {
            c3.get();
            fail("ExecutionException expected.");
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();
            assertTrue(WebApplicationException.class.isAssignableFrom(cause.getClass()));
            final Response r = ((WebApplicationException) cause).getResponse();
            assertEquals(404, r.getStatus());
            assertEquals("error", r.readEntity(String.class));
        }
    }

    @Test
    public void testSyncClientInvocation() throws InterruptedException, ExecutionException {
        final WebTarget resource = target().path("resource");

        Response r1 = resource.request().post(text("post1"));
        assertEquals("post1", r1.readEntity(String.class));

        String r2 = resource.request().post(text("post2"), String.class);
        assertEquals("post2", r2);

        List<JaxbString> r3 = resource.request().get(new GenericType<List<JaxbString>>() {
        });
        assertEquals(
                Arrays.asList("a", "b", "c").toString(),
                Collections2.transform(r3, new Function<JaxbString, String>() {
                    @Override
                    public String apply(JaxbString input) {
                        return input.value;
                    }
                }).toString());
    }

    @Test
    public void testSyncClientInvocationErrorResponse() throws InterruptedException, ExecutionException {
        final WebTarget errorResource = target().path("resource").path("error");

        Response r1 = errorResource.request().get();
        assertEquals(404, r1.getStatus());
        assertEquals("error", r1.readEntity(String.class));

        try {
            errorResource.request().get(String.class);
            fail("ExecutionException expected.");
        } catch (WebApplicationException ex) {
            final Response r = ex.getResponse();
            assertEquals(404, r.getStatus());
            assertEquals("error", r.readEntity(String.class));
        }

        try {
            target().path("resource").path("errorlist").request().get(new GenericType<List<String>>() {
            });
            fail("ExecutionException expected.");
        } catch (WebApplicationException ex) {
            final Response r = ex.getResponse();
            assertEquals(404, r.getStatus());
            assertEquals("error", r.readEntity(String.class));
        }
    }

    @Test
    // JERSEY-1412
    public void testAbortAsyncRequest() throws Exception {
        Invocation invocation = abortingTarget().request().buildPost(text("entity"));
        Future<String> future = invocation.submit(String.class);
        assertEquals("aborted", future.get());
    }

    @Test
    // JERSEY-1412
    public void testAbortSyncRequest() throws Exception {
        Invocation invocation = abortingTarget().request().buildPost(text("entity"));
        String response = invocation.invoke(String.class);
        assertEquals("aborted", response);
    }

    protected WebTarget abortingTarget() {
        Client client = ClientBuilder.newClient();
        client.register(new ClientRequestFilter() {
            @Override
            public void filter(ClientRequestContext ctx) throws IOException {
                Response r = Response.ok("aborted").build();
                ctx.abortWith(r);
            }
        });
        return client.target("http://any.web:888");
    }

    @ClientAsyncExecutor
    public static class CustomExecutorProvider extends ThreadPoolExecutorProvider {

        /**
         * Create a new instance of the thread pool executor provider.
         */
        public CustomExecutorProvider() {
            super("custom-async-request");
        }
    }

    public static class ThreadInterceptor implements WriterInterceptor {

        @Override
        public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
            final String name = Thread.currentThread().getName(); // e.g. custom-async-request-0
            final int lastIndexOfDash = name.lastIndexOf('-');
            context.setEntity(
                    name.substring(0, lastIndexOfDash < 0 ? name.length() : lastIndexOfDash) + "-" + context.getEntity());
            context.proceed();
        }
    }

    @Path("resource")
    public static class Resource {

        @GET
        @Path("error")
        public String getError() {
            throw new NotFoundException(Response.status(404).type("text/plain").entity("error").build());
        }

        @GET
        @Path("errorlist")
        @Produces(MediaType.APPLICATION_XML)
        public List<JaxbString> getErrorList() {
            throw new NotFoundException(Response.status(404).type("text/plain").entity("error").build());
        }

        @GET
        @Produces(MediaType.APPLICATION_XML)
        public List<JaxbString> get() {
            return Arrays.asList(new JaxbString("a"), new JaxbString("b"), new JaxbString("c"));
        }

        @POST
        public String post(String entity) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return entity;
        }
    }

    private abstract static class TestCallback<T> extends AbstractFuture<String> implements InvocationCallback<T> {

        @Override
        public void completed(T result) {
            try {
                set(process(result));
            } catch (Throwable t) {
                setException(t);
            }
        }

        protected abstract String process(T result);

        @Override
        public void failed(Throwable error) {
            if (error.getCause() instanceof WebApplicationException) {
                setException(error.getCause());
            } else {
                setException(error);
            }
        }
    }

    @XmlRootElement
    public static class JaxbString {

        public String value;

        public JaxbString() {
        }

        public JaxbString(String value) {
            this.value = value;
        }
    }
}


