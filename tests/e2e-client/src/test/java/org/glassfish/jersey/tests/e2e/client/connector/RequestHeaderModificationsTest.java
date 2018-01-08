/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2017 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.tests.e2e.client.connector;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Priorities;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.AsyncInvoker;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import javax.annotation.Priority;

import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.HttpUrlConnectorProvider;
import org.glassfish.jersey.client.spi.ConnectorProvider;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.grizzly.connector.GrizzlyConnectorProvider;
import org.glassfish.jersey.jetty.connector.JettyConnectorProvider;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

/**
 * JERSEY-2206 reproducer
 *
 * @author Libor Kramolis (libor.kramolis at oracle.com)
 */
@RunWith(Parameterized.class)
public class RequestHeaderModificationsTest extends JerseyTest {

    private static final Logger LOGGER = Logger.getLogger(RequestHeaderModificationsTest.class.getName());
    private static final boolean GZIP = false; // change to true when JERSEY-2341 fixed
    private static final boolean DUMP_ENTITY = false; // I have troubles to dump entity with async jetty!

    private static final String QUESTION = "QUESTION";
    private static final String ANSWER = "ANSWER";
    private static final String REQUEST_HEADER_NAME_CLIENT = "Client-Prop";
    private static final String REQUEST_HEADER_VALUE_CLIENT = "Client-Value";
    private static final String REQUEST_HEADER_NAME_FILTER = "Filter-Prop";
    private static final String REQUEST_HEADER_VALUE_FILTER = "Filter-Value";
    private static final String REQUEST_HEADER_NAME_INTERCEPTOR = "Iceptor-Prop";
    private static final String REQUEST_HEADER_VALUE_INTERCEPTOR = "Iceptor-Value";
    private static final String REQUEST_HEADER_NAME_MBW = "Mbw-Prop";
    private static final String REQUEST_HEADER_VALUE_MBW = "Mbw-Value";
    private static final String REQUEST_HEADER_MODIFICATION_SUPPORTED = "modificationSupported";
    private static final String PATH = "/resource";

    @Parameterized.Parameters(name = "{index}: {0} / modificationSupported= {1} / addHeader= {2}")
    public static List<Object[]> testData() {
        return Arrays.asList(new Object[][] {
                {HttpUrlConnectorProvider.class, true, false},
                {GrizzlyConnectorProvider.class, false, false}, // change to true when JERSEY-2341 fixed
                {JettyConnectorProvider.class, false, false}, // change to true when JERSEY-2341 fixed
                {ApacheConnectorProvider.class, false, false}, // change to true when JERSEY-2341 fixed
                {HttpUrlConnectorProvider.class, true, true},
                {GrizzlyConnectorProvider.class, false, true}, // change to true when JERSEY-2341 fixed
                {JettyConnectorProvider.class, false, true}, // change to true when JERSEY-2341 fixed
                {ApacheConnectorProvider.class, false, true}, // change to true when JERSEY-2341 fixed
        });
    }

    private final ConnectorProvider connectorProvider;
    private final boolean modificationSupported; // remove when JERSEY-2341 fixed
    private final boolean addHeader;

    public RequestHeaderModificationsTest(Class<? extends ConnectorProvider> connectorProviderClass,
                                          boolean modificationSupported, boolean addHeader)
            throws IllegalAccessException, InstantiationException {
        this.connectorProvider = connectorProviderClass.newInstance();
        this.modificationSupported = modificationSupported;
        this.addHeader = addHeader;
    }

    @Override
    protected Application configure() {
        set(TestProperties.RECORD_LOG_LEVEL, Level.WARNING.intValue());

        enable(TestProperties.LOG_TRAFFIC);
        if (DUMP_ENTITY) {
            enable(TestProperties.DUMP_ENTITY);
        }
        return new ResourceConfig(TestResource.class).register(new LoggingFeature(LOGGER, LoggingFeature.Verbosity.HEADERS_ONLY));
    }

    @Override
    protected void configureClient(ClientConfig clientConfig) {
        clientConfig.register(MyClientRequestFilter.class);
        clientConfig.register(new MyWriterInterceptor(addHeader));
        clientConfig.register(new MyMessageBodyWriter(addHeader));
        clientConfig.connectorProvider(connectorProvider);
    }

    @Test
    public void testWarningLogged() throws Exception {
        Response response = requestBuilder().post(requestEntity());
        assertResponse(response);
    }

    @Test
    public void testWarningLoggedAsync() throws Exception {
        AsyncInvoker asyncInvoker = requestBuilder().async();
        Future<Response> responseFuture = asyncInvoker.post(requestEntity());
        Response response = responseFuture.get();
        assertResponse(response);
    }

