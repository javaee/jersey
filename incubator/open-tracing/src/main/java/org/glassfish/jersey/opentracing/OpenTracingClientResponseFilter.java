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

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;

import io.opentracing.Span;
import io.opentracing.tag.Tags;

/**
 * Retrieves stored span from the {@link ClientRequestContext} and adds response details to the tracing info.
 *
 * @author Adam Lindenthal (adam.lindenthal at oracle.com)
 * @since 2.26
 */
public class OpenTracingClientResponseFilter implements ClientResponseFilter {

    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
        final Object spanProperty = requestContext.getProperty(OpenTracingFeature.SPAN_CONTEXT_PROPERTY);
        if (spanProperty != null && spanProperty instanceof Span) {
            ((Span) spanProperty)
                    .setTag(Tags.HTTP_STATUS.getKey(), responseContext.getStatus())
                    .setTag(LocalizationMessages.OPENTRACING_TAG_HAS_RESPONSE_ENTITY(), responseContext.hasEntity())
                    .setTag(LocalizationMessages.OPENTRACING_TAG_RESPONSE_LENGTH(), responseContext.getLength())
                    .setTag(LocalizationMessages.OPENTRACING_TAG_RESPONSE_HEADERS(),
                            OpenTracingUtils.headersAsString(responseContext.getHeaders()))
                    .finish();
        }
    }
}
