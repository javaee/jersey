/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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

import java.net.URI;
import java.util.Enumeration;
import java.util.Map;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.internal.PropertiesDelegate;
import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.message.internal.OutboundMessageContext;

/**
 * Jersey client request context.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class JerseyClientRequestContext extends OutboundMessageContext implements ClientRequestContext {
    // Executing client instance
    private final JerseyClient client;
    // Request-scoped configuration instance
    private final JerseyConfiguration configuration;
    // Request-scoped properties delegate
    private final PropertiesDelegate propertiesDelegate;
    // Absolute request URI
    private URI requestUri;
    // Request method
    private String httpMethod;
    // Request filter chain execution aborting response
    private Response abortResponse;
    // Entity providers
    private MessageBodyWorkers workers;

    /**
     * Create new Jersey client request context.
     *
     * @param client             executing Jersey client instance.
     * @param configuration      request configuration.
     * @param propertiesDelegate properties delegate.
     */
    public JerseyClientRequestContext(
            JerseyClient client, JerseyConfiguration configuration, PropertiesDelegate propertiesDelegate) {
        this.client = client;
        this.configuration = configuration;
        this.propertiesDelegate = propertiesDelegate;
    }

    @Override
    public Object getProperty(String name) {
        return propertiesDelegate.getProperty(name);
    }

    @Override
    public Enumeration<String> getPropertyNames() {
        return propertiesDelegate.getPropertyNames();
    }

    @Override
    public void setProperty(String name, Object object) {
        propertiesDelegate.setProperty(name, object);
    }

    @Override
    public void removeProperty(String name) {
        propertiesDelegate.removeProperty(name);
    }

    /**
     * Get the underlying properties delegate.
     *
     * @return underlying properties delegate.
     */
    PropertiesDelegate getPropertiesDelegate() {
        return propertiesDelegate;
    }

    @Override
    public URI getUri() {
        return requestUri;
    }

    @Override
    public void setUri(URI uri) {
        this.requestUri = uri;
    }

    @Override
    public String getMethod() {
        return httpMethod;
    }

    @Override
    public void setMethod(String method) {
        this.httpMethod = method;
    }

    @Override
    public JerseyClient getClient() {
        return client;
    }

    @Override
    public void abortWith(Response response) {
        // TODO: implement special handling of "outbound" response type?
        this.abortResponse = response;
    }

    /**
     * Get the request filter chain aborting response if set, or {@code null} otherwise.
     *
     * @return request filter chain aborting response if set, or {@code null} otherwise.
     */
    public Response getAbortResponse() {
        return abortResponse;
    }

    @Override
    public JerseyConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public Map<String, Cookie> getCookies() {
        return super.getRequestCookies();
    }

    /**
     * Get the message body workers associated with the request.
     *
     * @return message body workers.
     */
    public MessageBodyWorkers getWorkers() {
        return workers;
    }

    /**
     * Set the message body workers associated with the request.
     *
     * @param workers message body workers.
     */
    public void setWorkers(MessageBodyWorkers workers) {
        // TODO use this initializer method in the request processing
        this.workers = workers;
    }
}
