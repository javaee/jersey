/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.server.internal;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.InterceptorContext;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Provider;

import org.glassfish.jersey.JerseyPriorities;
import org.glassfish.jersey.message.MessageUtils;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.JSONP;

import jersey.repackaged.com.google.common.collect.Maps;
import jersey.repackaged.com.google.common.collect.Sets;

/**
 * A {@link WriterInterceptor} implementation for JSONP format. This interceptor wraps a JSON stream obtained by a underlying
 * JSON provider into a callback function that can be defined by the {@link JSONP} annotation.
 *
 * @author Michal Gajdos
 * @see JSONP
 */
@Priority(JerseyPriorities.POST_ENTITY_CODER)
// this interceptor has to run after content encoders (e.g. gzip/deflate), otherwise the added content (padding with the callback
// method call would not be encoded.
public class JsonWithPaddingInterceptor implements WriterInterceptor {

    private static final Map<String, Set<String>> JAVASCRIPT_TYPES;

    static {
        JAVASCRIPT_TYPES = Maps.newHashMapWithExpectedSize(2);

        JAVASCRIPT_TYPES.put("application", Sets.newHashSet("x-javascript", "ecmascript", "javascript"));
        JAVASCRIPT_TYPES.put("text", Sets.newHashSet("javascript", "x-javascript", "ecmascript", "jscript"));
    }

    @Inject
    private Provider<ContainerRequest> containerRequestProvider;

    @Override
    public void aroundWriteTo(final WriterInterceptorContext context) throws IOException, WebApplicationException {
        final boolean isJavascript = isJavascript(context.getMediaType());
        final JSONP jsonp = getJsonpAnnotation(context);

        final boolean wrapIntoCallback = isJavascript && jsonp != null;

        if (wrapIntoCallback) {
            context.setMediaType(MediaType.APPLICATION_JSON_TYPE);

            context.getOutputStream().write(getCallbackName(jsonp).getBytes(MessageUtils.getCharset(context.getMediaType())));
            context.getOutputStream().write('(');
        }

        context.proceed();

        if (wrapIntoCallback) {
            context.getOutputStream().write(')');
        }
    }

    /**
     * Returns a flag whether the given {@link MediaType media type} belongs to the group of JavaScript media types.
     *
     * @param mediaType media type to check.
     * @return {@code true} if the given media type is a JavaScript type, {@code false} otherwise (or if the media type is
     *         {@code null}}
     */
    private boolean isJavascript(final MediaType mediaType) {
        if (mediaType == null) {
            return false;
        }

        final Set<String> subtypes = JAVASCRIPT_TYPES.get(mediaType.getType());
        return subtypes != null && subtypes.contains(mediaType.getSubtype());
    }

    /**
     * Returns a JavaScript callback name to wrap the JSON entity into. The callback name is determined from the {@link JSONP}
     * annotation.
     *
     * @param jsonp {@link JSONP} annotation to determine the callback name from.
     * @return a JavaScript callback name.
     */
    private String getCallbackName(final JSONP jsonp) {
        String callback = jsonp.callback();

        if (!"".equals(jsonp.queryParam())) {
            final ContainerRequest containerRequest = containerRequestProvider.get();
            final UriInfo uriInfo = containerRequest.getUriInfo();
            final MultivaluedMap<String, String> queryParameters = uriInfo.getQueryParameters();
            final List<String> queryParameter = queryParameters.get(jsonp.queryParam());

            callback = (queryParameter != null && !queryParameter.isEmpty()) ? queryParameter.get(0) : callback;
        }

        return callback;
    }

    /**
     * Returns a {@link JSONP} annotation of the resource method responsible for handling the current request.
     *
     * @param context an {@link InterceptorContext interceptor context} to obtain the annotation from.
     * @return {@link JSONP} annotation or {@code null} if the resource method is not annotated with this annotation.
     * @see javax.ws.rs.ext.InterceptorContext#getAnnotations()
     */
    private JSONP getJsonpAnnotation(final InterceptorContext context) {
        final Annotation[] annotations = context.getAnnotations();

        if (annotations != null && annotations.length > 0) {
            for (final Annotation annotation : annotations) {
                if (annotation instanceof JSONP) {
                    return (JSONP) annotation;
                }
            }
        }

        return null;
    }
}
