/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.uri.internal;

import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriBuilderException;

import org.glassfish.jersey.uri.UriComponent;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Uri builder implementation test.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @author Martin Matula (martin.matula at oracle.com)
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 * @author Paul Sandoz (paul.sandoz at oracle.com)
 */
public class UriBuilderImplTest {

    public UriBuilderImplTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    // See JAX_RS_SPEC-245
    public void testReplacingUserInfo() {
        final String userInfo = "foo:foo";

        URI uri;
        uri = UriBuilder.fromUri("http://foo2:foo2@localhost:8080").userInfo(userInfo).build();
        assertEquals(userInfo, uri.getRawUserInfo());

        uri = UriBuilder.fromUri("http://localhost:8080").userInfo(userInfo).build();
        assertEquals(userInfo, uri.getRawUserInfo());
    }

    @Test
    public void testPathTemplateValueEncoding() throws URISyntaxException {
        String result;
        result = new UriBuilderImpl().uri(new URI("http://examples.jersey.java.net/")).userInfo("a/b").path("a/b").segment
                ("a/b").build().toString();
        assertEquals("http://a%2Fb@examples.jersey.java.net/a/b/a%2Fb", result);

        result = new UriBuilderImpl().uri(new URI("http://examples.jersey.java.net/")).userInfo("{T1}").path("{T2}").segment
                ("{T3}").build("a/b", "a/b", "a/b").toString();
        assertEquals("http://a%2Fb@examples.jersey.java.net/a%2Fb/a%2Fb", result);

        result = new UriBuilderImpl().uri(new URI("http://examples.jersey.java.net/")).userInfo("{T1}").path("{T2}").segment
                ("{T3}").build(new Object[]{"a/b", "a/b", "a/b"}, false).toString();
        assertEquals("http://a%2Fb@examples.jersey.java.net/a/b/a/b", result);

        result = new UriBuilderImpl().uri(new URI("http://examples.jersey.java.net/")).userInfo("{T1}").path("{T2}").segment
                ("{T2}").build("a@b", "a@b").toString();
        assertEquals("http://a%40b@examples.jersey.java.net/a@b/a@b", result);

        result = new UriBuilderImpl().uri(new URI("http://examples.jersey.java.net/")).userInfo("{T}").path("{T}").segment
                ("{T}").build("a@b").toString();
        assertEquals("http://a%40b@examples.jersey.java.net/a@b/a@b", result);
    }

    @Test
    public void testReplaceMatrixParamWithNull() {
        UriBuilder builder = new UriBuilderImpl().matrixParam("matrix", "param1", "param2");
        builder.replaceMatrixParam("matrix", (Object[]) null);
        assertEquals(builder.build().toString(), "");
    }

    // for completeness (added along with regression tests for JERSEY-1114)
    @Test
    public void testBuildNoSlashUri() {
        UriBuilder builder = new UriBuilderImpl().uri(URI.create("http://localhost:8080")).path("test");
        assertEquals("http://localhost:8080/test", builder.build().toString());
    }

    // regression test for JERSEY-1114
    @Test
    public void testBuildFromMapNoSlashInUri() {
        UriBuilder builder = new UriBuilderImpl().uri(URI.create("http://localhost:8080")).path("test");
        assertEquals("http://localhost:8080/test", builder.buildFromMap(new HashMap<String, Object>()).toString());
    }

    // regression test for JERSEY-1114
    @Test
    public void testBuildFromArrayNoSlashInUri() {
        UriBuilder builder = new UriBuilderImpl().uri(URI.create("http://localhost:8080")).path("test");
        assertEquals("http://localhost:8080/test", builder.build("testing").toString());
    }

    @Test
    public void testReplaceNullMatrixParam() {
        try {
            new UriBuilderImpl().replaceMatrixParam(null, "param");
        } catch (IllegalArgumentException e) {
            return;
        } catch (Exception e) {
            fail("Expected IllegalArgumentException but got " + e.toString());
        }
        fail("Expected IllegalArgumentException but no exception was thrown.");
    }

    // regression test for JERSEY-1081
    @Test
    public void testReplaceQueryParam() {
        URI uri = new UriBuilderImpl().path("http://localhost/").replaceQueryParam("foo", "test").build();
        assertEquals("http://localhost/?foo=test", uri.toString());
    }

    // regression test for JERSEY-1081
    @Test
    public void testReplaceQueryParamAndClone() {
        URI uri = new UriBuilderImpl().path("http://localhost/").replaceQueryParam("foo", "test").clone().build();
        assertEquals("http://localhost/?foo=test", uri.toString());
    }

    @Test
    public void testUriBuilderTemplatesSimple() {
        testUri("a:/path");
        testUri("a:/p");
        testUri("a:/path/x/y/z");
        testUri("a:/path/x?q=12#fragment");
        testUri("a:/p?q#f");
        testUri("a://host");
        testUri("a://host:5555/a/b");
        testUri("a://h:5/a/b");
        testUri("a:/user@host:12345");         //user@host:12345 is not authority but path
        testUri("a:/user@host:12345/a/b/c");
        testUri("a:/user@host:12345/a/b/c?aaa&bbb#ccc");
        testUri("a:/user@host.hhh.ddd.c:12345/a/b/c?aaa&bbb#ccc");
        testUri("/a");
        testUri("/a/../../b/c/d");
        testUri("//localhost:80/a/b");
        testUri("//l:8/a/b");
        testUri("a/b");
        testUri("a");
        testUri("../../s");
        testUri("mailto:test@test.com");
        testUri("http://orac@le:co@m:1234/a/b/ccc?a#fr");
        testUri("http://[::FFFF:129.144.52.38]:1234/a/b/ccc?a#fr");
        testUri("http://[FEDC:BA98:7654:3210:FEDC:BA98:7654:3210]:1234/a/b/ccc?a#fr");

    }

