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

package org.glassfish.jersey.server.internal.inject;

import java.util.SortedSet;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.glassfish.jersey.server.ContainerResponse;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author Paul Sandoz
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class QueryParamAsSortedSetPrimitiveTest extends AbstractTest {

    public QueryParamAsSortedSetPrimitiveTest() {
        initiateWebApplication(
                ResourceQueryPrimitiveSortedSet.class,
                ResourceQueryPrimitiveSortedSetDefaultEmpty.class,
                ResourceQueryPrimitiveSortedSetDefault.class,
                ResourceQueryPrimitiveSortedSetDefaultOverride.class
        );
    }

    @Path("/SortedSet")
    public static class ResourceQueryPrimitiveSortedSet {

        @GET
        @Produces("application/boolean")
        public String doGetBoolean(@QueryParam("boolean") SortedSet<Boolean> v) {
            assertTrue(v.contains(true));
            return "content";
        }

        @GET
        @Produces("application/byte")
        public String doGetByte(@QueryParam("byte") SortedSet<Byte> v) {
            assertTrue(v.contains((byte) 127));
            return "content";
        }

        @GET
        @Produces("application/short")
        public String doGetShort(@QueryParam("short") SortedSet<Short> v) {
            assertTrue(v.contains((short) 32767));
            return "content";
        }

        @GET
        @Produces("application/int")
        public String doGetInteger(@QueryParam("int") SortedSet<Integer> v) {
            assertTrue(v.contains(2147483647));
            return "content";
        }

        @GET
        @Produces("application/long")
        public String doGetLong(@QueryParam("long") SortedSet<Long> v) {
            assertTrue(v.contains(9223372036854775807L));
            return "content";
        }

        @GET
        @Produces("application/float")
        public String doGetFloat(@QueryParam("float") SortedSet<Float> v) {
            assertTrue(v.contains(3.14159265f));
            return "content";
        }

        @GET
        @Produces("application/double")
        public String doGetDouble(@QueryParam("double") SortedSet<Double> v) {
            assertTrue(v.contains(3.14159265358979d));
            return "content";
        }
    }

    @Path("/SortedSet/default/null")
    public static class ResourceQueryPrimitiveSortedSetDefaultEmpty {

        @GET
        @Produces("application/boolean")
        public String doGetBoolean(@QueryParam("boolean") SortedSet<Boolean> v) {
            assertEquals(0, v.size());
            return "content";
        }

        @GET
        @Produces("application/byte")
        public String doGetByte(@QueryParam("byte") SortedSet<Byte> v) {
            assertEquals(0, v.size());
            return "content";
        }

        @GET
        @Produces("application/short")
        public String doGetShort(@QueryParam("short") SortedSet<Short> v) {
            assertEquals(0, v.size());
            return "content";
        }

        @GET
        @Produces("application/int")
        public String doGetInteger(@QueryParam("int") SortedSet<Integer> v) {
            assertEquals(0, v.size());
            return "content";
        }

        @GET
        @Produces("application/long")
        public String doGetLong(@QueryParam("long") SortedSet<Long> v) {
            assertEquals(0, v.size());
            return "content";
        }

        @GET
        @Produces("application/float")
        public String doGetFloat(@QueryParam("float") SortedSet<Float> v) {
            assertEquals(0, v.size());
            return "content";
        }

        @GET
        @Produces("application/double")
        public String doGetDouble(@QueryParam("double") SortedSet<Double> v) {
            assertEquals(0, v.size());
            return "content";
        }
    }

    @Path("/SortedSet/default")
    public static class ResourceQueryPrimitiveSortedSetDefault {

        @GET
        @Produces("application/boolean")
        public String doGetBoolean(@QueryParam("boolean") @DefaultValue("true") SortedSet<Boolean> v) {
            assertTrue(v.contains(true));
            return "content";
        }

        @GET
        @Produces("application/byte")
        public String doGetByte(@QueryParam("byte") @DefaultValue("127") SortedSet<Byte> v) {
            assertTrue(v.contains((byte) 127));
            return "content";
        }

        @GET
        @Produces("application/short")
        public String doGetShort(@QueryParam("short") @DefaultValue("32767") SortedSet<Short> v) {
            assertTrue(v.contains((short) 32767));
            return "content";
        }

        @GET
        @Produces("application/int")
        public String doGetInteger(@QueryParam("int") @DefaultValue("2147483647") SortedSet<Integer> v) {
            assertTrue(v.contains(2147483647));
            return "content";
        }

        @GET
        @Produces("application/long")
        public String doGetLong(@QueryParam("long") @DefaultValue("9223372036854775807") SortedSet<Long> v) {
            assertTrue(v.contains(9223372036854775807L));
            return "content";
        }

        @GET
        @Produces("application/float")
        public String doGetFloat(@QueryParam("float") @DefaultValue("3.14159265") SortedSet<Float> v) {
            assertTrue(v.contains(3.14159265f));
            return "content";
        }

        @GET
        @Produces("application/double")
        public String doGetDouble(@QueryParam("double") @DefaultValue("3.14159265358979") SortedSet<Double> v) {
            assertTrue(v.contains(3.14159265358979d));
            return "content";
        }
    }

    @Path("/SortedSet/default/override")
    public static class ResourceQueryPrimitiveSortedSetDefaultOverride {

        @GET
        @Produces("application/boolean")
        public String doGetBoolean(@QueryParam("boolean") @DefaultValue("false") SortedSet<Boolean> v) {
            assertTrue(v.contains(true));
            return "content";
        }

        @GET
        @Produces("application/byte")
        public String doGetByte(@QueryParam("byte") @DefaultValue("0") SortedSet<Byte> v) {
            assertTrue(v.contains((byte) 127));
            return "content";
        }

        @GET
        @Produces("application/short")
        public String doGetShort(@QueryParam("short") @DefaultValue("0") SortedSet<Short> v) {
            assertTrue(v.contains((short) 32767));
            return "content";
        }

        @GET
        @Produces("application/int")
        public String doGetInteger(@QueryParam("int") @DefaultValue("0") SortedSet<Integer> v) {
            assertTrue(v.contains(2147483647));
            return "content";
        }

        @GET
        @Produces("application/long")
        public String doGetLong(@QueryParam("long") @DefaultValue("0") SortedSet<Long> v) {
            assertTrue(v.contains(9223372036854775807L));
            return "content";
        }

        @GET
        @Produces("application/float")
        public String doGetFloat(@QueryParam("float") @DefaultValue("0.0") SortedSet<Float> v) {
            assertTrue(v.contains(3.14159265f));
            return "content";
        }

        @GET
        @Produces("application/double")
        public String doGetDouble(@QueryParam("double") @DefaultValue("0.0") SortedSet<Double> v) {
            assertTrue(v.contains(3.14159265358979d));
            return "content";
        }
    }

    void _test(String type, String value) throws ExecutionException, InterruptedException {
        String param = type + "=" + value;

        super.getResponseContext("/SortedSet?" + param + "&" + param + "&" + param, "application/" + type);
    }

    void _testDefault(String base, String type, String value) throws ExecutionException, InterruptedException {
        super.getResponseContext(base + "default/null", "application/" + type);

        super.getResponseContext(base + "default", "application/" + type);

        String param = type + "=" + value;
        super.getResponseContext(base + "default/override?" + param, "application/" + type);
    }

    void _testSortedSetDefault(String type, String value) throws ExecutionException, InterruptedException {
        _testDefault("/SortedSet/", type, value);
    }

    @Test
    public void testGetBoolean() throws ExecutionException, InterruptedException {
        _test("boolean", "true");
    }

    @Test
    public void testGetBooleanPrimitiveSortedSetDefault() throws ExecutionException, InterruptedException {
        _testSortedSetDefault("boolean", "true");
    }

    @Test
    public void testGetByte() throws ExecutionException, InterruptedException {
        _test("byte", "127");
    }

    @Test
    public void testGetBytePrimitiveSortedSetDefault() throws ExecutionException, InterruptedException {
        _testSortedSetDefault("byte", "127");
    }

    @Test
    public void testGetShort() throws ExecutionException, InterruptedException {
        _test("short", "32767");
    }

    @Test
    public void testGetShortPrimtiveSortedSetDefault() throws ExecutionException, InterruptedException {
        _testSortedSetDefault("short", "32767");
    }

    @Test
    public void testGetInt() throws ExecutionException, InterruptedException {
        _test("int", "2147483647");
    }

    @Test
    public void testGetIntPrimitiveSortedSetDefault() throws ExecutionException, InterruptedException {
        _testSortedSetDefault("int", "2147483647");
    }

    @Test
    public void testGetLong() throws ExecutionException, InterruptedException {
        _test("long", "9223372036854775807");
    }

    @Test
    public void testGetLongPrimitiveSortedSetDefault() throws ExecutionException, InterruptedException {
        _testSortedSetDefault("long", "9223372036854775807");
    }

    @Test
    public void testGetFloat() throws ExecutionException, InterruptedException {
        _test("float", "3.14159265");
    }

    @Test
    public void testGetFloatPrimitiveSortedSetDefault() throws ExecutionException, InterruptedException {
        _testSortedSetDefault("float", "3.14159265");
    }

    @Test
    public void testGetDouble() throws ExecutionException, InterruptedException {
        _test("double", "3.14159265358979");
    }

    @Test
    public void testGetDoublePrimitiveSortedSetDefault() throws ExecutionException, InterruptedException {
        _testSortedSetDefault("double", "3.14159265358979");
    }

    @Test
    public void testBadPrimitiveSortedSetValue() throws ExecutionException, InterruptedException {
        final ContainerResponse response = super.getResponseContext("/SortedSet?int=abcdef&int=abcdef", "application/int");

        assertEquals(404, response.getStatus());
    }
}
