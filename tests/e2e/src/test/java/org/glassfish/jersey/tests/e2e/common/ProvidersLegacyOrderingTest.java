/*
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
*
* Copyright (c) 2012-2015 Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyWriter;

import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.message.MessageProperties;
import org.glassfish.jersey.message.internal.MediaTypes;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class ProvidersLegacyOrderingTest extends JerseyTest {

    @Produces("application/xml")
    public static class MyMBW5 implements MessageBodyWriter<Object> {

        @Override
        public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return false;
        }

        @Override
        public long getSize(Object myType, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return -1;
        }

        @Override
        public void writeTo(Object myType,
                            Class<?> type,
                            Type genericType,
                            Annotation[] annotations,
                            MediaType mediaType,
                            MultivaluedMap<String, Object> httpHeaders,
                            OutputStream entityStream) throws IOException, WebApplicationException {
        }
    }

    @Produces("application/*")
    public static class MyMBW6 implements MessageBodyWriter<Object> {

        @Override
        public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return false;
        }

        @Override
        public long getSize(Object myType, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return -1;
        }

        @Override
        public void writeTo(Object myType,
                            Class<?> type,
                            Type genericType,
                            Annotation[] annotations,
                            MediaType mediaType,
                            MultivaluedMap<String, Object> httpHeaders,
                            OutputStream entityStream) throws IOException, WebApplicationException {
        }
    }

    @Path("/")
    public static class MyResource {

        @Context
        MessageBodyWorkers messageBodyWorkers;

        @GET
        public String getMyType() {

            final Map<MediaType, List<MessageBodyWriter>> writers = messageBodyWorkers.getWriters(MediaType.APPLICATION_XML_TYPE);
            final List<MessageBodyWriter> messageBodyWriters1 = writers.get(MediaType.APPLICATION_XML_TYPE);
            final List<MessageBodyWriter> messageBodyWriters2 = writers
                    .get(MediaTypes.getTypeWildCart(MediaType.APPLICATION_XML_TYPE));

            // custom provider has priority - it has to be first
            assertTrue(messageBodyWriters1.get(0) instanceof MyMBW5);
            assertTrue(messageBodyWriters2.get(0) instanceof MyMBW6);

            return "";
        }
    }

    @Override
    protected Application configure() {
        final ResourceConfig resourceConfig = new ResourceConfig(MyResource.class, MyMBW5.class, MyMBW6.class);
        resourceConfig.property(MessageProperties.LEGACY_WORKERS_ORDERING, true);
        return resourceConfig;
    }

    @Test
    public void orderingTest() {
        try {
            Response response = target().request("application/test1").get(Response.class);

            assertNotNull(response);
            assertEquals("Request was not handled correctly, most likely fault in MessageBodyWorker selection.",
                    200, response.getStatus());
        } catch (Exception e) {
            fail("Request was not handled correctly, most likely fault in MessageBodyWorker selection.");
        }
    }
}
