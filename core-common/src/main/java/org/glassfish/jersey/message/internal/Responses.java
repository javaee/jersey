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

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.StatusType;

import org.glassfish.jersey.message.MessageBodyWorkers;

/**
 * Internal Jersey response factory & utility class. Provides various response factory
 * and utility methods.
 *
 * @author Paul Sandoz
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public final class Responses {

    /**
     * Create a response builder with no response entity & status code set to
     * {@link Status#NO_CONTENT}.
     *
     * @return response builder instance.
     */
    public static ResponseBuilder empty() {
        return new MutableResponse().toJaxrsResponseBuilder();
    }

    /**
     * Create a response builder for the request with a given status code.
     *
     * @param status response status code.
     * @param request request for which the response is created.
     *
     * @return response builder instance.
     */
    public static ResponseBuilder from(StatusType status, Request request) {
        return new MutableResponse(status, JaxrsRequestView.unwrap(request).workers()).toJaxrsResponseBuilder();
    }

    /**
     * Create a response builder for the request with a given status code.
     *
     * @param statusCode response status code.
     * @param request request for which the response is created.
     * @param externalInputStream sets the external raw input stream. This input stream is
     *            the stream "from wire" and is not intercepted yet.
     * @return response builder instance.
     */
    public static ResponseBuilder from(int statusCode, Request request, InputStream externalInputStream) {
        return new MutableResponse(statusCode, JaxrsRequestView.unwrap(request).workers(), externalInputStream)
                .toJaxrsResponseBuilder();
    }


    /**
     * Create a response builder for the request with a given status code.
     *
     * @param statusCode response status code.
     * @param request request for which the response is created.
     *
     * @return response builder instance.
     */
    public static ResponseBuilder from(int statusCode, Request request) {
        return new MutableResponse(statusCode, JaxrsRequestView.unwrap(request).workers()).toJaxrsResponseBuilder();
    }


    /**
     * Transforms a response to a response builder.
     *
     * @param response response instance to be transformed into a response builder.
     * @return response builder instance.
     */
    public static ResponseBuilder toBuilder(Response response) {
        return JaxrsResponseView.unwrap(response).toBuilder().toJaxrsResponseBuilder();
    }

    /**
     * Add headers to the response builder.
     *
     * @param responseBuilder JAX-RS response builder. It is assumed that the instance
     *     is represented by an internal Jersey response builder type.
     * @param headers map of headers. Each header name may be associated with multiple
     *     values stored as a list.
     */
    // TODO add to the JAX-RS ResponseBuilder/ Headers.Builder API?
    public static void fillHeaders(ResponseBuilder responseBuilder, Map<String, List<String>> headers) {
        JaxrsResponseBuilderView.unwrap(responseBuilder).headers(headers);
    }

    /**
     * Create a response builder for the request & status code set to
     * {@link Status#NOT_MODIFIED}.
     *
     * @param eTag entity tag header value to be set on the response.
     * @param request request for which the response is created.
     * @return response builder instance.
     */
    public static ResponseBuilder notModified(EntityTag eTag, Request request) {
        ResponseBuilder builder = responseBuilder(Status.NOT_MODIFIED, request);
        builder.tag(eTag);
        return builder;
    }

    /**
     * Create a response builder for the request & status code set to
     * {@link Status#NOT_MODIFIED}.
     *
     * @param request request for which the response is created.
     * @return response builder instance.
     */
    public static ResponseBuilder notModified(Request request) {
        return responseBuilder(Status.NOT_MODIFIED, request);
    }

    /**
     * Create a response builder for the request & status code set to
     * {@link Status#NO_CONTENT}.
     *
     * @param request request for which the response is created.
     * @return response builder instance.
     */
    public static ResponseBuilder noContent(Request request) {
        return responseBuilder(Status.NO_CONTENT, request);
    }

    /**
     * Create a response builder for the request & status code set to
     * {@link Status#BAD_REQUEST}.
     *
     * @param request request for which the response is created.
     * @return response builder instance.
     */
    public static ResponseBuilder clientError(Request request) {
        return responseBuilder(Status.BAD_REQUEST, request);
    }

    /**
     * Create a response builder for the request & status code set to
     * {@link Status#NOT_FOUND}.
     *
     * @param request request for which the response is created.
     * @return response builder instance.
     */
    public static ResponseBuilder notFound(Request request) {
        return responseBuilder(Status.NOT_FOUND, request);
    }

    /**
     * Create a response builder for the request & status code set to
     * {@link Status#METHOD_NOT_ALLOWED}.
     *
     * @param request request for which the response is created.
     * @return response builder instance.
     */
    public static ResponseBuilder methodNotAllowed(Request request) {
        return responseBuilder(Status.METHOD_NOT_ALLOWED, request);
    }

    /**
     * Create a response builder for the request & status code set to
     * {@link Status#NOT_ACCEPTABLE}.
     *
     * @param request request for which the response is created.
     * @return response builder instance.
     */
    public static ResponseBuilder notAcceptable(Request request) {
        return responseBuilder(Status.NOT_ACCEPTABLE, request);
    }

    /**
     * Create a response builder for the request & status code set to
     * {@link Status#PRECONDITION_FAILED}.
     *
     * @param request request for which the response is created.
     * @return response builder instance.
     */
    public static ResponseBuilder preconditionFailed(Request request) {
        return responseBuilder(Status.PRECONDITION_FAILED, request);
    }

    /**
     * Create a response builder for the request & status code set to
     * {@link Status#CONFLICT}.
     *
     * @param request request for which the response is created.
     * @return response builder instance.
     */
    public static ResponseBuilder conflict(Request request) {
        return responseBuilder(Status.CONFLICT, request);
    }

    /**
     * Create a response builder for the request & status code set to
     * {@link Status#UNSUPPORTED_MEDIA_TYPE}.
     *
     * @param request request for which the response is created.
     * @return response builder instance.
     */
    public static ResponseBuilder unsupportedMediaType(Request request) {
        return responseBuilder(Status.UNSUPPORTED_MEDIA_TYPE, request);
    }

    /**
     * Create a response builder for the request & with a supplied status type.
     *
     * @param status response status type.
     * @param request request for which the response is created.
     * @return response builder instance.
     */
    private static ResponseBuilder responseBuilder(StatusType status, Request request) {
        return new MutableResponse(status, JaxrsRequestView.unwrap(request).workers()).toJaxrsResponseBuilder();
    }

    /**
     * Create a response builder with a supplied status type.
     *
     * @param status response status type.
     * @param workers message body workers associated with the request context.
     * @return response builder instance.
     */
    public static ResponseBuilder responseBuilder(StatusType status, MessageBodyWorkers workers) {
        return new MutableResponse(status, workers).toJaxrsResponseBuilder();
    }

    /**
     * Set the {@link MessageBodyWorkers} to the response builder.
     *
     * @param responseBuilder JAX-RS response builder. It is assumed that the instance
     *     is represented by an internal Jersey response builder type.
     * @param workers message body readers and writers lookup factory.
     */
    public static void setMessageWorkers(Response.ResponseBuilder responseBuilder, MessageBodyWorkers workers) {
        JaxrsResponseBuilderView.unwrap(responseBuilder).workers(workers);
    }

    /**
     * Get the {@link MessageBodyWorkers} from the response.
     *
     * @param response JAX-RS response. It is assumed that the instance
     *     is represented by an internal Jersey response builder type.
     * @return message body readers and writers lookup factory.
     */
    public static MessageBodyWorkers getMessageWorkers(Response response) {
        return JaxrsResponseView.unwrap(response).workers();
    }

    /**
     * Prevents instantiation.
     */
    private Responses() {
    }

}
