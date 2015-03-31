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
package org.glassfish.jersey.tests.integration.tracing;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.TracingConfig;
import org.glassfish.jersey.server.internal.ServerTraceEvent;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.glassfish.jersey.test.external.ExternalTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import static org.junit.Assert.assertEquals;

/**
 * 'ALL' tracing support test that is running in external Jetty container.
 *
 * @author Libor Kramolis (libor.kramolis at oracle.com)
 */
@RunWith(Parameterized.class)
public class AllTracingSupportITCase extends JerseyTest {

    private final String resourcePath;

    public AllTracingSupportITCase(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    @Parameterized.Parameters(name = "{index}: {0}")
    public static List<Object[]> testData() {
        return Arrays.asList(new Object[][] {
                {"/root"},
                {"/async"},
        });
    }

    //
    // JerseyTest
    //

    @Override
    protected ResourceConfig configure() {
        enable(TestProperties.LOG_TRAFFIC);
        enable(TestProperties.DUMP_ENTITY);

        return new AllTracingSupport();
    }

    @Override
    protected void configureClient(ClientConfig clientConfig) {
        Utils.configure(clientConfig);
    }

    @Override
    protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
        return new ExternalTestContainerFactory();
    }

    //
    // tests
    //

    @Test
    public void testGet() {
        Invocation.Builder builder = resource(resourcePath).path("NAME").request();
        Response response = builder.get();
        assertXJerseyTrace(response, false);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testRuntimeException() {
        Invocation.Builder builder = resource(resourcePath).path("runtime-exception").request();

        Response response = builder.get();
        assertXJerseyTrace(response, true);
        assertEquals(500, response.getStatus());
    }

    @Test
    public void testMappedException() throws InterruptedException, IOException {
        Invocation.Builder builder = resource(resourcePath).path("mapped-exception").request();

        Response response = builder.get();
        assertXJerseyTrace(response, true);
        assertEquals(501, response.getStatus());
    }

    @Test
    public void testGet405() {
        Invocation.Builder builder = resource(resourcePath).request();

        Response response = builder.get();
        assertXJerseyTrace(response, false);
        assertEquals(405, response.getStatus());
    }

    @Test
    public void testPostSubResourceMethod() {
        Invocation.Builder builder = resource(resourcePath).path("sub-resource-method").request();

        Response response = builder.post(Entity.entity(new Message("POST"), Utils.APPLICATION_X_JERSEY_TEST));
        assertXJerseyTrace(response, false);
        assertEquals("TSOP", response.readEntity(Message.class).getText());
    }

    @Test
    public void testPostSubResourceLocator() {
        Invocation.Builder builder = resource(resourcePath).path("sub-resource-locator").request();

        Response response = builder.post(Entity.entity(new Message("POST"), Utils.APPLICATION_X_JERSEY_TEST));
        assertXJerseyTrace(response, false);
        assertEquals("TSOP", response.readEntity(Message.class).getText());
    }

    @Test
    public void testPostSubResourceLocatorNull() {
        Invocation.Builder builder = resource(resourcePath).path("sub-resource-locator-null").request();

        Response response = builder.post(Entity.entity(new Message("POST"), Utils.APPLICATION_X_JERSEY_TEST));
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testPostSubResourceLocatorSubResourceMethod() {
        Invocation.Builder builder = resource(resourcePath).path("sub-resource-locator").path("sub-resource-method").request();

        Response response = builder.post(Entity.entity(new Message("POST"), Utils.APPLICATION_X_JERSEY_TEST));
        assertXJerseyTrace(response, false);
        assertEquals("TSOP", response.readEntity(Message.class).getText());
    }

    @Test
    public void testTraceInnerException() {
        // PRE_MATCHING_REQUEST_FILTER
        testTraceInnerExceptionImpl(Utils.TestAction.PRE_MATCHING_REQUEST_FILTER_THROW_WEB_APPLICATION, 500, false);
        testTraceInnerExceptionImpl(Utils.TestAction.PRE_MATCHING_REQUEST_FILTER_THROW_PROCESSING, 500, true);
        testTraceInnerExceptionImpl(Utils.TestAction.PRE_MATCHING_REQUEST_FILTER_THROW_ANY, 500, true);
        // MESSAGE_BODY_WRITER
        testTraceInnerExceptionImpl(Utils.TestAction.MESSAGE_BODY_WRITER_THROW_WEB_APPLICATION, 500, false);
        testTraceInnerExceptionImpl(Utils.TestAction.MESSAGE_BODY_WRITER_THROW_PROCESSING, 500, true);
        testTraceInnerExceptionImpl(Utils.TestAction.MESSAGE_BODY_WRITER_THROW_ANY, 500, true);
        // MESSAGE_BODY_READER
        testTraceInnerExceptionImpl(Utils.TestAction.MESSAGE_BODY_READER_THROW_WEB_APPLICATION, 500, false);
        testTraceInnerExceptionImpl(Utils.TestAction.MESSAGE_BODY_READER_THROW_PROCESSING, 500, true);
        testTraceInnerExceptionImpl(Utils.TestAction.MESSAGE_BODY_READER_THROW_ANY, 500, true);
    }

    //
    // utils
    //

    private void testTraceInnerExceptionImpl(Utils.TestAction testAction, int expectedStatus, boolean exceptionExpected) {
        Invocation.Builder builder = resource(resourcePath).request();
        builder.header(Utils.HEADER_TEST_ACTION, testAction);

        Response response = builder.post(Entity.entity(new Message(testAction.name()), Utils.APPLICATION_X_JERSEY_TEST));
        assertXJerseyTrace(response, exceptionExpected);
        assertEquals(expectedStatus, response.getStatus());
    }

    private void assertXJerseyTrace(Response response, boolean exceptionExpected) {
        int finished = 0;
        int exceptionMapping = 0;

        for (String k : response.getHeaders().keySet()) {
            if (k.startsWith(Utils.HEADER_TRACING_PREFIX)) {
                String value = response.getHeaderString(k);
                if (value.startsWith(ServerTraceEvent.FINISHED.category())) {
                    finished++;
                } else if (value.startsWith(ServerTraceEvent.EXCEPTION_MAPPING.category())) {
                    exceptionMapping++;
                }
            }
        }
        assertEquals("Just one FINISHED expected!", 1, finished);
        if (exceptionExpected) {
            assertEquals("EXCEPTION expected!", 1, exceptionMapping);
        } else {
            assertEquals("EXCEPTION NOT expected!", 0, exceptionMapping);
        }
    }

    private WebTarget resource(String path) {
        return target("/" + TracingConfig.ALL).path(path);
    }

}
