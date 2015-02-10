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

package org.glassfish.jersey.message.internal;

/**
 * Internal {@link StringBuilder string builder} utilities for building HTTP header
 * values.
 *
 * @author Marc Hadley
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @author Martin Matula
 */
public class StringBuilderUtils {

    /**
     * Append a new value to the string builder.
     *
     * If the value contains non-token characters (e.g. control, white-space,
     * quotes, separators, etc.), the appended value is quoted and all the quotes
     * in the value are escaped.
     *
     * @param b string builder to be updated.
     * @param value value to be appended.
     */
    public static void appendQuotedIfNonToken(StringBuilder b, String value) {
        if (value == null) {
            return;
        }
        boolean quote = !GrammarUtil.isTokenString(value);
        if (quote) {
            b.append('"');
        }
        appendEscapingQuotes(b, value);
        if (quote) {
            b.append('"');
        }
    }

    /**
     * Append a new value to the string builder.
     *
     * If the value contains white-space characters, the appended
     * value is quoted and all the quotes in the value are escaped.
     *
     * @param b string builder to be updated.
     * @param value value to be appended.
     */
    public static void appendQuotedIfWhitespace(StringBuilder b, String value) {
        if (value == null) {
            return;
        }
        boolean quote = GrammarUtil.containsWhiteSpace(value);
        if (quote) {
            b.append('"');
        }
        appendEscapingQuotes(b, value);
        if (quote) {
            b.append('"');
        }
    }

    /**
     * Append a new quoted value to the string builder.
     *
     * The appended value is quoted and all the quotes in the value are escaped.
     *
     * @param b string builder to be updated.
     * @param value value to be appended.
     */
    public static void appendQuoted(StringBuilder b, String value) {
        b.append('"');
        appendEscapingQuotes(b, value);
        b.append('"');
    }

    /**
     * Append a new value to the string builder.
     *
     * All the quotes in the value are escaped before appending.
     *
     * @param b string builder to be updated.
     * @param value value to be appended.
     */
    public static void appendEscapingQuotes(StringBuilder b, String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '"') {
                b.append('\\');
            }
            b.append(c);
        }
    }

    /**
     * Prevents instantiation.
     */
    private StringBuilderUtils() {
    }
}
