/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.uri;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.MatchResult;

import org.glassfish.jersey.uri.internal.UriTemplateParser;

import org.junit.Test;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Taken from Jersey 1: jersey-tests: com.sun.jersey.impl.uri.UriTemplateTest
 *
 * @author Paul Sandoz
 * @author Gerard Davison (gerard.davison at oracle.com)
 */
public class UriTemplateTest {

    /**
     * Test the URI resolution as defined in RFC 3986,
     * <a href="http://tools.ietf.org/html/rfc3986#section-5.4.1">sect. 5.4.1</a> and
     * and <a href="http://tools.ietf.org/html/rfc3986#section-5.4.2">sect. 5.4.2</a>.
     */
    @Test
    public void testResolveUri() {
        final URI baseUri = URI.create("http://a/b/c/d;p?q");

        // Normal examples (RFC 3986, sect. 5.4.1)
        assertThat(UriTemplate.resolve(baseUri, URI.create("g:h")), equalTo(URI.create("g:h")));
        assertThat(UriTemplate.resolve(baseUri, URI.create("g:h")), equalTo(URI.create("g:h")));
        assertThat(UriTemplate.resolve(baseUri, URI.create("g")), equalTo(URI.create("http://a/b/c/g")));
        assertThat(UriTemplate.resolve(baseUri, URI.create("./g")), equalTo(URI.create("http://a/b/c/g")));
        assertThat(UriTemplate.resolve(baseUri, URI.create("g/")), equalTo(URI.create("http://a/b/c/g/")));
        assertThat(UriTemplate.resolve(baseUri, URI.create("/g")), equalTo(URI.create("http://a/g")));
        assertThat(UriTemplate.resolve(baseUri, URI.create("//g")), equalTo(URI.create("http://g")));
        assertThat(UriTemplate.resolve(baseUri, URI.create("?y")), equalTo(URI.create("http://a/b/c/d;p?y")));
        assertThat(UriTemplate.resolve(baseUri, URI.create("g?y")), equalTo(URI.create("http://a/b/c/g?y")));
        assertThat(UriTemplate.resolve(baseUri, URI.create("#s")), equalTo(URI.create("http://a/b/c/d;p?q#s")));
        assertThat(UriTemplate.resolve(baseUri, URI.create("g#s")), equalTo(URI.create("http://a/b/c/g#s")));
        assertThat(UriTemplate.resolve(baseUri, URI.create("g?y#s")), equalTo(URI.create("http://a/b/c/g?y#s")));
        assertThat(UriTemplate.resolve(baseUri, URI.create(";x")), equalTo(URI.create("http://a/b/c/;x")));
        assertThat(UriTemplate.resolve(baseUri, URI.create("g;x")), equalTo(URI.create("http://a/b/c/g;x")));
        assertThat(UriTemplate.resolve(baseUri, URI.create("g;x?y#s")), equalTo(URI.create("http://a/b/c/g;x?y#s")));
        assertThat(UriTemplate.resolve(baseUri, URI.create("")), equalTo(URI.create("http://a/b/c/d;p?q")));
        assertThat(UriTemplate.resolve(baseUri, URI.create(".")), equalTo(URI.create("http://a/b/c/")));
        assertThat(UriTemplate.resolve(baseUri, URI.create("./")), equalTo(URI.create("http://a/b/c/")));
        assertThat(UriTemplate.resolve(baseUri, URI.create("..")), equalTo(URI.create("http://a/b/")));
        assertThat(UriTemplate.resolve(baseUri, URI.create("../")), equalTo(URI.create("http://a/b/")));
        assertThat(UriTemplate.resolve(baseUri, URI.create("../g")), equalTo(URI.create("http://a/b/g")));
        assertThat(UriTemplate.resolve(baseUri, URI.create("../..")), equalTo(URI.create("http://a/")));
        assertThat(UriTemplate.resolve(baseUri, URI.create("../../")), equalTo(URI.create("http://a/")));
        assertThat(UriTemplate.resolve(baseUri, URI.create("../../g")), equalTo(URI.create("http://a/g")));

        // Abnormal examples (RFC 3986, sect. 5.4.2)
        assertThat(UriTemplate.resolve(baseUri, URI.create("../../../g")), equalTo(URI.create("http://a/g")));
        assertThat(UriTemplate.resolve(baseUri, URI.create("../../../../g")), equalTo(URI.create("http://a/g")));
        assertThat(UriTemplate.resolve(baseUri, URI.create("/./g")), equalTo(URI.create("http://a/g")));
        assertThat(UriTemplate.resolve(baseUri, URI.create("/../g")), equalTo(URI.create("http://a/g")));
        assertThat(UriTemplate.resolve(baseUri, URI.create("g.")), equalTo(URI.create("http://a/b/c/g.")));
        assertThat(UriTemplate.resolve(baseUri, URI.create(".g")), equalTo(URI.create("http://a/b/c/.g")));
        assertThat(UriTemplate.resolve(baseUri, URI.create("g..")), equalTo(URI.create("http://a/b/c/g..")));
        assertThat(UriTemplate.resolve(baseUri, URI.create("..g")), equalTo(URI.create("http://a/b/c/..g")));
        assertThat(UriTemplate.resolve(baseUri, URI.create("./../g")), equalTo(URI.create("http://a/b/g")));
        assertThat(UriTemplate.resolve(baseUri, URI.create("./g/.")), equalTo(URI.create("http://a/b/c/g/")));
        assertThat(UriTemplate.resolve(baseUri, URI.create("g/./h")), equalTo(URI.create("http://a/b/c/g/h")));
        assertThat(UriTemplate.resolve(baseUri, URI.create("g/../h")), equalTo(URI.create("http://a/b/c/h")));
        assertThat(UriTemplate.resolve(baseUri, URI.create("g;x=1/./y")), equalTo(URI.create("http://a/b/c/g;x=1/y")));
        assertThat(UriTemplate.resolve(baseUri, URI.create("g;x=1/../y")), equalTo(URI.create("http://a/b/c/y")));
        assertThat(UriTemplate.resolve(baseUri, URI.create("g?y/./x")), equalTo(URI.create("http://a/b/c/g?y/./x")));
        assertThat(UriTemplate.resolve(baseUri, URI.create("g?y/../x")), equalTo(URI.create("http://a/b/c/g?y/../x")));
        assertThat(UriTemplate.resolve(baseUri, URI.create("g#s/./x")), equalTo(URI.create("http://a/b/c/g#s/./x")));
        assertThat(UriTemplate.resolve(baseUri, URI.create("g#s/../x")), equalTo(URI.create("http://a/b/c/g#s/../x")));
        // Per RFC 3986, test below should resolve to "http:g" for strict parsers and "http://a/b/c/g" for backward compatibility
        assertThat(UriTemplate.resolve(baseUri, URI.create("http:g")), equalTo(URI.create("http:g")));

        // JDK bug http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4708535
        assertThat(UriTemplate.resolve(baseUri, URI.create("")), equalTo(baseUri));
    }

