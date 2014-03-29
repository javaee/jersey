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
package org.glassfish.jersey.test.external;

import java.net.URI;
import java.security.AccessController;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.internal.util.PropertiesHelper;
import org.glassfish.jersey.test.DeploymentContext;
import org.glassfish.jersey.test.ServletDeploymentContext;
import org.glassfish.jersey.test.spi.TestContainer;
import org.glassfish.jersey.test.spi.TestContainerFactory;

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
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class ExternalTestContainerFactory implements TestContainerFactory {

    /**
     * Specifies the active test container host address where application is deployed.
     * The value of the property must be a valid host name or IP address.
     * <p />
     * There is no default value.
     * <p />
     * The name of the configuration property is <tt>{@value}</tt>.
     */
    // TODO rename to jersey.config.test.external.container.host
    public static final String JERSEY_TEST_HOST = "jersey.test.host";

    @Override
    public TestContainer create(final URI baseUri, final DeploymentContext context) throws IllegalArgumentException {
        return new ExternalTestContainer(getBaseURI(baseUri), context);
    }

    private URI getBaseURI(final URI baseUri) {
        String stagingHostName = AccessController.doPrivileged(PropertiesHelper.getSystemProperty(JERSEY_TEST_HOST));
        if (stagingHostName != null) {
            return UriBuilder.fromUri(baseUri).host(stagingHostName).build();
        }

        return baseUri;
    }

    /**
     * Class which helps running tests on an external container. It assumes that
     * the container is started/stopped explicitly and also that the application is
     * pre-deployed.
     */
    private static class ExternalTestContainer implements TestContainer {
        private static final Logger LOGGER = Logger.getLogger(ExternalTestContainer.class.getName());

        private final URI baseUri;

        private ExternalTestContainer(final URI baseUri, final DeploymentContext context) {
            final UriBuilder uriBuilder = UriBuilder.fromUri(baseUri).path(context.getContextPath());
            if (context instanceof ServletDeploymentContext) {
                uriBuilder.path(((ServletDeploymentContext) context).getServletPath());
            }

            this.baseUri = uriBuilder.build();

            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.info("Creating ExternalTestContainer configured at the base URI " + this.baseUri);
            }
        }

        @Override
        public ClientConfig getClientConfig() {
            return null;
        }

        @Override
        public URI getBaseUri() {
            return baseUri;
        }

        @Override
        public void start() {
            // do nothing
        }

        @Override
        public void stop() {
            // do nothing
        }

    }

}
