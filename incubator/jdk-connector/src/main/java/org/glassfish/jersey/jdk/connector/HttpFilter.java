/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.jdk.connector;

import java.net.URI;
import java.nio.ByteBuffer;

import javax.ws.rs.core.HttpHeaders;

/**
 * @author Petr Janouch (petr.janouch at oracle.com)
 */
class HttpFilter extends Filter<HttpRequest, HttpResponse, ByteBuffer, ByteBuffer> {

    private final HttpParser httpParser;

    /**
     * Constructor.
     *
     * @param downstreamFilter downstream filter. Accessible directly as {@link #downstreamFilter} protected field.
     */
    HttpFilter(Filter<ByteBuffer, ByteBuffer, ?, ?> downstreamFilter, int maxHeaderSize, int maxBufferSize) {
        super(downstreamFilter);
        this.httpParser = new HttpParser(maxHeaderSize, maxBufferSize);
    }

    @Override
    void write(final HttpRequest httpRequest, final CompletionHandler<HttpRequest> completionHandler) {
        addTransportHeaders(httpRequest);

        ByteBuffer header = HttpRequestEncoder.encodeHeader(httpRequest);
        prepareForReply(httpRequest, completionHandler);
        downstreamFilter.write(header, new CompletionHandler<ByteBuffer>() {
            @Override
            public void failed(Throwable throwable) {
                completionHandler.failed(throwable);
            }

            @Override
            public void completed(ByteBuffer result) {
                writeBody(httpRequest, completionHandler);
            }
        });
    }

    private void writeBody(final HttpRequest httpRequest, final CompletionHandler<HttpRequest> completionHandler) {
        switch (httpRequest.getBodyMode()) {

            case CHUNKED: {
                ChunkedBodyOutputStream bodyStream = (ChunkedBodyOutputStream) httpRequest.getBodyStream();
                bodyStream.open(downstreamFilter);
                break;
            }

            case BUFFERED: {
                ByteBuffer body = httpRequest.getBufferedBody();
                downstreamFilter.write(body, new CompletionHandler<ByteBuffer>() {
                    @Override
                    public void failed(Throwable throwable) {
                        completionHandler.failed(throwable);
                    }
                });

                break;
            }
        }
    }

    private void prepareForReply(HttpRequest httpRequest, CompletionHandler<HttpRequest> completionHandler) {
        completionHandler.completed(httpRequest);

        boolean expectResponseBody = true;

        if (Constants.HEAD.equals(httpRequest.getMethod()) || Constants.CONNECT.equals(httpRequest.getMethod())) {
            expectResponseBody = false;
        }

        httpParser.reset(expectResponseBody);
    }

    @Override
    boolean processRead(ByteBuffer data) {
        boolean headerParsed = httpParser.isHeaderParsed();
        try {
            httpParser.parse(data);
        } catch (ParseException e) {
            onError(e);
        }

        if (!headerParsed && httpParser.isHeaderParsed()) {
            HttpResponse httpResponse = httpParser.getHttpResponse();
            upstreamFilter.onRead(httpResponse);
        }

        return false;
    }

    private void addTransportHeaders(HttpRequest httpRequest) {
        if (httpRequest.getBodyMode() == HttpRequest.BodyMode.BUFFERED) {
            httpRequest.addHeaderIfNotPresent(Constants.CONTENT_LENGTH, Integer.toString(httpRequest.getBodySize()));
        }

        URI uri = httpRequest.getUri();
        int port = Utils.getPort(uri);
        httpRequest.addHeaderIfNotPresent(Constants.HOST, uri.getHost() + ":" + port);

        if (httpRequest.getBodyMode() == HttpRequest.BodyMode.CHUNKED) {
            httpRequest.addHeaderIfNotPresent(Constants.TRANSFER_ENCODING_HEADER, Constants.TRANSFER_ENCODING_CHUNKED);
        }

        if (httpRequest.getBodyMode() == HttpRequest.BodyMode.NONE) {
            httpRequest.addHeaderIfNotPresent(HttpHeaders.CONTENT_LENGTH, Integer.toString(0));
        }
    }
}
