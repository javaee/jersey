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

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedMap;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;

/**
 * Utility methods for Jersey OpenTracing integration.
 *
 * @author Adam Lindenthal (adam.lindenthal at oracle.com)
 * @since 2.26
 */
public class OpenTracingUtils {

    private OpenTracingUtils() {
    }

    /**
     * Resolve resource-level span.
     * <p>
     * If open tracing is enabled and {@link GlobalTracer} is registered, resource-level span should be stored in the
     * {@link OpenTracingFeature#SPAN_CONTEXT_PROPERTY}. This span is resolved and returned as an {@link Optional}.
     *
     * @param context {@link ContainerRequestContext} instance, can be obtained via {@code @Context} injection
     * @return {@link Optional} of the resolved span, if found; empty optional if not
     */
    public static Optional<Span> getRequestSpan(final ContainerRequestContext context) {
        if (context != null) {
            final Object spanProperty = context.getProperty(OpenTracingFeature.SPAN_CONTEXT_PROPERTY);
            if (spanProperty != null && spanProperty instanceof Span) {
                return Optional.of((Span) spanProperty);
            }
        }
        return Optional.empty();
    }

    /**
     * Create and start ad-hoc custom span with the default name as a child span of the request span (if available).
     *
     * @param context {@link ContainerRequestContext} instance, can be obtained via {@code @Context} injection
     * @return If parent span ("request span") instance is stored in the {@code ContainerRequestContext}, new span is created
     * as a child span of the found span. If no parent span found, new "root" span is created. In both cases, the returned span
     * is already started. In order to successfully store the tracing, {@link Span#finish()} needs to be invoked explicitly,
     * after the traced code finishes.
     */
    public static Span getRequestChildSpan(final ContainerRequestContext context) {
        return getRequestChildSpan(context, OpenTracingFeature.DEFAULT_CHILD_SPAN_NAME);
    }

    /**
     * Create and start ad-hoc custom span with a custom name as a child span of the request span (if available).
     *
     * @param context  {@link ContainerRequestContext} instance, can be obtained via {@code @Context} injection
     * @param spanName name to be used for the created span
     * @return If parent span ("request span") instance is stored in the {@code ContainerRequestContext}, new span is created
     * as a child span of the found span. If no parent span found, new "root" span is created. In both cases, the returned span
     * is already started. In order to successfully store the tracing, {@link Span#finish()} needs to be invoked explicitly,
     * after the traced code finishes.
     */
    public static Span getRequestChildSpan(final ContainerRequestContext context, final String spanName) {
        Tracer.SpanBuilder spanBuilder = GlobalTracer.get().buildSpan(spanName);
        if (context != null) {
            final Object spanProperty = context.getProperty(OpenTracingFeature.SPAN_CONTEXT_PROPERTY);
            if (spanProperty != null && spanProperty instanceof Span) {
                spanBuilder = spanBuilder.asChildOf((Span) spanProperty);
            }
        }
        return spanBuilder.startManual();
    }

    /**
     * Convert request/response headers from {@link MultivaluedMap} into printable form.
     *
     * @param headers multi-valued map of request or response headers
     * @return {@code String} representation, e.g. "[header1=foo]; [header2=bar, baz]"
     */
    static String headersAsString(final MultivaluedMap<String, ?> headers) {
        return headers.entrySet()
                .stream()
                .map((entry) -> "["
                        + entry.getKey() + "="
                        + entry.getValue()
                        .stream()
                        .map(Object::toString)
                        .collect(Collectors.joining(", "))
                        + "]")
                .collect(Collectors.joining("; "));
    }

    static String formatList(List<?> list) {
        return list.stream().map(Object::toString).collect(Collectors.joining(", "));
    }

    static String formatProviders(Iterable<?> providers) {
        return StreamSupport.stream(providers.spliterator(), false)
                .map((provider) -> provider.getClass().getName())
                .collect(Collectors.joining(", "));
    }
}
