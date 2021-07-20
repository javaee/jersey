/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package org.glassfish.jersey.opentracing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseFilter;

import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapExtractAdapter;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;

/**
 * Application event listener responsible for creating and propagating server-side request {@link io.opentracing.Span}.
 *
 * @author Adam Lindenthal (adam.lindenthal at oracle.com)
 * @since 2.26
 */
class OpenTracingApplicationEventListener implements ApplicationEventListener {
    private final Tracer globalTracer = GlobalTracer.get();
    private final OpenTracingFeature.Verbosity verbosity;

    /**
     * Creates event listener instance with given {@link org.glassfish.jersey.opentracing.OpenTracingFeature.Verbosity}.
     *
     * @param verbosity desired verbosity level
     */
    public OpenTracingApplicationEventListener(OpenTracingFeature.Verbosity verbosity) {
        this.verbosity = verbosity;
    }

    @Override
    public void onEvent(ApplicationEvent event) {
        // we don't care about the server lifecycle
    }

    @Override
    public RequestEventListener onRequest(RequestEvent requestEvent) {
        if (requestEvent.getType() == RequestEvent.Type.START) {
            Span requestSpan = handleRequestStart(requestEvent.getContainerRequest());
            return new OpenTracingRequestEventListener(requestSpan);
        }
        return null;
    }

