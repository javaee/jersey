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

package org.glassfish.jersey.server.filter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.RequestContextBuilder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.spi.ContentEncoder;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Martin Matula
 */
public class EncodingFilterTest {
    public static class FooEncoding extends ContentEncoder {
        public FooEncoding() {
            super("foo");
        }

        @Override
        public InputStream decode(String contentEncoding, InputStream encodedStream) throws IOException {
            return encodedStream;
        }

        @Override
        public OutputStream encode(String contentEncoding, OutputStream entityStream) throws IOException {
            return entityStream;
        }
    }

    @Test
    public void testNoInterceptor() {
        ResourceConfig rc = new ResourceConfig(EncodingFilter.class);
        ContainerResponseFilter filter = new ApplicationHandler(rc).getServiceLocator().getService(ContainerResponseFilter.class);
        assertNotNull(filter);
        assertTrue(filter instanceof EncodingFilter);
        assertEquals(1, ((EncodingFilter) filter).getSupportedEncodings().size());
    }

    @Test
    public void testEnableFor() {
        EncodingFilter filter = initializeAndGetFilter();
        assertNotNull(filter);
        assertEquals(4, filter.getSupportedEncodings().size());
    }

    @Test
    public void testNoAcceptEncodingHeader() throws IOException {
        testEncoding(null);
    }

    @Test
    public void testAcceptEncodingHeaderNotSupported() throws IOException {
        testEncoding(null, "not-gzip");
    }

    @Test
    public void testGZipAcceptEncodingHeader() throws IOException {
        testEncoding("gzip", "gzip");
    }

    @Test
    public void testGZipPreferred() throws IOException {
        testEncoding("gzip", "foo; q=.5", "gzip");
    }

    @Test
    public void testFooPreferred() throws IOException {
        testEncoding("foo", "foo", "gzip; q=.5");
    }

    @Test
    public void testNotAcceptable() throws IOException {
        try {
            testEncoding(null, "one", "two", "*; q=0");
            fail(Response.Status.NOT_ACCEPTABLE + " response was expected.");
        } catch (WebApplicationException e) {
            assertEquals(Response.Status.NOT_ACCEPTABLE, e.getResponse().getStatusInfo());
        }
    }

    @Test
    public void testIdentityPreferred() throws IOException {
        testEncoding(null, "identity", "foo; q=.5");
    }

    @Test
    public void testAnyAcceptableExceptGZipAndIdentity() throws IOException {
        testEncoding("foo", "*", "gzip; q=0", "identity; q=0");
    }

    @Test
    public void testNoEntity() throws IOException {
        EncodingFilter filter = initializeAndGetFilter();
        ContainerRequest request = RequestContextBuilder.from("/resource", "GET").header(HttpHeaders.ACCEPT_ENCODING,
                "gzip").build();
        ContainerResponse response = new ContainerResponse(request, Response.ok().build());
        filter.filter(request, response);
        assertNull(response.getHeaders().getFirst(HttpHeaders.CONTENT_ENCODING));
        assertNull(response.getHeaders().getFirst(HttpHeaders.VARY));
    }

    @SuppressWarnings("unchecked")
    private EncodingFilter initializeAndGetFilter() {
        ResourceConfig rc = new ResourceConfig();
        EncodingFilter.enableFor(rc, FooEncoding.class, GZipEncoder.class);
        return (EncodingFilter) new ApplicationHandler(rc).getServiceLocator().getService(ContainerResponseFilter.class);
    }

    private void testEncoding(String expected, String... accepted) throws IOException {
        EncodingFilter filter = initializeAndGetFilter();
        RequestContextBuilder builder = RequestContextBuilder.from("/resource", "GET");
        for (String a : accepted) {
            builder.header(HttpHeaders.ACCEPT_ENCODING, a);
        }
        ContainerRequest request = builder.build();
        ContainerResponse response = new ContainerResponse(request, Response.ok("OK!").build());
        filter.filter(request, response);
        if (response.getStatus() != 200) {
            throw new WebApplicationException(Response.status(response.getStatus()).build());
        }
        assertEquals(expected, response.getHeaderString(HttpHeaders.CONTENT_ENCODING));
        assertEquals(HttpHeaders.ACCEPT_ENCODING, response.getHeaderString(HttpHeaders.VARY));
    }
}
