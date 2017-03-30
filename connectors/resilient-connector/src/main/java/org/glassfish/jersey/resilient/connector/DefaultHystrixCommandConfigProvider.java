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

import org.glassfish.jersey.client.ClientRequest;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixCommandProperties.ExecutionIsolationStrategy;
import com.netflix.hystrix.HystrixCommandProperties.Setter;

/**
 * Basic implementation of {@link ResilientConnectorProvider.HystrixCommandConfigProvider} which provides
 * configurations for {@link HystrixCommand} and {@link HystrixObservableCommand} with the following default configurations.
 *
 * <ul>
 *  <li>Command name - {@code request.getUri().getPath()}</li>
 *  <li>SEMAPHORE execution isolation strategy.</li>
 *  <li>Disabled execution timeout.</li>
 * </ul>
 *
 * @author Joel Chengottusseriyil
 *
 * @see com.netflix.hystrix.HystrixCommandProperties.ExecutionIsolationStrategy
 */
class DefaultHystrixCommandConfigProvider implements ResilientConnectorProvider.HystrixCommandConfigProvider {

    private static final String HYSTRIX_OBSERVABLE_COMMAND_GROUP_KEY = "JerseyClientHystrixObservableCommand";
    private static final String HYSTRIX_COMMAND_GROUP_KEY = "JerseyClientHystrixCommand";

    @Override
    public String commandName(ClientRequest requestContext) {
        return requestContext.getUri().getPath();
    }

    @Override
    public com.netflix.hystrix.HystrixCommand.Setter commandConfig(ClientRequest requestContext) {
        return com.netflix.hystrix.HystrixCommand.Setter
                .withGroupKey(HystrixCommandGroupKey.Factory.asKey(HYSTRIX_COMMAND_GROUP_KEY))
                .andCommandKey(HystrixCommandKey.Factory.asKey(commandName(requestContext)))
                .andCommandPropertiesDefaults(defaultCommandProperties());
    }

    @Override
    public com.netflix.hystrix.HystrixObservableCommand.Setter observableCommandConfig(ClientRequest requestContext) {
        return com.netflix.hystrix.HystrixObservableCommand.Setter
                .withGroupKey(HystrixCommandGroupKey.Factory.asKey(HYSTRIX_OBSERVABLE_COMMAND_GROUP_KEY))
                .andCommandKey(HystrixCommandKey.Factory.asKey(commandName(requestContext)))
                .andCommandPropertiesDefaults(defaultCommandProperties());
    }

    /**
     * Gets an instance of {@link HystrixCommandProperties.Setter} with the following default configurations,
     * <ul>
     * <li>SEMAPHORE execution isolation strategy.</li>
     * <li>Disabled execution timeout.</li>
     * </ul>
     *
     * @return An instance of Hystrix command properties.
     */
    private static Setter defaultCommandProperties() {
        return HystrixCommandProperties.Setter()
                .withExecutionIsolationStrategy(ExecutionIsolationStrategy.SEMAPHORE)
                .withExecutionTimeoutEnabled(false);
    }
}
