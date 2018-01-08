/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2017 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.tests.e2e.entity;

import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Reproducer for JERSEY-1845.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class InterceptedStreamCloseTest extends JerseyTest {
    @Path("resource")
    public static class TestResource {
        @POST
        public String post(String entity) {
            return entity;
        }
    }

    public static class ClientInterceptor implements ReaderInterceptor, WriterInterceptor {
        private volatile int readFromCount = 0;
        private volatile int inCloseCount = 0;
        private volatile int writeToCount = 0;
        private volatile int outCloseCount = 0;

        @Override
        public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException, WebApplicationException {
            readFromCount++;

            context.setInputStream(new FilterInputStream(context.getInputStream()) {
                @Override
                public void close() throws IOException {
                    inCloseCount++;
                    super.close();
                }
            });
            return context.proceed();
        }

        @Override
        public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
            writeToCount++;

            context.setOutputStream(new FilterOutputStream(context.getOutputStream()) {
                @Override
                public void close() throws IOException {
                    outCloseCount++;
                    super.close();
                }
            });

            context.proceed();
        }
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(TestResource.class);
    }

    @Test
    public void testWrappedStreamClosed() {
        ClientInterceptor interceptor = new ClientInterceptor();

        final Response response = target().register(interceptor)
                .path("resource").request().post(Entity.entity("entity", MediaType.TEXT_PLAIN_TYPE));
        assertEquals("entity", response.readEntity(String.class));

        assertEquals(1, interceptor.readFromCount);
        assertEquals(1, interceptor.inCloseCount);
        assertEquals(1, interceptor.writeToCount);
        assertEquals(1, interceptor.outCloseCount);
    }
}
