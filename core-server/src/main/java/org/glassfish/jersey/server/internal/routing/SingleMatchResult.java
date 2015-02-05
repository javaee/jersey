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
package org.glassfish.jersey.server.internal.routing;

import java.util.regex.MatchResult;

/**
 * {@link MatchResult} implementation that returns the nested string as a
 * single matching result. This match result mimics matching of a single
 * matching group with group index 0 (the one containing the whole expression).
 *
 * @author Paul Sandoz
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
final class SingleMatchResult implements MatchResult {

    private final String path;

    /**
     * Construct a match result matching the whole supplied path.
     *
     * @param path matched path.
     */
    public SingleMatchResult(final String path) {
        this.path = stripMatrixParams(path);
    }

    /**
     * Strip the matrix parameters from a path.
     *
     * @return path stripped of matrix parameters.
     */
    private static String stripMatrixParams(final String path) {
        int e = path.indexOf(';');
        if (e == -1) {
            return path;
        }

        int s = 0;
        StringBuilder sb = new StringBuilder();
        do {
            // Append everything up to but not including the ';'
            sb.append(path, s, e);

            // Skip everything up to but not including the '/'
            s = path.indexOf('/', e + 1);
            if (s == -1) {
                break;
            }
            e = path.indexOf(';', s);
        } while (e != -1);

        if (s != -1) {
            // Append any remaining characters
            sb.append(path, s, path.length());
        }

        return sb.toString();
    }


    @Override
    public int start() {
        return 0;
    }

    @Override
    public int start(final int group) {
        if (group == 0) {
            return start();
        }
        throw new IndexOutOfBoundsException();
    }

    @Override
    public int end() {
        return path.length();
    }

    @Override
    public int end(final int group) {
        if (group == 0) {
            return end();
        }
        throw new IndexOutOfBoundsException();
    }

    @Override
    public String group() {
        return path;
    }

    @Override
    public String group(final int group) {
        if (group == 0) {
            return group();
        }
        throw new IndexOutOfBoundsException();
    }

    @Override
    public int groupCount() {
        return 0;
    }
}
