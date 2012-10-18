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

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
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
public class ClientRequest extends OutboundMessageContext implements ClientRequestContext {
    // Request-scoped configuration instance
    private final ClientConfig configuration;
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
    // Flag indicating whether the request is asynchronous
    private boolean asynchronous;

    /**
     * Create new Jersey client request context.
     *
     * @param requestUri         request Uri.
     * @param configuration      request configuration.
     * @param propertiesDelegate properties delegate.
     */
    public ClientRequest(
            URI requestUri, ClientConfig configuration, PropertiesDelegate propertiesDelegate) {
        configuration.checkClient();

        this.requestUri = requestUri;
        this.configuration = configuration;
        this.propertiesDelegate = propertiesDelegate;
    }

    /**
     * Copy constructor.
     *
     * @param original original instance.
     */
    public ClientRequest(ClientRequest original) {
        super(original);
        this.requestUri = original.requestUri;
        this.httpMethod = original.httpMethod;
        this.workers = original.workers;
        this.configuration = original.configuration.snapshot();
        this.asynchronous = original.isAsynchronous();

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
        return configuration.getClient();
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
    public ClientConfig getConfiguration() {
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

    /**
     * Add new accepted types to the message headers.
     *
     * @param types accepted types to be added.
     */
    public void accept(MediaType... types) {
        getHeaders().addAll(HttpHeaders.ACCEPT, (Object[]) types);
    }

    /**
     * Add new accepted types to the message headers.
     *
     * @param types accepted types to be added.
     */
    public void accept(String... types) {
        getHeaders().addAll(HttpHeaders.ACCEPT, (Object[]) types);
    }

    /**
     * Add new accepted languages to the message headers.
     *
     * @param locales accepted languages to be added.
     */
    public void acceptLanguage(Locale... locales) {
        getHeaders().addAll(HttpHeaders.ACCEPT_LANGUAGE, (Object[]) locales);
    }

    /**
     * Add new accepted languages to the message headers.
     *
     * @param locales accepted languages to be added.
     */
    public void acceptLanguage(String... locales) {
        getHeaders().addAll(HttpHeaders.ACCEPT_LANGUAGE, (Object[]) locales);
    }

    /**
     * Add new cookie to the message headers.
     *
     * @param cookie cookie to be added.
     */
    public void cookie(Cookie cookie) {
        getHeaders().add(HttpHeaders.COOKIE, cookie);
    }

    /**
     * Add new cache control entry to the message headers.
     *
     * @param cacheControl cache control entry to be added.
     */
    public void cacheControl(CacheControl cacheControl) {
        getHeaders().add(HttpHeaders.CACHE_CONTROL, cacheControl);
    }

    /**
     * Set message encoding.
     *
     * @param encoding message encoding to be set.
     */
    public void encoding(String encoding) {
        if(encoding == null) {
            getHeaders().remove(HttpHeaders.CONTENT_ENCODING);
        } else {
            getHeaders().putSingle(HttpHeaders.CONTENT_ENCODING, encoding);
        }
    }

    /**
     * Set message language.
     *
     * @param language message language to be set.
     */
    public void language(String language) {
        if(language == null) {
            getHeaders().remove(HttpHeaders.CONTENT_LANGUAGE);
        } else {
            getHeaders().putSingle(HttpHeaders.CONTENT_LANGUAGE, language);
        }
    }

    /**
     * Set message language.
     *
     * @param language message language to be set.
     */
    public void language(Locale language) {
        if(language == null) {
            getHeaders().remove(HttpHeaders.CONTENT_LANGUAGE);
        } else {
            getHeaders().putSingle(HttpHeaders.CONTENT_LANGUAGE, language);
        }
    }

    /**
     * Set message content type.
     *
     * @param type message content type to be set.
     */
    public void type(MediaType type) {
        setMediaType(type);
    }

    /**
     * Set message content type.
     *
     * @param type message content type to be set.
     */
    public void type(String type) {
        type(type == null ? null : MediaType.valueOf(type));
    }

    /**
     * Set message content variant (type, language and encoding).
     *
     * @param variant message content content variant (type, language and encoding)
     *                to be set.
     */
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

    /**
     * Returns true if the request is called asynchronously using {@link javax.ws.rs.client.AsyncInvoker}
     *
     * @return True if the request is asynchronous; false otherwise.
     */
    public boolean isAsynchronous() {
        return asynchronous;
    }


    /**
     * Sets the flag indicating whether the request is called asynchronously using {@link javax.ws.rs.client.AsyncInvoker}.
     *
     * @param async True if the request is asynchronous; false otherwise.
     */
    void setAsynchronous(boolean async) {
        asynchronous = async;
    }
}
