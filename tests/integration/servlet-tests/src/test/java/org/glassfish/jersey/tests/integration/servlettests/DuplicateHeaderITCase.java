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

package org.glassfish.jersey.tests.integration.servlettests;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.external.ExternalTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * @author Libor Kramolis (libor.kramolis at oracle.com)
 */
public class DuplicateHeaderITCase extends JerseyTest {
    @Override
    protected Application configure() {
        // dummy resource config
        return new ResourceConfig();
    }

    @Override
    protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
        return new ExternalTestContainerFactory();
    }

    @Test
    public void testDuplicateHeader() throws IOException {
        testDuplicateHeaderImpl("contextPathFilter/contextPathResource");
        testDuplicateHeaderImpl("servlet/contextPathResource");
    }

    private void testDuplicateHeaderImpl(final String path) throws IOException {
        testDuplicateHeaderImpl(0, HttpURLConnection.HTTP_OK, path);
        testDuplicateHeaderImpl(1, HttpURLConnection.HTTP_OK, path);
        testDuplicateHeaderImpl(2, HttpURLConnection.HTTP_BAD_REQUEST, path);
    }

    private void testDuplicateHeaderImpl(final int headerCount, int expectedResponseCode, final String path)
            throws IOException {
        final String headerName = HttpHeaders.CONTENT_TYPE;
        URL getUrl = UriBuilder.fromUri(getBaseUri()).path(path).build().toURL();
        HttpURLConnection connection = (HttpURLConnection) getUrl.openConnection();
        try {
            connection.setRequestMethod("GET");
            for (int i = 0; i < headerCount; i++) {
                connection.addRequestProperty(headerName, "N/A");
            }
            connection.connect();
            assertEquals(path + " [" + headerName + ":" + headerCount + "x]", expectedResponseCode, connection.getResponseCode());
        } finally {
            connection.disconnect();
        }
    }

}
