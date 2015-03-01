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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Testing {@link Reader} on client and server.
 *
 * @author Miroslav Fuksa
 */
public class ReaderProviderTest extends JerseyTest {

    public static final String GET_READER_RESPONSE = "GET_READER_RESPONSE";
    public static final String GET_POST_RESPONSE = "GET_POST_RESPONSE";

    @Override
    protected Application configure() {
        return new ResourceConfig(ReaderResource.class);
    }

    @Test
    public void testReader() {
        Response response = target().path("test/postReaderGetReader").request().post(Entity.entity(GET_POST_RESPONSE,
                MediaType.TEXT_PLAIN));
        assertEquals(200, response.getStatus());
        assertEquals(GET_POST_RESPONSE, response.readEntity(String.class));
    }

    @Test
    public void testGetReader() {
        Response response = target().path("test/getReader").request().get();
        assertEquals(200, response.getStatus());
        assertEquals(GET_READER_RESPONSE, response.readEntity(String.class));
    }

    @Test
    public void testEmptyReader() throws IOException {
        Response response = target().path("test/getEmpty").request().get();
        assertEquals(204, response.getStatus());
        final Reader reader = response.readEntity(Reader.class);
        assertNotNull(reader);
        assertEquals(-1, reader.read());
    }

    @Test
    public void testReaderOnClientAsResponseEntity() throws IOException {
        Response response = target().path("test/getReader").request().get();
        assertEquals(200, response.getStatus());
        final Reader reader = response.readEntity(Reader.class);
        assertNotNull(reader);
        BufferedReader br = new BufferedReader(reader);
        assertEquals(GET_READER_RESPONSE, br.readLine());
    }

    @Test
    public void testReaderOnClientAsRequestEntity() throws IOException {
        Response response = target().path("test/postReaderGetReader").request()
                .post(Entity.entity(new StringReader(GET_POST_RESPONSE), MediaType.TEXT_PLAIN));
        assertEquals(200, response.getStatus());
        assertEquals(GET_POST_RESPONSE, response.readEntity(String.class));
    }

    @Path("test")
    public static class ReaderResource {

        @POST
        @Path("postReaderGetReader")
        public Reader postReader(Reader reader) throws IOException {
            return reader;
        }

        @GET
        @Path("getReader")
        public Reader getReader() throws IOException {
            return new StringReader(GET_READER_RESPONSE);
        }

        @GET
        @Path("getEmpty")
        public String getemptyResponse() throws IOException {
            return null;
        }
    }
}
