/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2014 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.uri;

import java.util.List;
import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * A pattern for matching a string against a regular expression
 * and returning capturing group values for any capturing groups present in
 * the expression.
 *
 * @author Paul Sandoz
 * @author Gerard Davison (gerard.davison at oracle.com)
 */
public class PatternWithGroups {

    private static final int[] EMPTY_INT_ARRAY = new int[0];
    /**
     * The empty pattern that matches the null or empty string.
     */
    public static final PatternWithGroups EMPTY = new PatternWithGroups();
    /**
     * The regular expression for matching and obtaining capturing group values.
     */
    private final String regex;
    /**
     * The compiled regular expression of {@link #regex}.
     */
    private final Pattern regexPattern;
    /**
     * The array of group indexes to capturing groups.
     */
    private final int[] groupIndexes;

    /**
     * Construct an empty pattern.
     */
    protected PatternWithGroups() {
        this.regex = "";
        this.regexPattern = null;
        this.groupIndexes = EMPTY_INT_ARRAY;
    }

    /**
     * Construct a new pattern.
     *
     * @param regex the regular expression. If the expression is {@code null} or an empty string then the pattern will only match
     *              a {@code null} or empty string.
     * @throws java.util.regex.PatternSyntaxException if the regular expression could not be compiled.
     */
    public PatternWithGroups(final String regex) throws PatternSyntaxException {
        this(regex, EMPTY_INT_ARRAY);
    }

    /**
     * Construct a new pattern.
     *
     * @param regex        the regular expression. If the expression is {@code null} or an empty string then the pattern will
     *                     only
     *                     match
     *                     a {@code null} or empty string.
     * @param groupIndexes the array of group indexes to capturing groups.
     * @throws java.util.regex.PatternSyntaxException if the regular expression could not be compiled.
     */
    public PatternWithGroups(final String regex, final int[] groupIndexes) throws PatternSyntaxException {
        this(compile(regex), groupIndexes);
    }

    private static Pattern compile(final String regex) throws PatternSyntaxException {
        return (regex == null || regex.isEmpty()) ? null : Pattern.compile(regex);
    }

    /**
     * Construct a new pattern.
     *
     * @param regexPattern the regular expression pattern.
     * @throws IllegalArgumentException if the regexPattern is {@code null}.
     */
    public PatternWithGroups(final Pattern regexPattern) throws IllegalArgumentException {
        this(regexPattern, EMPTY_INT_ARRAY);
    }

    /**
     * Construct a new pattern.
     *
     * @param regexPattern the regular expression pattern.
     * @param groupIndexes the array of group indexes to capturing groups.
     * @throws IllegalArgumentException if the regexPattern is {@code null}.
     */
    public PatternWithGroups(final Pattern regexPattern, final int[] groupIndexes) throws IllegalArgumentException {
        if (regexPattern == null) {
            throw new IllegalArgumentException();
        }

        this.regex = regexPattern.toString();
        this.regexPattern = regexPattern;
        this.groupIndexes = groupIndexes.clone();
    }

    /**
     * Get the regular expression.
     *
     * @return the regular expression.
     */
    public final String getRegex() {
        return regex;
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
        return groupIndexes.clone();
    }

    private static final class EmptyStringMatchResult implements MatchResult {

        @Override
        public int start() {
            return 0;
        }

        @Override
        public int start(final int group) {
            if (group != 0) {
                throw new IndexOutOfBoundsException();
            }
            return start();
        }

        @Override
        public int end() {
            return 0;
        }

        @Override
        public int end(final int group) {
            if (group != 0) {
                throw new IndexOutOfBoundsException();
            }
            return end();
        }

        @Override
        public String group() {
            return "";
        }

        @Override
        public String group(final int group) {
            if (group != 0) {
                throw new IndexOutOfBoundsException();
            }
            return group();
        }

        @Override
        public int groupCount() {
            return 0;
        }
    }

    private static final EmptyStringMatchResult EMPTY_STRING_MATCH_RESULT = new EmptyStringMatchResult();

    private final class GroupIndexMatchResult implements MatchResult {

        private final MatchResult result;

        GroupIndexMatchResult(final MatchResult r) {
            this.result = r;
        }

        @Override
        public int start() {
            return result.start();
        }

        @Override
        public int start(final int group) {
            if (group > groupCount()) {
                throw new IndexOutOfBoundsException();
            }

            return (group > 0) ? result.start(groupIndexes[group - 1]) : result.start();
        }

        @Override
        public int end() {
            return result.end();
        }

        @Override
        public int end(final int group) {
            if (group > groupCount()) {
                throw new IndexOutOfBoundsException();
            }

            return (group > 0) ? result.end(groupIndexes[group - 1]) : result.end();
        }

