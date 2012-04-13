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
package org.glassfish.jersey.client;

import org.glassfish.jersey.internal.util.CommittingOutputStream;
import com.google.common.base.Predicates;
import com.google.common.collect.Maps;
import org.glassfish.jersey.message.internal.Responses;
import org.glassfish.jersey.process.Inflector;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.List;

/**
 * Connector based on {@link HttpURLConnection}.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
class HttpUrlConnector extends RequestWriter implements Inflector<Request, Response> {

    private static InputStream getInputStream(HttpURLConnection uc) throws IOException {
        if (uc.getResponseCode() < 300) {
            return uc.getInputStream();
        } else {
            InputStream ein = uc.getErrorStream();
            return (ein != null) ? ein : new ByteArrayInputStream(new byte[0]);
        }
    }

    @Override
    public Response apply(Request request) {
        try {
            return _apply(request);
        } catch (IOException ioex) {
            throw new RuntimeException(ioex);
        }
    }

    public Response _apply(final Request request) throws IOException {
        final HttpURLConnection uc;
        // TODO introduce & leverage optional connection factory to support customized
        // connections
        uc = (HttpURLConnection) request.getUri().toURL().openConnection();
        uc.setRequestMethod(request.getMethod());
        // TODO process properties

        if (uc instanceof HttpsURLConnection) {
            if (request.getProperties().containsKey(ClientProperties.HOSTNAME_VERIFIER)) {
                final Object o = request.getProperties().get(ClientProperties.HOSTNAME_VERIFIER);
                if (o != null && (o instanceof HostnameVerifier)) {
                    ((HttpsURLConnection) uc).setHostnameVerifier((HostnameVerifier) o);
                }
            }

            if (request.getProperties().containsKey(ClientProperties.SSL_CONTEXT)) {
                final Object o = request.getProperties().get(ClientProperties.SSL_CONTEXT);
                if (o != null && (o instanceof SSLContext)) {
                    ((HttpsURLConnection) uc).setSSLSocketFactory(((SSLContext) o).getSocketFactory());
                }
            }
        }

        // TODO write entity
        final Object entity = request.getEntity();
        if (entity != null) {
            uc.setDoOutput(true);

            writeRequestEntity(request, new RequestEntityWriterListener() {
                @Override
                public void onRequestEntitySize(long size) {
                    if (size != -1 && size < Integer.MAX_VALUE) {
                        // HttpURLConnection uses the int type for content length
                        uc.setFixedLengthStreamingMode((int)size);
                    } else {
                        // TODO (copied from Jersey 1.x) it appears HttpURLConnection has some bugs in
                        // chunked encoding
                        // uc.setChunkedStreamingMode(0);

                        // TODO deal with chunked encoding
//                        Integer chunkedEncodingSize = (Integer)request.getProperties().get(
//                                ClientConfig.PROPERTY_CHUNKED_ENCODING_SIZE);
//                        if (chunkedEncodingSize != null) {
//                            uc.setChunkedStreamingMode(chunkedEncodingSize);
//                        }
                    }
                }

                @Override
                public OutputStream onGetOutputStream() throws IOException {
                    return new CommittingOutputStream() {
                        @Override
                        protected OutputStream getOutputStream() throws IOException {
                            return uc.getOutputStream();
                        }

                        @Override
                        public void commit() throws IOException {
                            writeOutBoundHeaders(request.getHeaders().asMap(), uc);
                        }
                    };
                }

            });
        } else {
            writeOutBoundHeaders(request.getHeaders().asMap(), uc);
        }

        Response.ResponseBuilder rb =
                Responses.from(uc.getResponseCode(), request, getInputStream(uc));
        Responses.fillHeaders(rb, Maps.filterKeys(uc.getHeaderFields(), Predicates.notNull()));

        return rb.build();
    }

    private void writeOutBoundHeaders(MultivaluedMap<String, String> headers, HttpURLConnection uc) {
        for (String key : headers.keySet()) {
            List<String> headerValues = headers.get(key);
            if (headerValues.size() == 1) {
                uc.setRequestProperty(key, headerValues.get(0));
            } else {
                StringBuilder b = new StringBuilder();
                boolean add = false;
                for (Object value : headerValues) {
                    if (add) {
                        b.append(',');
                    }
                    add = true;
                    b.append(value);
                }
                uc.setRequestProperty(key, b.toString());
            }
        }
    }
}
