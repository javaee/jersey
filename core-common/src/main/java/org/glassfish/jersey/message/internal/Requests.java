/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

import java.net.URI;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Request.RequestBuilder;

import org.glassfish.jersey.message.MessageBodyWorkers;

/**
 * Internal Jersey request factory & utility class. Provides various request factory
 * and utility methods.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public final class Requests {

    /**
     * Create new request builder with a base URI.
     *
     * @param baseUri base request URI; on the server side represented by the
     *     application root URI.
     * @param requestUri absolute request URI.
     * @param method request method.
     * @return new request builder.
     */
    public static RequestBuilder from(URI baseUri, URI requestUri, String method) {
        return new MutableRequest(baseUri, requestUri, method).toJaxrsRequestBuilder();
    }

    /**
     * Create new request builder without any base URI.
     *
     * @param requestUri absolute request URI.
     * @param method request method.
     * @return new request builder.
     */
    public static RequestBuilder from(URI requestUri, String method) {
        return new MutableRequest(null, requestUri, method).toJaxrsRequestBuilder();
    }

    /**
     * Create new request builder with a base URI.
     *
     * @param baseUri base request URI; on the server side represented by the
     *     application root URI.
     * @param requestUri absolute request URI.
     * @param method request method.
     * @return new request builder.
     */
    public static RequestBuilder from(String baseUri, String requestUri, String method) {
        return new MutableRequest(baseUri, requestUri, method).toJaxrsRequestBuilder();
    }

    /**
     * Create new request builder without any base URI.
     *
     * @param requestUri absolute request URI.
     * @param method request method.
     * @return new request builder.
     */
    public static RequestBuilder from(String requestUri, String method) {
        return new MutableRequest(null, requestUri, method).toJaxrsRequestBuilder();
    }

    /**
     * Create a new request instance as a copy of an existing request instance.
     *
     * @param request original JAX-RS request to be copied. It is assumed that
     *     the instance is represented by an internal Jersey request type.
     * @return copy of an existing request instance.
     */
    public static RequestBuilder from(Request request) {
        return new MutableRequest(JaxrsRequestView.unwrap(request)).toJaxrsRequestBuilder();
    }

    /**
     * Request builder initialized from the request.
     *
     * @param request JAX-RS request. It is assumed that the instance is represented
     *     by an internal Jersey request type.
     * @return request builder initialized from the request.
     */
    public static RequestBuilder toBuilder(Request request) {
        return JaxrsRequestView.unwrap(request).toBuilder().toJaxrsRequestBuilder();
    }

    /**
     * Get the path of the request relative to the base request URI as a string.
     * All sequences of escaped octets are decoded, equivalent to
     * {@link #relativePath(javax.ws.rs.core.Request, boolean) Requests.relativePath(request, true)}.
     *
     * @param request JAX-RS request. It is assumed that the instance is represented
     *     by an internal Jersey request type.
     * @return decoded relative request path.
     */
     // TODO expose as part of JAX-RS Request API?
    public static String relativePath(Request request) {
        return JaxrsRequestView.unwrap(request).relativePath(true);
    }

    /**
     * Get the path of the request relative to the base request URI as a string.
     *
     * @param request JAX-RS request. It is assumed that the instance is represented
     *     by an internal Jersey request type.
     * @param decode controls whether sequences of escaped octets are decoded
     *     ({@code true}) or not ({@code false}).
     * @return relative request path.
     */
    // TODO expose as part of JAX-RS Request API?
    public static String relativePath(Request request, boolean decode) {
        return JaxrsRequestView.unwrap(request).relativePath(decode);
    }

    /**
     * Get the base URI of the request.
     * <p/>
     * On the server side base request URI is the URI of the application.
     * URIs of root resource classes are all relative to this base URI.
     *
     * @param request JAX-RS request. It is assumed that the instance is represented
     *     by an internal Jersey request type.
     * @return base URI of the request.
     */
    // TODO expose as part of JAX-RS Request API?
    public static URI baseUri(Request request) {
        return JaxrsRequestView.unwrap(request).baseUri();
    }

    /**
     * Get {@link HttpHeaders} instance representing the headers of the request.
     *
     * @param request JAX-RS request. It is assumed that the instance is represented
     *     by an internal Jersey request type.
     * @return {@link HttpHeaders} instance for the headers of the request.
     */
    public static HttpHeaders httpHeaders(Request request) {
        return JaxrsRequestHeadersView.unwrap(request.getHeaders());
    }

    /**
     * Set the {@link MessageBodyWorkers} to the request builder.
     *
     * @param requestBuilder JAX-RS request builder. It is assumed that the instance
     *     is represented by an internal Jersey request builder type.
     * @param workers message body readers and writers lookup factory.
     */
    public static void setMessageWorkers(RequestBuilder requestBuilder, MessageBodyWorkers workers) {
        JaxrsRequestBuilderView.unwrap(requestBuilder).workers(workers);
    }

    /**
     * Get the {@link MessageBodyWorkers} from the request.
     *
     * @param request JAX-RS request. It is assumed that the instance
     *     is represented by an internal Jersey request builder type.
     * @return message body readers and writers lookup factory.
     */
    public static MessageBodyWorkers getMessageWorkers(Request request) {
        return JaxrsRequestView.unwrap(request).workers();
    }
}
