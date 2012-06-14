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
package org.glassfish.jersey._remove;

import java.util.Map;

import javax.ws.rs.core.Request;
import org.glassfish.jersey._remove.RequestBuilder;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.message.internal.JaxrsRequestView;
import org.glassfish.jersey.message.internal.JaxrsResponseView;

/**
 * Context class used by filters implementing
 * {@link RequestFilter} or
 * {@link ResponseFilter} (or both).
 *
 * @author Santiago Pericas-Geertsen
 * @author Bill Burke
 * @since 2.0
 */
public interface FilterContext {

    /**
     * Get a mutable map of request-scoped properties that can be used for communication
     * between different request/response processing components. May be empty, but
     * MUST never be {@code null}. In the scope of a single request/response processing,
     * a same property map instance is shared by the following methods:
     * <ul>
     *     <li>{@link javax.ws.rs.core.Request#getProperties() }</li>
     *     <li>{@link javax.ws.rs.core.Response#getProperties() }</li>
     *     <li>{@link FilterContext#getProperties() }</li>
     *     <li>{@link javax.ws.rs.ext.InterceptorContext#getProperties() }</li>
     * </ul>
     * A request-scoped property is an application-defined property that may be
     * added, removed or modified by any of the components (user, filter, interceptor etc.)
     * that participate in a given request/response processing flow.
     * <p />
     * On the client side, this property map is initialized by calling
     * {@link javax.ws.rs.client.Configuration#setProperties(java.util.Map) } or
     * {@link javax.ws.rs.client.Configuration#setProperty(java.lang.String, java.lang.Object) }
     * on the configuration object associated with the corresponding
     * {@link javax.ws.rs.client.Invocation request invocation}.
     * <p />
     * On the server side, specifying the initial values is implementation-specific.
     * <p />
     * If there are no initial properties set, the request-scoped property map is
     * initialized to an empty map.
     *
     * @return a mutable request-scoped property map.
     * @see javax.ws.rs.client.Configuration
     */
    Map<String, Object> getProperties();

    /**
     * Get the request object.
     *
     * @return request object being filtered.
     */
    JaxrsRequestView getRequest();

    /**
     * Get the response object. May return {@code null} if a
     * response is not available, e.g. in a
     * {@link RequestFilter}, and has not been
     * set by calling {@link #setResponse(javax.ws.rs.core.Response)}.
     *
     * @return response object being filtered or {@code null}.
     */
    JaxrsResponseView getResponse();

    /**
     * Set the request object in the context.
     *
     * @param req request object to be set.
     */
    void setRequest(Request req);

    /**
     * Set the response object in the context. A caching filter
     * that implements {@link RequestFilter} or {@link PreMatchRequestFilter}
     * could set a response by calling this method. See
     * {@link RequestFilter#preFilter} and {@link PreMatchRequestFilter#preMatchFilter}
     * for more information.
     *
     * @param res response object to be set.
     */
    void setResponse(Response res);

    /**
     * Get a builder for the request object. A newly built request can
     * be set by calling {@link #setRequest(javax.ws.rs.core.Request)}.
     *
     * @return request builder object.
     */
    RequestBuilder getRequestBuilder();

    /**
     * Get a builder for the response object. May return {@code null} if a
     * response is not available, e.g. in a
     * {@link RequestFilter}, and has not been set.
     * A newly built response can be set by calling
     * {@link #setResponse(javax.ws.rs.core.Response)}.
     *
     * @return response builder object or {@code null}.
     */
    Response.ResponseBuilder getResponseBuilder();

    /**
     * Create a fresh response builder instance. A caching filter
     * could call this method to get a response builder and
     * initialize it from a cache. This method does not update
     * the state of the context object.
     *
     * @return newly created response builder
     * @see #setResponse(javax.ws.rs.core.Response)
     */
    Response.ResponseBuilder createResponse();
}