    @Test
    public void testRelativizeUri() {
        URI baseUri;

        baseUri = URI.create("http://a/b/c/d");
        assertThat(UriTemplate.relativize(baseUri, URI.create("http://a/b/c/d/e")), equalTo(URI.create("e")));
        assertThat(UriTemplate.relativize(baseUri, URI.create("http://a/b/c/d/./e")), equalTo(URI.create("e")));
        assertThat(UriTemplate.relativize(baseUri, URI.create("http://a/b/c/d/e/../f")), equalTo(URI.create("f")));
        assertThat(UriTemplate.relativize(baseUri, URI.create("http://a/b/c/d/e/.././f")), equalTo(URI.create("f")));

        baseUri = URI.create("http://a/b/c/d?q=v");
        assertThat(UriTemplate.relativize(baseUri, URI.create("http://a/b/c/d/e")), equalTo(URI.create("e")));
        assertThat(UriTemplate.relativize(baseUri, URI.create("http://a/b/c/d/./e")), equalTo(URI.create("e")));
        assertThat(UriTemplate.relativize(baseUri, URI.create("http://a/b/c/d/e/../f")), equalTo(URI.create("f")));
        assertThat(UriTemplate.relativize(baseUri, URI.create("http://a/b/c/d/e/.././f")), equalTo(URI.create("f")));

        // NOTE: At the moment, in sync with the JDK implementation of relativize() method,
        // we do not support relativization of URIs that do not fully prefix the base URI.
        // Once (if) we decide to improve this support beyond what JDK supports, we may need
        // to update the assertions below.
        baseUri = URI.create("http://a/b/c/d");
        assertThat(UriTemplate.relativize(baseUri, URI.create("http://a/b/c/e")), equalTo(URI.create("http://a/b/c/e")));
        assertThat(UriTemplate.relativize(baseUri, URI.create("http://a/b/c/./e")), equalTo(URI.create("http://a/b/c/e")));
        assertThat(UriTemplate.relativize(baseUri, URI.create("http://a/b/c/d/.././e")), equalTo(URI.create("http://a/b/c/e")));

        baseUri = URI.create("http://a/b/c/d?q=v");
        assertThat(UriTemplate.relativize(baseUri, URI.create("http://a/b/c/e")), equalTo(URI.create("http://a/b/c/e")));
        assertThat(UriTemplate.relativize(baseUri, URI.create("http://a/b/c/./e")), equalTo(URI.create("http://a/b/c/e")));
        assertThat(UriTemplate.relativize(baseUri, URI.create("http://a/b/c/d/.././e")), equalTo(URI.create("http://a/b/c/e")));
    }

