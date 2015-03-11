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

import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.RequestContextBuilder;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 *
 * @author Paul Sandoz
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class HeaderParamAsPrimitiveTest extends AbstractTest {

    public HeaderParamAsPrimitiveTest() {
        initiateWebApplication(
                ResourceHeaderPrimitives.class,
                ResourceHeaderPrimitivesDefaultNull.class,
                ResourceHeaderPrimitivesDefault.class,
                ResourceHeaderPrimitivesDefaultOverride.class,
                ResourceHeaderPrimitiveWrappers.class,
                ResourceHeaderPrimitiveWrappersDefaultNull.class,
                ResourceHeaderPrimitiveWrappersDefault.class,
                ResourceHeaderPrimitiveWrappersDefaultOverride.class,
                ResourceHeaderPrimitiveList.class,
                ResourceHeaderPrimitiveListDefaultEmpty.class,
                ResourceHeaderPrimitiveListDefault.class,
                ResourceHeaderPrimitiveListDefaultOverride.class
        );
    }

    @Path("/")
    public static class ResourceHeaderPrimitives {

        @GET
        @Produces("application/boolean")
        public String doGet(@HeaderParam("boolean") boolean v) {
            assertEquals(true, v);
            return "content";
        }

        @GET
        @Produces("application/byte")
        public String doGet(@HeaderParam("byte") byte v) {
            assertEquals(127, v);
            return "content";
        }

        @GET
        @Produces("application/char")
        public String doGet(@HeaderParam("char") char v) {
            assertEquals('c', v);
            return "content";
        }

        @GET
        @Produces("application/short")
        public String doGet(@HeaderParam("short") short v) {
            assertEquals(32767, v);
            return "content";
        }

        @GET
        @Produces("application/int")
        public String doGet(@HeaderParam("int") int v) {
            assertEquals(2147483647, v);
            return "content";
        }

        @GET
        @Produces("application/long")
        public String doGet(@HeaderParam("long") long v) {
            assertEquals(9223372036854775807L, v);
            return "content";
        }

        @GET
        @Produces("application/float")
        public String doGet(@HeaderParam("float") float v) {
            assertEquals(3.14159265f, v, 0);
            return "content";
        }

        @GET
        @Produces("application/double")
        public String doGet(@HeaderParam("double") double v) {
            assertEquals(3.14159265358979d, v, 0);
            return "content";
        }
    }

    @Path("/default/null")
    public static class ResourceHeaderPrimitivesDefaultNull {

        @GET
        @Produces("application/boolean")
        public String doGet(@HeaderParam("boolean") boolean v) {
            assertEquals(false, v);
            return "content";
        }

        @GET
        @Produces("application/byte")
        public String doGet(@HeaderParam("byte") byte v) {
            assertEquals(0, v);
            return "content";
        }

        @GET
        @Produces("application/char")
        public String doGet(@HeaderParam("char") char v) {
            assertEquals(0x00, v);
            return "content";
        }

        @GET
        @Produces("application/short")
        public String doGet(@HeaderParam("short") short v) {
            assertEquals(0, v);
            return "content";
        }

        @GET
        @Produces("application/int")
        public String doGet(@HeaderParam("int") int v) {
            assertEquals(0, v);
            return "content";
        }

        @GET
        @Produces("application/long")
        public String doGet(@HeaderParam("long") long v) {
            assertEquals(0L, v);
            return "content";
        }

        @GET
        @Produces("application/float")
        public String doGet(@HeaderParam("float") float v) {
            assertEquals(0.0f, v, 0);
            return "content";
        }

        @GET
        @Produces("application/double")
        public String doGet(@HeaderParam("double") double v) {
            assertEquals(0.0d, v, 0);
            return "content";
        }
    }

    @Path("/default")
    public static class ResourceHeaderPrimitivesDefault {

        @GET
        @Produces("application/boolean")
        public String doGet(@HeaderParam("boolean") @DefaultValue("true") boolean v) {
            assertEquals(true, v);
            return "content";
        }

        @GET
        @Produces("application/byte")
        public String doGet(@HeaderParam("byte") @DefaultValue("127") byte v) {
            assertEquals(127, v);
            return "content";
        }

        @GET
        @Produces("application/char")
        public String doGet(@HeaderParam("char") @DefaultValue("c") char v) {
            assertEquals('c', v);
            return "content";
        }

        @GET
        @Produces("application/short")
        public String doGet(@HeaderParam("short") @DefaultValue("32767") short v) {
            assertEquals(32767, v);
            return "content";
        }

        @GET
        @Produces("application/int")
        public String doGet(@HeaderParam("int") @DefaultValue("2147483647") int v) {
            assertEquals(2147483647, v);
            return "content";
        }

        @GET
        @Produces("application/long")
        public String doGet(@HeaderParam("long") @DefaultValue("9223372036854775807") long v) {
            assertEquals(9223372036854775807L, v);
            return "content";
        }

        @GET
        @Produces("application/float")
        public String doGet(@HeaderParam("float") @DefaultValue("3.14159265") float v) {
            assertEquals(3.14159265f, v, 0);
            return "content";
        }

        @GET
        @Produces("application/double")
        public String doGet(@HeaderParam("double") @DefaultValue("3.14159265358979") double v) {
            assertEquals(3.14159265358979d, v, 0);
            return "content";
        }
    }

    @Path("/default/override")
    public static class ResourceHeaderPrimitivesDefaultOverride {

        @GET
        @Produces("application/boolean")
        public String doGet(@HeaderParam("boolean") @DefaultValue("false") boolean v) {
            assertEquals(true, v);
            return "content";
        }

        @GET
        @Produces("application/byte")
        public String doGet(@HeaderParam("byte") @DefaultValue("1") byte v) {
            assertEquals(127, v);
            return "content";
        }

        @GET
        @Produces("application/char")
        public String doGet(@HeaderParam("char") @DefaultValue("1") char v) {
            assertEquals('c', v);
            return "content";
        }

        @GET
        @Produces("application/short")
        public String doGet(@HeaderParam("short") @DefaultValue("1") short v) {
            assertEquals(32767, v);
            return "content";
        }

        @GET
        @Produces("application/int")
        public String doGet(@HeaderParam("int") @DefaultValue("1") int v) {
            assertEquals(2147483647, v);
            return "content";
        }

        @GET
        @Produces("application/long")
        public String doGet(@HeaderParam("long") @DefaultValue("1") long v) {
            assertEquals(9223372036854775807L, v);
            return "content";
        }

        @GET
        @Produces("application/float")
        public String doGet(@HeaderParam("float") @DefaultValue("0.0") float v) {
            assertEquals(3.14159265f, v, 0);
            return "content";
        }

        @GET
        @Produces("application/double")
        public String doGet(@HeaderParam("double") @DefaultValue("0.0") double v) {
            assertEquals(3.14159265358979d, v, 0);
            return "content";
        }
    }

    @Path("/wrappers")
    public static class ResourceHeaderPrimitiveWrappers {

        @GET
        @Produces("application/boolean")
        public String doGet(@HeaderParam("boolean") Boolean v) {
            assertEquals(true, v);
            return "content";
        }

        @GET
        @Produces("application/byte")
        public String doGet(@HeaderParam("byte") Byte v) {
            assertEquals(127, v.byteValue());
            return "content";
        }

        @GET
        @Produces("application/char")
        public String doGet(@HeaderParam("char") Character v) {
            assertEquals('c', v.charValue());
            return "content";
        }

        @GET
        @Produces("application/short")
        public String doGet(@HeaderParam("short") Short v) {
            assertEquals(32767, v.shortValue());
            return "content";
        }

        @GET
        @Produces("application/int")
        public String doGet(@HeaderParam("int") Integer v) {
            assertEquals(2147483647, v.intValue());
            return "content";
        }

        @GET
        @Produces("application/long")
        public String doGet(@HeaderParam("long") Long v) {
            assertEquals(9223372036854775807L, v.longValue());
            return "content";
        }

        @GET
        @Produces("application/float")
        public String doGet(@HeaderParam("float") Float v) {
            assertEquals(3.14159265f, v, 0);
            return "content";
        }

        @GET
        @Produces("application/double")
        public String doGet(@HeaderParam("double") Double v) {
            assertEquals(3.14159265358979d, v, 0);
            return "content";
        }
    }

    @Path("/wrappers/default/null")
    public static class ResourceHeaderPrimitiveWrappersDefaultNull {

        @GET
        @Produces("application/boolean")
        public String doGet(@HeaderParam("boolean") Boolean v) {
            assertEquals(null, v);
            return "content";
        }

        @GET
        @Produces("application/byte")
        public String doGet(@HeaderParam("byte") Byte v) {
            assertEquals(null, v);
            return "content";
        }

        @GET
        @Produces("application/char")
        public String doGet(@HeaderParam("char") Character v) {
            assertEquals(null, v);
            return "content";
        }

        @GET
        @Produces("application/short")
        public String doGet(@HeaderParam("short") Short v) {
            assertEquals(null, v);
            return "content";
        }

        @GET
        @Produces("application/int")
        public String doGet(@HeaderParam("int") Integer v) {
            assertEquals(null, v);
            return "content";
        }

        @GET
        @Produces("application/long")
        public String doGet(@HeaderParam("long") Long v) {
            assertEquals(null, v);
            return "content";
        }

        @GET
        @Produces("application/float")
        public String doGet(@HeaderParam("float") Float v) {
            assertEquals(null, v);
            return "content";
        }

        @GET
        @Produces("application/double")
        public String doGet(@HeaderParam("double") Double v) {
            assertEquals(null, v);
            return "content";
        }
    }

    @Path("/wrappers/default")
    public static class ResourceHeaderPrimitiveWrappersDefault {

        @GET
        @Produces("application/boolean")
        public String doGet(@HeaderParam("boolean") @DefaultValue("true") Boolean v) {
            assertEquals(true, v);
            return "content";
        }

        @GET
        @Produces("application/byte")
        public String doGet(@HeaderParam("byte") @DefaultValue("127") Byte v) {
            assertEquals(127, v.byteValue());
            return "content";
        }

        @GET
        @Produces("application/char")
        public String doGet(@HeaderParam("char") @DefaultValue("d") Character v) {
            assertEquals('d', v.charValue());
            return "content";
        }

        @GET
        @Produces("application/short")
        public String doGet(@HeaderParam("short") @DefaultValue("32767") Short v) {
            assertEquals(32767, v.shortValue());
            return "content";
        }

        @GET
        @Produces("application/int")
        public String doGet(@HeaderParam("int") @DefaultValue("2147483647") Integer v) {
            assertEquals(2147483647, v.intValue());
            return "content";
        }

        @GET
        @Produces("application/long")
        public String doGet(@HeaderParam("long") @DefaultValue("9223372036854775807") Long v) {
            assertEquals(9223372036854775807L, v.longValue());
            return "content";
        }

        @GET
        @Produces("application/float")
        public String doGet(@HeaderParam("float") @DefaultValue("3.14159265") Float v) {
            assertEquals(3.14159265f, v, 0);
            return "content";
        }

        @GET
        @Produces("application/double")
        public String doGet(@HeaderParam("double") @DefaultValue("3.14159265358979") Double v) {
            assertEquals(3.14159265358979d, v, 0);
            return "content";
        }
    }

    @Path("/wrappers/default/override")
    public static class ResourceHeaderPrimitiveWrappersDefaultOverride {

        @GET
        @Produces("application/boolean")
        public String doGet(@HeaderParam("boolean") @DefaultValue("false") Boolean v) {
            assertEquals(true, v);
            return "content";
        }

        @GET
        @Produces("application/byte")
        public String doGet(@HeaderParam("byte") @DefaultValue("1") Byte v) {
            assertEquals(127, v.byteValue());
            return "content";
        }

        @GET
        @Produces("application/char")
        public String doGet(@HeaderParam("char") @DefaultValue("d") Character v) {
            assertEquals('c', v.charValue());
            return "content";
        }

        @GET
        @Produces("application/short")
        public String doGet(@HeaderParam("short") @DefaultValue("1") Short v) {
            assertEquals(32767, v.shortValue());
            return "content";
        }

        @GET
        @Produces("application/int")
        public String doGet(@HeaderParam("int") @DefaultValue("1") Integer v) {
            assertEquals(2147483647, v.intValue());
            return "content";
        }

        @GET
        @Produces("application/long")
        public String doGet(@HeaderParam("long") @DefaultValue("1") Long v) {
            assertEquals(9223372036854775807L, v.longValue());
            return "content";
        }

        @GET
        @Produces("application/float")
        public String doGet(@HeaderParam("float") @DefaultValue("0.0") Float v) {
            assertEquals(3.14159265f, v, 0);
            return "content";
        }

        @GET
        @Produces("application/double")
        public String doGet(@HeaderParam("double") @DefaultValue("0.0") Double v) {
            assertEquals(3.14159265358979d, v, 0);
            return "content";
        }
    }

    @Path("/list")
    public static class ResourceHeaderPrimitiveList {

        @GET
        @Produces("application/boolean")
        public String doGetBoolean(@HeaderParam("boolean") List<Boolean> v) {
            assertEquals(true, v.get(0));
            assertEquals(true, v.get(1));
            assertEquals(true, v.get(2));
            return "content";
        }

        @GET
        @Produces("application/byte")
        public String doGetByte(@HeaderParam("byte") List<Byte> v) {
            assertEquals(127, v.get(0).byteValue());
            assertEquals(127, v.get(1).byteValue());
            assertEquals(127, v.get(2).byteValue());
            return "content";
        }

        @GET
        @Produces("application/char")
        public String doGetCharacter(@HeaderParam("char") List<Character> v) {
            assertEquals('c', v.get(0).charValue());
            assertEquals('c', v.get(1).charValue());
            assertEquals('c', v.get(2).charValue());
            return "content";
        }

        @GET
        @Produces("application/short")
        public String doGetShort(@HeaderParam("short") List<Short> v) {
            assertEquals(32767, v.get(0).shortValue());
            assertEquals(32767, v.get(1).shortValue());
            assertEquals(32767, v.get(2).shortValue());
            return "content";
        }

        @GET
        @Produces("application/int")
        public String doGetInteger(@HeaderParam("int") List<Integer> v) {
            assertEquals(2147483647, v.get(0).intValue());
            assertEquals(2147483647, v.get(1).intValue());
            assertEquals(2147483647, v.get(2).intValue());
            return "content";
        }

        @GET
        @Produces("application/long")
        public String doGetLong(@HeaderParam("long") List<Long> v) {
            assertEquals(9223372036854775807L, v.get(0).longValue());
            assertEquals(9223372036854775807L, v.get(1).longValue());
            assertEquals(9223372036854775807L, v.get(2).longValue());
            return "content";
        }

        @GET
        @Produces("application/float")
        public String doGetFloat(@HeaderParam("float") List<Float> v) {
            assertEquals(3.14159265f, v.get(0), 0);
            assertEquals(3.14159265f, v.get(1), 0);
            assertEquals(3.14159265f, v.get(2), 0);
            return "content";
        }

        @GET
        @Produces("application/double")
        public String doGetDouble(@HeaderParam("double") List<Double> v) {
            assertEquals(3.14159265358979d, v.get(0), 0);
            assertEquals(3.14159265358979d, v.get(1), 0);
            assertEquals(3.14159265358979d, v.get(2), 0);
            return "content";
        }
    }

    @Path("/list/default/null")
    public static class ResourceHeaderPrimitiveListDefaultEmpty {

        @GET
        @Produces("application/boolean")
        public String doGetBoolean(@HeaderParam("boolean") List<Boolean> v) {
            assertEquals(0, v.size());
            return "content";
        }

        @GET
        @Produces("application/byte")
        public String doGetByte(@HeaderParam("byte") List<Byte> v) {
            assertEquals(0, v.size());
            return "content";
        }

        @GET
        @Produces("application/char")
        public String doGetChararcter(@HeaderParam("char") List<Character> v) {
            assertEquals(0, v.size());
            return "content";
        }

        @GET
        @Produces("application/short")
        public String doGetShort(@HeaderParam("short") List<Short> v) {
            assertEquals(0, v.size());
            return "content";
        }

        @GET
        @Produces("application/int")
        public String doGetInteger(@HeaderParam("int") List<Integer> v) {
            assertEquals(0, v.size());
            return "content";
        }

        @GET
        @Produces("application/long")
        public String doGetLong(@HeaderParam("long") List<Long> v) {
            assertEquals(0, v.size());
            return "content";
        }

        @GET
        @Produces("application/float")
        public String doGetFloat(@HeaderParam("float") List<Float> v) {
            assertEquals(0, v.size());
            return "content";
        }

        @GET
        @Produces("application/double")
        public String doGetDouble(@HeaderParam("double") List<Double> v) {
            assertEquals(0, v.size());
            return "content";
        }
    }

    @Path("/list/default")
    public static class ResourceHeaderPrimitiveListDefault {

        @GET
        @Produces("application/boolean")
        public String doGetBoolean(@HeaderParam("boolean") @DefaultValue("true") List<Boolean> v) {
            assertEquals(true, v.get(0));
            return "content";
        }

        @GET
        @Produces("application/byte")
        public String doGetByte(@HeaderParam("byte") @DefaultValue("127") List<Byte> v) {
            assertEquals(127, v.get(0).byteValue());
            return "content";
        }

        @GET
        @Produces("application/char")
        public String doGetCharacter(@HeaderParam("char") @DefaultValue("d") List<Character> v) {
            assertEquals('d', v.get(0).charValue());
            return "content";
        }

        @GET
        @Produces("application/short")
        public String doGetShort(@HeaderParam("short") @DefaultValue("32767") List<Short> v) {
            assertEquals(32767, v.get(0).shortValue());
            return "content";
        }

        @GET
        @Produces("application/int")
        public String doGetInteger(@HeaderParam("int") @DefaultValue("2147483647") List<Integer> v) {
            assertEquals(2147483647, v.get(0).intValue());
            return "content";
        }

        @GET
        @Produces("application/long")
        public String doGetLong(@HeaderParam("long") @DefaultValue("9223372036854775807") List<Long> v) {
            assertEquals(9223372036854775807L, v.get(0).longValue());
            return "content";
        }

        @GET
        @Produces("application/float")
        public String doGetFloat(@HeaderParam("float") @DefaultValue("3.14159265") List<Float> v) {
            assertEquals(3.14159265f, v.get(0), 0);
            return "content";
        }

        @GET
        @Produces("application/double")
        public String doGetDouble(@HeaderParam("double") @DefaultValue("3.14159265358979") List<Double> v) {
            assertEquals(3.14159265358979d, v.get(0), 0);
            return "content";
        }
    }

    @Path("/list/default/override")
    public static class ResourceHeaderPrimitiveListDefaultOverride {

        @GET
        @Produces("application/boolean")
        public String doGetBoolean(@HeaderParam("boolean") @DefaultValue("false") List<Boolean> v) {
            assertEquals(true, v.get(0));
            return "content";
        }

        @GET
        @Produces("application/byte")
        public String doGetByte(@HeaderParam("byte") @DefaultValue("0") List<Byte> v) {
            assertEquals(127, v.get(0).byteValue());
            return "content";
        }

        @GET
        @Produces("application/char")
        public String doGetCharacter(@HeaderParam("char") @DefaultValue("d") List<Character> v) {
            assertEquals('c', v.get(0).charValue());
            return "content";
        }

        @GET
        @Produces("application/short")
        public String doGetShort(@HeaderParam("short") @DefaultValue("0") List<Short> v) {
            assertEquals(32767, v.get(0).shortValue());
            return "content";
        }

        @GET
        @Produces("application/int")
        public String doGetInteger(@HeaderParam("int") @DefaultValue("0") List<Integer> v) {
            assertEquals(2147483647, v.get(0).intValue());
            return "content";
        }

        @GET
        @Produces("application/long")
        public String doGetLong(@HeaderParam("long") @DefaultValue("0") List<Long> v) {
            assertEquals(9223372036854775807L, v.get(0).longValue());
            return "content";
        }

        @GET
        @Produces("application/float")
        public String doGetFloat(@HeaderParam("float") @DefaultValue("0.0") List<Float> v) {
            assertEquals(3.14159265f, v.get(0), 0);
            return "content";
        }

        @GET
        @Produces("application/double")
        public String doGetDouble(@HeaderParam("double") @DefaultValue("0.0") List<Double> v) {
            assertEquals(3.14159265358979d, v.get(0), 0);
            return "content";
        }
    }

    void _test(String type, String value) throws ExecutionException, InterruptedException {
        assertEquals("content", apply(
                RequestContextBuilder.from("/", "GET")
                        .accept("application/" + type)
                        .header(type, value).build()
        ).getEntity());

        assertEquals("content", apply(
                RequestContextBuilder.from("/wrappers", "GET").accept("application/" + type).header(type, value).build()
        ).getEntity());

        assertEquals("content", apply(
                RequestContextBuilder.from("/list", "GET").accept("application/" + type).header(type, value).header(type, value)
                        .header(type, value).build()
        ).getEntity());
    }

    void _testDefault(String base, String type, String value) throws ExecutionException, InterruptedException {
        assertEquals("content", apply(
                RequestContextBuilder.from(base + "default/null", "GET").accept("application/" + type).build()
        ).getEntity());

        assertEquals("content", apply(
                RequestContextBuilder.from(base + "default", "GET").accept("application/" + type).build()
        ).getEntity());

        assertEquals("content", apply(
                RequestContextBuilder.from(base + "default/override", "GET").accept("application/" + type).header(type, value)
                        .build()
        ).getEntity());
    }

    void _testDefault(String type, String value) throws ExecutionException, InterruptedException {
        _testDefault("/", type, value);
    }

    void _testWrappersDefault(String type, String value) throws ExecutionException, InterruptedException {
        _testDefault("/wrappers/", type, value);
    }

    void _testListDefault(String type, String value) throws ExecutionException, InterruptedException {
        _testDefault("/list/", type, value);
    }

    @Test
    public void testGetBoolean() throws ExecutionException, InterruptedException {
        _test("boolean", "true");
    }

    @Test
    public void testGetBooleanPrimitivesDefault() throws ExecutionException, InterruptedException {
        _testDefault("boolean", "true");
    }

    @Test
    public void testGetBooleanPrimitiveWrapperDefault() throws ExecutionException, InterruptedException {
        _testWrappersDefault("boolean", "true");
    }

    @Test
    public void testGetBooleanPrimitiveListDefault() throws ExecutionException, InterruptedException {
        _testListDefault("boolean", "true");
    }

    @Test
    public void testGetByte() throws ExecutionException, InterruptedException {
        _test("byte", "127");
    }

    @Test
    public void testGetBytePrimitivesDefault() throws ExecutionException, InterruptedException {
        _testDefault("byte", "127");
    }

    @Test
    public void testGetBytePrimitiveWrappersDefault() throws ExecutionException, InterruptedException {
        _testWrappersDefault("byte", "127");
    }

    @Test
    public void testGetBytePrimitiveListDefault() throws ExecutionException, InterruptedException {
        _testListDefault("byte", "127");
    }

    @Test
    public void testGetCharacter() throws ExecutionException, InterruptedException {
        _test("char", "c");
    }

    @Test
    public void testGetCharacterPrimitivesDefault() throws ExecutionException, InterruptedException {
        _testDefault("char", "c");
    }

    @Test
    public void testGetCharacterPrimitiveWrappersDefault() throws ExecutionException, InterruptedException {
        _testWrappersDefault("char", "c");
    }

    @Test
    public void testGetCharacterPrimitiveListDefault() throws ExecutionException, InterruptedException {
        _testListDefault("char", "c");
    }

    @Test
    public void testGetShort() throws ExecutionException, InterruptedException {
        _test("short", "32767");
    }

    @Test
    public void testGetShortPrimtivesDefault() throws ExecutionException, InterruptedException {
        _testDefault("short", "32767");
    }

    @Test
    public void testGetShortPrimtiveWrappersDefault() throws ExecutionException, InterruptedException {
        _testWrappersDefault("short", "32767");
    }

    @Test
    public void testGetShortPrimtiveListDefault() throws ExecutionException, InterruptedException {
        _testListDefault("short", "32767");
    }

    @Test
    public void testGetInt() throws ExecutionException, InterruptedException {
        _test("int", "2147483647");
    }

    @Test
    public void testGetIntPrimitivesDefault() throws ExecutionException, InterruptedException {
        _testDefault("int", "2147483647");
    }

    @Test
    public void testGetIntPrimitiveWrappersDefault() throws ExecutionException, InterruptedException {
        _testWrappersDefault("int", "2147483647");
    }

    @Test
    public void testGetIntPrimitiveListDefault() throws ExecutionException, InterruptedException {
        _testListDefault("int", "2147483647");
    }

    @Test
    public void testGetLong() throws ExecutionException, InterruptedException {
        _test("long", "9223372036854775807");
    }

    @Test
    public void testGetLongPrimitivesDefault() throws ExecutionException, InterruptedException {
        _testDefault("long", "9223372036854775807");
    }

    @Test
    public void testGetLongPrimitiveWrappersDefault() throws ExecutionException, InterruptedException {
        _testWrappersDefault("long", "9223372036854775807");
    }

    @Test
    public void testGetLongPrimitiveListDefault() throws ExecutionException, InterruptedException {
        _testListDefault("long", "9223372036854775807");
    }

    @Test
    public void testGetFloat() throws ExecutionException, InterruptedException {
        _test("float", "3.14159265");
    }

    @Test
    public void testGetFloatPrimitivesDefault() throws ExecutionException, InterruptedException {
        _testDefault("float", "3.14159265");
    }

    @Test
    public void testGetFloatPrimitiveWrappersDefault() throws ExecutionException, InterruptedException {
        _testWrappersDefault("float", "3.14159265");
    }

    @Test
    public void testGetFloatPrimitiveListDefault() throws ExecutionException, InterruptedException {
        _testListDefault("float", "3.14159265");
    }

    @Test
    public void testGetDouble() throws ExecutionException, InterruptedException {
        _test("double", "3.14159265358979");
    }

    @Test
    public void testGetDoublePrimitivesDefault() throws ExecutionException, InterruptedException {
        _testDefault("double", "3.14159265358979");
    }

    @Test
    public void testGetDoublePrimitiveWrappersDefault() throws ExecutionException, InterruptedException {
        _testWrappersDefault("double", "3.14159265358979");
    }

    @Test
    public void testGetDoublePrimitiveListDefault() throws ExecutionException, InterruptedException {
        _testListDefault("double", "3.14159265358979");
    }

    @Test
    public void testBadPrimitiveValue() throws ExecutionException, InterruptedException {
        final ContainerResponse responseContext = apply(
                RequestContextBuilder.from("/", "GET").accept("application/int").header("int", "abcdef").build()
        );

        assertEquals(400, responseContext.getStatus());
    }

    @Test
    public void testBadPrimitiveWrapperValue() throws ExecutionException, InterruptedException {
        final ContainerResponse responseContext = apply(
                RequestContextBuilder.from("/wrappers", "GET").accept("application/int").header("int", "abcdef").build()
        );

        assertEquals(400, responseContext.getStatus());
    }

    @Test
    public void testBadPrimitiveListValue() throws ExecutionException, InterruptedException {
        final ContainerResponse responseContext = apply(
                RequestContextBuilder.from("/", "GET").accept("application/int").header("int", "abcdef").header("int", "abcdef")
                        .header("int", "abcdef").build()
        );

        assertEquals(400, responseContext.getStatus());
    }
}
