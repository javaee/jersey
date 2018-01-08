/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.e2e.server.filter;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 *
 * @author Paul Sandoz
 * @author Martin Matula
 */
public class UriConnegLanguageTest extends JerseyTest {

    @Path("/abc")
    public static class LanguageVariantResource {

        @GET
        public Response doGet(@Context Request request, @Context HttpHeaders headers) {

            assertEquals(1, headers.getAcceptableLanguages().size());

            List<Variant> vs = Variant.VariantListBuilder.newInstance()
                    .languages(new Locale("zh"))
                    .languages(new Locale("fr"))
                    .languages(new Locale("en")).add()
                    .build();

            Variant v = request.selectVariant(vs);
            if (v == null) {
                return Response.notAcceptable(vs).build();
            } else {
                return Response.ok(v.getLanguage().toString(), v).build();
            }
        }
    }

    @Override
    protected Application configure() {
        Map<String, String> languages = new HashMap<>();
        languages.put("english", "en");
        languages.put("french", "fr");

        ResourceConfig rc = new ResourceConfig(LanguageVariantResource.class);
        rc.property(ServerProperties.LANGUAGE_MAPPINGS, languages);
        return rc;
    }

    @Test
    public void testLanguages() {
        Response response = target().path("abc.english").request().get();
        assertEquals("en", response.readEntity(String.class));
        assertEquals("en", response.getLanguage().toString());

        response = target().path("abc.french").request().get();
        assertEquals("fr", response.readEntity(String.class));
        assertEquals("fr", response.getLanguage().toString());

        response = target().path("abc.french").request().header(HttpHeaders.ACCEPT_LANGUAGE, "en").get();
        assertEquals("fr", response.readEntity(String.class));
        assertEquals("fr", response.getLanguage().toString());

        response = target().path("abc").request().header(HttpHeaders.ACCEPT_LANGUAGE, "en").get();
        assertEquals("en", response.readEntity(String.class));
        assertEquals("en", response.getLanguage().toString());

        response = target().path("abc").request().header(HttpHeaders.ACCEPT_LANGUAGE, "fr").get();
        assertEquals("fr", response.readEntity(String.class));
        assertEquals("fr", response.getLanguage().toString());
    }
}