    @Test
    public void testTemplateNames() {
        _testTemplateNames("{a}", "a");
        _testTemplateNames("{  a}", "a");
        _testTemplateNames("{  a  }", "a");
        _testTemplateNames("{a:}", "a");
        _testTemplateNames("{a :}", "a");
        _testTemplateNames("{a : }", "a");

        _testTemplateNames("http://example.org/{a}/{b}/", "a", "b");
        _testTemplateNames("http://example.org/page1#{a}", "a");
        _testTemplateNames("{scheme}://{20}.example.org?date={wilma}&option={a}", "scheme", "20", "wilma", "a");
        _testTemplateNames("http://example.org/{a-b}", "a-b");
        _testTemplateNames("http://example.org?{p}", "p");
        _testTemplateNames("http://example.com/order/{c}/{c}/{c}/", "c", "c", "c");
    }

    void _testTemplateNames(final String template, final String... names) {
        final UriTemplate t = new UriTemplate(template);
        _testTemplateNames(t.getTemplateVariables(), names);
    }

    void _testTemplateNames(final List<String> regexNames, final String... names) {
        assertEquals(names.length, regexNames.size());

        final Iterator<String> i = regexNames.iterator();
        for (final String name : names) {
            assertEquals(name, i.next());
        }
    }

    @Test
    public void testMatching() {
        _testMatching("http://example.org/{a}/{b}/",
                "http://example.org/fred/barney/",
                "fred", "barney");
        _testMatching("http://example.org/page1#{a}",
                "http://example.org/page1#fred",
                "fred");
        _testMatching("{scheme}://{20}.example.org?date={wilma}&option={a}",
                "https://this-is-spinal-tap.example.org?date=2008&option=fred",
                "https", "this-is-spinal-tap", "2008", "fred");
        _testMatching("http://example.org/{a-b}",
                "http://example.org/none%20of%20the%20above",
                "none%20of%20the%20above");
        _testMatching("http://example.org?{p}",
                "http://example.org?quote=to+bo+or+not+to+be",
                "quote=to+bo+or+not+to+be");
        _testMatching("http://example.com/order/{c}/{c}/{c}/",
                "http://example.com/order/cheeseburger/cheeseburger/cheeseburger/",
                "cheeseburger", "cheeseburger", "cheeseburger");
        _testMatching("http://example.com/{q}",
                "http://example.com/hullo#world",
                "hullo#world");
        _testMatching("http://example.com/{e}/",
                "http://example.com/xxx/",
                "xxx");
    }

    @Test
    public void testTemplateRegexes() {
        _testTemplateRegex("{a:}", "(" + UriTemplateParser.TEMPLATE_VALUE_PATTERN.pattern() + ")");
        _testTemplateRegex("{a:.*}", "(.*)");
        _testTemplateRegex("{a:  .*}", "(.*)");
        _testTemplateRegex("{a:  .*  }", "(.*)");
        _testTemplateRegex("{a :  .*  }", "(.*)");
    }

