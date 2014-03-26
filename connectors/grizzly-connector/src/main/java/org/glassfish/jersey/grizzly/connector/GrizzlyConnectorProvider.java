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
package org.glassfish.jersey.grizzly.connector;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Configurable;
import javax.ws.rs.core.Configuration;

import org.glassfish.jersey.client.Initializable;
import org.glassfish.jersey.client.spi.Connector;
import org.glassfish.jersey.client.spi.ConnectorProvider;

import com.ning.http.client.AsyncHttpClient;

/**
 * Connector provider for Grizzly asynchronous HTTP client-based connectors.
 * <p>
 * Connectors created by this provider use {@link org.glassfish.jersey.client.RequestEntityProcessing#CHUNKED
 * chunked encoding} as a default setting. This can be overridden by setting the
 * {@link org.glassfish.jersey.client.ClientProperties#REQUEST_ENTITY_PROCESSING} property.
 * </p>
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @since 2.5
 */
public class GrizzlyConnectorProvider implements ConnectorProvider {
    @Override
    public Connector getConnector(Client client, Configuration config) {
        return new GrizzlyConnector(client, config);
    }

    /**
     * Retrieve the underlying Grizzly {@link AsyncHttpClient} instance from
     * {@link org.glassfish.jersey.client.JerseyClient} or {@link org.glassfish.jersey.client.JerseyWebTarget}
     * configured to use {@code GrizzlyConnectorProvider}.
     *
     * @param component {@code JerseyClient} or {@code JerseyWebTarget} instance that is configured to use
     *                  {@code GrizzlyConnectorProvider}.
     * @return underlying Grizzly {@code AsyncHttpClient} instance.
     *
     * @throws java.lang.IllegalArgumentException in case the {@code component} is neither {@code JerseyClient}
     *                                            nor {@code JerseyWebTarget} instance or in case the component
     *                                            is not configured to use a {@code GrizzlyConnectorProvider}.
     * @since 2.8
     */
    public static AsyncHttpClient getHttpClient(Configurable<?> component) {
        if (!(component instanceof Initializable)) {
            throw new IllegalArgumentException(
                    LocalizationMessages.INVALID_CONFIGURABLE_COMPONENT_TYPE(component.getClass().getName()));
        }

        final Initializable<?> initializable = (Initializable<?>) component;
        Connector connector = initializable.getConfiguration().getConnector();
        if (connector == null) {
            initializable.preInitialize();
            connector = initializable.getConfiguration().getConnector();
        }

        if (connector instanceof GrizzlyConnector) {
            return ((GrizzlyConnector) connector).getGrizzlyClient();
        }

        throw new IllegalArgumentException(LocalizationMessages.EXPECTED_CONNECTOR_PROVIDER_NOT_USED());
    }
}
