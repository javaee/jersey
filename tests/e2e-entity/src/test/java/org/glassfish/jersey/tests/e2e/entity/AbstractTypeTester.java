/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2017 Oracle and/or its affiliates. All rights reserved.
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;

import javax.ws.rs.ConstrainedTo;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.message.internal.ReaderWriter;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import static org.junit.Assert.assertEquals;

/**
 * Abstract entity type tester base class.
 *
 * @author Paul Sandoz
 * @author Martin Matula
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public abstract class AbstractTypeTester extends JerseyTest {

    protected static final ThreadLocal localRequestEntity = new ThreadLocal();

    public abstract static class AResource<T> {

        @POST
        public T post(T t) {
            return t;
        }
    }

    public static class RequestEntityInterceptor implements WriterInterceptor {

        @Override
        public void aroundWriteTo(WriterInterceptorContext writerInterceptorContext) throws IOException, WebApplicationException {
            OutputStream original = writerInterceptorContext.getOutputStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            writerInterceptorContext.setOutputStream(baos);
            writerInterceptorContext.proceed();
            final byte[] requestEntity = baos.toByteArray();
            writerInterceptorContext.setProperty("requestEntity", requestEntity);
            original.write(requestEntity);
        }
    }

    public static class ResponseFilter implements ClientResponseFilter {

        @Override
        public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
            localRequestEntity.set(requestContext.getProperty("requestEntity"));
        }
    }

    /**
     * Looks for all resources and providers declared as inner classes of the subclass of this class
     * and adds them to the returned ResourceConfig (unless constrained to client side).
     *
     * @return ResourceConfig instance
     */
    @Override
    protected Application configure() {
        HashSet<Class<?>> classes = new HashSet<Class<?>>();

        for (Class<?> cls : getClass().getDeclaredClasses()) {
            if (cls.getAnnotation(Path.class) != null) {
                classes.add(cls);
            } else if (cls.getAnnotation(Provider.class) != null) {
                final ConstrainedTo constrainedTo = cls.getAnnotation(ConstrainedTo.class);
                if (constrainedTo == null || constrainedTo.value() == RuntimeType.SERVER) {
                    classes.add(cls);
                }
            }
        }

        return new ResourceConfig(classes);
    }

    /**
     * Looks for all providers declared as inner classes of the subclass of this class
     * and adds them to the client configuration (unless constrained to server side).
     */
    @Override
    protected void configureClient(ClientConfig config) {
        config.register(RequestEntityInterceptor.class);
        config.register(ResponseFilter.class);

        for (Class<?> cls : getClass().getDeclaredClasses()) {
            if (cls.getAnnotation(Provider.class) != null) {
                final ConstrainedTo constrainedTo = cls.getAnnotation(ConstrainedTo.class);
                if (constrainedTo == null || constrainedTo.value() == RuntimeType.CLIENT) {
                    config.register(cls);
                }
            }
        }
    }

    protected <T> void _test(T in, Class resource) {
        _test(in, resource, true);
    }

    protected <T> void _test(T in, Class resource, MediaType m) {
        _test(in, resource, m, true);
    }

    protected <T> void _test(T in, Class resource, boolean verify) {
        _test(in, resource, MediaType.TEXT_PLAIN_TYPE, verify);
    }

    protected <T> void _test(T in, Class resource, MediaType m, boolean verify) {
        WebTarget target = target(resource.getSimpleName());
        Response response = target.request().post(Entity.entity(in, m));

        byte[] inBytes = getRequestEntity();
        byte[] outBytes = getEntityAsByteArray(response);

        if (verify) {
            _verify(inBytes, outBytes);
        }
    }

    protected static byte[] getRequestEntity() {
        try {
            return (byte[]) localRequestEntity.get();
        } finally {
            localRequestEntity.set(null);
        }
    }

    protected static void _verify(byte[] in, byte[] out) {
        assertEquals(in.length, out.length);
        for (int i = 0; i < in.length; i++) {
            if (in[i] != out[i]) {
                assertEquals("Index: " + i, in[i], out[i]);
            }
        }
    }

    protected static byte[] getEntityAsByteArray(Response r) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ReaderWriter.writeTo(r.readEntity(InputStream.class), baos);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return baos.toByteArray();
    }
}