    private void _testTemplateRegex(final String template, final String regex) {
        final UriTemplate t = new UriTemplate(template);
        assertEquals(regex, t.getPattern().toString());
    }

    @Test
    public void testRegexMatching() {
        _testMatching("{b: .+}",
                "1",
                "1");

        _testMatching("{b: .+}",
                "1/2/3",
                "1/2/3");

        _testMatching("http://example.org/{a}/{b: .+}",
                "http://example.org/fred/barney/x/y/z",
                "fred", "barney/x/y/z");

        _testMatching("{b: \\d+}",
                "1234567890",
                "1234567890");

        _testMatching("{a}/{b: .+}/{c}{d: (/.*)?}",
                "1/2/3/4",
                "1", "2/3", "4", "");

        _testMatching("{a}/{b: .+}/{c}{d: (/.*)?}",
                "1/2/3/4/",
                "1", "2/3", "4", "/");
    }

    @Test
    public void testRegexMatchingWithNestedGroups() {
        _testMatching("{b: (\\d+)}",
                "1234567890",
                "1234567890");

        _testMatching("{b: (\\d+)-(\\d+)-(\\d+)}",
                "12-34-56",
                "12-34-56");

        _testMatching("{a: (\\d)(\\d*)}-{b: (\\d)(\\d*)}-{c: (\\d)(\\d*)}",
                "12-34-56",
                "12", "34", "56");
    }

    void _testMatching(final String template, final String uri, final String... values) {
        final UriTemplate t = new UriTemplate(template);
        final Map<String, String> m = new HashMap<String, String>();

        boolean isMatch = t.match(uri, m);
        assertTrue(isMatch);
        assertEquals(values.length, t.getTemplateVariables().size());

        final Iterator<String> names = t.getTemplateVariables().iterator();
        for (final String value : values) {
            final String mapValue = m.get(names.next());
            assertEquals(value, mapValue);
        }

        final List<String> matchedValues = new ArrayList<String>();
        isMatch = t.match(uri, matchedValues);
        assertTrue(isMatch);
        assertEquals(values.length, matchedValues.size());

        for (int i = 0; i < values.length; i++) {
            assertEquals(values[i], matchedValues.get(i));
        }

        final MatchResult mr = t.getPattern().match(uri);
        assertNotNull(mr);
        assertEquals(values.length, mr.groupCount());
        assertEquals(uri, mr.group());
        assertEquals(uri, mr.group(0));
        assertEquals(0, mr.start());
        assertEquals(uri.length(), mr.end());
        assertEquals(0, mr.start(0));
        assertEquals(uri.length(), mr.end(0));
        for (int i = 0; i < mr.groupCount(); i++) {
            assertEquals(values[i], mr.group(i + 1));
            assertEquals(values[i], uri.substring(mr.start(i + 1), mr.end(i + 1)));
        }
    }

    @Test
    public void testNullMatching() {
        final Map<String, String> m = new HashMap<String, String>();

        UriTemplate t = UriTemplate.EMPTY;
        assertEquals(false, t.match("/", m));
        assertEquals(true, t.match(null, m));
        assertEquals(true, t.match("", m));

        t = new UriTemplate("/{v}");
        assertEquals(false, t.match(null, m));
        assertEquals(true, t.match("/one", m));
    }

    @Test
    public void testOrder() {
        final List<UriTemplate> l = new ArrayList<UriTemplate>();

        l.add(UriTemplate.EMPTY);
        l.add(new UriTemplate("/{a}"));
        l.add(new UriTemplate("/{a}/{b}"));
        l.add(new UriTemplate("/{a}/one/{b}"));

        Collections.sort(l, UriTemplate.COMPARATOR);

        assertEquals(new UriTemplate("/{a}/one/{b}").getTemplate(),
                l.get(0).getTemplate());
        assertEquals(new UriTemplate("/{a}/{b}").getTemplate(),
                l.get(1).getTemplate());
        assertEquals(new UriTemplate("/{a}").getTemplate(),
                l.get(2).getTemplate());
        assertEquals(UriTemplate.EMPTY.getTemplate(),
                l.get(3).getTemplate());
    }

