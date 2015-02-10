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

package org.glassfish.jersey.tests.e2e.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import javax.annotation.Priority;

import org.glassfish.jersey.CommonProperties;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.inmemory.InMemoryTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Test determining of the content length.
 *
 * @author Miroslav Fuksa
 */
public class DetermineContentLengthTest extends JerseyTest {

    public static final int BUFFER_SIZE = 20;

    public static final String STR0 = "";
    public static final String STR6 = "123456";
    public static final String STR12 = "123456789ABC";

    @Path("root")
    public static class Resource {

        @GET
        @Path("6")
        @Produces("text/plain")
        public String get6() {
            return STR6;
        }


        @GET
        @Path("0")
        @Produces("text/plain")
        public String get0() {
            return STR0;
        }


        @GET
        @Produces("text/plain")
        @Path("12")
        public String get5() {
            return STR12;
        }

        @Path("test")
        @POST
        @Consumes("text/plain")
        @Produces("text/plain")
        public String testLength(String entity, @DefaultValue("-1") @HeaderParam(HttpHeaders.CONTENT_LENGTH) String length) {
            return length;
        }
    }

    @Priority(300)
    public static class DoubleInterceptor implements WriterInterceptor, ReaderInterceptor {

        @Override
        public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
            final OutputStream old = context.getOutputStream();
            context.setOutputStream(new OutputStream() {

                @Override
                public void write(int b) throws IOException {
                    old.write(b);
                    old.write(b);
                }
            });
            context.proceed();
            context.setOutputStream(old);
        }


        @Override
        public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException, WebApplicationException {
            final InputStream old = context.getInputStream();
            try {
                context.setInputStream(new InputStream() {

                    @Override
                    public int read() throws IOException {
                        old.read();
                        return old.read();
                    }
                });

                return context.proceed();
            } finally {
                context.setInputStream(old);
            }

        }
    }

    @Override
    protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
        return new InMemoryTestContainerFactory();
    }

    @Override
    protected Application configure() {
        final ResourceConfig resourceConfig = new ResourceConfig(Resource.class, DoubleInterceptor.class);
        resourceConfig.property(CommonProperties.OUTBOUND_CONTENT_LENGTH_BUFFER, BUFFER_SIZE);
        return resourceConfig;
    }

    @Override
    protected void configureClient(ClientConfig config) {
        config.property(CommonProperties.OUTBOUND_CONTENT_LENGTH_BUFFER, BUFFER_SIZE);
        config.register(DoubleInterceptor.class);
    }

    @Test
    public void test0() {
        final Response response = target().path("root/0").request().get();
        assertEquals(200, response.getStatus());
        final Object headerContentLength = Integer.valueOf(response.getHeaderString(HttpHeaders.CONTENT_LENGTH));
        assertEquals(STR0.length() * 2, headerContentLength);
        assertEquals(STR0, response.readEntity(String.class));
    }

    @Test
    public void testHead0() {
        final Response response = target().path("root/0").request().head();
        assertEquals(200, response.getStatus());
        final Object headerContentLength = Integer.valueOf(response.getHeaderString(HttpHeaders.CONTENT_LENGTH));
        assertEquals(STR0.length() * 2, headerContentLength);
    }

    @Test
    public void test6() {
        final Response response = target().path("root/6").request().get();
        assertEquals(200, response.getStatus());
        final Object headerContentLength = Integer.valueOf(response.getHeaderString(HttpHeaders.CONTENT_LENGTH));
        assertEquals(STR6.length() * 2, headerContentLength);
        assertEquals(STR6, response.readEntity(String.class));
    }

    @Test
    public void testHead6() {
        final Response response = target().path("root/6").request().head();
        assertEquals(200, response.getStatus());
        final Object headerContentLength = Integer.valueOf(response.getHeaderString(HttpHeaders.CONTENT_LENGTH));
        assertEquals(STR6.length() * 2, headerContentLength);
    }

    @Test
    public void test12() {
        final Response response = target().path("root/12").request().get();
        assertEquals(200, response.getStatus());
        checkEmptyContentLength(response);
        assertEquals(STR12, response.readEntity(String.class));
    }

    @Test
    public void testHead12() {
        final Response response = target().path("root/12").request().head();
        assertEquals(200, response.getStatus());
        checkEmptyContentLength(response);
    }

    private void checkEmptyContentLength(Response response) {
        final String headerString = response.getHeaderString(HttpHeaders.CONTENT_LENGTH);
        if (headerString != null) {
            final Object headerContentLength = Integer.valueOf(headerString);
            assertEquals(-1, headerContentLength);
        }
    }

    @Test
    public void testClientLength0() {
        final Response response = target().path("root/test").request().post(Entity.entity(STR0, MediaType.TEXT_PLAIN_TYPE));
        assertEquals(200, response.getStatus());
        assertEquals(STR0.length() * 2, response.readEntity(Integer.class).intValue());
    }

    @Test
    public void testClientLength6() {
        final Response response = target().path("root/test").request().post(Entity.entity(STR6, MediaType.TEXT_PLAIN_TYPE));
        assertEquals(200, response.getStatus());
        assertEquals(STR6.length() * 2, response.readEntity(Integer.class).intValue());
    }

    @Test
    public void testClientLength12() {
        final Response response = target().path("root/test").request().post(Entity.entity(STR12, MediaType.TEXT_PLAIN_TYPE));
        assertEquals(200, response.getStatus());
        assertEquals(-1, response.readEntity(Integer.class).intValue());
    }
}
