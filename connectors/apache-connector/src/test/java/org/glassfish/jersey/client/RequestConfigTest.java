/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.client;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.params.*;
import org.glassfish.jersey.apache.connector.ApacheClientProperties;
import org.glassfish.jersey.apache.connector.ApacheConnector;
import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Arul Dhesiaseelan (aruld at acm.org)
 */
@SuppressWarnings("deprecation")
public class RequestConfigTest extends JerseyTest {
    private static final Logger LOGGER = Logger.getLogger(RequestConfigTest.class.getName());

    @Path("/test")
    public static class HttpMethodResource {
        @GET
        public String get() {
            return "GET";
        }
    }

    @Override
    protected Application configure() {
        ResourceConfig config = new ResourceConfig(HttpMethodResource.class);
        config.register(new LoggingFilter(LOGGER, true));
        return config;
    }

    private Client createClientWithDeprecatedHttpParams() {
        ClientConfig config = new ClientConfig();
        HttpParams params = new BasicHttpParams();
        params.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 5000);
        params.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 5000);
        params.setBooleanParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, Boolean.FALSE);
        params.setBooleanParameter(CoreProtocolPNames.USE_EXPECT_CONTINUE, Boolean.TRUE);
        params.setBooleanParameter(ClientPNames.HANDLE_AUTHENTICATION, Boolean.FALSE);
        params.setBooleanParameter(ClientPNames.ALLOW_CIRCULAR_REDIRECTS, Boolean.TRUE);
        params.setIntParameter(ClientPNames.CONN_MANAGER_TIMEOUT, 5000);
        params.setIntParameter(ClientPNames.MAX_REDIRECTS, 100);
        params.setBooleanParameter(ClientPNames.REJECT_RELATIVE_REDIRECT, Boolean.TRUE);
        params.setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, Boolean.FALSE);
        config.property(ApacheClientProperties.HTTP_PARAMS, params);
        return ClientBuilder.newClient(config.connector(new ApacheConnector(config.getConfiguration())));
    }

    private Client createClientWithRequestConfig() {
        ClientConfig config = new ClientConfig();
        RequestConfig.Builder requestConfigBuilder = RequestConfig.custom();
        requestConfigBuilder.setSocketTimeout(5000);
        requestConfigBuilder.setConnectTimeout(5000);
        requestConfigBuilder.setStaleConnectionCheckEnabled(false);
        requestConfigBuilder.setExpectContinueEnabled(true);
        requestConfigBuilder.setAuthenticationEnabled(false);
        requestConfigBuilder.setCircularRedirectsAllowed(true);
        requestConfigBuilder.setConnectionRequestTimeout(5000);
        requestConfigBuilder.setMaxRedirects(100);
        requestConfigBuilder.setRelativeRedirectsAllowed(false);
        requestConfigBuilder.setRedirectsEnabled(false);
        config.property(ApacheClientProperties.REQUEST_CONFIG, requestConfigBuilder.build());
        return ClientBuilder.newClient(config.connector(new ApacheConnector(config.getConfiguration())));
    }

    @Test
    public void testRequestConfig() {
        Client client = createClientWithDeprecatedHttpParams();
        WebTarget target = client.target(getBaseUri()).path("test");
        Response response = target.request().get();
        assertEquals("GET", response.readEntity(String.class));
        response.close();
        HttpClientContext context = (HttpClientContext) ((ScopedJaxrsResponse)response).getRequestContext().getPropertiesDelegate().getProperty(ApacheConnector.CLIENT_CONTEXT);
        assertEquals(5000, context.getRequestConfig().getSocketTimeout());
        assertEquals(5000, context.getRequestConfig().getConnectTimeout());
        assertFalse(context.getRequestConfig().isStaleConnectionCheckEnabled());
        assertTrue(context.getRequestConfig().isExpectContinueEnabled());
        assertFalse(context.getRequestConfig().isAuthenticationEnabled());
        assertTrue(context.getRequestConfig().isCircularRedirectsAllowed());
        assertEquals(5000, context.getRequestConfig().getConnectionRequestTimeout());
        assertEquals(100, context.getRequestConfig().getMaxRedirects());
        assertFalse(context.getRequestConfig().isRelativeRedirectsAllowed());
        assertFalse(context.getRequestConfig().isRedirectsEnabled());
        client.close();

        client = createClientWithRequestConfig();
        target = client.target(getBaseUri()).path("test");
        response = target.request().get();
        assertEquals("GET", response.readEntity(String.class));
        response.close();
        context = (HttpClientContext) ((ScopedJaxrsResponse)response).getRequestContext().getPropertiesDelegate().getProperty(ApacheConnector.CLIENT_CONTEXT);
        assertEquals(5000, context.getRequestConfig().getSocketTimeout());
        assertEquals(5000, context.getRequestConfig().getConnectTimeout());
        assertFalse(context.getRequestConfig().isStaleConnectionCheckEnabled());
        assertTrue(context.getRequestConfig().isExpectContinueEnabled());
        assertFalse(context.getRequestConfig().isAuthenticationEnabled());
        assertTrue(context.getRequestConfig().isCircularRedirectsAllowed());
        assertEquals(5000, context.getRequestConfig().getConnectionRequestTimeout());
        assertEquals(100, context.getRequestConfig().getMaxRedirects());
        assertFalse(context.getRequestConfig().isRelativeRedirectsAllowed());
        assertFalse(context.getRequestConfig().isRedirectsEnabled());
        client.close();

    }

}
