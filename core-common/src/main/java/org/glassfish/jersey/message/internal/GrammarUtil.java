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
package org.glassfish.jersey.message.internal;

/**
 * Class containing static utility methods to work with HTTP headers.
 *
 * @author Paul Sandoz
 * @author Martin Matula (martin.matula at oracle.com)
 */
final class GrammarUtil {

    /** Represents token type in the TYPE_TABLE */
    public static final int TOKEN = 0;
    /** Represents quotes type in the TYPE_TABLE */
    public static final int QUOTED_STRING = 1;
    /** Represents comment type in the TYPE_TABLE */
    public static final int COMMENT = 2;
    /** Represents separator type in the TYPE_TABLE */
    public static final int SEPARATOR = 3;
    /** Represents control type in the TYPE_TABLE */
    public static final int CONTROL = 4;
    /** Array of chars representing white spaces */
    private static final char[] WHITE_SPACE = {'\t', '\r', '\n', ' '};
    /** Array of chars representing separators */
    private static final char[] SEPARATORS = {'(', ')', '<', '>', '@', ',', ';', ':', '\\', '"', '/', '[', ']', '?', '=', '{', '}', ' ', '\t'};
    /** Mapping of chars to types */
    private static final int[] TYPE_TABLE = createEventTable();
    /** Convenience table mapping chars to true if they are white space chars */
    private static final boolean[] IS_WHITE_SPACE = createWhiteSpaceTable();
    /** convenience table mapping chars to true if they are tokens */
    private static final boolean[] IS_TOKEN = createTokenTable();

    private static int[] createEventTable() {
        int[] table = new int[128];

        // Token
        for (int i = 0; i < 127; i++) {
            table[i] = TOKEN;
        }

        // Separator
        for (char c : SEPARATORS) {
            table[c] = SEPARATOR;
        }

        // Comment
        table['('] = COMMENT;

        // QuotedString
        table['"'] = QUOTED_STRING;

        // Control
        for (int i = 0; i < 32; i++) {
            table[i] = CONTROL;
        }
        table[127] = CONTROL;

        // White space
        for (char c : WHITE_SPACE) {
            table[c] = -1;
        }

        return table;
    }

    private static boolean[] createWhiteSpaceTable() {
        boolean[] table = new boolean[128];

        for (char c : WHITE_SPACE) {
            table[c] = true;
        }

        return table;
    }

    private static boolean[] createTokenTable() {
        boolean[] table = new boolean[128];

        for (int i = 0; i < 128; i++) {
            table[i] = (TYPE_TABLE[i] == TOKEN);
        }

        return table;
    }

    /**
     * Returns {@code true} if the provided char is a white space.
     *
     * @param c char to check.
     * @return {@code true} if c is a white space.
     */
    public static boolean isWhiteSpace(final char c) {
        return (c < 128 && IS_WHITE_SPACE[c]);
    }

    /**
     * Returns {@code true} if the provided char is a token.
     *
     * @param c char to check.
     * @return {@code true} if c is a token.
     */
    public static boolean isToken(final char c) {
        return (c < 128 && IS_TOKEN[c]);
    }

    /**
     * Get the character type.
     *
     * @param c char to check.
     * @return character type identifier.
     * @throws IllegalArgumentException in case the character value is greater than 127.
     */
    public static int getType(final char c) {
        if (c > 127) {
            throw new IllegalArgumentException("Unsupported character - ordinal value too high: " + c);
        }
        return TYPE_TABLE[c];
    }

    /**
     * Returns {@code true} if the provided char is a separator.
     *
     * @param c char to check.
     * @return {@code true} if c is a token.
     */
    public static boolean isSeparator(final char c) {
        return (c < 128 && TYPE_TABLE[c] == SEPARATOR);
    }

    /**
     * Returns {@code true} if all chars in string s are tokens.
     *
     * @param s string to check for tokens.
     * @return {@code true} if all chars in s are tokens.
     */
    public static boolean isTokenString(final String s) {
        for (char c : s.toCharArray()) {
            if (!isToken(c)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns {@code true} if string s contains a white space char.
     *
     * @param s string to check for white spaces.
     * @return {@code true} if s contains white spaces.
     */
    public static boolean containsWhiteSpace(final String s) {
        for (char c : s.toCharArray()) {
            if (isWhiteSpace(c)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Filter a substring of a string by removing any new-line characters and
     * un-escaping escaped characters.
     *
     * @param s string to use for substring token filtering.
     * @param start start filtering position in the string.
     * @param end end filtering position in the string.
     * @return filtered substring.
     */
    public static String filterToken(final String s, final int start, final int end) {
        StringBuilder sb = new StringBuilder();
        char c;
        boolean gotEscape = false;
        boolean gotCR = false;

        for (int i = start; i < end; i++) {
            c = s.charAt(i);
            if (c == '\n' && gotCR) {
                // This LF is part of an unescaped
                // CRLF sequence (i.e, LWSP). Skip it.
                gotCR = false;
                continue;
            }

            gotCR = false;
            if (!gotEscape) {
                // Previous character was NOT '\'
                if (c == '\\') { // skip this character
                    gotEscape = true;
                } else if (c == '\r') { // skip this character
                    gotCR = true;
                } else { // append this character
                    sb.append(c);
                }
            } else {
                // Previous character was '\'. So no need to
                // bother with any special processing, just
                // append this character
                sb.append(c);
                gotEscape = false;
            }
        }
        return sb.toString();
    }

    /**
     * Prevents instantiation.
     */
    private GrammarUtil() {
    }
}
