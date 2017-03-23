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

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.message.internal.AbstractMessageReaderWriterProvider;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Paul Sandoz
 * @author Martin Matula
 */
public class BeanStreamingTest extends JerseyTest {

    @Override
    protected Application configure() {
        return new ResourceConfig(getClass().getDeclaredClasses());
    }

    @Override
    protected void configureClient(ClientConfig config) {
        for (Class<?> c : getClass().getDeclaredClasses()) {
            if (c.getAnnotation(Provider.class) != null) {
                config.register(c);
            }
        }
    }

    @Test
    public void testBean() throws Exception {
        Bean b = new Bean("bean", 123, 3.1415f);

        // the following should work using BeanProvider which
        // supports Bean.class for type application/bean
        WebTarget r = target().path("/bean");
        r.request().post(Entity.entity(b, "application/bean"), Bean.class);

        try {
            r = target().path("/plain");
            r.request().post(Entity.entity(b, "text/plain"), Bean.class);
            assertFalse(false);
        } catch (ProcessingException ex) {
            assertTrue(true);
        }
    }

    @Test
    public void testBeanWild() throws Exception {
        Bean b = new Bean("bean", 123, 3.1415f);

        // the following should work using BeanWildProvider which
        // supports Bean.class for type application/*
        target().path("/wild").request().post(Entity.entity(b, "application/wild-bean"), Bean.class);
    }

    @Test
    public void testBean2() throws Exception {
        Bean2 b = new Bean2("bean", 123, 3.1415f);

        target().path("/bean2").request().post(Entity.entity(b, "application/bean"), Bean2.class);

        try {
            target().path("/plain2").request().post(Entity.entity(b, "text/plain"), Bean2.class);
            assertFalse(false);
        } catch (ProcessingException ex) {
            assertTrue(true);
        }
    }

    @Test
    public void testBean2UsingBean() throws Exception {
        Bean2 b = new Bean2("bean", 123, 3.1415f);

        // the following should work using BeanProvider which
        // supports Bean.class for type application/bean
        target().path("/bean").request().post(Entity.entity(b, "application/bean"), Bean2.class);

        try {
            target().path("/plain").request().post(Entity.entity(b, "text/plain"), Bean2.class);
            fail();
        } catch (ProcessingException ex) {
            // good
        }
    }

    @Test
    public void testBean2Wild() throws Exception {
        Bean2 b = new Bean2("bean", 123, 3.1415f);

        // the following should work using BeanWildProvider which
        // supports Bean.class for type application/*
        target().path("/wild2").request().post(Entity.entity(b, "application/wild-bean"), Bean2.class);
    }

    @Test
    public void testBean2WildUsingBean() throws Exception {
        Bean2 b = new Bean2("bean", 123, 3.1415f);

        // the following should work using BeanWildProvider which
        // supports Bean.class for type application/*
        target().path("/wild").request().post(Entity.entity(b, "application/wild-bean"), Bean2.class);
    }

    public static class Bean implements Serializable {
        private String string;
        private int integer;
        private float real;

        public Bean() {
        }

        public Bean(String string, int integer, float real) {
            this.string = string;
            this.integer = integer;
            this.real = real;
        }

        public String getString() {
            return string;
        }

        public void setString(String string) {
            this.string = string;
        }

        public int getInteger() {
            return integer;
        }

        public void setInteger(int integer) {
            this.integer = integer;
        }

        public float getReal() {
            return real;
        }

        public void setReal(float real) {
            this.real = real;
        }
    }

    @Provider
    @Produces("application/bean")
    @Consumes("application/bean")
    public static class BeanProvider extends AbstractMessageReaderWriterProvider<Bean> {

        public boolean isReadable(Class<?> type, Type genericType, Annotation annotations[], MediaType mt) {
            return type == Bean.class;
        }

        public Bean readFrom(
                Class<Bean> type,
                Type genericType,
                Annotation annotations[],
                MediaType mediaType,
                MultivaluedMap<String, String> httpHeaders,
                InputStream entityStream) throws IOException {
            ObjectInputStream oin = new ObjectInputStream(entityStream);
            try {
                return (Bean) oin.readObject();
            } catch (ClassNotFoundException cause) {
                IOException effect = new IOException(cause.getLocalizedMessage());
                effect.initCause(cause);
                throw effect;
            }
        }

