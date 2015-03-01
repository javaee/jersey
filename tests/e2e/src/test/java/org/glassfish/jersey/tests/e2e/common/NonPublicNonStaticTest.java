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
package org.glassfish.jersey.tests.e2e.common;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests deployment of non-standard or invalid resources and providers.
 * <p/>
 * The tests are checking, that the failure (if expected) takes place in the expected time (during deployment,
 * or on first request).
 *
 * @author Paul Sandoz
 * @author Adam Lindenthal (adam.lindenthal at oracle.com)
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({NonPublicNonStaticTest.NonStaticResourceTest.class,
        NonPublicNonStaticTest.NonStaticResourceSubResourceTest.class,
        NonPublicNonStaticTest.NonPublicResourceTest.class,
        NonPublicNonStaticTest.NonPublicResourceSubResourceTest.class,
        NonPublicNonStaticTest.AbstractResourceTest.class,
        NonPublicNonStaticTest.AbstractResourceSubResourceTest.class,
        NonPublicNonStaticTest.InterfaceResourceTest.class,
        NonPublicNonStaticTest.InterfaceResourceSubResourceTest.class,
        NonPublicNonStaticTest.NonPublicResourceWithConstructorTest.class,
        NonPublicNonStaticTest.NonPublicResourceSubresourceWithConstructorTest.class,
        NonPublicNonStaticTest.PublicResourceWithPrivateConstructorTest.class,
        NonPublicNonStaticTest.NonStaticProviderTest.class,
        NonPublicNonStaticTest.NonPublicProviderTest.class,
        NonPublicNonStaticTest.PublicProviderWithPrivateConstructorTest.class})
public class NonPublicNonStaticTest {

    // various resources and providers for the test bellow
    // as we need to create and deploy separate and customized resourceConfigs for each test case,
    // the testcases are encapsulated in inner-classes, which are subclasses of JerseyTest.
    // Those resources and providers are, however in the outter class (can be reused inside resource configs)

    @Path("/non-static")
    public class NonStaticResource {

        public String get() {
            return "Hi";
        }
    }

    @Path("/non-static-sub")
    public static class NonStaticResourceSubResource {

        @Path("class")
        public Class<NonStaticResource> getClazz() {
            return NonStaticResource.class;
        }
    }

    @Path("/non-public")
    static class NonPublicResource {

        @GET
        public String get() {
            return "hi";
        }
    }

    @Path("/non-public-sub")
    public static class NonPublicResourceSubResource {

        @Path("class")
        public Class<NonPublicResource> getClazz() {
            return NonPublicResource.class;
        }
    }

    @Path("/abstract")
    public abstract static class AbstractResource {

        @GET
        public String get() {
            return "Hi!";
        }
    }

    @Path("/abstract-sub")
    public static class AbstractResourceSubResource {

        @Path("class")
        public Class<AbstractResource> getClazz() {
            return AbstractResource.class;
        }
    }

    @Path("interface")
    public static interface InterfaceResource {
    }

    @Path("interface-sub")
    public static class InterfaceResourceSubResource {

        @Path("class")
        public Class<InterfaceResource> getClazz() {
            return InterfaceResource.class;
        }
    }

    @Path("non-public-with-constructor")
    static class NonPublicResourceWithConstructor {

        public NonPublicResourceWithConstructor() {
        }

        @GET
        public String get() {
            return "Hi!";
        }
    }

    @Path("non-public-sub-with-constructor")
    public static class NonPublicResourceSubResourceWithConstructor {

        @Path("class")
        public Class<NonPublicResourceWithConstructor> getClazz() {
            return NonPublicResourceWithConstructor.class;
        }
    }

    @Path("public-with-private-constructor")
    public static class PublicResourceWithPrivateConstructor {

        private PublicResourceWithPrivateConstructor() {
        }

        @GET
        public String get() {
            return "Hi!";
        }
    }

    @Path("provider-resource")
    public static class ProviderResource {

        @GET
        public String get() {
            return "Hi!";
        }
    }

    @Provider
    public class NonStaticProvider implements MessageBodyWriter<String> {

        @Override
        public boolean isWriteable(final Class<?> type, final Type genericType, final Annotation[] annotations,
                                   final MediaType mediaType) {
            return type == String.class;
        }

        @Override
        public long getSize(final String s, final Class<?> type, final Type genericType, final Annotation[] annotations,
                            final MediaType mediaType) {
            return -1;
        }

        @Override
        public void writeTo(final String s, final Class<?> type, final Type genericType, final Annotation[] annotations,
                            final MediaType mediaType, final MultivaluedMap<String, Object> httpHeaders,
                            final OutputStream entityStream) throws IOException, WebApplicationException {
            entityStream.write(s.getBytes());
        }
    }

    @Provider
    static class NonPublicProvider implements MessageBodyWriter<String> {

        @Override
        public boolean isWriteable(final Class<?> type, final Type genericType, final Annotation[] annotations,
                                   final MediaType mediaType) {
            return type == String.class;
        }

