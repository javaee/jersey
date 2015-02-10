/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.server.internal.monitoring;

import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.ExceptionMapper;

import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.ExtendedUriInfo;
import org.glassfish.jersey.server.monitoring.RequestEvent;

/**
 * A contract for {@link RequestEvent request monitoring event} builder.
 *
 * @author Miroslav Fuksa
 */
public interface RequestEventBuilder {

    /**
     * Set the exception mapper.
     *
     * @param exceptionMapper Exception mapper.
     * @return Builder instance.
     */
    public RequestEventBuilder setExceptionMapper(ExceptionMapper<?> exceptionMapper);

    /**
     * Set the container request.
     *
     * @param containerRequest Container request.
     * @return Builder instance.
     */
    public RequestEventBuilder setContainerRequest(ContainerRequest containerRequest);

    /**
     * Set the container response.
     *
     * @param containerResponse Container response.
     * @return Builder instance.
     */
    public RequestEventBuilder setContainerResponse(ContainerResponse containerResponse);

    /**
     * Set the flag indicating whether the response processing was successful. Set {@code true}
     * if the request and response has been successfully processed. Response is successfully
     * processed when the response code is smaller than 400 and response was successfully written.
     *
     * @param success True if response processing was successful.
     * @return Builder instance.
     *
     * @see RequestEvent#isSuccess()
     */
    public RequestEventBuilder setSuccess(boolean success);

    /**
     * Set the flag indicating whether response has been successfully written.
     *
     * @param responseWritten {@code true} is response has been written without failure.
     * @return Builder instance.
     */
    public RequestEventBuilder setResponseWritten(boolean responseWritten);

    /**
     * Set exception thrown.
     *
     * @param throwable      Exception.
     * @param exceptionCause Cause of the {@code throwable}
     * @return Builder instance.
     */
    public RequestEventBuilder setException(Throwable throwable, RequestEvent.ExceptionCause exceptionCause);

    /**
     * Set uri info.
     *
     * @param extendedUriInfo Extended uri info.
     * @return Builder instance.
     */
    public RequestEventBuilder setExtendedUriInfo(ExtendedUriInfo extendedUriInfo);

    /**
     * Set response filters.
     *
     * @param containerResponseFilters Container response filters.
     * @return Builder instance.
     */
    public RequestEventBuilder setContainerResponseFilters(Iterable<ContainerResponseFilter> containerResponseFilters);

    /**
     * Set request filters.
     *
     * @param containerRequestFilters Container request filters.
     * @return Request filters.
     */
    public RequestEventBuilder setContainerRequestFilters(Iterable<ContainerRequestFilter> containerRequestFilters);

    /**
     * Set the flag indicating whether the response has been successfully mapped by an exception mapper.
     *
     * @param responseSuccessfullyMapped {@code true} if the response has been successfully mapped.
     * @return Builder instance.
     */
    public RequestEventBuilder setResponseSuccessfullyMapped(boolean responseSuccessfullyMapped);

    /**
     * Build the instance of {@link RequestEvent request event}.
     *
     * @param eventType Type of the event to be built.
     * @return Request event instance.
     */
    public RequestEvent build(RequestEvent.Type eventType);
}
