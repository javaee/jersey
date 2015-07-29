/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.test.util.client;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Basic {@link org.glassfish.jersey.test.util.client.LoopBackConnector} unit tests.
 *
 * @author Michal Gajdos
 */
public class LoopBackConnectorTest {

    private Client client;

    @Before
    public void setUp() throws Exception {
        client = ClientBuilder.newClient(LoopBackConnectorProvider.getClientConfig());
    }

    @After
    public void tearDown() throws Exception {
        if (client != null) {
            client.close();
        }
    }

    @Test
    public void testHeadersAndStatus() throws Exception {
        final Response response = client.target("baz").request()
                .header("foo", "bar")
                .header("bar", "foo")
                .get();

        assertThat("Unexpected HTTP response status", response.getStatus(), is(LoopBackConnector.TEST_LOOPBACK_CODE));
        assertThat("Invalid value of header 'foo'", response.getHeaderString("foo"), is("bar"));
        assertThat("Invalid value of header 'bar'", response.getHeaderString("bar"), is("foo"));
    }

    @Test
    public void testEntity() throws Exception {
        final Response response = client.target("baz").request().post(Entity.text("foo"));

        assertThat("Invalid entity received", response.readEntity(String.class), is("foo"));
    }

    @Test
    public void testEntityMediaType() throws Exception {
        final Response response = client.target("baz").request().post(Entity.entity("foo", "foo/bar"));

        assertThat("Invalid entity received", response.readEntity(String.class), is("foo"));
        assertThat("Invalid content-type received", response.getMediaType(), is(new MediaType("foo", "bar")));
    }

    @Test(expected = IllegalStateException.class)
    public void testClose() throws Exception {
        client.close();
        client.target("baz").request().get();
    }

    @Test
    public void testAsync() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Throwable> throwable = new AtomicReference<>();

        client.target("baz").request().async().get(new InvocationCallback<Response>() {
            @Override
            public void completed(final Response response) {
                latch.countDown();
            }

            @Override
            public void failed(final Throwable t) {
                throwable.set(t);
                latch.countDown();
            }
        });

        latch.await();

        assertThat("Async request failed", throwable.get(), nullValue());
    }

    @Test(expected = ProcessingException.class)
    public void testInvalidEntity() throws Exception {
        client.target("baz").request().post(Entity.json(Arrays.asList("foo", "bar")));
    }
}
