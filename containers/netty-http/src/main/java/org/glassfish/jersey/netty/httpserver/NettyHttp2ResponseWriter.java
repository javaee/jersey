/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.netty.httpserver;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.glassfish.jersey.server.ContainerException;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.spi.ContainerResponseWriter;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2HeadersFrame;

/**
 * Netty implementation of {@link ContainerResponseWriter}.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
class NettyHttp2ResponseWriter implements ContainerResponseWriter {

    private final ChannelHandlerContext ctx;
    private final Http2HeadersFrame headersFrame;
    private final NettyHttpContainer container;

    private volatile ScheduledFuture<?> suspendTimeoutFuture;
    private volatile Runnable suspendTimeoutHandler;

    NettyHttp2ResponseWriter(ChannelHandlerContext ctx, Http2HeadersFrame headersFrame, NettyHttpContainer container) {
        this.ctx = ctx;
        this.headersFrame = headersFrame;
        this.container = container;
    }

    @Override
    public OutputStream writeResponseStatusAndHeaders(long contentLength, ContainerResponse responseContext)
            throws ContainerException {

        String reasonPhrase = responseContext.getStatusInfo().getReasonPhrase();
        int statusCode = responseContext.getStatus();

        HttpResponseStatus status = reasonPhrase == null
                ? HttpResponseStatus.valueOf(statusCode)
                : new HttpResponseStatus(statusCode, reasonPhrase);

        DefaultHttp2Headers response = new DefaultHttp2Headers();
        response.status(Integer.toString(responseContext.getStatus()));

        for (final Map.Entry<String, List<String>> e : responseContext.getStringHeaders().entrySet()) {
            response.add(e.getKey().toLowerCase(), e.getValue());
        }

        response.set(HttpHeaderNames.CONTENT_LENGTH, Long.toString(contentLength));

        ctx.writeAndFlush(new DefaultHttp2HeadersFrame(response));

        if (!headersFrame.headers().method().equals(HttpMethod.HEAD.asciiName())
            && (contentLength > 0 || contentLength == -1)) {

            return new OutputStream() {
                @Override
                public void write(int b) throws IOException {
                    write(new byte[]{(byte) b});
                }

                @Override
                public void write(byte[] b) throws IOException {
                    write(b, 0, b.length);
                }

                @Override
                public void write(byte[] b, int off, int len) throws IOException {

                    ByteBuf buffer = ctx.alloc().buffer(len);
                    buffer.writeBytes(b, off, len);

                    ctx.writeAndFlush(new DefaultHttp2DataFrame(buffer, false));
                }

                @Override
                public void flush() throws IOException {
                    ctx.flush();
                }

                @Override
                public void close() throws IOException {
                    ctx.write(new DefaultHttp2DataFrame(true)).addListener(NettyResponseWriter.FLUSH_FUTURE);
                }
            };

        } else {
            ctx.writeAndFlush(new DefaultHttp2DataFrame(true));
            return null;
        }
    }

    @Override
    public boolean suspend(long timeOut, TimeUnit timeUnit, final ContainerResponseWriter.TimeoutHandler
            timeoutHandler) {

        suspendTimeoutHandler = new Runnable() {
            @Override
            public void run() {
                timeoutHandler.onTimeout(NettyHttp2ResponseWriter.this);
            }
        };

        if (timeOut <= 0) {
            return true;
        }

        suspendTimeoutFuture =
                container.getScheduledExecutorService().schedule(suspendTimeoutHandler, timeOut, timeUnit);

        return true;
    }

    @Override
    public void setSuspendTimeout(long timeOut, TimeUnit timeUnit) throws IllegalStateException {

        // suspend(0, .., ..) was called, so suspendTimeoutFuture is null.
        if (suspendTimeoutFuture != null) {
            suspendTimeoutFuture.cancel(true);
        }

        if (timeOut <= 0) {
            return;
        }

        suspendTimeoutFuture =
                container.getScheduledExecutorService().schedule(suspendTimeoutHandler, timeOut, timeUnit);
    }

    @Override
    public void commit() {
        ctx.flush();
    }

    @Override
    public void failure(Throwable error) {
        ctx.writeAndFlush(new DefaultHttp2Headers().status(HttpResponseStatus.INTERNAL_SERVER_ERROR.codeAsText()))
           .addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public boolean enableResponseBuffering() {
        return true;
    }
}
