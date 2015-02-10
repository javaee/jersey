/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.test.jdkhttp;

import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.test.DeploymentContext;
import org.glassfish.jersey.test.spi.TestContainer;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.glassfish.jersey.test.spi.TestHelper;

import com.sun.net.httpserver.HttpServer;

/**
 * Factory for testing {@link org.glassfish.jersey.jdkhttp.JdkHttpHandlerContainer}.
 *
 * @author Miroslav Fuksa
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class JdkHttpServerTestContainerFactory implements TestContainerFactory {

    private static class JdkHttpServerTestContainer implements TestContainer {

        private URI baseUri;
        private final HttpServer server;
        private final AtomicBoolean started = new AtomicBoolean(false);
        private static final Logger LOGGER = Logger.getLogger(JdkHttpServerTestContainer.class.getName());

        private JdkHttpServerTestContainer(final URI baseUri, final DeploymentContext context) {
            this.baseUri = UriBuilder.fromUri(baseUri).path(context.getContextPath()).build();

            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.info("Creating JdkHttpServerTestContainer configured at the base URI "
                        + TestHelper.zeroPortToAvailablePort(baseUri));
            }

            this.server = JdkHttpServerFactory.createHttpServer(this.baseUri, context.getResourceConfig(), false);
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
            if (started.compareAndSet(false, true)) {
                LOGGER.log(Level.FINE, "Starting JdkHttpServerTestContainer...");
                server.start();

                if (baseUri.getPort() == 0) {
                    baseUri = UriBuilder.fromUri(baseUri)
                            .port(server.getAddress().getPort())
                            .build();
                    LOGGER.log(Level.INFO, "Started JdkHttpServerTestContainer at the base URI " + baseUri);
                }
            } else {
                LOGGER.log(Level.WARNING, "Ignoring start request - JdkHttpServerTestContainer is already started.");
            }
        }

        @Override
        public void stop() {
            if (started.compareAndSet(true, false)) {
                LOGGER.log(Level.FINE, "Stopping JdkHttpServerTestContainer...");
                this.server.stop(3);
            } else {
                LOGGER.log(Level.WARNING, "Ignoring stop request - JdkHttpServerTestContainer is already stopped.");
            }
        }
    }

    @Override
    public TestContainer create(final URI baseUri, final DeploymentContext context) throws IllegalArgumentException {
        return new JdkHttpServerTestContainer(baseUri, context);
    }
}
