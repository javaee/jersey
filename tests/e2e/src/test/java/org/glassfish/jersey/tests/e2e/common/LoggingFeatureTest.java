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

package org.glassfish.jersey.tests.e2e.common;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.client.Entity;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import javax.annotation.Priority;

import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;

/**
 * {@link LoggingFeature} end-to-end tests.
 *
 * @author Michal Gajdos
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
        LoggingFeatureTest.ClientTest.class,
        LoggingFeatureTest.ContainerTest.class,
        LoggingFeatureTest.ContainerAutodiscoveryTest.class,
        LoggingFeatureTest.FiltersOrderTest.class
})
public class LoggingFeatureTest {

    private static final String LOGGER_NAME = "org.glassfish.jersey.logging.feature";
    private static final String BINARY_MEDIA_TYPE = "application/binary";
    private static final String TEXT_MEDIA_TYPE = MediaType.TEXT_PLAIN;
    private static final String ENTITY = "This entity must (not) be logged";

    @Path("/")
    public static class MyResource {

        @GET
        @Produces(BINARY_MEDIA_TYPE)
        public Response getHeadersAndBinaryPayload() {
            return Response
                    .ok(ENTITY)
                    .header("001", "First Header Value")
                    .header("002", "Second Header Value")
                    .header("003", "Third Header Value")
                    .header("004", "Fourth Header Value")
                    .header("005", "Fifth Header Value")
                    .build();
        }

        @Path("/text")
        @GET
        @Produces(TEXT_MEDIA_TYPE)
        public Response getHeadersAndTextPayload() {
            return Response
                    .ok(ENTITY)
                    .header("001", "First Header Value")
                    .header("002", "Second Header Value")
                    .header("003", "Third Header Value")
                    .header("004", "Fourth Header Value")
                    .header("005", "Fifth Header Value")
                    .build();
        }

        @Path("/text")
        @POST
        @Produces(TEXT_MEDIA_TYPE)
        public Response post(String text) {
            return Response
                    .ok(ENTITY)
                    .build();
        }

    }

    /**
     * General client side tests.
     */
    public static class ClientTest extends JerseyTest {

        @Override
        protected Application configure() {
            set(TestProperties.RECORD_LOG_LEVEL, Level.FINE.intValue());

            return new ResourceConfig(MyResource.class);
        }

        @Test
        public void testFilterAsClientRequestFilter() throws Exception {
            final Response response = target()
                    .register(new LoggingFeature(Logger.getLogger(LOGGER_NAME)))
                    .request()
                    .get();

            // Correct response status.
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            // Check logs for proper id.
            assertThat(getLoggingFilterRequestLogRecord(getLoggedRecords()).getMessage(), containsString("1 *"));
        }

        @Test
        public void testOrderOfHeadersOnClient() throws Exception {
            final Response response = target()
                    .register(new LoggingFeature(Logger.getLogger(LOGGER_NAME)))
                    .request()
                    .get();
            assertThat(response.readEntity(String.class), equalTo(ENTITY));

            final LogRecord record = getLoggingFilterResponseLogRecord(getLoggedRecords());
            final String message = record.getMessage();

            int i = 1;
            do {
                final String h1 = "00" + i++;
                final String h2 = "00" + i;

                final int i1 = message.indexOf(h1);
                final int i2 = message.indexOf(h2);

                assertThat("Header " + h1 + " has been logged sooner than header " + h2, i1, lessThan(i2));
            } while (i < 5);
        }

        @Test
        public void testVerbosityAnyPayload() throws Exception {
            final Response response = target()
                    .register(new LoggingFeature(Logger.getLogger(LOGGER_NAME), LoggingFeature.Verbosity.PAYLOAD_ANY))
                    .request()
                    .get();

            // Correct response status.
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            // Check logs for proper id.
            assertThat(getLoggingFilterLogRecord(getLoggedRecords()).get(1).getMessage(), containsString(ENTITY));
        }

        @Test
        public void testVerbosityAnyPayloadSetVerbosityAsText() throws Exception {
            final Response response = target()
                    .register(new LoggingFeature(Logger.getLogger(LOGGER_NAME)))
                    .property(LoggingFeature.LOGGING_FEATURE_VERBOSITY, "PAYLOAD_ANY")
                    .request()
                    .get();

            // Correct response status.
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            // Check logs for proper id.
            assertThat(getLoggingFilterLogRecord(getLoggedRecords()).get(1).getMessage(), containsString(ENTITY));
        }

        @Test
        public void testVerbosityTextPayloadBinaryFiltered() throws Exception {
            final Response response = target()
                    .register(new LoggingFeature(Logger.getLogger(LOGGER_NAME), LoggingFeature.Verbosity.PAYLOAD_TEXT))
                    .request()
                    .get();

            // Correct response status.
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            // Check logs for proper id.
            assertThat(getLoggingFilterLogRecord(getLoggedRecords()).get(1).getMessage(), not(containsString(ENTITY)));
        }

        @Test
        public void testVerbosityTextPayload() throws Exception {
            final Response response = target("/text")
                    .register(new LoggingFeature(Logger.getLogger(LOGGER_NAME), LoggingFeature.Verbosity.PAYLOAD_TEXT))
                    .request()
                    .get();

            // Correct response status.
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            // Check logs for proper id.
            assertThat(getLoggingFilterLogRecord(getLoggedRecords()).get(1).getMessage(), containsString(ENTITY));
        }

        @Test
        public void testVerbosityHeadersPayload() throws Exception {
            final Response response = target()
                    .register(new LoggingFeature(Logger.getLogger(LOGGER_NAME), LoggingFeature.Verbosity.HEADERS_ONLY))
                    .request()
                    .get();

            // Correct response status.
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            // Check logs for proper id.
            assertThat(getLoggingFilterLogRecord(getLoggedRecords()).get(1).getMessage(), not(containsString(ENTITY)));
        }

        @Test
        public void testPostedEntityLogged() throws Exception {
            final Response response = target("/text")
                    .register(new LoggingFeature(Logger.getLogger(LOGGER_NAME), LoggingFeature.Verbosity.PAYLOAD_TEXT))
                    .request()
                    .post(Entity.text(ENTITY));

            // Correct response status.
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            // Check logs for proper id.
            assertThat(getLoggingFilterLogRecord(getLoggedRecords()).get(0).getMessage(), containsString(ENTITY));
        }

    }

    /**
     * General client side tests.
     */
    public static class ContainerTest extends JerseyTest {

        @Override
        protected Application configure() {
            set(TestProperties.RECORD_LOG_LEVEL, Level.FINE.intValue());

            return new ResourceConfig(MyResource.class)
                    .register(LoggingFeature.class)
                    .property(LoggingFeature.LOGGING_FEATURE_LOGGER_NAME_SERVER, LOGGER_NAME);
        }

        @Test
        public void testLoggingAsContainer() throws Exception {
            // Correct response status.
            assertThat(target().request().get().getStatus(), is(Response.Status.OK.getStatusCode()));

            // Check logs for proper id.
            assertThat(getLoggingFilterRequestLogRecord(getLoggedRecords()).getMessage(), containsString("1 *"));
            assertThat(getLoggingFilterResponseLogRecord(getLoggedRecords()).getMessage(), containsString("1 *"));
        }

        @Test
        public void testLoggingAsContainerTextPayload() throws Exception {
            // Correct response status.
            assertThat(target("/text").request().get().getStatus(), is(Response.Status.OK.getStatusCode()));

            // Check logs for proper id.
            assertThat(getLoggingFilterRequestLogRecord(getLoggedRecords()).getMessage(), containsString("1 *"));
            assertThat(getLoggingFilterResponseLogRecord(getLoggedRecords()).getMessage(), containsString("1 *"));
            assertThat(getLoggingFilterLogRecord(getLoggedRecords()).get(1).getMessage(), containsString(ENTITY));
        }

        @Test
        public void testLoggingAsContainerBinaryPayload() throws Exception {
            // Correct response status.
            assertThat(target().request().get().getStatus(), is(Response.Status.OK.getStatusCode()));

            // Check logs for proper id.
            assertThat(getLoggingFilterRequestLogRecord(getLoggedRecords()).getMessage(), containsString("1 *"));
            assertThat(getLoggingFilterResponseLogRecord(getLoggedRecords()).getMessage(), containsString("1 *"));
            assertThat(getLoggingFilterLogRecord(getLoggedRecords()).get(1).getMessage(), not(containsString(ENTITY)));
        }

        @Test
        public void testPostedEntityLogged() throws Exception {
            final Response response = target("/text")
                    .request()
                    .post(Entity.text(ENTITY));

            // Correct response status.
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            // Check logs for proper id.
            assertThat(getLoggingFilterLogRecord(getLoggedRecords()).get(0).getMessage(), containsString(ENTITY));

        }
    }

    /**
     * General client side tests.
     */
    public static class ContainerAutodiscoveryTest extends JerseyTest {

        @Override
        protected Application configure() {
            set(TestProperties.RECORD_LOG_LEVEL, Level.INFO.intValue());

            return new ResourceConfig(MyResource.class)
                    .property(LoggingFeature.LOGGING_FEATURE_LOGGER_NAME_SERVER, LOGGER_NAME)
                    .property(LoggingFeature.LOGGING_FEATURE_LOGGER_LEVEL_SERVER, "INFO");
        }

        @Test
        public void testFilterAsContainerFilter() throws Exception {
            // Correct response status.
            assertThat(target().request().get().getStatus(), is(Response.Status.OK.getStatusCode()));

            // Check logs for proper id.
            assertThat(getLoggingFilterRequestLogRecord(getLoggedRecords()).getMessage(), containsString("1 *"));
            assertThat(getLoggingFilterResponseLogRecord(getLoggedRecords()).getMessage(), containsString("1 *"));
        }
    }

    private static LogRecord getLoggingFilterRequestLogRecord(final List<LogRecord> records) {
        return getLoggingFilterLogRecord(records, true);
    }

    private static LogRecord getLoggingFilterResponseLogRecord(final List<LogRecord> records) {
        return getLoggingFilterLogRecord(records, false);
    }

    private static LogRecord getLoggingFilterLogRecord(final List<LogRecord> records, final boolean requestQuery) {
        for (final LogRecord record : getLoggingFilterLogRecord(records)) {
            if (record.getMessage().contains(requestQuery ? "request" : "response")) {
                return record;
            }
        }

        throw new AssertionError("Unable to find proper log record.");
    }

    private static List<LogRecord> getLoggingFilterLogRecord(final List<LogRecord> records) {
        final List<LogRecord> loggingFilterRecords = new ArrayList<>(records.size());

        for (final LogRecord record : records) {
            if (record.getLoggerName().startsWith(LOGGER_NAME)) {
                loggingFilterRecords.add(record);
            }
        }

        return loggingFilterRecords;
    }

    public static class FiltersOrderTest extends JerseyTest {

        @Priority(1000)
        private static class CustomFilter implements ClientRequestFilter, ClientResponseFilter,
                                                     ContainerRequestFilter, ContainerResponseFilter {

            static final String CUSTOM_HEADER = "custom_header";

            @Override
            public void filter(final ClientRequestContext requestContext) throws IOException {
                requestContext.getHeaders().add(CUSTOM_HEADER, "client/request");
            }

            @Override
            public void filter(final ClientRequestContext requestContext, final ClientResponseContext responseContext)
                    throws IOException {
                responseContext.getHeaders().add(CUSTOM_HEADER, "client/response");
            }

            @Override
            public void filter(final ContainerRequestContext requestContext) throws IOException {
                requestContext.getHeaders().add(CUSTOM_HEADER, "container/request");
            }

            @Override
            public void filter(final ContainerRequestContext requestContext, final ContainerResponseContext responseContext)
                    throws IOException {
                responseContext.getHeaders().add(CUSTOM_HEADER, "container/response");
            }
        }

        @Override
        protected Application configure() {
            set(TestProperties.RECORD_LOG_LEVEL, Level.INFO.intValue());

            return new ResourceConfig(MyResource.class)
                    .property(LoggingFeature.LOGGING_FEATURE_LOGGER_NAME, LOGGER_NAME)
                    .property(LoggingFeature.LOGGING_FEATURE_LOGGER_LEVEL, "INFO")
                    .register(CustomFilter.class);
        }

        @Test
        public void testFilterAsContainerFilter() throws Exception {
            // Correct response status.
            assertThat(target()
                    .register(CustomFilter.class)
                    .register(new LoggingFeature(Logger.getLogger(LOGGER_NAME),
                            Level.INFO,
                            LoggingFeature.Verbosity.HEADERS_ONLY,
                            0))
                    .request().get().getStatus(), is(Response.Status.OK.getStatusCode()));

            for (LogRecord record : getLoggedRecords()) {
                System.out.println(record.getMessage());
            }

            // --- client request log entry
            // client added header before request has sent (and logged)
            assertThat(getLoggedRecords().get(0).getMessage(),
                    containsString("1 > custom_header: client/request\n"));


            // --- container request log entry
            // container receives header from client request
            assertThat(getLoggedRecords().get(1).getMessage(),
                    containsString("1 > custom_header: client/request\n"));
            // container has added its own header after logging filter logged message
            assertThat(getLoggedRecords().get(1).getMessage(),
                    not(containsString("1 > custom_header: container/request\n")));


            // --- container response log entry
            // container added header to the response and it was logged
            assertThat(getLoggedRecords().get(2).getMessage(),
                    containsString("1 < custom_header: container/response\n"));

            // --- client response log entry
            // client received header
            assertThat(getLoggedRecords().get(3).getMessage(),
                    containsString("1 < custom_header: container/response\n"));
            assertThat(getLoggedRecords().get(3).getMessage(),
                    not(containsString("1 < custom_header: client/response\n")));

        }

    }
}