    @Test
    @Ignore
    public void failingTests() {
        testUri("a://#fragment"); // fails in UriBuilderImpl
        testUri("a://?query");


        // fails: opaque uris are not supported by UriTemplate
        URI uri = new UriBuilderImpl().uri("{scheme}://{mailto}").build("mailto", "email@test.ttt");
        assertEquals("mailto:email@test.ttt", uri.toString());
    }

    @Test
    public void testUriBuilderTemplates() {
        URI uri = new UriBuilderImpl().uri("http://localhost:8080/{path}").build("a/b/c");
        assertEquals("http://localhost:8080/a%2Fb%2Fc", uri.toString());

        uri = new UriBuilderImpl().uri("{scheme}://{host}").build("http", "localhost");
        assertEquals("http://localhost", uri.toString());


        uri = new UriBuilderImpl().uri("http://{host}:8080/{path}").build("l", "a/b/c");
        assertEquals("http://l:8080/a%2Fb%2Fc", uri.toString());

        uri = new UriBuilderImpl().uri("{scheme}://{host}:{port}/{path}").build("s", "h", new Integer(1), "a");
        assertEquals("s://h:1/a", uri.toString());

        Map<String, Object> values = new HashMap<String, Object>();
        values.put("scheme", "s");
        values.put("host", "h");
        values.put("port", new Integer(1));
        values.put("path", "p/p");
        values.put("query", "q");
        values.put("fragment", "f");

        uri = new UriBuilderImpl().uri("{scheme}://{host}:{port}/{path}?{query}#{fragment}").buildFromMap(values);
        assertEquals("s://h:1/p%2Fp?q#f", uri.toString());


        uri = new UriBuilderImpl().uri("{scheme}://{host}:{port}/{path}/{path2}").build("s", "h", new Integer(1), "a", "b");
        assertEquals("s://h:1/a/b", uri.toString());


        uri = new UriBuilderImpl().uri("{scheme}://{host}:{port}/{path}/{path2}").build("s", "h", new Integer(1), "a", "b");
        assertEquals("s://h:1/a/b", uri.toString());

        uri = new UriBuilderImpl().uri("//{host}:{port}/{path}/{path2}").build("h", new Integer(1), "a", "b");
        assertEquals("//h:1/a/b", uri.toString());


        uri = new UriBuilderImpl().uri("/{a}/{a}/{b}").build("a", "b");
        assertEquals("/a/a/b", uri.toString());

        uri = new UriBuilderImpl().uri("/{a}/{a}/{b}?{queryParam}").build("a", "b", "query");
        assertEquals("/a/a/b?query", uri.toString());

        // partial templates
        uri = new UriBuilderImpl().uri("/{a}xx/{a}/{b}?{queryParam}").build("a", "b", "query");
        assertEquals("/axx/a/b?query", uri.toString());

        uri = new UriBuilderImpl().uri("my{scheme}://my{host}:1{port}/my{path}/my{path2}").build("s", "h", new Integer(1), "a",
                "b/c");
        assertEquals("mys://myh:11/mya/myb%2Fc", uri.toString());

        uri = new UriBuilderImpl().uri("my{scheme}post://my{host}post:5{port}9/my{path}post/my{path2}post").build("s", "h",
                new Integer(1), "a", "b");
        assertEquals("myspost://myhpost:519/myapost/mybpost", uri.toString());
    }

    @Test
    public void testUriBuilderTemplatesNotEncodedSlash() {
        URI uri = new UriBuilderImpl().uri("http://localhost:8080/{path}").build(new Object[]{"a/b/c"}, false);
        assertEquals("http://localhost:8080/a/b/c", uri.toString());

        uri = new UriBuilderImpl().uri("http://{host}:8080/{path}").build(new Object[]{"l", "a/b/c"}, false);
        assertEquals("http://l:8080/a/b/c", uri.toString());

        Map<String, Object> values = new HashMap<String, Object>();
        values.put("scheme", "s");
        values.put("host", "h");
        values.put("port", new Integer(1));
        values.put("path", "p/p");
        values.put("query", "q");
        values.put("fragment", "f");

        uri = new UriBuilderImpl().uri("{scheme}://{host}:{port}/{path}?{query}#{fragment}").buildFromMap(values, false);
        assertEquals("s://h:1/p/p?q#f", uri.toString());
    }

    private void testUri(String input) {
        URI uri = new UriBuilderImpl().uri(input).clone().build();

        URI originalUri = URI.create(input);
        assertEquals(originalUri.getScheme(), uri.getScheme());
        assertEquals(originalUri.getHost(), uri.getHost());
        assertEquals(originalUri.getPort(), uri.getPort());
        assertEquals(originalUri.getUserInfo(), uri.getUserInfo());
        assertEquals(originalUri.getPath(), uri.getPath());
        assertEquals(originalUri.getQuery(), uri.getQuery());
        assertEquals(originalUri.getFragment(), uri.getFragment());
        assertEquals(originalUri.getRawSchemeSpecificPart(), uri.getRawSchemeSpecificPart());
        assertEquals(originalUri.isAbsolute(), uri.isAbsolute());
        assertEquals(input, uri.toString());
    }


    @org.junit.Test
    public void testOpaqueUri() {
        URI uri = UriBuilder.fromUri("mailto:a@b").build();
        Assert.assertEquals("mailto:a@b", uri.toString());
    }


    @Test
    public void testOpaqueUriReplaceSchemeSpecificPart() {
        URI uri = UriBuilder.fromUri("mailto:a@b").schemeSpecificPart("c@d").build();
        Assert.assertEquals("mailto:c@d", uri.toString());
    }

