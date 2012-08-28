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
package org.glassfish.jersey.client.filter;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientFactory;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.ClientRequest;
import org.glassfish.jersey.client.ClientResponse;
import org.glassfish.jersey.message.DeflateEncoder;
import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.process.Inflector;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Martin Matula (martin.matula at oracle.com)
 */
public class EncodingFilterTest {
    @Test
    public void testAcceptEncoding() {
        Client client = ClientFactory.newClient(new ClientConfig(
                EncodingFilter.class,
                GZipEncoder.class,
                DeflateEncoder.class
        ).connector(new TestConnector()));
        Invocation.Builder invBuilder = client.target(UriBuilder.fromUri("/").build()).request();
        Response r = invBuilder.get();
        assertEquals("deflate,gzip,x-gzip", r.getHeaderString(HttpHeaders.ACCEPT_ENCODING));
        assertNull(r.getHeaderString(HttpHeaders.CONTENT_ENCODING));
    }

    @Test
    public void testContentEncoding() {
        Client client = ClientFactory.newClient(new ClientConfig(
                EncodingFilter.class,
                GZipEncoder.class,
                DeflateEncoder.class
        ).setProperty(ClientProperties.USE_ENCODING, "gzip").connector(new TestConnector()));
        Invocation.Builder invBuilder = client.target(UriBuilder.fromUri("/").build()).request();
        Response r = invBuilder.get();
        assertEquals("deflate,gzip,x-gzip", r.getHeaderString(HttpHeaders.ACCEPT_ENCODING));
        assertEquals("gzip", r.getHeaderString(HttpHeaders.CONTENT_ENCODING));
    }

    @Test
    public void testContentEncodingViaFeature() {
        Client client = ClientFactory.newClient(new ClientConfig()
                .connector(new TestConnector())
                .register(new EncodingFeature("gzip", GZipEncoder.class, DeflateEncoder.class)));
        Invocation.Builder invBuilder = client.target(UriBuilder.fromUri("/").build()).request();
        Response r = invBuilder.get();
        assertEquals("deflate,gzip,x-gzip", r.getHeaderString(HttpHeaders.ACCEPT_ENCODING));
        assertEquals("gzip", r.getHeaderString(HttpHeaders.CONTENT_ENCODING));
    }

    @Test
    public void testUnsupportedContentEncoding() {
        Client client = ClientFactory.newClient(new ClientConfig(
                EncodingFilter.class,
                GZipEncoder.class,
                DeflateEncoder.class
        ).setProperty(ClientProperties.USE_ENCODING, "non-gzip").connector(new TestConnector()));
        Invocation.Builder invBuilder = client.target(UriBuilder.fromUri("/").build()).request();
        Response r = invBuilder.get();
        assertEquals("deflate,gzip,x-gzip", r.getHeaderString(HttpHeaders.ACCEPT_ENCODING));
        assertNull(r.getHeaderString(HttpHeaders.CONTENT_ENCODING));
    }

    private static class TestConnector implements Inflector<ClientRequest, ClientResponse> {
        @Override
        public ClientResponse apply(ClientRequest requestContext) {
            final ClientResponse responseContext = new ClientResponse(
                    Response.Status.OK, requestContext);

            String headerValue = requestContext.getHeaderString(HttpHeaders.ACCEPT_ENCODING);
            if (headerValue != null) {
                responseContext.header(HttpHeaders.ACCEPT_ENCODING, headerValue);
            }
            headerValue = requestContext.getHeaderString(HttpHeaders.CONTENT_ENCODING);
            if (headerValue != null) {
                responseContext.header(HttpHeaders.CONTENT_ENCODING, headerValue);
            }
            return responseContext;
        }
    }
}
