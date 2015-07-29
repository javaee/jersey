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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.process.JerseyProcessingUncaughtExceptionHandler;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;

import jersey.repackaged.com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * @author Michal Gajdos
 */
public class RxTest {

    private ExecutorService executor;

    @Before
    public void setUp() throws Exception {
        executor = new ScheduledThreadPoolExecutor(1, new ThreadFactoryBuilder()
                .setNameFormat("jersey-rx-client-test-%d")
                .setUncaughtExceptionHandler(new JerseyProcessingUncaughtExceptionHandler())
                .build());
    }

    @After
    public void tearDown() throws Exception {
        executor.shutdown();
    }

    @Test
    public void testNewClient() throws Exception {
        final RxWebTarget<RxFutureInvoker> target = Rx.newClient(RxFutureInvoker.class)
                .target("http://jersey.java.net");

        testTarget(target, false);
    }

    @Test
    public void testNewClientExecutor() throws Exception {
        final RxWebTarget<RxFutureInvoker> target = Rx.newClient(RxFutureInvoker.class, executor)
                .target("http://jersey.java.net");

        testTarget(target, true);
    }

    @Test
    public void testFromClient() throws Exception {
        final Client client = ClientBuilder.newClient();
        final RxWebTarget<RxFutureInvoker> target = Rx.from(client, RxFutureInvoker.class)
                .target("http://jersey.java.net");

        testTarget(target, false);
    }

    @Test
    public void testFromClientExecutor() throws Exception {
        final Client client = ClientBuilder.newClient();
        final RxWebTarget<RxFutureInvoker> target = Rx.from(client, RxFutureInvoker.class, executor)
                .target("http://jersey.java.net");

        testTarget(target, true);
    }

    @Test
    public void testFromTarget() throws Exception {
        final WebTarget target = ClientBuilder.newClient().target("http://jersey.java.net");
        final RxWebTarget<RxFutureInvoker> rxTarget = Rx.from(target, RxFutureInvoker.class);

        testTarget(rxTarget, false);
    }

    @Test
    public void testFromTargetExecutor() throws Exception {
        final WebTarget target = ClientBuilder.newClient().target("http://jersey.java.net");
        final RxWebTarget<RxFutureInvoker> rxTarget = Rx.from(target, RxFutureInvoker.class, executor);

        testTarget(rxTarget, true);
    }

    private static void testTarget(final RxWebTarget<RxFutureInvoker> target,
                                   final boolean testDedicatedThread) throws Exception {
        // Sync.
        testResponse(target.request().get(), false);

        // Async.
        testResponse(target.request().async().get().get(), false);

        // Rx.
        testResponse(target.request().rx().get().get(), testDedicatedThread);
    }

    private static void testResponse(final Response response, final boolean testDedicatedThread) {
        assertThat(response.getStatus(), is(200));
        assertThat(response.readEntity(String.class), is("NO-ENTITY"));

        // Executor.
        final Matcher<String> matcher = containsString("jersey-rx-client-test");
        assertThat(response.getHeaderString("Test-Thread"), testDedicatedThread ? matcher : not(matcher));

        // Properties.
        assertThat(response.getHeaderString("Test-Uri"), is("http://jersey.java.net"));
        assertThat(response.getHeaderString("Test-Method"), is("GET"));
    }
}