    private Span handleRequestStart(ContainerRequest request) {

        final Map<String, String> mappedHeaders = request
                .getHeaders()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        (entry) -> OpenTracingUtils.formatList(entry.getValue())));

        final SpanContext extractedContext =
                globalTracer.extract(Format.Builtin.HTTP_HEADERS, new TextMapExtractAdapter(mappedHeaders));

        Tracer.SpanBuilder spanBuilder = globalTracer
                .buildSpan(OpenTracingFeature.DEFAULT_REQUEST_SPAN_NAME)
                .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER)
                .withTag(Tags.HTTP_METHOD.getKey(), request.getMethod())
                .withTag(Tags.HTTP_URL.getKey(), request.getRequestUri().toASCIIString())
                .withTag(LocalizationMessages.OPENTRACING_TAG_REQUEST_HEADERS(),
                        OpenTracingUtils.headersAsString(request.getHeaders()))
                .withTag(LocalizationMessages.OPENTRACING_TAG_HAS_REQUEST_ENTITY(), request.hasEntity());

        if (extractedContext != null) {
            spanBuilder = spanBuilder.asChildOf(extractedContext);
        }

        final Span span = spanBuilder.startManual();
        request.setProperty(OpenTracingFeature.SPAN_CONTEXT_PROPERTY, span);
        span.log(LocalizationMessages.OPENTRACING_LOG_REQUEST_STARTED());
        return span;
    }

    class OpenTracingRequestEventListener implements RequestEventListener {
        private Span requestSpan;
        private Span resourceSpan = null;

        OpenTracingRequestEventListener(final Span requestSpan) {
            this.requestSpan = requestSpan;
        }

        @Override
        public void onEvent(RequestEvent event) {

            switch (event.getType()) {
                case MATCHING_START:
                    logVerbose(LocalizationMessages.OPENTRACING_LOG_MATCHING_STARTED());
                    break;

                case LOCATOR_MATCHED:
                    logVerbose(LocalizationMessages.OPENTRACING_LOG_LOCATOR_MATCHED(
                            OpenTracingUtils.formatList(event.getUriInfo().getMatchedResourceLocators())));
                    break;

                case SUBRESOURCE_LOCATED:
                    logVerbose(LocalizationMessages.OPENTRACING_LOG_SUBRESOURCE_LOCATED(
                            OpenTracingUtils.formatList(event.getUriInfo().getLocatorSubResources())));
                    break;


                case REQUEST_MATCHED:
                    logVerbose(LocalizationMessages.OPENTRACING_LOG_REQUEST_MATCHED(event.getUriInfo()
                            .getMatchedResourceMethod()
                            .getInvocable()
                            .getDefinitionMethod()));
                    log(LocalizationMessages.OPENTRACING_LOG_REQUEST_FILTERING_STARTED());
                    break;

                case REQUEST_FILTERED:
                    List<ContainerRequestFilter> requestFilters = new ArrayList<>();
                    event.getContainerRequestFilters().forEach(requestFilters::add);

                    logVerbose(LocalizationMessages.OPENTRACING_LOG_REQUEST_FILTERING_FINISHED(requestFilters.size()));
                    if (requestFilters.size() > 0) {
                        log(LocalizationMessages.OPENTRACING_LOG_APPLIED_REQUEST_FILTERS(
                                OpenTracingUtils.formatProviders(requestFilters)));
                    }
                    break;

                case RESOURCE_METHOD_START:
                    logVerbose(LocalizationMessages.OPENTRACING_LOG_RESOURCE_METHOD_STARTED(
                            event.getUriInfo().getMatchedResourceMethod().getInvocable().getDefinitionMethod()));

                    resourceSpan = globalTracer.buildSpan(OpenTracingFeature.DEFAULT_RESOURCE_SPAN_NAME)
                                               .asChildOf(requestSpan)
                                               .startManual();

                    event.getContainerRequest().setProperty(OpenTracingFeature.SPAN_CONTEXT_PROPERTY, resourceSpan);
                    break;

                case RESOURCE_METHOD_FINISHED:
                    log(LocalizationMessages.OPENTRACING_LOG_RESOURCE_METHOD_FINISHED());
                    break;

                case RESP_FILTERS_START:
                    // this is the first event after resource method is guaranteed to have finished, even for asynchronous
                    // processing; resourceSpan will be finished and the span in the context will be switched back to the
                    // resource span before any further tracing can occur.
                    event.getContainerRequest().setProperty(OpenTracingFeature.SPAN_CONTEXT_PROPERTY, requestSpan);
                    resourceSpan.finish();
                    logVerbose(LocalizationMessages.OPENTRACING_LOG_RESPONSE_FILTERING_STARTED());
                    break;

                case RESP_FILTERS_FINISHED:
                    List<ContainerResponseFilter> responseFilters = new ArrayList<>();
                    event.getContainerResponseFilters().forEach(responseFilters::add);
                    logVerbose(LocalizationMessages.OPENTRACING_LOG_RESPONSE_FILTERING_FINISHED(responseFilters.size()));
                    if (responseFilters.size() > 0) {
                        log(LocalizationMessages.OPENTRACING_LOG_APPLIED_RESPONSE_FILTERS(
                                OpenTracingUtils.formatProviders(responseFilters)));
                    }
                    break;

                case ON_EXCEPTION:
                    if (resourceSpan != null) {
                        resourceSpan.setTag(Tags.ERROR.getKey(), true);
                        resourceSpan.finish();
                    }
                    requestSpan.setTag(Tags.ERROR.getKey(), true);
                    logError(event.getException());
                    break;

                case EXCEPTION_MAPPER_FOUND:
                    log(LocalizationMessages.OPENTRACING_LOG_EXCEPTION_MAPPER_FOUND(
                            event.getExceptionMapper().getClass().getName()));
                    break;

                case EXCEPTION_MAPPING_FINISHED:
                    log(LocalizationMessages.OPENTRACING_LOG_EXCEPTION_MAPPING_FINISHED()
                            + (event.isResponseSuccessfullyMapped()
                            ? LocalizationMessages.OPENTRACING_LOG_EXCEPTION_MAPPING_SUCCESS()
                            : LocalizationMessages.OPENTRACING_LOG_EXCEPTION_MAPPING_NOEXCEPTION_OR_FAILED()));

                    break;


                case FINISHED:
                    if (requestSpan != null) {
                        ContainerResponse response = event.getContainerResponse();
                        if (response != null) {
                            int status = response.getStatus();
                            requestSpan
                                    .setTag(Tags.HTTP_STATUS.getKey(), status)
                                    .setTag(LocalizationMessages.OPENTRACING_TAG_HAS_RESPONSE_ENTITY(), response.hasEntity())
                                    .setTag(LocalizationMessages.OPENTRACING_TAG_RESPONSE_LENGTH(), response.getLength());

                            if (400 <= status) {
                                requestSpan.setTag(Tags.ERROR.getKey(), true);
                            }
                        }
                        requestSpan.finish();
                    }
                    break;
            }
        }

        /**
         * Adds a {@link OpenTracingFeature.Verbosity#TRACE}-level log entry into the request span.
         * @param s log message
         */
        private void logVerbose(String s) {
            log(OpenTracingFeature.Verbosity.TRACE, s);
        }

        /**
         * Adds a {@link OpenTracingFeature.Verbosity#INFO}-level log entry into the request span.
         * @param s log message
         */
        private void log(String s) {
            log(OpenTracingFeature.Verbosity.INFO, s);
        }

        /**
         * Adds a log entry with given {@link org.glassfish.jersey.opentracing.OpenTracingFeature.Verbosity}-level into the
         * request span.
         *
         * @param level desired verbosity level
         * @param s log message
         */
        private void log(OpenTracingFeature.Verbosity level, String s) {
            if (level.ordinal() <= verbosity.ordinal()) {
                requestSpan.log(s);
            }
        }

        /**
         * Adds an error log into the request span.
         * @param t exception to be logged.
         */
        private void logError(final Throwable t) {
            Map<String, Object> errorMap = new HashMap<>(2);
            errorMap.put("event", "error");
            errorMap.put("error.object", t);
            requestSpan.log(errorMap);
        }
    }

}
