/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.netty.connector;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingDeque;

import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientRequest;
import org.glassfish.jersey.client.ClientResponse;
import org.glassfish.jersey.client.spi.AsyncConnectorCallback;
import org.glassfish.jersey.netty.connector.internal.NettyInputStream;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

/**
 * Jersey implementation of Netty channel handler.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
class JerseyClientHandler extends SimpleChannelInboundHandler<HttpObject> {

    private final NettyConnector connector;
    private final LinkedBlockingDeque<InputStream> isList = new LinkedBlockingDeque<>();

    private final AsyncConnectorCallback asyncConnectorCallback;
    private final ClientRequest jerseyRequest;
    private final CompletableFuture future;

    JerseyClientHandler(NettyConnector nettyConnector, ClientRequest request,
                        AsyncConnectorCallback callback, CompletableFuture future) {
        this.connector = nettyConnector;
        this.asyncConnectorCallback = callback;
        this.jerseyRequest = request;
        this.future = future;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, HttpObject msg) {
        if (msg instanceof HttpResponse) {
            final HttpResponse response = (HttpResponse) msg;

            final ClientResponse jerseyResponse = new ClientResponse(new Response.StatusType() {
                @Override
                public int getStatusCode() {
                    return response.status().code();
                }

                @Override
                public Response.Status.Family getFamily() {
                    return Response.Status.Family.familyOf(response.status().code());
                }

                @Override
                public String getReasonPhrase() {
                    return response.status().reasonPhrase();
                }
            }, jerseyRequest);

            for (Map.Entry<String, String> entry : response.headers().entries()) {
                jerseyResponse.getHeaders().add(entry.getKey(), entry.getValue());
            }

            // request entity handling.
            if ((response.headers().contains(HttpHeaderNames.CONTENT_LENGTH) && HttpUtil.getContentLength(response) > 0)
                    || HttpUtil.isTransferEncodingChunked(response)) {

                ctx.channel().closeFuture().addListener(new GenericFutureListener<Future<? super Void>>() {
                    @Override
                    public void operationComplete(Future<? super Void> future) throws Exception {
                        isList.add(NettyInputStream.END_OF_INPUT_ERROR);
                    }
                });

                jerseyResponse.setEntityStream(new NettyInputStream(isList));
            } else {
                jerseyResponse.setEntityStream(new InputStream() {
                    @Override
                    public int read() throws IOException {
                        return -1;
                    }
                });
            }

            if (asyncConnectorCallback != null) {
                connector.executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        asyncConnectorCallback.response(jerseyResponse);
                        future.complete(jerseyResponse);
                    }
                });
            }

        }
        if (msg instanceof HttpContent) {

            HttpContent httpContent = (HttpContent) msg;

            ByteBuf content = httpContent.content();

            if (content.isReadable()) {
                // copy bytes - when netty reads last chunk, it automatically closes the channel, which invalidates all
                // relates ByteBuffs.
                byte[] bytes = new byte[content.readableBytes()];
                content.getBytes(content.readerIndex(), bytes);
                isList.add(new ByteArrayInputStream(bytes));
            }

            if (msg instanceof LastHttpContent) {
                isList.add(NettyInputStream.END_OF_INPUT);
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, final Throwable cause) {
        if (asyncConnectorCallback != null) {
            connector.executorService.execute(new Runnable() {
                @Override
                public void run() {
                    asyncConnectorCallback.failure(cause);
                }
            });
        }
        future.completeExceptionally(cause);
        isList.add(NettyInputStream.END_OF_INPUT_ERROR);
    }
}
