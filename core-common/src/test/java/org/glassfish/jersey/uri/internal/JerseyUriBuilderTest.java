/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2015 Oracle and/or its affiliates. All rights reserved.
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

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.uri.UriComponent;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Uri builder implementation test.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @author Martin Matula
 * @author Miroslav Fuksa
 * @author Paul Sandoz
 * @author Vetle Leinonen-Roeim (vetle at roeim.net)
 */
public class JerseyUriBuilderTest {

    public JerseyUriBuilderTest() {
    }

    // Reproducer for JERSEY-2537
    @Test
    public void shouldKeepTrailingSlash() throws MalformedURLException, URISyntaxException {
        final URL url = new URL("http://example.com/authentications;email=joe@joe.com/");
        final UriBuilder builder = UriBuilder.fromPath(url.getPath()).replaceMatrix(null);

        final URI result = builder.build();
        assertEquals("/authentications/", result.toString());
    }

    // Reproducer for JERSEY-2537
    @Test
    public void shouldRemoveAllIncludingSemicolon() throws MalformedURLException, URISyntaxException {
        final URL url = new URL("http://example.com/authentications;email=joe@joe.com");
        final UriBuilder builder = UriBuilder.fromPath(url.getPath()).replaceMatrix(null);

        final URI result = builder.build();
        assertEquals("/authentications", result.toString());
    }

    // Reproducer for JERSEY-2537
    @Test
    public void shouldLeaveURIUntouched() {
        final UriBuilder builder = UriBuilder.fromPath("/apples;order=random;color=blue/2006").replaceMatrix(null);
        final URI result = builder.build();
        assertEquals("/apples;order=random;color=blue/2006", result.toString());
    }

    // Reproducer for JERSEY-2537
    @Test
    public void shouldLeaveURIUntouchedAndKeepSlash() {
        final UriBuilder builder = UriBuilder.fromPath("/apples;order=random;color=blue/2006/").replaceMatrix(null);
        final URI result = builder.build();
        assertEquals("/apples;order=random;color=blue/2006/", result.toString());
    }

    // Reproducer for JERSEY-2537
    @Test
    public void shouldOnlyRemoveMatrixInFinalSegment() {
        final UriBuilder builder = UriBuilder.fromPath("/apples;order=random;color=blue/2006/bar;zot=baz").replaceMatrix(null);
        final URI result = builder.build();
        assertEquals("/apples;order=random;color=blue/2006/bar", result.toString());
    }

    // Reproducer for JERSEY-2537
    @Test
    public void shouldOnlyRemoveMatrixInFinalSegmentAndKeepSlash() {
        final UriBuilder builder = UriBuilder.fromPath("/apples;order=random;color=blue/2006/bar;zot=baz/").replaceMatrix(null);
        final URI result = builder.build();
        assertEquals("/apples;order=random;color=blue/2006/bar/", result.toString());
    }

