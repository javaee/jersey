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
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package org.glassfish.jersey.uri.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.glassfish.jersey.internal.LocalizationMessages;
import org.glassfish.jersey.uri.UriComponent;

/**
 * A URI template parser that parses JAX-RS specific URI templates.
 *
 * @author Paul Sandoz
 * @author Gerard Davison (gerard.davison at oracle.com)
 */
public class UriTemplateParser {

    /* package */ static final int[] EMPTY_INT_ARRAY = new int[0];
    private static final Set<Character> RESERVED_REGEX_CHARACTERS = initReserved();
    private static final String[] HEX_TO_UPPERCASE_REGEX = initHexToUpperCaseRegex();

    private static Set<Character> initReserved() {
        char[] reserved = {
                '.', '^', '&', '!',
                '?', '-', ':', '<',
                '(', '[', '$', '=',
                ')', ']', ',', '>',
                '*', '+', '|'};

        Set<Character> s = new HashSet<Character>(reserved.length);
        for (char c : reserved) {
            s.add(c);
        }
        return s;
    }

    /**
     * Default URI template value regexp pattern.
     */
    public static final Pattern TEMPLATE_VALUE_PATTERN = Pattern.compile("[^/]+");

    private final String template;
    private final StringBuffer regex = new StringBuffer();
    private final StringBuffer normalizedTemplate = new StringBuffer();
    private final StringBuffer literalCharactersBuffer = new StringBuffer();
    private final Pattern pattern;
    private final List<String> names = new ArrayList<String>();
    private final List<Integer> groupCounts = new ArrayList<Integer>();
    private final Map<String, Pattern> nameToPattern = new HashMap<String, Pattern>();
    private int numOfExplicitRegexes;
    private int skipGroup;

    private int literalCharacters;

    /**
     * Parse a template.
     *
     * @param template the template.
     * @throws IllegalArgumentException if the template is null, an empty string
     *                                  or does not conform to a JAX-RS URI template.
     */
    public UriTemplateParser(final String template) throws IllegalArgumentException {
        if (template == null || template.isEmpty()) {
            throw new IllegalArgumentException("Template is null or has zero length");
        }

        this.template = template;
        parse(new CharacterIterator(template));
        try {
            pattern = Pattern.compile(regex.toString());
        } catch (PatternSyntaxException ex) {
            throw new IllegalArgumentException("Invalid syntax for the template expression '"
                    + regex + "'",
                    ex
            );
        }
    }

    /**
     * Get the template.
     *
     * @return the template.
     */
    public final String getTemplate() {
        return template;
    }

    /**
     * Get the pattern.
     *
     * @return the pattern.
     */
    public final Pattern getPattern() {
        return pattern;
    }

    /**
     * Get the normalized template.
     * <p>
     * A normalized template is a template without any explicit regular
     * expressions.
     *
     * @return the normalized template.
     */
    public final String getNormalizedTemplate() {
        return normalizedTemplate.toString();
    }

    /**
     * Get the map of template names to patterns.
     *
     * @return the map of template names to patterns.
     */
    public final Map<String, Pattern> getNameToPattern() {
        return nameToPattern;
    }

    /**
     * Get the list of template names.
     *
     * @return the list of template names.
     */
    public final List<String> getNames() {
        return names;
    }

    /**
     * Get the capturing group counts for each template variable.
     *
     * @return the capturing group counts.
     */
    public final List<Integer> getGroupCounts() {
        return groupCounts;
    }

    /**
     * Get the group indexes to capturing groups.
     * <p>
     * Any nested capturing groups will be ignored and the
     * the group index will refer to the top-level capturing
     * groups associated with the templates variables.
     *
     * @return the group indexes to capturing groups.
     */
    public final int[] getGroupIndexes() {
        if (names.isEmpty()) {
            return EMPTY_INT_ARRAY;
        }

        int[] indexes = new int[names.size()];
        indexes[0] = 0 + groupCounts.get(0);
        for (int i = 1; i < indexes.length; i++) {
            indexes[i] = indexes[i - 1] + groupCounts.get(i);
        }

        return indexes;
    }

