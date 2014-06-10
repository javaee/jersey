/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2014 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.test.simple;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.simple.SimpleContainerFactory;
import org.glassfish.jersey.simple.SimpleServer;
import org.glassfish.jersey.test.DeploymentContext;
import org.glassfish.jersey.test.spi.TestContainer;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;

/**
 * Factory for testing {@link org.glassfish.jersey.simple.SimpleContainer}.
 *
 * @author Arul Dhesiaseelan (aruld@acm.org)
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class SimpleTestContainerFactory implements TestContainerFactory {

    private static class SimpleTestContainer implements TestContainer {

        private static final Logger LOGGER = Logger.getLogger(SimpleTestContainer.class.getName());

        private final DeploymentContext deploymentContext;

        private URI baseUri;
        private SimpleServer server;

        private SimpleTestContainer(final URI baseUri, final DeploymentContext context) {
            final URI base = UriBuilder.fromUri(baseUri).path(context.getContextPath()).build();

            if (!"/".equals(base.getRawPath())) {
                throw new TestContainerException(String.format(
                        "Cannot deploy on %s. Simple framework container only supports deployment on root path.",
                        base.getRawPath()));
            }

            this.baseUri = base;

            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.info("Creating SimpleTestContainer configured at the base URI " + this.baseUri);
            }
            this.deploymentContext = context;
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
            LOGGER.log(Level.FINE, "Starting SimpleTestContainer...");

            try {
                server = SimpleContainerFactory.create(baseUri, deploymentContext.getResourceConfig());

                if (baseUri.getPort() == 0) {
                    baseUri = UriBuilder.fromUri(baseUri)
                            .port(server.getPort())
                            .build();

                    LOGGER.log(Level.INFO, "Started SimpleTestContainer at the base URI " + baseUri);
                }
            } catch (ProcessingException e) {
                throw new TestContainerException(e);
            }
        }

        @Override
        public void stop() {
            LOGGER.log(Level.FINE, "Stopping SimpleTestContainer...");
            try {
                this.server.close();
            } catch (IOException ex) {
                LOGGER.log(Level.WARNING, "Error Stopping SimpleTestContainer...", ex);
            } finally {
                this.server = null;
            }
        }
    }

    @Override
    public TestContainer create(final URI baseUri, final DeploymentContext context) throws IllegalArgumentException {
        return new SimpleTestContainer(baseUri, context);
    }
}