        public boolean isWriteable(Class<?> type, Type genericType, Annotation annotations[], MediaType mt) {
            return type == Bean.class;
        }

        public void writeTo(
                Bean t,
                Class<?> type,
                Type genericType,
                Annotation annotations[],
                MediaType mediaType,
                MultivaluedMap<String, Object> httpHeaders,
                OutputStream entityStream) throws IOException {
            ObjectOutputStream out = new ObjectOutputStream(entityStream);
            out.writeObject(t);
            out.flush();
        }
    }

    @Provider
    @Produces("application/*")
    @Consumes("application/*")
    public static class BeanWildProvider extends BeanProvider {
        @Override
        public boolean isReadable(Class<?> type, Type genericType, Annotation annotations[], MediaType mt) {
            return type == Bean.class;
        }

        @Override
        public boolean isWriteable(Class<?> type, Type genericType, Annotation annotations[], MediaType mt) {
            return type == Bean.class;
        }
    }

    @Provider
    @Produces("application/bean")
    @Consumes("application/bean")
    public static class Bean2Provider extends AbstractMessageReaderWriterProvider<Bean2> {

        public boolean isReadable(Class<?> type, Type genericType, Annotation annotations[], MediaType mt) {
            return type == Bean2.class;
        }

        public Bean2 readFrom(
                Class<Bean2> type,
                Type genericType,
                Annotation annotations[],
                MediaType mediaType,
                MultivaluedMap<String, String> httpHeaders,
                InputStream entityStream) throws IOException {
            ObjectInputStream oin = new ObjectInputStream(entityStream);
            try {
                return (Bean2) oin.readObject();
            } catch (ClassNotFoundException cause) {
                IOException effect = new IOException(cause.getLocalizedMessage());
                effect.initCause(cause);
                throw effect;
            }
        }

        public boolean isWriteable(Class<?> type, Type genericType, Annotation annotations[], MediaType mt) {
            return type == Bean2.class;
        }

        public void writeTo(
                Bean2 t,
                Class<?> type,
                Type genericType,
                Annotation annotations[],
                MediaType mediaType,
                MultivaluedMap<String, Object> httpHeaders,
                OutputStream entityStream) throws IOException {
            ObjectOutputStream out = new ObjectOutputStream(entityStream);
            out.writeObject(t);
            out.flush();
        }
    }

    @Provider
    @Produces("application/*")
    @Consumes("application/*")
    public static class Bean2WildProvider extends Bean2Provider {
        @Override
        public boolean isReadable(Class<?> type, Type genericType, Annotation annotations[], MediaType mt) {
            return type == Bean2.class;
        }

        @Override
        public boolean isWriteable(Class<?> type, Type genericType, Annotation annotations[], MediaType mt) {
            return type == Bean2.class;
        }
    }

    public static class Bean2 extends Bean {
        public Bean2(String string, int integer, float real) {
            super(string, integer, real);
        }
    }

    @Path("/bean")
    public static class BeanResource {
        @POST
        @Consumes("application/bean")
        @Produces("application/bean")
        public Bean post(Bean t) {
            return t;
        }
    }

    @Path("/bean2")
    public static class Bean2Resource {
        @POST
        @Consumes("application/bean")
        @Produces("application/bean")
        public Bean2 post(Bean2 t) {
            return t;
        }
    }

    @Path("/plain")
    public static class BeanTextPlainResource {
        @POST
        @Consumes("text/plain")
        @Produces("text/plain")
        public Bean post(Bean t) {
            return t;
        }
    }

    @Path("/plain2")
    public static class Bean2TextPlainResource {
        @POST
        @Consumes("text/plain")
        @Produces("text/plain")
        public Bean2 post(Bean2 t) {
            return t;
        }
    }

    @Path("/wild")
    public static class BeanWildResource {
        @POST
        @Consumes("application/*")
        @Produces("application/*")
        public Bean post(Bean t) {
            return t;
        }
    }

    @Path("/wild2")
    public static class Bean2WildResource {
        @POST
        @Consumes("application/*")
        @Produces("application/*")
        public Bean2 post(Bean2 t) {
            return t;
        }
    }
}
