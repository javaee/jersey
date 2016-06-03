/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2016 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.client;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.net.URI;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.WriterInterceptor;

import org.glassfish.jersey.internal.MapPropertiesDelegate;
import org.glassfish.jersey.internal.PropertiesDelegate;
import org.glassfish.jersey.internal.util.ExceptionUtils;
import org.glassfish.jersey.message.MessageBodyWorkers;

import org.hamcrest.core.Is;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;

/**
 * {@code ClientRequest} unit tests.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class ClientRequestTest {

    @Mock
    private MessageBodyWorkers workers;
    @Mock
    private GenericType<?> entityType;

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    /**
     * Test of resolving properties in the client request.
     */
    @Test
    public void testResolveProperty() {
        JerseyClient client;
        ClientRequest request;

        // test property in neither config nor request
        client = new JerseyClientBuilder().build();

        request = new ClientRequest(
                URI.create("http://example.org"),
                client.getConfiguration(),
                new MapPropertiesDelegate());

        assertFalse(request.getConfiguration().getPropertyNames().contains("name"));
        assertFalse(request.getPropertyNames().contains("name"));

        assertNull(request.getConfiguration().getProperty("name"));
        assertNull(request.getProperty("name"));

        assertNull(request.resolveProperty("name", String.class));
        assertEquals("value-default", request.resolveProperty("name", "value-default"));

        // test property in config only
        client = new JerseyClientBuilder().property("name", "value-global").build();

        request = new ClientRequest(
                URI.create("http://example.org"),
                client.getConfiguration(),
                new MapPropertiesDelegate());

        assertTrue(request.getConfiguration().getPropertyNames().contains("name"));
        assertFalse(request.getPropertyNames().contains("name"));

        assertEquals("value-global", request.getConfiguration().getProperty("name"));
        assertNull(request.getProperty("name"));

        assertEquals("value-global", request.resolveProperty("name", String.class));
        assertEquals("value-global", request.resolveProperty("name", "value-default"));

        // test property in request only
        client = new JerseyClientBuilder().build();

        request = new ClientRequest(
                URI.create("http://example.org"),
                client.getConfiguration(),
                new MapPropertiesDelegate());
        request.setProperty("name", "value-request");

        assertFalse(request.getConfiguration().getPropertyNames().contains("name"));
        assertTrue(request.getPropertyNames().contains("name"));

        assertNull(request.getConfiguration().getProperty("name"));
        assertEquals("value-request", request.getProperty("name"));

        assertEquals("value-request", request.resolveProperty("name", String.class));
        assertEquals("value-request", request.resolveProperty("name", "value-default"));

        // test property in config and request
        client = new JerseyClientBuilder().property("name", "value-global").build();

        request = new ClientRequest(
                URI.create("http://example.org"),
                client.getConfiguration(),
                new MapPropertiesDelegate());
        request.setProperty("name", "value-request");

        assertTrue(request.getConfiguration().getPropertyNames().contains("name"));
        assertTrue(request.getPropertyNames().contains("name"));

        assertEquals("value-global", request.getConfiguration().getProperty("name"));
        assertEquals("value-request", request.getProperty("name"));

        assertEquals("value-request", request.resolveProperty("name", String.class));
        assertEquals("value-request", request.resolveProperty("name", "value-default"));
    }

    private ClientRequest mockThrowing(Exception exception) throws IOException {
        JerseyClient client = new JerseyClientBuilder().build();
        final ClientRequest request = new ClientRequest(
                URI.create("http://example.org"),
                client.getConfiguration(),
                new MapPropertiesDelegate());

        Mockito.doThrow(exception).when(workers)
                .writeTo(any(), same(entityType.getRawType()), same(entityType.getType()),
                        Mockito.<Annotation[]>any(), Mockito.<MediaType>any(),
                        Mockito.<MultivaluedMap<String, Object>>any(), Mockito.<PropertiesDelegate>any(),
                        Mockito.<OutputStream>any(), Mockito.<Iterable<WriterInterceptor>>any());
        return request;
    }

    @Test
    public void testSSLExceptionHandling()
            throws Exception {
        final IOException ioException = new IOException("Test");

        final ClientRequest request = mockThrowing(ioException);

        try {
            request.doWriteEntity(workers, entityType);
            fail("An IOException exception should be thrown.");
        } catch (IOException e) {
            Assert.assertThat("Detected a un-expected exception! \n" + ExceptionUtils.exceptionStackTraceAsString(e),
                    e, Is.is(ioException));
        }
    }

    @Test
    public void testRuntimeExceptionBeingReThrown()
            throws Exception {

        final RuntimeException runtimeException = new RuntimeException("Test");

        ClientRequest request = mockThrowing(runtimeException);

        try {
            request.doWriteEntity(workers, entityType);
            Assert.fail("A RuntimeException exception should be thrown.");
        } catch (RuntimeException e) {
            Assert.assertThat("Detected a un-expected exception! \n" + ExceptionUtils.exceptionStackTraceAsString(e),
                    e, Is.is(runtimeException));
        }
    }

}
