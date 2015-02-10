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
 * {@link RequestEvent Request event} implementation. Instances are immutable.
 *
 * @author Miroslav Fuksa
 */
public class RequestEventImpl implements RequestEvent {


    /**
     * Builder of {@link RequestEventImpl}.
     */
    public static class Builder implements RequestEventBuilder {
        private ContainerRequest containerRequest;
        private ContainerResponse containerResponse;
        private Throwable throwable;
        private ExtendedUriInfo extendedUriInfo;
        private Iterable<ContainerResponseFilter> containerResponseFilters;
        private Iterable<ContainerRequestFilter> containerRequestFilters;
        private ExceptionMapper<?> exceptionMapper;
        private boolean success;
        private boolean responseWritten;
        private boolean responseSuccessfullyMapped;
        private ExceptionCause exceptionCause;

        @Override
        public Builder setExceptionMapper(ExceptionMapper<?> exceptionMapper) {
            this.exceptionMapper = exceptionMapper;
            return this;
        }

        @Override
        public Builder setContainerRequest(ContainerRequest containerRequest) {
            this.containerRequest = containerRequest;
            return this;
        }

        @Override
        public Builder setContainerResponse(ContainerResponse containerResponse) {
            this.containerResponse = containerResponse;
            return this;
        }

        @Override
        public Builder setResponseWritten(boolean responseWritten) {
            this.responseWritten = responseWritten;
            return this;
        }

        @Override
        public Builder setSuccess(boolean success) {
            this.success = success;
            return this;
        }

        @Override
        public Builder setException(Throwable throwable, ExceptionCause exceptionCause) {
            this.throwable = throwable;
            this.exceptionCause = exceptionCause;
            return this;
        }


        @Override
        public Builder setExtendedUriInfo(ExtendedUriInfo extendedUriInfo) {
            this.extendedUriInfo = extendedUriInfo;
            return this;
        }

        @Override
        public Builder setContainerResponseFilters(Iterable<ContainerResponseFilter> containerResponseFilters) {
            this.containerResponseFilters = containerResponseFilters;
            return this;
        }

        @Override
        public Builder setContainerRequestFilters(Iterable<ContainerRequestFilter> containerRequestFilters) {
            this.containerRequestFilters = containerRequestFilters;
            return this;
        }

        @Override
        public Builder setResponseSuccessfullyMapped(boolean responseSuccessfullyMapped) {
            this.responseSuccessfullyMapped = responseSuccessfullyMapped;
            return this;
        }

        @Override
        public RequestEventImpl build(Type type) {
            return new RequestEventImpl(type, containerRequest, containerResponse, throwable,
                    extendedUriInfo, containerResponseFilters, containerRequestFilters, exceptionMapper, success,
                    responseSuccessfullyMapped, exceptionCause, responseWritten);
        }
    }


    private RequestEventImpl(Type type, ContainerRequest containerRequest, ContainerResponse containerResponse,
                             Throwable throwable,
                             ExtendedUriInfo extendedUriInfo, Iterable<ContainerResponseFilter> containerResponseFilters,
                             Iterable<ContainerRequestFilter> containerRequestFilters,
                             ExceptionMapper<?> exceptionMapper,
                             boolean success,
                             boolean responseSuccessfullyMapped, ExceptionCause exceptionCause, boolean responseWritten) {
        this.type = type;
        this.containerRequest = containerRequest;
        this.containerResponse = containerResponse;
        this.throwable = throwable;
        this.extendedUriInfo = extendedUriInfo;
        this.containerResponseFilters = containerResponseFilters;
        this.containerRequestFilters = containerRequestFilters;
        this.exceptionMapper = exceptionMapper;
        this.success = success;
        this.responseSuccessfullyMapped = responseSuccessfullyMapped;
        this.exceptionCause = exceptionCause;
        this.responseWritten = responseWritten;
    }


    private final Type type;
    private final ContainerRequest containerRequest;
    private final ContainerResponse containerResponse;
    private final Throwable throwable;
    private final ExtendedUriInfo extendedUriInfo;
    private final Iterable<ContainerResponseFilter> containerResponseFilters;
    private final Iterable<ContainerRequestFilter> containerRequestFilters;
    private final ExceptionMapper<?> exceptionMapper;
    private final boolean success;
    private final boolean responseSuccessfullyMapped;
    private final ExceptionCause exceptionCause;
    private final boolean responseWritten;


    @Override
    public ContainerRequest getContainerRequest() {
        return containerRequest;
    }

    @Override
    public ContainerResponse getContainerResponse() {
        return containerResponse;
    }

    @Override
    public Throwable getException() {
        return throwable;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public ExtendedUriInfo getUriInfo() {
        return extendedUriInfo;
    }

    @Override
    public ExceptionMapper<?> getExceptionMapper() {
        return exceptionMapper;
    }

    @Override
    public Iterable<ContainerRequestFilter> getContainerRequestFilters() {
        return containerRequestFilters;
    }

    @Override
    public Iterable<ContainerResponseFilter> getContainerResponseFilters() {
        return containerResponseFilters;
    }

    @Override
    public boolean isSuccess() {
        return success;
    }

    @Override
    public boolean isResponseSuccessfullyMapped() {
        return responseSuccessfullyMapped;
    }

    @Override
    public ExceptionCause getExceptionCause() {
        return exceptionCause;
    }

    @Override
    public boolean isResponseWritten() {
        return responseWritten;
    }
}
