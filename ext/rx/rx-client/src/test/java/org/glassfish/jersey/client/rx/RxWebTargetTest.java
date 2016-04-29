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

import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.MediaType;

import javax.annotation.Priority;

import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.client.JerseyWebTarget;
import org.glassfish.jersey.logging.LoggingFeature;

import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Michal Gajdos
 */
public class RxWebTargetTest {

    private JerseyWebTarget target;
    private RxWebTarget<RxFutureInvoker> rxTarget;

    @Before
    public void setUp() throws Exception {
        target = JerseyClientBuilder.createClient().target("http://jersey.java.net");
        rxTarget = Rx.from(target, RxFutureInvoker.class);
    }

    @After
    public void tearDown() throws Exception {
        target = null;
        rxTarget = null;
    }

    @Test
    public void testGetUri() throws Exception {
        assertThat(rxTarget.getUri(), is(target.getUri()));
    }

    @Test
    public void testGetUriBuilder() throws Exception {
        assertThat(rxTarget.getUriBuilder().build(), is(target.getUriBuilder().build()));
    }

    @Test
    public void testPath() throws Exception {
        final RxWebTarget<RxFutureInvoker> webTarget = rxTarget.path("documentation");

        assertThat(webTarget.getUri(), is(URI.create("http://jersey.java.net/documentation")));
        assertThat(webTarget.getUri(), is(not(rxTarget.getUri())));
    }

    @Test
    public void testResolveTemplate() throws Exception {
        final RxWebTarget<RxFutureInvoker> webTarget = rxTarget.path("{foo}").resolveTemplate("foo", "documentation");

        assertThat(webTarget.getUri(), is(URI.create("http://jersey.java.net/documentation")));
        assertThat(webTarget.getUri(), is(not(rxTarget.getUri())));
    }

    @Test
    public void testResolveTemplateSlash() throws Exception {
        final RxWebTarget<RxFutureInvoker> webTarget = rxTarget.path("{foo}");

        RxWebTarget<RxFutureInvoker> resolved = webTarget.resolveTemplate("foo", "documentation/latest", true);
        assertThat(resolved.getUri(), is(URI.create("http://jersey.java.net/documentation%2Flatest")));
        assertThat(resolved.getUri(), is(not(rxTarget.getUri())));

        resolved = webTarget.resolveTemplate("foo", "documentation/latest", false);
        assertThat(resolved.getUri(), is(URI.create("http://jersey.java.net/documentation/latest")));
        assertThat(resolved.getUri(), is(not(rxTarget.getUri())));
    }

    @Test
    public void testResolveTemplateFromEncoded() throws Exception {
        final RxWebTarget<RxFutureInvoker> webTarget = rxTarget.path("{foo}");
        final RxWebTarget<RxFutureInvoker> resolved = webTarget.resolveTemplateFromEncoded("foo", "documentation%2Flatest");

        assertThat(resolved.getUri(), is(URI.create("http://jersey.java.net/documentation%2Flatest")));
        assertThat(resolved.getUri(), is(not(rxTarget.getUri())));
    }

    @Test
    public void testResolveTemplateValues() throws Exception {
        final RxWebTarget<RxFutureInvoker> webTarget = rxTarget.path("{foo}/{bar}");
        final RxWebTarget<RxFutureInvoker> resolved = webTarget.resolveTemplates(new HashMap<String, Object>() {{
            put("foo", "documentation");
            put("bar", "latest");
        }});

        assertThat(resolved.getUri(), is(URI.create("http://jersey.java.net/documentation/latest")));
        assertThat(resolved.getUri(), is(not(rxTarget.getUri())));
    }

    @Test
    public void testResolveTemplateValuesSlash() throws Exception {
        final RxWebTarget<RxFutureInvoker> webTarget = rxTarget.path("{foo}/{bar}");
        final RxWebTarget<RxFutureInvoker> resolved = webTarget.resolveTemplates(new HashMap<String, Object>() {{
            put("foo", "documentation");
            put("bar", "latest/index.html");
        }}, true);

        assertThat(resolved.getUri(), is(URI.create("http://jersey.java.net/documentation/latest%2Findex.html")));
        assertThat(resolved.getUri(), is(not(rxTarget.getUri())));
    }

    @Test
    public void testResolveTemplateValuesFromEncoded() throws Exception {
        final RxWebTarget<RxFutureInvoker> webTarget = rxTarget.path("{foo}/{bar}");
        final RxWebTarget<RxFutureInvoker> resolved = webTarget.resolveTemplatesFromEncoded(new HashMap<String, Object>() {{
            put("foo", "documentation");
            put("bar", "latest%2Findex.html");
        }});

        assertThat(resolved.getUri(), is(URI.create("http://jersey.java.net/documentation/latest%2Findex.html")));
        assertThat(resolved.getUri(), is(not(rxTarget.getUri())));
    }

    @Test
    public void testMatrixParam() throws Exception {
        final RxWebTarget<RxFutureInvoker> webTarget = rxTarget.matrixParam("foo", "bar");

        assertThat(webTarget.getUri(), is(URI.create("http://jersey.java.net/;foo=bar")));
        assertThat(webTarget.getUri(), is(not(rxTarget.getUri())));
    }

