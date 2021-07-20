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
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package org.glassfish.jersey.tests.integration.servlet_25_mvc_3;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.tests.integration.servlet_25_mvc_3.resource.Book;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class ItemITCase extends TestSupport {

    @Test
    public void testResourceAsHtml() throws Exception {
        // NOTE: HttpUrlConnector sends several accepted types by default when not explicitly set by the caller.
        // In such case, the .accept("text/html") call is not necessary. However, other connectors act in a different way and
        // this leads in different behaviour when selecting the MessageBodyWriter. Leaving the definition explicit for broader
        // compatibility.
        final String response = item1resource().request(MediaType.TEXT_HTML).get(String.class);
        assertItemHtmlResponse(response);
    }

    @Test
    public void testResourceAsHtmlUtf8() throws Exception {
        final Response response = item1resource().path("utf").request().get();
        final String html = response.readEntity(String.class);

        assertItemHtmlResponse(html);
        assertResponseContains(html, "Ha\u0161ek");
    }

    @Test
    public void testResourceAsHtmlIso88592() throws Exception {
        final Response response = item1resource().path("iso").request().get();
        response.bufferEntity();

        final String htmlUtf8 = response.readEntity(String.class);

        assertItemHtmlResponse(htmlUtf8);
        assertFalse("Response shouldn't contain Ha\u0161ek but was: " + htmlUtf8, htmlUtf8.contains("Ha\u0161ek"));

        final byte[] bytes = response.readEntity(byte[].class);
        final String htmlIso = new String(bytes, "ISO-8859-2");

        assertItemHtmlResponse(htmlIso);
        assertFalse("Response shouldn't contain Ha\u0161ek but was: " + htmlIso, htmlIso.contains("Ha\u0161ek"));
        assertResponseContains(htmlIso, new String("Ha\u0161ek".getBytes(), "ISO-8859-2"));
    }

    @Test
    public void testResourceAsXml() throws Exception {
        final String text = item1resource().request("application/xml").get(String.class);
        System.out.println("Item XML is: " + text);

        final Book response = item1resource().request("application/xml").get(Book.class);
        assertNotNull("Should have returned an item!", response);
        assertEquals("item title", "Svejk", response.getTitle());
    }

    @Test
    public void testResourceAsHtmlUsingWebKitAcceptHeaders() throws Exception {
        final String response = item1resource().request(
                "text/html",
                "application/xhtml+xml",
                "application/xml;q=0.9",
                "*/*;q=0.8").get(String.class);

        assertItemHtmlResponse(response);
    }

    protected void assertItemHtmlResponse(final String response) {
        assertHtmlResponse(response);
        assertResponseContains(response, "<title>Book</title>");
        assertResponseContains(response, "<h1>Svejk</h1>");
    }

    protected WebTarget item1resource() {
        return target().path("/items/1");
    }
}
