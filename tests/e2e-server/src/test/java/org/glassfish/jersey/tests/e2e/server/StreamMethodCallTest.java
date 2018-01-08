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

package org.glassfish.jersey.tests.e2e.server;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests that appropriate methods are called in the intercepted output stream.
 *
 * @author Miroslav Fuksa
 *
 */
public class StreamMethodCallTest extends JerseyTest {

    @Override
    protected Application configure() {
        return new ResourceConfig(TestWriterInterceptor.class, TestResource.class);
    }

    public static class TestWriterInterceptor implements WriterInterceptor {

        @Override
        public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
            final OutputStream outputStreamoldOs = context.getOutputStream();
            context.setOutputStream(new TestOutputStream(new GZIPOutputStream(outputStreamoldOs)));
            context.proceed();
        }
    }

    public static class TestOutputStream extends OutputStream {
        private final GZIPOutputStream gzos;
        public static boolean closeCalled = false;
        public static boolean flushCalledBeforeClose = false;
        public static boolean writeCalled = false;

        public TestOutputStream(GZIPOutputStream gzos) {
            this.gzos = gzos;
        }

        @Override
        public void write(int b) throws IOException {
            writeCalled = true;
            gzos.write(b);
        }

        @Override
        public void close() throws IOException {
            TestOutputStream.closeCalled = true;
            gzos.close();
        }

        @Override
        public void flush() throws IOException {
            if (!closeCalled) {
                flushCalledBeforeClose = true;
            }
            gzos.flush();
        }
    }

    @Path("resource")
    public static class TestResource {
        @GET
        public String get() {
            return "get";
        }
    }

    @Test
    public void testCalledMethods() {
        final Response response = target().path("resource").request().get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertTrue("close() has not been called.", TestOutputStream.closeCalled);
        Assert.assertTrue("flush() has not been called before close().", TestOutputStream.flushCalledBeforeClose);
        Assert.assertTrue("write() has not been called.", TestOutputStream.writeCalled);
    }
}
