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

package org.glassfish.jersey.jaxb.internal;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>API for performing inflections (pluralization, singularization, and so on)
 * on various strings.  These inflections will be useful in code generators that
 * convert things like database table names into Java class names.</p>
 *
 * <p>The {@code getInstance()} method returns a singleton instance of
 * this class with a default set of rules, which can then be customized.
 * Rules added during customization will take precedence over the standard ones.
 * Use the {@code addIrregular()}, {@code addPlural()}, {@code addSingular()},
 * and {@code addUncountable()} methods to add additional rules ot the default
 * ones.</p>
 *
 * <p><strong>IMPLEMENTATION NOTE</strong> - The default implementation is
 * intended to be functionally compatible with the {@code Inflector::inflections}
 * class in Ruby on Rails.  The {@code gsub()} method on Ruby strings
 * matches regular expressions anywhere in the input.  However, nearly all of
 * the actual patterns used in this component use {@code $} at the end to
 * match the end of the input string (so that only the last word in a multiple
 * word phrase will be singularized or pluralized).  Therefore, the Java versions
 * of the regular expressions have been modified to capture all text before the
 * interesting characters at the end, and emit them as part of the result, so
 * that the entire string can be matched against a pattern once.</p>
 *
 * @author Florian Rosenberg
 */
final class NounInflector {

    // ------------------------------------------------------------ Constructors

    /**
     * <p>Private constructor to avoid instantiation.</p>
     */
    private NounInflector() {

        addPlural("$", "s", false);
        addPlural("(.*)$", "\\1s");
        addPlural("(.*)(ax|test)is$", "\\1\\2es");
        addPlural("(.*)(octop|vir)us$", "\\1\\2i");
        addPlural("(.*)(alias|status)$", "\\1\\2es");
        addPlural("(.*)(bu)s$", "\\1\\2ses");
        addPlural("(.*)(buffal|tomat)o$", "\\1\\2oes");
        addPlural("(.*)([ti])um$", "\\1\\2a");
        addPlural("(.*)sis$", "\\1ses");
        addPlural("(.*)(?:([^f])fe|([lr])f)$", "\\1\\3ves");
        addPlural("(.*)(hive)$", "\\1\\2s");
        addPlural("(.*)(tive)$", "\\1\\2s"); // Added for consistency with singular rules
        addPlural("(.*)([^aeiouy]|qu)y$", "\\1\\2ies");
        addPlural("(.*)(series)$", "\\1\\2"); // Added for consistency with singular rules
        addPlural("(.*)(movie)$", "\\1\\2s"); // Added for consistency with singular rules
        addPlural("(.*)(x|ch|ss|sh)$", "\\1\\2es");
        addPlural("(.*)(matr|vert|ind)ix|ex$", "\\1\\2ices");
        addPlural("(.*)(o)$", "\\1\\2es"); // Added for consistency with singular rules
        addPlural("(.*)(shoe)$", "\\1\\2s"); // Added for consistency with singular rules
        addPlural("(.*)([m|l])ouse$", "\\1\\2ice");
        addPlural("^(ox)$", "\\1en");
        addPlural("(.*)(vert|ind)ex$", "\\1\\2ices"); // Added for consistency with singular rules
        addPlural("(.*)(matr)ix$", "\\1\\2ices"); // Added for consistency with singular rules
        addPlural("(.*)(quiz)$", "\\1\\2zes");

        addSingular("(.*)s$", "\\1");
        addSingular("(.*)(n)ews$", "\\1\\2ews");
        addSingular("(.*)([ti])a$", "\\1\\2um");
        addSingular("(.*)((a)naly|(b)a|(d)iagno|(p)arenthe|(p)rogno|(s)ynop|(t)he)ses$", "\\1\\2sis");
        addSingular("(.*)(^analy)ses$", "\\1\\2sis");
        addSingular("(.*)([^f])ves$", "\\1\\2fe");
        addSingular("(.*)(hive)s$", "\\1\\2");
        addSingular("(.*)(tive)s$", "\\1\\2");
        addSingular("(.*)([lr])ves$", "\\1\\2f");
        addSingular("(.*)([^aeiouy]|qu)ies$", "\\1\\2y");
        addSingular("(.*)(s)eries$", "\\1\\2eries");
        addSingular("(.*)(m)ovies$", "\\1\\2ovie");
        addSingular("(.*)(x|ch|ss|sh)es$", "\\1\\2");
        addSingular("(.*)([m|l])ice$", "\\1\\2ouse");
        addSingular("(.*)(bus)es$", "\\1\\2");
        addSingular("(.*)(o)es$", "\\1\\2");
        addSingular("(.*)(shoe)s$", "\\1\\2");
        addSingular("(.*)(cris|ax|test)es$", "\\1\\2is");
        addSingular("(.*)(octop|vir)i$", "\\1\\2us");
        addSingular("(.*)(alias|status)es$", "\\1\\2");
        addSingular("^(ox)en", "\\1");
        addSingular("(.*)(vert|ind)ices$", "\\1\\2ex");
        addSingular("(.*)(matr)ices$", "\\1\\2ix");
        addSingular("(.*)(quiz)zes$", "\\1\\2");

        addIrregular("child", "children");
        addIrregular("man", "men");
        addIrregular("move", "moves");
        addIrregular("person", "people");
        addIrregular("sex", "sexes");

        addUncountable("equipment");
        addUncountable("fish");
        addUncountable("information");
        addUncountable("money");
        addUncountable("rice");
        addUncountable("series");
        addUncountable("sheep");
        addUncountable("species");

    }
    // -------------------------------------------------------- Static Variables
    /**
     * <p>The singleton instance returned by the default <code>getInstance()</code>
     * method.</p>
     */
    private static transient NounInflector instance = null;
    /**
     * <p>List of <code>Replacer</code>s for performing replacement operations
     * on matches for plural words.</p>
     */
    private final List<Replacer> plurals = new LinkedList<Replacer>();
    /**
     * <p>List of <code>Replacer</code>s for performing replacement operations
     * on matches for addSingular words.</p>
     */
    private final List<Replacer> singulars = new ArrayList<Replacer>();
    /**
     * <p>List of words that represent addUncountable concepts that cannot be
     * pluralized or singularized.</p>
     */
    private final List<String> uncountables = new LinkedList<String>();

