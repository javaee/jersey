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

package org.glassfish.jersey.message;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.message.internal.ReaderWriter;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Michal Gajdos
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({MetaInfServicesTest.Enable.class, MetaInfServicesTest.DisableServer.class,
        MetaInfServicesTest.DisableClient.class})
public class MetaInfServicesTest {

    public static class MetaInf {

        private String value;

        public MetaInf(final String value) {
            this.value = value;
        }
    }

    @Path("resource")
    public static class MetaInfServicesResource {

        @POST
        public MetaInf post(final MetaInf entity) {
            return entity;
        }
    }

    public static class MessageProvider implements MessageBodyReader<MetaInf>, MessageBodyWriter<MetaInf> {

        @Context
        private Configuration config;

        @Override
        public boolean isReadable(final Class<?> type, final Type genericType, final Annotation[] annotations,
                                  final MediaType mediaType) {
            return true;
        }

        @Override
        public MetaInf readFrom(final Class<MetaInf> type, final Type genericType, final Annotation[] annotations,
                               final MediaType mediaType, final MultivaluedMap<String, String> httpHeaders,
                               final InputStream entityStream) throws IOException, WebApplicationException {
            return new MetaInf(ReaderWriter.readFromAsString(entityStream, mediaType)
                    + "_read_" + config.getRuntimeType().name());
        }

        @Override
        public boolean isWriteable(final Class<?> type, final Type genericType, final Annotation[] annotations,
                                   final MediaType mediaType) {
            return true;
        }

        @Override
        public long getSize(final MetaInf s, final Class<?> type, final Type genericType, final Annotation[] annotations,
                            final MediaType mediaType) {
            return -1;
        }

        @Override
        public void writeTo(final MetaInf s, final Class<?> type, final Type genericType, final Annotation[] annotations,
                            final MediaType mediaType, final MultivaluedMap<String, Object> httpHeaders,
                            final OutputStream entityStream) throws IOException, WebApplicationException {
            entityStream.write((s.value + "_write_" + config.getRuntimeType().name()).getBytes());
        }
    }

    public static class Enable extends JerseyTest {

        @Override
        protected Application configure() {
            return new ResourceConfig(MetaInfServicesResource.class);
        }

        @Test
        public void testEnable() throws Exception {
            final Response response = target("resource").request().post(Entity.text(new MetaInf("foo")));

            assertThat(response.getStatus(), is(200));
            assertThat(response.readEntity(MetaInf.class).value,
                    is("foo_write_CLIENT_read_SERVER_write_SERVER_read_CLIENT"));
        }
    }

    public static class DisableServer extends JerseyTest {

        @Override
        protected Application configure() {
            return new ResourceConfig(MetaInfServicesResource.class)
                    .property(ServerProperties.FEATURE_AUTO_DISCOVERY_DISABLE, true);
        }

        @Test
        public void testDisableServer() throws Exception {
            final Response response = target("resource").request().post(Entity.text(new MetaInf("foo")));

            assertThat(response.getStatus(), is(200));
            assertThat(response.readEntity(MetaInf.class).value,
                    is("foo_write_CLIENT_read_SERVER_write_SERVER_read_CLIENT"));
        }
    }

    public static class DisableClient extends JerseyTest {

        @Override
        protected Application configure() {
            return new ResourceConfig(MetaInfServicesResource.class);
        }

        @Override
        protected void configureClient(final ClientConfig config) {
            config.property(ClientProperties.FEATURE_AUTO_DISCOVERY_DISABLE, true);
        }

        @Test
        public void testDisableServer() throws Exception {
            final Response response = target("resource").request().post(Entity.text(new MetaInf("foo")));

            assertThat(response.getStatus(), is(200));
            assertThat(response.readEntity(MetaInf.class).value,
                    is("foo_write_CLIENT_read_SERVER_write_SERVER_read_CLIENT"));
        }
    }
}