    @Test
    public void testOrderDuplicitParams() {
        final List<UriTemplate> l = new ArrayList<UriTemplate>();

        l.add(new UriTemplate("/{a}"));
        l.add(new UriTemplate("/{a}/{a}"));

        Collections.sort(l, UriTemplate.COMPARATOR);

        assertEquals(new UriTemplate("/{a}/{a}").getTemplate(),
                l.get(0).getTemplate());
        assertEquals(new UriTemplate("/{a}").getTemplate(),
                l.get(1).getTemplate());
    }

    @Test
    public void testSubstitutionArray() {
        _testSubstitutionArray("http://example.org/{a}/{b}/",
                "http://example.org/fred/barney/",
                "fred", "barney");
        _testSubstitutionArray("http://example.org/page1#{a}",
                "http://example.org/page1#fred",
                "fred");
        _testSubstitutionArray("{scheme}://{20}.example.org?date={wilma}&option={a}",
                "https://this-is-spinal-tap.example.org?date=&option=fred",
                "https", "this-is-spinal-tap", "", "fred");
        _testSubstitutionArray("http://example.org/{a-b}",
                "http://example.org/none%20of%20the%20above",
                "none%20of%20the%20above");
        _testSubstitutionArray("http://example.org?{p}",
                "http://example.org?quote=to+bo+or+not+to+be",
                "quote=to+bo+or+not+to+be");
        _testSubstitutionArray("http://example.com/order/{c}/{c}/{c}/",
                "http://example.com/order/cheeseburger/cheeseburger/cheeseburger/",
                "cheeseburger");
        _testSubstitutionArray("http://example.com/{q}",
                "http://example.com/hullo#world",
                "hullo#world");
        _testSubstitutionArray("http://example.com/{e}/",
                "http://example.com//",
                "");
        _testSubstitutionArray("http://example.com/{a}/{b}/{a}",
                "http://example.com/fred/barney/fred",
                "fred", "barney", "joe");
    }

    @Test
    public void testGroupIndexes() throws Exception {
        UriTemplate template = new UriTemplate("/a");
        assertThat(template.getPattern().getGroupIndexes(), equalTo(new int[0]));

        template = new UriTemplate("/{a}");
        assertThat(template.getPattern().getGroupIndexes(), equalTo(new int[] {1}));

        template = new UriTemplate("/{a}/b");
        assertThat(template.getPattern().getGroupIndexes(), equalTo(new int[] {1}));

        template = new UriTemplate("/{a}/{b}");
        assertThat(template.getPattern().getGroupIndexes(), equalTo(new int[] {1, 2}));

        template = new UriTemplate("/{a}/{b}");
        assertThat(template.getPattern().getGroupIndexes(), equalTo(new int[] {1, 2}));

        template = new UriTemplate("/{a}/b/{c}");
        assertThat(template.getPattern().getGroupIndexes(), equalTo(new int[] {1, 2}));

        template = new UriTemplate("/{a: (abc)+}");
        assertThat(template.getPattern().getGroupIndexes(), equalTo(new int[] {1}));

        template = new UriTemplate("/{a: (abc)+}/b");
        assertThat(template.getPattern().getGroupIndexes(), equalTo(new int[] {1}));

        template = new UriTemplate("/{a: (abc)+}/{b}");
        assertThat(template.getPattern().getGroupIndexes(), equalTo(new int[] {1, 3}));

        template = new UriTemplate("/{a: (abc)+}/b/{c}");
        assertThat(template.getPattern().getGroupIndexes(), equalTo(new int[] {1, 3}));
    }

    void _testSubstitutionArray(final String template, final String uri, final String... values) {
        final UriTemplate t = new UriTemplate(template);

        assertEquals(uri, t.createURI(values));
    }

