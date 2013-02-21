/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Martin Matula (martin.matula at oracle.com)
 */
public class JerseyInvocationTest {
    /**
     * Regression test for JERSEY-1257
     */
    @Test
    public void testOverrideHeadersWithMap() {
        MultivaluedMap<String, Object> map = new MultivaluedHashMap<String, Object>();
        map.add("a-header", "b-header");
        JerseyInvocation invocation = buildInvocationWithHeaders(map);
        assertEquals(1, invocation.request().getHeaders().size());
        assertEquals("b-header", invocation.request().getHeaders().getFirst("a-header"));
    }

    /**
     * Regression test for JERSEY-1257
     */
    @Test
    public void testClearHeaders() {
        JerseyInvocation invocation = buildInvocationWithHeaders(null);
        assertTrue(invocation.request().getHeaders().isEmpty());
    }

    private JerseyInvocation buildInvocationWithHeaders(MultivaluedMap<String, Object> headers) {
        Client c = ClientBuilder.newClient();
        Invocation.Builder builder = c.target("http://localhost:8080/mypath").request();
        return (JerseyInvocation) builder
                .header("unexpected-header", "unexpected-header").headers(headers)
                .buildGet();
    }

    @Test
    public void testNullResponseType() throws Exception {
        final Client client = ClientBuilder.newClient();
        client.register(new ClientRequestFilter() {
            @Override
            public void filter(final ClientRequestContext requestContext) throws IOException {
                requestContext.abortWith(Response.ok().build());
            }
        });

        final WebTarget target = client.target("http://localhost:8080/mypath");

        final Class<Response> responseType = null;
        final String[] methods = new String[] {"GET", "PUT", "POST", "DELETE", "OPTIONS"};

        for (final String method : methods) {
            final Invocation.Builder request = target.request();

            try {
                request.method(method, responseType);
                fail("IllegalArgumentException expected.");
            } catch (IllegalArgumentException iae) {
                // OK.
            }

            final Invocation build = "PUT".equals(method) ?
                    request.build(method, Entity.entity("", MediaType.TEXT_PLAIN_TYPE)) : request.build(method);

            try {
                build.submit(responseType);
                fail("IllegalArgumentException expected.");
            } catch (IllegalArgumentException iae) {
                // OK.
            }

            try {
                build.invoke(responseType);
                fail("IllegalArgumentException expected.");
            } catch (IllegalArgumentException iae) {
                // OK.
            }

            try {
                request.async().method(method, responseType);
                fail("IllegalArgumentException expected.");
            } catch (IllegalArgumentException iae) {
                // OK.
            }
        }
    }

    @Test
    public void failedCallbackTest() {
        final Invocation.Builder builder = ClientBuilder.newClient().target("http://localhost:888/").request();
        for (int i = 0; i < 1; i++) {
            final AtomicInteger ai = new AtomicInteger(0);
            InvocationCallback<String> callback = new InvocationCallback<String>() {
                @Override
                public void completed(String arg0) {
                    ai.set(ai.get() + 1);
                }

                @Override
                public void failed(Throwable throwable) {
                    int result = 10;
                    if (throwable instanceof ProcessingException) {
                        result += 100;
                    }
                    final Throwable ioe = throwable.getCause();
                    if (ioe instanceof IOException) {
                        result += 1000;
                    }
                    ai.set(ai.get() + result);
                }
            };

            Invocation invocation = builder.buildGet();
            Future<String> future = invocation.submit(callback);
            try {
                future.get();
                fail("future.get() should have failed.");
            } catch (ExecutionException e) {
                final Throwable pe = e.getCause();
                assertTrue("Execution exception cause is not a ProcessingException: " + pe.toString(),
                        pe instanceof ProcessingException);
                final Throwable ioe = pe.getCause();
                assertTrue("Execution exception cause is not an IOException: " + ioe.toString(),
                        ioe instanceof IOException);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            assertEquals(1110, ai.get());
        }
    }
}
