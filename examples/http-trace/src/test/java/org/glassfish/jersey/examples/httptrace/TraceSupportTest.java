/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package org.glassfish.jersey.examples.httptrace;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.grizzly.connector.GrizzlyConnectorProvider;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TraceSupportTest extends JerseyTest {

    @Override
    protected ResourceConfig configure() {
        enable(TestProperties.DUMP_ENTITY);
        enable(TestProperties.LOG_TRAFFIC);
        return App.create();
    }

    private String[] expectedFragmentsProgrammatic = new String[]{
            "TRACE http://localhost:" + this.getPort() + "/tracing/programmatic"
    };
    private String[] expectedFragmentsAnnotated = new String[]{
            "TRACE http://localhost:" + this.getPort() + "/tracing/annotated"
    };

    private WebTarget prepareTarget(String path) {
        final WebTarget target = target();
        target.register(LoggingFeature.class);
        return target.path(path);
    }

    @Test
    public void testProgrammaticApp() throws Exception {
        Response response = prepareTarget(App.ROOT_PATH_PROGRAMMATIC).request("text/plain").method(TRACE.NAME);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusInfo().getStatusCode());

        String responseEntity = response.readEntity(String.class);
        for (String expectedFragment : expectedFragmentsProgrammatic) {
            assertTrue("Expected fragment '" + expectedFragment + "' not found in response:\n" + responseEntity,
                    // toLowerCase - http header field names are case insensitive
                    responseEntity.contains(expectedFragment));
        }
    }

    @Test
    public void testAnnotatedApp() throws Exception {
        Response response = prepareTarget(App.ROOT_PATH_ANNOTATED).request("text/plain").method(TRACE.NAME);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusInfo().getStatusCode());

        String responseEntity = response.readEntity(String.class);
        for (String expectedFragment : expectedFragmentsAnnotated) {
            assertTrue("Expected fragment '" + expectedFragment + "' not found in response:\n" + responseEntity,
                    // toLowerCase - http header field names are case insensitive
                    responseEntity.contains(expectedFragment));
        }
    }

    @Test
    public void testTraceWithEntity() throws Exception {
        _testTraceWithEntity(false, false);
    }

    @Test
    public void testAsyncTraceWithEntity() throws Exception {
        _testTraceWithEntity(true, false);
    }

    @Test
    public void testTraceWithEntityGrizzlyConnector() throws Exception {
        _testTraceWithEntity(false, true);
    }

    @Test
    public void testAsyncTraceWithEntityGrizzlyConnector() throws Exception {
        _testTraceWithEntity(true, true);
    }

    private void _testTraceWithEntity(final boolean isAsync, final boolean useGrizzlyConnection) throws Exception {
        try {
            WebTarget target = useGrizzlyConnection ? getGrizzlyClient().target(target().getUri()) : target();
            target = target.path(App.ROOT_PATH_ANNOTATED);

            final Entity<String> entity = Entity.entity("trace", MediaType.WILDCARD_TYPE);

            Response response;
            if (!isAsync) {
                response = target.request().method(TRACE.NAME, entity);
            } else {
                response = target.request().async().method(TRACE.NAME, entity).get();
            }

            fail("A TRACE request MUST NOT include an entity. (response=" + response + ")");
        } catch (Exception e) {
            // OK
        }
    }

    private Client getGrizzlyClient() {
        return ClientBuilder.newClient(new ClientConfig().connectorProvider(new GrizzlyConnectorProvider()));
    }
}
