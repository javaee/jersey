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

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.message.internal.TracingLogger;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.TracingConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.glassfish.jersey.test.external.ExternalTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 'ON_DEMAND' tracing support test that is running in external Jetty container.
 *
 * @author Libor Kramolis (libor.kramolis at oracle.com)
 */
public class OnDemandTracingSupportITCase extends JerseyTest {

    //
    // JerseyTest
    //

    @Override
    protected ResourceConfig configure() {
        enable(TestProperties.LOG_TRAFFIC);
        enable(TestProperties.DUMP_ENTITY);

        return new OnDemandTracingSupport();
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
    public void testNoTracing() {
        Invocation.Builder builder = resource("/root").request();
        Response response = builder.post(Entity.entity(new Message("POST"), Utils.APPLICATION_X_JERSEY_TEST));
        assertFalse(hasX_Jersey_Trace(response));
        assertEquals("TSOP", response.readEntity(Message.class).getText());
    }

    @Test
    public void testTraceAcceptEmpty() {
        testTraceAccept("");
    }

    @Test
    public void testTraceAcceptTrue() {
        testTraceAccept("true");
    }

    @Test
    public void testTraceAcceptWhatever() {
        testTraceAccept("whatever");
    }

    private void testTraceAccept(String acceptValue) {
        Invocation.Builder builder = resource("/root").request();
        Response response = builder.header(TracingLogger.HEADER_ACCEPT, acceptValue)
                .post(Entity.entity(new Message("POST"), Utils.APPLICATION_X_JERSEY_TEST));
        assertTrue(hasX_Jersey_Trace(response));
        assertEquals("TSOP", response.readEntity(Message.class).getText());
    }

    //
    // utils
    //

    private boolean hasX_Jersey_Trace(Response response) {
        for (String k : response.getHeaders().keySet()) {
            if (k.startsWith(Utils.HEADER_TRACING_PREFIX)) {
                return true;
            }
        }
        return false;
    }

    private WebTarget resource(String path) {
        return target("/" + TracingConfig.ON_DEMAND).path(path);
    }

}
