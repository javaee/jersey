/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015-2017 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.tests.e2e.client.connector.ssl;

import java.util.Arrays;
import java.util.List;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.client.spi.ConnectorProvider;
import org.glassfish.jersey.grizzly.connector.GrizzlyConnectorProvider;
import org.glassfish.jersey.jetty.connector.JettyConnectorProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * SSL connector hostname verification tests.
 *
 * @author Petr Bouda (petr.bouda at oracle.com)
 */
@RunWith(Parameterized.class)
public class SslConnectorHostnameVerifierTest extends AbstractConnectorServerTest {

    private static final String CLIENT_TRUST_STORE = "truststore-example_com-client";
    private static final String SERVER_KEY_STORE = "keystore-example_com-server";

    @Override
    protected String serverKeyStore() {
        return SERVER_KEY_STORE;
    }

    @Override
    protected String clientTrustStore() {
        return CLIENT_TRUST_STORE;
    }

    /**
     * Test to apply {@link HostnameVerifier} along with SSL in the predefined connectors
     *
     * @throws Exception in case of a test failure.
     */
    @Test
    public void testHostnameVerifierApplied() throws Exception {
        // Grizzly and Jetty connectors don't support Hostname Verification
        if (isExcluded(Arrays.asList(GrizzlyConnectorProvider.class, JettyConnectorProvider.class))) {
            return;
        }

        final Client client = ClientBuilder.newBuilder()
                .withConfig(new ClientConfig().connectorProvider(connectorProvider))
                .register(HttpAuthenticationFeature.basic("user", "password"))
                .hostnameVerifier(new CustomHostnameVerifier())
                .sslContext(getSslContext())
                .build();

        try {
            client.target(Server.BASE_URI).request().get(Response.class);
            fail("HostnameVerifier was not applied.");
        } catch (ProcessingException pex) {
            CustomHostnameVerifier.HostnameVerifierException hve = getHVE(pex);

            if (hve != null) {
                assertEquals(CustomHostnameVerifier.EX_VERIFIER_MESSAGE, hve.getMessage());
            } else {
                fail("Invalid wrapped exception.");
            }
        }
    }

    private boolean isExcluded(List<Class<? extends ConnectorProvider>> excluded) {
        for (Class<?> clazz : excluded) {
            if (clazz.isAssignableFrom(connectorProvider.getClass())) {
                return true;
            }
        }

        return false;
    }

    private static CustomHostnameVerifier.HostnameVerifierException getHVE(final Throwable stacktrace) {
        Throwable temp = stacktrace;
        do {
            temp = temp.getCause();
            if (temp instanceof CustomHostnameVerifier.HostnameVerifierException) {
                return (CustomHostnameVerifier.HostnameVerifierException) temp;
            }
        } while (temp != null);
        return null;
    }

    public static class CustomHostnameVerifier implements HostnameVerifier {

        private static final String EX_VERIFIER_MESSAGE = "Verifier Applied";

        @Override
        public boolean verify(final String s, final SSLSession sslSession) {
            throw new HostnameVerifierException(EX_VERIFIER_MESSAGE);
        }

        @Override
        public final String toString() {
            return "CUSTOM_HOST_VERIFIER";
        }

        public static class HostnameVerifierException extends RuntimeException {

            public HostnameVerifierException(final String message) {
                super(message);
            }
        }
    }
}