    @Test
    public void testOpaqueReplaceUri() {
        URI uri = UriBuilder.fromUri("mailto:a@b").uri(URI.create("c@d")).build();
        Assert.assertEquals("mailto:c@d", uri.toString());
    }

    @Test
    public void testReplaceScheme() {
        URI uri = UriBuilder.fromUri("http://localhost:8080/a/b/c").
                scheme("https").build();
        Assert.assertEquals("https://localhost:8080/a/b/c", uri.toString());

        uri = UriBuilder.fromUri("http://localhost:8080/a/b/c").
                scheme(null).build();
        Assert.assertEquals("//localhost:8080/a/b/c", uri.toString());

        uri = UriBuilder.fromUri("http://localhost:8080/a/b/c").
                scheme(null).host(null).build();
        Assert.assertEquals("//:8080/a/b/c", uri.toString());

        uri = UriBuilder.fromUri("http://localhost:8080/a/b/c").
                scheme(null).host(null).port(-1).build();
        Assert.assertEquals("/a/b/c", uri.toString());
    }

    @Test
    public void testReplaceSchemeSpecificPart() {
        URI uri = UriBuilder.fromUri("http://localhost:8080/a/b/c").
                schemeSpecificPart("//localhost:8080/a/b/c/d").build();
        Assert.assertEquals(URI.create("http://localhost:8080/a/b/c/d"), uri);
    }

    @Test
    public void testNameAuthorityUri() {
        URI uri = UriBuilder.fromUri("http://x_y/a/b/c").build();
        Assert.assertEquals(URI.create("http://x_y/a/b/c"), uri);
    }

    @Test
    public void testReplaceNameAuthorityUriWithHost() {
        URI uri = UriBuilder.fromUri("http://x_y.com/a/b/c").host("xy.com").build();
        Assert.assertEquals(URI.create("http://xy.com/a/b/c"), uri);
    }

    @Test
    public void testReplaceNameAuthorityUriWithSSP() {
        URI uri = UriBuilder.fromUri("http://x_y.com/a/b/c").schemeSpecificPart("//xy.com/a/b/c").build();
        Assert.assertEquals(URI.create("http://xy.com/a/b/c"), uri);

        uri = UriBuilder.fromUri("http://x_y.com/a/b/c").schemeSpecificPart("//v_w.com/a/b/c").build();
        Assert.assertEquals(URI.create("http://v_w.com/a/b/c"), uri);
    }

    @Test
    public void testReplaceUserInfo() {
        URI uri = UriBuilder.fromUri("http://bob@localhost:8080/a/b/c").
                userInfo("sue").build();
        Assert.assertEquals(URI.create("http://sue@localhost:8080/a/b/c"), uri);
    }

    @Test
    public void testReplaceHost() {
        URI uri = UriBuilder.fromUri("http://localhost:8080/a/b/c").
                host("a.com").build();
        Assert.assertEquals(URI.create("http://a.com:8080/a/b/c"), uri);
    }

    @Test
    public void testReplacePort() {
        URI uri = UriBuilder.fromUri("http://localhost:8080/a/b/c").
                port(9090).build();
        Assert.assertEquals(URI.create("http://localhost:9090/a/b/c"), uri);

        uri = UriBuilder.fromUri("http://localhost:8080/a/b/c").
                port(-1).build();
        Assert.assertEquals(URI.create("http://localhost/a/b/c"), uri);
    }

    @Test
    public void testReplacePath() {
        URI uri = UriBuilder.fromUri("http://localhost:8080/a/b/c").
                replacePath("/x/y/z").build();
        Assert.assertEquals(URI.create("http://localhost:8080/x/y/z"), uri);
    }

    @Test
    public void testReplacePathNull() {
        URI uri = UriBuilder.fromUri("http://localhost:8080/a/b/c").
                replacePath(null).build();

        Assert.assertEquals(URI.create("http://localhost:8080"), uri);
    }

    @Test
    public void testReplaceMatrix() {
        URI uri = UriBuilder.fromUri("http://localhost:8080/a/b/c;a=x;b=y").
                replaceMatrix("x=a;y=b").build();
        Assert.assertEquals(URI.create("http://localhost:8080/a/b/c;x=a;y=b"), uri);
    }

    @Test
    public void testReplaceMatrixParams() {
        UriBuilder ubu = UriBuilder.fromUri("http://localhost:8080/a/b/c;a=x;b=y").
                replaceMatrixParam("a", "z", "zz");

        {
            URI uri = ubu.build();
            List<PathSegment> ps = UriComponent.decodePath(uri, true);
            MultivaluedMap<String, String> mps = ps.get(2).getMatrixParameters();
            List<String> a = mps.get("a");
            Assert.assertEquals(2, a.size());
            Assert.assertEquals("z", a.get(0));
            Assert.assertEquals("zz", a.get(1));
            List<String> b = mps.get("b");
            Assert.assertEquals(1, b.size());
            Assert.assertEquals("y", b.get(0));
        }

        {
            URI uri = ubu.replaceMatrixParam("a", "_z_", "_zz_").build();
            List<PathSegment> ps = UriComponent.decodePath(uri, true);
            MultivaluedMap<String, String> mps = ps.get(2).getMatrixParameters();
            List<String> a = mps.get("a");
            Assert.assertEquals(2, a.size());
            Assert.assertEquals("_z_", a.get(0));
            Assert.assertEquals("_zz_", a.get(1));
            List<String> b = mps.get("b");
            Assert.assertEquals(1, b.size());
            Assert.assertEquals("y", b.get(0));
        }

        {
            URI uri = UriBuilderImpl.fromUri("http://localhost:8080/a/b/c;a=x;b=y").
                    replaceMatrixParam("a", "z", "zz").matrixParam("c", "c").
                    path("d").build();

            List<PathSegment> ps = UriComponent.decodePath(uri, true);
            MultivaluedMap<String, String> mps = ps.get(2).getMatrixParameters();
            List<String> a = mps.get("a");
            Assert.assertEquals(2, a.size());
            Assert.assertEquals("z", a.get(0));
            Assert.assertEquals("zz", a.get(1));
            List<String> b = mps.get("b");
            Assert.assertEquals(1, b.size());
            Assert.assertEquals("y", b.get(0));
            List<String> c = mps.get("c");
            Assert.assertEquals(1, c.size());
            Assert.assertEquals("c", c.get(0));
        }
    }

