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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.client.ClientException;
import javax.ws.rs.core.MultivaluedMap;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

import org.glassfish.jersey.client.internal.LocalizationMessages;
import org.glassfish.jersey.internal.util.PropertiesHelper;
import org.glassfish.jersey.message.internal.OutboundMessageContext;
import org.glassfish.jersey.message.internal.Statuses;
import org.glassfish.jersey.process.Inflector;

import com.google.common.base.Predicates;
import com.google.common.collect.Maps;

/**
 * Default client transport connector using {@link HttpURLConnection}.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class HttpUrlConnector extends RequestWriter implements Inflector<ClientRequest, ClientResponse> {

    private static InputStream getInputStream(HttpURLConnection uc) throws IOException {
        if (uc.getResponseCode() < 300) {
            return uc.getInputStream();
        } else {
            InputStream ein = uc.getErrorStream();
            return (ein != null) ? ein : new ByteArrayInputStream(new byte[0]);
        }
    }

    @Override
    public ClientResponse apply(ClientRequest request) {
        try {
            return _apply(request);
        } catch (IOException ex) {
            throw new ClientException(ex);
        }
    }

    private ClientResponse _apply(final ClientRequest requestContext) throws IOException {
        final HttpURLConnection uc;
        // TODO introduce & leverage optional connection factory to support customized connections
        uc = (HttpURLConnection) requestContext.getUri().toURL().openConnection();
        uc.setRequestMethod(requestContext.getMethod());

        final Map<String,Object> configurationProperties = requestContext.getConfiguration().getProperties();
        uc.setInstanceFollowRedirects(PropertiesHelper.getValue(configurationProperties,
                ClientProperties.FOLLOW_REDIRECTS, true));

        uc.setConnectTimeout(PropertiesHelper.getValue(configurationProperties,
                ClientProperties.CONNECT_TIMEOUT, 0));

        uc.setReadTimeout(PropertiesHelper.getValue(configurationProperties,
                ClientProperties.READ_TIMEOUT, 0));

        if (uc instanceof HttpsURLConnection) {
            Object o = configurationProperties.get(ClientProperties.HOSTNAME_VERIFIER);
            if (o instanceof HostnameVerifier) {
                ((HttpsURLConnection) uc).setHostnameVerifier((HostnameVerifier) o);
            }

            o = configurationProperties.get(ClientProperties.SSL_CONTEXT);
            if (o instanceof SSLContext) {
                ((HttpsURLConnection) uc).setSSLSocketFactory(((SSLContext) o).getSocketFactory());
            }
        }

        final Object entity = requestContext.getEntity();
        if (entity != null) {
            uc.setDoOutput(true);

            if(requestContext.getMethod().equalsIgnoreCase("GET")) {
                final Logger logger = Logger.getLogger(HttpUrlConnector.class.getName());
                if(logger.isLoggable(Level.INFO)) {
                    logger.log(Level.INFO, LocalizationMessages.HTTPURLCONNECTION_REPLACES_GET_WITH_ENTITY());
                }
            }

            writeRequestEntity(requestContext, new RequestEntityWriterListener() {
                @Override
                public void onRequestEntitySize(long size) {
                    if (size != -1 && size < Integer.MAX_VALUE) {
                        // HttpURLConnection uses the int type for content length

                        // this just does not work for several consecutive requests.
                        // another day wasted on HttpUrlConnection bug :/
                        // uc.setFixedLengthStreamingMode((int)size);
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
                public OutboundMessageContext.StreamProvider onGetStreamProvider() throws IOException {
                    return new OutboundMessageContext.StreamProvider() {
                        @Override
                        public OutputStream getOutputStream() throws IOException {
                            return uc.getOutputStream();
                        }

                        @Override
                        public void commit() throws IOException {
                            writeOutBoundHeaders(requestContext.getStringHeaders(), uc);
                        }
                    };
                }

            });
        } else {
            writeOutBoundHeaders(requestContext.getStringHeaders(), uc);
        }

        ClientResponse responseContext = new ClientResponse(
                Statuses.from(uc.getResponseCode()), requestContext);
        responseContext.setEntityStream(getInputStream(uc));
        responseContext.headers(Maps.<String, List<String>>filterKeys(uc.getHeaderFields(), Predicates.notNull()));

        return responseContext;
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
