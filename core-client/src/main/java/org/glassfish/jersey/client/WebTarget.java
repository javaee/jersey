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

    private final ClientConfig configuration;
    private final UriBuilder targetUri;
    private final Map<String, Object> pathParams;

    /**
     * Create new web target instance.
     *
     * @param uri target URI.
     * @param parent parent client.
     */
    /*package*/ WebTarget(String uri, JerseyClient parent) {
        this(UriBuilder.fromUri(uri), null, parent.configuration().snapshot());
    }

    /**
     * Create new web target instance.
     *
     * @param uri target URI.
     * @param parent parent client.
     */
    /*package*/ WebTarget(URI uri, JerseyClient parent) {
        this(UriBuilder.fromUri(uri), null, parent.configuration().snapshot());
    }

    /**
     * Create new web target instance.
     *
     * @param uriBuilder builder for the target URI.
     * @param parent parent client.
     */
    /*package*/ WebTarget(UriBuilder uriBuilder, JerseyClient parent) {
        this(uriBuilder.clone(), null, parent.configuration().snapshot());
    }

    /**
     * Create new web target instance.
     *
     * @param link link to the target URI.
     * @param parent parent client.
     */
    /*package*/ WebTarget(Link link, JerseyClient parent) {
        // TODO handle relative links
        this(UriBuilder.fromUri(link.getUri()), null, parent.configuration().snapshot());
    }

    /**
     * Create new web target instance.
     *
     * @param uriBuilder builder for the target URI.
     * @param that original target to copy the internal data from.
     */
    protected WebTarget(UriBuilder uriBuilder, WebTarget that) {
        this(uriBuilder, that.pathParams, that.configuration.snapshot());
    }

    /**
     * Create new web target instance.
     *
     * @param uriBuilder builder for the target URI.
     * @param pathParams map of path parameter names to values.
     * @param clientConfig target configuration.
     */
    protected WebTarget(UriBuilder uriBuilder, Map<String, Object> pathParams, ClientConfig clientConfig) {
        clientConfig.checkClient();

        this.targetUri = uriBuilder;
        if (pathParams != null) {
            this.pathParams = Maps.newHashMap(pathParams);
        } else {
            this.pathParams = Maps.newHashMap();
        }
        this.configuration = clientConfig;
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

    /**
     * Replace path parameter values.
     *
     * @param valueMap path parameter name to value map.
     */
    protected final void replacePathParams(Map<String, Object> valueMap) {
        pathParams.clear();
        if (valueMap != null) {
            pathParams.putAll(valueMap);
        }
    }

    @Override
    public URI getUri() {
        configuration.getClient().checkNotClosed();
        return targetUri.buildFromMap(pathParams);
    }

    private void checkNotClosed() {
        configuration.getClient().checkNotClosed();
    }

    @Override
    public UriBuilder getUriBuilder() {
        checkNotClosed();
        return targetUri.clone();
    }

    @Override
    public ClientConfig configuration() {
        checkNotClosed();
        return configuration;
    }

    @Override
    public WebTarget path(String path) throws NullPointerException {
        checkNotClosed();
        return new WebTarget(getUriBuilder().path(path), this);
    }

    @Override
    public WebTarget pathParam(String name, Object value) throws IllegalArgumentException, NullPointerException {
        checkNotClosed();
        WebTarget result = new WebTarget(getUriBuilder(), this);
        result.setPathParam(name, value);
        return result;
    }

    @Override
    public WebTarget pathParams(Map<String, Object> parameters) throws IllegalArgumentException, NullPointerException {
        checkNotClosed();
        WebTarget result = new WebTarget(getUriBuilder(), this);
        result.replacePathParams(parameters);
        return result;
    }

    @Override
    public WebTarget matrixParam(String name, Object... values) throws NullPointerException {
        checkNotClosed();
        return new WebTarget(getUriBuilder().matrixParam(name, values), this);
    }

    @Override
    public WebTarget queryParam(String name, Object... values) throws NullPointerException {
        checkNotClosed();
        return new WebTarget(getUriBuilder().queryParam(name, values), this);
    }

    @Override
    public WebTarget queryParams(MultivaluedMap<String, Object> parameters) throws IllegalArgumentException, NullPointerException {
        // TODO move the implementation to a proprietary Jersey uri builder or leave it here?
        checkNotClosed();
        UriBuilder ub = getUriBuilder(); // clone
        for (Entry<String, List<Object>> e : parameters.entrySet()) {
            ub.queryParam(e.getKey(), e.getValue().toArray());
        }

        return new WebTarget(ub, this);
    }

    @Override
    public JerseyInvocation.Builder request() {
        // TODO values
        checkNotClosed();
        return new JerseyInvocation.Builder(getUri(), configuration.snapshot());
    }

    @Override
    public JerseyInvocation.Builder request(String... acceptedResponseTypes) {
        // TODO values
        checkNotClosed();
        JerseyInvocation.Builder b = new JerseyInvocation.Builder(getUri(), configuration.snapshot());
        b.request().accept(acceptedResponseTypes);
        return b;
    }

    @Override
    public JerseyInvocation.Builder request(MediaType... acceptedResponseTypes) {
        // TODO values
        checkNotClosed();
        JerseyInvocation.Builder b = new JerseyInvocation.Builder(getUri(), configuration.snapshot());
        b.request().accept(acceptedResponseTypes);
        return b;
    }
}
