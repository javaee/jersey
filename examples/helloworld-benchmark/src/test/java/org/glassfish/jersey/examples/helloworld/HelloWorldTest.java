/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.examples.helloworld;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;

import org.junit.Test;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * {@link org.glassfish.jersey.examples.helloworld.HelloWorldResource} tests.
 *
 * @author Michal Gajdos
 */
public class HelloWorldTest extends JerseyTest {

    @Override
    protected ResourceConfig configure() {
        enable(TestProperties.LOG_TRAFFIC);

        return new ResourceConfig(HelloWorldResource.class);
    }

    @Test
    public void testGet() {
        final Response response = target().path("helloworld").request("text/plain").get();

        assertThat("Wrong HTTP response code returned.", response.getStatus(), is(200));
        assertThat("Unexpected response entity.", response.readEntity(String.class), is(HelloWorldResource.CLICHED_MESSAGE));
    }

    @Test
    public void testGetLocator() {
        final Response response = target().path("helloworld").path("locator").request("text/plain").get();

        assertThat("Wrong HTTP response code returned.", response.getStatus(), is(200));
        assertThat("Unexpected response entity.", response.readEntity(String.class), is(HelloWorldResource.CLICHED_MESSAGE));
    }

    @Test
    public void testPost() {
        final Response response = target().path("helloworld")
                .request("text/plain")
                .post(Entity.text(HelloWorldResource.CLICHED_MESSAGE));

        assertThat("Wrong HTTP response code returned.", response.getStatus(), is(200));
        assertThat("Unexpected response entity.", response.readEntity(String.class), is(HelloWorldResource.CLICHED_MESSAGE));
    }

    @Test
    public void testPostLocator() {
        final Response response = target().path("helloworld").path("locator")
                .request("text/plain")
                .post(Entity.text(HelloWorldResource.CLICHED_MESSAGE));

        assertThat("Wrong HTTP response code returned.", response.getStatus(), is(200));
        assertThat("Unexpected response entity.", response.readEntity(String.class), is(HelloWorldResource.CLICHED_MESSAGE));
    }

    @Test
    public void testPut() {
        final Response response = target().path("helloworld")
                .request("text/plain")
                .put(Entity.text(HelloWorldResource.CLICHED_MESSAGE));

        assertThat("Wrong HTTP response code returned.", response.getStatus(), is(204));
    }

    @Test
    public void testPutLocator() {
        final Response response = target().path("helloworld").path("locator")
                .request("text/plain")
                .put(Entity.text(HelloWorldResource.CLICHED_MESSAGE));

        assertThat("Wrong HTTP response code returned.", response.getStatus(), is(204));
    }
}