    /**
     * Get the number of explicit regular expressions.
     *
     * @return the number of explicit regular expressions.
     */
    public final int getNumberOfExplicitRegexes() {
        return numOfExplicitRegexes;
    }

    /**
     * Get the number of regular expression groups
     *
     * @return the number of regular expressions groups
     *
     * @since 2.9
     */
    public final int getNumberOfRegexGroups() {
        if (groupCounts.isEmpty()) {
            return 0;
        } else {
            int[] groupIndex = getGroupIndexes();
            return groupIndex[groupIndex.length - 1] + skipGroup;
        }
    }

    /**
     * Get the number of literal characters.
     *
     * @return the number of literal characters.
     */
    public final int getNumberOfLiteralCharacters() {
        return literalCharacters;
    }

    /**
     * Encode literal characters of a template.
     *
     * @param characters the literal characters
     * @return the encoded literal characters.
     */
    protected String encodeLiteralCharacters(final String characters) {
        return characters;
    }

    private void parse(final CharacterIterator ci) {
        try {
            while (ci.hasNext()) {
                char c = ci.next();
                if (c == '{') {
                    processLiteralCharacters();
                    skipGroup = parseName(ci, skipGroup);
                } else {
                    literalCharactersBuffer.append(c);
                }
            }
            processLiteralCharacters();
        } catch (NoSuchElementException ex) {
            throw new IllegalArgumentException(LocalizationMessages.ERROR_TEMPLATE_PARSER_INVALID_SYNTAX_TERMINATED(
                    template), ex);
        }
    }

