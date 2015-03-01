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

import java.util.Set;
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
public class QueryParamAsSetPrimitiveTest extends AbstractTest {

    public QueryParamAsSetPrimitiveTest() {
        initiateWebApplication(
                ResourceQueryPrimitiveSet.class,
                ResourceQueryPrimitiveSetDefaultEmpty.class,
                ResourceQueryPrimitiveSetDefault.class,
                ResourceQueryPrimitiveSetDefaultOverride.class
        );
    }

    @Path("/Set")
    public static class ResourceQueryPrimitiveSet {

        @GET
        @Produces("application/boolean")
        public String doGetBoolean(@QueryParam("boolean") Set<Boolean> v) {
            assertTrue(v.contains(true));
            return "content";
        }

        @GET
        @Produces("application/byte")
        public String doGetByte(@QueryParam("byte") Set<Byte> v) {
            assertTrue(v.contains((byte) 127));
            return "content";
        }

        @GET
        @Produces("application/short")
        public String doGetShort(@QueryParam("short") Set<Short> v) {
            assertTrue(v.contains((short) 32767));
            return "content";
        }

        @GET
        @Produces("application/int")
        public String doGetInteger(@QueryParam("int") Set<Integer> v) {
            assertTrue(v.contains(2147483647));
            return "content";
        }

        @GET
        @Produces("application/long")
        public String doGetLong(@QueryParam("long") Set<Long> v) {
            assertTrue(v.contains(9223372036854775807L));
            return "content";
        }

        @GET
        @Produces("application/float")
        public String doGetFloat(@QueryParam("float") Set<Float> v) {
            assertTrue(v.contains(3.14159265f));
            return "content";
        }

        @GET
        @Produces("application/double")
        public String doGetDouble(@QueryParam("double") Set<Double> v) {
            assertTrue(v.contains(3.14159265358979d));
            return "content";
        }
    }

    @Path("/Set/default/null")
    public static class ResourceQueryPrimitiveSetDefaultEmpty {

        @GET
        @Produces("application/boolean")
        public String doGetBoolean(@QueryParam("boolean") Set<Boolean> v) {
            assertEquals(0, v.size());
            return "content";
        }

        @GET
        @Produces("application/byte")
        public String doGetByte(@QueryParam("byte") Set<Byte> v) {
            assertEquals(0, v.size());
            return "content";
        }

        @GET
        @Produces("application/short")
        public String doGetShort(@QueryParam("short") Set<Short> v) {
            assertEquals(0, v.size());
            return "content";
        }

        @GET
        @Produces("application/int")
        public String doGetInteger(@QueryParam("int") Set<Integer> v) {
            assertEquals(0, v.size());
            return "content";
        }

        @GET
        @Produces("application/long")
        public String doGetLong(@QueryParam("long") Set<Long> v) {
            assertEquals(0, v.size());
            return "content";
        }

        @GET
        @Produces("application/float")
        public String doGetFloat(@QueryParam("float") Set<Float> v) {
            assertEquals(0, v.size());
            return "content";
        }

        @GET
        @Produces("application/double")
        public String doGetDouble(@QueryParam("double") Set<Double> v) {
            assertEquals(0, v.size());
            return "content";
        }
    }

    @Path("/Set/default")
    public static class ResourceQueryPrimitiveSetDefault {

        @GET
        @Produces("application/boolean")
        public String doGetBoolean(@QueryParam("boolean") @DefaultValue("true") Set<Boolean> v) {
            assertTrue(v.contains(true));
            return "content";
        }

        @GET
        @Produces("application/byte")
        public String doGetByte(@QueryParam("byte") @DefaultValue("127") Set<Byte> v) {
            assertTrue(v.contains((byte) 127));
            return "content";
        }

        @GET
        @Produces("application/short")
        public String doGetShort(@QueryParam("short") @DefaultValue("32767") Set<Short> v) {
            assertTrue(v.contains((short) 32767));
            return "content";
        }

        @GET
        @Produces("application/int")
        public String doGetInteger(@QueryParam("int") @DefaultValue("2147483647") Set<Integer> v) {
            assertTrue(v.contains(2147483647));
            return "content";
        }

        @GET
        @Produces("application/long")
        public String doGetLong(@QueryParam("long") @DefaultValue("9223372036854775807") Set<Long> v) {
            assertTrue(v.contains(9223372036854775807L));
            return "content";
        }

        @GET
        @Produces("application/float")
        public String doGetFloat(@QueryParam("float") @DefaultValue("3.14159265") Set<Float> v) {
            assertTrue(v.contains(3.14159265f));
            return "content";
        }

