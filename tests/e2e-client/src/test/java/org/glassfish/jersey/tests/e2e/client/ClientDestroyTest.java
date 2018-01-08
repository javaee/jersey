/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2017 Oracle and/or its affiliates. All rights reserved.
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
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;

import javax.annotation.PreDestroy;

import org.glassfish.jersey.client.ClientLifecycleListener;
import org.glassfish.jersey.client.JerseyClient;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Before;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThat;

/**
 * Assert that pre destroy method on providers is invoked.
 *
 * @author Michal Gajdos
 */
public class ClientDestroyTest extends JerseyTest {

    private static final Map<String, Boolean> destroyed = new HashMap<>();

    @Override
    @Before
    public void setUp() throws Exception {
        destroyed.clear();
        destroyed.put("filter", false);
        destroyed.put("reader", false);
        destroyed.put("feature", false);

        super.setUp();
    }

    @Path("/")
    public static class Resource {

        @GET
        public String get(@HeaderParam("foo") final String foo) {
            return "resource-" + foo;
        }
    }

    public static class MyFilter implements ClientRequestFilter {

        @Override
        public void filter(final ClientRequestContext requestContext) throws IOException {
            requestContext.getHeaders().putSingle("foo", "bar");
        }

        @PreDestroy
        public void preDestroy() {
            destroyed.put("filter", true);
        }
    }

    public static class MyReader implements ReaderInterceptor {

        @Override
        public Object aroundReadFrom(final ReaderInterceptorContext context) throws IOException, WebApplicationException {
            final Object entity = context.proceed();
            return "reader-" + entity;
        }

        @PreDestroy
        public void preDestroy() {
            destroyed.put("reader", true);
        }
    }

    public static class MyFeature implements Feature {

        @PreDestroy
        public void preDestroy() {
            destroyed.put("feature", true);
        }

        @Override
        public boolean configure(final FeatureContext context) {
            return true;
        }
    }

    @Test
    public void testClientInvokePreDestroyMethodOnProviderClass() throws Exception {
        final Client client = ClientBuilder.newClient()
                .register(MyFilter.class)
                .register(MyReader.class)
                .register(MyFeature.class);

        assertThat(client.target(getBaseUri()).request().get(String.class), is("reader-resource-bar"));

        checkDestroyed(false);
        client.close();
        checkDestroyed(true);
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(Resource.class);
    }

    private void checkDestroyed(final boolean shouldBeDestroyed) {
        for (final Map.Entry<String, Boolean> entry : destroyed.entrySet()) {
            assertThat(entry.getKey() +  " should" + (shouldBeDestroyed ? "" : " not") + " be destroyed",
                    entry.getValue(), is(shouldBeDestroyed));
        }
    }

    public static class FooListener implements ClientRequestFilter, ClientLifecycleListener {
        public static volatile boolean INITIALIZED = false;
        public static volatile boolean CLOSED = false;

        @Override
        public void filter(final ClientRequestContext requestContext) throws IOException { /* do nothing */ }

        @Override
        public void onClose() {
            CLOSED = true;
        }

        @Override
        public void onInit() {
            INITIALIZED = true;
        }

        // to check if closing works also for class-registered providers
        public static boolean isClosed() {
            return CLOSED;
        }

        public static boolean isInitialized() {
            return INITIALIZED;
        }
    }

    public static class BarListener implements ClientRequestFilter, ClientLifecycleListener {
        protected volatile boolean closed = false;
        protected volatile boolean initialized = false;

        @Override
        public void filter(final ClientRequestContext requestContext) throws IOException { /* do nothing */ }

        @Override
        public void onInit() {
            this.initialized = true;
        }

        @Override
        public synchronized void onClose() {
            this.closed = true;
        }

        public boolean isClosed() {
            return closed;
        }

        public boolean isInitialized() {
            return initialized;
        }
    }

    // another type needed, as multiple registrations of the same type are not allowed
    public static class BarListener2 extends BarListener {
    }

    @Test
    public void testLifecycleListenerProvider() {
        final JerseyClientBuilder builder = new JerseyClientBuilder();
        final JerseyClient client = builder.build();

        final BarListener filter = new BarListener();
        final BarListener filter2 = new BarListener2();

        // ClientRuntime initializes lazily, so it is forced by invoking a (dummy) request
        client.register(filter2);                                                   // instance registered into client
        client.target(getBaseUri()).register(filter).request().get(String.class);   // instance registration into target

        assertTrue("Filter was expected to be already initialized.", filter.isInitialized());
        assertTrue("Filter2 was expected to be already initialized.", filter2.isInitialized());

        client.target(getBaseUri()).register(FooListener.class).request().get(String.class); // class registration into target

        assertTrue("Class-registered filter was expected to be already initialized", FooListener.isInitialized());

        assertFalse("Class-registered filter was expected to be still open.", FooListener.isClosed());
        assertFalse("Filter was expected to be still open.", filter.isClosed());
        assertFalse("Filter2 was expected to be still open.", filter2.isClosed());

        client.close();

        assertTrue("Class-registered filter was expected to be closed.", FooListener.isClosed());
        assertTrue("Filter was expected to be closed.", filter.isClosed());
        assertTrue("Filter2 was expected to be closed.", filter2.isClosed());
    }

}