    private Invocation.Builder requestBuilder() {
        return target(PATH)
                .request()
                .header(REQUEST_HEADER_NAME_CLIENT, REQUEST_HEADER_VALUE_CLIENT)
                .header(REQUEST_HEADER_MODIFICATION_SUPPORTED, modificationSupported && addHeader)
                .header("hello", "double").header("hello", "value");
    }

    private Entity<MyEntity> requestEntity() {
        return Entity.text(new MyEntity(QUESTION));
    }

    private void assertResponse(Response response) {
        if (!modificationSupported) {
            final String UNSENT_HEADER_CHANGES = "Unsent header changes";
            LogRecord logRecord = findLogRecord(UNSENT_HEADER_CHANGES);
            if (addHeader) {
                assertNotNull("Missing LogRecord for message '" + UNSENT_HEADER_CHANGES + "'.", logRecord);
                assertThat(logRecord.getMessage(), containsString(REQUEST_HEADER_NAME_INTERCEPTOR));
                assertThat(logRecord.getMessage(), containsString(REQUEST_HEADER_NAME_MBW));
            } else {
                assertNull("Unexpected LogRecord for message '" + UNSENT_HEADER_CHANGES + "'.", logRecord);
            }
        }

        assertEquals(200, response.getStatus());
        assertEquals(ANSWER, response.readEntity(String.class));
    }

    private LogRecord findLogRecord(String messageContains) {
        for (final LogRecord record : getLoggedRecords()) {
            if (record.getMessage().contains(messageContains)) {
                return record;
            }
        }
        return null;
    }

    @Path(PATH)
    public static class TestResource {

        @POST
        public String handle(InputStream questionStream,
                             @HeaderParam(REQUEST_HEADER_NAME_CLIENT) String client,
                             @HeaderParam(REQUEST_HEADER_NAME_FILTER) String filter,
                             @HeaderParam(REQUEST_HEADER_NAME_INTERCEPTOR) String interceptor,
                             @HeaderParam(REQUEST_HEADER_NAME_MBW) String mbw,
                             @HeaderParam(REQUEST_HEADER_MODIFICATION_SUPPORTED) boolean modificationSupported)
                throws IOException {
            assertEquals(REQUEST_HEADER_VALUE_CLIENT, client);
            assertEquals(REQUEST_HEADER_VALUE_FILTER, filter);
            if (modificationSupported) {
                assertEquals(REQUEST_HEADER_VALUE_INTERCEPTOR, interceptor);
                assertEquals(REQUEST_HEADER_VALUE_MBW, mbw);
            }
            assertEquals(QUESTION, new Scanner(GZIP ? new GZIPInputStream(questionStream) : questionStream).nextLine());
            return ANSWER;
        }
    }

    public static class MyWriterInterceptor implements WriterInterceptor {

        private final boolean addHeader;

        public MyWriterInterceptor(boolean addHeader) {
            this.addHeader = addHeader;
        }

        @Override
        public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
            if (addHeader) {
                context.getHeaders().add(REQUEST_HEADER_NAME_INTERCEPTOR, REQUEST_HEADER_VALUE_INTERCEPTOR);
            }
            if (GZIP) {
                context.setOutputStream(new GZIPOutputStream(context.getOutputStream()));
            }
            context.proceed();
        }
    }

    public static class MyClientRequestFilter implements ClientRequestFilter {

        @Override
        public void filter(ClientRequestContext requestContext) throws IOException {
            requestContext.getHeaders().add(REQUEST_HEADER_NAME_FILTER, REQUEST_HEADER_VALUE_FILTER);
        }
    }

    @Priority(Priorities.ENTITY_CODER)
    public static class MyMessageBodyWriter implements MessageBodyWriter<MyEntity> {

        private final boolean addHeader;

        public MyMessageBodyWriter(boolean addHeader) {
            this.addHeader = addHeader;
        }

        @Override
        public boolean isWriteable(Class type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return true;
        }

        @Override
        public long getSize(MyEntity o, Class type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return -1; //ignored
        }

        @Override
        public void writeTo(MyEntity o, Class type, Type genericType, Annotation[] annotations, MediaType mediaType,
                            MultivaluedMap httpHeaders, OutputStream entityStream)
                throws IOException, WebApplicationException {
            if (addHeader) {
                httpHeaders.add(REQUEST_HEADER_NAME_MBW, REQUEST_HEADER_VALUE_MBW);
            }
            entityStream.write(o.getValue().getBytes());
        }
    }

    public static class MyEntity {

        private String value;

        public MyEntity() {
        }

        public MyEntity(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

}