        @GET
        @Produces("application/double")
        public String doGetDouble(@QueryParam("double") @DefaultValue("3.14159265358979") Set<Double> v) {
            assertTrue(v.contains(3.14159265358979d));
            return "content";
        }
    }

    @Path("/Set/default/override")
    public static class ResourceQueryPrimitiveSetDefaultOverride {

        @GET
        @Produces("application/boolean")
        public String doGetBoolean(@QueryParam("boolean") @DefaultValue("false") Set<Boolean> v) {
            assertTrue(v.contains(true));
            return "content";
        }

        @GET
        @Produces("application/byte")
        public String doGetByte(@QueryParam("byte") @DefaultValue("0") Set<Byte> v) {
            assertTrue(v.contains((byte) 127));
            return "content";
        }

        @GET
        @Produces("application/short")
        public String doGetShort(@QueryParam("short") @DefaultValue("0") Set<Short> v) {
            assertTrue(v.contains((short) 32767));
            return "content";
        }

        @GET
        @Produces("application/int")
        public String doGetInteger(@QueryParam("int") @DefaultValue("0") Set<Integer> v) {
            assertTrue(v.contains(2147483647));
            return "content";
        }

        @GET
        @Produces("application/long")
        public String doGetLong(@QueryParam("long") @DefaultValue("0") Set<Long> v) {
            assertTrue(v.contains(9223372036854775807L));
            return "content";
        }

        @GET
        @Produces("application/float")
        public String doGetFloat(@QueryParam("float") @DefaultValue("0.0") Set<Float> v) {
            assertTrue(v.contains(3.14159265f));
            return "content";
        }

        @GET
        @Produces("application/double")
        public String doGetDouble(@QueryParam("double") @DefaultValue("0.0") Set<Double> v) {
            assertTrue(v.contains(3.14159265358979d));
            return "content";
        }
    }

    void _test(String type, String value) throws ExecutionException, InterruptedException {
        String param = type + "=" + value;

        super.getResponseContext("/Set?" + param + "&" + param + "&" + param, "application/" + type);
    }

    void _testDefault(String base, String type, String value) throws ExecutionException, InterruptedException {
        super.getResponseContext(base + "default/null", "application/" + type);

        super.getResponseContext(base + "default", "application/" + type);

        String param = type + "=" + value;
        super.getResponseContext(base + "default/override?" + param, "application/" + type);
    }

    void _testSetDefault(String type, String value) throws ExecutionException, InterruptedException {
        _testDefault("/Set/", type, value);
    }

    @Test
    public void testGetBoolean() throws ExecutionException, InterruptedException {
        _test("boolean", "true");
    }

    @Test
    public void testGetBooleanPrimitiveSetDefault() throws ExecutionException, InterruptedException {
        _testSetDefault("boolean", "true");
    }

    @Test
    public void testGetByte() throws ExecutionException, InterruptedException {
        _test("byte", "127");
    }

    @Test
    public void testGetBytePrimitiveSetDefault() throws ExecutionException, InterruptedException {
        _testSetDefault("byte", "127");
    }

    @Test
    public void testGetShort() throws ExecutionException, InterruptedException {
        _test("short", "32767");
    }

    @Test
    public void testGetShortPrimtiveSetDefault() throws ExecutionException, InterruptedException {
        _testSetDefault("short", "32767");
    }

    @Test
    public void testGetInt() throws ExecutionException, InterruptedException {
        _test("int", "2147483647");
    }

    @Test
    public void testGetIntPrimitiveSetDefault() throws ExecutionException, InterruptedException {
        _testSetDefault("int", "2147483647");
    }

    @Test
    public void testGetLong() throws ExecutionException, InterruptedException {
        _test("long", "9223372036854775807");
    }

    @Test
    public void testGetLongPrimitiveSetDefault() throws ExecutionException, InterruptedException {
        _testSetDefault("long", "9223372036854775807");
    }

    @Test
    public void testGetFloat() throws ExecutionException, InterruptedException {
        _test("float", "3.14159265");
    }

    @Test
    public void testGetFloatPrimitiveSetDefault() throws ExecutionException, InterruptedException {
        _testSetDefault("float", "3.14159265");
    }

    @Test
    public void testGetDouble() throws ExecutionException, InterruptedException {
        _test("double", "3.14159265358979");
    }

    @Test
    public void testGetDoublePrimitiveSetDefault() throws ExecutionException, InterruptedException {
        _testSetDefault("double", "3.14159265358979");
    }

    @Test
    public void testBadPrimitiveSetValue() throws ExecutionException, InterruptedException {
        final ContainerResponse response = super.getResponseContext("/Set?int=abcdef&int=abcdef", "application/int");

        assertEquals(404, response.getStatus());
    }
}
