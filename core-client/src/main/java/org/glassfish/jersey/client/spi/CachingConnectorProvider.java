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
package org.glassfish.jersey.client.spi;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Configuration;

/**
 * Caching connector provider.
 *
 * This utility provider can be used to serve as a lazily initialized provider of the same connector instance.
 * <p>
 * Note however that the connector instance will be configured using the runtime configuration of the first client instance that
 * has invoked the {@link #getConnector(javax.ws.rs.client.Client, javax.ws.rs.core.Configuration)} method.
 * {@link javax.ws.rs.client.Client} and {@link javax.ws.rs.core.Configuration} instance passed to subsequent
 * {@code getConnector(...)} invocations will be ignored. As such, this connector provider should not be shared among client
 * instances that have significantly different connector-specific settings.
 * </p>
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @since 2.10
 */
public class CachingConnectorProvider implements ConnectorProvider {

    private final ConnectorProvider delegate;
    private Connector connector;


    /**
     * Create the caching connector provider.
     *
     * @param delegate delegate connector provider that will be used to initialize and create the connector instance which
     *                 will be subsequently cached and reused.
     */
    public CachingConnectorProvider(final ConnectorProvider delegate) {
        this.delegate = delegate;
    }

    @Override
    public synchronized Connector getConnector(Client client, Configuration runtimeConfig) {
        if (connector == null) {
            connector = delegate.getConnector(client, runtimeConfig);
        }
        return connector;
    }
}