    @Test
    public void testReplaceMatrixParamsEmpty() {
        UriBuilder ubu = UriBuilder.fromUri("http://localhost:8080/a/b/c").
                replaceMatrixParam("a", "z", "zz");
        {
            URI uri = ubu.build();
            List<PathSegment> ps = UriComponent.decodePath(uri, true);
            MultivaluedMap<String, String> mps = ps.get(2).getMatrixParameters();
            List<String> a = mps.get("a");
            Assert.assertEquals(2, a.size());
            Assert.assertEquals("z", a.get(0));
            Assert.assertEquals("zz", a.get(1));
        }
    }

    @Test
    public void testReplaceMatrixParamsEncoded() throws URISyntaxException {
        UriBuilder ubu = UriBuilder.fromUri("http://localhost/").
                replaceMatrix("limit=10;sql=select+*+from+users");
        ubu.replaceMatrixParam("limit", 100);

        URI uri = ubu.build();
        Assert.assertEquals(URI.create("http://localhost/;limit=100;sql=select+*+from+users"), uri);
    }

    @Test
    public void testReplaceQuery() {
        URI uri = UriBuilder.fromUri("http://localhost:8080/a/b/c?a=x&b=y").
                replaceQuery("x=a&y=b").build();
        Assert.assertEquals(URI.create("http://localhost:8080/a/b/c?x=a&y=b"), uri);
    }

    @Test
    public void testBuildEncodedQuery() {
        URI u = UriBuilder.fromPath("").
                queryParam("y", "1 %2B 2").build();
        Assert.assertEquals(URI.create("?y=1+%2B+2"), u);

        // Issue 216
        u = UriBuilder.fromPath("http://localhost:8080").path("/{x}/{y}/{z}/{x}").
                buildFromEncoded("%xy", " ", "=");
        Assert.assertEquals(URI.create("http://localhost:8080/%25xy/%20/=/%25xy"), u);
    }

    @Test
    public void testReplaceQueryParams() {
        UriBuilder ubu = UriBuilder.fromUri("http://localhost:8080/a/b/c?a=x&b=y").
                replaceQueryParam("a", "z", "zz").queryParam("c", "c");

        {
            URI uri = ubu.build();

            MultivaluedMap<String, String> qps = UriComponent.decodeQuery(uri, true);
            List<String> a = qps.get("a");
            Assert.assertEquals(2, a.size());
            Assert.assertEquals("z", a.get(0));
            Assert.assertEquals("zz", a.get(1));
            List<String> b = qps.get("b");
            Assert.assertEquals(1, b.size());
            Assert.assertEquals("y", b.get(0));
            List<String> c = qps.get("c");
            Assert.assertEquals(1, c.size());
            Assert.assertEquals("c", c.get(0));
        }

        {
            URI uri = ubu.replaceQueryParam("a", "_z_", "_zz_").build();

            MultivaluedMap<String, String> qps = UriComponent.decodeQuery(uri, true);
            List<String> a = qps.get("a");
            Assert.assertEquals(2, a.size());
            Assert.assertEquals("_z_", a.get(0));
            Assert.assertEquals("_zz_", a.get(1));
            List<String> b = qps.get("b");
            Assert.assertEquals(1, b.size());
            Assert.assertEquals("y", b.get(0));
            List<String> c = qps.get("c");
            Assert.assertEquals(1, c.size());
            Assert.assertEquals("c", c.get(0));
        }

        // issue 257 - param is removed after setting it to null
        {
            URI u1 = UriBuilder.fromPath("http://localhost:8080").queryParam("x", "10").replaceQueryParam("x", null).build();
            Assert.assertTrue(u1.toString().equals("http://localhost:8080"));

            URI u2 = UriBuilder.fromPath("http://localhost:8080").queryParam("x", "10").replaceQueryParam("x").build();
            Assert.assertTrue(u2.toString().equals("http://localhost:8080"));
        }

        // issue 257 - IllegalArgumentException
        {
            boolean caught = false;

            try {
                URI u = UriBuilder.fromPath("http://localhost:8080").queryParam("x", "10").replaceQueryParam("x", "1", null, "2")
                        .build();
            } catch (IllegalArgumentException iae) {
                caught = true;
            }

            Assert.assertTrue(caught);
        }

    }

    @Test
    public void testReplaceQueryParamsEmpty() {
        UriBuilder ubu = UriBuilder.fromUri("http://localhost:8080/a/b/c").
                replaceQueryParam("a", "z", "zz").queryParam("c", "c");

        {
            URI uri = ubu.build();

            MultivaluedMap<String, String> qps = UriComponent.decodeQuery(uri, true);
            List<String> a = qps.get("a");
            Assert.assertEquals(2, a.size());
            Assert.assertEquals("z", a.get(0));
            Assert.assertEquals("zz", a.get(1));
            List<String> c = qps.get("c");
            Assert.assertEquals(1, c.size());
            Assert.assertEquals("c", c.get(0));
        }
    }

