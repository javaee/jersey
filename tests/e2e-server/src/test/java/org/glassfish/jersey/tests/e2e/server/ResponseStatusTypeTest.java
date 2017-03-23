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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.grizzly.connector.GrizzlyConnectorProvider;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.grizzly.GrizzlyTestContainerFactory;
import org.glassfish.jersey.test.inmemory.InMemoryTestContainerFactory;
import org.glassfish.jersey.test.jdkhttp.JdkHttpServerTestContainerFactory;
import org.glassfish.jersey.test.simple.SimpleTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Tests custom response status reason phrase with jersey containers and connectors.
 *
 * @author Miroslav Fuksa
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({ResponseStatusTypeTest.InMemoryTest.class,
        ResponseStatusTypeTest.GrizzlyContainerGrizzlyConnectorTest.class,
        ResponseStatusTypeTest.GrizzlyContainerApacheConnectorTest.class,
        ResponseStatusTypeTest.SimpleContainerHttpUrlConnectorTest.class})
public class ResponseStatusTypeTest {

    public static final String REASON_PHRASE = "my-phrase";

    public static class InMemoryTest extends JerseyTest {
        @Override
        protected Application configure() {
            return new ResourceConfig(TestResource.class);

        }

        @Override
        protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
            return new InMemoryTestContainerFactory();
        }

        @Test
        public void testCustom() {
            _testCustom(target());
        }

        @Test
        public void testBadRequest() {
            _testBadRequest(target());
        }

        @Test
        public void testCustomBadRequest() {
            // with InMemory container and connector status info should be transferred as it is produced.
            final Response response = target().path("resource/custom-bad-request").request().get();
            Assert.assertEquals(400, response.getStatus());
            Assert.assertNull(response.getStatusInfo().getReasonPhrase());

        }

    }

    public static class GrizzlyContainerGrizzlyConnectorTest extends JerseyTest {
        @Override
        protected Application configure() {
            return new ResourceConfig(TestResource.class);

        }

        @Override
        protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
            return new GrizzlyTestContainerFactory();
        }

        @Override
        protected void configureClient(ClientConfig config) {
            config.connectorProvider(new GrizzlyConnectorProvider());
        }


        @Test
        public void testCustom() {
            _testCustom(target());
        }

        @Test
        public void testBadRequest() {
            _testBadRequest(target());
        }

        @Test
        public void testCustomBadRequest() {
            _testCustomBadRequest(target());
        }
    }

    public static class GrizzlyContainerApacheConnectorTest extends JerseyTest {
        @Override
        protected Application configure() {
            return new ResourceConfig(TestResource.class);

        }

        @Override
        protected void configureClient(ClientConfig config) {
            config.connectorProvider(new ApacheConnectorProvider());
        }


        @Test
        public void testCustom() {
            _testCustom(target());
        }

        @Test
        public void testBadRequest() {
            _testBadRequest(target());
        }

        @Test
        public void testCustomBadRequest() {
            _testCustomBadRequest(target());
        }
    }

    public static class SimpleContainerHttpUrlConnectorTest extends JerseyTest {
        @Override
        protected Application configure() {
            return new ResourceConfig(TestResource.class);

        }

        @Override
        protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
            return new SimpleTestContainerFactory();
        }

        @Test
        public void testCustom() {
            _testCustom(target());
        }

        @Test
        public void testBadRequest() {
            _testBadRequest(target());
        }

        @Test
        public void testCustomBadRequest() {
            _testCustomBadRequest(target());
        }
    }

    public static class JdkHttpContainerHttpUrlConnectorTest extends JerseyTest {
        @Override
        protected Application configure() {
            return new ResourceConfig(TestResource.class);

        }

        @Override
        protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
            return new JdkHttpServerTestContainerFactory();
        }

        @Test
        @Ignore("Jdk http container does not support custom response reason phrases.")
        public void testCustom() {
            _testCustom(target());
        }

        @Test
        public void testBadRequest() {
            _testBadRequest(target());
        }

        @Test
        public void testCustomBadRequest() {
            _testCustomBadRequest(target());
        }
    }

    @Path("resource")
    public static class TestResource {

        @GET
        @Path("custom")
        public Response testStatusType() {
            return Response.status(new Custom428Type()).build();
        }

        @GET
        @Path("bad-request")
        public Response badRequest() {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        @GET
        @Path("custom-bad-request")
        public Response customBadRequest() {
            return Response.status(new CustomBadRequestWithoutReasonType()).build();
        }

    }

    public static class Custom428Type implements Response.StatusType {
        @Override
        public int getStatusCode() {
            return 428;
        }

        @Override
        public String getReasonPhrase() {
            return REASON_PHRASE;
        }

        @Override
        public Response.Status.Family getFamily() {
            return Response.Status.Family.CLIENT_ERROR;
        }
    }

    public static class CustomBadRequestWithoutReasonType implements Response.StatusType {
        @Override
        public int getStatusCode() {
            return 400;
        }

        @Override
        public String getReasonPhrase() {
            return null;
        }

        @Override
        public Response.Status.Family getFamily() {
            return Response.Status.Family.CLIENT_ERROR;
        }
    }

    public static void _testCustom(WebTarget target) {
        final Response response = target.path("resource/custom").request().get();
        Assert.assertEquals(428, response.getStatus());
        Assert.assertEquals(REASON_PHRASE, response.getStatusInfo().getReasonPhrase());
    }

    public static void _testBadRequest(WebTarget target) {
        final Response response = target.path("resource/bad-request").request().get();
        Assert.assertEquals(400, response.getStatus());
        Assert.assertEquals(Response.Status.BAD_REQUEST.getReasonPhrase(), response.getStatusInfo().getReasonPhrase());
    }

    public static void _testCustomBadRequest(WebTarget target) {
        final Response response = target.path("resource/custom-bad-request").request().get();
        Assert.assertEquals(400, response.getStatus());
        Assert.assertEquals(Response.Status.BAD_REQUEST.getReasonPhrase(), response.getStatusInfo().getReasonPhrase());
    }
}
