/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.e2e;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import javax.annotation.Priority;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Tests gzip interceptors.
 *
 * @author Miroslav Fuksa
 *
 */
public class InterceptorGzipTest extends JerseyTest {

    private static final String FROM_RESOURCE = "-from_resource";

    @Override
    protected ResourceConfig configure() {
        return new ResourceConfig(TestResource.class, GZIPWriterTestInterceptor.class,
                GZIPReaderTestInterceptor.class); // GZIPInterceptor.class
    }

    @Test
    public void testGzipInterceptorOnlyOnServer() throws IOException {
        client().register(GZIPWriterTestInterceptor.class);
        WebTarget target = target().path("test");
        String entity = "hello, this is text entity";
        Response response = target.request().put(Entity.entity(entity, MediaType.TEXT_PLAIN_TYPE));
        InputStream is = response.readEntity(InputStream.class);
        GZIPInputStream gzipIs = new GZIPInputStream(is);
        BufferedReader br = new BufferedReader(new InputStreamReader(gzipIs));
        String str = br.readLine();
        assertEquals(entity + FROM_RESOURCE, str);
    }

    @Test
    public void testGzipInterceptorOnServerandClient() throws IOException {
        client().register(GZIPReaderTestInterceptor.class).register(GZIPWriterTestInterceptor.class);
        WebTarget target = target().path("test");
        String entity = "hello, this is text entity";
        Response response = target.request().put(Entity.entity(entity, MediaType.TEXT_PLAIN_TYPE));
        String str = response.readEntity(String.class);
        assertEquals(entity + FROM_RESOURCE, str);
    }

    @Path("test")
    public static class TestResource {

        @PUT
        @Produces("text/plain")
        @Consumes("text/plain")
        public String put(String str) {
            System.out.println("resource: " + str);
            return str + FROM_RESOURCE;
        }
    }

    @Provider
    @Priority(200)
    public static class CustomWriterInterceptor implements WriterInterceptor {

        @Override
        public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
            final OutputStream old = context.getOutputStream();
            OutputStream newOut = new OutputStream() {

                @Override
                public void write(int b) throws IOException {
                    if (b == Byte.MAX_VALUE) {
                        old.write(Byte.MIN_VALUE);
                    } else {
                        old.write(b + 1);
                    }
                }
            };
            context.setOutputStream(newOut);
            try {
                context.proceed();
            } finally {
                context.setOutputStream(old);
            }
        }
    }

    @Provider
    @Priority(200)
    public static class GZIPWriterTestInterceptor implements WriterInterceptor {

        @Override
        public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
            OutputStream old = context.getOutputStream();
            GZIPOutputStream newStream = new GZIPOutputStream(old);
            context.setOutputStream(newStream);
            if (context.getHeaders().containsKey(HttpHeaders.CONTENT_LENGTH)) {
                List<Object> clen = new ArrayList<>();
                clen.add(-1L);
                context.getHeaders().put(HttpHeaders.CONTENT_LENGTH, clen);
            }
            try {
                context.proceed();
            } finally {
                newStream.finish();
                context.setOutputStream(old);
            }
        }
    }

    @Provider
    @Priority(200)
    public static class GZIPReaderTestInterceptor implements ReaderInterceptor {

        @Override
        public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException, WebApplicationException {
            InputStream old = context.getInputStream();
            context.setInputStream(new GZIPInputStream(old));
            try {
                return context.proceed();
            } finally {
                context.setInputStream(old);
            }
        }

    }
}