    @Test
    public void testSubstitutionMap() {
        _testSubstitutionMap("http://example.org/{a}/{b}/",
                "http://example.org/fred/barney/",
                "a", "fred",
                "b", "barney");
        _testSubstitutionMap("http://example.org/page1#{a}",
                "http://example.org/page1#fred",
                "a", "fred");
        _testSubstitutionMap("{scheme}://{20}.example.org?date={wilma}&option={a}",
                "https://this-is-spinal-tap.example.org?date=&option=fred",
                "scheme", "https",
                "20", "this-is-spinal-tap",
                "wilma", "",
                "a", "fred");
        _testSubstitutionMap("http://example.org/{a-b}",
                "http://example.org/none%20of%20the%20above",
                "a-b", "none%20of%20the%20above");
        _testSubstitutionMap("http://example.org?{p}",
                "http://example.org?quote=to+bo+or+not+to+be",
                "p", "quote=to+bo+or+not+to+be");
        _testSubstitutionMap("http://example.com/order/{c}/{c}/{c}/",
                "http://example.com/order/cheeseburger/cheeseburger/cheeseburger/",
                "c", "cheeseburger");
        _testSubstitutionMap("http://example.com/{q}",
                "http://example.com/hullo#world",
                "q", "hullo#world");
        _testSubstitutionMap("http://example.com/{e}/",
                "http://example.com//",
                "e", "");
    }

    void _testSubstitutionMap(final String template, final String uri, final String... variablesAndvalues) {
        final UriTemplate t = new UriTemplate(template);

        final Map<String, String> variableMap = new HashMap<String, String>();
        for (int i = 0; i < variablesAndvalues.length; i += 2) {
            variableMap.put(variablesAndvalues[i], variablesAndvalues[i + 1]);
        }

        assertEquals(uri, t.createURI(variableMap));
    }

    @Test
    public void testNormalizesURIs() throws Exception {
        this.validateNormalize("/some-path", "/some-path");
        this.validateNormalize("http://example.com/some/../path", "http://example.com/path");
        // note, that following behaviour differs from Jersey-1.x UriHelper.normalize(), the '..' segment is simply left out in
        // this case, where older UriHelper.normalize() would return the path including the '..' segment. It is also mentioned
        // in the UriTemplate.normalize() javadoc.
        this.validateNormalize("http://example.com/../path", "http://example.com/path");
        this.validateNormalize("http://example.com//path", "http://example.com//path");
    }

    private void validateNormalize(final String path, final String expected) throws Exception {
        final URI result = UriTemplate.normalize(path);
        assertEquals(expected, result.toString());
    }

    @Test
    public void testSingleQueryParameter() throws Exception {
        final UriTemplate tmpl = new UriTemplate("/test{?query}");

        final Map<String, String> result = new HashMap<String, String>();
        tmpl.match("/test?query=x", result);

        assertEquals(
                "incorrect size for match string",
                1,
                result.size()
        );

        assertEquals(
                "query parameter is not matched",
                "x",
                result.get("query")
        );
    }

    @Test
    public void testDoubleQueryParameter() throws Exception {
        final UriTemplate tmpl = new UriTemplate("/test{?query,secondQuery}");

        final List<String> list = new ArrayList<String>();
        tmpl.match("/test?query=x&secondQuery=y", list);

        final Map<String, String> result = new HashMap<String, String>();
        tmpl.match("/test?query=x&secondQuery=y", result);

        assertEquals(
                "incorrect size for match string",
                2,
                result.size()
        );

        assertEquals(
                "query parameter is not matched",
                "x",
                result.get("query")
        );
        assertEquals(
                "query parameter is not matched",
                "y",
                result.get("secondQuery")
        );
    }

    @Test
    public void testSettingQueryParameter() throws Exception {
        final UriTemplate tmpl = new UriTemplate("/test{?query}");

        final Map<String, String> values = new HashMap<String, String>();
        values.put("query", "example");

        final String uri = tmpl.createURI(values);
        assertEquals(
                "query string is not set",
                "/test?query=example",
                uri
        );
    }

    @Test
    public void testSettingTwoQueryParameter() throws Exception {
        final UriTemplate tmpl = new UriTemplate("/test{?query,other}");

        final Map<String, String> values = new HashMap<String, String>();
        values.put("query", "example");
        values.put("other", "otherExample");

        final String uri = tmpl.createURI(values);
        assertEquals(
                "query string is not set",
                "/test?query=example&other=otherExample",
                uri
        );

    }

    @Test
    public void testNotSettingQueryParameter() throws Exception {
        final UriTemplate tmpl = new UriTemplate("/test{?query}");

        final Map<String, String> values = new HashMap<String, String>();

        final String uri = tmpl.createURI(values);
        assertEquals(
                "query string is set",
                "/test",
                uri
        );

    }

