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
package org.glassfish.jersey.tests.integration.jersey2176;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.external.ExternalTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;

import org.junit.Assert;
import org.junit.Test;

/**
 * Reproducer tests for JERSEY-2176.
 *
 * @author Libor Kramolis (libor.kramolis at oracle.com)
 */
public abstract class Jersey2176ITCaseBase extends JerseyTest {

    @Override
    protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
        return new ExternalTestContainerFactory();
    }

    @Test
    public void testGetContent222() {
        testGetContent(222, true);
    }

    @Test
    public void testGetContent333() {
        testGetContent(333, true);
    }

    @Test
    public void testGetContent444() {
        testGetContent(444, true);
    }

    @Test
    public void testGetContent555() {
        testGetContent(555, true);
    }

    @Test
    public void testGetContent222NoResponseEntity() {
        testGetContent(222, false);
    }

    @Test
    public void testGetContent333NoResponseEntity() {
        testGetContent(333, false);
    }

    @Test
    public void testGetContent444NoResponseEntity() {
        testGetContent(444, false);
    }

    @Test
    public void testGetContent555NoResponseEntity() {
        testGetContent(555, false);
    }

    @Test
    public void testGetException_1() {
        testGetException(-1, 500, false);
    }

    @Test
    public void testGetException_2() {
        testGetException(-2, 500, false);
    }

    @Test
    public void testGetException_3() {
        testGetException(-3, 321, false);
    }

    @Test
    public void testGetException_4() {
        testGetException(-4, 432, false);
    }

    @Test
    public void testGetException222() {
        testGetException(222, 500, true);
    }

    private void testGetContent(int uc, boolean responseEntity) {
        String expectedContent = "ENTITY";
        expectedContent = "[INTERCEPTOR]" + expectedContent + "[/INTERCEPTOR]";
        expectedContent = "[FILTER]" + expectedContent + "[/FILTER]";

        Invocation.Builder builder = target().path("/resource/" + uc).request();
        if (responseEntity) {
            builder.header(Issue2176ReproducerResource.X_RESPONSE_ENTITY_HEADER, true);
        } else {
            builder.header(TraceResponseFilter.X_NO_FILTER_HEADER, true);
        }

        final Response response = builder.get();
        final String assertMessage = uc + "|" + responseEntity;

        Assert.assertEquals(assertMessage, uc, response.getStatus());
        if (!sendErrorExpected(uc, responseEntity)) {
            Assert.assertEquals(assertMessage, "OK", response.getHeaderString(TraceResponseFilter.X_STATUS_HEADER));
            Assert.assertNotNull(assertMessage, response.getHeaderString(TraceResponseFilter.X_SERVER_DURATION_HEADER));
            if (responseEntity) {
                Assert.assertEquals(assertMessage, expectedContent, response.readEntity(String.class));
                Assert.assertEquals(assertMessage, String.valueOf(expectedContent.length()),
                        response.getHeaderString(HttpHeaders.CONTENT_LENGTH));
            }
        } else {
            Assert.assertNull(assertMessage, response.getHeaderString(TraceResponseFilter.X_STATUS_HEADER));
            Assert.assertNull(assertMessage, response.getHeaderString(TraceResponseFilter.X_SERVER_DURATION_HEADER));
        }
    }

    private void testGetException(int uc, int expectedStatus, boolean fail) {
        Invocation.Builder builder = target().path("/resource/" + uc).request();
        builder = builder.header(Issue2176ReproducerResource.X_RESPONSE_ENTITY_HEADER, true);
        if (fail) {
            builder = builder.header(Issue2176ReproducerResource.X_FAIL_HEADER, true);
        }

        final Response response = builder.get();

        final String expectedContent = "[FILTER][/FILTER]";
        final String assertMessage = uc + ":" + expectedStatus + "|" + fail;

        Assert.assertEquals(assertMessage, expectedStatus, response.getStatus());
        if (!sendErrorExpected(expectedStatus, false)) {
            Assert.assertEquals(assertMessage, expectedStatus == 500 ? "FAIL" : "OK",
                    response.getHeaderString(TraceResponseFilter.X_STATUS_HEADER));
            Assert.assertNotNull(assertMessage, response.getHeaderString(TraceResponseFilter.X_SERVER_DURATION_HEADER));
            Assert.assertEquals(assertMessage, String.valueOf(expectedContent.length()),
                    response.getHeaderString(HttpHeaders.CONTENT_LENGTH));
        } else {
            Assert.assertNull(assertMessage, response.getHeaderString(TraceResponseFilter.X_STATUS_HEADER));
            Assert.assertNull(assertMessage, response.getHeaderString(TraceResponseFilter.X_SERVER_DURATION_HEADER));
        }
    }

    private boolean sendErrorExpected(final int uc, final boolean responseEntity) {
        return !((Jersey2176App) configure()).isSetStatusOverSendError() && (uc >= 400) && !responseEntity;
    }

}