        @Override
        public long getSize(final String s, final Class<?> type, final Type genericType, final Annotation[] annotations,
                            final MediaType mediaType) {
            return -1;
        }

        @Override
        public void writeTo(final String s, final Class<?> type, final Type genericType, final Annotation[] annotations,
                            final MediaType mediaType, final MultivaluedMap<String, Object> httpHeaders,
                            final OutputStream entityStream) throws IOException, WebApplicationException {
            final String wrapped = ">> " + s + " <<";
            entityStream.write(wrapped.getBytes());
        }
    }

    @Provider
    static class PublicProviderWithPrivateConstructor implements MessageBodyWriter<String> {

        private PublicProviderWithPrivateConstructor() {
        }

        @Override
        public boolean isWriteable(final Class<?> type, final Type genericType, final Annotation[] annotations,
                                   final MediaType mediaType) {
            return type == String.class;
        }

        @Override
        public long getSize(final String s, final Class<?> type, final Type genericType, final Annotation[] annotations,
                            final MediaType mediaType) {
            return -1;
        }

        @Override
        public void writeTo(final String s, final Class<?> type, final Type genericType, final Annotation[] annotations,
                            final MediaType mediaType, final MultivaluedMap<String, Object> httpHeaders,
                            final OutputStream entityStream) throws IOException, WebApplicationException {
            entityStream.write(s.getBytes());
        }
    }

    @Provider
    public abstract static class AbstractProvider implements MessageBodyWriter<String> {

        @Override
        public boolean isWriteable(final Class<?> type, final Type genericType, final Annotation[] annotations,
                                   final MediaType mediaType) {
            return type == String.class;
        }

        @Override
        public long getSize(final String s, final Class<?> type, final Type genericType, final Annotation[] annotations,
                            final MediaType mediaType) {
            return -1;
        }

        @Override
        public void writeTo(final String s, final Class<?> type, final Type genericType, final Annotation[] annotations,
                            final MediaType mediaType, final MultivaluedMap<String, Object> httpHeaders,
                            final OutputStream entityStream) throws IOException, WebApplicationException {
            entityStream.write(s.getBytes());
        }
    }

    // Inner test classes - each needed resource config variation has its own inner class

    public static class NonStaticResourceTest extends JerseyTest {

        @Override
        public ResourceConfig configure() {
            set(TestProperties.RECORD_LOG_LEVEL, Level.WARNING.intValue());
            return new ResourceConfig(NonStaticResource.class);
        }

        @Test
        public void testNonStaticResource() throws IOException {
            final List<LogRecord> loggedRecords = getLoggedRecords();
            boolean firstFound = false;
            boolean secondFound = false;
            for (LogRecord record : loggedRecords) {
                if (record.getMessage().contains("cannot be instantiated and will be ignored")) {
                    firstFound = true;
                } else if (record.getMessage().contains("is empty. It has no resource")) {
                    secondFound = true;
                }
                if (firstFound && secondFound) {
                    break;
                }
            }
            assertTrue("Expected log message (1st) was not found.", firstFound);
            assertTrue("Expected log message (2nd) was not found.", secondFound);
        }
    }

    public static class NonStaticResourceSubResourceTest extends JerseyTest {

        @Override
        public ResourceConfig configure() {
            return new ResourceConfig(NonStaticResourceSubResource.class);
        }

        @Test(expected = InternalServerErrorException.class)
        public void testNonStaticResource() throws IOException {
            target().path("/non-static-sub/class").request().get(String.class);
        }
    }

    public static class NonPublicResourceTest extends JerseyTest {

        @Override
        public ResourceConfig configure() {
            return new ResourceConfig(NonPublicResource.class);
        }

        @Test(expected = InternalServerErrorException.class)
        public void testNonPublicResource() throws IOException {
            target().path("/non-public").request().get(String.class);
        }
    }

    public static class NonPublicResourceSubResourceTest extends JerseyTest {

        @Override
        public ResourceConfig configure() {
            return new ResourceConfig(NonPublicResourceSubResource.class);
        }

        @Test(expected = InternalServerErrorException.class)
        public void testNonPublicResource() throws IOException {
            target().path("/non-public-sub/class").request().get(String.class);
        }
    }

    public static class AbstractResourceTest extends JerseyTest {

        @Override
        public ResourceConfig configure() {
            set(TestProperties.RECORD_LOG_LEVEL, Level.WARNING.intValue());
            return new ResourceConfig(AbstractResource.class);
        }

        @Test
        public void testAbstractResource() throws IOException {
            final List<LogRecord> loggedRecords = getLoggedRecords();
            boolean found = false;
            for (LogRecord record : loggedRecords) {
                if (record.getMessage().contains("cannot be instantiated and will be ignored")) {
                    found = true;
                    break;
                }
            }
            assertTrue("Expected log record was not found.", found);
        }
    }

    public static class AbstractResourceSubResourceTest extends JerseyTest {

