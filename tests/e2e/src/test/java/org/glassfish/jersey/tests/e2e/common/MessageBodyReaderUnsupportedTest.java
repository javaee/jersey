/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.tests.e2e.common;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import javax.xml.bind.annotation.XmlRootElement;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test case for unsupported media type.
 *
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 *
 */
public class MessageBodyReaderUnsupportedTest extends JerseyTest {

    @Override
    protected ResourceConfig configure() {
        // JsonJaxbBinder must not be registered in the application for this test case.
        return new ResourceConfig(Resource.class);
    }

    /**
     * Send request to with application/json content to server where JsonJaxbBinder is not registered. UNSUPPORTED_MEDIA_TYPE
     * should be returned.
     */
    @Test
    public void testUnsupportedMesageBodyReader() {
        client().configuration().register(new JacksonFeature());
        TestEntity entity = new TestEntity("testEntity");
        Response response = target().path("test").request("application/json").post(Entity.json(entity));

        // JsonJaxbBinder is not registered on the server and therefore the server should return UNSUPPORTED_MEDIA_TYPE
        assertEquals(Status.UNSUPPORTED_MEDIA_TYPE.getStatusCode(), response.getStatus());
        assertFalse(Resource.methodJsonCalled);
        String responseEntity = response.readEntity(String.class);
        assertTrue((responseEntity == null) || (responseEntity.length() == 0));
    }

    /**
     * Test Resource class.
     *
     * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
     *
     */
    @Path("test")
    public static class Resource {

        private static boolean methodJsonCalled;

        @POST
        @Produces("application/json")
        @Consumes("application/json")
        @SuppressWarnings("UnusedParameters")
        public TestEntity processJsonAndProduceNullAsJson(TestEntity entity) {
            methodJsonCalled = true;
            return null;
        }
    }

    /**
     * Test bean.
     *
     * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
     *
     */
    @XmlRootElement
    @SuppressWarnings("UnusedDeclaration")
    public static class TestEntity {

        private String value;

        public String getValue() {
            return value;
        }

        public TestEntity(String value) {
            super();
            this.value = value;
        }
    }
}
