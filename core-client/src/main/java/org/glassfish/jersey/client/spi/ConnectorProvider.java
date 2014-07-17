/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2014 Oracle and/or its affiliates. All rights reserved.
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
 * Jersey client connector provider contract.
 *
 * Connector provider is invoked by Jersey client runtime to provide a client connector
 * to be used to send client requests over the wire to server-side resources.
 * There can be only one connector provider registered in a single Jersey client instance.
 * <p>
 * Note that unlike most of the other {@link org.glassfish.jersey.spi.Contract Jersey SPI extension contracts},
 * {@code ConnectorProvider} is not a typical runtime extension and as such cannot be registered
 * using a configuration {@code register(...)} method. Instead, it must be registered using via
 * {@link org.glassfish.jersey.client.JerseyClientBuilder} using it's
 * {@link org.glassfish.jersey.client.ClientConfig#connectorProvider(ConnectorProvider)}
 * initializer method.
 * </p>
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @since 2.5
 */
// Must not be annotated with @Contract
public interface ConnectorProvider {

    /**
     * Get a Jersey client connector instance for a given {@link Client client} instance
     * and Jersey client runtime {@link Configuration configuration}.
     * <p>
     * Note that the supplied runtime configuration can be different from the client instance
     * configuration as a single client can be used to serve multiple differently configured runtimes.
     * While the {@link Client#getSslContext() SSL context} or {@link Client#getHostnameVerifier() hostname verifier}
     * are shared, other configuration properties may change in each runtime.
     * </p>
     * <p>
     * Based on the supplied client and runtime configuration data, it is up to each connector provider
     * implementation to decide whether a new dedicated connector instance is required or if the existing,
     * previously create connector instance can be reused.
     * </p>
     *
     * @param client        Jersey client instance.
     * @param runtimeConfig Jersey client runtime configuration.
     * @return configured {@link org.glassfish.jersey.client.spi.Connector} instance to be used by the client.
     */
    public Connector getConnector(Client client, Configuration runtimeConfig);
}
