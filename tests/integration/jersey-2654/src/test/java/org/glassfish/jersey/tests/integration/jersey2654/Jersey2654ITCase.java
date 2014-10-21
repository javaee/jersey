/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.tests.integration.jersey2654;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

import javax.ws.rs.core.Application;

import org.glassfish.jersey.test.JerseyTest;

import org.glassfish.jersey.test.external.ExternalTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Reproducer for JERSEY-2654
 *
 * Tests, that unencoded curly brackets (typically used in URI queries containing JSON) do not cause the request to
 * fail when running in a servlet environment and configured as a filter.
 *
 * @author Adam Lindenthal (adam.lindenthal at oracle.com)
 */
public class Jersey2654ITCase extends JerseyTest {

    @Override
    protected Application configure() {
        return new TestApplication();
    }

    @Override
    protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
        return new ExternalTestContainerFactory();
    }

    @Test
    public void testJsonInUriWithSockets() throws IOException {
        // Low level approach with sockets is used, because common Java HTTP clients are using java.net.URI,
        // which fails when unencoded curly bracket is part of the URI
        final Socket socket = new Socket(getBaseUri().getHost(), getBaseUri().getPort());
        final PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())));

        // quotes are encoded by browsers, curly brackets are not, so the quotes will be sent pre-encoded
        // HTTP 1.0 is used for simplicity
        pw.println("GET /filter?json={%22foo%22:%22bar%22} HTTP/1.0");
        pw.println();   // http request should end with a blank line
        pw.flush();

        final BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        String lastLine = null;
        String line;
        while ((line = br.readLine()) != null) {
            // read the response and remember the last line
            lastLine = line;
        }
        assertEquals("{\"foo\":\"bar\"}", lastLine);
        br.close();
    }
}
