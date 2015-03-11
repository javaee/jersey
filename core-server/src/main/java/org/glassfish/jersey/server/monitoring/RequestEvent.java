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

package org.glassfish.jersey.server.monitoring;

import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.ExceptionMapper;

import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.ExtendedUriInfo;
import org.glassfish.jersey.server.model.Resource;

/**
 * An event informing about details of a request processing. The event is created by a Jersey runtime and
 * handled by {@link RequestEventListener} (javadoc of listener describes how to register the listener for
 * particular request).
 * <p/>
 * The event contains the {@link Type} which distinguishes between types of event. There are various
 * properties in the event (accessible by getters) and some of them might be relevant only to specific event types.
 * <p/>
 * Note that internal state of the event must be modified. Even the event is immutable it exposes objects
 * which might be mutable and the code of event listener must not change state of these objects.
 *
 * @author Miroslav Fuksa
 */
public interface RequestEvent {

    /**
     * The type of the event which describes in which request processing phase the event
     * is triggered.
     */
    public static enum Type {
        /**
         * The request processing has started. This event type is handled only by
         * {@link org.glassfish.jersey.server.monitoring.ApplicationEventListener#onRequest(RequestEvent)} and will
         * never be called for {@link RequestEventListener#onEvent(RequestEvent)}.
         */
        START,

        /**
         * The matching of the resource and resource method has started.
         */
        MATCHING_START,
        /**
         * The sub resource locator method is found and it will be called.
         * The locator method can be retrieved from {@link #getUriInfo()} by method
         * {@link org.glassfish.jersey.server.ExtendedUriInfo#getMatchedResourceLocators()}.
         */
        LOCATOR_MATCHED,

        /**
         * The sub resource has been returned from sub resource locator, model was constructed, enhanced by
         * {@link org.glassfish.jersey.server.model.ModelProcessor model processor}, validated and the matching
         * is going to be performed on the sub {@link Resource resource}.
         * The sub resource can be retrieved from {@link #getUriInfo()} by method
         * {@link org.glassfish.jersey.server.ExtendedUriInfo#getLocatorSubResources()}.
         */
        SUBRESOURCE_LOCATED,

        /**
         * The matching has been finished and {@link ContainerRequestFilter container request filters}
         * are going to be executed. The request filters can be retrieved from event by
         * {@link #getContainerRequestFilters()} method. This method also determines end of the matching
         * process and therefore the matching results can be retrieved using {@link #getUriInfo()}.
         */
        REQUEST_MATCHED,

        /**
         * Execution of {@link ContainerRequestFilter container request filters} has been finished.
         */
        REQUEST_FILTERED,

        /**
         * Resource method is going to be executed. The resource method can be extracted from {@link ExtendedUriInfo}
         * returned by {@link #getUriInfo()}.
         */
        RESOURCE_METHOD_START,

        /**
         * Resource method execution has finished. In the case of synchronous processing
         * the response is not available yet. In the case of asynchronous processing the situation depends on the
         * method design and it in some cases on race conditions. In asynchronous cases this event can be
         * triggered even after the response is completely processed. Exactly defined, this event
         * is triggered when the thread executing the resource method returns from the resource method.
         */
        RESOURCE_METHOD_FINISHED,

        /**
         * {@link ContainerResponseFilter Container response filters} are going to be executed. In this point
         * the response is already available and can be retrieved by {@link #getContainerResponse()}. The
         * response filters can be retrieved by {@link #getContainerResponseFilters()}.
         * <p/>
         * This phase is executed in the regular response processing but might also been executed for
         * processing on response mapped from exceptions by {@link ExceptionMapper exception mappers}.
         * In this case the {@link #ON_EXCEPTION} event type precedes this event.
         */
        RESP_FILTERS_START,

        /**
         * Execution of {@link ContainerResponseFilter Container response filters} has finished.
         * <p/>
         * This phase is executed in the regular response processing but might also been executed for
         * processing on response mapped from exceptions by {@link ExceptionMapper exception mappers}.
         * In this case the {@link #ON_EXCEPTION} event type precedes this event.
         */
        RESP_FILTERS_FINISHED,

        /**
         * Exception has been thrown during the request/response processing. This situation can
         * occur in almost all phases of request processing and therefore there is no fixed order of
         * events in which this event type can be triggered.
         * <p/>
         * The origin of exception can be retrieved
         * by {@link #getExceptionCause()}. This event type can be received even two types in the case
         * when first exception is thrown during the standard request processing and the second one
         * is thrown during the processing of the response mapped from the exception.
         * <p/>
         * The exception thrown can be retrieved by {@link #getException()}.
         */
        ON_EXCEPTION,

