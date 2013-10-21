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

import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author Paul Sandoz (paul.sandoz at oracle.com)
 */
public class HeadTest extends AbstractJettyServerTester {
    @Path("/")
    public static class Resource {
        @Path("string")
        @GET
        public String getString() {
            return "GET";
        }

        @Path("byte")
        @GET
        public byte[] getByte() {
            return "GET".getBytes();
        }

        @Path("ByteArrayInputStream")
        @GET
        public InputStream getInputStream() {
            return new ByteArrayInputStream("GET".getBytes());
        }
    }

    @Test
    public void testHead() throws Exception {
        startServer(Resource.class);

        Client client = ClientBuilder.newClient();
        WebTarget r = client.target(getUri().path("/").build());

        Response cr = r.path("string").request("text/plain").head();
        assertEquals(200, cr.getStatus());
        String lengthStr = cr.getHeaderString(HttpHeaders.CONTENT_LENGTH);
        Assert.assertNotNull(lengthStr);
        assertEquals(3, Integer.parseInt(lengthStr));
        assertEquals(MediaType.TEXT_PLAIN_TYPE, cr.getMediaType());
        assertFalse(cr.hasEntity());

        cr = r.path("byte").request("application/octet-stream").head();
        assertEquals(200, cr.getStatus());
        int length = cr.getLength();
        Assert.assertNotNull(length);
        assertEquals(3, length);
        assertEquals(MediaType.APPLICATION_OCTET_STREAM_TYPE, cr.getMediaType());
        assertFalse(cr.hasEntity());

        cr = r.path("ByteArrayInputStream").request("application/octet-stream").head();
        assertEquals(200, cr.getStatus());
        length = cr.getLength();
        Assert.assertNotNull(length);
        assertEquals(3, length);
        assertEquals(MediaType.APPLICATION_OCTET_STREAM_TYPE, cr.getMediaType());
        assertFalse(cr.hasEntity());
    }
}