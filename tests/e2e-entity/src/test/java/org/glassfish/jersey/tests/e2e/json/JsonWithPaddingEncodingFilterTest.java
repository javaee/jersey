/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2017 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.tests.e2e.json;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.message.DeflateEncoder;
import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.server.JSONP;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.EncodingFilter;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests, that JSONP callback wrapping takes places before the eventual entity compression.
 *
 * See https://java.net/jira/browse/JERSEY-2524 for the original issue description.
 *
 * @author Adam Lindenthal (adam.lindenthal at oracle.com)
 */
public class JsonWithPaddingEncodingFilterTest extends JerseyTest {

    @Override
    protected ResourceConfig configure() {
        enable(TestProperties.LOG_TRAFFIC);
        return new ResourceConfig(MyResource.class)
                .register(JacksonFeature.class)
                .register(EncodingFilter.class)
                .register(GZipEncoder.class)
                .register(DeflateEncoder.class);
    }

    @Path("rest")
    public static class MyResource {
        @GET
        @Path("jsonp")
        @JSONP(queryParam = JSONP.DEFAULT_QUERY)
        @Produces("application/x-javascript")
        public Message getHelloJsonP(@Context final HttpHeaders headers) {
            final MultivaluedMap<String, String> headerParams = headers.getRequestHeaders();
            for (final String key : headerParams.keySet()) {
                System.out.println(key + ": ");
                for (final String value : headerParams.get(key)) {
                    System.out.print(value + ", ");
                }
                System.out.println("\b\b");
            }
            return new Message("Hello world JsonP!", "English");
        }
    }

    public static class Message {
        private String greeting;
        private String language;

        public Message(final String greeting, final String language) {
            this.greeting = greeting;
            this.language = language;
        }

        public String getGreeting() {
            return greeting;
        }

        public String getLanguage() {
            return language;
        }
    }

    @Test
    public void testCorrectGzipDecoding() {
        final Response response = target().path("rest/jsonp").queryParam("__callback", "dialog")
                .register(GZipEncoder.class).request("application/x-javascript")
                .header("Accept-Encoding", "gzip").get();

        final String result = response.readEntity(String.class);
        assertEquals("gzip", response.getHeaders().getFirst("Content-Encoding"));

        assertTrue(result.startsWith("dialog("));
        assertTrue(result.contains("Hello world JsonP!"));
        assertTrue(result.contains("English"));
    }

    @Test
    public void testCorrectDeflateDecoding() {
        final Response response = target().path("rest/jsonp").queryParam("__callback", "dialog")
                .register(DeflateEncoder.class).request("application/x-javascript")
                .header("Accept-Encoding", "deflate").get();

        final String result = response.readEntity(String.class);
        assertEquals("deflate", response.getHeaders().getFirst("Content-Encoding"));

        assertTrue(result.startsWith("dialog("));
        assertTrue(result.contains("Hello world JsonP!"));
        assertTrue(result.contains("English"));
    }
}
