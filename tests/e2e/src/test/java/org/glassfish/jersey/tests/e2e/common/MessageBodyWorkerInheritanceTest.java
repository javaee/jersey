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
package org.glassfish.jersey.tests.e2e.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Test-case for JERSEY-1481.
 *
 * {@link JacksonFeature Jackson provider} should not take precedence over our
 * custom provider.
 *
 * @author Michal Gajdos
 */
public class MessageBodyWorkerInheritanceTest extends JerseyTest {

    public static interface Model<T> {

        public T getValue();
    }

    public static class StringModel implements Model<String> {

        private final String value;

        public StringModel(final String value) {
            this.value = value;
        }

        @Override
        public String getValue() {
            return value;
        }
    }

    public static interface InterfaceType extends Model {
    }

    @Consumes(MediaType.APPLICATION_JSON)
    @Provider
    public static class ModelReader<T extends Model> implements MessageBodyReader<T> {

        @Override
        public boolean isReadable(
                        Class<?> type,
                        Type genericType,
                        Annotation[] annotations,
                        MediaType mediaType) {

            return Model.class.isAssignableFrom(type);
        }

        @Override
        public T readFrom(
                    Class<T> type,
                    Type genericType,
                    Annotation[] annotations,
                    MediaType mediaType,
                    MultivaluedMap<String, String> httpHeaders,
                    InputStream entityStream) throws IOException, WebApplicationException {

            return (T) new InterfaceType() {
                @Override
                public Object getValue() {
                    return "fromInterfaceTypeReader";
                }
            };
        }
    }

    @Provider
    public abstract static class BaseProvider<T> implements MessageBodyWriter<T> {

        @Override
        public boolean isWriteable(final Class<?> type,
                final Type genericType,
                final Annotation[] annotations,
                final MediaType mediaType) {
            return true;
        }

        @Override
        public long getSize(final T t,
                final Class<?> type,
                final Type genericType,
                final Annotation[] annotations,
                final MediaType mediaType) {
            return -1;
        }

        @Override
        public void writeTo(final T t,
                final Class<?> type,
                final Type genericType,
                final Annotation[] annotations,
                final MediaType mediaType,
                final MultivaluedMap<String, Object> httpHeaders,
                final OutputStream entityStream) throws IOException, WebApplicationException {
            entityStream.write(getContent(t).getBytes("UTF-8"));
        }

        public abstract String getContent(T proxy);
    }

    @Provider
    @Produces(MediaType.APPLICATION_JSON)
    public static class GenericModelWriter extends BaseProvider<Model> {

        @Override
        public String getContent(final Model proxy) {
            return "{\"bar\":\"" + proxy.getValue() + "\"}";
        }
    }

    @Provider
    @Produces(MediaType.APPLICATION_JSON)
    public static class IntegerModelWriter extends BaseProvider<Model<Integer>> {

        @Override
        public String getContent(final Model<Integer> proxy) {
            return "{\"foo\":\"" + proxy.getValue() + "\"}";
        }
    }

    @Path("resource")
    public static class Resource {

        @GET
        public Model<String> getStringModel() {
            return new StringModel("foo");
        }

        @POST
        @Produces(MediaType.TEXT_PLAIN)
        public String post(InterfaceType t) {
            return t.getValue().toString();
        }
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(Resource.class)
                .register(GenericModelWriter.class)
                .register(ModelReader.class)
                .register(JacksonFeature.class);
    }

    @Test
    public void testMessageBodyWorkerInheritance() throws Exception {
        assertEquals("{\"bar\":\"foo\"}", target().path("resource").request(MediaType.APPLICATION_JSON_TYPE).get(String.class));
    }

    @Test
    public void testMessageBodyWorkerInterfaceInheritance() throws Exception {

        final Response response = target().path("resource")
                                     .request().post(Entity.json("{\"value\":\"ignored\"}"));

        assertEquals(200, response.getStatus());
        assertEquals("fromInterfaceTypeReader", response.readEntity(String.class));
    }
}
