/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2015 Oracle and/or its affiliates. All rights reserved.
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

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.internal.util.collection.StringKeyIgnoreCaseMultivaluedMap;
import org.glassfish.jersey.process.JerseyProcessingUncaughtExceptionHandler;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import jersey.repackaged.com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * @author Michal Gajdos
 */
public class RxInvocationBuilderTest {

    private RxInvocationBuilder<RxFutureInvoker> rxBuilder;

    @Before
    public void setUp() throws Exception {
        rxBuilder = Rx.newClient(RxFutureInvoker.class).target("http://jersey.java.net").request();
    }

    @After
    public void tearDown() throws Exception {
        rxBuilder = null;
    }

    @Test
    public void testRx() throws Exception {
        assertThat(rxBuilder.rx(), notNullValue());
    }

    @Test
    public void testRxNegative() throws Exception {
        assertThat(Rx.newClient(RxInvoker.class).target("http://jersey.java.net").request().rx(), nullValue());
    }

    @Test
    public void testRxCustomExecutor() throws Exception {
        final ExecutorService executor = new ScheduledThreadPoolExecutor(1, new ThreadFactoryBuilder()
                .setNameFormat("jersey-rx-client-test-%d")
                .setUncaughtExceptionHandler(new JerseyProcessingUncaughtExceptionHandler())
                .build());

        assertThat(rxBuilder.rx(executor).get().get().getHeaderString("Test-Thread"), containsString("jersey-rx-client-test"));
    }

    @Test
    public void testAcceptString() throws Exception {
        final RxInvocationBuilder<RxFutureInvoker> builder = rxBuilder.accept("foo/bar");
        assertThat(builder.get().getHeaderString("Test-Header-Accept"), is("[foo/bar]"));
    }

    @Test
    public void testAcceptMediaType() throws Exception {
        final RxInvocationBuilder<RxFutureInvoker> builder = rxBuilder.accept(MediaType.APPLICATION_JSON_TYPE);
        assertThat(builder.get().getHeaderString("Test-Header-Accept"), is("[application/json]"));
    }

    @Test
    public void testAcceptLanguageString() throws Exception {
        final RxInvocationBuilder<RxFutureInvoker> builder = rxBuilder.acceptLanguage("en-US");
        assertThat(builder.get().getHeaderString("Test-Header-Accept-Language"), is("[en-US]"));
    }

    @Test
    public void testAcceptLanguageLocale() throws Exception {
        final RxInvocationBuilder<RxFutureInvoker> builder = rxBuilder.acceptLanguage(Locale.US);
        assertThat(builder.get().getHeaderString("Test-Header-Accept-Language"), is("[en-US]"));
    }

    @Test
    public void testAcceptEncoding() throws Exception {
        final RxInvocationBuilder<RxFutureInvoker> builder = rxBuilder.acceptLanguage("UTF-8");
        assertThat(builder.get().getHeaderString("Test-Header-Accept-Language"), is("[UTF-8]"));
    }

    @Test
    public void testCookie() throws Exception {
        final RxInvocationBuilder<RxFutureInvoker> builder = rxBuilder.cookie(new Cookie("foo", "bar"));
        assertThat(builder.get().getHeaderString("Test-Header-Cookie"), is("[$Version=1;foo=bar]"));
    }

    @Test
    public void testCookieString() throws Exception {
        final RxInvocationBuilder<RxFutureInvoker> builder = rxBuilder.cookie("foo", "bar");
        assertThat(builder.get().getHeaderString("Test-Header-Cookie"), is("[$Version=1;foo=bar]"));
    }

    @Test
    public void testCacheControl() throws Exception {
        final CacheControl cacheControl = new CacheControl();
        cacheControl.setNoCache(true);
        final RxInvocationBuilder<RxFutureInvoker> builder = rxBuilder.cacheControl(cacheControl);
        assertThat(builder.get().getHeaderString("Test-Header-Cache-Control"), is("[no-cache, no-transform]"));
    }

    @Test
    public void testHeader() throws Exception {
        final RxInvocationBuilder<RxFutureInvoker> builder = rxBuilder.header("foo", "bar");
        assertThat(builder.get().getHeaderString("Test-Header-Foo"), is("[bar]"));
    }

    @Test
    public void testHeaders() throws Exception {
        final RxInvocationBuilder<RxFutureInvoker> builder = rxBuilder.headers(new StringKeyIgnoreCaseMultivaluedMap<Object>() {{
            add("foo", "bar");
            add("foo", "baz");
        }});
        assertThat(builder.get().getHeaderString("Test-Header-Foo"), is("[bar, baz]"));
    }

