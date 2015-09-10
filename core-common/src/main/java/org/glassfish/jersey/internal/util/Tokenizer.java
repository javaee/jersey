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

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * A utility class providing methods capable of splitting String entries
 * into an array of tokens based on either default or custom token delimiters.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public final class Tokenizer {
    private Tokenizer() {
        // prevents instantiation.
    }

    /**
     * Common Jersey delimiters used by various properties.
     */
    public static final String COMMON_DELIMITERS = " ,;\n";

    /**
     * Get a canonical array of tokens from an array of String entries
     * where each entry may contain zero or more tokens separated by
     * common delimiters {@code " ,;\n"}.
     *
     * @param entries an array where each String entry may contain zero or more
     *                {@link #COMMON_DELIMITERS common delimiters} separated tokens.
     * @return the array of tokens, each token is trimmed, the array will
     *         not contain any empty or {@code null} entries.
     */
    public static String[] tokenize(final String[] entries) {
        return tokenize(entries, COMMON_DELIMITERS);
    }

    /**
     * Get a canonical array of tokens from an array of String entries
     * where each entry may contain zero or more tokens separated by characters
     * in delimiters string.
     *
     * @param entries    an array where each String entry may contain zero or more
     *                   delimiters separated tokens.
     * @param delimiters string with delimiters, every character represents one
     *                   delimiter.
     * @return the array of tokens, each token is trimmed, the array will
     *         not contain any empty or {@code null} entries.
     */
    public static String[] tokenize(final String[] entries, final String delimiters) {
        final List<String> tokens = new LinkedList<String>();

        for (String entry : entries) {
            if (entry == null || entry.isEmpty()) {
                continue;
            }
            entry = entry.trim();
            if (entry.isEmpty()) {
                continue;
            }
            tokenize(entry, delimiters, tokens);
        }

        return tokens.toArray(new String[tokens.size()]);
    }

    /**
     * Get a canonical array of tokens from a String entry that may contain
     * zero or more tokens separated by common delimiters {@code " ,;\n"}.
     *
     * @param entry a String that may contain zero or more
     *              {@link #COMMON_DELIMITERS common delimiters} separated tokens.
     * @return the array of tokens, each tokens is trimmed, the array will
     *         not contain any empty or {@code null} entries.
     */
    public static String[] tokenize(final String entry) {
        return tokenize(entry, COMMON_DELIMITERS);
    }

    /**
     * Get a canonical array of tokens from a String entry
     * that may contain zero or more tokens separated by characters in
     * delimiters string.
     *
     * @param entry      a String that may contain zero or more
     *                   delimiters separated tokens.
     * @param delimiters string with delimiters, every character represents one
     *                   delimiter.
     * @return the array of tokens, each tokens is trimmed, the array will
     *         not contain any empty or {@code null} entries.
     */
    public static String[] tokenize(final String entry, final String delimiters) {
        final Collection<String> tokens = tokenize(entry, delimiters, new LinkedList<String>());
        return tokens.toArray(new String[tokens.size()]);
    }

    private static Collection<String> tokenize(final String entry, final String delimiters, final Collection<String> tokens) {
        final StringBuilder regexpBuilder = new StringBuilder(delimiters.length() * 3);
        regexpBuilder.append('[');
        for (final char c : delimiters.toCharArray()) {
            regexpBuilder.append(Pattern.quote(String.valueOf(c)));
        }
        regexpBuilder.append(']');

        final String[] tokenArray = entry.split(regexpBuilder.toString());
        for (String token : tokenArray) {
            if (token == null || token.isEmpty()) {
                continue;
            }

            token = token.trim();
            if (token.isEmpty()) {
                continue;
            }

            tokens.add(token);
        }

        return tokens;
    }
}
