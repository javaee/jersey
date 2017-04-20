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

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.internal.LocalizationMessages;
import org.glassfish.jersey.spi.ExtendedExceptionMapper;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;

import org.junit.Assert;
import org.junit.Test;
import static org.junit.Assert.fail;

/**
 * @author Miroslav Fuksa
 *
 */
public class ExtendedExceptionMapperTest extends JerseyTest {

    @Override
    protected Application configure() {
        set(TestProperties.RECORD_LOG_LEVEL, Level.FINE.intValue());

        return new ResourceConfig(
                TestResource.class,
                ExceptionMapperA.class,
                ExceptionMapperB.class,
                ExceptionMapperC.class,
                ExceptionMapperD.class,
                ExceptionMapperE.class,
                ExceptionMapperX.class
        );
    }

    public static class ExceptionA extends RuntimeException {
        public ExceptionA(String message) {
            super(message);
        }
    }

    public static class ExceptionB extends ExceptionA {
        public ExceptionB(String message) {
            super(message);
        }
    }

    public static class ExceptionC extends ExceptionB {
        public ExceptionC(String message) {
            super(message);
        }
    }

    public static class ExceptionD extends ExceptionC {
        public ExceptionD(String message) {
            super(message);
        }
    }

    public static class ExceptionE extends ExceptionD {
        public ExceptionE(String message) {
            super(message);
        }
    }

    public static class ExceptionX extends RuntimeException {
        public ExceptionX(String message) {
            super(message);
        }
    }


    public static class ExceptionMapperA implements ExtendedExceptionMapper<ExceptionA> {

        @Override
        public boolean isMappable(ExceptionA exception) {
            return exception.getMessage().substring(2).contains("A");
        }

        @Override
        public Response toResponse(ExceptionA exception) {
            return Response.ok("A").build();
        }
    }

    public static class ExceptionMapperB implements ExtendedExceptionMapper<ExceptionB> {

        @Override
        public boolean isMappable(ExceptionB exception) {
            return exception.getMessage().substring(2).contains("B");
        }

        @Override
        public Response toResponse(ExceptionB exception) {
            return Response.ok("B").build();
        }
    }

    public static class ExceptionMapperC implements ExceptionMapper<ExceptionC> {

        @Override
        public Response toResponse(ExceptionC exception) {
            return Response.ok("C").build();
        }
    }

    public static class ExceptionMapperD implements ExtendedExceptionMapper<ExceptionD> {

        @Override
        public boolean isMappable(ExceptionD exception) {
            return exception.getMessage().substring(2).contains("D");
        }

        @Override
        public Response toResponse(ExceptionD exception) {
            return Response.ok("D").build();
        }
    }

    public static class ExceptionMapperE implements ExtendedExceptionMapper<ExceptionE> {

        @Override
        public boolean isMappable(ExceptionE exception) {
            return exception.getMessage().substring(2).contains("E");
        }

        @Override
        public Response toResponse(ExceptionE exception) {
            return Response.ok("E").build();
        }
    }

    public static class ExceptionMapperX implements ExtendedExceptionMapper<ExceptionX> {

        @Override
        public boolean isMappable(ExceptionX exception) {
            return exception.getMessage().substring(2).contains("X");
        }

        @Override
        public Response toResponse(ExceptionX exception) {
            return Response.ok("X").build();
        }
    }

    @Path("resource")
    public static class TestResource {
        @POST
        public String post(String e) {
            if (e.charAt(0) == 'A') {
                throw new ExceptionA(String.valueOf(e));
            } else if (e.charAt(0) == 'B') {
                throw new ExceptionB(String.valueOf(e));
            } else if (e.charAt(0) == 'C') {
                throw new ExceptionC(String.valueOf(e));
            } else if (e.charAt(0) == 'D') {
                throw new ExceptionD(String.valueOf(e));
            } else if (e.charAt(0) == 'E') {
                throw new ExceptionE(String.valueOf(e));
            } else if (e.charAt(0) == 'X') {
                throw new ExceptionX(String.valueOf(e));
            }
            return "get";
        }
    }

    @Test
    public void test() {
        // Format of first param: [exception thrown]-[exception mappers which will return true in isMappable]
        _test("A-A", "A");
        _test("A-AB", "A");
        _test("A-ABC", "A");
        _test("A-ABCD", "A");
        _test("A-ABCDE", "A");
        _test("A-ABCDEX", "A");
        _test("A-C", null);
        _test("A-D", null);
        _test("A-E", null);
        _test("A-D", null);
        _test("A-BCDEX", null);
        _test("A-00000", null);
        _test("A-X", null);


        _test("B-A", "A");
        _test("B-B", "B");
        _test("B-AB", "B");
        _test("B-ABC", "B");
        _test("B-ABCD", "B");
        _test("B-ABCDE", "B");
        _test("B-ABCDEX", "B");
        _test("B-C", null);
        _test("B-D", null);
        _test("B-E", null);
        _test("B-CDEX", null);
        _test("B-X", null);
        _test("B-000", null);

        // C is not an ExtendedExceptionMapper but just ExceptionMapper (always mappable)
        _test("C-C", "C");
        _test("C-A", "C");
        _test("C-AB", "C");
        _test("C-ABC", "C");
        _test("C-AEX", "C");
        _test("C-00000", "C");
        _test("C-ABCDEX", "C");
        _test("C-E", "C");
        _test("C-DE", "C");
        _test("C-D", "C");
        _test("C-X", "C");

        _test("D-000", "C");
        _test("D-A", "C");
        _test("D-B", "C");
        _test("D-C", "C");
        _test("D-D", "D");
        _test("D-E", "C");
        _test("D-ABC", "C");
        _test("D-ABCEX", "C");
        _test("D-ABCDEX", "D");
        _test("D-DE", "D");
        _test("D-ABEX", "C");
        _test("D-AEX", "C");
        _test("D-X", "C");

        _test("E-A", "C");
        _test("E-B", "C");
        _test("E-C", "C");
        _test("E-D", "D");
        _test("E-E", "E");
        _test("E-ABC", "C");
        _test("E-ABCD", "D");
        _test("E-ABCDE", "E");
        _test("E-ABCDEX", "E");
        _test("E-DE", "E");
        _test("E-X", "C");
        _test("E-000000", "C");

        _test("X-A", null);
        _test("X-ABCDE", null);
        _test("X-ABCDEX", "X");
        _test("X-X", "X");

        // Check logs. (??)
        for (final LogRecord logRecord : getLoggedRecords()) {

            // TODO: this test is fragile.
            if (logRecord.getLoggerName().contains("ClientExecutorProvidersConfigurator")) {
                continue;
            }

            for (final String message : new String[]{
                    LocalizationMessages.ERROR_EXCEPTION_MAPPING_ORIGINAL_EXCEPTION(),
                    LocalizationMessages.ERROR_EXCEPTION_MAPPING_THROWN_TO_CONTAINER()}) {

                if (logRecord.getMessage().contains(message) && logRecord.getLevel().intValue() > Level.FINE.intValue()) {
                    fail("Log message should be logged at lower (FINE) level: " + message);
                }
            }
        }
    }

    private void _test(String input, String expectedMapper) {
        final Response response = target("resource").request().post(Entity.entity(input, MediaType.TEXT_PLAIN_TYPE));
        if (expectedMapper == null) {
            Assert.assertEquals(500, response.getStatus());
        } else {
            Assert.assertEquals(200, response.getStatus());
            Assert.assertEquals(expectedMapper, response.readEntity(String.class));
        }
    }


}
