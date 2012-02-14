/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

import java.util.Comparator;

/**
 * A path pattern that is a regular expression generated from a URI path
 * template.
 * <p>
 * The path pattern is normalized by removing a terminating "/" if present.
 * <p>
 * The path pattern is postfixed with a right hand pattern that consists of either
 * a matching group that matches zero or more path segments,
 * see {@link RightHandPath#capturingZeroOrMoreSegments}, or zero path
 * segments, see {@link RightHandPath#capturingZeroSegments}.
 *
 * @author Paul Sandoz
 */
public final class PathPattern extends PatternWithGroups {

    public static final PathPattern EMPTY_PATH = new PathPattern();
    /**
     * Defer to comparing the templates associated with the patterns
     */
    static public final Comparator<PathPattern> COMPARATOR = new Comparator<PathPattern>() {

        @Override
        public int compare(PathPattern o1, PathPattern o2) {
            return UriTemplate.COMPARATOR.compare(o1.template, o2.template);
        }
    };

    /**
     * The set of right hand path patterns that may be postfixed to a path
     * pattern.
     */
    public static enum RightHandPath {

        /**
         * A capturing group that matches zero or more path segments.
         */
        capturingZeroOrMoreSegments("(/.*)?"),
        /**
         * A capturing group that matches zero segments.
         */
        capturingZeroSegments("(/)?");
        //
        private final String regex;

        RightHandPath(String regex) {
            this.regex = regex;
        }

        String getRegex() {
            return regex;
        }
    }
    //
    private final UriTemplate template;

    private PathPattern() {
        super();
        this.template = UriTemplate.EMPTY;
    }

    /**
     * Create a path pattern and post fix with
     * {@link RightHandPath#capturingZeroOrMoreSegments}.
     *
     * @param template the path template.
     *
     * @see #PathPattern(String, PathPattern.RightHandPath)
     */
    public PathPattern(String template) {
        this(new PathTemplate(template));
    }

    /**
     * Create a path pattern and post fix with
     * {@link RightHandPath#capturingZeroOrMoreSegments}.
     *
     * @param template the path template
     *
     * @see #PathPattern(PathTemplate, PathPattern.RightHandPath)
     */
    public PathPattern(PathTemplate template) {
        super(postfixWithCapturingGroup(template.getPattern().getRegex()),
                addIndexForRightHandPathCapturingGroup(template.getPattern().getGroupIndexes()));

        this.template = template;
    }

    /**
     * Create a path pattern and post fix with a right hand path pattern.
     *
     * @param template the path template.
     * @param rhpp the right hand path pattern postfix.
     */
    public PathPattern(String template, RightHandPath rhpp) {
        this(new PathTemplate(template), rhpp);
    }

    /**
     * Create a path pattern and post fix with a right hand path pattern.
     *
     * @param template the path template.
     * @param rhpp the right hand path pattern postfix.
     */
    public PathPattern(PathTemplate template, RightHandPath rhpp) {
        super(postfixWithCapturingGroup(template.getPattern().getRegex(), rhpp),
                addIndexForRightHandPathCapturingGroup(template.getPattern().getGroupIndexes()));

        this.template = template;
    }

    public UriTemplate getTemplate() {
        return template;
    }

    private static String postfixWithCapturingGroup(String regex) {
        return postfixWithCapturingGroup(regex, RightHandPath.capturingZeroOrMoreSegments);
    }

    private static String postfixWithCapturingGroup(String regex, RightHandPath rhpp) {
        if (regex.endsWith("/")) {
            regex = regex.substring(0, regex.length() - 1);
        }

        return regex + rhpp.getRegex();
    }

    private static int[] addIndexForRightHandPathCapturingGroup(int[] indexes) {
        if (indexes.length == 0) {
            return indexes;
        }

        int[] cgIndexes = new int[indexes.length + 1];
        System.arraycopy(indexes, 0, cgIndexes, 0, indexes.length);

        cgIndexes[indexes.length] = cgIndexes[indexes.length - 1] + 1;
        return cgIndexes;
    }
}