    @Test
    public void testReplaceQueryParamsEncoded1() throws URISyntaxException {
        UriBuilder ubu = UriBuilder.fromUri(new URI("http://localhost/")).
                replaceQuery("limit=10&sql=select+*+from+users");
        ubu.replaceQueryParam("limit", 100);

        URI uri = ubu.build();
        Assert.assertEquals(URI.create("http://localhost/?limit=100&sql=select+*+from+users"), uri);
    }

    @Test
    public void testReplaceQueryParamsEncoded2() throws URISyntaxException {
        UriBuilder ubu = UriBuilder.fromUri(new URI("http://localhost")).
                replaceQuery("limit=10&sql=select+*+from+users");
        ubu.replaceQueryParam("limit", 100);

        URI uri = ubu.build();
        Assert.assertEquals(URI.create("http://localhost/?limit=100&sql=select+*+from+users"), uri);
    }

    @Test
    public void testReplaceQueryParamsEncoded3() throws URISyntaxException {
        UriBuilder ubu = UriBuilder.fromUri("http://localhost/").
                replaceQuery("limit=10&sql=select+*+from+users");
        ubu.replaceQueryParam("limit", 100);

        URI uri = ubu.build();
        Assert.assertEquals(URI.create("http://localhost/?limit=100&sql=select+*+from+users"), uri);
    }

    @Test
    public void testReplaceQueryParamsEncoded4() throws URISyntaxException {
        UriBuilder ubu = UriBuilder.fromUri("http://localhost").
                replaceQuery("limit=10&sql=select+*+from+users");
        ubu.replaceQueryParam("limit", 100);

        URI uri = ubu.build();
        Assert.assertEquals(URI.create("http://localhost/?limit=100&sql=select+*+from+users"), uri);
    }

    @Test
    public void testReplaceFragment() {
        URI uri = UriBuilder.fromUri("http://localhost:8080/a/b/c?a=x&b=y#frag").
                fragment("ment").build();
        Assert.assertEquals(URI.create("http://localhost:8080/a/b/c?a=x&b=y#ment"), uri);
    }

    @Test
    public void testReplaceUri() {
        URI u = URI.create("http://bob@localhost:8080/a/b/c?a=x&b=y#frag");

        URI uri = UriBuilder.fromUri(u).
                uri(URI.create("https://bob@localhost:8080")).build();
        Assert.assertEquals(URI.create("https://bob@localhost:8080/a/b/c?a=x&b=y#frag"), uri);

        uri = UriBuilder.fromUri(u).
                uri(URI.create("https://sue@localhost:8080")).build();
        Assert.assertEquals(URI.create("https://sue@localhost:8080/a/b/c?a=x&b=y#frag"), uri);

        uri = UriBuilder.fromUri(u).
                uri(URI.create("https://sue@localhost:9090")).build();
        Assert.assertEquals(URI.create("https://sue@localhost:9090/a/b/c?a=x&b=y#frag"), uri);

        uri = UriBuilder.fromUri(u).
                uri(URI.create("/x/y/z")).build();
        Assert.assertEquals(URI.create("http://bob@localhost:8080/x/y/z?a=x&b=y#frag"), uri);

        uri = UriBuilder.fromUri(u).
                uri(URI.create("?x=a&b=y")).build();
        Assert.assertEquals(URI.create("http://bob@localhost:8080/a/b/c?x=a&b=y#frag"), uri);

        uri = UriBuilder.fromUri(u).
                uri(URI.create("#ment")).build();
        Assert.assertEquals(URI.create("http://bob@localhost:8080/a/b/c?a=x&b=y#ment"), uri);
    }

    @Test
    public void testSchemeSpecificPart() {
        URI u = URI.create("http://bob@localhost:8080/a/b/c?a=x&b=y#frag");

        URI uri = UriBuilder.fromUri(u).
                schemeSpecificPart("//sue@remotehost:9090/x/y/z?x=a&y=b").build();
        Assert.assertEquals(URI.create("http://sue@remotehost:9090/x/y/z?x=a&y=b#frag"), uri);
    }

    @Test
    public void testAppendPath() {
        URI uri = UriBuilder.fromUri("http://localhost:8080").
                path("a/b/c").build();
        Assert.assertEquals(URI.create("http://localhost:8080/a/b/c"), uri);

        uri = UriBuilder.fromUri("http://localhost:8080/").
                path("a/b/c").build();
        Assert.assertEquals(URI.create("http://localhost:8080/a/b/c"), uri);

        uri = UriBuilder.fromUri("http://localhost:8080").
                path("/a/b/c").build();
        Assert.assertEquals(URI.create("http://localhost:8080/a/b/c"), uri);

        uri = UriBuilder.fromUri("http://localhost:8080/a/b/c/").
                path("/").build();
        Assert.assertEquals(URI.create("http://localhost:8080/a/b/c/"), uri);

        uri = UriBuilder.fromUri("http://localhost:8080/a/b/c/").
                path("/x/y/z").build();
        Assert.assertEquals(URI.create("http://localhost:8080/a/b/c/x/y/z"), uri);

        uri = UriBuilder.fromUri("http://localhost:8080/a/b/c").
                path("/x/y/z").build();
        Assert.assertEquals(URI.create("http://localhost:8080/a/b/c/x/y/z"), uri);

        uri = UriBuilder.fromUri("http://localhost:8080/a/b/c").
                path("x/y/z").build();
        Assert.assertEquals(URI.create("http://localhost:8080/a/b/c/x/y/z"), uri);

        uri = UriBuilder.fromUri("http://localhost:8080/a/b/c").
                path("/").build();
        Assert.assertEquals(URI.create("http://localhost:8080/a/b/c/"), uri);

        uri = UriBuilder.fromUri("http://localhost:8080/a/b/c").
                path("").build();
        Assert.assertEquals(URI.create("http://localhost:8080/a/b/c"), uri);

        uri = UriBuilder.fromUri("http://localhost:8080/a%20/b%20/c%20").
                path("/x /y /z ").build();
        Assert.assertEquals(URI.create("http://localhost:8080/a%20/b%20/c%20/x%20/y%20/z%20"), uri);
    }