    // ------------------------------------------------------ Instance Variables
    // ---------------------------------------------------------- Static Methods

    /**
     * <p>Return a fully configured {@link NounInflector} instance that can be used
     * for performing transformations.</p>
     */
    public static NounInflector getInstance() {

        if (instance == null) {
            instance = new NounInflector();
        }
        return instance;

    }

    // ---------------------------------------------------------- Public Methods

    /**
     * <p>Convert strings to <code>EmbeddedCamelCase</code>.  Embedded
     * underscores will be removed.</p>
     *
     * @param word Word to be converted
     */
    public String camelize(final String word) {

        return camelize(word, false);

    }

    /**
     * <p>Convert word strings consisting of lower case letters and
     * underscore characters between words into <code>embeddedCamelCase</code>
     * or <code>EmbeddedCamelCase</code>, depending on the <code>lower</code>
     * flag.  Embedded underscores will be removed.  Embedded '/'
     * characters will be replaced by '.', making this method useful
     * in converting path-like names into fully qualified classnames.</p>
     *
     * <p><strong>IMPLEMENTATION DIFFERENCE</strong> - The Rails version of this
     * method also converts '/' characters to '::' because that reflects
     * the normal syntax for fully qualified names in Ruby.</p>
     *
     * <table border="1" width="100%">
     *   <tr>
     *     <th>Input</th>
     *     <th>Output</th>
     *   </tr>
     *   <tr>
     *     <td>"foo_bar", false</td>
     *     <td>"FooBar"</td>
     *   </tr>
     *   <tr>
     *     <td>"foo_bar", true</td>
     *     <td>"fooBar"</td>
     *   </tr>
     *   <tr>
     *     <td>"foo_bar/baz", false</td>
     *     <td>"FooBar.Baz"</td>
     *   </tr>
     *   <tr>
     *     <td>"foo_bar/baz", true</td>
     *     <td>"fooBar.Baz"</td>
     *   </tr>
     * </table>
     *
     * @param word Word to be converted
     * @param flag Flag indicating that the initial character should
     *  be lower cased instead of upper cased
     */
    public String camelize(final String word, final boolean flag) {
        if (word.length() == 0) {
            return word;
        }

        final StringBuilder sb = new StringBuilder(word.length());
        if (flag) {
            sb.append(Character.toLowerCase(word.charAt(0)));
        } else {
            sb.append(Character.toUpperCase(word.charAt(0)));
        }
        boolean capitalize = false;
        for (int i = 1; i < word.length(); i++) {
            final char ch = word.charAt(i);
            if (capitalize) {
                sb.append(Character.toUpperCase(ch));
                capitalize = false;
            } else if (ch == '_') {
                capitalize = true;
            } else if (ch == '/') {
                capitalize = true;
                sb.append('.');
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();

    }

    /**
     * <p>Create and return a simple class name that corresponds to a
     * addPlural table name.  Any leading schema name will be trimmed.</p>
     *
     * <table border="1" width="100%">
     *   <tr>
     *     <th>Input</th>
     *     <th>Output</th>
     *   </tr>
     *   <tr>
     *     <td>"foo_bars"</td>
     *     <td>"FooBar"</td>
     *   </tr>
     *   <tr>
     *     <td>"baz"</td>
     *     <td>"Baz"</td>
     *   </tr>
     * </table>
     *
     * @param tableName Table name to be converted
     */
    public String classify(String tableName) {

        final int period = tableName.lastIndexOf('.');
        if (period >= 0) {
            tableName = tableName.substring(period + 1);
        }
        return camelize(singularize(tableName));

    }

    /**
     * <p>Replace underscores in the specified word with dashes.</p>
     *
     * <table border="1" width="100%">
     *   <tr>
     *     <th>Input</th>
     *     <th>Output</th>
     *   </tr>
     *   <tr>
     *     <td>"foo_bar"</td>
     *     <td>"foo-bar"</td>
     *   </tr>
     *   <tr>
     *     <td>"baz"</td>
     *     <td>"baz"</td>
     *   </tr>
     * </table>
     *
     * @param word Word to be converted
     */
    public String dasherize(final String word) {

        return word.replace('_', '-');

    }

    /**
     * <p>Remove any package name from a fully qualified class name,
     * returning only the simple classname.</p>
     *
     * <table border="1" width="100%">
     *   <tr>
     *     <th>Input</th>
     *     <th>Output</th>
     *   </tr>
     *   <tr>
     *     <td>"java.util.Map"</td>
     *     <td>"Map"</td>
     *   </tr>
     *   <tr>
     *     <td>"String"</td>
     *     <td>"String"</td>
     *   </tr>
     * </table>
     *
     * @param className Fully qualified class name to be converted
     */
    public String demodulize(final String className) {

        final int period = className.lastIndexOf('.');
        if (period >= 0) {
            return className.substring(period + 1);
        } else {
            return className;
        }

    }

    /**
     * <p>Create and return a foreign key name from a class name,
     * separating the "id" suffix with an underscore.</p>
     */
    public String foreignKey(final String className) {

        return foreignKey(className, true);

    }

    /**
     * <p>Create and return a foreign key name from a class name,
     * optionally inserting an underscore before the "id" portion.</p>
     *
     * <table border="1" width="100%">
     *   <tr>
     *     <th>Input</th>
     *     <th>Output</th>
     *   </tr>
     *   <tr>
     *     <td>"com.mymodel.Order", false</td>
     *     <td>"orderid"</td>
     *   </tr>
     *   <tr>
     *     <td>"com.mymodel.Order", true</td>
     *     <td>"order_id"</td>
     *   </tr>
     *   <tr>
     *     <td>"Message", false</td>
     *     <td>"messageid"</td>
     *   </tr>
     *   <tr>
     *     <td>"Message", true</td>
     *     <td>"message_id"</td>
     *   </tr>
     * </table>
     *
     * @param className Class name for which to create a foreign key
     * @param underscore Flag indicating whether an underscore should
     *  be emitted between the class name and the "id" suffix
     */
    public String foreignKey(final String className, final boolean underscore) {

        return underscore(demodulize(className) + (underscore ? "_id" : "id"));

    }

    /**
     * <p>Capitalize the first word in a lower cased and underscored string,
     * turn underscores into spaces, and string any trailing "_id".  Like
     * <code>titleize()</code>, this is meant for creating pretty output,
     * and is not intended for code generation.</p>
     *
     * <table border="1" width="100%">
     *   <tr>
     *     <th>Input</th>
     *     <th>Output</th>
     *   </tr>
     *   <tr>
     *     <td>"employee_salary"</td>
     *     <td>"Employee salary"</td>
     *   </tr>
     *   <tr>
     *     <td>"author_id"</td>
     *     <td>"Author"</td>
     *   </tr>
     * </table>
     *
     * @param words Word string to be converted
     */
    public String humanize(String words) {

        if (words.endsWith("_id")) {
            words = words.substring(0, words.length() - 3);
        }
        final StringBuilder sb = new StringBuilder(words.length());
        sb.append(Character.toUpperCase(words.charAt(0)));
        for (int i = 1; i < words.length(); i++) {
            final char ch = words.charAt(i);
            if (ch == '_') {
                sb.append(' ');
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();

    }

    /**
     * <p>Turn a number into a corresponding ordinal string used to
     * denote the position in an ordered sequence.</p>
     *
     * <table border="1" width="100%">
     *   <tr>
     *     <th>Input</th>
     *     <th>Output</th>
     *   </tr>
     *   <tr>
     *     <td>1</td>
     *     <td>"1st"</td>
     *   </tr>
     *   <tr>
     *     <td>2</td>
     *     <td>"2nd"</td>
     *   </tr>
     *   <tr>
     *     <td>3</td>
     *     <td>"3rd"</td>
     *   </tr>
     *   <tr>
     *     <td>4</td>
     *     <td>"rth"</td>
     *   </tr>
     *   <tr>
     *     <td>1002</td>
     *     <td>"1002nd"</td>
     *   </tr>
     *   <tr>
     *     <td>2012</td>
     *     <td>"2012th"</td>
     *   </tr>
     * </table>
     *
     * @param number Number to be converted
     */
    public String ordinalize(final int number) {

        final int modulo = number % 100;
        if ((modulo >= 11) && (modulo <= 13)) {
            return "" + number + "th";
        }
        switch (number % 10) {
            case 1:
                return "" + number + "st";
            case 2:
                return "" + number + "nd";
            case 3:
                return "" + number + "rd";
            default:
                return "" + number + "th";
        }

    }

    /**
     * <p>Return a addPlural version of the specified (addSingular) word.</p>
     *
     *
     * @param word Singular word to be converted
     */
    public String pluralize(final String word) {

        // Scan uncountables and leave alone
        for (final String uncountable : uncountables) {
            if (uncountable.equals(word)) {
                return word;
            }
        }

        // Scan our patterns for a match and return the correct replacement
        for (final Replacer plural : plurals) {
            final String replacement = plural.replacement(word);
            if (replacement != null) {
                return replacement;
            }
        }

        // Return the original string unchanged
        return word;

    }

    /**
     * <p>Return a addSingular version of the specified (addPlural) word.</p>
     *
     *
     * @param word Plural word to be converted
     */
    public String singularize(final String word) {

        // Scan uncountables and leave alone
        for (final String uncountable : uncountables) {
            if (uncountable.equals(word)) {
                return word;
            }
        }

        // Scan our patterns for a match and return the correct replacement
        for (final Replacer singular : singulars) {
            final String replacement = singular.replacement(word);
            if (replacement != null) {
                return replacement;
            }
        }

        // Return the original string unchanged
        return word;

    }

    /**
     * <p>Convert the simple name of a model class into the corresponding
     * name of a database table, by uncamelizing, inserting underscores,
     * and pluralizing the last word.</p>
     *
     * <table border="1" width="100%">
     *   <tr>
     *     <th>Input</th>
     *     <th>Output</th>
     *   </tr>
     *   <tr>
     *     <td>"RawScaledScorer"</td>
     *     <td>"raw_scaled_scorers"</td>
     *   </tr>
     *   <tr>
     *     <td>"fancyCategory"</td>
     *     <td>"fancy_categories"</td>
     *   </tr>
     * </table>
     *
     * @param className Class name to be converted
     */
    public String tableize(final String className) {

        return pluralize(underscore(className));

    }

    /**
     * <p>Capitalize all the words, and replace some characters in the string
     * to create a nicer looking title.  This is meant for creating pretty
     * output, and is not intended for code generation.</p>
     *
     * <table border="1" width="100%">
     *   <tr>
     *     <th>Input</th>
     *     <th>Output</th>
     *   </tr>
     *   <tr>
     *     <td>"the honeymooners"</td>
     *     <td>"The Honeymooners"</td>
     *   </tr>
     *   <tr>
     *     <td>"x-men: the last stand"</td>
     *     <td>"X Men: The Last Stand"</td>
     *   </tr>
     * </table>
     *
     * @param words Word string to be converted
     */
    public String titleize(final String words) {

        final StringBuilder sb = new StringBuilder(words.length());
        boolean capitalize = true; // To get the first character right
        for (int i = 0; i < words.length(); i++) {
            final char ch = words.charAt(i);
            if (Character.isWhitespace(ch)) {
                sb.append(' ');
                capitalize = true;
            } else if (ch == '-') {
                sb.append(' ');
                capitalize = true;
            } else if (capitalize) {
                sb.append(Character.toUpperCase(ch));
                capitalize = false;
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();

    }

    public String decapitalize(final String word) {
        // do nothing if null or empty
        if ((word == null) || (word.length() < 1)) {
            return word;
        }
        // or if already decapitalized
        final char first = word.charAt(0);
        if (Character.isLowerCase(first)) {
            return word;
        }
        // otherwise turn the first character to lower case and attach the rest
        final StringBuilder sb = new StringBuilder(word.length());
        sb.append(Character.toLowerCase(first));
        sb.append(word.substring(1));
        return sb.toString();
    }

    /**
     * <p>The reverse of <code>camelize()</code>, makes an underscored form
     * from the expression in the string.  Changes "." to "/" to convert
     * fully qualified class names into paths.</p>
     *
     * <table border="1" width="100%">
     *   <tr>
     *     <th>Input</th>
     *     <th>Output</th>
     *   </tr>
     *   <tr>
     *     <td>"FooBar"</td>
     *     <td>"foo_bar"</td>
     *   </tr>
     *   <tr>
     *     <td>"fooBar"</td>
     *     <td>"foo_bar"</td>
     *   </tr>
     *   <tr>
     *     <td>"FooBar.Baz"</td>
     *     <td>"foo_bar/baz"</td>
     *   </tr>
     *   <tr>
     *     <td>"FooBar.Baz"</td>
     *     <td>"foo_bar/baz"</td>
     *   </tr>
     * </table>
     *
     * @param word Camel cased word to be converted
     */
    public String underscore(final String word) {

        final StringBuilder sb = new StringBuilder(word.length() + 5);
        boolean uncapitalize = false;
        for (int i = 0; i < word.length(); i++) {
            final char ch = word.charAt(i);
            if (uncapitalize) {
                sb.append(Character.toLowerCase(ch));
                uncapitalize = false;
            } else if (ch == '.') {
                sb.append('/');
                uncapitalize = true;
            } else if (Character.isUpperCase(ch)) {
                if (i > 0) {
                    sb.append('_');
                }
                sb.append(Character.toLowerCase(ch));
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();

    }

    // --------------------------------------------------- Customization Methods

    /**
     * <p>Add the addSingular and addPlural forms of words that cannot be
     * converted using the normal rules.</p>
     *
     *
     * @param singular Singular form of the word
     * @param plural Plural form of the word
     */
    public void addIrregular(final String singular, final String plural) {

        addPlural("(.*)(" + singular.substring(0, 1) + ")" + singular.substring(1) + "$",
                "\\1\\2" + plural.substring(1));
        addSingular("(.*)(" + plural.substring(0, 1) + ")" + plural.substring(1) + "$",
                "\\1\\2" + singular.substring(1));

    }

    /**
     * <p>Add a match pattern and replacement rule for converting addPlural
     * forms to addSingular forms.  By default, matches will be case
     * insensitive.</p>
     *
     *
     * @param match Match pattern regular expression
     * @param rule Replacement rule
     */
    public void addPlural(final String match, final String rule) {

        addPlural(match, rule, true);

    }

    /**
     * <p>Add a match pattern and replacement rule for converting addPlural
     * forms to addSingular forms.</p>
     *
     *
     * @param match Match pattern regular expression
     * @param rule Replacement rule
     * @param insensitive Flag indicating this match should be case insensitive
     */
    public void addPlural(final String match, final String rule, final boolean insensitive) {

        plurals.add(0, new Replacer(match, rule, insensitive));

    }

    /**
     * <p>Add a match pattern and replacement rule for converting addSingular
     * forms to addPlural forms.  By default, matches will be case insensitive.</p>
     *
     *
     * @param match Match pattern regular expression
     * @param rule Replacement rule
     */
    public void addSingular(final String match, final String rule) {

        addSingular(match, rule, true);

    }

    /**
     * <p>Add a match pattern and replacement rule for converting addSingular
     * forms to addPlural forms.</p>
     *
     *
     * @param match Match pattern regular expression
     * @param rule Replacement rule
     * @param insensitive Flag indicating this match should be case insensitive
     */
    public void addSingular(final String match, final String rule, final boolean insensitive) {

        singulars.add(0, new Replacer(match, rule, insensitive));

    }

    /**
     * <p>Add a word that cannot be converted between addSingular and addPlural.</p>
     *
     *
     * @param word Word to be added
     */
    public void addUncountable(final String word) {

        uncountables.add(0, word.toLowerCase());

    }

    // --------------------------------------------------------- Private Classes

    /**
     * <p>Internal class that uses a regular expression matcher to both
     * match the specified regular expression to a specified word, and
     * (if successful) perform the appropriate substitutions.</p>
     */
    private static class Replacer {

        // --------------------------------------------------------- Constructor
        public Replacer(final String match, final String rule, final boolean insensitive) {

            pattern = Pattern.compile(match,
                    insensitive ? Pattern.CASE_INSENSITIVE : 0);
            this.rule = rule;

        }

        // -------------------------------------------------- Instance Variables
        private Pattern pattern = null;
        private String rule = null;

        // ------------------------------------------------------ Public Methods

        /**
         * Replace the input if it matches the pattern.
         *
         * @param input the input string.
         * @return the replacement, if the input matches, otherwise null.
         */
        public String replacement(final String input) {
            final Matcher matcher = pattern.matcher(input);
            if (matcher.matches()) {
                final StringBuilder sb = new StringBuilder();
                boolean group = false;
                for (int i = 0; i < rule.length(); i++) {
                    final char ch = rule.charAt(i);
                    if (group) {
                        sb.append(matcher.group(Character.digit(ch, 10)));
                        group = false;
                    } else if (ch == '\\') {
                        group = true;
                    } else {
                        sb.append(ch);
                    }
                }
                return sb.toString();
            } else {
                return null;
            }
        }
    }
}
