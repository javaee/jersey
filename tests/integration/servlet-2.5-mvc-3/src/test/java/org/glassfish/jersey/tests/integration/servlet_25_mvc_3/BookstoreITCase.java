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

package org.glassfish.jersey.tests.integration.servlet_25_mvc_3;

import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.tests.integration.servlet_25_mvc_3.resource.Bookstore;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class BookstoreITCase extends TestSupport {

    @Test
    public void testResourceAsHtml() throws Exception {
        // NOTE: HttpUrlConnector sends several accepted types by default when not explicitly set by the caller.
        // In such case, the .accept("text/html") call is not necessary. However, other connectors act in a different way and
        // this leads in different behaviour when selecting the MessageBodyWriter. Leaving the definition explicit for broader
        // compatibility.
        assertBookstoreHtmlResponse(target().request(MediaType.TEXT_HTML).get(String.class));
    }

    @Test
    public void testResourceAsXml() throws Exception {
        final Bookstore response = target().request("application/xml").get(Bookstore.class);

        assertNotNull("Should have returned a bookstore!", response);
        assertEquals("bookstore name", "Czech Bookstore", response.getName());
    }

    @Test
    public void testResourceAsHtmlUsingWebKitAcceptHeaders() throws Exception {
        final String response = target().request(
                "text/html",
                "application/xhtml+xml",
                "application/xml;q=0.9",
                "*/*;q=0.8").get(String.class);

        assertBookstoreHtmlResponse(response);
    }

    protected void assertBookstoreHtmlResponse(String response) {
        assertHtmlResponse(response);
        assertResponseContains(response, "Bookstore");
        assertResponseContains(response, "Item List");
    }
}