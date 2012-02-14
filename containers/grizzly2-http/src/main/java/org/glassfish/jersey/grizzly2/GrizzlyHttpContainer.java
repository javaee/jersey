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
package org.glassfish.jersey.grizzly2;

import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.utils.Charsets;
import org.glassfish.jersey.message.internal.Requests;
import org.glassfish.jersey.server.Application;
import org.glassfish.jersey.server.ContainerResponseWriter;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request.RequestBuilder;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

/**
 *
 * Grizzly 2 Jersey HTTP Container Prototype
 *
 * @author Jakub Podlesak
 */
public final class GrizzlyHttpContainer extends HttpHandler {

    private final static class Writer implements ContainerResponseWriter {

        final Response grizzlyResponse;

        Writer(final Response response) {
            this.grizzlyResponse = response;
        }

        @Override
        public void finish() throws IOException {
        }

        @Override
        public OutputStream writeStatusAndHeaders(final long contentLength,
                final javax.ws.rs.core.Response jaxrsResponse) throws IOException {

            grizzlyResponse.setStatus(jaxrsResponse.getStatus());

            for (final Map.Entry<String, List<String>> e : jaxrsResponse.getHeaders().asMap().entrySet()) {
                for (final String value : e.getValue()) {
                    grizzlyResponse.addHeader(e.getKey(), value);
                }
            }

            final String contentType = jaxrsResponse.getHeaders().getHeader(HttpHeaders.CONTENT_TYPE);
            if (contentLength > 0 && contentType != null) {
                grizzlyResponse.setContentType(contentType);
            }

            return grizzlyResponse.getOutputStream();
        }
    }
    //
    private Application application;

    /**
     * Creates a new Grizzly container.
     *
     * @param application Jersey application to be deployed on Grizzly container.
     */
    GrizzlyHttpContainer(final Application application) {
        this.application = application;
    }

    // HttpRequestProcessor
    @Override
    public void service(final Request request, final Response response) {
        application.apply(toJaxrsRequest(request), new Writer(response));
        response.finish();
    }

    private URI getBaseUri(final Request request) {
        try {
            return new URI(request.getScheme(), null, request.getServerName(),
                    request.getServerPort(), getBasePath(request), null, null);
        } catch (final URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    private String getBasePath(final Request request) {
        final String contextPath = request.getContextPath();

        if (contextPath == null || contextPath.length() == 0) {
            return "/";
        } else if (contextPath.charAt(contextPath.length() - 1) != '/') {
            return contextPath + "/";
        } else {
            return contextPath;
        }
    }

    private javax.ws.rs.core.Request toJaxrsRequest(Request grizzlyRequest) {

        final URI baseUri = getBaseUri(grizzlyRequest);

        // TODO: this is terrible, there must be a way to obtain the original request URI!
        String originalURI = UriBuilder.fromPath(
                grizzlyRequest.getRequest().getRequestURIRef().getOriginalRequestURIBC().toString(Charsets.DEFAULT_CHARSET)).build().toString();

        String queryString = grizzlyRequest.getQueryString();
        if (queryString != null) {
            originalURI = originalURI + "?" + queryString;
        }

        final URI requestUri = baseUri.resolve(originalURI);

        final String method = grizzlyRequest.getMethod().getMethodString();

        RequestBuilder rb = Requests.from(baseUri, requestUri, method).entity(grizzlyRequest.getInputStream());

        for (String name : grizzlyRequest.getHeaderNames()) {
            for (String value : grizzlyRequest.getHeaders(name)) {
                rb.header(name, value);
            }
        }

        return rb.build();
    }
}
