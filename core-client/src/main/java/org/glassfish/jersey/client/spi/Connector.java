/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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

import java.util.concurrent.Future;

import org.glassfish.jersey.client.ClientRequest;
import org.glassfish.jersey.client.ClientResponse;
import org.glassfish.jersey.process.Inflector;

/**
 * Client transport connector extension contract.
 * <p>
 * Note that unlike most of the other {@link org.glassfish.jersey.spi.Contract Jersey SPI extension contracts},
 * Jersey {@code Connector} is not a typical runtime extension and as such cannot be directly registered
 * using a configuration {@code register(...)} method. Jersey client runtime retrieves a {@code Connector}
 * instance upon Jersey client runtime initialization using a {@link org.glassfish.jersey.client.spi.ConnectorProvider}
 * registered in {@link org.glassfish.jersey.client.ClientConfig}.
 * </p>
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
// Must not be annotated with @Contract
public interface Connector extends Inflector<ClientRequest, ClientResponse> {
    /**
     * Synchronously process client request into a response.
     *
     * The method is used by Jersey client runtime to synchronously send a request
     * and receive a response.
     *
     * @param request Jersey client request to be sent.
     * @return Jersey client response received for the client request.
     * @throws javax.ws.rs.ProcessingException in case of any invocation failure.
     */
    @Override
    ClientResponse apply(ClientRequest request);

    /**
     * Asynchronously process client request into a response.
     *
     * The method is used by Jersey client runtime to asynchronously send a request
     * and receive a response.
     *
     * @param request  Jersey client request to be sent.
     * @param callback Jersey asynchronous connector callback to asynchronously receive
     *                 the request processing result (either a response or a failure).
     * @return asynchronously executed task handle.
     */
    Future<?> apply(ClientRequest request, AsyncConnectorCallback callback);

    /**
     * Get name of current connector.
     *
     * Should contain identification of underlying specification and optionally version number.
     * Will be used in User-Agent header.
     *
     * @return name of current connector. Returning {@code null} or empty string means not including
     * this information in a generated <tt>{@value javax.ws.rs.core.HttpHeaders#USER_AGENT}</tt> header.
     */
    public String getName();

    /**
     * Close connector and release all it's internally associated resources.
     */
    public void close();
}
