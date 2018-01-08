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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.MediaType;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapInjectAdapter;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;

/**
 * Client-side request filter, that creates the client request {@code Span}.
 * <p>
 * Stores request-related metadata into the {@code Span} as {@code Tags}
 * and {@link GlobalTracer#inject(SpanContext, Format, Object) injects} it into http headers.
 *
 * @author Adam Lindenthal (adam.lindenthal at oracle.com)
 * @since 2.26
 */
class OpenTracingClientRequestFilter implements ClientRequestFilter {

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        Tracer.SpanBuilder spanBuilder = GlobalTracer.get()
                .buildSpan(LocalizationMessages.OPENTRACING_SPAN_PREFIX_CLIENT() + requestContext.getMethod())
                .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT)
                .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT)
                .withTag(Tags.HTTP_URL.getKey(), requestContext.getUri().toASCIIString())
                .withTag(Tags.HTTP_METHOD.getKey(), requestContext.getMethod())
                .withTag(LocalizationMessages.OPENTRACING_TAG_HAS_REQUEST_ENTITY(), requestContext.hasEntity())
                .withTag(LocalizationMessages.OPENTRACING_TAG_ACCEPTABLE_MEDIA_TYPES(), requestContext.getAcceptableMediaTypes()
                        .stream()
                        .map(MediaType::toString)
                        .collect(Collectors.joining(", ")))
                .withTag(LocalizationMessages.OPENTRACING_TAG_REQUEST_HEADERS(),
                        OpenTracingUtils.headersAsString(requestContext.getHeaders()));

        // if pre-stored "span" property is found, propagate the stored context
        final Object property = requestContext.getProperty(OpenTracingFeature.SPAN_CONTEXT_PROPERTY);
        if (property != null && property instanceof SpanContext) {
            spanBuilder = spanBuilder.asChildOf((SpanContext) property);
        }
        Span span = spanBuilder.startManual();

        requestContext.setProperty(OpenTracingFeature.SPAN_CONTEXT_PROPERTY, span);
        Map<String, String> addedHeaders = new HashMap<>();
        GlobalTracer.get().inject(span.context(), Format.Builtin.HTTP_HEADERS, new TextMapInjectAdapter(addedHeaders));
        addedHeaders.forEach((key, value) -> requestContext.getHeaders().add(key, value));
    }
}