        @Override
        public ResourceConfig configure() {
            return new ResourceConfig(AbstractResourceSubResource.class);
        }

        @Test(expected = InternalServerErrorException.class)
        public void testAbstractResourceSubResource() throws IOException {
            target().path("abstract-sub/class").request().get(String.class);
        }
    }

    public static class InterfaceResourceTest extends JerseyTest {

        @Override
        public ResourceConfig configure() {
            set(TestProperties.RECORD_LOG_LEVEL, Level.WARNING.intValue());
            return new ResourceConfig(InterfaceResource.class);
        }

        @Test
        public void testInterfaceResource() throws IOException {
            final List<LogRecord> loggedRecords = getLoggedRecords();
            boolean firstFound = false;
            boolean secondFound = false;
            for (LogRecord record : loggedRecords) {
                if (record.getMessage().contains("cannot be instantiated and will be ignored")) {
                    firstFound = true;
                } else if (record.getMessage().contains("is empty. It has no resource")) {
                    secondFound = true;
                }
                if (firstFound && secondFound) {
                    break;
                }
            }
            assertTrue("Expected log message (1st) was not found.", firstFound);
            assertTrue("Expected log message (2nd) was not found.", secondFound);
        }
    }

    public static class InterfaceResourceSubResourceTest extends JerseyTest {

        @Override
        public ResourceConfig configure() {
            return new ResourceConfig(InterfaceResourceSubResource.class);
        }

        @Test(expected = InternalServerErrorException.class)
        public void testInterfaceResourceSubResource() throws IOException {
            target().path("interface-sub/class").request().get(String.class);
        }
    }

    public static class NonPublicResourceWithConstructorTest extends JerseyTest {

        @Override
        public ResourceConfig configure() {
            return new ResourceConfig(NonPublicResourceWithConstructor.class);
        }

        @Test(expected = InternalServerErrorException.class)
        public void testNonPublicResourceWithConstructor() throws IOException {
            target().path("non-public-with-constructor").request().get(String.class);
        }
    }

    public static class NonPublicResourceSubresourceWithConstructorTest extends JerseyTest {

        @Override
        public ResourceConfig configure() {
            return new ResourceConfig(NonPublicResourceSubResourceWithConstructor.class);
        }

        @Test(expected = InternalServerErrorException.class)
        public void testNonPublicResourceSubResourceWithConstructor() throws IOException {
            target().path("non-public-sub-with-constructor/class").request().get(String.class);
        }
    }

    public static class PublicResourceWithPrivateConstructorTest extends JerseyTest {

        @Override
        public ResourceConfig configure() {
            set(TestProperties.RECORD_LOG_LEVEL, Level.WARNING.intValue());
            return new ResourceConfig(PublicResourceWithPrivateConstructor.class);
        }

        @Test(expected = InternalServerErrorException.class)
        public void testPublicResourceWithPrivateConstructor() throws IOException {
            target().path("public-with-private-constructor").request().get(String.class);
        }
    }

    public static class NonStaticProviderTest extends JerseyTest {

        @Override
        public ResourceConfig configure() {
            set(TestProperties.RECORD_LOG_LEVEL, Level.WARNING.intValue());
            return new ResourceConfig(ProviderResource.class, NonStaticProvider.class);
        }

        @Test
        public void testNonStaticProvider() throws IOException {
            final List<LogRecord> loggedRecords = getLoggedRecords();
            boolean found = false;
            for (LogRecord record : loggedRecords) {
                if (record.getMessage().contains("Instantiation of non-static member classes is not supported")) {
                    found = true;
                    break;
                }
            }
            assertTrue("Expected log record was not found.", found);
        }
    }

    public static class NonPublicProviderTest extends JerseyTest {

        @Override
        public ResourceConfig configure() {
            return new ResourceConfig(ProviderResource.class, NonPublicProvider.class);
        }

        @Test
        public void testNonPublicProvider() throws IOException {
            final String s = target().path("provider-resource").request().get(String.class);
            assertEquals(">> Hi! <<", s);
        }
    }

    // NOTE - the original Jersey 1 test nonPublicProviderWithConstructor does not make sense considering that it works even
    // w/o constructor

    public static class PublicProviderWithPrivateConstructorTest extends JerseyTest {

        @Override
        public ResourceConfig configure() {
            set(TestProperties.RECORD_LOG_LEVEL, Level.WARNING.intValue());
            return new ResourceConfig(ProviderResource.class, PublicProviderWithPrivateConstructor.class);
        }

        @Test
        public void testNonPublicProvider() throws IOException {
            final List<LogRecord> loggedRecords = getLoggedRecords();
            boolean found = false;
            for (LogRecord record : loggedRecords) {
                if (record.getMessage().contains("Could not find a suitable constructor")) {
                    found = true;
                    break;
                }
            }
            assertTrue("Expected log record was not found.", found);
        }
    }

    // abstract provider - throws MultiException after calling configure(), but before running the @Test method
    // the same for interface provider test

}
