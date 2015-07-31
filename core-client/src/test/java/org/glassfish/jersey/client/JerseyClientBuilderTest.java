/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.client;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.Response;

import javax.annotation.Priority;
import javax.net.ssl.SSLContext;

import org.junit.Before;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * {@link JerseyClient} unit test.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @author Michal Gajdos
 */
public class JerseyClientBuilderTest {

    private JerseyClientBuilder builder;

    @Before
    public void setUp() {
        builder = new JerseyClientBuilder();
    }

    @Test
    public void testBuildClientWithNullSslConfig() throws KeyStoreException {
        try {
            builder.sslContext(null);
            fail("NullPointerException expected for 'null' SSL context.");
        } catch (NullPointerException npe) {
            // pass
        }

        try {
            builder.keyStore(null, "abc");
            fail("NullPointerException expected for 'null' SSL context.");
        } catch (NullPointerException npe) {
            // pass
        }
        try {
            builder.keyStore(null, "abc".toCharArray());
            fail("NullPointerException expected for 'null' SSL context.");
        } catch (NullPointerException npe) {
            // pass
        }

        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        try {
            builder.keyStore(ks, (String) null);
            fail("NullPointerException expected for 'null' SSL context.");
        } catch (NullPointerException npe) {
            // pass
        }
        try {
            builder.keyStore(ks, (char[]) null);
            fail("NullPointerException expected for 'null' SSL context.");
        } catch (NullPointerException npe) {
            // pass
        }

        try {
            builder.keyStore(null, (String) null);
            fail("NullPointerException expected for 'null' SSL context.");
        } catch (NullPointerException npe) {
            // pass
        }
        try {
            builder.keyStore(null, (char[]) null);
            fail("NullPointerException expected for 'null' SSL context.");
        } catch (NullPointerException npe) {
            // pass
        }

        try {
            builder.trustStore(null);
            fail("NullPointerException expected for 'null' SSL context.");
        } catch (NullPointerException npe) {
            // pass
        }
    }

    @Test
    public void testOverridingSslConfig() throws KeyStoreException, NoSuchAlgorithmException {
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        SSLContext ctx = SSLContext.getInstance("SSL");
        Client client;

        client = new JerseyClientBuilder().keyStore(ks, "qwerty").sslContext(ctx).build();
        assertSame("SSL context not the same as set in the client builder.", ctx, client.getSslContext());

        client = new JerseyClientBuilder().sslContext(ctx).trustStore(ks).build();
        assertNotSame("SSL context not overridden in the client builder.", ctx, client.getSslContext());
    }

    @Priority(2)
    public static class AbortingClientFilter implements ClientRequestFilter {

        @Override
        public void filter(final ClientRequestContext requestContext) throws IOException {
            requestContext.abortWith(Response.ok("ok").build());
        }
    }

    @Priority(1)
    public static class ClientCreatingFilter implements ClientRequestFilter {

        @Override
        public void filter(final ClientRequestContext requestContext) throws IOException {
            if (Boolean.valueOf(requestContext.getHeaderString("create"))) {
                assertThat(requestContext.getProperty("foo").toString(), equalTo("rab"));

                final Client client = ClientBuilder.newBuilder().withConfig(requestContext.getConfiguration()).build();
                final Response response = client.target("http://localhost").request().header("create", false).get();

                requestContext.abortWith(response);
            } else {
                assertThat(requestContext.getConfiguration().getProperty("foo").toString(), equalTo("bar"));
            }
        }
    }

    public static class ClientFeature implements Feature {

        @Override
        public boolean configure(final FeatureContext context) {
            if (context.getConfiguration().isRegistered(AbortingClientFilter.class)) {
                throw new RuntimeException("Already Configured!");
            }

            context.register(ClientCreatingFilter.class);
            context.register(AbortingClientFilter.class);

            context.property("foo", "bar");

            return true;
        }
    }

    @Test
    public void testCreateClientWithConfigFromClient() throws Exception {
        _testCreateClientWithAnotherConfig(false);
    }

    @Test
    public void testCreateClientWithConfigFromRequestContext() throws Exception {
        _testCreateClientWithAnotherConfig(true);
    }

    public void _testCreateClientWithAnotherConfig(final boolean clientInFilter) throws Exception {
        final Client client = ClientBuilder.newBuilder().register(new ClientFeature()).build();
        Response response = client.target("http://localhost")
                .request().property("foo", "rab").header("create", clientInFilter).get();

        assertThat(response.getStatus(), equalTo(200));
        assertThat(response.readEntity(String.class), equalTo("ok"));

        final Client newClient = ClientBuilder.newClient(client.getConfiguration());
        response = newClient.target("http://localhost")
                .request().property("foo", "rab").header("create", clientInFilter).get();

        assertThat(response.getStatus(), equalTo(200));
        assertThat(response.readEntity(String.class), equalTo("ok"));

        final Client newClientFromBuilder = ClientBuilder.newBuilder().withConfig(client.getConfiguration()).build();
        response = newClientFromBuilder.target("http://localhost")
                .request().property("foo", "rab").header("create", clientInFilter).get();

        assertThat(response.getStatus(), equalTo(200));
        assertThat(response.readEntity(String.class), equalTo("ok"));
    }
}
