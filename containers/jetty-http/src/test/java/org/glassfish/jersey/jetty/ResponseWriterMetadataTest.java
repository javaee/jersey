/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.jetty;

import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


/**
 * @author Paul Sandoz (paul.sandoz at oracle.com)
 */
public class ResponseWriterMetadataTest extends AbstractJettyServerTester {
    @Provider
    @Produces("text/plain")
    public static class StringWriter implements MessageBodyWriter<String> {

        public boolean isWriteable(Class<?> c, Type t, Annotation[] as, MediaType mediaType) {
            return String.class == c;
        }

        public long getSize(String s, Class<?> type, Type genericType, Annotation annotations[], MediaType mediaType) {
            return -1;
        }

        public void writeTo(String s, Class<?> c, Type t, Annotation[] as,
                            MediaType mt, MultivaluedMap<String, Object> headers, OutputStream out)
                throws IOException, WebApplicationException {
            headers.add("X-BEFORE-WRITE", "x");
            out.write(s.getBytes());
            headers.add("X-AFTER-WRITE", "x");
        }
    }

    @Path("/")
    public static class Resource {
        @GET
        public String get() {
            return "one";
        }
    }

    @Test
    public void testResponse() {
        ResourceConfig rc = new ResourceConfig(Resource.class,
                StringWriter.class);
        startServer(rc);

        Client client = ClientBuilder.newClient();
        WebTarget r = client.target(getUri().path("/").build());
        Response cr = r.request("text/plain").get(Response.class);
        assertEquals("x", cr.getHeaderString("X-BEFORE-WRITE"));
        assertNull(cr.getHeaderString("X-AFTER-WRITE"));
    }
}