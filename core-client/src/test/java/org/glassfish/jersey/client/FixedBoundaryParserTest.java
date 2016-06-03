/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Tests several parsing use-cases of ChunkedInput
 *
 * @author Petr Bouda (petr.bouda at oracle.com)
 **/
public class FixedBoundaryParserTest {

    public static final String DELIMITER_4 = "1234";

    public static final String DELIMITER_1 = "#";

    @Test
    public void testFixedBoundaryParserNullInput() throws IOException {
        final ChunkParser parser = ChunkedInput.createParser(DELIMITER_4);
        InputStream input = new ByteArrayInputStream(new byte[] {});
        assertNull(parser.readChunk(input));
    }

    @Test
    public void testFixedBoundaryParserDelimiter4() throws IOException {
        final ChunkParser parser = ChunkedInput.createParser(DELIMITER_4);

        // delimiter is the same char sequence as an input
        assertNull(parse(parser, DELIMITER_4));

        // input starts with the delimiter
        assertEquals("123", parse(parser, DELIMITER_4 + "123"));

        // beginning of the input and delimiter are not the same
        assertEquals("abc", parse(parser, "abc" + DELIMITER_4 + "def"));

        // delimiter in the input is not complete, only partial
        assertEquals("abc123", parse(parser, "abc123"));

        // delimiter in the input is not complete, only partial,
        // and then continue with a char which is not part of the
        // delimiter
        assertEquals("abc1235", parse(parser, "abc1235"));

        // delimiter in the input is not complete, only partial,
        // and then continue with a char which is part of the
        // delimiter
        assertEquals("abc1231", parse(parser, "abc1231"));

        // input has the same beginning as the delimiter
        assertEquals("12", parse(parser, "121234"));

        // input ends with first char of delimiter
        assertEquals("1231", parse(parser, "1231"));
    }

    @Test
    public void testFixedBoundaryParserDelimiter1() throws IOException {
        final ChunkParser parser = ChunkedInput.createParser(DELIMITER_1);

        // delimiter is the same char sequence as an input
        assertNull(parse(parser, DELIMITER_1));

        // input starts with the delimiter
        assertEquals("123", parse(parser, DELIMITER_1 + "123"));

        // beginning of the input and delimiter are not the same
        assertEquals("abc", parse(parser, "abc" + DELIMITER_1 + "def"));

        // delimiter in the input is not complete, only partial
        assertEquals("abc123", parse(parser, "abc123"));
    }

    @Test
    public void delimiterWithRepeatedInitialCharacters() throws IOException {
        ChunkParser parser = ChunkedInput.createParser("**b**");
        assertEquals("1*", parse(parser, "1***b**"));
    }

    private static String parse(ChunkParser parser, String str) throws IOException {
        InputStream input = new ByteArrayInputStream(str.getBytes());
        byte[] bytes = parser.readChunk(input);
        return bytes == null ? null : new String(bytes);
    }

    @Test
    public void testFixedBoundaryParserFlow() throws IOException {
        final ChunkParser parser = ChunkedInput.createParser(DELIMITER_4);

        String input = "abc" + DELIMITER_4 + "edf" + DELIMITER_4 + "ghi";
        InputStream stream = new ByteArrayInputStream(input.getBytes());

        byte[] bytes = parser.readChunk(stream);
        assertEquals("abc", new String(bytes));

        bytes = parser.readChunk(stream);
        assertEquals("edf", new String(bytes));

        bytes = parser.readChunk(stream);
        assertEquals("ghi", new String(bytes));
    }

    @Test
    public void testFixedBoundaryParserFlowDelimiterFirst() throws IOException {
        final ChunkParser parser = ChunkedInput.createParser(DELIMITER_4);

        String input = DELIMITER_4 + "edf" + DELIMITER_4 + "ghi";
        InputStream stream = new ByteArrayInputStream(input.getBytes());

        byte[] bytes = parser.readChunk(stream);
        assertEquals("edf", new String(bytes));

        bytes = parser.readChunk(stream);
        assertEquals("ghi", new String(bytes));
    }

    @Test
    public void testFixedBoundaryParserFlowDelimiterEnds() throws IOException {
        final ChunkParser parser = ChunkedInput.createParser(DELIMITER_4);

        String input = "abc" + DELIMITER_4 + "edf" + DELIMITER_4;
        InputStream stream = new ByteArrayInputStream(input.getBytes());

        byte[] bytes = parser.readChunk(stream);
        assertEquals("abc", new String(bytes));

        bytes = parser.readChunk(stream);
        assertEquals("edf", new String(bytes));
    }

}
