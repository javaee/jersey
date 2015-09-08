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
package org.glassfish.jersey.test.maven.rule;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.plugins.enforcer.AbstractNonCacheableEnforcerRule;

import com.google.common.io.Files;
import com.google.common.io.LineProcessor;

/**
 * Maven enforcer rule to enforce that given file does not contain line matching given pattern. When matched, exception is
 * raised.
 * <p/>
 * This is equivalent to shell pseudo-script: {code grep PATTERN FILE && 'raise error'}
 *
 * @author Stepan Vavra (stepan.vavra at oracle.com)
 */
public class PatternNotMatchedInFileRule extends AbstractNonCacheableEnforcerRule {

    /**
     * The pattern to look for in the given file.
     */
    String pattern;

    /**
     * The file where to look for lines matching given pattern.
     */
    File file;

    /**
     * Maximum number of lines to be matched (exception is raised regardless of the number of found lines as long as the number is
     * greater than 0)
     */
    int maxMatchedLines = 0;

    public void execute(EnforcerRuleHelper helper)
            throws EnforcerRuleException {

        if (file == null || !file.exists()) {
            return;
        }

        final Pattern patternCompiled = Pattern.compile(pattern);
        try {

            final List<String> lines = Files.readLines(file, Charset.defaultCharset(), new LineProcessor<List<String>>() {
                private List<String> matchedLines = new LinkedList<>();

                @Override
                public boolean processLine(final String line) throws IOException {
                    if (patternCompiled.matcher(line).matches()) {
                        matchedLines.add(line);
                        if (maxMatchedLines != 0 && maxMatchedLines <= matchedLines.size()) {
                            return false;
                        }
                    }
                    return true;
                }

                @Override
                public List<String> getResult() {
                    return matchedLines;
                }
            });

            if (lines.size() > 0) {
                throw new EnforcerRuleException(
                        "Found lines matching pattern: '" + pattern + "'! Lines matched: " + Arrays.toString(lines.toArray())
                                + " in file: " + file.getAbsolutePath());
            }

        } catch (IOException e) {
            throw new EnforcerRuleException("I/O Error occurred during processing of file: " + file.getAbsolutePath(), e);
        }
    }
}