    @Test
    public void testProperty() throws Exception {
        final RxInvocationBuilder<RxFutureInvoker> builder = rxBuilder.property("foo", "bar");
        assertThat(builder.get().getHeaderString("Test-Property-Foo"), is("bar"));
    }

    @Test
    public void testAsync() throws Exception {
        assertThat(rxBuilder.async(), notNullValue());
    }

    @Test
    public void testBuild() throws Exception {
        assertThat(rxBuilder.build("GET"), notNullValue());
    }

    @Test
    public void testBuildEntity() throws Exception {
        assertThat(rxBuilder.build("POST", Entity.json("")), notNullValue());
    }

    @Test
    public void testBuildDelete() throws Exception {
        assertThat(rxBuilder.buildDelete(), notNullValue());
    }

    @Test
    public void testBuildGet() throws Exception {
        assertThat(rxBuilder.buildGet(), notNullValue());
    }

    @Test
    public void testBuildPost() throws Exception {
        assertThat(rxBuilder.buildPost(Entity.json("")), notNullValue());
    }

    @Test
    public void testBuildPut() throws Exception {
        assertThat(rxBuilder.buildPut(Entity.json("")), notNullValue());
    }

    @Test
    public void testGet() throws Exception {
        assertThat(rxBuilder.get(), notNullValue());
    }

    @Test
    public void testGetType() throws Exception {
        assertThat(rxBuilder.get(Response.class), notNullValue());
    }

    @Test
    public void testGetGeneric() throws Exception {
        assertThat(rxBuilder.get(new GenericType<Response>() {
        }), notNullValue());
    }

    @Test
    public void testPut() throws Exception {
        assertThat(rxBuilder.put(Entity.json("")), notNullValue());
    }

    @Test
    public void testPutType() throws Exception {
        assertThat(rxBuilder.put(Entity.json(""), Response.class), notNullValue());
    }

    @Test
    public void testPutGeneric() throws Exception {
        assertThat(rxBuilder.put(Entity.json(""), new GenericType<Response>() {
        }), notNullValue());
    }

    @Test
    public void testPost() throws Exception {
        assertThat(rxBuilder.post(Entity.json("")), notNullValue());
    }

    @Test
    public void testPostType() throws Exception {
        assertThat(rxBuilder.post(Entity.json(""), Response.class), notNullValue());
    }

    @Test
    public void testPostGeneric() throws Exception {
        assertThat(rxBuilder.post(Entity.json(""), new GenericType<Response>() {
        }), notNullValue());
    }

    @Test
    public void testDelete() throws Exception {
        assertThat(rxBuilder.delete(), notNullValue());
    }

    @Test
    public void testDeleteType() throws Exception {
        assertThat(rxBuilder.delete(Response.class), notNullValue());
    }

    @Test
    public void testDeleteGeneric() throws Exception {
        assertThat(rxBuilder.delete(new GenericType<Response>() {
        }), notNullValue());
    }

    @Test
    public void testOptions() throws Exception {
        assertThat(rxBuilder.options(), notNullValue());
    }

    @Test
    public void testOptionsType() throws Exception {
        assertThat(rxBuilder.options(Response.class), notNullValue());
    }

    @Test
    public void testOptionsGeneric() throws Exception {
        assertThat(rxBuilder.options(new GenericType<Response>() {
        }), notNullValue());
    }

    @Test
    public void testTrace() throws Exception {
        assertThat(rxBuilder.trace(), notNullValue());
    }

    @Test
    public void testTraceType() throws Exception {
        assertThat(rxBuilder.trace(Response.class), notNullValue());
    }

    @Test
    public void testTraceGeneric() throws Exception {
        assertThat(rxBuilder.trace(new GenericType<Response>() {
        }), notNullValue());
    }

    @Test
    public void testHead() throws Exception {
        assertThat(rxBuilder.head(), notNullValue());
    }

    @Test
    public void testMethod() throws Exception {
        assertThat(rxBuilder.method("GET"), notNullValue());
    }

    @Test
    public void testMethodType() throws Exception {
        assertThat(rxBuilder.method("GET", Response.class), notNullValue());
    }

    @Test
    public void testMethodGeneric() throws Exception {
        assertThat(rxBuilder.method("GET", new GenericType<Response>() {
        }), notNullValue());
    }

    @Test
    public void testMethodEntity() throws Exception {
        assertThat(rxBuilder.method("POST", Entity.json("")), notNullValue());
    }

    @Test
    public void testMethodEntityType() throws Exception {
        assertThat(rxBuilder.method("POST", Entity.json(""), Response.class), notNullValue());
    }

    @Test
    public void testMethodEntityGeneric() throws Exception {
        assertThat(rxBuilder.method("POST", Entity.json(""), new GenericType<Response>() {
        }), notNullValue());
    }
}