        /**
         * An {@link ExceptionMapper} is successfully found and it is going to be executed. The
         * {@code ExceptionMapper} can be retrieved by {@link #getExceptionMapper()}.
         */
        EXCEPTION_MAPPER_FOUND,

        /**
         * Exception mapping is finished. The result of exception mapping can be checked by
         * {@link #isResponseSuccessfullyMapped()} which returns true when the exception mapping
         * was successful. In this case the new response is available in the {@link #getContainerResponse()}.
         */
        EXCEPTION_MAPPING_FINISHED,

        /**
         * The request and response processing has finished. The result of request processing can be checked
         * by {@link #isSuccess()} method. This method is called even when request processing fails and ends
         * up with not handled exceptions.
         */
        FINISHED
    }


    /**
     * Describes the origin of the exception.
     */
    public static enum ExceptionCause {
        /**
         * An exception was thrown during the standard request and response processing and was not thrown
         * from processing of mapped response.
         */
        ORIGINAL,
        /**
         * An exception was thrown during the processing of response
         * mapped from {@link ExceptionMapper exception mappers}.
         */
        MAPPED_RESPONSE
    }

    /**
     * Returns the {@link Type type} of this event.
     *
     * @return Request event type.
     */
    public Type getType();

    /**
     * Get the container request. The container request is available for all event types. Returned
     * request must not be modified by the {@link RequestEventListener request event listener}.
     *
     * @return The non-null container request.
     */
    public ContainerRequest getContainerRequest();

    /**
     * Get the container response. The response is available only for certain {@link Type event types}. The
     * returned response might vary also on the event type. The getter returns always the latest response being
     * processed. So, for example for event {@link Type#EXCEPTION_MAPPING_FINISHED} event type the method
     * returns mapped response and not the original response created from execution of the resource method.
     *
     * @return Latest response being processed or {@code null} if no response has been produced yet.
     */
    public ContainerResponse getContainerResponse();

    /**
     * Get the latest exception, if any, thrown by the request and response processing. When this method
     * returns not null value, the method {@link #getExceptionCause()} returns the origin of the exception.
     *
     * @return Exception thrown or {@code null} if no exception has been thrown.
     */
    public Throwable getException();

    /**
     * Get the {@link ExtendedUriInfo extended uri info} associated with this request. This method returns
     * null for {@link Type#START} event. The returned {@code ExtendedUriInfo} can be used to retrieve
     * information relevant to many event types (especially event types describing the matching process).
     *
     * @return Extended uri info or {@code null} if it is not available yet.
     */
    public ExtendedUriInfo getUriInfo();

    /**
     * Get the {@link ExceptionMapper} that was found and used during the exception mapping phase.
     *
     * @return Exception mapper or {@code null} if no exception mapper was found or even needed.
     */
    public ExceptionMapper<?> getExceptionMapper();


    /**
     * Get {@link ContainerRequestFilter container request filters} used during the request filtering
     * phase.
     *
     * @return Container request filters or {@code null} if no filters were used yet.
     */
    public Iterable<ContainerRequestFilter> getContainerRequestFilters();

    /**
     * Get {@link ContainerResponseFilter container response filters} used during the response filtering
     * phase.
     *
     * @return Container response filter or {@code null} if no filters were used yet.
     */
    public Iterable<ContainerResponseFilter> getContainerResponseFilters();

    /**
     * Return {@code true} if the request and response has been successfully processed. Response is successfully
     * processed when the response code is smaller than 400 and response was successfully written. If the exception
     * occurred but was mapped into a response with successful response code and written, this method returns
     * {@code true}.
     *
     * @return True if the response was successfully processed.
     */
    public boolean isSuccess();

    /**
     * Returns {@code true} if the response was successfully mapped from an exception
     * by {@link ExceptionMapper exception mappers}. When exception mapping phase failed or when
     * no exception was thrown at all the, the method returns false. This method is convenient when
     * handling the {@link Type#EXCEPTION_MAPPING_FINISHED} event type.
     *
     * @return True if the exception occurred and it was successfully mapped into a response.
     */
    public boolean isResponseSuccessfullyMapped();

    /**
     * Get the {@link ExceptionCause exception cause}. This method is relevant only in cases when
     * {@link #getException()} returns non-null value (for example when handling {@link Type#ON_EXCEPTION})
     * event type.
     *
     * @return Exception cause of the latest exception or {@code null} if no exception has occurred.
     */
    public ExceptionCause getExceptionCause();

    /**
     * Returns {@code true} if the response has been successfully written. {@code true} is returned
     * even for cases when the written response contains error response code.
     *
     * @return {@code true} if the response was successfully written;{@code false} when the response
     *         has not been written yet or when writing of response failed.
     */
    public boolean isResponseWritten();
}
