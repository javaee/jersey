/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;

import com.google.common.collect.Maps;

/**
 * Jersey implementation of {@link javax.ws.rs.client.WebTarget JAX-RS client target}
 * contract.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class WebTarget implements javax.ws.rs.client.WebTarget {

    // TODO base URI support
    private final ClientConfig configuration;
    private final UriBuilder targetUri;
    private final Map<String, Object> pathParams;
    private final JerseyClient client;

    /*package*/ WebTarget(String uri, JerseyClient parent) {
        this(UriBuilder.fromUri(uri), null, parent.configuration().snapshot(), parent);
    }

    /*package*/ WebTarget(URI uri, JerseyClient parent) {
        this(UriBuilder.fromUri(uri), null, parent.configuration().snapshot(), parent);
    }

    /*package*/ WebTarget(UriBuilder uriBuilder, JerseyClient parent) {
        this(uriBuilder.clone(), null, parent.configuration().snapshot(), parent);
    }

    /*package*/ WebTarget(Link link, JerseyClient parent) {
        // TODO handle relative links
        this(UriBuilder.fromUri(link.getUri()), null, parent.configuration().snapshot(), parent);
    }

    protected WebTarget(UriBuilder targetUri, WebTarget that) {
        this(targetUri, that.pathParams, that.configuration.snapshot(), that.client);
    }

    protected WebTarget(UriBuilder targetUri, Map<String, Object> pathParams, ClientConfig clientConfig, JerseyClient client) {
        this.targetUri = targetUri;
        if (pathParams != null) {
            this.pathParams = Maps.newHashMap(pathParams);
        } else {
            this.pathParams = Maps.newHashMap();
        }
        this.configuration = clientConfig;
        this.client = client;
    }

    /**
     * Set value of a path parameter.
     *
     * @param name path parameter name.
     * @param value path parameter value. If {@code null}, any existing mapping
     *     for the path parameter name will be removed.
     */
    protected final void setPathParam(String name, Object value) {
        if (value == null) {
            pathParams.remove(name);
        } else {
            pathParams.put(name, value);
        }
    }

    protected final void replacePathParams(Map<String, Object> params) {
        pathParams.clear();
        if (params != null) {
            pathParams.putAll(params);
        }
    }

    @Override
    public URI getUri() {
        client.checkClosed();
        return targetUri.buildFromMap(pathParams);
    }

    @Override
    public UriBuilder getUriBuilder() {
        client.checkClosed();
        return targetUri.clone();
    }

    @Override
    public ClientConfig configuration() {
        client.checkClosed();
        return configuration;
    }

    @Override
    public WebTarget path(String path) throws NullPointerException {
        client.checkClosed();
        return new WebTarget(getUriBuilder().path(path), this);
    }

    @Override
    public WebTarget pathParam(String name, Object value) throws IllegalArgumentException, NullPointerException {
        client.checkClosed();
        WebTarget result = new WebTarget(getUriBuilder(), this);
        result.setPathParam(name, value);
        return result;
    }

    @Override
    public WebTarget pathParams(Map<String, Object> parameters) throws IllegalArgumentException, NullPointerException {
        client.checkClosed();
        WebTarget result = new WebTarget(getUriBuilder(), this);
        result.replacePathParams(parameters);
        return result;
    }

    @Override
    public WebTarget matrixParam(String name, Object... values) throws NullPointerException {
        client.checkClosed();
        return new WebTarget(getUriBuilder().matrixParam(name, values), this);
    }

    @Override
    public WebTarget queryParam(String name, Object... values) throws NullPointerException {
        client.checkClosed();
        return new WebTarget(getUriBuilder().queryParam(name, values), this);
    }

    @Override
    public WebTarget queryParams(MultivaluedMap<String, Object> parameters) throws IllegalArgumentException, NullPointerException {
        // TODO move the implementation to a proprietary Jersey uri builder or leave it here?
        client.checkClosed();
        UriBuilder ub = getUriBuilder(); // clone
        for (Entry<String, List<Object>> e : parameters.entrySet()) {
            ub.queryParam(e.getKey(), e.getValue().toArray());
        }

        return new WebTarget(ub, this);
    }

    @Override
    public JerseyInvocation.Builder request() {
        // TODO values
        client.checkClosed();
        return new JerseyInvocation.Builder(getUri(), configuration.snapshot(), client);
    }

    @Override
    public JerseyInvocation.Builder request(String... acceptedResponseTypes) {
        // TODO values
        client.checkClosed();
        JerseyInvocation.Builder b = new JerseyInvocation.Builder(getUri(), configuration.snapshot(), client);
        b.request().accept(acceptedResponseTypes);
        return b;
    }

    @Override
    public JerseyInvocation.Builder request(MediaType... acceptedResponseTypes) {
        // TODO values
        client.checkClosed();
        JerseyInvocation.Builder b = new JerseyInvocation.Builder(getUri(), configuration.snapshot(), client);
        b.request().accept(acceptedResponseTypes);
        return b;
    }
}
