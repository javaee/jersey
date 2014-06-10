/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2014 Oracle and/or its affiliates. All rights reserved.
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

import java.util.regex.MatchResult;

import org.junit.Assert;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Tests {@link PathTemplate}.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @author Gerard Davison (gerard.davison at oracle.com)
 */
public class PathPatternTest {

    @Test
    public void testTerminalPathPatterMatching() {
        final String[] rootPaths = new String[]{
                "/a/b/c",
                "/a/b/c/"
        };

        final String[] subPaths = new String[]{
                "d/e",
                "d/e/",
                "/d/e",
                "/d/e/",
        };

        final String path1 = "/a/b/c";
        final String path2 = "/a/b/c/";

        final PathPattern[] patterns = new PathPattern[]{
                new PathPattern("a"),
                new PathPattern("b"),
                new PathPattern("c"),
                new PathPattern("", PathPattern.RightHandPath.capturingZeroSegments)
        };

        String rhp = path1;
        MatchResult matchResult;

        for (PathPattern pattern : patterns) {
            matchResult = pattern.match(rhp);
            assertNotNull("No match of " + rhp + " for pattern " + pattern, matchResult);
            rhp = matchResult.group(matchResult.groupCount());
            rhp = (rhp == null) ? "" : rhp;
        }

        Assert.assertEquals("", rhp);

        rhp = path2;

        for (PathPattern pattern : patterns) {
            matchResult = pattern.match(rhp);
            rhp = matchResult.group(matchResult.groupCount());
        }

        assertEquals("/", rhp);
    }

    @Test
    public void testSimplePattern() throws Exception {
        PathPattern pattern = new PathPattern("/test");
        assertNull(pattern.match("doesn't match"));
        assertNotNull(pattern.match("/test/me"));
    }

    @Test
    public void testSimplePatternWithRightHandSide() throws Exception {

        PathPattern pattern = new PathPattern(new PathTemplate("/test/{template: abc.*}"));
        assertNull("Why matched?", pattern.match("/test/me"));
        assertNotNull("Why not matched?", pattern.match("/test/abc-should_work"));
    }

    @Test
    public void testSetsAndGetsUriTemplate() throws Exception {
        PathTemplate tmpl = new PathTemplate("/test");
        PathPattern pattern = new PathPattern(tmpl);
        assertEquals(
                "We just injected the value, why it is different?",
                tmpl,
                pattern.getTemplate()
        );
    }

    @Test
    public void testLastElementOfMatchIsRestOfPath() throws Exception {
        PathPattern path = new PathPattern("{a: (\\d)(\\d*)}-{b: (\\d)(\\d*)}-{c: (\\d)(\\d*)}");


        MatchResult m = path.match("/123-456-789/d");
        String value = m.group(m.groupCount());

        assertEquals(
                "Last value should match all of the trailing part",
                "/d",
                value
        );
    }
}
