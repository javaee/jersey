/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2016 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.client.rx;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.client.JerseyClient;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.logging.LoggingFeature;

import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Michal Gajdos
 */
public class RxClientTest {

    private JerseyClient client;
    private RxClient<RxFutureInvoker> rxClient;

    @Before
    public void setUp() throws Exception {
        client = JerseyClientBuilder.createClient();
        rxClient = Rx.from(client, RxFutureInvoker.class);
    }

    @After
    public void tearDown() throws Exception {
        client = null;
        rxClient = null;
    }

    @Test
    public void testClose() throws Exception {
        assertThat(client.isClosed(), is(false));
        rxClient.close();
        assertThat(client.isClosed(), is(true));
    }

    @Test
    public void testTargetString() throws Exception {
        testTarget(rxClient.target("http://jersey.java.net"));
    }

    @Test
    public void testTargetUri() throws Exception {
        testTarget(rxClient.target(URI.create("http://jersey.java.net")));
    }

    @Test
    public void testTargetUriBuilder() throws Exception {
        testTarget(rxClient.target(UriBuilder.fromUri("http://jersey.java.net")));
    }

    @Test
    public void testTargetLink() throws Exception {
        testTarget(rxClient.target(Link.fromUri("http://jersey.java.net").build()));
    }

    private void testTarget(final RxWebTarget<RxFutureInvoker> target) {
        assertThat(target.getUri(), is(URI.create("http://jersey.java.net")));
    }

    @Test
    public void testInvocation() throws Exception {
        final RxInvocationBuilder<RxFutureInvoker> invocation = rxClient.invocation(Link.fromUri("http://jersey.java.net")
                .build());

        assertThat(invocation.get().getHeaderString("Test-Uri"), is("http://jersey.java.net"));
    }

    @Test
    public void testSslContext() throws Exception {
        assertThat(rxClient.getSslContext(), is(client.getSslContext()));
    }

    @Test
    public void testHostnameVerifier() throws Exception {
        assertThat(rxClient.getHostnameVerifier(), is(client.getHostnameVerifier()));
    }

    @Test
    public void testConfiguration() throws Exception {
        assertThat(rxClient.getConfiguration(), CoreMatchers.<Configuration>is(client.getConfiguration()));
    }

    @Test
    public void testProperty() throws Exception {
        final RxClient<RxFutureInvoker> updated = rxClient.property("foo", "bar");
        assertThat(updated.getConfiguration().getProperty("foo"), CoreMatchers.<Object>is("bar"));
    }

    @Test
    public void testRegisterClass() throws Exception {
        final RxClient<RxFutureInvoker> updated = rxClient.register(LoggingFeature.class);
        assertThat(updated.getConfiguration().isRegistered(LoggingFeature.class), is(true));
    }

    @Test
    public void testRegisterClassPriority() throws Exception {
        final RxClient<RxFutureInvoker> updated = rxClient.register(LoggingFeature.class, 42);

        for (final Map.Entry<Class<?>,
                Integer> entry : updated.getConfiguration().getContracts(LoggingFeature.class).entrySet()) {
            assertThat(entry.getKey().isAssignableFrom(LoggingFeature.class), is(true));
            assertThat(entry.getValue(), is(42));
        }
    }

    @Ignore
    @Test
    public void testRegisterClassContracts() throws Exception {
        final RxClient<RxFutureInvoker> updated = rxClient
                .register(MyComponent.class, FirstContract.class, SecondContract.class);

        final Map<Class<?>, Integer> contracts = updated.getConfiguration().getContracts(MyComponent.class);

        assertThat(contracts.size(), is(2));
        assertThat(contracts.keySet(), hasItems(FirstContract.class, SecondContract.class));
    }

    @Test
    public void testRegisterClassContractsPriorities() throws Exception {
        final Map<Class<?>, Integer> contracts = new HashMap<>();
        contracts.put(FirstContract.class, 42);
        contracts.put(SecondContract.class, 23);

        final RxClient<RxFutureInvoker> updated = rxClient.register(MyComponent.class, contracts);

        assertThat(updated.getConfiguration().getContracts(MyComponent.class), is(contracts));
    }

    @Test
    public void testRegisterObject() throws Exception {
        final RxClient<RxFutureInvoker> updated = rxClient.register(LoggingFeature.class);
        assertThat(updated.getConfiguration().isRegistered(LoggingFeature.class), is(true));
    }

    @Test
    public void testRegisterObjectPriority() throws Exception {
        final RxClient<RxFutureInvoker> updated = rxClient.register(MyComponent.class, 42);

        for (final Map.Entry<Class<?>,
                Integer> entry : updated.getConfiguration().getContracts(MyComponent.class).entrySet()) {
            assertThat(entry.getKey().isAssignableFrom(MyComponent.class), is(true));
            assertThat(entry.getValue(), is(42));
        }
    }

    @Ignore
    @Test
    public void testRegisterObjectContracts() throws Exception {
        final RxClient<RxFutureInvoker> updated = rxClient
                .register(MyComponent.class, FirstContract.class, SecondContract.class);

        final Map<Class<?>, Integer> contracts = updated.getConfiguration().getContracts(MyComponent.class);

        assertThat(contracts.size(), is(2));
        assertThat(contracts.keySet(), hasItems(FirstContract.class, SecondContract.class));
    }

    @Test
    public void testRegisterObjectContractsPriorities() throws Exception {
        final Map<Class<?>, Integer> contracts = new HashMap<>();
        contracts.put(FirstContract.class, 42);
        contracts.put(SecondContract.class, 23);

        final RxClient<RxFutureInvoker> updated = rxClient.register(MyComponent.class, contracts);

        assertThat(updated.getConfiguration().getContracts(MyComponent.class), is(contracts));
    }

    private static class MyComponent implements FirstContract, SecondContract {

    }

    private interface FirstContract {}

    private interface SecondContract {}
}
