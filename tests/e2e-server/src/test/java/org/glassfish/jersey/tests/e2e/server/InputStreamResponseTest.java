/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2017 Oracle and/or its affiliates. All rights reserved.
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

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * This is to make sure you can just pass an input stream to Jersey,
 * where entity body data would be read from.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public class InputStreamResponseTest extends JerseyTest {

    @Override
    protected Application configure() {
        return new ResourceConfig(InputStreamResource.class, WrappedInputStreamResource.class);
    }

    @Path("/directInputStream")
    public static class InputStreamResource {

        @GET
        public InputStream get() {
            return new ByteArrayInputStream("Josefka".getBytes());
        }
    }

    @Test
    public void testDirectInputStream() throws Exception {
        final String s = target().path("directInputStream").request().get(String.class);

        assertEquals("Josefka", s);
    }

    @Path("/responseWrappedInputStream")
    public static class WrappedInputStreamResource {

        @GET
        public Response get() {
            return Response.ok(new ByteArrayInputStream("Marie".getBytes()), MediaType.TEXT_PLAIN_TYPE).build();
        }
    }

    @Test
    public void testWrappedInputStream() throws Exception {
        final Response response = target().path("responseWrappedInputStream").request().get();

        assertEquals(200, response.getStatus());
        assertEquals(MediaType.TEXT_PLAIN_TYPE, response.getMediaType());
        assertEquals("Marie", response.readEntity(String.class));
    }
}