    // Reproducer for JERSEY-2036
    @Test
    public void testReplaceNonAsciiQueryParam() throws UnsupportedEncodingException, MalformedURLException, URISyntaxException {
        final URL url = new URL("http://example.com/getMyName?néme=t");
        final String query = url.getQuery();

        final UriBuilder builder = UriBuilder.fromPath(url.getPath()).scheme(url.getProtocol()).host(url.getHost())
                .port(url.getPort())
                .replaceQuery(query).fragment(url.getRef());

        // Replace QueryParam.
        final String parmName = "néme";
        final String value = "value";

        builder.replaceQueryParam(parmName, value);

        final URI result = builder.build();
        final URI expected = new URI("http://example.com/getMyName?néme=value");
        assertEquals(expected.toASCIIString(), result.toASCIIString());
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

    // Reproducer for JERSEY-1800
    @Test
    public void testEmptyUriString() throws URISyntaxException {
        final URI uri = URI.create("");
        JerseyUriBuilder ub = new JerseyUriBuilder().uri("news:comp.lang.java").uri(uri);
        assertEquals("news:", ub.toTemplate());
        // note that even though the URI is valid according to RFC 3986,
        // it is not possible to create a java.net.URI from this builder if SSP is empty

        ub = new JerseyUriBuilder().uri("news:comp.lang.java").uri("");
        assertEquals("news:", ub.toTemplate());
        // note that even though the URI is valid according to RFC 3986,
        // it is not possible to create a java.net.URI from this builder if SSP is empty
    }

    // Reproducer for JERSEY-2753
    @Test
    public void testUriBuilderShouldLeaveRelativePathRelative() {
        final UriBuilder builder = JerseyUriBuilder.fromPath("");
        builder.scheme("http");
        builder.replacePath("path");

        assertEquals("http:path", builder.build().toString());
    }

    @Test
    public void testToTemplate() throws URISyntaxException {
        final JerseyUriBuilder ub = new JerseyUriBuilder().uri(new URI("http://examples.jersey.java.net/")).userInfo("{T1}")
                .path("{T2}").segment("{T3}").queryParam("a", "{T4}", "v1")
                .queryParam("b", "v2");
        assertEquals("http://{T1}@examples.jersey.java.net/{T2}/{T3}?a={T4}&a=v1&b=v2", ub.toTemplate());

        ub.queryParam("a", "v3").queryParam("c", "v4");
        assertEquals("http://{T1}@examples.jersey.java.net/{T2}/{T3}?a={T4}&a=v1&b=v2&a=v3&c=v4", ub.toTemplate());
    }

    @Test
    public void testPathTemplateValueEncoding() throws URISyntaxException {
        String result;
        result = new JerseyUriBuilder().uri(new URI("http://examples.jersey.java.net/")).userInfo("a/b").path("a/b")
                .segment("a/b").build().toString();
        assertEquals("http://a%2Fb@examples.jersey.java.net/a/b/a%2Fb", result);

        result = new JerseyUriBuilder().uri(new URI("http://examples.jersey.java.net/")).userInfo("{T1}").path("{T2}")
                .segment("{T3}").build("a/b", "a/b", "a/b").toString();
        assertEquals("http://a%2Fb@examples.jersey.java.net/a%2Fb/a%2Fb", result);

        result = new JerseyUriBuilder().uri(new URI("http://examples.jersey.java.net/")).userInfo("{T1}").path("{T2}")
                .segment("{T3}").build(new Object[] {"a/b", "a/b", "a/b"}, false).toString();
        assertEquals("http://a%2Fb@examples.jersey.java.net/a/b/a/b", result);

        result = new JerseyUriBuilder().uri(new URI("http://examples.jersey.java.net/")).userInfo("{T1}").path("{T2}")
                .segment("{T2}").build("a@b", "a@b").toString();
        assertEquals("http://a%40b@examples.jersey.java.net/a@b/a@b", result);

        result = new JerseyUriBuilder().uri(new URI("http://examples.jersey.java.net/")).userInfo("{T}").path("{T}")
                .segment("{T}").build("a@b").toString();
        assertEquals("http://a%40b@examples.jersey.java.net/a@b/a@b", result);
    }

    @Test
    public void testReplaceMatrixParamWithNull() {
        final UriBuilder builder = new JerseyUriBuilder().matrixParam("matrix", "param1", "param2");
        builder.replaceMatrixParam("matrix", (Object[]) null);
        assertEquals(builder.build().toString(), "");
    }

    // for completeness (added along with regression tests for JERSEY-1114)
    @Test
    public void testBuildNoSlashUri() {
        final UriBuilder builder = new JerseyUriBuilder().uri(URI.create("http://localhost:8080")).path("test");
        assertEquals("http://localhost:8080/test", builder.build().toString());
    }

    // regression test for JERSEY-1114
    @Test
    public void testBuildFromMapNoSlashInUri() {
        final UriBuilder builder = new JerseyUriBuilder().uri(URI.create("http://localhost:8080")).path("test");
        assertEquals("http://localhost:8080/test", builder.buildFromMap(new HashMap<String, Object>()).toString());
    }

    // regression test for JERSEY-1114
    @Test
    public void testBuildFromArrayNoSlashInUri() {
        final UriBuilder builder = new JerseyUriBuilder().uri(URI.create("http://localhost:8080")).path("test");
        assertEquals("http://localhost:8080/test", builder.build("testing").toString());
    }

    @Test
    public void testReplaceNullMatrixParam() {
        try {
            new JerseyUriBuilder().replaceMatrixParam(null, "param");
        } catch (final IllegalArgumentException e) {
            return;
        } catch (final Exception e) {
            fail("Expected IllegalArgumentException but got " + e.toString());
        }
        fail("Expected IllegalArgumentException but no exception was thrown.");
    }

    // regression test for JERSEY-1081
    @Test
    public void testReplaceQueryParam() {
        final URI uri = new JerseyUriBuilder().path("http://localhost/").replaceQueryParam("foo", "test").build();
        assertEquals("http://localhost/?foo=test", uri.toString());
    }

    // regression test for JERSEY-1081
    @Test
    public void testReplaceQueryParamAndClone() {
        final URI uri = new JerseyUriBuilder().path("http://localhost/").replaceQueryParam("foo", "test").clone().build();
        assertEquals("http://localhost/?foo=test", uri.toString());
    }

    // regression test for JERSEY-1341
    @Test
    public void testEmptyQueryParamValue() {
        final URI uri = new JerseyUriBuilder().path("http://localhost/").queryParam("test", "").build();
        assertEquals("http://localhost/?test=", uri.toString());
    }

    // regression test for JERSEY-1457
    @Test
    public void testChangeSspViaStringUriTemplate() throws Exception {
        final String[] origUris = new String[] {"news:comp.lang.java", "tel:+1-816-555-1212"};
        final URI[] replaceUris = new URI[] {new URI(null, "news.lang.java", null), new URI(null, "+1-866-555-1212", null)};
        final String[] results = new String[] {"news:news.lang.java", "tel:+1-866-555-1212"};
        int i = 0;
        while (i < origUris.length) {
            assertEquals(results[i],
                    UriBuilder.fromUri(new URI(origUris[i])).uri(replaceUris[i].toASCIIString()).build().toString());
            i++;
        }
    }

    @Test
    public void testChangeUriStringAfterChangingOpaqueSchemeToHttp() {
        assertEquals("http://www.example.org/test",
                UriBuilder.fromUri("tel:+1-816-555-1212").scheme("http").uri("//www.{host}.org").path("test").build("example")
                        .toString());
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
        testUri("a://#fragment"); // fails in JerseyUriBuilder
        testUri("a://?query");

        // fails: opaque uris are not supported by UriTemplate
        final URI uri = new JerseyUriBuilder().uri("{scheme}://{mailto}").build("mailto", "email@test.ttt");
        assertEquals("mailto:email@test.ttt", uri.toString());
    }

    @Test
    public void testUriBuilderTemplates() {
        URI uri = new JerseyUriBuilder().uri("http://localhost:8080/{path}").build("a/b/c");
        assertEquals("http://localhost:8080/a%2Fb%2Fc", uri.toString());

        uri = new JerseyUriBuilder().uri("{scheme}://{host}").build("http", "localhost");
        assertEquals("http://localhost", uri.toString());

        uri = new JerseyUriBuilder().uri("http://{host}:8080/{path}").build("l", "a/b/c");
        assertEquals("http://l:8080/a%2Fb%2Fc", uri.toString());

        uri = new JerseyUriBuilder().uri("{scheme}://{host}:{port}/{path}").build("s", "h", new Integer(1), "a");
        assertEquals("s://h:1/a", uri.toString());

        final Map<String, Object> values = new HashMap<String, Object>();
        values.put("scheme", "s");
        values.put("host", "h");
        values.put("port", 1);
        values.put("path", "p/p");
        values.put("query", "q");
        values.put("fragment", "f");

        uri = new JerseyUriBuilder().uri("{scheme}://{host}:{port}/{path}?{query}#{fragment}").buildFromMap(values);
        assertEquals("s://h:1/p%2Fp?q#f", uri.toString());

        uri = new JerseyUriBuilder().uri("{scheme}://{host}:{port}/{path}/{path2}").build("s", "h", new Integer(1), "a", "b");
        assertEquals("s://h:1/a/b", uri.toString());

        uri = new JerseyUriBuilder().uri("{scheme}://{host}:{port}/{path}/{path2}").build("s", "h", new Integer(1), "a", "b");
        assertEquals("s://h:1/a/b", uri.toString());

        uri = new JerseyUriBuilder().uri("//{host}:{port}/{path}/{path2}").build("h", new Integer(1), "a", "b");
        assertEquals("//h:1/a/b", uri.toString());

        uri = new JerseyUriBuilder().uri("/{a}/{a}/{b}").build("a", "b");
        assertEquals("/a/a/b", uri.toString());

        uri = new JerseyUriBuilder().uri("/{a}/{a}/{b}?{queryParam}").build("a", "b", "query");
        assertEquals("/a/a/b?query", uri.toString());

        // partial templates
        uri = new JerseyUriBuilder().uri("/{a}xx/{a}/{b}?{queryParam}").build("a", "b", "query");
        assertEquals("/axx/a/b?query", uri.toString());

        uri = new JerseyUriBuilder().uri("my{scheme}://my{host}:1{port}/my{path}/my{path2}")
                .build("s", "h", new Integer(1), "a", "b/c");
        assertEquals("mys://myh:11/mya/myb%2Fc", uri.toString());

        uri = new JerseyUriBuilder().uri("my{scheme}post://my{host}post:5{port}9/my{path}post/my{path2}post")
                .build("s", "h", new Integer(1), "a", "b");
        assertEquals("myspost://myhpost:519/myapost/mybpost", uri.toString());
    }

    @Test
    public void testUriBuilderTemplatesNotEncodedSlash() {
        URI uri = new JerseyUriBuilder().uri("http://localhost:8080/{path}").build(new Object[] {"a/b/c"}, false);
        assertEquals("http://localhost:8080/a/b/c", uri.toString());

        uri = new JerseyUriBuilder().uri("http://{host}:8080/{path}").build(new Object[] {"l", "a/b/c"}, false);
        assertEquals("http://l:8080/a/b/c", uri.toString());

        final Map<String, Object> values = new HashMap<String, Object>();
        values.put("scheme", "s");
        values.put("host", "h");
        values.put("port", 1);
        values.put("path", "p/p");
        values.put("query", "q");
        values.put("fragment", "f");

        uri = new JerseyUriBuilder().uri("{scheme}://{host}:{port}/{path}?{query}#{fragment}").buildFromMap(values, false);
        assertEquals("s://h:1/p/p?q#f", uri.toString());
    }

    private void testUri(final String input) {
        final URI uri = new JerseyUriBuilder().uri(input).clone().build();

        final URI originalUri = URI.create(input);
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
        final URI uri = UriBuilder.fromUri("mailto:a@b").build();
        Assert.assertEquals("mailto:a@b", uri.toString());
    }

    @Test
    public void testOpaqueUriReplaceSchemeSpecificPart() {
        final URI uri = UriBuilder.fromUri("mailto:a@b").schemeSpecificPart("c@d").build();
        Assert.assertEquals("mailto:c@d", uri.toString());
    }

    @Test
    public void testOpaqueReplaceUri() {
        final URI uri = UriBuilder.fromUri("mailto:a@b").uri(URI.create("c@d")).build();
        Assert.assertEquals("mailto:c@d", uri.toString());
    }

    @Test
    public void testReplaceScheme() {
        URI uri = UriBuilder.fromUri("http://localhost:8080/a/b/c").scheme("https").build();
        Assert.assertEquals("https://localhost:8080/a/b/c", uri.toString());

        uri = UriBuilder.fromUri("http://localhost:8080/a/b/c").scheme(null).build();
        Assert.assertEquals("//localhost:8080/a/b/c", uri.toString());

        uri = UriBuilder.fromUri("http://localhost:8080/a/b/c").scheme(null).host(null).build();
        Assert.assertEquals("//:8080/a/b/c", uri.toString());

        uri = UriBuilder.fromUri("http://localhost:8080/a/b/c").scheme(null).host(null).port(-1).build();
        Assert.assertEquals("/a/b/c", uri.toString());
    }

    @Test
    public void testReplaceSchemeSpecificPart() {
        final URI uri = UriBuilder.fromUri("http://localhost:8080/a/b/c").schemeSpecificPart("//localhost:8080/a/b/c/d").build();
        Assert.assertEquals(URI.create("http://localhost:8080/a/b/c/d"), uri);
    }

    @Test
    public void testNameAuthorityUri() {
        final URI uri = UriBuilder.fromUri("http://x_y/a/b/c").build();
        Assert.assertEquals(URI.create("http://x_y/a/b/c"), uri);
    }

    @Test
    public void testReplaceNameAuthorityUriWithHost() {
        final URI uri = UriBuilder.fromUri("http://x_y.com/a/b/c").host("xy.com").build();
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
        final URI uri = UriBuilder.fromUri("http://bob@localhost:8080/a/b/c").userInfo("sue").build();
        Assert.assertEquals(URI.create("http://sue@localhost:8080/a/b/c"), uri);
    }

    @Test
    public void testReplaceHost() {
        URI uri = UriBuilder.fromUri("http://localhost:8080/a/b/c").host("a.com").build();
        Assert.assertEquals(URI.create("http://a.com:8080/a/b/c"), uri);

        uri = UriBuilder.fromUri("http://localhost:8080/a/b/c").host("[::FFFF:129.144.52.38]").build();
        Assert.assertEquals(URI.create("http://[::FFFF:129.144.52.38]:8080/a/b/c"), uri);
    }

    @Test
    public void testReplacePort() {
        URI uri = UriBuilder.fromUri("http://localhost:8080/a/b/c").port(9090).build();
        Assert.assertEquals(URI.create("http://localhost:9090/a/b/c"), uri);

        uri = UriBuilder.fromUri("http://localhost:8080/a/b/c").port(-1).build();
        Assert.assertEquals(URI.create("http://localhost/a/b/c"), uri);
    }

    @Test
    public void testReplacePath() {
        final URI uri = UriBuilder.fromUri("http://localhost:8080/a/b/c").replacePath("/x/y/z").build();
        Assert.assertEquals(URI.create("http://localhost:8080/x/y/z"), uri);
    }

    @Test
    public void testReplacePathNull() {
        final URI uri = UriBuilder.fromUri("http://localhost:8080/a/b/c").replacePath(null).build();

        Assert.assertEquals(URI.create("http://localhost:8080"), uri);
    }

    @Test
    public void testReplaceMatrix() {
        final URI uri = UriBuilder.fromUri("http://localhost:8080/a/b/c;a=x;b=y").replaceMatrix("x=a;y=b").build();
        Assert.assertEquals(URI.create("http://localhost:8080/a/b/c;x=a;y=b"), uri);
    }

    @Test
    public void testReplaceMatrixParams() {
        final UriBuilder ubu = UriBuilder.fromUri("http://localhost:8080/a/b/c;a=x;b=y").replaceMatrixParam("a", "z", "zz");

        {
            final URI uri = ubu.build();
            final List<PathSegment> ps = UriComponent.decodePath(uri, true);
            final MultivaluedMap<String, String> mps = ps.get(2).getMatrixParameters();
            final List<String> a = mps.get("a");
            Assert.assertEquals(2, a.size());
            Assert.assertEquals("z", a.get(0));
            Assert.assertEquals("zz", a.get(1));
            final List<String> b = mps.get("b");
            Assert.assertEquals(1, b.size());
            Assert.assertEquals("y", b.get(0));
        }

        {
            final URI uri = ubu.replaceMatrixParam("a", "_z_", "_zz_").build();
            final List<PathSegment> ps = UriComponent.decodePath(uri, true);
            final MultivaluedMap<String, String> mps = ps.get(2).getMatrixParameters();
            final List<String> a = mps.get("a");
            Assert.assertEquals(2, a.size());
            Assert.assertEquals("_z_", a.get(0));
            Assert.assertEquals("_zz_", a.get(1));
            final List<String> b = mps.get("b");
            Assert.assertEquals(1, b.size());
            Assert.assertEquals("y", b.get(0));
        }

        {
            final URI uri = JerseyUriBuilder.fromUri("http://localhost:8080/a/b/c;a=x;b=y").replaceMatrixParam("a", "z", "zz")
                    .matrixParam("c", "c").path(
                            "d").build();

            final List<PathSegment> ps = UriComponent.decodePath(uri, true);
            final MultivaluedMap<String, String> mps = ps.get(2).getMatrixParameters();
            final List<String> a = mps.get("a");
            Assert.assertEquals(2, a.size());
            Assert.assertEquals("z", a.get(0));
            Assert.assertEquals("zz", a.get(1));
            final List<String> b = mps.get("b");
            Assert.assertEquals(1, b.size());
            Assert.assertEquals("y", b.get(0));
            final List<String> c = mps.get("c");
            Assert.assertEquals(1, c.size());
            Assert.assertEquals("c", c.get(0));
        }

        {
            final URI uri = JerseyUriBuilder.fromUri("http://localhost:8080/a;w=123;q=15/b/c;a=x;b=y").replaceMatrixParam("a",
                    "z", "zz").matrixParam("c", "c").path("d").build();

            final List<PathSegment> ps = UriComponent.decodePath(uri, true);
            MultivaluedMap<String, String> mps = ps.get(0).getMatrixParameters();

            List<String> w = mps.get("w");
            Assert.assertEquals(1, w.size());
            Assert.assertEquals("123", w.get(0));

            w = mps.get("q");
            Assert.assertEquals(1, w.size());
            Assert.assertEquals("15", w.get(0));

            mps = ps.get(2).getMatrixParameters();
            final List<String> a = mps.get("a");
            Assert.assertEquals(2, a.size());
            Assert.assertEquals("z", a.get(0));
            Assert.assertEquals("zz", a.get(1));
            final List<String> b = mps.get("b");
            Assert.assertEquals(1, b.size());
            Assert.assertEquals("y", b.get(0));
            final List<String> c = mps.get("c");
            Assert.assertEquals(1, c.size());
            Assert.assertEquals("c", c.get(0));
        }
    }

    @Test
    public void testReplaceMatrixParamsEmpty() {
        final UriBuilder ubu = UriBuilder.fromUri("http://localhost:8080/a/b/c").replaceMatrixParam("a", "z", "zz");
        {
            final URI uri = ubu.build();
            final List<PathSegment> ps = UriComponent.decodePath(uri, true);
            final MultivaluedMap<String, String> mps = ps.get(2).getMatrixParameters();
            final List<String> a = mps.get("a");
            Assert.assertEquals(2, a.size());
            Assert.assertEquals("z", a.get(0));
            Assert.assertEquals("zz", a.get(1));
        }
    }

    @Test
    public void testReplaceMatrixParamsEncoded() throws URISyntaxException {
        final UriBuilder ubu = UriBuilder.fromUri("http://localhost/").replaceMatrix("limit=10;sql=select+*+from+users");
        ubu.replaceMatrixParam("limit", 100);

        final URI uri = ubu.build();
        Assert.assertEquals(URI.create("http://localhost/;limit=100;sql=select+*+from+users"), uri);
    }

    @Test
    public void testMatrixParamsWithTheSameName() {
        UriBuilder first = UriBuilder.fromUri("http://www.com/").replaceMatrixParam("example", "one", "two");
        first = first.path("/child");
        first = first.replaceMatrixParam("example", "another");

        Assert.assertEquals("http://www.com/;example=one;example=two/child;example=another", first.build().toString());
    }

    @Test
    public void testMatrixParamsWithTheDifferentName() {
        UriBuilder first = UriBuilder.fromUri("http://www.com/").replaceMatrixParam("example", "one", "two");
        first = first.path("/child");
        first = first.replaceMatrixParam("other", "another");

        Assert.assertEquals("http://www.com/;example=one;example=two/child;other=another", first.build().toString());
    }

    @Test
    public void testReplaceQuery() {
        final URI uri = UriBuilder.fromUri("http://localhost:8080/a/b/c?a=x&b=y").replaceQuery("x=a&y=b").build();
        Assert.assertEquals(URI.create("http://localhost:8080/a/b/c?x=a&y=b"), uri);
    }

    @Test
    public void testBuildEncodedQuery() {
        URI u = UriBuilder.fromPath("").queryParam("y", "1 %2B 2").build();
        Assert.assertEquals(URI.create("?y=1+%2B+2"), u);

        // Issue 216
        u = UriBuilder.fromPath("http://localhost:8080").path("/{x}/{y}/{z}/{x}").buildFromEncoded("%xy", " ", "=");
        Assert.assertEquals(URI.create("http://localhost:8080/%25xy/%20/=/%25xy"), u);
    }

    @Test
    public void testReplaceQueryParams() {
        final UriBuilder ubu = UriBuilder.fromUri("http://localhost:8080/a/b/c?a=x&b=y").replaceQueryParam("a", "z", "zz")
                .queryParam("c", "c");

        {
            final URI uri = ubu.build();

            final MultivaluedMap<String, String> qps = UriComponent.decodeQuery(uri, true);
            final List<String> a = qps.get("a");
            Assert.assertEquals(2, a.size());
            Assert.assertEquals("z", a.get(0));
            Assert.assertEquals("zz", a.get(1));
            final List<String> b = qps.get("b");
            Assert.assertEquals(1, b.size());
            Assert.assertEquals("y", b.get(0));
            final List<String> c = qps.get("c");
            Assert.assertEquals(1, c.size());
            Assert.assertEquals("c", c.get(0));
        }

        {
            final URI uri = ubu.replaceQueryParam("a", "_z_", "_zz_").build();

            final MultivaluedMap<String, String> qps = UriComponent.decodeQuery(uri, true);
            final List<String> a = qps.get("a");
            Assert.assertEquals(2, a.size());
            Assert.assertEquals("_z_", a.get(0));
            Assert.assertEquals("_zz_", a.get(1));
            final List<String> b = qps.get("b");
            Assert.assertEquals(1, b.size());
            Assert.assertEquals("y", b.get(0));
            final List<String> c = qps.get("c");
            Assert.assertEquals(1, c.size());
            Assert.assertEquals("c", c.get(0));
        }

        // issue 257 - param is removed after setting it to null
        {
            final URI u1 = UriBuilder.fromPath("http://localhost:8080").queryParam("x", "10")
                    .replaceQueryParam("x", (Object[]) null)
                    .build();
            Assert.assertTrue(u1.toString().equals("http://localhost:8080"));

            final URI u2 = UriBuilder.fromPath("http://localhost:8080").queryParam("x", "10").replaceQueryParam("x").build();
            Assert.assertTrue(u2.toString().equals("http://localhost:8080"));
        }

        // issue 257 - IllegalArgumentException
        {
            boolean caught = false;

            try {
                UriBuilder.fromPath("http://localhost:8080").queryParam("x", "10").replaceQueryParam("x", "1", null, "2").build();
            } catch (final IllegalArgumentException iae) {
                caught = true;
            }

            Assert.assertTrue(caught);
        }

    }

    @Test
    public void testReplaceQueryParamsEmpty() {
        final UriBuilder ubu = UriBuilder.fromUri("http://localhost:8080/a/b/c").replaceQueryParam("a", "z", "zz")
                .queryParam("c", "c");

        {
            final URI uri = ubu.build();

            final MultivaluedMap<String, String> qps = UriComponent.decodeQuery(uri, true);
            final List<String> a = qps.get("a");
            Assert.assertEquals(2, a.size());
            Assert.assertEquals("z", a.get(0));
            Assert.assertEquals("zz", a.get(1));
            final List<String> c = qps.get("c");
            Assert.assertEquals(1, c.size());
            Assert.assertEquals("c", c.get(0));
        }
    }

    @Test
    public void testReplaceQueryParamsEncoded1() throws URISyntaxException {
        final UriBuilder ubu = UriBuilder.fromUri(new URI("http://localhost/")).replaceQuery("limit=10&sql=select+*+from+users");
        ubu.replaceQueryParam("limit", 100);

        final URI uri = ubu.build();
        Assert.assertEquals(URI.create("http://localhost/?limit=100&sql=select+%2A+from+users"), uri);
    }

    @Test
    public void testReplaceQueryParamsEncoded2() throws URISyntaxException {
        final UriBuilder ubu = UriBuilder.fromUri(new URI("http://localhost")).replaceQuery("limit=10&sql=select+*+from+users");
        ubu.replaceQueryParam("limit", 100);

        final URI uri = ubu.build();
        Assert.assertEquals(URI.create("http://localhost/?limit=100&sql=select+%2A+from+users"), uri);
    }

    @Test
    public void testReplaceQueryParamsEncoded3() throws URISyntaxException {
        final UriBuilder ubu = UriBuilder.fromUri("http://localhost/").replaceQuery("limit=10&sql=select+*+from+users");
        ubu.replaceQueryParam("limit", 100);

        final URI uri = ubu.build();
        Assert.assertEquals(URI.create("http://localhost/?limit=100&sql=select+%2A+from+users"), uri);
    }

    @Test
    public void testReplaceQueryParamsEncoded4() throws URISyntaxException {
        final UriBuilder ubu = UriBuilder.fromUri("http://localhost").replaceQuery("limit=10&sql=select+*+from+users");
        ubu.replaceQueryParam("limit", 100);

        final URI uri = ubu.build();
        Assert.assertEquals(URI.create("http://localhost/?limit=100&sql=select+%2A+from+users"), uri);
    }

    @Test
    public void testReplaceFragment() {
        final URI uri = UriBuilder.fromUri("http://localhost:8080/a/b/c?a=x&b=y#frag").fragment("ment").build();
        Assert.assertEquals(URI.create("http://localhost:8080/a/b/c?a=x&b=y#ment"), uri);
    }

    @Test
    public void testReplaceUri() {
        final URI u = URI.create("http://bob@localhost:8080/a/b/c?a=x&b=y#frag");

        URI uri = UriBuilder.fromUri(u).uri(URI.create("https://bob@localhost:8080")).build();
        Assert.assertEquals(URI.create("https://bob@localhost:8080/a/b/c?a=x&b=y#frag"), uri);

        uri = UriBuilder.fromUri(u).uri(URI.create("https://sue@localhost:8080")).build();
        Assert.assertEquals(URI.create("https://sue@localhost:8080/a/b/c?a=x&b=y#frag"), uri);

        uri = UriBuilder.fromUri(u).uri(URI.create("https://sue@localhost:9090")).build();
        Assert.assertEquals(URI.create("https://sue@localhost:9090/a/b/c?a=x&b=y#frag"), uri);

        uri = UriBuilder.fromUri(u).uri(URI.create("/x/y/z")).build();
        Assert.assertEquals(URI.create("http://bob@localhost:8080/x/y/z?a=x&b=y#frag"), uri);

        uri = UriBuilder.fromUri(u).uri(URI.create("?x=a&b=y")).build();
        Assert.assertEquals(URI.create("http://bob@localhost:8080/a/b/c?x=a&b=y#frag"), uri);

        uri = UriBuilder.fromUri(u).uri(URI.create("#ment")).build();
        Assert.assertEquals(URI.create("http://bob@localhost:8080/a/b/c?a=x&b=y#ment"), uri);
    }

    @Test
    public void testSchemeSpecificPart() {
        final URI u = URI.create("http://bob@localhost:8080/a/b/c?a=x&b=y#frag");

        final URI uri = UriBuilder.fromUri(u).schemeSpecificPart("//sue@remotehost:9090/x/y/z?x=a&y=b").build();
        Assert.assertEquals(URI.create("http://sue@remotehost:9090/x/y/z?x=a&y=b#frag"), uri);
    }

    @Test
    public void testAppendPath() {
        URI uri = UriBuilder.fromUri("http://localhost:8080").path("a/b/c").build();
        Assert.assertEquals(URI.create("http://localhost:8080/a/b/c"), uri);

        uri = UriBuilder.fromUri("http://localhost:8080/").path("a/b/c").build();
        Assert.assertEquals(URI.create("http://localhost:8080/a/b/c"), uri);

        uri = UriBuilder.fromUri("http://localhost:8080").path("/a/b/c").build();
        Assert.assertEquals(URI.create("http://localhost:8080/a/b/c"), uri);

        uri = UriBuilder.fromUri("http://localhost:8080/a/b/c/").path("/").build();
        Assert.assertEquals(URI.create("http://localhost:8080/a/b/c/"), uri);

        uri = UriBuilder.fromUri("http://localhost:8080/a/b/c/").path("/x/y/z").build();
        Assert.assertEquals(URI.create("http://localhost:8080/a/b/c/x/y/z"), uri);

        uri = UriBuilder.fromUri("http://localhost:8080/a/b/c").path("/x/y/z").build();
        Assert.assertEquals(URI.create("http://localhost:8080/a/b/c/x/y/z"), uri);

        uri = UriBuilder.fromUri("http://localhost:8080/a/b/c").path("x/y/z").build();
        Assert.assertEquals(URI.create("http://localhost:8080/a/b/c/x/y/z"), uri);

        uri = UriBuilder.fromUri("http://localhost:8080/a/b/c").path("/").build();
        Assert.assertEquals(URI.create("http://localhost:8080/a/b/c/"), uri);

        uri = UriBuilder.fromUri("http://localhost:8080/a/b/c").path("").build();
        Assert.assertEquals(URI.create("http://localhost:8080/a/b/c"), uri);

        uri = UriBuilder.fromUri("http://localhost:8080/a%20/b%20/c%20").path("/x /y /z ").build();
        Assert.assertEquals(URI.create("http://localhost:8080/a%20/b%20/c%20/x%20/y%20/z%20"), uri);
    }

    @Test
    public void testAppendSegment() {
        final URI uri = UriBuilder.fromUri("http://localhost:8080").segment("a/b/c;x").build();
        Assert.assertEquals(URI.create("http://localhost:8080/a%2Fb%2Fc%3Bx"), uri);
    }

    @Test
    public void testWhitespacesInPathParams() {
        final URI uri = UriBuilder.fromUri("http://localhost:80/aaa/{  par1}/").path("bbb/{  par2   }/ccc")
                .build("1param", "2param");
        assertEquals(URI.create("http://localhost:80/aaa/1param/bbb/2param/ccc"), uri);
    }

    @Test
    public void testWhitespacesInPathParamsByResolve() {
        final URI uri = UriBuilder.fromUri("http://localhost:80/aaa/{  par1}/").path("bbb/{  par2   }/ccc")
                .build("1param", "2param");
        assertEquals(URI.create("http://localhost:80/aaa/1param/bbb/2param/ccc"), uri);
    }

    @Test
    public void testWhitespacesInPathParams2() {
        final URI uri = UriBuilder.fromUri("http://localhost:80/aaa/{  par1}").path("bbb/{  par2 : \\d*  }/ccc")
                .build("1param", "2");
        assertEquals(URI.create("http://localhost:80/aaa/1param/bbb/2/ccc"), uri);
    }

    @Test
    public void testWhitespacesInPathParams2ByResolve() {
        final URI uri = UriBuilder.fromUri("http://localhost:80/aaa/{  par1}").path("bbb/{  par2 : \\d*  }/ccc")
                .resolveTemplate("par1", "1param").resolveTemplate("par2", "2").build();
        assertEquals(URI.create("http://localhost:80/aaa/1param/bbb/2/ccc"), uri);
    }

    @Test
    public void testWhitespacesInQueryParams() {
        final URI uri = UriBuilder.fromUri("http://localhost:80/aaa?a={      param   : \\.d*  }").build("5");
        assertEquals(URI.create("http://localhost:80/aaa?a=5"), uri);
    }

    @Test
    public void testWhitespacesInQueryParamsByResolve() {
        final URI uri = UriBuilder.fromUri("http://localhost:80/aaa?a={      param   : \\.d*  }").resolveTemplate("param", "5")
                .build();
        assertEquals(URI.create("http://localhost:80/aaa?a=5"), uri);
    }

    @Test
    public void testRelativeFromUri() {
        URI uri = UriBuilder.fromUri("a/b/c").build();
        Assert.assertEquals(URI.create("a/b/c"), uri);

        uri = UriBuilder.fromUri("a/b/c").path("d").build();
        Assert.assertEquals(URI.create("a/b/c/d"), uri);

        uri = UriBuilder.fromUri("a/b/c/").path("d").build();
        Assert.assertEquals(URI.create("a/b/c/d"), uri);

        uri = UriBuilder.fromUri("a/b/c").path("/d").build();
        Assert.assertEquals(URI.create("a/b/c/d"), uri);

        uri = UriBuilder.fromUri("a/b/c/").path("/d").build();
        Assert.assertEquals(URI.create("a/b/c/d"), uri);

        uri = UriBuilder.fromUri("").queryParam("x", "y").build();
        Assert.assertEquals(URI.create("?x=y"), uri);

    }

    @Test
    public void testRelativefromPath() {
        URI uri = UriBuilder.fromPath("a/b/c").build();
        Assert.assertEquals(URI.create("a/b/c"), uri);

        uri = UriBuilder.fromPath("a/b/c").path("d").build();
        Assert.assertEquals(URI.create("a/b/c/d"), uri);

        uri = UriBuilder.fromPath("a/b/c/").path("d").build();
        Assert.assertEquals(URI.create("a/b/c/d"), uri);

        uri = UriBuilder.fromPath("a/b/c").path("/d").build();
        Assert.assertEquals(URI.create("a/b/c/d"), uri);

        uri = UriBuilder.fromPath("a/b/c/").path("/d").build();
        Assert.assertEquals(URI.create("a/b/c/d"), uri);

        uri = UriBuilder.fromPath("").queryParam("x", "y").build();
        Assert.assertEquals(URI.create("?x=y"), uri);
    }

    @Test
    public void testAppendQueryParams() throws URISyntaxException {
        URI uri = UriBuilder.fromUri("http://localhost:8080/a/b/c?a=x&b=y").queryParam("c", "z").build();
        Assert.assertEquals(URI.create("http://localhost:8080/a/b/c?a=x&b=y&c=z"), uri);

        uri = UriBuilder.fromUri("http://localhost:8080/a/b/c?a=x&b=y").queryParam("c= ", "z= ").build();
        Assert.assertEquals(URI.create("http://localhost:8080/a/b/c?a=x&b=y&c%3D+=z%3D+"), uri);

        uri = UriBuilder.fromUri(new URI("http://localhost:8080/")).queryParam("c", "z").build();
        Assert.assertEquals(URI.create("http://localhost:8080/?c=z"), uri);

        uri = UriBuilder.fromUri(new URI("http://localhost:8080")).queryParam("c", "z").build();
        Assert.assertEquals(URI.create("http://localhost:8080/?c=z"), uri);

        uri = UriBuilder.fromUri("http://localhost:8080/").queryParam("c", "z").build();
        Assert.assertEquals(URI.create("http://localhost:8080/?c=z"), uri);

        uri = UriBuilder.fromUri("http://localhost:8080").queryParam("c", "z").build();
        Assert.assertEquals(URI.create("http://localhost:8080/?c=z"), uri);

        try {
            UriBuilder.fromPath("http://localhost:8080").queryParam("name", "x", null).build();
            fail("IllegalArgumentException expected.");
        } catch (final IllegalArgumentException e) {
            // exception expected, move on...
        }
    }

    @Test
    public void testAppendMatrixParams() {
        URI uri = UriBuilder.fromUri("http://localhost:8080/a/b/c;a=x;b=y").matrixParam("c", "z").build();
        Assert.assertEquals(URI.create("http://localhost:8080/a/b/c;a=x;b=y;c=z"), uri);

        uri = UriBuilder.fromUri("http://localhost:8080/a/b/c;a=x;b=y").matrixParam("c=/ ;", "z=/ ;").build();
        Assert.assertEquals(URI.create("http://localhost:8080/a/b/c;a=x;b=y;c%3D%2F%20%3B=z%3D%2F%20%3B"), uri);
    }

    @Test
    public void testAppendPathAndMatrixParams() {
        final URI uri = UriBuilder.fromUri("http://localhost:8080/").path(
                "a").matrixParam("x", "foo").matrixParam("y", "bar").path("b").matrixParam("x", "foo").matrixParam("y", "bar")
                .build();
        Assert.assertEquals(URI.create("http://localhost:8080/a;x=foo;y=bar/b;x=foo;y=bar"), uri);
    }

    @Path("resource")
    class Resource {

        @GET
        @Path("method")
        public String get() {
            return "";
        }

        @Path("locator")
        public Object locator() {
            return null;
        }
    }

    @Test
    public void testResourceAppendPath() throws NoSuchMethodException {
        URI ub = UriBuilder.fromUri("http://localhost:8080/base").path(Resource.class).build();
        Assert.assertEquals(URI.create("http://localhost:8080/base/resource"), ub);

        ub = UriBuilder.fromUri("http://localhost:8080/base").path(Resource.class, "get").build();
        Assert.assertEquals(URI.create("http://localhost:8080/base/method"), ub);

        final Method get = Resource.class.getMethod("get");
        final Method locator = Resource.class.getMethod("locator");
        ub = UriBuilder.fromUri("http://localhost:8080/base").path(get).path(locator).build();
        Assert.assertEquals(URI.create("http://localhost:8080/base/method/locator"), ub);
    }

    @Path("resource/{id}")
    class ResourceWithTemplate {

        @GET
        @Path("method/{id1}")
        public String get() {
            return "";
        }

        @Path("locator/{id2}")
        public Object locator() {
            return null;
        }
    }

    @Test
    public void testResourceWithTemplateAppendPath() throws NoSuchMethodException {
        URI ub = UriBuilder.fromUri("http://localhost:8080/base").path(ResourceWithTemplate.class).build("foo");
        Assert.assertEquals(URI.create("http://localhost:8080/base/resource/foo"), ub);

        ub = UriBuilder.fromUri("http://localhost:8080/base").path(ResourceWithTemplate.class, "get").build("foo");
        Assert.assertEquals(URI.create("http://localhost:8080/base/method/foo"), ub);

        final Method get = ResourceWithTemplate.class.getMethod("get");
        final Method locator = ResourceWithTemplate.class.getMethod("locator");
        ub = UriBuilder.fromUri("http://localhost:8080/base").path(get).path(locator).build("foo", "bar");
        Assert.assertEquals(URI.create("http://localhost:8080/base/method/foo/locator/bar"), ub);
    }

    @Path("resource/{id: .+}")
    class ResourceWithTemplateRegex {

        @GET
        @Path("method/{id1: .+}")
        public String get() {
            return "";
        }

        @Path("locator/{id2: .+}")
        public Object locator() {
            return null;
        }
    }

    @Test
    public void testResourceWithTemplateRegexAppendPath() throws NoSuchMethodException {
        URI ub = UriBuilder.fromUri("http://localhost:8080/base").path(ResourceWithTemplateRegex.class).build("foo");
        Assert.assertEquals(URI.create("http://localhost:8080/base/resource/foo"), ub);

        ub = UriBuilder.fromUri("http://localhost:8080/base").path(ResourceWithTemplateRegex.class, "get").build("foo");
        Assert.assertEquals(URI.create("http://localhost:8080/base/method/foo"), ub);

        final Method get = ResourceWithTemplateRegex.class.getMethod("get");
        final Method locator = ResourceWithTemplateRegex.class.getMethod("locator");
        ub = UriBuilder.fromUri("http://localhost:8080/base").path(get).path(locator).build("foo", "bar");
        Assert.assertEquals(URI.create("http://localhost:8080/base/method/foo/locator/bar"), ub);
    }

    interface GenericInterface<T, U> {

        T find(U u);
    }

    @Path("resource/")
    class ResourceWithGenericInterface implements GenericInterface<Object, String> {

        @GET
        @Path("{id}")
        @Override
        public Object find(@PathParam("id") final String s) {
            return null;
        }
    }

    @Test
    public void testResourceWithGenericInterfaceAppendPath() {
        final URI ub = UriBuilder.fromUri("http://localhost:8080/base").path(ResourceWithGenericInterface.class, "find")
                .build("foo");
        Assert.assertEquals(URI.create("http://localhost:8080/base/foo"), ub);
    }

    @Test
    public void testBuildTemplates() {
        URI uri = UriBuilder.fromUri("http://localhost:8080/a/b/c").path("/{foo}/{bar}/{baz}/{foo}").build("x", "y", "z");
        Assert.assertEquals(URI.create("http://localhost:8080/a/b/c/x/y/z/x"), uri);

        final Map<String, Object> m = new HashMap<String, Object>();
        m.put("foo", "x");
        m.put("bar", "y");
        m.put("baz", "z");
        uri = UriBuilder.fromUri("http://localhost:8080/a/b/c").path("/{foo}/{bar}/{baz}/{foo}").buildFromMap(m);
        Assert.assertEquals(URI.create("http://localhost:8080/a/b/c/x/y/z/x"), uri);
    }

    @Test
    public void testBuildTemplatesByResolve() {
        final Map<String, Object> m = new HashMap<String, Object>();
        m.put("foo", "x");
        m.put("bar", "y");
        m.put("baz", "z");

        final URI uri = UriBuilder.fromUri("http://localhost:8080/a/b/c").path("/{foo}/{bar}/{baz}/{foo}").resolveTemplates(m)
                .build();

        Assert.assertEquals(URI.create("http://localhost:8080/a/b/c/x/y/z/x"), uri);
    }

    @Test
    public void testBuildTemplatesWithNameAuthority() {
        URI uri = UriBuilder.fromUri("http://x_y.com:8080/a/b/c").path("/{foo}/{bar}/{baz}/{foo}").build("x", "y", "z");
        Assert.assertEquals(URI.create("http://x_y.com:8080/a/b/c/x/y/z/x"), uri);

        final Map<String, Object> m = new HashMap<String, Object>();
        m.put("foo", "x");
        m.put("bar", "y");
        m.put("baz", "z");
        uri = UriBuilder.fromUri("http://x_y.com:8080/a/b/c").path("/{foo}/{bar}/{baz}/{foo}").buildFromMap(m);
        Assert.assertEquals(URI.create("http://x_y.com:8080/a/b/c/x/y/z/x"), uri);
    }

    @Test
    public void testBuildTemplatesWithNameAuthorityByResolve() {
        final Map<String, Object> m = new HashMap<String, Object>();
        m.put("foo", "x");
        m.put("bar", "y");
        m.put("baz", "z");
        final URI uri = UriBuilder.fromUri("http://x_y.com:8080/a/b/c")
                .path("/{foo}/{bar}/{baz}/{foo}").buildFromMap(m);
        Assert.assertEquals(URI.create("http://x_y.com:8080/a/b/c/x/y/z/x"), uri);
    }

    @Test
    public void testBuildFromMap() {
        final Map<String, Object> maps = new HashMap<String, Object>();
        maps.put("x", null);
        maps.put("y", "/path-absolute/test1");
        maps.put("z", "fred@example.com");
        maps.put("w", "path-rootless/test2");
        maps.put("u", "extra");

        boolean caught = false;

        try {
            System.out.println(UriBuilder.fromPath("").path("{w}/{x}/{y}/{z}/{x}").buildFromEncodedMap(maps));

        } catch (final IllegalArgumentException ex) {
            caught = true;
        }

        Assert.assertTrue(caught);
    }

    @Test
    public void testBuildFromMapByResolve() {
        final Map<String, Object> maps = new HashMap<String, Object>();
        maps.put("x", null);
        maps.put("y", "/path-absolute/test1");
        maps.put("z", "fred@example.com");
        maps.put("w", "path-rootless/test2");
        maps.put("u", "extra");

        boolean caught = false;

        try {
            System.out.println(UriBuilder.fromPath("").path("{w}/{x}/{y}/{z}/{x}").resolveTemplates(maps).build());

        } catch (final IllegalArgumentException ex) {
            caught = true;
        }

        Assert.assertTrue(caught);
    }

    @Test
    public void testBuildQueryTemplates() {
        URI uri = UriBuilder.fromUri("http://localhost:8080/a/b/c").queryParam("a", "{b}").build("=+&%xx%20");
        Assert.assertEquals(URI.create("http://localhost:8080/a/b/c?a=%3D%2B%26%25xx%2520"), uri);

        final Map<String, Object> m = new HashMap<String, Object>();
        m.put("b", "=+&%xx%20");
        uri = UriBuilder.fromUri("http://localhost:8080/a/b/c").queryParam("a", "{b}").buildFromMap(m);
        Assert.assertEquals(URI.create("http://localhost:8080/a/b/c?a=%3D%2B%26%25xx%2520"), uri);
    }

    @Test
    public void testBuildFromEncodedQueryTemplates() {
        URI uri = UriBuilder.fromUri("http://localhost:8080/a/b/c").queryParam("a", "{b}").buildFromEncoded("=+&%xx%20");
        Assert.assertEquals(URI.create("http://localhost:8080/a/b/c?a=%3D%2B%26%25xx%20"), uri);

        final Map<String, Object> m = new HashMap<String, Object>();
        m.put("b", "=+&%xx%20");
        uri = UriBuilder.fromUri("http://localhost:8080/a/b/c").queryParam("a", "{b}").buildFromEncodedMap(m);
        Assert.assertEquals(URI.create("http://localhost:8080/a/b/c?a=%3D%2B%26%25xx%20"), uri);
    }

    @Test
    public void testBuildFromEncodedSlashInParamValue() {
        assertEquals("/A/B", UriBuilder.fromUri("/{param}").buildFromEncoded("A/B").toString());
    }

    @Test
    public void testResolveTemplateFromEncodedQueryTemplates() {
        URI uri = UriBuilder.fromUri("http://localhost:8080/a/b/c").queryParam("a", "{b}")
                .resolveTemplateFromEncoded("b", "=+&%xx%20").build();
        Assert.assertEquals(URI.create("http://localhost:8080/a/b/c?a=%3D%2B%26%25xx%20"), uri);

        final Map<String, Object> m = new HashMap<String, Object>();
        m.put("b", "=+&%xx%20");
        uri = UriBuilder.fromUri("http://localhost:8080/a/b/c").queryParam("a", "{b}").resolveTemplatesFromEncoded(m).build();
        Assert.assertEquals(URI.create("http://localhost:8080/a/b/c?a=%3D%2B%26%25xx%20"), uri);
    }

    @Test
    public void testBuildFragmentTemplates() {
        URI uri = UriBuilder.fromUri("http://localhost:8080/a/b/c").path("/{foo}/{bar}/{baz}/{foo}").fragment("{foo}")
                .build("x", "y", "z");
        Assert.assertEquals(URI.create("http://localhost:8080/a/b/c/x/y/z/x#x"), uri);

        final Map<String, Object> m = new HashMap<String, Object>();
        m.put("foo", "x");
        m.put("bar", "y");
        m.put("baz", "z");
        uri = UriBuilder.fromUri("http://localhost:8080/a/b/c").path("/{foo}/{bar}/{baz}/{foo}").fragment("{foo}")
                .buildFromMap(m);
        Assert.assertEquals(URI.create("http://localhost:8080/a/b/c/x/y/z/x#x"), uri);
    }

    @Test
    public void testResolveTemplateFromFragmentTemplates() {
        URI uri = UriBuilder.fromUri("http://localhost:8080/a/b/c").path("/{foo}/{bar}/{baz}/{foo}").fragment("{foo}")
                .resolveTemplate("foo", "x").resolveTemplate("bar", "y")
                .resolveTemplate("baz", "z").build();

        Assert.assertEquals(URI.create("http://localhost:8080/a/b/c/x/y/z/x#x"), uri);

        final Map<String, Object> m = new HashMap<String, Object>();
        m.put("foo", "x");
        m.put("bar", "y");
        m.put("baz", "z");
        uri = UriBuilder.fromUri("http://localhost:8080/a/b/c").path("/{foo}/{bar}/{baz}/{foo}").fragment("{foo}")
                .resolveTemplates(m).build();
        Assert.assertEquals(URI.create("http://localhost:8080/a/b/c/x/y/z/x#x"), uri);
    }

    @Test
    public void testTemplatesDefaultPort() {
        URI uri = UriBuilder.fromUri("http://localhost/a/b/c").path("/{foo}/{bar}/{baz}/{foo}").build("x", "y", "z");
        Assert.assertEquals(URI.create("http://localhost/a/b/c/x/y/z/x"), uri);

        final Map<String, Object> m = new HashMap<String, Object>();
        m.put("foo", "x");
        m.put("bar", "y");
        m.put("baz", "z");
        uri = UriBuilder.fromUri("http://localhost/a/b/c").path("/{foo}/{bar}/{baz}/{foo}").buildFromMap(m);
        Assert.assertEquals(URI.create("http://localhost/a/b/c/x/y/z/x"), uri);
    }

    @Test
    public void testResolveTemplatesDefaultPort() {
        URI uri = UriBuilder.fromUri("http://localhost/a/b/c").path("/{foo}/{bar}/{baz}/{foo}").resolveTemplate("foo", "x")
                .resolveTemplate("bar", "y").resolveTemplate("baz" + "", "z").build();
        Assert.assertEquals(URI.create("http://localhost/a/b/c/x/y/z/x"), uri);

        final Map<String, Object> m = new HashMap<String, Object>();
        m.put("foo", "x");
        m.put("bar", "y");
        m.put("baz", "z");
        uri = UriBuilder.fromUri("http://localhost/a/b/c").path("/{foo}/{bar}/{baz}/{foo}").resolveTemplates(m).build();
        Assert.assertEquals(URI.create("http://localhost/a/b/c/x/y/z/x"), uri);
    }

    @Test
    public void testClone() {
        final UriBuilder ub = UriBuilder.fromUri("http://user@localhost:8080/?query#fragment").path("a");
        final URI full = ub.clone().path("b").build();
        final URI base = ub.build();

        Assert.assertEquals(URI.create("http://user@localhost:8080/a?query#fragment"), base);
        Assert.assertEquals(URI.create("http://user@localhost:8080/a/b?query#fragment"), full);
    }

    @Test
    public void testIllegalArgumentException() {
        boolean caught = false;
        try {
            UriBuilder.fromPath(null);
        } catch (final IllegalArgumentException e) {
            caught = true;
        }
        Assert.assertTrue(caught);

        caught = false;
        try {
            UriBuilder.fromUri((URI) null);
        } catch (final IllegalArgumentException e) {
            caught = true;
        }
        Assert.assertTrue(caught);

        caught = false;
        try {
            UriBuilder.fromUri((String) null);
        } catch (final IllegalArgumentException e) {
            caught = true;
        }
        Assert.assertTrue(caught);
    }

    @Test
    public void testUriEncoding() {
        final URI expected = URI.create("http://localhost:8080/%5E");
        assertEquals(expected, new JerseyUriBuilder().uri("http://localhost:8080/^").build());
        assertEquals(expected, UriBuilder.fromUri("http://localhost:8080/^").build());
    }

    // Regression test for JERSEY-1324 fix.
    @Test
    public void testInvalidUriTemplateEncodedAsPath() {
        assertEquals(URI.create("http%20ftp%20xml//:888:888/1:8080:80"),
                new JerseyUriBuilder().uri("http ftp xml//:888:888/1:8080:80").build());
    }

    @Test
    public void testVariableWithoutValue() {
        boolean caught = false;
        try {
            UriBuilder.fromPath("http://localhost:8080").path("/{a}/{b}").buildFromEncoded("aVal");

        } catch (final IllegalArgumentException e) {
            caught = true;
        }
        Assert.assertTrue(caught);
    }

    @Test
    public void testPortValue() {
        boolean caught = false;
        try {
            UriBuilder.fromPath("http://localhost").port(-2);
        } catch (final IllegalArgumentException e) {
            caught = true;
        }
        Assert.assertTrue(caught);
    }

    @Test
    public void testPortSetting() throws URISyntaxException {
        URI uri;

        uri = new JerseyUriBuilder().uri("http://localhost").port(8080).build();
        Assert.assertEquals(URI.create("http://localhost:8080"), uri);

        uri = new JerseyUriBuilder().uri(new URI("http://localhost")).port(8080).build();
        Assert.assertEquals(URI.create("http://localhost:8080"), uri);

        uri = new JerseyUriBuilder().uri("http://localhost/").port(8080).build();
        Assert.assertEquals(URI.create("http://localhost:8080/"), uri);

        uri = new JerseyUriBuilder().uri(new URI("http://localhost/")).port(8080).build();
        Assert.assertEquals(URI.create("http://localhost:8080/"), uri);
    }

    @Test
    public void testHostValue() {
        boolean caught = false;
        try {
            UriBuilder.fromPath("http://localhost").host("");
        } catch (final IllegalArgumentException e) {
            caught = true;
        }
        Assert.assertTrue(caught);

        URI uri = UriBuilder.fromPath("").host("abc").build();
        Assert.assertEquals(URI.create("//abc"), uri);

        uri = UriBuilder.fromPath("").host("abc").host(null).build();
        Assert.assertEquals(URI.create(""), uri);
    }

    /**
     * This test has been rewritten as part of fix for JERSEY-2378.
     * The new purpose of this test is to demonstrate how old behavior of UriBuilder.build() method
     * wrt. unresolved templates can be achieved via {@link org.glassfish.jersey.uri.UriComponent#encodeTemplateNames(String)}
     * method.
     */
    @Test
    public void testEncodeTemplateNames() {
        final URI uri = URI.create(UriComponent.encodeTemplateNames(UriBuilder.fromPath("http://localhost:8080").path(
                "/{a}/{b}").replaceQuery("q={c}").toTemplate()));
        Assert.assertEquals(URI.create("http://localhost:8080/%7Ba%7D/%7Bb%7D?q=%7Bc%7D"), uri);
    }

    @Test
    public void resolveTemplateTest() {
        final UriBuilder uriBuilder = UriBuilder.fromPath("http://localhost:8080").path("{a}").path("{b}")
                .queryParam("query", "{q}");
        uriBuilder.resolveTemplate("a", "param-a");
        uriBuilder.resolveTemplate("q", "param-q");
        final Map<String, Object> m = new HashMap<String, Object>();
        m.put("a", "ignored-a");
        m.put("b", "param-b");
        m.put("q", "ignored-q");
        Assert.assertEquals(URI.create("http://localhost:8080/param-a/param-b?query=param-q"), uriBuilder.buildFromMap(m));
    }

    @Test
    public void resolveTemplateFromEncodedTest() {
        final UriBuilder uriBuilder = UriBuilder.fromPath("http://localhost:8080").path("{a}").path("{b}").path("{c}")
                .queryParam("query", "{q}");
        uriBuilder.resolveTemplateFromEncoded("a", "x/y/z%3F%20");
        uriBuilder.resolveTemplateFromEncoded("q", "q?%20%26");
        uriBuilder.resolveTemplate("c", "paramc1/paramc2");
        final Map<String, Object> m = new HashMap<String, Object>();
        m.put("a", "ignored-a");
        m.put("b", "param-b/aaa");
        m.put("q", "ignored-q");
        Assert.assertEquals("http://localhost:8080/x/y/z%3F%20/param-b/aaa/paramc1%2Fparamc2?query=q%3F%20%26",
                uriBuilder.buildFromEncodedMap(m).toString());
    }

    @Test
    public void resolveTemplateWithoutEncodedTest() {
        final UriBuilder uriBuilder = UriBuilder.fromPath("http://localhost:8080").path("{a}").path("{b}").path("{c}")
                .queryParam("query", "{q}");
        uriBuilder.resolveTemplate("a", "x/y/z%3F%20");
        uriBuilder.resolveTemplate("q", "q?%20%26");
        uriBuilder.resolveTemplate("c", "paramc1/paramc2");
        final Map<String, Object> m = new HashMap<String, Object>();
        m.put("a", "ignored-a");
        m.put("b", "param-b/aaa");
        m.put("q", "ignored-q");
        Assert.assertEquals("http://localhost:8080/x%2Fy%2Fz%253F%2520/param-b%2Faaa/paramc1%2Fparamc2?query=q%3F%2520%2526",
                uriBuilder.buildFromMap(m).toString());
    }

    @Test
    public void resolveTemplateWithEncodedSlashTest() {
        final UriBuilder uriBuilder = UriBuilder.fromPath("http://localhost:8080").path("{a}").path("{b}")
                .queryParam("query", "{q}");
        uriBuilder.resolveTemplate("a", "param-a/withSlash", false);
        uriBuilder.resolveTemplate("b", "param-b/withEncodedSlash", true);
        uriBuilder.resolveTemplate("q", "param-q", true);
        Assert.assertEquals(URI.create("http://localhost:8080/param-a/withSlash/param-b%2FwithEncodedSlash?query=param-q"),
                uriBuilder.build());
        uriBuilder.build();
    }

    @Test
    public void resolveTemplatesTest() {
        final UriBuilder uriBuilder = UriBuilder.fromPath("http://localhost:8080").path("{a}").path("{b}")
                .queryParam("query", "{q}");

        uriBuilder.resolveTemplate("a", "param-a");
        uriBuilder.resolveTemplate("q", "param-q");
        final Map<String, Object> buildMap = new HashMap<String, Object>();
        buildMap.put("a", "ignored-a");
        buildMap.put("b", "param-b");
        buildMap.put("q", "ignored-q");
        Assert.assertEquals(URI.create("http://localhost:8080/param-a/param-b?query=param-q"), uriBuilder.buildFromMap(buildMap));
    }

    @Test
    public void resolveTemplatesFromEncodedTest() {
        final UriBuilder uriBuilder = UriBuilder.fromPath("http://localhost:8080").path("{a}").path("{b}").path("{c}")
                .queryParam("query", "{q}");

        final Map<String, Object> resolveMap = new HashMap<String, Object>();
        resolveMap.put("a", "x/y/z%3F%20");
        resolveMap.put("q", "q?%20%26");
        resolveMap.put("c", "paramc1/paramc2");
        uriBuilder.resolveTemplatesFromEncoded(resolveMap);
        final Map<String, Object> buildMap = new HashMap<String, Object>();
        buildMap.put("b", "param-b/aaa");
        Assert.assertEquals("http://localhost:8080/x/y/z%3F%20/param-b/aaa/paramc1/paramc2?query=q%3F%20%26",
                uriBuilder.buildFromEncodedMap(buildMap).toString());
    }

    @Test
    public void resolveTemplatesFromNotEncodedTest() {
        final UriBuilder uriBuilder = UriBuilder.fromPath("http://localhost:8080").path("{a}").path("{b}").path("{c}")
                .queryParam("query", "{q}");

        final Map<String, Object> resolveMap = new HashMap<String, Object>();
        resolveMap.put("a", "x/y/z%3F%20");
        resolveMap.put("q", "q?%20%26");
        resolveMap.put("c", "paramc1/paramc2");
        uriBuilder.resolveTemplates(resolveMap);
        final Map<String, Object> buildMap = new HashMap<String, Object>();
        buildMap.put("b", "param-b/aaa");
        Assert.assertEquals("http://localhost:8080/x%2Fy%2Fz%253F%2520/param-b%2Faaa/paramc1%2Fparamc2?query=q%3F%2520%2526",
                uriBuilder.buildFromMap(buildMap).toString());
    }

    @Test
    public void resolveTemplatesEncodeSlash() {
        final UriBuilder uriBuilder = UriBuilder.fromPath("http://localhost:8080").path("{a}").path("{b}").path("{c}")
                .queryParam("query", "{q}");

        final Map<String, Object> resolveMap = new HashMap<String, Object>();
        resolveMap.put("a", "x/y/z%3F%20");
        resolveMap.put("q", "q?%20%26");
        resolveMap.put("c", "paramc1/paramc2");
        uriBuilder.resolveTemplates(resolveMap, false);
        final Map<String, Object> buildMap = new HashMap<String, Object>();
        buildMap.put("b", "param-b/aaa");
        Assert.assertEquals("http://localhost:8080/x/y/z%253F%2520/param-b/aaa/paramc1/paramc2?query=q%3F%2520%2526",
                uriBuilder.buildFromMap(buildMap, false).toString());
    }

    @Test
    public void resolveTemplatesWithEncodedSlashTest() {
        final UriBuilder uriBuilder = UriBuilder.fromPath("http://localhost:8080").path("{a}").path("{b}")
                .queryParam("query", "{q}");
        final Map<String, Object> resolveMap = new HashMap<String, Object>();
        resolveMap.put("a", "param-a/withSlash");
        resolveMap.put("q", "param-q");
        uriBuilder.resolveTemplates(resolveMap, false);
        uriBuilder.resolveTemplate("b", "param-b/withEncodedSlash", true);
        Assert.assertEquals(URI.create("http://localhost:8080/param-a/withSlash/param-b%2FwithEncodedSlash?query=param-q"),
                uriBuilder.build());
        uriBuilder.build();
    }

    @Test
    public void resolveTemplateMultipleCall() {
        final UriBuilder uriBuilder = UriBuilder.fromPath("http://localhost:8080").path("{start}").path("{a}")
                .resolveTemplate("a", "first-a").path("{a}").resolveTemplate("a", "second-a")
                .path("{a}/{a}").resolveTemplate("a", "twice-a");

        Assert.assertEquals(URI.create("http://localhost:8080/start-path/first-a/second-a/twice-a/twice-a"),
                uriBuilder.build("start-path"));
    }

    @Test
    public void replaceWithEmtpySchemeFromUriTest() throws URISyntaxException {
        final String uriOrig = "ftp://ftp.is.co.za/rfc/rfc1808.txt";
        final URI uriReplace = new URI(null, "ftp.is.co.za", "/test/rfc1808.txt", null, null);
        final URI uri = UriBuilder.fromUri(new URI(uriOrig)).uri(uriReplace).build();
        Assert.assertEquals("ftp://ftp.is.co.za/test/rfc1808.txt", uri.toString());
    }

    @Test
    public void replaceWithEmptySchemeFromStringTest() throws URISyntaxException {
        final String uriOrig = "ftp://ftp.is.co.za/rfc/rfc1808.txt";
        final URI uriReplace = new URI(null, "ftp.is.co.za", "/test/rfc1808.txt", null, null);

        final URI uri = UriBuilder.fromUri(new URI(uriOrig)).uri(uriReplace.toASCIIString()).build();
        Assert.assertEquals("ftp://ftp.is.co.za/test/rfc1808.txt", uri.toString());
    }

    @Test
    public void replaceWithEmptyQueryFromStringTest() throws URISyntaxException {
        final String uriOrig = "ftp://ftp.is.co.za/rfc/rfc1808.txt?a=1";
        final URI uriReplace = new URI(null, "ftp.is.co.za", "/test/rfc1808.txt", null, null);

        final URI uri = UriBuilder.fromUri(new URI(uriOrig)).uri(uriReplace.toASCIIString()).build();
        Assert.assertEquals("ftp://ftp.is.co.za/test/rfc1808.txt?a=1", uri.toString());
    }

    @Test
    public void replaceWithEmptyFragmentFromStringTest() throws URISyntaxException {
        final String uriOrig = "ftp://ftp.is.co.za/rfc/rfc1808.txt#myFragment";
        final URI uriReplace = new URI(null, "ftp.is.co.za", "/test/rfc1808.txt", null, null);

        final URI uri = UriBuilder.fromUri(new URI(uriOrig)).uri(uriReplace.toASCIIString()).build();
        Assert.assertEquals("ftp://ftp.is.co.za/test/rfc1808.txt#myFragment", uri.toString());
    }

    @Test
    public void replaceOpaqueUriWithNonOpaqueFromStringTest() throws URISyntaxException {
        final String first = "news:comp.lang.java";
        final String second = "http://comp.lang.java";
        UriBuilder.fromUri(new URI(first)).uri(second);
    }

    @Test
    public void replaceOpaqueUriWithNonOpaqueFromStringTest2() throws URISyntaxException {
        final String first = "news:comp.lang.java";
        final String second = "http://comp.lang.java";
        UriBuilder.fromUri(new URI(first)).scheme("http").uri(second);
    }

    @Test
    public void replaceOpaqueUriWithNonOpaqueFromUriTest() throws URISyntaxException {
        final String first = "news:comp.lang.java";
        final String second = "http://comp.lang.java";
        UriBuilder.fromUri(new URI(first)).uri(new URI(second));
    }

    @Test
    public void testQueryParamEncoded() {
        final UriBuilder uriBuilder = UriBuilder.fromUri("http://localhost:8080/path");
        uriBuilder.queryParam("query", "%dummy23");
        Assert.assertEquals("http://localhost:8080/path?query=%25dummy23", uriBuilder.build().toString());
    }

    @Test
    public void testQueryParamEncoded2() {
        final UriBuilder uriBuilder = UriBuilder.fromUri("http://localhost:8080/path");
        uriBuilder.queryParam("query", "{param}");
        Assert.assertEquals("http://localhost:8080/path?query=%25dummy23", uriBuilder.build("%dummy23").toString());
    }

    @Test
    public void testQueryParamEncoded3() {
        final UriBuilder uriBuilder = UriBuilder.fromUri("http://localhost:8080/path");
        uriBuilder.queryParam("query", "{param}");
        Assert.assertEquals("http://localhost:8080/path?query=%2525test", uriBuilder.build("%25test").toString());
    }

    @Test
    public void testQueryParamEncoded4() {
        final UriBuilder uriBuilder = UriBuilder.fromUri("http://localhost:8080/path");
        uriBuilder.queryParam("query", "{param}");
        Assert.assertEquals("http://localhost:8080/path?query=%25test", uriBuilder.buildFromEncoded("%25test").toString());
    }

    @Test
    public void testQueryParamEncoded5() {
        final UriBuilder uriBuilder = UriBuilder.fromUri("http://localhost:8080/path");
        uriBuilder.queryParam("query", "! # $ & ' ( ) * + , / : ; = ? @ [ ]");
        Assert.assertEquals(
                "http://localhost:8080/path?query=%21+%23+%24+%26+%27+%28+%29+%2A+%2B+%2C+%2F+%3A+%3B+%3D+%3F+%40+%5B+%5D",
                uriBuilder.build().toString());
    }

}
