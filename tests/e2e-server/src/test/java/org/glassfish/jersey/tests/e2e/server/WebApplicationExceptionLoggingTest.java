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

package org.glassfish.jersey.tests.e2e.server;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import javax.validation.ValidationException;
import javax.validation.constraints.NotNull;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests that {@link WebApplicationException} is logged on the correct level.
 *
 * @author Miroslav Fuksa
 */
public class WebApplicationExceptionLoggingTest extends JerseyTest {

    @Path("/test")
    public static class StatusResource {

        @GET
        @Produces("text/plain")
        public String test(@NotNull @QueryParam("id") final String id) {
            return "ok";
        }

        @GET
        @Path("WAE-no-entity")
        @Produces("text/plain")
        public String testWithoutEntity() {
            throw new WebApplicationException("WAE without entity", Response.status(400).build());
        }

        @GET
        @Path("WAE-entity")
        @Produces("text/plain")
        public String testWithEntity() {
            throw new WebApplicationException("WAE with entity", Response.status(400).entity("WAE with entity").build());
        }
    }

    @Provider
    public static class ValidationExceptionMapper implements ExceptionMapper<ValidationException> {

        @Override
        public Response toResponse(final ValidationException ex) {
            return Response.status(200).entity("Error mapped: " + ex.toString()).type("text/plain").build();
        }
    }

    @Override
    protected Application configure() {
        set(TestProperties.RECORD_LOG_LEVEL, Level.FINER.intValue());

        return new ResourceConfig(StatusResource.class, ValidationExceptionMapper.class);
    }

    private LogRecord getLogRecord(final String messagePrefix) {
        for (final LogRecord logRecord : getLoggedRecords()) {
            if (logRecord.getMessage() != null && logRecord.getMessage().startsWith(messagePrefix)) {
                return logRecord;
            }
        }
        return null;
    }

    @Test
    public void testValidationException() {
        final Response response = target().path("test").request().get();
        assertEquals(200, response.getStatus());

        final String entity = response.readEntity(String.class);
        assertTrue(entity.startsWith("Error mapped:"));

        // check logs
        final LogRecord logRecord = this.getLogRecord("Starting mapping of the exception");
        assertNotNull(logRecord);
        assertEquals(Level.FINER, logRecord.getLevel());

        // check that there is no exception logged on the level higher than FINE
        for (final LogRecord record : getLoggedRecords()) {
            if (record.getThrown() != null) {
                assertTrue(record.getLevel().intValue() <= Level.FINE.intValue());
            }
        }

    }

    @Test
    public void testWAEWithEntity() {
        final Response response = target().path("test/WAE-entity").request().get();
        assertEquals(400, response.getStatus());
        final String entity = response.readEntity(String.class);
        assertEquals("WAE with entity", entity);

        // check logs
        LogRecord logRecord = this.getLogRecord("Starting mapping of the exception");
        assertNotNull(logRecord);
        assertEquals(Level.FINER, logRecord.getLevel());

        logRecord = this.getLogRecord("WebApplicationException (WAE) with non-null entity thrown.");
        assertNotNull(logRecord);
        assertEquals(Level.FINE, logRecord.getLevel());
        assertTrue(logRecord.getThrown() instanceof WebApplicationException);
        logRecord.getThrown().printStackTrace();
    }

    @Test
    public void testWAEWithoutEntity() {
        final Response response = target().path("test/WAE-no-entity").request().get();
        assertEquals(400, response.getStatus());
        assertFalse(response.hasEntity());

        // check logs
        LogRecord logRecord = this.getLogRecord("Starting mapping of the exception");
        assertNotNull(logRecord);
        assertEquals(Level.FINER, logRecord.getLevel());

        logRecord = this.getLogRecord("WebApplicationException (WAE) with no entity thrown and no");
        assertNotNull(logRecord);
        assertEquals(Level.FINE, logRecord.getLevel());
        assertTrue(logRecord.getThrown() instanceof WebApplicationException);
        logRecord.getThrown().printStackTrace();
    }
}
