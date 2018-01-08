/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.examples.httppatch;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import javax.json.Json;
import javax.json.JsonArray;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.grizzly.connector.GrizzlyConnectorProvider;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * HTTP PATCH Example unit tests.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class HttpPatchTest extends JerseyTest {

    @Override
    protected ResourceConfig configure() {
        // Uncomment to enable message exchange logging
        // enable(TestProperties.DUMP_ENTITY);
        // enable(TestProperties.LOG_TRAFFIC);
        return App.create();
    }

    @Override
    protected void configureClient(ClientConfig config) {
        config.register(App.createMoxyJsonResolver())
                .connectorProvider(new GrizzlyConnectorProvider());

    }

    /**
     * This test verifies that the patching of the resource state works.
     * <p>
     * The patch is created using the new standard JSON Processing API for Java and
     * is then sent to the server. {@code PATCH} response as well as the new resource
     * state obtained via subsequent {@code GET} method is verified against the expected
     * state.
     * </p>
     */
    @Test
    public void testPatch() {
        final WebTarget target = target(App.ROOT_PATH);

        // initial precondition check
        final State expected = new State();
        assertEquals(expected, target.request("application/json").get(State.class));

        // apply first patch
        expected.setMessage("patchedMessage");
        expected.setTitle("patchedTitle");
        expected.getList().add("one");
        expected.getList().add("two");

        JsonArray patch_1 = Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                        .add("op", "replace")
                        .add("path", "/message")
                        .add("value", expected.getMessage())
                        .build())
                .add(Json.createObjectBuilder()
                        .add("op", "replace")
                        .add("path", "/title")
                        .add("value", expected.getTitle())
                        .build())
                .add(Json.createObjectBuilder()
                        .add("op", "replace")
                        .add("path", "/list")
                        .add("value", Json.createArrayBuilder()
                                .add(expected.getList().get(0))
                                .add(expected.getList().get(1))
                                .build())
                        .build())
                .build();

        assertEquals(expected, target.request()
                                     .method("PATCH",
                                             Entity.entity(patch_1, MediaType.APPLICATION_JSON_PATCH_JSON), State.class));
        assertEquals(expected, target.request("application/json").get(State.class));

        // apply second patch
        expected.getList().add("three");

        JsonArray patch_2 = Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                        .add("op", "add")
                        .add("path", "/list/-")
                        .add("value", expected.getList().get(2))
                        .build())
                .build();
        assertEquals(expected, target.request()
                                     .method("PATCH",
                                             Entity.entity(patch_2, MediaType.APPLICATION_JSON_PATCH_JSON), State.class));
        assertEquals(expected, target.request("application/json").get(State.class));
    }
}
