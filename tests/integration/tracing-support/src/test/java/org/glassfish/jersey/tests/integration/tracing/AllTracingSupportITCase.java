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

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.TracingConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.glassfish.jersey.test.external.ExternalTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * 'ALL' tracing support test that is running in external Jetty container.
 *
 * @author Libor Kramolis (libor.kramolis at oracle.com)
 */
public class AllTracingSupportITCase extends JerseyTest {

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
    public void testRuntimeException() {
        Invocation.Builder builder = resource("/root").path("runtime-exception").request();

        Response response = builder.get();
        test(response);
        assertEquals(500, response.getStatus());
    }

    @Test
    public void testMappedException() throws InterruptedException, IOException {
        Invocation.Builder builder = resource("/root").path("mapped-exception").request();

        Response response = builder.get();
        test(response);
        assertEquals(501, response.getStatus());
    }

    @Test
    public void testPost() throws IOException {
        Invocation.Builder builder = resource("/root").request();

        Response response = builder.post(Entity.entity(new Message("POST"), Utils.APPLICATION_X_JERSEY_TEST));
        test(response);
        assertEquals("TSOP", response.readEntity(Message.class).getText());
    }

    @Test
    public void testGet405() {
        Invocation.Builder builder = resource("/root").request();

        Response response = builder.get();
        test(response);
        assertEquals(405, response.getStatus());
    }

    @Test
    public void testPostSubResourceMethod() {
        Invocation.Builder builder = resource("/root").path("sub-resource-method").request();

        Response response = builder.post(Entity.entity(new Message("POST"), Utils.APPLICATION_X_JERSEY_TEST));
        test(response);
        assertEquals("TSOP", response.readEntity(Message.class).getText());
    }

    @Test
    public void testPostSubResourceLocator() {
        Invocation.Builder builder = resource("/root").path("sub-resource-locator").request();

        Response response = builder.post(Entity.entity(new Message("POST"), Utils.APPLICATION_X_JERSEY_TEST));
        test(response);
        assertEquals("TSOP", response.readEntity(Message.class).getText());
    }

    @Test
    public void testPostSubResourceLocatorNull() {
        Invocation.Builder builder = resource("/root").path("sub-resource-locator-null").request();

        Response response = builder.post(Entity.entity(new Message("POST"), Utils.APPLICATION_X_JERSEY_TEST));
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testPostSubResourceLocatorSubResourceMethod() {
        Invocation.Builder builder = resource("/root").path("sub-resource-locator").path("sub-resource-method").request();

        Response response = builder.post(Entity.entity(new Message("POST"), Utils.APPLICATION_X_JERSEY_TEST));
        test(response);
        assertEquals("TSOP", response.readEntity(Message.class).getText());
    }

    @Test
    public void testTraceInnerException() {
        testTraceInnerExceptionImpl(Utils.TestAction.PRE_MATCHING_REQUEST_FILTER_THROW_WEB_APPLICATION, 500);
        testTraceInnerExceptionImpl(Utils.TestAction.PRE_MATCHING_REQUEST_FILTER_THROW_PROCESSING, 500);
        testTraceInnerExceptionImpl(Utils.TestAction.PRE_MATCHING_REQUEST_FILTER_THROW_ANY, 500);
    }

    private void testTraceInnerExceptionImpl(Utils.TestAction testAction, int expectedStatus) {
        Invocation.Builder builder = resource("/root").path(testAction != null ? testAction.name() : "NAME").request();
        if (testAction != null) {
            builder.header(Utils.HEADER_TEST_ACTION, testAction);
        }

        Response response = builder.get();
        test(response);
        assertEquals(expectedStatus, response.getStatus());
    }

    @Test
    public void testGet() {
        Invocation.Builder builder = resource("/root").path("NAME").request();
        Response response = builder.get();
        test(response);
        assertEquals(200, response.getStatus());
    }

    //
    // utils
    //

    private void test(Response response) {
        assertTrue(hasX_Jersey_Trace(response));
    }

    private boolean hasX_Jersey_Trace(Response response) {
        for (String k : response.getHeaders().keySet()) {
            if (k.startsWith(Utils.HEADER_TRACING_PREFIX)) {
                return true;
            }
        }

        return false;
    }

    private WebTarget resource(String path) {
        return target("/" + TracingConfig.ALL).path(path);
    }

}