    private void processLiteralCharacters() {
        if (literalCharactersBuffer.length() > 0) {
            literalCharacters += literalCharactersBuffer.length();

            String s = encodeLiteralCharacters(literalCharactersBuffer.toString());

            normalizedTemplate.append(s);

            // Escape if reserved regex character
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (RESERVED_REGEX_CHARACTERS.contains(c)) {
                    regex.append("\\");
                    regex.append(c);
                } else if (c == '%') {
                    final char c1 = s.charAt(i + 1);
                    final char c2 = s.charAt(i + 2);
                    if (UriComponent.isHexCharacter(c1) && UriComponent.isHexCharacter(c2)) {
                        regex.append("%").append(HEX_TO_UPPERCASE_REGEX[c1]).append(HEX_TO_UPPERCASE_REGEX[c2]);
                        i += 2;
                    }
                } else {
                    regex.append(c);
                }
            }
            literalCharactersBuffer.setLength(0);
        }
    }

    private static String[] initHexToUpperCaseRegex() {
        String[] table = new String[0x80];
        for (int i = 0; i < table.length; i++) {
            table[i] = String.valueOf((char) i);
        }

        for (char c = 'a'; c <= 'f'; c++) {
            // initialize table values: table[a] = ([aA]) ...
            table[c] = "[" + c + (char) (c - 'a' + 'A') + "]";
        }

        for (char c = 'A'; c <= 'F'; c++) {
            // initialize table values: table[A] = ([aA]) ...
            table[c] = "[" + (char) (c - 'A' + 'a') + c + "]";
        }
        return table;
    }

    private int parseName(final CharacterIterator ci, int skipGroup) {
        char c = consumeWhiteSpace(ci);

        char paramType = 'p'; // Normal path param unless otherwise stated
        StringBuilder nameBuffer = new StringBuilder();

        // Look for query or matrix types
        if (c == '?' || c == ';') {
            paramType = c;
            c = ci.next();
        }

        if (Character.isLetterOrDigit(c) || c == '_') {
            // Template name character
            nameBuffer.append(c);
        } else {
            throw new IllegalArgumentException(LocalizationMessages.ERROR_TEMPLATE_PARSER_ILLEGAL_CHAR_START_NAME(c, ci.pos(),
                    template));
        }

        String nameRegexString = "";
        while (true) {
            c = ci.next();
            // "\\{(\\w[-\\w\\.]*)
            if (Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == '.') {
                // Template name character
                nameBuffer.append(c);
            } else if (c == ',' && paramType != 'p') {
                // separator allowed for non-path parameter names
                nameBuffer.append(c);
            } else if (c == ':' && paramType == 'p') {
                nameRegexString = parseRegex(ci);
                break;
            } else if (c == '}') {
                break;
            } else if (c == ' ') {
                c = consumeWhiteSpace(ci);

                if (c == ':') {
                    nameRegexString = parseRegex(ci);
                    break;
                } else if (c == '}') {
                    break;
                } else {
                    // Error
                    throw new IllegalArgumentException(
                            LocalizationMessages.ERROR_TEMPLATE_PARSER_ILLEGAL_CHAR_AFTER_NAME(c, ci.pos(), template));
                }
            } else {
                throw new IllegalArgumentException(
                        LocalizationMessages.ERROR_TEMPLATE_PARSER_ILLEGAL_CHAR_PART_OF_NAME(c, ci.pos(), template));
            }
        }

        String name = nameBuffer.toString();
        Pattern namePattern;
        try {
            if (paramType == '?' || paramType == ';') {
                String[] subNames = name.split(",\\s?");

                // Build up the regex for each of these properties
                StringBuilder regexBuilder = new StringBuilder(paramType == '?' ? "\\?" : ";");
                String separator = paramType == '?' ? "\\&" : ";/\\?";

                // Start a group because each parameter could repeat
                //                names.add("__" + (paramType == '?' ? "query" : "matrix"));

                boolean first = true;

                regexBuilder.append("(");
                for (String subName : subNames) {
                    regexBuilder.append("(&?");
                    regexBuilder.append(subName);
                    regexBuilder.append("(=([^");
                    regexBuilder.append(separator);
                    regexBuilder.append("]*))?");
                    regexBuilder.append(")");
                    if (!first) {
                        regexBuilder.append("|");
                    }

                    names.add(subName);
                    groupCounts.add(
                            first ? 5 : 3);
                    first = false;
                }

                //                groupCounts.add(1);
                skipGroup = 1;

                // Knock of last bar
                regexBuilder.append(")*");

                namePattern = Pattern.compile(regexBuilder.toString());

                // Make sure we display something useful
                name = paramType + name;
            } else {
                names.add(name);
                //               groupCounts.add(1 + skipGroup);

                if (!nameRegexString.isEmpty()) {
                    numOfExplicitRegexes++;
                }
                namePattern = (nameRegexString.isEmpty())
                        ? TEMPLATE_VALUE_PATTERN : Pattern.compile(nameRegexString);
                if (nameToPattern.containsKey(name)) {
                    if (!nameToPattern.get(name).equals(namePattern)) {
                        throw new IllegalArgumentException(
                                LocalizationMessages.ERROR_TEMPLATE_PARSER_NAME_MORE_THAN_ONCE(name, template));
                    }
                } else {
                    nameToPattern.put(name, namePattern);
                }

                // Determine group count of pattern
                Matcher m = namePattern.matcher("");
                int g = m.groupCount();
                groupCounts.add(1 + skipGroup);
                skipGroup = g;
            }

            regex.append('(')
                    .append(namePattern)
                    .append(')');

            normalizedTemplate.append('{')
                    .append(name)
                    .append('}');
        } catch (PatternSyntaxException ex) {
            throw new IllegalArgumentException(
                    LocalizationMessages.ERROR_TEMPLATE_PARSER_INVALID_SYNTAX(nameRegexString, name, template), ex);
        }

        // Tell the next time through the loop how many to skip
        return skipGroup;
    }

    private String parseRegex(final CharacterIterator ci) {
        StringBuilder regexBuffer = new StringBuilder();

        int braceCount = 1;
        while (true) {
            char c = ci.next();
            if (c == '{') {
                braceCount++;
            } else if (c == '}') {
                braceCount--;
                if (braceCount == 0) {
                    break;
                }
            }
            regexBuffer.append(c);
        }

        return regexBuffer.toString().trim();
    }

    private char consumeWhiteSpace(final CharacterIterator ci) {
        char c;
        do {
            c = ci.next();
        } while (Character.isWhitespace(c));

        return c;
    }
}
