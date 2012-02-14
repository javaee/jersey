/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.test.inmemory;

import org.glassfish.jersey.server.Application;
import org.glassfish.jersey.test.inmemory.internal.InMemoryTransport;
import org.glassfish.jersey.test.spi.TestContainer;
import org.glassfish.jersey.test.spi.TestContainerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientFactory;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * In-memory test container factory.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class InMemoryTestContainerFactory implements TestContainerFactory {

    private static class InMemoryContainer implements TestContainer {
        private final URI baseUri;
        private final Application application;

        private static final Logger LOGGER = Logger.getLogger(InMemoryContainer.class.getName());

        private InMemoryContainer(final URI baseUri, final Application application) {
            this.baseUri = baseUri;
            this.application = application;
        }

        @Override
        public Client getClient() {
            return ClientFactory.
                    newClientBy(org.glassfish.jersey.client.Client.Builder.Factory.class).
                    transport(new InMemoryTransport(baseUri, application)).build();
        }

        @Override
        public URI getBaseUri() {
            return baseUri;
        }

        @Override
        public void start() {
            if(LOGGER.isLoggable(Level.INFO)) {
                LOGGER.log(Level.INFO, "Starting InMemoryContainer...");
            }
        }

        @Override
        public void stop() {
            if(LOGGER.isLoggable(Level.INFO)) {
                LOGGER.log(Level.INFO, "Stopping InMemoryContainer...");
            }
        }
    }

    @Override
    public TestContainer create(final URI baseUri, final Application application) throws IllegalArgumentException {
        return new InMemoryContainer(baseUri, application);
    }
}
