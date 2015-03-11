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
package org.glassfish.jersey.client.proxy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;

import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Martin Matula
 */
public class WebResourceFactoryTest extends JerseyTest {

    private MyResourceIfc resource;
    private MyResourceIfc resourceWithXML;

    @Override
    protected ResourceConfig configure() {
        // mvn test -Djersey.config.test.container.factory=org.glassfish.jersey.test.inmemory.InMemoryTestContainerFactory
        // mvn test -Djersey.config.test.container.factory=org.glassfish.jersey.test.grizzly.GrizzlyTestContainerFactory
        // mvn test -Djersey.config.test.container.factory=org.glassfish.jersey.test.jdkhttp.JdkHttpServerTestContainerFactory
        // mvn test -Djersey.config.test.container.factory=org.glassfish.jersey.test.simple.SimpleTestContainerFactory
        enable(TestProperties.LOG_TRAFFIC);
        //        enable(TestProperties.DUMP_ENTITY);
        return new ResourceConfig(MyResource.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        resource = WebResourceFactory.newResource(MyResourceIfc.class, target());

        final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>(1);
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML);
        resourceWithXML = WebResourceFactory
                .newResource(MyResourceIfc.class, target(), false, headers, Collections.<Cookie>emptyList(), new Form());
    }

    @Test
    public void testGetIt() {
        assertEquals("Got it!", resource.getIt());
    }

    @Test
    public void testPostIt() {
        final MyBean bean = new MyBean();
        bean.name = "Ahoj";
        assertEquals("Ahoj", resource.postIt(Collections.singletonList(bean)).get(0).name);
    }

    @Test
    public void testPostValid() {
        final MyBean bean = new MyBean();
        bean.name = "Ahoj";
        assertEquals("Ahoj", resource.postValid(bean).name);
    }

    @Test
    public void testPathParam() {
        assertEquals("jouda", resource.getId("jouda"));
    }

    @Test
    public void testQueryParam() {
        assertEquals("jiri", resource.getByName("jiri"));
    }

    @Test
    public void testFormParam() {
        assertEquals("jiri", resource.postByNameFormParam("jiri"));
    }

    @Test
    public void testCookieParam() {
        assertEquals("jiri", resource.getByNameCookie("jiri"));
    }

    @Test
    public void testHeaderParam() {
        assertEquals("jiri", resource.getByNameHeader("jiri"));
    }

    @Test
    public void testMatrixParam() {
        assertEquals("jiri", resource.getByNameMatrix("jiri"));
    }

    @Test
    public void testSubResource() {
        assertEquals("Got it!", resource.getSubResource().getMyBean().name);
    }

    @Test
    public void testQueryParamsAsList() {
        final List<String> list = new ArrayList<>();
        list.add("a");
        list.add("bb");
        list.add("ccc");

        assertEquals("3:[a, bb, ccc]", resource.getByNameList(list));
    }

    @Test
    public void testQueryParamsAsSet() {
        final Set<String> set = new HashSet<>();
        set.add("a");
        set.add("bb");
        set.add("ccc");

        final String result = resource.getByNameSet(set);
        checkSet(result);
    }

    @Test
    public void testQueryParamsAsSortedSet() {
        final SortedSet<String> set = new TreeSet<>();
        set.add("a");
        set.add("bb");
        set.add("ccc");

        final String result = resource.getByNameSortedSet(set);
        assertEquals("3:[a, bb, ccc]", result);
    }

    @Test
    @Ignore("See issue JERSEY-2441")
    public void testHeaderCookieAsList() {
        final List<String> list = new ArrayList<>();
        list.add("a");
        list.add("bb");
        list.add("ccc");

        assertEquals("3:[a, bb, ccc]", resource.getByNameCookieList(list));
    }

    @Test
    @Ignore("See issue JERSEY-2441")
    public void testHeaderCookieAsSet() {
        final Set<String> set = new HashSet<>();
        set.add("a");
        set.add("bb");
        set.add("ccc");

        final String result = resource.getByNameCookieSet(set);
        checkSet(result);
    }

