/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.server.internal.scanning;

import java.io.Closeable;
import java.io.InputStream;
import java.net.URI;
import java.util.Enumeration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.glassfish.jersey.server.ResourceFinder;

import org.jboss.vfs.TempFileProvider;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;
import org.junit.Before;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Martin Snyder
 * @author Jason T. Greene
 */
public class VFSSchemeResourceFinderTest {

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

    /**
     * Test case for JERSEY-2197, JERSEY-2175.
     */
    @Test
    public void testClassEnumeration() throws Exception {
        // Count actual entries.

        int actualEntries = 0;
        try (JarFile jarFile = new JarFile(jaxRsApiPath)) {
            final Enumeration<JarEntry> entries = jarFile.entries();

            while (entries.hasMoreElements()) {
                final JarEntry entry = entries.nextElement();

                if (entry.getName().endsWith(".class")) {
                    actualEntries++;
                }
            }
        }

        // Scan entries using VFS scanner.
        final VirtualFile mountDir = VFS.getChild("content");
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        try (TempFileProvider provider = TempFileProvider.create("test", executor, false);
             Closeable mount = VFS.mountZip(VFS.getChild(jaxRsApiPath), mountDir, provider)) {

            ResourceFinder finder = new VfsSchemeResourceFinderFactory()
                    .create(new URI(mountDir.toURI().toString() + "/javax/ws/rs"), true);

            int scannedEntryCount = 0;
            while (finder.hasNext()) {
                // Fetch next entry.
                finder.next();

                // This test doesn't actually do anything with the input stream, but it is important that it
                // open/close the stream to simulate actual usage.  The reported defect is only exposed if you
                // call open/close in some fashion.

                try (InputStream classStream = finder.open()) {
                    scannedEntryCount++;
                }
            }

            assertThat("Failed to enumerate all contents of javax.ws.rs-api.", scannedEntryCount, equalTo(actualEntries));
        } finally {
            executor.shutdownNow();
        }
    }
}
