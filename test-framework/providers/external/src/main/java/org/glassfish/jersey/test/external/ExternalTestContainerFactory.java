/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.test.external;

import org.glassfish.jersey.client.JerseyClient;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.test.spi.TestContainer;
import org.glassfish.jersey.test.spi.TestContainerFactory;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;

/**
 * A Web-based test container factory for creating test container instances
 * when the Web application is independently deployed in a separate JVM to that
 * of the tests. For example, the application may be deployed to the
 * Glassfish v2 or v3 application server.
 * <P>
 * If you would like to run your tests on a staging server, just set the machine's
 * IP address or fully-qualified domain name to the System Property <I>jersey.test.host</I>.
 *
 * @author Srinivas Bhimisetty
 */
public class ExternalTestContainerFactory implements TestContainerFactory {

    @Override
    public TestContainer create(URI baseUri, ApplicationHandler application) throws IllegalArgumentException {
        return new ExternalTestContainer(getBaseURI(baseUri));
    }

    private URI getBaseURI(URI baseUri) {
        String stagingHostName = System.getProperty("jersey.test.host");
        if (stagingHostName != null) {
            return UriBuilder.fromUri(baseUri)
                .host(stagingHostName).build();
        }

        return baseUri;
    }

    /**
     * Class which helps running tests on an external container. It assumes that
     * the container is started/stopped explicitly and also that the application is
     * pre-deployed.
     */
    private static class ExternalTestContainer implements TestContainer {

        final URI baseUri;

        private ExternalTestContainer(URI baseUri) {
            this.baseUri = UriBuilder.fromUri(baseUri).build();
        }

        public JerseyClient getClient() {
            return null;
        }

        public URI getBaseUri() {
            return baseUri;
        }

        public void start() {
            // do nothing
        }

        public void stop() {
            // do nothing
        }

    }

}