    @Test
    @Ignore("See issue JERSEY-2441")
    public void testHeaderCookieAsSortedSet() {
        final SortedSet<String> set = new TreeSet<>();
        set.add("a");
        set.add("bb");
        set.add("ccc");

        final String result = resource.getByNameCookieSortedSet(set);
        assertEquals("3:[a, bb, ccc]", result);
    }

    /**
     * This cannot work with jersey now. Server side parses header params only if they are send as more
     * lines in the request. Jersey has currently no possibility to do so. See JERSEY-2263.
     */
    @Test
    @Ignore("See issue JERSEY-2263")
    public void testHeaderParamsAsList() {
        final List<String> list = new ArrayList<>();
        list.add("a");
        list.add("bb");
        list.add("ccc");

        assertEquals("3:[a, bb, ccc]", resource.getByNameHeaderList(list));
    }

    @Test
    @Ignore("See issue JERSEY-2263")
    public void testHeaderParamsAsSet() {
        final Set<String> set = new HashSet<>();
        set.add("a");
        set.add("bb");
        set.add("ccc");

        final String result = resource.getByNameHeaderSet(set);
        checkSet(result);
    }

    @Test
    @Ignore("See issue JERSEY-2263")
    public void testHeaderParamsAsSortedSet() {
        final SortedSet<String> set = new TreeSet<>();
        set.add("a");
        set.add("bb");
        set.add("ccc");

        final String result = resource.getByNameHeaderSortedSet(set);
        assertEquals("3:[a, bb, ccc]", result);
    }

    @Test
    public void testMatrixParamsAsList() {
        final List<String> list = new ArrayList<>();
        list.add("a");
        list.add("bb");
        list.add("ccc");

        assertEquals("3:[a, bb, ccc]", resource.getByNameMatrixList(list));
    }

    @Test
    public void testMatrixParamsAsSet() {
        final Set<String> set = new HashSet<>();
        set.add("a");
        set.add("bb");
        set.add("ccc");

        final String result = resource.getByNameMatrixSet(set);
        checkSet(result);
    }

    @Test
    public void testMatrixParamsAsSortedSet() {
        final SortedSet<String> set = new TreeSet<>();
        set.add("a");
        set.add("bb");
        set.add("ccc");

        final String result = resource.getByNameMatrixSortedSet(set);
        assertEquals("3:[a, bb, ccc]", result);
    }

    private void checkSet(final String result) {
        assertTrue("Set does not contain 3 items.", result.startsWith("3:["));
        assertTrue("Set does not contain 'a' item.", result.contains("a"));
        assertTrue("Set does not contain 'bb' item.", result.contains("bb"));
        assertTrue("Set does not contain 'ccc' item.", result.contains("ccc"));
    }

    @Test
    public void testFormParamsAsList() {
        final List<String> list = new ArrayList<>();
        list.add("a");
        list.add("bb");
        list.add("ccc");

        assertEquals("3:[a, bb, ccc]", resource.postByNameFormList(list));
    }

    @Test
    public void testFormParamsAsSet() {
        final Set<String> set = new HashSet<>();
        set.add("a");
        set.add("bb");
        set.add("ccc");

        final String result = resource.postByNameFormSet(set);
        checkSet(result);
    }

    @Test
    public void testFormParamsAsSortedSet() {
        final SortedSet<String> set = new TreeSet<>();
        set.add("a");
        set.add("bb");
        set.add("ccc");

        final String result = resource.postByNameFormSortedSet(set);
        assertEquals("3:[a, bb, ccc]", result);
    }

    @Test
    public void testAcceptHeader() {
        assertTrue("Accept HTTP header does not match @Produces annotation", resource.isAcceptHeaderValid(null));
    }

    @Test
    public void testPutWithExplicitContentType() {
        assertEquals("Content-Type HTTP header does not match explicitly provided type", resourceWithXML.putIt(new MyBean()),
                MediaType.APPLICATION_XML);
    }

    @Test
    public void testToString() throws Exception {
        final String actual = resource.toString();
        final String expected = target().path("myresource").toString();

        assertEquals(expected, actual);
    }
}
