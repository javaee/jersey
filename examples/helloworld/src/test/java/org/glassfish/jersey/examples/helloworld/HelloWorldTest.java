/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.examples.helloworld;

import java.net.HttpURLConnection;
import java.net.URL;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.Target;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.ext.ClientFactory;

import org.glassfish.jersey.server.Application;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HelloWorldTest extends JerseyTest {

    @Override
    protected Application configure() {
        // mvn test -DargLine="-Djersey.config.test.container.factory=org.glassfish.jersey.test.inmemory.InMemoryTestContainerFactory"
        // mvn test -DargLine="-Djersey.config.test.container.factory=org.glassfish.jersey.test.grizzly.GrizzlyTestContainerFactory"
        // mvn test -DargLine="-Djersey.config.test.container.factory=org.glassfish.jersey.test.jdkhttp.JdkHttpServerTestContainerFactory"
        enable(TestProperties.LOG_TRAFFIC);
//        enable(TestProperties.DUMP_ENTITY);
        ResourceConfig resourceConfig = ResourceConfig.builder().addClasses(HelloWorldResource.class).build();

        return Application.builder(resourceConfig).build();
    }

    @Test
    @Ignore("not compatible with test framework (doesn't use client())")
    public void testHelloWorld() throws Exception {
        URL getUrl = UriBuilder.fromUri(getBaseURI()).path(App.ROOT_PATH).build().toURL();
        HttpURLConnection connection = (HttpURLConnection) getUrl.openConnection();
        try {
            connection.setDoOutput(true);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type", "text/plain");
            assertEquals(HttpURLConnection.HTTP_OK, connection.getResponseCode());
        } finally {
            connection.disconnect();
        }
    }

    @Test
    public void testConnection() {
        Response response = target().path(App.ROOT_PATH).request("text/plain").get();
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testClientStringResponse() {
        String s = target().path(App.ROOT_PATH).request().get(String.class);
        assertEquals(HelloWorldResource.CLICHED_MESSAGE, s);
    }

    @Test
    public void testHead() {
        Response response = target().path(App.ROOT_PATH).request().head();
        assertEquals(200, response.getStatus());
        assertEquals(MediaType.TEXT_PLAIN_TYPE, response.getHeaders().getMediaType());
    }

    @Test
    public void testOptions() {
        Response response = target().path(App.ROOT_PATH).request().options();
        assertEquals(200, response.getStatus());
        final String allowHeader = response.getHeaders().getHeader("Allow");
        _checkAllowContent(allowHeader);
        assertEquals(MediaType.TEXT_PLAIN_TYPE, response.getHeaders().getMediaType());
        final String responseBody = response.readEntity(String.class);
        _checkAllowContent(responseBody);
    }

    private void _checkAllowContent(final String content) {
        assertTrue(content.contains("GET"));
        assertTrue(content.contains("HEAD"));
        assertTrue(content.contains("OPTIONS"));
    }

    @Test
    public void testMissingResourceNotFound() {
        Response response;

        response = target().path(App.ROOT_PATH + "arbitrary").request().get();
        assertEquals(404, response.getStatus());

        response = target().path(App.ROOT_PATH).path("arbitrary").request().get();
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testLoggingFilterClientClass() {
        Client client = client();
        client.configuration().register(CustomLoggingFilter.class).setProperty("foo", "bar");
        CustomLoggingFilter.preFilterCalled = CustomLoggingFilter.postFilterCalled = 0;
        String s = target().path(App.ROOT_PATH).request().get(String.class);
        assertEquals(HelloWorldResource.CLICHED_MESSAGE, s);
        assertEquals(1, CustomLoggingFilter.preFilterCalled);
        assertEquals(1, CustomLoggingFilter.postFilterCalled);
    }

    @Test
    public void testLoggingFilterClientInstance() {
        Client client = client();
        client.configuration().register(new CustomLoggingFilter()).setProperty("foo", "bar");
        CustomLoggingFilter.preFilterCalled = CustomLoggingFilter.postFilterCalled = 0;
        String s = target().path(App.ROOT_PATH).request().get(String.class);
        assertEquals(HelloWorldResource.CLICHED_MESSAGE, s);
        assertEquals(1, CustomLoggingFilter.preFilterCalled);
        assertEquals(1, CustomLoggingFilter.postFilterCalled);
    }

    @Test
    public void testLoggingFilterTargetClass() {
        Target target = target().path(App.ROOT_PATH);
        target.configuration().register(CustomLoggingFilter.class).setProperty("foo", "bar");
        CustomLoggingFilter.preFilterCalled = CustomLoggingFilter.postFilterCalled = 0;
        String s = target.request().get(String.class);
        assertEquals(HelloWorldResource.CLICHED_MESSAGE, s);
        assertEquals(1, CustomLoggingFilter.preFilterCalled);
        assertEquals(1, CustomLoggingFilter.postFilterCalled);
    }

    @Test
    public void testLoggingFilterTargetInstance() {
        Target target = target().path(App.ROOT_PATH);
        target.configuration().register(new CustomLoggingFilter()).setProperty("foo", "bar");
        CustomLoggingFilter.preFilterCalled = CustomLoggingFilter.postFilterCalled = 0;
        String s = target.request().get(String.class);
        assertEquals(HelloWorldResource.CLICHED_MESSAGE, s);
        assertEquals(1, CustomLoggingFilter.preFilterCalled);
        assertEquals(1, CustomLoggingFilter.postFilterCalled);
    }

    @Test
    public void testLoggingFilterInvocationClass() {
        Invocation.Builder inv = target().path(App.ROOT_PATH).request();
        inv.configuration().register(CustomLoggingFilter.class).setProperty("foo", "bar");
        CustomLoggingFilter.preFilterCalled = CustomLoggingFilter.postFilterCalled = 0;
        String s = inv.get(String.class);
        assertEquals(HelloWorldResource.CLICHED_MESSAGE, s);
        assertEquals(1, CustomLoggingFilter.preFilterCalled);
        assertEquals(1, CustomLoggingFilter.postFilterCalled);
    }

    @Test
    public void testLoggingFilterInvocationInstance() {
        Invocation.Builder inv = target().path(App.ROOT_PATH).request();
        inv.configuration().register(new CustomLoggingFilter()).setProperty("foo", "bar");
        CustomLoggingFilter.preFilterCalled = CustomLoggingFilter.postFilterCalled = 0;
        String s = inv.get(String.class);
        assertEquals(HelloWorldResource.CLICHED_MESSAGE, s);
        assertEquals(1, CustomLoggingFilter.preFilterCalled);
        assertEquals(1, CustomLoggingFilter.postFilterCalled);
    }

    @Test
    public void testConfigurationUpdate() {
        Client client = client();
        client.configuration().register(CustomLoggingFilter.class).setProperty("foo", "bar");
        client.configuration().update(ClientFactory.newClient().configuration());
        CustomLoggingFilter.preFilterCalled = CustomLoggingFilter.postFilterCalled = 0;
        String s = target().path(App.ROOT_PATH).request().get(String.class);
        assertEquals(HelloWorldResource.CLICHED_MESSAGE, s);
        assertEquals(0, CustomLoggingFilter.preFilterCalled);
        assertEquals(0, CustomLoggingFilter.postFilterCalled);
    }
}
