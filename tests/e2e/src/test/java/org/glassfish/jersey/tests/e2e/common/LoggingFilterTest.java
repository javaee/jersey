/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2015 Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;

/**
 * {@link LoggingFilter} end-to-end tests.
 *
 * @author Michal Gajdos
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
        LoggingFilterTest.ClientTest.class,
        LoggingFilterTest.ContainerTest.class,
        LoggingFilterTest.ContainerRequestFilterTest.class,
        LoggingFilterTest.ContainerResponseFilterTest.class
})
public class LoggingFilterTest {

    @Path("/")
    public static class MyResource {

        @GET
        public Response getHeaders() {
            return Response
                    .ok("ok")
                    .header("001", "First Header Value")
                    .header("002", "Second Header Value")
                    .header("003", "Third Header Value")
                    .header("004", "Fourth Header Value")
                    .header("005", "Fifth Header Value")
                    .build();
        }
    }

    /**
     * General client side tests.
     */
    public static class ClientTest extends JerseyTest {

        @Override
        protected Application configure() {
            set(TestProperties.RECORD_LOG_LEVEL, Level.INFO.intValue());

            return new ResourceConfig(MyResource.class);
        }

        @Test
        public void testFilterAsClientRequestFilter() throws Exception {
            final Response response = target()
                    .register(LoggingFilter.class, ClientRequestFilter.class)
                    .request()
                    .get();

            // Correct response status.
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            // Check logs for proper id.
            assertThat(getLoggingFilterRequestLogRecord(getLoggedRecords()).getMessage(), containsString("1 *"));
        }

        @Test
        public void testFilterAsClientResponseFilter() throws Exception {
            final Response response = target()
                    .register(LoggingFilter.class, ClientResponseFilter.class)
                    .request()
                    .get();

            // Correct response status.
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            // Check logs for proper id.
            assertThat(getLoggingFilterResponseLogRecord(getLoggedRecords()).getMessage(), containsString("1 *"));
        }

        @Test
        public void testOrderOfHeadersOnClient() throws Exception {
            final Response response = target().register(LoggingFilter.class).request().get();
            assertThat(response.readEntity(String.class), equalTo("ok"));

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
    }

    /**
     * General client side tests.
     */
    public static class ContainerTest extends JerseyTest {

        @Override
        protected Application configure() {
            set(TestProperties.RECORD_LOG_LEVEL, Level.INFO.intValue());

            return new ResourceConfig(MyResource.class).register(LoggingFilter.class);
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

    /**
     * Container tests where logging filter is registered only as container response filter.
     */
    public static class ContainerResponseFilterTest extends JerseyTest {

        @Override
        protected Application configure() {
            set(TestProperties.RECORD_LOG_LEVEL, Level.INFO.intValue());

            return new ResourceConfig(MyResource.class)
                    .register(LoggingFilter.class, ContainerResponseFilter.class);
        }

        @Test
        public void testFilterAsContainerResponseFilter() throws Exception {
            // Correct response status.
            assertThat(target().request().get().getStatus(), is(Response.Status.OK.getStatusCode()));

            // Check logs for proper id.
            assertThat(getLoggingFilterResponseLogRecord(getLoggedRecords()).getMessage(), containsString("1 *"));
        }
    }

    /**
     * Container tests where logging filter is registered only as container request filter.
     */
    public static class ContainerRequestFilterTest extends JerseyTest {

        @Override
        protected Application configure() {
            set(TestProperties.RECORD_LOG_LEVEL, Level.INFO.intValue());

            return new ResourceConfig(MyResource.class)
                    .register(LoggingFilter.class, ContainerRequestFilter.class);
        }

        @Test
        public void testFilterAsContainerRequestFilter() throws Exception {
            // Correct response status.
            assertThat(target().request().get().getStatus(), is(Response.Status.OK.getStatusCode()));

            // Check logs for proper id.
            assertThat(getLoggingFilterRequestLogRecord(getLoggedRecords()).getMessage(), containsString("1 *"));
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
            if (LoggingFilter.class.getName().equals(record.getLoggerName())) {
                loggingFilterRecords.add(record);
            }
        }

        return loggingFilterRecords;
    }
}
