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
package org.glassfish.jersey.tests.e2e.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.Providers;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test if JAX-RS injection points work in client side providers.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public class InjectedClientBodyWorker extends JerseyTest {

    // media types are used to determine what kind of injection should be tested
    static final String ProviderType = "test/providers";
    static final String ConfigurationTYPE = "test/configuration";

    public static class MyContext {}

    @Provider
    public static class MyContextResolver implements ContextResolver<MyContext> {

        @Override
        public MyContext getContext(Class<?> type) {
            return null;
        }
    }

    @Provider
    @Produces(ProviderType)
    public static class ProvidersInjectedWriter implements MessageBodyWriter<String> {

        @Context
        Providers providers;

        @Override
        public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return type == String.class;
        }

        @Override
        public long getSize(String t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return -1;
        }

        @Override
        public void writeTo(String t, Class<?> type, Type genericType, Annotation[] annotations,
                            MediaType mediaType, MultivaluedMap<String, Object> httpHeaders,
                            OutputStream entityStream) throws IOException, WebApplicationException {

            // make sure no exception occurs here
            providers.getExceptionMapper(Exception.class);

            final ContextResolver<MyContext> contextResolver = providers
                    .getContextResolver(MyContext.class, MediaType.valueOf(ProviderType));
            entityStream.write(String.format("%s", contextResolver).getBytes());
        }
    }

    @Provider
    @Consumes(ProviderType)
    public static class ProvidersInjectedReader implements MessageBodyReader<String> {

        @Context
        Providers providers;

        @Override
        public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return type == String.class;
        }

        @Override
        public String readFrom(Class<String> type, Type genericType, Annotation[] annotations,
                               MediaType mediaType, MultivaluedMap<String, String> httpHeaders,
                               InputStream entityStream) throws IOException, WebApplicationException {

            // make sure no exception occurs here
            providers.getExceptionMapper(Exception.class);

            final ContextResolver<MyContext> contextResolver = providers
                    .getContextResolver(MyContext.class, MediaType.valueOf(ProviderType));
            return String.format("%s", contextResolver);
        }

    }

    @Provider
    @Produces(ConfigurationTYPE)
    public static class ConfigurationInjectedWriter implements MessageBodyWriter<String> {

        @Context
        Configuration configuration;

        @Override
        public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return type == String.class;
        }

        @Override
        public long getSize(String t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return -1;
        }

        @Override
        public void writeTo(String t, Class<?> type, Type genericType, Annotation[] annotations,
                            MediaType mediaType, MultivaluedMap<String, Object> httpHeaders,
                            OutputStream entityStream) throws IOException, WebApplicationException {

            final boolean ctxResolverRegistered = configuration.isRegistered(MyContextResolver.class);
            entityStream.write(String.format("%b", ctxResolverRegistered).getBytes());
        }
    }

    @Provider
    @Consumes(ConfigurationTYPE)
    public static class ConfigurationInjectedReader implements MessageBodyReader<String> {

        @Context
        Configuration configuration;

        @Override
        public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return type == String.class;
        }

        @Override
        public String readFrom(Class<String> type, Type genericType, Annotation[] annotations,
                               MediaType mediaType, MultivaluedMap<String, String> httpHeaders,
                               InputStream entityStream) throws IOException, WebApplicationException {
            final boolean ctxResolverRegistered = configuration.isRegistered(MyContextResolver.class);
            return String.format("%b", ctxResolverRegistered);
        }
    }

    @Path("echo")
    public static class EchoResource {

        @POST
        @Consumes({ProviderType, ConfigurationTYPE, MediaType.TEXT_PLAIN})
        @Produces({ProviderType, ConfigurationTYPE, MediaType.TEXT_PLAIN})
        public String post(String p) {
            return p;
        }
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(EchoResource.class);
    }

    @Override
    protected void configureClient(ClientConfig config) {
        config
                .register(ProvidersInjectedWriter.class)
                .register(ConfigurationInjectedWriter.class)
                .register(ProvidersInjectedReader.class)
                .register(ConfigurationInjectedReader.class);
    }

    @Test
    public void testProvidersInReader() throws Exception {
        _testProviders(ProviderType, MediaType.TEXT_PLAIN);
    }

    @Test
    public void testProvidersInWriter() throws Exception {
        _testProviders(MediaType.TEXT_PLAIN, ProviderType);
    }

    @Test
    public void testConfigurationInReader() throws Exception {
        testConfiguration(ConfigurationTYPE, MediaType.TEXT_PLAIN);
    }

    @Test
    public void testConfigurationInWriter() throws Exception {
        testConfiguration(MediaType.TEXT_PLAIN, ConfigurationTYPE);
    }

    private void _testProviders(final String incomingType, final String outgoingType) throws Exception {

        final String postWithoutProviderResult = target("echo")
                .request(outgoingType)
                .post(Entity.entity("does not matter", incomingType), String.class);
        assertThat(postWithoutProviderResult, is("null"));

        final String postWithProviderResult = target("echo")
                .register(MyContextResolver.class)
                .request(outgoingType)
                .post(Entity.entity("ignored", incomingType), String.class);
        assertThat(postWithProviderResult, containsString(MyContextResolver.class.getName()));
    }

    private void testConfiguration(final String incomingType, final String outgoingType) throws Exception {

        final String postWithoutProviderResult = target("echo")
                .request(incomingType)
                .post(Entity.entity("whatever", outgoingType), String.class);
        assertThat(postWithoutProviderResult, is("false"));

        final String postWithProviderResult = target("echo")
                .register(MyContextResolver.class)
                .request(incomingType)
                .post(Entity.entity("bummer", outgoingType), String.class);
        assertThat(postWithProviderResult, is("true"));
    }
}