    @Test
    public void testSettingMatrixParameter() throws Exception {
        final UriTemplate tmpl = new UriTemplate("/test{;matrix}/other");

        final Map<String, String> values = new HashMap<String, String>();
        values.put("matrix", "example");

        final String uri = tmpl.createURI(values);
        assertEquals(
                "query string is not set",
                "/test;matrix=example/other",
                uri
        );

    }

    @Test
    public void testSettingTwoMatrixParameter() throws Exception {
        final UriTemplate tmpl = new UriTemplate("/test{;matrix,other}/other");

        final Map<String, String> values = new HashMap<String, String>();
        values.put("matrix", "example");
        values.put("other", "otherExample");

        final String uri = tmpl.createURI(values);
        assertEquals(
                "query string is not set",
                "/test;matrix=example;other=otherExample/other",
                uri
        );

    }

    @Test
    public void testSettingTwoSeperatedMatrixParameter() throws Exception {
        final UriTemplate tmpl = new UriTemplate("/test{;matrix}/other{;other}");

        final Map<String, String> values = new HashMap<String, String>();
        values.put("matrix", "example");
        values.put("other", "otherExample");

        final String uri = tmpl.createURI(values);
        assertEquals(
                "query string is not set",
                "/test;matrix=example/other;other=otherExample",
                uri
        );
    }

    @Test
    public void testNotSettingMatrixParameter() throws Exception {
        final UriTemplate tmpl = new UriTemplate("/test{;query}/other");

        final Map<String, String> values = new HashMap<String, String>();

        final String uri = tmpl.createURI(values);
        assertEquals(
                "query string is set",
                "/test/other",
                uri
        );
    }

    /*
        RFC 6570, section 3.2:

             count := ("one", "two", "three")
             dom   := ("example", "com")
             dub   := "me/too"
             hello := "Hello World!"
             half  := "50%"
             var   := "value"
             who   := "fred"
             base  := "http://example.com/home/"
             path  := "/foo/bar"
             list  := ("red", "green", "blue")
             keys  := [("semi",";"),("dot","."),("comma",",")]
             v     := "6"
             x     := "1024"
             y     := "768"
             empty := ""
             empty_keys  := []
             undef := null
     */
    private static final List<String> count = Arrays.asList("one", "two", "three");
    private static final List<String> dom = Arrays.asList("example", "com");
    private static final String dub = "me/too";
    private static final String hello = "Hello World!";
    private static final String half = "50%";
    private static final String var = "value";
    private static final String who = "fred";
    private static final String base = "http://example.com/home/";
    private static final String path = "/foo/bar";
    private static final List<String> list = Arrays.asList("red", "green", "blue");
    private static final Map<String, String> keys = new HashMap<String, String>() {{
        put("semi", ";");
        put("dot", ".");
        put("comma", ",");
    }};
    private static final String v = "6";
    private static final String x = "1024";
    private static final String y = "768";
    private static final String empty = "";
    private static final Map<String, String> emptyKeys = Collections.emptyMap();

    @Test
    public void testRfc6570QueryTemplateExamples() {
        /*
            RFC 6570, section 3.2.8:

               {?who}             ?who=fred
               {?half}            ?half=50%25
               {?x,y}             ?x=1024&y=768
               {?x,y,empty}       ?x=1024&y=768&empty=
               {?x,y,undef}       ?x=1024&y=768
               {?var:3}           ?var=val
               {?list}            ?list=red,green,blue
               {?list*}           ?list=red&list=green&list=blue
               {?keys}            ?keys=semi,%3B,dot,.,comma,%2C
               {?keys*}           ?semi=%3B&dot=.&comma=%2C
        */

        assertEncodedQueryTemplateExpansion("?who=fred", "{?who}", who);
        assertEncodedQueryTemplateExpansion("?half=50%25", "{?half}", half);
        assertEncodedQueryTemplateExpansion("?x=1024&y=768", "{?x,y}", x, y);
        assertEncodedQueryTemplateExpansion("?x=1024&y=768&empty=", "{?x,y,empty}", x, y, empty);
        assertEncodedQueryTemplateExpansion("?x=1024&y=768", "{?x,y,undef}", x, y);

        // TODO assertEncodedQueryTemplateExpansion("?var=val", "{?var:3}", var);
        // TODO assertEncodedQueryTemplateExpansion("?list=red,green,blue", "{?list}", list);
        // TODO assertEncodedQueryTemplateExpansion("?list=red&list=green&list=blue", "{?list*}", list);
        // TODO assertEncodedQueryTemplateExpansion("?keys=semi,%3B,dot,.,comma,%2C", "{?keys}", keys);
        // TODO assertEncodedQueryTemplateExpansion("?semi=%3B&dot=.&comma=%2C", "{?keys*}", keys);
    }

