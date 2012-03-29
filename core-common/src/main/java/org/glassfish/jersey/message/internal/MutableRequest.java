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

    private transient javax.ws.rs.core.Request jaxrsView;
    private transient javax.ws.rs.core.Request.RequestBuilder jaxrsBuilderView;
    private transient javax.ws.rs.core.RequestHeaders jaxrsHeadersView;
    // Absolute application root URI (base URI)
    private URI applicationRootUri;
    // Absolute request URI
    private URI requestUri;
    // Lazily computed relative (to application root URI) request paths
    private String encodedRelativePath = null;
    private String decodedRelativePath = null;
    // Request method
    private String method;

    MutableRequest(MutableRequest that) {
        super(that);

        this.applicationRootUri = that.applicationRootUri;
        this.requestUri = that.requestUri;
        this.method = that.method;
    }

    MutableRequest(URI applicationRootUri, URI requestUri, String method) {
        this.applicationRootUri = applicationRootUri;
        this.requestUri = requestUri;
        this.method = method;
    }

    MutableRequest(String applicationRootUri, String requestUri, String method) {
        this.applicationRootUri = URI.create(applicationRootUri != null ? applicationRootUri : "/");
        this.requestUri = URI.create(requestUri);
        this.method = method;
    }

    MutableRequest(Request request) {
        super(request.headers(), request.content(InputStream.class), request.properties());

        this.applicationRootUri = request.baseUri();
        this.requestUri = request.uri();
        this.method = request.method();
    }

    @Override
    public URI baseUri() {
        return applicationRootUri;
    }

    @Override
    public String relativePath(boolean decode) {
        if (decode) {
            if (decodedRelativePath != null) {
                return decodedRelativePath;
            }

            return decodedRelativePath = UriComponent.decode(
                    encodedRelativePath(),
                    UriComponent.Type.PATH);
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
        if (applicationRootUri == null) {
            result = requestUriRawPath;
        } else {
            final String applicationRootUriRawPath = applicationRootUri.getRawPath();
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
        this.applicationRootUri = this.requestUri = URI.create(uri);
        return this;
    }

    @Override
    public MutableRequest uri(URI uri) {
        this.applicationRootUri = this.requestUri = uri;
        return this;
    }

    @Override
    public Builder uris(String applicationRootUri, String requestUri) {
        this.applicationRootUri = URI.create(applicationRootUri);
        this.requestUri = URI.create(requestUri);
        return this;
    }

    @Override
    public Builder uris(URI applicationRootUri, URI requestUri) {
        this.applicationRootUri = applicationRootUri;
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
