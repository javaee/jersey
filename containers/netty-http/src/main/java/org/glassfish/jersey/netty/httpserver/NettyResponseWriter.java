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

import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.jersey.netty.connector.internal.JerseyChunkedInput;
import org.glassfish.jersey.server.ContainerException;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.spi.ContainerResponseWriter;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpChunkedInput;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.LastHttpContent;

/**
 * Netty implementation of {@link ContainerResponseWriter}.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
class NettyResponseWriter implements ContainerResponseWriter {

    private static final Logger LOGGER = Logger.getLogger(NettyResponseWriter.class.getName());

    static final ChannelFutureListener FLUSH_FUTURE = new ChannelFutureListener() {
        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            future.channel().flush();
        }
    };

    private final ChannelHandlerContext ctx;
    private final HttpRequest req;
    private final NettyHttpContainer container;

    private volatile ScheduledFuture<?> suspendTimeoutFuture;
    private volatile Runnable suspendTimeoutHandler;

    private boolean responseWritten = false;

    NettyResponseWriter(ChannelHandlerContext ctx, HttpRequest req, NettyHttpContainer container) {
        this.ctx = ctx;
        this.req = req;
        this.container = container;
    }

    @Override
    public synchronized OutputStream writeResponseStatusAndHeaders(long contentLength, ContainerResponse responseContext)
            throws ContainerException {

        if (responseWritten) {
            LOGGER.log(Level.FINE, "Response already written.");
            return null;
        }

        responseWritten = true;

        String reasonPhrase = responseContext.getStatusInfo().getReasonPhrase();
        int statusCode = responseContext.getStatus();

        HttpResponseStatus status = reasonPhrase == null
                ? HttpResponseStatus.valueOf(statusCode)
                : new HttpResponseStatus(statusCode, reasonPhrase);

        DefaultHttpResponse response;
        if (contentLength == 0) {
            response = new DefaultFullHttpResponse(req.protocolVersion(), status);
        } else {
            response = new DefaultHttpResponse(req.protocolVersion(), status);
        }

        for (final Map.Entry<String, List<String>> e : responseContext.getStringHeaders().entrySet()) {
            response.headers().add(e.getKey(), e.getValue());
        }

        if (contentLength == -1) {
            HttpUtil.setTransferEncodingChunked(response, true);
        } else {
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, contentLength);
        }

        if (HttpUtil.isKeepAlive(req)) {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }

        ctx.writeAndFlush(response);

        if (req.method() != HttpMethod.HEAD && (contentLength > 0 || contentLength == -1)) {

            JerseyChunkedInput jerseyChunkedInput = new JerseyChunkedInput(ctx.channel());

            if (HttpUtil.isTransferEncodingChunked(response)) {
                ctx.write(new HttpChunkedInput(jerseyChunkedInput)).addListener(FLUSH_FUTURE);
            } else {
                ctx.write(new HttpChunkedInput(jerseyChunkedInput)).addListener(FLUSH_FUTURE);
            }
            return jerseyChunkedInput;

        } else {
            ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
            return null;
        }
    }

    @Override
    public boolean suspend(long timeOut, TimeUnit timeUnit, final ContainerResponseWriter.TimeoutHandler
            timeoutHandler) {

        suspendTimeoutHandler = new Runnable() {
            @Override
            public void run() {
                timeoutHandler.onTimeout(NettyResponseWriter.this);
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
        ctx.writeAndFlush(new DefaultFullHttpResponse(req.protocolVersion(), HttpResponseStatus.INTERNAL_SERVER_ERROR))
           .addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public boolean enableResponseBuffering() {
        return true;
    }
}
