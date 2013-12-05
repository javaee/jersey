/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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

import java.util.logging.Level;
import java.util.logging.LogRecord;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;

import org.junit.Test;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;

/**
 * @author Michal Gajdos (michal.gajdos at oracle.com)
 */
public class LoggingFilterTest extends JerseyTest {

    @Override
    protected Application configure() {
        set(TestProperties.RECORD_LOG_LEVEL, Level.INFO.intValue());

        return new ResourceConfig(MyResource.class);
    }

    @Override
    protected void configureClient(final ClientConfig config) {
        config.register(LoggingFilter.class);
    }

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

    @Test
    public void testOrderOfHeadersOnClient() throws Exception {
        final Response response = target().request().get();
        assertThat(response.readEntity(String.class), equalTo("ok"));

        final LogRecord record = getLoggingFilterResponseLogRecord();
        final String message = record.getMessage();

        int i = 1;
        do {
            final String h1 = "00" + i++;
            final String h2 = "00" + i;

            int i1 = message.indexOf(h1);
            int i2 = message.indexOf(h2);

            assertThat("Header " + h1 + " has been logged sooner than header " + h2, i1, lessThan(i2));
        } while (i < 5);
    }

    private LogRecord getLoggingFilterResponseLogRecord() {
        for (final LogRecord record : getLoggedRecords()) {
            if(LoggingFilter.class.getName().equals(record.getLoggerName())
                    && record.getMessage().contains("Response")) {
                return record;
            }
        }
        return null;
    }
}