    @Test
    public void testQueryParam() throws Exception {
        final RxWebTarget<RxFutureInvoker> webTarget = rxTarget.queryParam("foo", "bar");

        assertThat(webTarget.getUri(), is(URI.create("http://jersey.java.net/?foo=bar")));
        assertThat(webTarget.getUri(), is(not(rxTarget.getUri())));
    }

    @Test
    public void testRequest() throws Exception {
        final RxInvocationBuilder<RxFutureInvoker> request = rxTarget.request();
        assertThat(request, notNullValue());
    }

    @Test
    public void testRequestStringMediaTypes() throws Exception {
        final RxInvocationBuilder<RxFutureInvoker> request = rxTarget.request("foo/bar", "bar/foo");
        assertThat(request.get().getHeaderString("Test-Header-Accept"), is("[foo/bar, bar/foo]"));
    }

    @Test
    public void testRequestMediaTypes() throws Exception {
        final RxInvocationBuilder<RxFutureInvoker> request = rxTarget.request(MediaType.APPLICATION_JSON_TYPE,
                MediaType.TEXT_PLAIN_TYPE);
        assertThat(request.get().getHeaderString("Test-Header-Accept"), is("[application/json, text/plain]"));
    }

    @Test
    public void testConfiguration() throws Exception {
        assertThat(rxTarget.getConfiguration(), CoreMatchers.<Configuration>is(target.getConfiguration()));
    }

    @Test
    public void testProperty() throws Exception {
        final RxWebTarget<RxFutureInvoker> webTarget = rxTarget.property("foo", "bar");
        assertThat(webTarget.getConfiguration().getProperty("foo"), CoreMatchers.<Object>is("bar"));
    }

    @Test
    public void testRegisterClass() throws Exception {
        final RxWebTarget<RxFutureInvoker> webTarget = rxTarget.register(MyComponent.class);
        assertThat(webTarget.getConfiguration().isRegistered(MyComponent.class), is(true));
    }

    @Test
    public void testRegisterClassPriority() throws Exception {
        final RxWebTarget<RxFutureInvoker> webTarget = rxTarget.register(MyComponent.class, 42);

        for (final Map.Entry<Class<?>, Integer> entry : webTarget.getConfiguration().getContracts(MyComponent.class)
                .entrySet()) {
            assertThat(entry.getKey().isAssignableFrom(MyComponent.class), is(true));
            assertThat(entry.getValue(), is(42));
        }
    }

    @Ignore
    @Test
    public void testRegisterClassContracts() throws Exception {
        final RxWebTarget<RxFutureInvoker> webTarget = rxTarget
                .register(MyComponent.class, FirstContract.class, SecondContract.class);

        final Map<Class<?>, Integer> contracts = webTarget.getConfiguration().getContracts(MyComponent.class);

        assertThat(contracts.size(), is(2));
        assertThat(contracts.keySet(), hasItems(FirstContract.class, SecondContract.class));
    }

    @Test
    public void testRegisterClassContractsPriorities() throws Exception {
        final Map<Class<?>, Integer> contracts = new HashMap<>();
        contracts.put(FirstContract.class, 42);
        contracts.put(SecondContract.class, 23);

        final RxWebTarget<RxFutureInvoker> webTarget = rxTarget.register(MyComponent.class, contracts);

        assertThat(webTarget.getConfiguration().getContracts(MyComponent.class), is(contracts));
    }

    @Test
    public void testRegisterObject() throws Exception {
        final RxWebTarget<RxFutureInvoker> webTarget = rxTarget.register(LoggingFeature.class);
        assertThat(webTarget.getConfiguration().isRegistered(LoggingFeature.class), is(true));
    }

    @Test
    public void testRegisterObjectPriority() throws Exception {
        final RxWebTarget<RxFutureInvoker> webTarget = rxTarget.register(MyComponent.class, 42);

        for (final Map.Entry<Class<?>, Integer> entry : webTarget.getConfiguration().getContracts(MyComponent.class)
                .entrySet()) {
            assertThat(entry.getKey().isAssignableFrom(MyComponent.class), is(true));
            assertThat(entry.getValue(), is(42));
        }
    }

    @Ignore
    @Test
    public void testRegisterObjectContracts() throws Exception {
        final RxWebTarget<RxFutureInvoker> webTarget = rxTarget
                .register(MyComponent.class, FirstContract.class, SecondContract.class);

        final Map<Class<?>, Integer> contracts = webTarget.getConfiguration().getContracts(MyComponent.class);

        assertThat(contracts.size(), is(2));
        assertThat(contracts.keySet(), hasItems(FirstContract.class, SecondContract.class));
    }

    @Test
    public void testRegisterObjectContractsPriorities() throws Exception {
        final Map<Class<?>, Integer> contracts = new HashMap<>();
        contracts.put(FirstContract.class, 42);
        contracts.put(SecondContract.class, 23);

        final RxWebTarget<RxFutureInvoker> webTarget = rxTarget.register(MyComponent.class, contracts);

        assertThat(webTarget.getConfiguration().getContracts(MyComponent.class), is(contracts));
    }


    private static class MyComponent implements Feature, FirstContract, SecondContract {

        @Override
        public boolean configure(final FeatureContext context) {
            return false;
        }
    }

    private interface FirstContract {}

    private interface SecondContract {}

}