    @Test
    public void testAppendSegment() {
        URI uri = UriBuilder.fromUri("http://localhost:8080").
                segment("a/b/c;x").build();
        Assert.assertEquals(URI.create("http://localhost:8080/a%2Fb%2Fc%3Bx"), uri);
    }

    @Test
    public void testRelativeFromUri() {
        URI uri = UriBuilder.fromUri("a/b/c").
                build();
        Assert.assertEquals(URI.create("a/b/c"), uri);

        uri = UriBuilder.fromUri("a/b/c").path("d").
                build();
        Assert.assertEquals(URI.create("a/b/c/d"), uri);

        uri = UriBuilder.fromUri("a/b/c/").path("d").
                build();
        Assert.assertEquals(URI.create("a/b/c/d"), uri);

        uri = UriBuilder.fromUri("a/b/c").path("/d").
                build();
        Assert.assertEquals(URI.create("a/b/c/d"), uri);

        uri = UriBuilder.fromUri("a/b/c/").path("/d").
                build();
        Assert.assertEquals(URI.create("a/b/c/d"), uri);

        uri = UriBuilder.fromUri("").queryParam("x", "y").
                build();
        Assert.assertEquals(URI.create("?x=y"), uri);

    }

    @Test
    public void testRelativefromPath() {
        URI uri = UriBuilder.fromPath("a/b/c").
                build();
        Assert.assertEquals(URI.create("a/b/c"), uri);

        uri = UriBuilder.fromPath("a/b/c").path("d").
                build();
        Assert.assertEquals(URI.create("a/b/c/d"), uri);

        uri = UriBuilder.fromPath("a/b/c/").path("d").
                build();
        Assert.assertEquals(URI.create("a/b/c/d"), uri);

        uri = UriBuilder.fromPath("a/b/c").path("/d").
                build();
        Assert.assertEquals(URI.create("a/b/c/d"), uri);

        uri = UriBuilder.fromPath("a/b/c/").path("/d").
                build();
        Assert.assertEquals(URI.create("a/b/c/d"), uri);

        uri = UriBuilder.fromPath("").queryParam("x", "y").
                build();
        Assert.assertEquals(URI.create("?x=y"), uri);
    }

