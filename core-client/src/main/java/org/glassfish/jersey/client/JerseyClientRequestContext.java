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
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;

import org.glassfish.jersey.internal.MapPropertiesDelegate;
import org.glassfish.jersey.internal.PropertiesDelegate;
import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.message.internal.OutboundMessageContext;

/**
 * Jersey client request context.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
// TODO complete javadoc on class
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
     * @param requestUri         request Uri.
     * @param client             executing Jersey client instance.
     * @param configuration      request configuration.
     * @param propertiesDelegate properties delegate.
     */
    public JerseyClientRequestContext(
            URI requestUri, JerseyClient client, JerseyConfiguration configuration, PropertiesDelegate propertiesDelegate) {
        this.requestUri = requestUri;
        this.client = client;
        this.configuration = configuration;
        this.propertiesDelegate = propertiesDelegate;
    }

    /**
     * Copy constructor.
     *
     * @param original original instance.
     */
    public JerseyClientRequestContext(JerseyClientRequestContext original) {
        super(original);
        this.requestUri = original.requestUri;
        this.httpMethod = original.httpMethod;
        this.client = original.client;
        this.workers = original.workers;
        this.configuration = original.configuration.snapshot();

        this.propertiesDelegate = new MapPropertiesDelegate(original.propertiesDelegate);
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
        this.workers = workers;
    }

    public void accept(MediaType... types) {
        headers(HttpHeaders.ACCEPT, (Object[]) types);
    }

    public void accept(String... types) {
        headers(HttpHeaders.ACCEPT, types);
    }

    public void acceptLanguage(Locale... locales) {
        headers(HttpHeaders.ACCEPT_LANGUAGE, locales);
    }

    public void acceptLanguage(String... locales) {
        headers(HttpHeaders.ACCEPT_LANGUAGE, locales);
    }

    public void cookie(Cookie cookie) {
        header(HttpHeaders.COOKIE, cookie);
    }

    public void allow(String... methods) {
        headers(HttpHeaders.ALLOW, methods);
    }

    public void allow(Set<String> methods) {
        headers(HttpHeaders.ALLOW, methods);
    }

    public void cacheControl(CacheControl cacheControl) {
        header(HttpHeaders.CACHE_CONTROL, cacheControl);
    }

    public void encoding(String encoding) {
        replace(HttpHeaders.CONTENT_ENCODING, encoding);
    }

    public void replaceHeaders(MultivaluedMap<String, Object> map) {
        replaceAll(map);
    }

    public void language(String language) {
        replace(HttpHeaders.CONTENT_LANGUAGE, language);
    }

    public void language(Locale language) {
        replace(HttpHeaders.CONTENT_LANGUAGE, language);
    }

    public void type(MediaType type) {
        setMediaType(type);
    }

    public void type(String type) {
        type(type == null ? null : MediaType.valueOf(type));
    }

    public void variant(Variant variant) {
        if (variant == null) {
            type((MediaType) null);
            language((String) null);
            encoding(null);
        } else {
            type(variant.getMediaType());
            language(variant.getLanguage());
            encoding(variant.getEncoding());
        }
    }
}
