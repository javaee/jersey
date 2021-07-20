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
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package org.glassfish.jersey.client;

import java.io.IOException;

import javax.ws.rs.ConstrainedTo;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.Response;

import javax.annotation.Priority;

import org.glassfish.jersey.CommonProperties;
import org.glassfish.jersey.internal.spi.AutoDiscoverable;
import org.glassfish.jersey.internal.util.PropertiesHelper;

import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Note: Auto-discoverables from this test "affects" all other tests in suit.
 *
 * @author Michal Gajdos
 */
public class AutoDiscoverableClientTest {

    private static final String PROPERTY = "AutoDiscoverableTest";

    private static final ClientRequestFilter component = new ClientRequestFilter() {
        @Override
        public void filter(final ClientRequestContext requestContext) throws IOException {
            requestContext.abortWith(Response.status(400).entity("CommonAutoDiscoverable").build());
        }
    };

    public static class CommonAutoDiscoverable implements AutoDiscoverable {

        @Override
        public void configure(final FeatureContext context) {
            // Return if PROPERTY is not true - applicable for other tests.
            if (!PropertiesHelper.isProperty(context.getConfiguration().getProperty(PROPERTY))) {
                return;
            }

            context.register(component, 1);
        }
    }

    @Priority(10)
    public static class AbortFilter implements ClientRequestFilter {

        @Override
        public void filter(final ClientRequestContext requestContext) throws IOException {
            requestContext.abortWith(Response.status(400).entity("AbortFilter").build());
        }
    }

    public static class FooLifecycleListener implements ContainerRequestFilter, ClientLifecycleListener {
        private static boolean CLOSED = false;
        private static boolean INITIALIZED = false;

        @Override
        public void onInit() {
            INITIALIZED = true;
        }

        @Override
        public void onClose() {
            CLOSED = true;
        }

        @Override
        public void filter(final ContainerRequestContext requestContext) throws IOException {
            // do nothing
        }

        public static boolean isClosed() {
            return CLOSED;
        }

        public static boolean isInitialized() {
            return INITIALIZED;
        }
    }

    @ConstrainedTo(RuntimeType.CLIENT)
    public static class LifecycleListenerAutoDiscoverable implements AutoDiscoverable {
        @Override
        public void configure(final FeatureContext context) {
            // Return if PROPERTY is not true - applicable for other tests.
            if (!PropertiesHelper.isProperty(context.getConfiguration().getProperty(PROPERTY))) {
                return;
            }
            context.register(new FooLifecycleListener(), 1);
        }
    }

    @Test
    public void testAutoDiscoverableGlobalDefaultServerDefault() throws Exception {
        _test("CommonAutoDiscoverable", null, null);
    }

    @Test
    public void testAutoDiscoverableGlobalDefaultServerEnabled() throws Exception {
        _test("CommonAutoDiscoverable", null, false);
    }

    @Test
    public void testAutoDiscoverableGlobalDefaultServerDisabled() throws Exception {
        _test("AbortFilter", null, true);
    }

    @Test
    public void testAutoDiscoverableGlobalDisabledServerDefault() throws Exception {
        _test("AbortFilter", true, null);
    }

    @Test
    public void testAutoDiscoverableGlobalDisabledServerEnabled() throws Exception {
        _test("CommonAutoDiscoverable", true, false);
    }

    @Test
    public void testAutoDiscoverableGlobalDisabledServerDisabled() throws Exception {
        _test("AbortFilter", true, true);
    }

    @Test
    public void testAutoDiscoverableGlobalEnabledServerDefault() throws Exception {
        _test("CommonAutoDiscoverable", false, null);
    }

    @Test
    public void testAutoDiscoverableGlobalEnabledServerEnabled() throws Exception {
        _test("CommonAutoDiscoverable", false, false);
    }

    @Test
    public void testAutoDiscoverableGlobalEnabledServerDisabled() throws Exception {
        _test("AbortFilter", false, true);
    }

    /**
     * Tests, that {@link org.glassfish.jersey.client.ClientLifecycleListener} registered via
     * {@link org.glassfish.jersey.internal.spi.AutoDiscoverable}
     * {@link javax.ws.rs.core.Feature} will be notified when {@link javax.ws.rs.client.Client#close()} is invoked.
     */
    @Test
    @Ignore("intermittent failures.")
    public void testAutoDiscoverableClosing() {
        final ClientConfig config = new ClientConfig();
        config.property(PROPERTY, true);
        final JerseyClient client = (JerseyClient) ClientBuilder.newClient(config);

        assertFalse(FooLifecycleListener.isClosed());

        client.getConfiguration().getRuntime(); // force runtime init
        assertTrue("FooLifecycleListener was expected to be already initialized.", FooLifecycleListener.isInitialized());
        assertFalse("FooLifecycleListener was not expected to be closed yet.", FooLifecycleListener.isClosed());

        client.close();

        assertTrue("FooLifecycleListener should have been closed.", FooLifecycleListener.isClosed());
    }

    private void _test(final String response, final Boolean globalDisable, final Boolean clientDisable) throws Exception {
        final ClientConfig config = new ClientConfig();
        config.register(AbortFilter.class);
        config.property(PROPERTY, true);

        if (globalDisable != null) {
            config.property(CommonProperties.FEATURE_AUTO_DISCOVERY_DISABLE, globalDisable);
        }
        if (clientDisable != null) {
            config.property(ClientProperties.FEATURE_AUTO_DISCOVERY_DISABLE, clientDisable);
        }

        final Client client = ClientBuilder.newClient(config);
        final Invocation.Builder request = client.target("").request();

        assertEquals(response, request.get().readEntity(String.class));
    }
}
