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
package org.glassfish.jersey.message.internal;

import java.io.InputStream;
import java.net.URI;

import javax.ws.rs.core.Request.RequestBuilder;
import javax.ws.rs.core.RequestHeaders;

import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.uri.UriComponent;

/**
 * Mutable request message implementation class.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
class MutableRequest extends AbstractMutableMessage<MutableRequest> implements Request, Request.Builder {

    private static URI DEFAULT_BASE_URI = URI.create("/");

    private transient javax.ws.rs.core.Request jaxrsView;
    private transient javax.ws.rs.core.Request.RequestBuilder jaxrsBuilderView;
    private transient javax.ws.rs.core.RequestHeaders jaxrsHeadersView;
    // Absolute application root URI (base URI)
    private URI baseUri;
    // Absolute request URI
    private URI requestUri;
    // Lazily computed relative (to application root URI) request paths
    private String encodedRelativePath = null;
    private String decodedRelativePath = null;
    // Request method
    private String method;

    /**
     * Creates new instance initialized from {@link MutableRequest another instance}.
     * @param that Instance from which new instance will be initialized.
     */
    MutableRequest(MutableRequest that) {
        super(that);

        this.baseUri = that.baseUri;
        this.requestUri = that.requestUri;
        this.method = that.method;
    }

    /**
     * Creates new instance initialized with {@code applicationRootUri}, {@code requestUri}, {@code http method}.

     * @param baseUri Absolute application root URI (base URI).
     * @param requestUri Absolute request URI.
     * @param method Request HTTP method.
     */
    MutableRequest(URI baseUri, URI requestUri, String method) {
        this.baseUri = (baseUri != null) ? normalizeBaseUri(baseUri) : DEFAULT_BASE_URI;
        this.requestUri = requestUri.normalize();
        this.method = method;
    }

    /**
     * Creates new instance initialized with {@code applicationRootUri}, {@code requestUri}, {@code http method}
     * and {@code entity inputStream}.
     * @param baseUri Absolute application root URI (base URI).
     * @param requestUri Absolute request URI.
     * @param method Request HTTP method.
     * @param inputStream {@link InputStream Entity content stream}.
     */
    MutableRequest(URI baseUri, URI requestUri, String method, InputStream inputStream) {
        this.baseUri = (baseUri != null) ? normalizeBaseUri(baseUri) : DEFAULT_BASE_URI;
        this.requestUri = requestUri.normalize();
        this.method = method;
        entity().rawEntityStream(inputStream);
    }

    /**
     * Creates new instance initialized with URIs created from strings ({@code applicationRootUri}, {@code requestUri})
     *  and from {@code HTTP method}.
     * @param baseUri Absolute application root URI (base URI).
     * @param requestUri Absolute request URI.
     * @param method Request HTTP method.
     */
    MutableRequest(String baseUri, String requestUri, String method) {
        this.baseUri = baseUri != null ? normalizeBaseUri(URI.create(baseUri)) : DEFAULT_BASE_URI;
        this.requestUri = URI.create(requestUri).normalize();
        this.method = method;
    }

    /**
     * Creates new instance of request initialized from the given instance.
     * @param request {@link MutableRequest} from which new instance should be initialized.
     */
    MutableRequest(Request request) {
        super(request.headers(), request.content(InputStream.class), request.properties());

        this.baseUri = request.baseUri();
        this.requestUri = request.uri();
        this.method = request.method();
    }

    private URI normalizeBaseUri(URI baseUri) {
        return baseUri.normalize();
    }

    @Override
    public URI baseUri() {
        return baseUri;
    }

    @Override
    public String relativePath(boolean decode) {
        if (decode) {
            if (decodedRelativePath != null) {
                return decodedRelativePath;
            }

            return decodedRelativePath = UriComponent.decode(encodedRelativePath(), UriComponent.Type.PATH);
        } else {
            return encodedRelativePath();
        }
    }

    private String encodedRelativePath() {
        if (encodedRelativePath != null) {
            return encodedRelativePath;
        }

        String result;
        final String requestUriRawPath = requestUri.getRawPath();
        if (baseUri == null) {
            result = requestUriRawPath;
        } else {
            final String applicationRootUriRawPath = baseUri.getRawPath();
            if (applicationRootUriRawPath.length() > requestUriRawPath.length()) {
                result = "";
            } else {
                result = requestUriRawPath.substring(applicationRootUriRawPath.length());
            }
        }

        if (result.isEmpty()) {
            result = "/";
        }

        return encodedRelativePath = (result.charAt(0) == '/') ? result : '/' + result;
    }

    @Override
    public URI uri() {
        return requestUri;
    }

    @Override
    public String method() {
        return method;
    }

    @Override
    public MutableRequest uri(String uri) {
        this.requestUri = URI.create(uri);
        return this;
    }

    @Override
    public MutableRequest uri(URI uri) {
        this.requestUri = uri;
        return this;
    }

    @Override
    public Builder uris(String baseUri, String requestUri) {
        this.baseUri = URI.create(baseUri);
        this.requestUri = URI.create(requestUri);
        return this;
    }

    @Override
    public Builder uris(URI baseUri, URI requestUri) {
        this.baseUri = baseUri;
        this.requestUri = requestUri;
        return this;
    }

    @Override
    public MutableRequest method(String method) {
        this.method = method;
        return this;
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public MutableRequest clone() {
        return new MutableRequest(this);
    }

    @Override
    public javax.ws.rs.core.Request toJaxrsRequest() {
        if (jaxrsView == null) {
            jaxrsView = new JaxrsRequestView(this);
        }

        return jaxrsView;
    }

    @Override
    public RequestBuilder toJaxrsRequestBuilder() {
        if (jaxrsBuilderView == null) {
            jaxrsBuilderView = new JaxrsRequestBuilderView(this);
        }

        return jaxrsBuilderView;
    }

    @Override
    public RequestHeaders getJaxrsHeaders() {
        if (jaxrsHeadersView == null) {
            jaxrsHeadersView = new JaxrsRequestHeadersView(this);
        }

        return jaxrsHeadersView;
    }

    @Override
    public boolean isEntityRetrievable() {
        return this.isEmpty() || this.type() != null;
    }

    @Override
    public Builder workers(MessageBodyWorkers workers) {
        entityWorkers(workers);
        return this;
    }

    @Override
    public MessageBodyWorkers workers() {
        return entityWorkers();
    }

}
