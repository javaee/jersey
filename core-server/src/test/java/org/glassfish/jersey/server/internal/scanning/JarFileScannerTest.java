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
package org.glassfish.jersey.server.internal.scanning;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Martin Snyder
 */
@RunWith(Theories.class)
public class JarFileScannerTest {

    @DataPoint
    public static final boolean RECURSIVE = true;
    @DataPoint
    public static final boolean NON_RECURSIVE = false;

    private String jaxRsApiPath;

    @Before
    public void setUp() throws Exception {
        final String classPath = System.getProperty("java.class.path");
        final String[] entries = classPath.split(System.getProperty("path.separator"));

        for (final String entry : entries) {
            if (entry.contains("javax.ws.rs-api")) {
                jaxRsApiPath = entry;
                break;
            }
        }

        if (jaxRsApiPath == null) {
            fail("Could not find javax.ws.rs-api.");
        }
    }

    @Test
    public void testRecursiveResourceEnumerationOfAllPackages() throws IOException {
        final int actualEntries = countJarEntriesByPattern(Pattern.compile(".*\\.(class|properties|xml)"));
        final int scannedEntries = countJarEntriesUsingScanner("", true);
        assertThat("Failed to enumerate all contents of javax.ws.rs-api", scannedEntries, equalTo(actualEntries));
    }

    @Test
    public void testRecursiveClassEnumerationWithExistantPackage() throws IOException {
        final int actualEntries = countJarEntriesByPattern(Pattern.compile("javax/ws/rs/.*\\.class"));
        final int scannedEntries = countJarEntriesUsingScanner("javax/ws/rs", true);
        assertThat("Failed to enumerate all contents of javax.ws.rs-api", scannedEntries, equalTo(actualEntries));
    }

    @Test
    public void testNonRecursiveClassEnumerationWithExistantPackage() throws IOException {
        final int actualEntries = countJarEntriesByPattern(Pattern.compile("javax/ws/rs/[^/]*\\.class"));
        final int scannedEntries = countJarEntriesUsingScanner("javax/ws/rs", false);
        assertThat("Failed to enumerate package 'javax.ws.rs' of javax.ws.rs-api", scannedEntries, equalTo(actualEntries));
    }

    @Test
    public void testRecursiveClassEnumerationWithOptionalTrailingSlash() throws IOException {
        final int scannedEntriesWithoutSlash = countJarEntriesUsingScanner("javax/ws/rs", true);
        final int scannedEntriesWithSlash = countJarEntriesUsingScanner("javax/ws/rs/", true);
        assertThat("Adding a trailing slash incorrectly affects recursive scanning", scannedEntriesWithSlash,
                equalTo(scannedEntriesWithoutSlash));
    }

    @Test
    public void testNonRecursiveClassEnumerationWithOptionalTrailingSlash() throws IOException {
        final int scannedEntriesWithoutSlash = countJarEntriesUsingScanner("javax/ws/rs", false);
        final int scannedEntriesWithSlash = countJarEntriesUsingScanner("javax/ws/rs/", false);
        assertThat("Adding a trailing slash incorrectly affects recursive scanning", scannedEntriesWithSlash,
                equalTo(scannedEntriesWithoutSlash));
    }

    private int countJarEntriesByPattern(final Pattern pattern) throws IOException {
        int matchingEntries = 0;

        try (final JarFile jarFile = new JarFile(this.jaxRsApiPath)) {
            final Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                final JarEntry entry = entries.nextElement();
                if (pattern.matcher(entry.getName()).matches()) {
                    matchingEntries++;
                }
            }
        }

        return matchingEntries;
    }

    private int countJarEntriesUsingScanner(final String parent, final boolean recursive) throws IOException {
        int scannedEntryCount = 0;

        try (final InputStream jaxRsApi = new FileInputStream(this.jaxRsApiPath)) {
            final JarFileScanner jarFileScanner = new JarFileScanner(jaxRsApi, parent, recursive);
            while (jarFileScanner.hasNext()) {
                // Fetch next entry.
                jarFileScanner.next();

                // JERSEY-2175 and JERSEY-2197:
                // This test doesn't actually do anything with the input stream, but it is important that it
                // open/close the stream to simulate actual usage.  The reported defect is only exposed if you
                // call open/close in some fashion.
                try (final InputStream classStream = jarFileScanner.open()) {
                    scannedEntryCount++;
                }
            }
        }

        return scannedEntryCount;
    }

    @Theory
    public void testClassEnumerationWithNonexistentPackage(final boolean recursive) throws IOException {
        try (final InputStream jaxRsApi = new FileInputStream(this.jaxRsApiPath)) {
            final JarFileScanner jarFileScanner = new JarFileScanner(jaxRsApi, "javax/ws/r", recursive);
            assertFalse("Unexpectedly found package 'javax.ws.r' in javax.ws.rs-api", jarFileScanner.hasNext());
        }
    }

    @Theory
    public void testClassEnumerationWithClassPrefix(final boolean recursive) throws IOException {
        try (final InputStream jaxRsApi = new FileInputStream(this.jaxRsApiPath)) {
            final JarFileScanner jarFileScanner = new JarFileScanner(jaxRsApi, "javax/ws/rs/GE", recursive);
            assertFalse("Unexpectedly found package 'javax.ws.rs.GE' in javax.ws.rs-api", jarFileScanner.hasNext());
        }
    }
}
