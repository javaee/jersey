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

package org.glassfish.jersey.client.rx.spi;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.rx.Rx;
import org.glassfish.jersey.client.rx.RxFutureInvoker;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Michal Gajdos
 */
public class AbstractRxInvokerTest {

    private RxFutureInvoker rx;

    @Before
    public void setUp() throws Exception {
        rx = Rx.newClient(RxFutureInvoker.class).target("http://jersey.java.net").request().rx();
    }

    @After
    public void tearDown() throws Exception {
        rx = null;
    }

    @Test
    public void testGet() throws Exception {
        testResponse(rx.get().get(), "GET");
    }

    @Test
    public void testGetType() throws Exception {
        testResponse(rx.get(Response.class).get(), "GET");
    }

    @Test
    public void testGetGeneric() throws Exception {
        testResponse(rx.get(new GenericType<Response>() {
        }).get(), "GET");
    }

    @Test
    public void testPost() throws Exception {
        testResponse(rx.post(Entity.text("ENTITY")).get(), "POST");
    }

    @Test
    public void testPostType() throws Exception {
        testResponse(rx.post(Entity.text("ENTITY"), Response.class).get(), "POST");
    }

    @Test
    public void testPostGeneric() throws Exception {
        testResponse(rx.post(Entity.text("ENTITY"), new GenericType<Response>() {
        }).get(), "POST");
    }

    @Test
    public void testPut() throws Exception {
        testResponse(rx.put(Entity.text("ENTITY")).get(), "PUT");
    }

    @Test
    public void testPutType() throws Exception {
        testResponse(rx.put(Entity.text("ENTITY"), Response.class).get(), "PUT");
    }

    @Test
    public void testPutGeneric() throws Exception {
        testResponse(rx.put(Entity.text("ENTITY"), new GenericType<Response>() {
        }).get(), "PUT");
    }

    @Test
    public void testDelete() throws Exception {
        testResponse(rx.delete().get(), "DELETE");
    }

    @Test
    public void testDeleteType() throws Exception {
        testResponse(rx.delete(Response.class).get(), "DELETE");
    }

    @Test
    public void testDeleteGeneric() throws Exception {
        testResponse(rx.delete(new GenericType<Response>() {
        }).get(), "DELETE");
    }

    @Test
    public void testHead() throws Exception {
        testResponse(rx.head().get(), "HEAD");
    }

    @Test
    public void testOptions() throws Exception {
        testResponse(rx.options().get(), "OPTIONS");
    }

    @Test
    public void testOptionsType() throws Exception {
        testResponse(rx.options(Response.class).get(), "OPTIONS");
    }

    @Test
    public void testOptionsGeneric() throws Exception {
        testResponse(rx.options(new GenericType<Response>() {
        }).get(), "OPTIONS");
    }

    @Test
    public void testTrace() throws Exception {
        testResponse(rx.trace().get(), "TRACE");
    }

    @Test
    public void testTraceType() throws Exception {
        testResponse(rx.trace(Response.class).get(), "TRACE");
    }

    @Test
    public void testTraceGeneric() throws Exception {
        testResponse(rx.trace(new GenericType<Response>() {
        }).get(), "TRACE");
    }

    @Test
    public void testMethod() throws Exception {
        testResponse(rx.method("GET").get(), "GET");
    }

    @Test
    public void testMethodType() throws Exception {
        testResponse(rx.method("GET", Response.class).get(), "GET");
    }

    @Test
    public void testMethodGeneric() throws Exception {
        testResponse(rx.method("GET", new GenericType<Response>() {
        }).get(), "GET");
    }

    @Test
    public void testMethodEntity() throws Exception {
        testResponse(rx.method("PUT", Entity.text("ENTITY")).get(), "PUT");
    }

    @Test
    public void testMethodEntityType() throws Exception {
        testResponse(rx.method("PUT", Entity.text("ENTITY"), Response.class).get(), "PUT");
    }

    @Test
    public void testMethodEntityGeneric() throws Exception {
        testResponse(rx.method("PUT", Entity.text("ENTITY"), new GenericType<Response>() {
        }).get(), "PUT");
    }

    private void testResponse(final Response response, final String method) {
        assertThat(response.getHeaderString("Test-Uri"), is("http://jersey.java.net"));
        assertThat(response.getHeaderString("Test-Method"), is(method));

        final String entity = "PUT".equals(method) || "POST".equals(method) ? "ENTITY" : "NO-ENTITY";
        assertThat(response.readEntity(String.class), is(entity));
    }
}