        @Override
        public String group() {
            return result.group();
        }

        @Override
        public String group(final int group) {
            if (group > groupCount()) {
                throw new IndexOutOfBoundsException();
            }

            return (group > 0) ? result.group(groupIndexes[group - 1]) : result.group();
        }

        @Override
        public int groupCount() {
            return groupIndexes.length;
        }
    }

    /**
     * Match against the pattern.
     *
     * @param cs the char sequence to match against the template.
     * @return the match result, otherwise null if no match occurs.
     */
    public final MatchResult match(final CharSequence cs) {
        // Check for match against the empty pattern
        if (cs == null) {
            return (regexPattern == null) ? EMPTY_STRING_MATCH_RESULT : null;
        } else if (regexPattern == null) {
            return null;
        }

        // Match regular expression
        Matcher m = regexPattern.matcher(cs);
        if (!m.matches()) {
            return null;
        }

        if (cs.length() == 0) {
            return EMPTY_STRING_MATCH_RESULT;
        }

        return (groupIndexes.length > 0) ? new GroupIndexMatchResult(m) : m;
    }

    /**
     * Match against the pattern.
     * <p/>
     * If a matched then the capturing group values (if any) will be added to a list passed in as parameter.
     *
     * @param cs          the char sequence to match against the template.
     * @param groupValues the list to add the values of a pattern's capturing groups if matching is successful. The values are
     *                    added in the same order as the pattern's capturing groups. The list is cleared before values are added.
     * @return {@code true} if the char sequence matches the pattern, otherwise {@code false}.
     *
     * @throws IllegalArgumentException if the group values is {@code null}.
     */
    public final boolean match(final CharSequence cs, final List<String> groupValues) throws IllegalArgumentException {
        if (groupValues == null) {
            throw new IllegalArgumentException();
        }

        // Check for match against the empty pattern
        if (cs == null || cs.length() == 0) {
            return regexPattern == null;
        } else if (regexPattern == null) {
            return false;
        }

        // Match the regular expression
        Matcher m = regexPattern.matcher(cs);
        if (!m.matches()) {
            return false;
        }

        groupValues.clear();
        if (groupIndexes.length > 0) {
            for (int i = 0; i < groupIndexes.length; i++) {
                groupValues.add(m.group(groupIndexes[i]));
            }
        } else {
            for (int i = 1; i <= m.groupCount(); i++) {
                groupValues.add(m.group(i));
            }
        }

        // TODO check for consistency of different capturing groups
        // that must have the same value

        return true;
    }

    /**
     * Match against the pattern.
     * <p/>
     * If a matched then the capturing group values (if any) will be added to a list passed in as parameter.
     *
     * @param cs          the char sequence to match against the template.
     * @param groupNames  the list names associated with a pattern's capturing groups. The names MUST be in the same order as the
     *                    pattern's capturing groups and the size MUST be equal to or less than the number of capturing groups.
     * @param groupValues the map to add the values of a pattern's capturing groups if matching is successful. A values is put
     *                    into the map using the group name associated with the capturing group. The map is cleared before values
     *                    are added.
     * @return {@code true} if the matches the pattern, otherwise {@code false}.
     *
     * @throws IllegalArgumentException if group values is {@code null}.
     */
    public final boolean match(final CharSequence cs, final List<String> groupNames, final Map<String,
            String> groupValues) throws IllegalArgumentException {
        if (groupValues == null) {
            throw new IllegalArgumentException();
        }

        // Check for match against the empty pattern
        if (cs == null || cs.length() == 0) {
            return regexPattern == null;
        } else if (regexPattern == null) {
            return false;
        }

        // Match the regular expression
        Matcher m = regexPattern.matcher(cs);
        if (!m.matches()) {
            return false;
        }

        // Assign the matched group values to group names
        groupValues.clear();

        for (int i = 0; i < groupNames.size(); i++) {
            String name = groupNames.get(i);
            String currentValue = m.group((groupIndexes.length > 0) ? groupIndexes[i] : i + 1);

            // Group names can have the same name occurring more than once,
            // check that groups values are same.
            String previousValue = groupValues.get(name);
            if (previousValue != null && !previousValue.equals(currentValue)) {
                return false;
            }

            groupValues.put(name, currentValue);
        }

        return true;
    }

    @Override
    public final int hashCode() {
        return regex.hashCode();
    }

    @Override
    public final boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PatternWithGroups that = (PatternWithGroups) obj;
        if (this.regex != that.regex && (this.regex == null || !this.regex.equals(that.regex))) {
            return false;
        }
        return true;
    }

    @Override
    public final String toString() {
        return regex;
    }
}
