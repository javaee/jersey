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
package org.glassfish.jersey.tests.api;

import java.security.AccessController;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;

import org.glassfish.jersey.internal.util.PropertiesHelper;
import org.glassfish.jersey.message.internal.HeaderUtils;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class ResponseTest {

    /*
     * Create an instance of Response using Response.ok(String, Variant).build()
     * verify that correct status code is returned
     */
    @Test
    public void OkTest5() {
        Response resp;
        int status = 200;
        String content = "Test Only";
        List<String> encoding = Arrays.asList("gzip", "compress");
        List<String> lang = Arrays.asList("en-US", "en-GB", "zh-CN");

        MediaType mt = new MediaType("text", "plain");
        List<Variant> vts = Variant.VariantListBuilder.newInstance().mediaTypes(mt)
                .languages(new Locale("en", "US"), new Locale("en", "GB"),
                        new Locale("zh", "CN")).encodings((String[]) encoding.toArray())
                .add().build();

        String tmp;
        for (Variant vt : vts) {
            resp = Response.ok(content, vt).build();
            tmp = verifyResponse(resp, content, status, encoding, lang, null,
                    null, null, null);
            if (tmp.endsWith("false")) {
                System.out.println("### " + tmp);
                fail();
            }
        }
    }

    /*
     * Create an instance of Response using
     * Response.ResponseBuilder.clone()
     * verify that correct status code is returned
     */
    @Test
    public void cloneTest() throws CloneNotSupportedException {
        StringBuilder sb = new StringBuilder();

        int status = 200;
        List<String> type = Arrays.asList("text/plain", "text/html");
        List<String> encoding = Arrays.asList("gzip", "compress");
        List<String> lang = Arrays.asList("en-US", "en-GB", "zh-CN");

        String name = "name_1";
        String value = "value_1";
        Cookie ck1 = new Cookie(name, value);
        NewCookie nck1 = new NewCookie(ck1);

        List<String> cookies = Arrays.asList(nck1.toString().toLowerCase());

        Response.ResponseBuilder respb1 = Response.status(status)
                .header("Content-type", "text/plain").header("Content-type",
                "text/html").header("Content-Language", "en-US")
                .header("Content-Language", "en-GB").header("Content-Language",
                "zh-CN").header("Cache-Control", "no-transform")
                .header("Set-Cookie", "name_1=value_1;version=1");
        Response.ResponseBuilder respb2 = respb1.clone();

        Response resp2 = respb2.build();

        String tmp = verifyResponse(resp2, null, status, encoding, lang, type,
                null, null, cookies);
        if (tmp.endsWith("false")) {
            System.out.println("### " + sb.toString());
            fail();
        }
        sb.append(tmp).append(newline);

        String content = "TestOnly";
        Response resp1 = respb1.entity(content).cookie((NewCookie) null).build();
        tmp = verifyResponse(resp1, content, status, encoding, lang, type,
                null, null, null);
        if (tmp.endsWith("false")) {
            System.out.println("### " + sb.toString());
            fail();
        }

        MultivaluedMap<java.lang.String, java.lang.Object> mvp =
                resp1.getMetadata();
        if (mvp.containsKey("Set-Cookie")) {
            sb.append("Response contains unexpected Set-Cookie: ").append(mvp.getFirst("Set-Cookie").toString()).append(newline);
            System.out.println("### " + sb.toString());
            fail();
        }
        sb.append(tmp).append(newline);
    }

    /*
     * Create an instance of Response using
     * Response.fromResponse(Response).build()
     * verify that correct status code is returned
     */
    @Test
    public void fromResponseTest() {
        int status = 200;
        String content = "Test Only";
        List<String> type = Arrays.asList("text/plain", "text/html");
        List<String> encoding = Arrays.asList("gzip", "compress");
        List<String> lang = Arrays.asList("en-US", "en-GB", "zh-CN");

        MediaType mt1 = new MediaType("text", "plain");
        MediaType mt2 = new MediaType("text", "html");
        List<Variant> vts = Variant.VariantListBuilder.newInstance().mediaTypes(mt1, mt2)
                .languages(new Locale("en", "US"), new Locale("en", "GB"),
                        new Locale("zh", "CN")).encodings((String[]) encoding.toArray())
                .add().build();

        String tmp;
        for (Variant vt : vts) {
            Response resp1 = Response.ok(content, vt).build();
            Response resp = Response.fromResponse(resp1).build();
            tmp = verifyResponse(resp, content, status, encoding, lang, type,
                    null, null, null);
            if (tmp.endsWith("false")) {
                System.out.println("### " + tmp);
                fail();
            }
        }
    }

    /*
     * Create an instance of Response using
     * Response.ResponseBuilder.header(String, Object).build()
     * verify that correct status code is returned
     */
    @Test
    public void headerTest() {
        int status = 200;
        List<String> type = Arrays.asList("text/plain", "text/html");
        List<String> encoding = Arrays.asList("gzip", "compress");
        List<String> lang = Arrays.asList("en-US", "en-GB", "zh-CN");

        String name = "name_1";
        String value = "value_1";
        Cookie ck1 = new Cookie(name, value);
        NewCookie nck1 = new NewCookie(ck1);

        List<String> cookies = Arrays.asList(nck1.toString().toLowerCase());

        Response resp = Response.status(status).header("Content-type",
                "text/plain").header("Content-type", "text/html").header("Content-Language", "en-US")
                .header("Content-Language", "en-GB").header("Content-Language",
                "zh-CN").header("Cache-Control", "no-transform")
                .header("Set-Cookie", "name_1=value_1;version=1").build();
        String tmp = verifyResponse(resp, null, status, encoding, lang, type,
                null, null, cookies);
        if (tmp.endsWith("false")) {
            System.out.println("### " + tmp);
            fail();
        }
    }

    /*
     * Create an instance of Response using
     * Response.status(int).variant(Variant).build()
     * verify that correct status code is returned
     */
    @Test
    public void variantTest() {
        Response resp;
        int status = 200;
        List<String> encoding = Arrays.asList("gzip", "compress");
        List<String> lang = Arrays.asList("en-US", "en-GB", "zh-CN");

        MediaType mt = new MediaType("text", "plain");
        List<Variant> vts = Variant.VariantListBuilder.newInstance().mediaTypes(mt)
                .languages(new Locale("en", "US"), new Locale("en", "GB"),
                        new Locale("zh", "CN")).encodings((String[]) encoding.toArray())
                .add().build();

        String tmp;
        for (Variant vt : vts) {
            resp = Response.status(status).variant(vt).build();
            tmp = verifyResponse(resp, null, status, encoding, lang, null, null,
                    null, null);
            if (tmp.endsWith("false")) {
                System.out.println("### " + tmp);
                fail();
            }
        }
    }

    private static final String indent = "    ";
    private static final String newline = AccessController.doPrivileged(PropertiesHelper.getSystemProperty("line.separator"));

    private String verifyResponse(Response resp, String content, int status,
                                  List<String> encoding, List<String> language, List<String> type,
                                  List<String> var, List<String> ccl, List<String> cookies) {
        boolean pass = true;
        StringBuilder sb = new StringBuilder();

        sb.append("========== Verifying a Response: ").append(newline);

        String tmp = verifyResponse(resp, content, status, null);
        sb.append(indent).append(tmp).append(newline);
        if (tmp.endsWith("false")) {
            pass = false;
        }

        MultivaluedMap<String, String> mvp = HeaderUtils.asStringHeaders(
                resp.getMetadata());

        for (String key : mvp.keySet()) {
            sb.append(indent + "Processing Key found in response: ").append(key).append(": ").append(mvp.get(key)).append("; ")
                    .append(newline);

            if (key.equalsIgnoreCase("Vary")) {
                for (String value : var) {
                    String actual = mvp.get(key).toString().toLowerCase();
                    if (!actual.contains(value)) {
                        pass = false;
                        sb.append(indent + indent + "Expected header ").append(value).append(" not set in Vary.").append(newline);
                    } else {
                        sb.append(indent + indent + "Found expected header ").append(value).append(".").append(newline);
                    }
                }
            }

            if (encoding != null) {
                if (key.equalsIgnoreCase("Content-encoding")) {
                    for (Object enc : mvp.get(key)) {
                        if (!encoding.contains(enc.toString().toLowerCase())) {
                            pass = false;
                            sb.append(indent + indent + "Encoding test failed: ").append(newline);
                        }
                    }
                }
            }

            if (language != null) {
                if (key.equalsIgnoreCase("Content-language")) {
                    for (String lang : mvp.get(key)) {
                        if (!language.contains(lang)) {
                            pass = false;
                            sb.append(indent + indent + "language test failed: ").append(lang)
                                    .append(" is not expected in Response").append(newline);
                            for (String tt : language) {
                                sb.append(indent + indent + "Expecting Content-Language ").append(tt).append(newline);
                            }
                        }
                    }
                }
            }

            if (type != null) {
                if (key.equalsIgnoreCase("Content-Type")) {
                    for (Object lang : mvp.get(key)) {
                        if (!type.contains(lang.toString().toLowerCase())) {
                            pass = false;
                            sb.append(indent + indent + "Content-Type test failed: ").append(lang)
                                    .append(" is not expected in Response").append(newline);
                        }
                    }
                }
            }

            if (ccl != null) {
                for (String tt : ccl) {
                    sb.append("Expecting Cache-Control ").append(tt).append(newline);
                }
                if (key.equalsIgnoreCase("Cache-Control")) {
                    for (Object all_ccl : mvp.get(key)) {
                        for (String cc : ccl) {
                            if (!(all_ccl.toString().toLowerCase().contains(cc.toLowerCase()))) {
                                pass = false;
                                sb.append(indent + indent + "Cache-Control test failed: ").append(cc)
                                        .append(" is not found in Response.").append(newline);
                            }
                        }
                    }
                }
            }

            if (cookies != null) {
                for (String tt : cookies) {
                    sb.append(indent + indent + "Expecting Set-Cookie").append(tt).append(newline);
                }
                if (key.equalsIgnoreCase("Set-Cookie")) {
                    for (Object nck_actual : mvp.get(key)) {
                        sb.append(indent + indent + "Processing ").append(nck_actual.toString()).append(newline);
                        if (!cookies.contains(nck_actual.toString().toLowerCase()
                                .replace(" ", ""))) {
                            pass = false;
                            sb.append(indent + indent + "Set-Cookie test failed: ").append(nck_actual)
                                    .append(" is not expected in Response.").append(newline);
                        } else {
                            sb.append(indent + indent + "Expected Set-Cookie: ").append(nck_actual)
                                    .append(" is found in Response.").append(newline);
                        }
                    }
                }
            }
        }

        sb.append(indent).append(pass);

        return sb.toString();
    }

    private String verifyResponse(Response resp, String content, int status, HashMap<String, String> expected_map) {
        boolean pass = true;
        StringBuilder sb = new StringBuilder();

        sb.append("========== Verifying a Response with Map: ").append(newline);

        if ((content == null) || (content.equals(""))) {
            if (!(resp.getEntity() == null) || "".equals(resp.getEntity())) {
                pass = false;
                sb.append(indent + "Entity verification failed: expecting no content, got ").append((String) resp.getEntity())
                        .append(newline);
            }
        } else if (!content.equals(resp.getEntity())) {
            pass = false;
            sb.append(indent + "Entity verification failed: expecting ").append(content).append(", got ")
                    .append((String) resp.getEntity()).append(newline);
        } else {
            sb.append(indent + "Correct content found in Response: ").append((String) resp.getEntity()).append(newline);
        }

        if (resp.getStatus() != status) {
            pass = false;
            sb.append(indent + "Status code verification failed: expecting ").append(status).append(", got ")
                    .append(resp.getStatus()).append(newline);
        } else {
            sb.append(indent + "Correct status found in Response: ").append(status).append(newline);
        }

        MultivaluedMap<java.lang.String, java.lang.Object> mvp =
                resp.getMetadata();
        if (expected_map == null) {
            sb.append(indent + "No keys to verify or expected, but found the following keys in Response:").append(newline);
            for (String key : mvp.keySet()) {
                sb.append(indent + indent + "Key: ").append(key).append("; ").append(mvp.getFirst(key)).append(";")
                        .append(newline);
            }
        } else {
            for (String key_actual : mvp.keySet()) {
                sb.append(indent + "Response contains key: ").append(key_actual).append(newline);
            }
            sb.append(indent + "Verifying the following keys in Response:").append(newline);
            String actual, expected;
            for (String key : expected_map.keySet()) {
                if (!mvp.containsKey(key)) {
                    pass = false;
                    sb.append(indent + indent + "Key: ").append(key).append(" is not found in Response;").append(newline);
                } else if (key.equalsIgnoreCase("last-modified")) {
                    sb.append(indent + indent + "Key Last-Modified is found in response").append(newline);
                } else {
                    expected = expected_map.get(key).toLowerCase();
                    actual = mvp.getFirst(key).toString().toLowerCase();

                    if (actual.startsWith("\"") && actual.endsWith("\"")) {
                        actual = actual.substring(1, actual.length() - 1);
                    }

                    if (!actual.equals(expected)) {
                        pass = false;
                        sb.append(indent + indent + "Key: ").append(key).append(" found in Response, but with different value;")
                                .append(newline);
                        sb.append(indent + indent + "Expecting ").append(expected_map.get(key)).append("; got ")
                                .append(mvp.getFirst(key)).append(newline);
                    }
                    sb.append(indent + indent + "Processed key ").append(key).append(" with expected value ")
                            .append(expected_map.get(key)).append(newline);
                }
            }
        }
        sb.append(indent).append(pass);
        return sb.toString();
    }

    @Test
    public void testAllowString() {
        Response.ResponseBuilder responseBuilder = Response.ok();

        responseBuilder = responseBuilder.allow("GET");
        assertTrue(responseBuilder.build().getHeaderString(HttpHeaders.ALLOW).contains("GET"));
        responseBuilder = responseBuilder.allow((String) null);
        assertTrue(responseBuilder.build().getHeaderString(HttpHeaders.ALLOW) == null);
    }

    @Test
    public void testAllowSet() {
        Response.ResponseBuilder responseBuilder = Response.ok();

        responseBuilder = responseBuilder.allow(new HashSet<>(Arrays.asList("GET")));
        assertTrue(responseBuilder.build().getHeaderString(HttpHeaders.ALLOW).contains("GET"));
        responseBuilder = responseBuilder.allow((Set<String>) null);
        assertEquals(null, responseBuilder.build().getHeaderString(HttpHeaders.ALLOW));
    }

    @Test
    public void testAllowVariant() {
        Response.ResponseBuilder responseBuilder = Response.ok();

        responseBuilder = responseBuilder.allow(new HashSet<>(Arrays.asList("GET")));
        assertTrue(responseBuilder.build().getHeaderString(HttpHeaders.ALLOW).contains("GET"));
        responseBuilder = responseBuilder.allow((String[]) null);
        assertEquals(null, responseBuilder.build().getHeaderString(HttpHeaders.ALLOW));
    }

    @Test
    public void bufferEntityTest() {
        Response response = Response.ok().build();
        response.close();
        try {
            response.bufferEntity();
            fail("IllegalStateException expected when reading entity after response has been closed.");
        } catch (IllegalStateException ex) {
            // expected
        }
    }

    @Test
    public void getEntityTest() {
        Response response = Response.ok().build();
        response.close();
        try {
            response.getEntity();
            fail("IllegalStateException expected when reading entity after response has been closed.");
        } catch (IllegalStateException ex) {
            // expected
        }
    }

    @Test
    public void hasEntityTest() {
        Response response = Response.ok().build();
        response.close();
        try {
            response.hasEntity();
            fail("IllegalStateException expected when reading entity after response has been closed.");
        } catch (IllegalStateException ex) {
            // expected
        }
    }

    // Reproducer for JERSEY-1553
    @Test
    public void testVariants() {
        List<String> encoding = Arrays.asList("gzip", "compress");
        List<Variant> list = Variant.VariantListBuilder
                .newInstance()
                .mediaTypes(MediaType.TEXT_PLAIN_TYPE)
                .languages(new Locale("en", "US"), new Locale("en", "GB"))
                .encodings(encoding.toArray(new String[encoding.size()])).add().build();

        final Response r1 = Response.ok().variants(list).build();
        assertNotNull(r1);
        assertNotNull(r1.getHeaderString(HttpHeaders.VARY));

        final Response r2 = Response.ok().variants(list.toArray(new Variant[list.size()])).build();
        assertNotNull(r2);
        assertNotNull(r2.getHeaderString(HttpHeaders.VARY));
    }
}