    @Test
    public void testAppendQueryParams() throws URISyntaxException {
        URI uri = UriBuilder.fromUri("http://localhost:8080/a/b/c?a=x&b=y").
                queryParam("c", "z").build();
        Assert.assertEquals(URI.create("http://localhost:8080/a/b/c?a=x&b=y&c=z"), uri);

        uri = UriBuilder.fromUri("http://localhost:8080/a/b/c?a=x&b=y").
                queryParam("c= ", "z= ").build();
        Assert.assertEquals(URI.create("http://localhost:8080/a/b/c?a=x&b=y&c%3D+=z%3D+"), uri);

        uri = UriBuilder.fromUri(new URI("http://localhost:8080/")).
                queryParam("c", "z").build();
        Assert.assertEquals(URI.create("http://localhost:8080/?c=z"), uri);

        uri = UriBuilder.fromUri(new URI("http://localhost:8080")).
                queryParam("c", "z").build();
        Assert.assertEquals(URI.create("http://localhost:8080/?c=z"), uri);

        uri = UriBuilder.fromUri("http://localhost:8080/").
                queryParam("c", "z").build();
        Assert.assertEquals(URI.create("http://localhost:8080/?c=z"), uri);

        uri = UriBuilder.fromUri("http://localhost:8080").
                queryParam("c", "z").build();
        Assert.assertEquals(URI.create("http://localhost:8080/?c=z"), uri);

        try {
            uri = UriBuilder.fromPath("http://localhost:8080").queryParam("name", "x", null).build();
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        } catch (NullPointerException e) {
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testAppendMatrixParams() {
        URI uri = UriBuilder.fromUri("http://localhost:8080/a/b/c;a=x;b=y").
                matrixParam("c", "z").build();
        Assert.assertEquals(URI.create("http://localhost:8080/a/b/c;a=x;b=y;c=z"), uri);

        uri = UriBuilder.fromUri("http://localhost:8080/a/b/c;a=x;b=y").
                matrixParam("c=/ ;", "z=/ ;").build();
        Assert.assertEquals(URI.create("http://localhost:8080/a/b/c;a=x;b=y;c%3D%2F%20%3B=z%3D%2F%20%3B"), uri);
    }

    @Test
    public void testAppendPathAndMatrixParams() {
        URI uri = UriBuilder.fromUri("http://localhost:8080/").
                path("a").matrixParam("x", "foo").matrixParam("y", "bar").
                path("b").matrixParam("x", "foo").matrixParam("y", "bar").build();
        Assert.assertEquals(URI.create("http://localhost:8080/a;x=foo;y=bar/b;x=foo;y=bar"), uri);
    }

    @Path("resource")
    class Resource {
        @Path("method")
        public
        @GET
        String get() {
            return "";
        }

        @Path("locator")
        public Object locator() {
            return null;
        }
    }

    @Test
    public void testResourceAppendPath() throws NoSuchMethodException {
        URI ub = UriBuilder.fromUri("http://localhost:8080/base").
                path(Resource.class).build();
        Assert.assertEquals(URI.create("http://localhost:8080/base/resource"), ub);

        ub = UriBuilder.fromUri("http://localhost:8080/base").
                path(Resource.class, "get").build();
        Assert.assertEquals(URI.create("http://localhost:8080/base/method"), ub);

        Method get = Resource.class.getMethod("get");
        Method locator = Resource.class.getMethod("locator");
        ub = UriBuilder.fromUri("http://localhost:8080/base").
                path(get).path(locator).build();
        Assert.assertEquals(URI.create("http://localhost:8080/base/method/locator"), ub);
    }

    @Path("resource/{id}")
    class ResourceWithTemplate {
        @Path("method/{id1}")
        public
        @GET
        String get() {
            return "";
        }

        @Path("locator/{id2}")
        public Object locator() {
            return null;
        }
    }

    @Test
    public void testResourceWithTemplateAppendPath() throws NoSuchMethodException {
        URI ub = UriBuilder.fromUri("http://localhost:8080/base").
                path(ResourceWithTemplate.class).build("foo");
        Assert.assertEquals(URI.create("http://localhost:8080/base/resource/foo"), ub);

        ub = UriBuilder.fromUri("http://localhost:8080/base").
                path(ResourceWithTemplate.class, "get").build("foo");
        Assert.assertEquals(URI.create("http://localhost:8080/base/method/foo"), ub);

        Method get = ResourceWithTemplate.class.getMethod("get");
        Method locator = ResourceWithTemplate.class.getMethod("locator");
        ub = UriBuilder.fromUri("http://localhost:8080/base").
                path(get).path(locator).build("foo", "bar");
        Assert.assertEquals(URI.create("http://localhost:8080/base/method/foo/locator/bar"), ub);
    }

    @Path("resource/{id: .+}")
    class ResourceWithTemplateRegex {
        @Path("method/{id1: .+}")
        public
        @GET
        String get() {
            return "";
        }

        @Path("locator/{id2: .+}")
        public Object locator() {
            return null;
        }
    }

    @Test
    public void testResourceWithTemplateRegexAppendPath() throws NoSuchMethodException {
        URI ub = UriBuilder.fromUri("http://localhost:8080/base").
                path(ResourceWithTemplateRegex.class).build("foo");
        Assert.assertEquals(URI.create("http://localhost:8080/base/resource/foo"), ub);

        ub = UriBuilder.fromUri("http://localhost:8080/base").
                path(ResourceWithTemplateRegex.class, "get").build("foo");
        Assert.assertEquals(URI.create("http://localhost:8080/base/method/foo"), ub);

        Method get = ResourceWithTemplateRegex.class.getMethod("get");
        Method locator = ResourceWithTemplateRegex.class.getMethod("locator");
        ub = UriBuilder.fromUri("http://localhost:8080/base").
                path(get).path(locator).build("foo", "bar");
        Assert.assertEquals(URI.create("http://localhost:8080/base/method/foo/locator/bar"), ub);
    }

    @Test
    public void testBuildTemplates() {
        URI uri = UriBuilder.fromUri("http://localhost:8080/a/b/c").
                path("/{foo}/{bar}/{baz}/{foo}").build("x", "y", "z");
        Assert.assertEquals(URI.create("http://localhost:8080/a/b/c/x/y/z/x"), uri);

        Map<String, Object> m = new HashMap<String, Object>();
        m.put("foo", "x");
        m.put("bar", "y");
        m.put("baz", "z");
        uri = UriBuilder.fromUri("http://localhost:8080/a/b/c").
                path("/{foo}/{bar}/{baz}/{foo}").buildFromMap(m);
        Assert.assertEquals(URI.create("http://localhost:8080/a/b/c/x/y/z/x"), uri);
    }

    @Test
    public void testBuildTemplatesWithNameAuthority() {
        URI uri = UriBuilder.fromUri("http://x_y.com:8080/a/b/c").
                path("/{foo}/{bar}/{baz}/{foo}").build("x", "y", "z");
        Assert.assertEquals(URI.create("http://x_y.com:8080/a/b/c/x/y/z/x"), uri);

        Map<String, Object> m = new HashMap<String, Object>();
        m.put("foo", "x");
        m.put("bar", "y");
        m.put("baz", "z");
        uri = UriBuilder.fromUri("http://x_y.com:8080/a/b/c").
                path("/{foo}/{bar}/{baz}/{foo}").buildFromMap(m);
        Assert.assertEquals(URI.create("http://x_y.com:8080/a/b/c/x/y/z/x"), uri);
    }

    @Test
    public void testBuildFromMap() {
        Map maps = new HashMap();
        maps.put("x", null);
        maps.put("y", "/path-absolute/test1");
        maps.put("z", "fred@example.com");
        maps.put("w", "path-rootless/test2");
        maps.put("u", "extra");

        boolean caught = false;

        try {
            System.out.println(UriBuilder.fromPath("").path("{w}/{x}/{y}/{z}/{x}").
                    buildFromEncodedMap(maps));

        } catch (IllegalArgumentException ex) {
            caught = true;
        }

        Assert.assertTrue(caught);
    }

    @Test
    public void testBuildQueryTemplates() {
        URI uri = UriBuilder.fromUri("http://localhost:8080/a/b/c").
                queryParam("a", "{b}").build("=+&%xx%20");
        Assert.assertEquals(URI.create("http://localhost:8080/a/b/c?a=%3D%2B%26%25xx%2520"), uri);

        Map<String, Object> m = new HashMap<String, Object>();
        m.put("b", "=+&%xx%20");
        uri = UriBuilder.fromUri("http://localhost:8080/a/b/c").
                queryParam("a", "{b}").buildFromMap(m);
        Assert.assertEquals(URI.create("http://localhost:8080/a/b/c?a=%3D%2B%26%25xx%2520"), uri);
    }

    @Test
    public void testBuildFromEncodedQueryTemplates() {
        URI uri = UriBuilder.fromUri("http://localhost:8080/a/b/c").
                queryParam("a", "{b}").buildFromEncoded("=+&%xx%20");
        Assert.assertEquals(URI.create("http://localhost:8080/a/b/c?a=%3D%2B%26%25xx%20"), uri);

        Map<String, Object> m = new HashMap<String, Object>();
        m.put("b", "=+&%xx%20");
        uri = UriBuilder.fromUri("http://localhost:8080/a/b/c").
                queryParam("a", "{b}").buildFromEncodedMap(m);
        Assert.assertEquals(URI.create("http://localhost:8080/a/b/c?a=%3D%2B%26%25xx%20"), uri);
    }

    @Test
    public void testBuildFragmentTemplates() {
        URI uri = UriBuilder.fromUri("http://localhost:8080/a/b/c").
                path("/{foo}/{bar}/{baz}/{foo}").fragment("{foo}").build("x", "y", "z");
        Assert.assertEquals(URI.create("http://localhost:8080/a/b/c/x/y/z/x#x"), uri);

        Map<String, Object> m = new HashMap<String, Object>();
        m.put("foo", "x");
        m.put("bar", "y");
        m.put("baz", "z");
        uri = UriBuilder.fromUri("http://localhost:8080/a/b/c").
                path("/{foo}/{bar}/{baz}/{foo}").fragment("{foo}").buildFromMap(m);
        Assert.assertEquals(URI.create("http://localhost:8080/a/b/c/x/y/z/x#x"), uri);
    }

    @Test
    public void testTemplatesDefaultPort() {
        URI uri = UriBuilder.fromUri("http://localhost/a/b/c").
                path("/{foo}/{bar}/{baz}/{foo}").build("x", "y", "z");
        Assert.assertEquals(URI.create("http://localhost/a/b/c/x/y/z/x"), uri);

        Map<String, Object> m = new HashMap<String, Object>();
        m.put("foo", "x");
        m.put("bar", "y");
        m.put("baz", "z");
        uri = UriBuilder.fromUri("http://localhost/a/b/c").
                path("/{foo}/{bar}/{baz}/{foo}").buildFromMap(m);
        Assert.assertEquals(URI.create("http://localhost/a/b/c/x/y/z/x"), uri);
    }

    @Test
    public void testClone() {
        UriBuilder ub = UriBuilder.fromUri("http://user@localhost:8080/?query#fragment").path("a");
        URI full = ub.clone().path("b").build();
        URI base = ub.build();

        Assert.assertEquals(URI.create("http://user@localhost:8080/a?query#fragment"), base);
        Assert.assertEquals(URI.create("http://user@localhost:8080/a/b?query#fragment"), full);
    }

    @Test
    public void testIllegalArgumentException() {
        boolean caught = false;
        try {
            UriBuilder.fromPath(null);
        } catch (IllegalArgumentException e) {
            caught = true;
        }
        Assert.assertTrue(caught);

        caught = false;
        try {
            UriBuilder.fromUri((URI) null);
        } catch (IllegalArgumentException e) {
            caught = true;
        }
        Assert.assertTrue(caught);

        caught = false;
        try {
            UriBuilder.fromUri((String) null);
        } catch (IllegalArgumentException e) {
            caught = true;
        }
        Assert.assertTrue(caught);
    }


    @Test
    public void testIllegalUri() {
        boolean caught = false;
        try {
            new UriBuilderImpl().uri("http://localhost:8080/^").build();
        } catch (UriBuilderException ex) {
            caught = true;
        }
        Assert.assertTrue(caught);

        caught = false;
        try {
            new UriBuilderImpl().uri(URI.create("http://localhost:8080/^"));
        } catch (IllegalArgumentException ex) {
            caught = true;
        }
        Assert.assertTrue(caught);
    }

    @Test
    public void testVariableWithoutValue() {
        boolean caught = false;
        try {
            UriBuilder.fromPath("http://localhost:8080").
                    path("/{a}/{b}").
                    buildFromEncoded("aVal");

        } catch (IllegalArgumentException e) {
            caught = true;
        }
        Assert.assertTrue(caught);
    }

    @Test
    public void testPortValue() {
        boolean caught = false;
        try {
            UriBuilder.fromPath("http://localhost").port(-2);
        } catch (IllegalArgumentException e) {
            caught = true;
        }
        Assert.assertTrue(caught);
    }

    @Test
    public void testPortSetting() throws URISyntaxException {
        URI uri;

        uri = new UriBuilderImpl().uri("http://localhost").port(8080).build();
        Assert.assertEquals(URI.create("http://localhost:8080"), uri);

        uri = new UriBuilderImpl().uri(new URI("http://localhost")).port(8080).build();
        Assert.assertEquals(URI.create("http://localhost:8080"), uri);

        uri = new UriBuilderImpl().uri("http://localhost/").port(8080).build();
        Assert.assertEquals(URI.create("http://localhost:8080/"), uri);

        uri = new UriBuilderImpl().uri(new URI("http://localhost/")).port(8080).build();
        Assert.assertEquals(URI.create("http://localhost:8080/"), uri);
    }

    @Test
    public void testHostValue() {
        boolean caught = false;
        try {
            UriBuilder.fromPath("http://localhost").host("");
        } catch (IllegalArgumentException e) {
            caught = true;
        }
        Assert.assertTrue(caught);

        URI uri = UriBuilder.fromPath("").host("abc").build();
        Assert.assertEquals(URI.create("//abc"), uri);

        uri = UriBuilder.fromPath("").host("abc").host(null).build();
        Assert.assertEquals(URI.create(""), uri);
    }


    @Test
    public void testEncodeTemplateNames() {
        URI uri = UriBuilder.fromPath("http://localhost:8080").
                path("/{a}/{b}").
                replaceQuery("q={c}").
                build();
        Assert.assertEquals(URI.create("http://localhost:8080/%7Ba%7D/%7Bb%7D?q=%7Bc%7D"), uri);
    }

}
