/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.integration.jersey2794;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.ws.rs.core.Application;

import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.external.ExternalTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;

import org.junit.Test;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * JERSEY-2794 reproducer.
 *
 * @author Michal Gajdos
 */
public class Jersey2794ITCase extends JerseyTest {

    @Override
    protected Application configure() {
        return new Application();
    }

    @Override
    protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
        return new ExternalTestContainerFactory();
    }

    @Test
    public void mimeTempFileRemoved() throws Exception {
        final String tempDir = System.getProperty("java.io.tmpdir");

        // Get number of matching MIME*tmp files (the number should be the same at the end of the test).
        final int expectedTempFiles = matchingTempFiles(tempDir);

        final URL url = new URL(getBaseUri().toString());
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("PUT");
        connection.setRequestProperty("Accept", "text/plain");
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=XXXX_YYYY");

        connection.setDoOutput(true);
        connection.connect();

        final OutputStream outputStream = connection.getOutputStream();
        outputStream.write("--XXXX_YYYY".getBytes());
        outputStream.write('\n');
        outputStream.write("Content-Type: text/plain".getBytes());
        outputStream.write('\n');
        outputStream.write("Content-Disposition: form-data; name=\"big-part\"".getBytes());
        outputStream.write('\n');
        outputStream.write('\n');

        // Send big chunk of data.
        for (int i = 0; i < 16 * 4096; i++) {
            outputStream.write('E');
            if (i % 1024 == 0) {
                outputStream.flush();
            }
        }

        // Do NOT send end of the MultiPart message to simulate the issue.

        // Get Response ...
        assertThat("Bad Request expected", connection.getResponseCode(), is(400));

        // Make sure that the Mimepull message and it's parts have been closed and temporary files deleted.
        assertThat("Temporary mimepull files were not deleted", matchingTempFiles(tempDir), is(expectedTempFiles));

        // ... Disconnect.
        connection.disconnect();
    }

    private int matchingTempFiles(final String tempDir) throws IOException {
        return (int) Files.walk(Paths.get(tempDir)).filter(path -> {
            final String name = path.getFileName().toString();
            return name.startsWith("MIME") && name.endsWith("tmp");
        }).count();
    }
}