    private void assertEncodedQueryTemplateExpansion(final String expectedExpansion,
                                                     final String queryTemplate,
                                                     final Object... values) {
        assertEquals("Unexpected encoded query template expansion result.",
                expectedExpansion,
                UriTemplate.createURI(null, null, null, null, null, null, queryTemplate, null, values, true, false));
    }

    private void assertEncodedQueryTemplateExpansion(final String expectedExpansion,
                                                     final String queryTemplate,
                                                     final Map<String, ?> values) {
        assertEquals("Unexpected encoded query template expansion result.",
                expectedExpansion,
                UriTemplate.createURI(null, null, null, null, null, null, queryTemplate, null, values, true, false));
    }

    @Test
    public void testRfc6570MatrixTemplateExamples() {
        /*
            RFC 6570, section 3.2.7:

               {;who}             ;who=fred
               {;half}            ;half=50%25
               {;empty}           ;empty
               {;v,empty,who}     ;v=6;empty;who=fred
               {;v,bar,who}       ;v=6;who=fred
               {;x,y}             ;x=1024;y=768
               {;x,y,empty}       ;x=1024;y=768;empty
               {;x,y,undef}       ;x=1024;y=768
               {;hello:5}         ;hello=Hello
               {;list}            ;list=red,green,blue
               {;list*}           ;list=red;list=green;list=blue
               {;keys}            ;keys=semi,%3B,dot,.,comma,%2C
               {;keys*}           ;semi=%3B;dot=.;comma=%2C
       */
        assertEncodedPathTemplateExpansion(";who=fred", "{;who}", who);
        assertEncodedPathTemplateExpansion(";half=50%25", "{;half}", half);
        assertEncodedPathTemplateExpansion(";empty", "{;empty}", empty);
        assertEncodedPathTemplateExpansion(";v=6;empty;who=fred", "{;v,empty,who}", v, empty, who);
        assertEncodedPathTemplateExpansion(";v=6;who=fred", "{;v,bar,who}", new HashMap<String, String>() {{
            put("v", v);
            put("who", who);
        }});
        assertEncodedPathTemplateExpansion(";x=1024;y=768", "{;x,y}", x, y);
        assertEncodedPathTemplateExpansion(";x=1024;y=768;empty", "{;x,y,empty}", x, y, empty);
        assertEncodedPathTemplateExpansion(";x=1024;y=768", "{;x,y,undef}", x, y);
        // TODO assertEncodedPathTemplateExpansion(";hello=Hello", "{;hello:5}", hello);
        // TODO assertEncodedPathTemplateExpansion(";list=red,green,blue", "{;list}", list);
        // TODO assertEncodedPathTemplateExpansion(";list=red;list=green;list=blue", "{;list*}", list);
        // TODO assertEncodedPathTemplateExpansion(";keys=semi,%3B,dot,.,comma,%2C", "{;keys}", keys);
        // TODO assertEncodedPathTemplateExpansion(";semi=%3B;dot=.;comma=%2C", "{;keys*}", keys);
    }

    private void assertEncodedPathTemplateExpansion(final String expectedExpansion,
                                                    final String pathTemplate,
                                                    final Object... values) {
        assertEquals("Unexpected encoded matrix parameter template expansion result.",
                expectedExpansion,
                UriTemplate.createURI(null, null, null, null, null, pathTemplate, null, null, values, true, false));
    }

    private void assertEncodedPathTemplateExpansion(final String expectedExpansion,
                                                    final String pathTemplate,
                                                    final Map<String, ?> values) {
        assertEquals("Unexpected encoded matrix parameter template expansion result.",
                expectedExpansion,
                UriTemplate.createURI(null, null, null, null, null, pathTemplate, null, null, values, true, false));
    }
}
