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

package org.glassfish.jersey.internal.util;

import java.util.Arrays;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 *
 * @author Martin Matula
 */
public class Base64Test {

    private static String[] decoded = new String[]{
            "any carnal pleasure.",
            "any carnal pleasure",
            "any carnal pleasur",
            "any carnal pleasu",
            "any carnal pleas"
    };

    private static String[] encoded = new String[]{
            "YW55IGNhcm5hbCBwbGVhc3VyZS4=",
            "YW55IGNhcm5hbCBwbGVhc3VyZQ==",
            "YW55IGNhcm5hbCBwbGVhc3Vy",
            "YW55IGNhcm5hbCBwbGVhc3U=",
            "YW55IGNhcm5hbCBwbGVhcw=="
    };

    @Test
    public void testEncodeString() throws Exception {
        for (int i = 0; i < decoded.length; i++) {
            assertEquals(encoded[i], new String(Base64.encode(decoded[i].getBytes("ASCII")), "ASCII"));
        }
    }

    @Test
    public void testDecodeString() throws Exception {
        for (int i = 0; i < encoded.length; i++) {
            assertEquals(decoded[i], new String(Base64.decode(encoded[i].getBytes("ASCII")), "ASCII"));
        }
    }

    @Test
    public void testRoundtripLengthMod3Equals0() {
        byte[] data = {0, 1, 2, 3, 4, 5, 6, 7, 8};
        byte[] result = Base64.decode(Base64.encode(data));
        assertTrue("failed to roundtrip value to base64", Arrays.equals(data, result));
    }

    @Test
    public void testRoundtripLengthMod3Equals1() {
        byte[] data = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        byte[] result = Base64.decode(Base64.encode(data));
        assertTrue("failed to roundtrip value to base64", Arrays.equals(data, result));
    }

    @Test
    public void testRoundtripLengthMod3Equals2() {
        byte[] data = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        byte[] result = Base64.decode(Base64.encode(data));
        assertTrue("failed to roundtrip value to base64", Arrays.equals(data, result));
    }

    @Test
    public void testRoundtripOneByteGreaterThan127() {
        byte[] data = {(byte) 128};
        try {
            byte[] result = Base64.decode(Base64.encode(data));
            fail();
        } catch (Exception e) {
            // ok
        }
    }

    @Test
    public void testRoundtripAssortedValues() {
        byte[] data = {0, 1, 63, 64, 65, (byte) 127, (byte) 128, (byte) 1299, (byte) 254, (byte) 255};
        try {
            byte[] result = Base64.decode(Base64.encode(data));
            fail();
        } catch (Exception e) {
            // ok
        }
    }

    @Test
    public void testEncodeByteArray() {
        String expectedResult = "//79/Pv6+fj39vX08/Lx8O/u7ezr6uno5+bl5OPi4eDf3t3c29rZ2NfW1dTT0tHQz87NzMvKycjHxsXEw8LBwL++vby7urm4t7a1tLOysbCvrq2sq6qpqKempaSjoqGgn56dnJuamZiXlpWUk5KRkI+OjYyLiomIh4aFhIOCgYB/fn18e3p5eHd2dXRzcnFwb25tbGtqaWhnZmVkY2JhYF9eXVxbWllYV1ZVVFNSUVBPTk1MS0pJSEdGRURDQkFAPz49PDs6OTg3NjU0MzIxMC8uLSwrKikoJyYlJCMiISAfHh0cGxoZGBcWFRQTEhEQDw4NDAsKCQgHBgUEAwIBAA==";
        byte[] data = new byte[256];
        for (int i = 0; i < 256; ++i) {
            data[i] = (byte) (255 - i);
        }
        try {
            String result = new String(Base64.encode(data));
            fail();
        } catch (Exception e) {
            // ok
        }
    }

    @Test
    public void testDecodeString2() {
        String data = "AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8gISIjJCUmJygpKissLS4vMDEyMzQ1Njc4OTo7PD0+P0BBQkNERUZHSElKS0xNTk9QUVJTVFVWV1hZWltcXV5fYGFiY2RlZmdoaWprbG1ub3BxcnN0dXZ3eHl6e3x9fn+AgYKDhIWGh4iJiouMjY6PkJGSk5SVlpeYmZqbnJ2en6ChoqOkpaanqKmqq6ytrq+wsbKztLW2t7i5uru8vb6/wMHCw8TFxsfIycrLzM3Oz9DR0tPU1dbX2Nna29zd3t/g4eLj5OXm5+jp6uvs7e7v8PHy8/T19vf4+fr7/P3+/w==";
        byte[] result = Base64.decode(data.getBytes());
        assertEquals("incorrect length", result.length, 256);
        for (int i = 0; i < 256; ++i) {
            assertEquals("incorrect value", result[i], (byte) i);
        }
    }
}
