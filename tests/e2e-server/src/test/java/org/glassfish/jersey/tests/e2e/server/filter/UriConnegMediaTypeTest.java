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

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

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
public class UriConnegMediaTypeTest extends JerseyTest {

    @Override
    protected Application configure() {
        Map<String, MediaType> mediaTypes = new HashMap<>();
        mediaTypes.put("foo", MediaType.valueOf("application/foo"));
        mediaTypes.put("bar", MediaType.valueOf("application/bar"));
        mediaTypes.put("foot", MediaType.valueOf("application/foot"));

        Set<Class<?>> classes = new HashSet<>();

        for (Class<?> c : UriConnegMediaTypeTest.class.getClasses()) {
            if (c.getAnnotation(Path.class) != null) {
                classes.add(c);
            }
        }

        ResourceConfig rc = new ResourceConfig(classes);
        rc.property(ServerProperties.MEDIA_TYPE_MAPPINGS, mediaTypes);
        return rc;
    }

    public abstract static class Base {
        @GET
        @Produces("application/foo")
        public String doGetFoo(@Context HttpHeaders headers) {
            assertEquals(1, headers.getAcceptableMediaTypes().size());
            return "foo";
        }

        @GET
        @Produces("application/foot")
        public String doGetFoot() {
            return "foot";
        }

        @GET
        @Produces("application/bar")
        public String doGetBar() {
            return "bar";
        }
    }

    @Path("/abc")
    public static class SingleSegment extends Base {
    }

    @Path("/xyz/")
    public static class SingleSegmentSlash extends Base {
    }

    @Path("/xyz/abc")
    public static class MultipleSegments extends Base {
    }

    @Path("/xyz/xxx/")
    public static class MultipleSegmentsSlash extends Base {
    }

    @Path("/xyz/abc.xml")
    public static class DotPrefixSegments extends Base {
    }

    @Path("/foo_bar_foot")
    public static class PathWithSuffixSegment extends Base {
    }

    @Path("/")
    public static class SubResourceMethods extends Base {
        @Path("sub")
        @GET
        @Produces("application/foo")
        public String doGetFooS() {
            return "foo";
        }

        @Path("sub")
        @GET
        @Produces("application/foot")
        public String doGetFootS() {
            return "foot";
        }

        @Path("sub")
        @GET
        @Produces("application/bar")
        public String doGetBarS() {
            return "bar";
        }
    }

    @Test
    public void testSlash() throws IOException {
        _test("/");
    }

    @Test
    public void testSingleSegment() throws IOException {
        _test("/", "abc");
    }

    @Test
    public void testSingleSegmentSlash() throws IOException {
        _test("/", "xyz", "/");
    }

    @Test
    public void testMultipleSegments() throws IOException {
        _test("/xyz", "abc");
    }

    @Test
    public void testMultipleSegmentsSlash() throws IOException {
        _test("/xyz", "xxx", "/");
    }

    @Test
    public void testDotPrefixSegments() throws IOException {
        _test("/xyz", "abc.xml");
        _test("/xyz", "abc", ".xml");
    }

    @Test
    public void testXXXSegment() throws IOException {
        _test("/", "foo_bar_foot");
    }

    @Test
    public void testSubResourceMethods() throws IOException {
        _test("/", "sub");
    }

    private void _test(String base) {
        _test(base, "", "");
    }

    private void _test(String base, String path) {
        _test(base, path, "");
    }

    private void _test(String base, String path, String terminate) {
        WebTarget r = target().path(base);

        String s = r.path(path + ".foo" + terminate).request().get(String.class);
        assertEquals("foo", s);

        s = r.path(path + ".foo" + terminate).request("application/bar").get(String.class);
        assertEquals("foo", s);

        s = r.path(path + ".foot" + terminate).request().get(String.class);
        assertEquals("foot", s);

        s = r.path(path + ".bar" + terminate).request().get(String.class);
        assertEquals("bar", s);

        s = r.path(path + terminate).request("application/foo").get(String.class);
        assertEquals("foo", s);

        s = r.path(path + terminate).request("application/foot").get(String.class);
        assertEquals("foot", s);

        s = r.path(path + terminate).request("application/foo;q=0.1").get(String.class);
        assertEquals("foo", s);
    }
}
