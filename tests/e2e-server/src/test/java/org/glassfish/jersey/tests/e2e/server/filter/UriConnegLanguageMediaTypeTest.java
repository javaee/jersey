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
import javax.ws.rs.core.MediaType;
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
public class UriConnegLanguageMediaTypeTest extends JerseyTest {

    @Path("/abc")
    public static class LanguageVariantResource {

        @GET
        public Response doGet(@Context Request r) {
            final List<Variant> variants = Variant.VariantListBuilder.newInstance()
                    .mediaTypes(MediaType.valueOf("application/foo"))
                    .languages(new Locale("en")).languages(new Locale("fr")).add()
                    .mediaTypes(MediaType.valueOf("application/bar"))
                    .languages(new Locale("en")).languages(new Locale("fr")).add()
                    .build();

            final Variant variant = r.selectVariant(variants);
            if (variant == null) {
                return Response.notAcceptable(variants).build();
            } else {
                return Response.ok(variant.getMediaType().toString() + ", " + variant.getLanguage(), variant).build();
            }
        }
    }

    @Override
    protected Application configure() {
        Map<String, MediaType> mediaTypes = new HashMap<>();
        mediaTypes.put("foo", MediaType.valueOf("application/foo"));
        mediaTypes.put("bar", MediaType.valueOf("application/bar"));

        Map<String, String> languages = new HashMap<>();
        languages.put("english", "en");
        languages.put("french", "fr");

        ResourceConfig rc = new ResourceConfig(LanguageVariantResource.class);
        rc.property(ServerProperties.LANGUAGE_MAPPINGS, languages);
        rc.property(ServerProperties.MEDIA_TYPE_MAPPINGS, mediaTypes);
        return rc;
    }

    @Test
    public void testMediaTypesAndLanguages() {
        _test("english", "foo", "en", "application/foo");
        _test("french", "foo", "fr", "application/foo");

        _test("english", "bar", "en", "application/bar");
        _test("french", "bar", "fr", "application/bar");
    }

    private void _test(String ul, String um, String l, String m) {
        Response r = target().path("abc." + ul + "." + um).request().get();
        assertEquals(m + ", " + l, r.readEntity(String.class));
        assertEquals(l, r.getLanguage().toString());
        assertEquals(m, r.getMediaType().toString());

        r = target().path("abc." + um + "." + ul).request().get();
        assertEquals(m + ", " + l, r.readEntity(String.class));
        assertEquals(l, r.getLanguage().toString());
        assertEquals(m, r.getMediaType().toString());

        r = target().path("abc").request(m).header(HttpHeaders.ACCEPT_LANGUAGE, l).get();
        assertEquals(m + ", " + l, r.readEntity(String.class));
        assertEquals(l, r.getLanguage().toString());
        assertEquals(m, r.getMediaType().toString());
    }
}
