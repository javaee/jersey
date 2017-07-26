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

package org.glassfish.jersey.examples.clipboard;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class ClipboardTest extends JerseyTest {

    @Override
    protected ResourceConfig configure() {
        enable(TestProperties.LOG_TRAFFIC);
        return App.createApp();
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        client().target(getBaseUri()).path(App.ROOT_PATH).request().delete();
        client().target(getBaseUri()).path(App.ROOT_PATH).path("history").request().delete();
    }

    @Override
    protected void configureClient(ClientConfig config) {
        config.register(ClipboardDataProvider.ApplicationJson.class).register(ClipboardDataProvider.TextPlain.class);
    }

    @Test
    public void testDeclarativeClipboardTextPlain() throws Exception {
        testDeclarativeClipboard(MediaType.TEXT_PLAIN_TYPE);
    }

    @Test
    public void testDeclarativeClipboardAppJson() throws Exception {
        testDeclarativeClipboard(MediaType.APPLICATION_JSON_TYPE);
    }

    public void testDeclarativeClipboard(MediaType mediaType) throws Exception {
        final WebTarget clipboard = client().target(getBaseUri()).path(App.ROOT_PATH);

        Response response;

        response = clipboard.request(mediaType).get();
        assertEquals(204, response.getStatus());

        response = clipboard.request(mediaType).put(Entity.entity(new ClipboardData("Hello"), mediaType));
        assertEquals(204, response.getStatus());

        assertEquals("Hello", clipboard.request(mediaType).get(ClipboardData.class).toString());

        response = clipboard.request(mediaType).post(Entity.entity(new ClipboardData(" World!"), mediaType));
        assertEquals(200, response.getStatus());

        assertEquals("Hello World!", clipboard.request(mediaType).get(ClipboardData.class).toString());

        response = clipboard.request(mediaType).delete();
        assertEquals(204, response.getStatus());

        assertEquals(204, clipboard.request(mediaType).get().getStatus());
    }

    @Test
    public void testProgrammaticEchoTextPlain() throws Exception {
        testProgrammaticEcho(MediaType.TEXT_PLAIN_TYPE);
    }

    @Test
    public void testProgrammaticEchoAppJson() throws Exception {
        testProgrammaticEcho(MediaType.APPLICATION_JSON_TYPE);
    }

    @Test
    public void testHistoryInJson() throws Exception {
        callGetHistory(MediaType.APPLICATION_JSON_TYPE);
    }

    @Test
    public void testHistoryInTextPlain() throws Exception {
        callGetHistory(MediaType.TEXT_PLAIN_TYPE);
    }

    private void callGetHistory(final MediaType mediaType) {
        final WebTarget clipboard = client().target(getBaseUri()).path(App.ROOT_PATH);
        clipboard.request(mediaType).post(Entity.entity(new ClipboardData("Task 1 "), mediaType));
        clipboard.request(mediaType).post(Entity.entity(new ClipboardData("Task 2 "), mediaType));
        clipboard.request(mediaType).post(Entity.entity(new ClipboardData("Task 3 "), mediaType));

        ClipboardData response = clipboard.path("history").request(mediaType).get(ClipboardData.class);
        assertEquals(new ClipboardData("[Task 1 , Task 1 Task 2 ]"), response);
    }

    public void testProgrammaticEcho(MediaType mediaType) throws Exception {
        final WebTarget echo = client().target(getBaseUri()).path("echo");

        Response response = echo.request(mediaType).post(Entity.entity(new ClipboardData("Hello"), mediaType));
        assertEquals("Hello", response.readEntity(ClipboardData.class).toString());
    }
}
