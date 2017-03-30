/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2016 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.resilient.connector;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Configuration;

import org.glassfish.jersey.client.ClientRequest;
import org.glassfish.jersey.client.ClientResponse;
import org.glassfish.jersey.client.spi.Connector;
import org.glassfish.jersey.client.spi.ConnectorProvider;
import org.glassfish.jersey.internal.util.Property;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixObservableCommand;

/**
 * A <code>ResilientConnectorProvider</code> adds functionality to another {@link ConnectorProvider} namely,
 *
 * <ul>
 * <li>Circuit breaker pattern.</li>
 * <li>Graceful degradation through fallback mechanism.</li>
 * </ul>
 *
 * @author Joel Chengottusseriyil
 */
public class ResilientConnectorProvider implements ConnectorProvider {

    /**
     * A {@link ResilientConnectorProvider.HystrixCommandConfigProvider command config provider} instance to be
     * used to configure the Hystrix command for the request.
     *
     * The value MUST be an instance implementing the  {@link ResilientConnectorProvider.HystrixCommandConfigProvider} SPI.
     * <p>
     * A default value is set to {@link DefaultHystrixCommandConfigProvider}.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     *
     * @see org.glassfish.jersey.resilient.connector.ResilientConnectorProvider.HystrixCommandConfigProvider
     */
    @Property
    public static final String HYSTRIX_COMMAND_CONFIG_PROVIDER = "jersey.config.client.hystrix.command.config.provider";

    /**
     * A {@link ResilientConnectorProvider.HystrixCommandFallback Hystrix command fallback} instance to be
     * used to fallback when the Hystrix command fails.
     *
     * The value MUST be an instance implementing the  {@link ResilientConnectorProvider.HystrixCommandFallback} SPI.
     * <p>
     * A default value is not set (is {@code null}).
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     *
     * @see org.glassfish.jersey.resilient.connector.ResilientConnectorProvider.HystrixCommandFallback
     */
    @Property
    public static final String HYSTRIX_COMMAND_FALLBACK = "jersey.config.client.hystrix.command.fallback";

    private final ConnectorProvider delegateConnectorProvider;

    /**
     * A contract that provides {@link HystrixCommand.Setter} and {@link HystrixObservableCommand.Setter} configurations
     * in the context of the request.
     *
     * These configurations will be applied to configure the {@link HystrixCommand} and {@link HystrixObservableCommand}.
     */
    public static interface HystrixCommandConfigProvider {

        /**
         * Gets the {@link HystrixCommandKey#name() Hystrix command name} for the request.
         *
         * @param requestContext Jersey client request for which the Hystrix command is being built.
         * @return Hystrix command name.
         *
         * @see com.netflix.hystrix.HystrixCommandKey
         */
        public String commandName(final ClientRequest requestContext);

        /**
         * Gets an instance of {@link HystrixCommand.Setter} configuration to be used for creating
         * the Hystrix command for the request.
         *
         * @param requestContext Jersey client request for which the Hystrix command is being built.
         * @return An instance of {@link HystrixCommand.Setter}.
         */
        public HystrixCommand.Setter commandConfig(final ClientRequest requestContext);

        /**
         * Gets an instance of {@link HystrixObservableCommand.Setter} configuration to be used for creating
         * the Hystrix observable command for the request.
         *
         * @param requestContext Jersey client request for which the Hystrix command is being built.
         * @return An instance of {@link HystrixObservableCommand.Setter}.
         */
        public HystrixObservableCommand.Setter observableCommandConfig(final ClientRequest requestContext);

    }

    /**
     * A SPI to specify the fallback mechanism when the Hystrix command fails.
     *
     */
    public static interface HystrixCommandFallback {

        /**
         * If the Hystrix command fails in any way, then this method is be executed to get the {@link ClientResponse}.
         *
         * From Hystrix docs:
         * <p>
         * This should do work that does not require network transport to produce.
         * <p>
         * In other words, this should be a static or cached result that can immediately be returned upon failure.
         * <p>
         * <p>
         * If network traffic is wanted for fallback (such as going to MemCache) then the fallback implementation
         * should invoke another {@link HystrixCommand} instance that protects against that network
         * access and possibly has another level of fallback that does not involve network access.
         * <p>
         *
         * @param requestContext Jersey client request for which the fallback is being executed.
         * @return Jersey client response after the fallback is executed.
         */
        public ClientResponse execute(ClientRequest requestContext);
    }
    /**
     * Creates a {@code ResilientConnectorProvider} with the specified
     * {@code connectorProvider}.
     *
     * @param connectorProvider the delegate connector provider.
     */
    public ResilientConnectorProvider(final ConnectorProvider connectorProvider) {
        this.delegateConnectorProvider = connectorProvider;
    }

    @Override
    public Connector getConnector(Client client, Configuration runtimeConfig) {
        Connector delegateConnector = delegateConnectorProvider.getConnector(client, runtimeConfig);
        return new ResilientConnector(client, runtimeConfig, delegateConnector);
    }

}
