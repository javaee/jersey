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
import java.io.InputStream;
import java.net.URI;
import java.security.Principal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;

import javax.ws.rs.core.SecurityContext;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.glassfish.jersey.internal.PropertiesDelegate;
import org.glassfish.jersey.netty.connector.internal.NettyInputStream;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.internal.ContainerUtils;

/**
 * {@link io.netty.channel.ChannelInboundHandler} which servers as a bridge
 * between Netty and Jersey.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
class JerseyServerHandler extends ChannelInboundHandlerAdapter {

    private final URI baseUri;
    private final LinkedBlockingDeque<InputStream> isList = new LinkedBlockingDeque<>();
    private final NettyHttpContainer container;

    /**
     * Constructor.
     *
     * @param baseUri   base {@link URI} of the container (includes context path, if any).
     * @param container Netty container implementation.
     */
    public JerseyServerHandler(URI baseUri, NettyHttpContainer container) {
        this.baseUri = baseUri;
        this.container = container;
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) {

        if (msg instanceof HttpRequest) {
            final HttpRequest req = (HttpRequest) msg;

            if (HttpUtil.is100ContinueExpected(req)) {
                ctx.write(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE));
            }

            isList.clear(); // clearing the content - possible leftover from previous request processing.
            final ContainerRequest requestContext = createContainerRequest(ctx, req);

            requestContext.setWriter(new NettyResponseWriter(ctx, req, container));

            // must be like this, since there is a blocking read from Jersey
            container.getExecutorService().execute(new Runnable() {
                @Override
                public void run() {
                    container.getApplicationHandler().handle(requestContext);
                }
            });
        }

        if (msg instanceof HttpContent) {
            HttpContent httpContent = (HttpContent) msg;

            ByteBuf content = httpContent.content();

            if (content.isReadable()) {
                isList.add(new ByteBufInputStream(content));
            }

            if (msg instanceof LastHttpContent) {
                isList.add(NettyInputStream.END_OF_INPUT);
            }
        }
    }

    /**
     * Create Jersey {@link ContainerRequest} based on Netty {@link HttpRequest}.
     *
     * @param ctx Netty channel context.
     * @param req Netty Http request.
     * @return created Jersey Container Request.
     */
    private ContainerRequest createContainerRequest(ChannelHandlerContext ctx, HttpRequest req) {

        String s = req.uri().startsWith("/") ? req.uri().substring(1) : req.uri();
        URI requestUri = URI.create(baseUri + ContainerUtils.encodeUnsafeCharacters(s));

        ContainerRequest requestContext = new ContainerRequest(
                baseUri, requestUri, req.method().name(), getSecurityContext(),
                new PropertiesDelegate() {

                    private final Map<String, Object> properties = new HashMap<>();

                    @Override
                    public Object getProperty(String name) {
                        return properties.get(name);
                    }

                    @Override
                    public Collection<String> getPropertyNames() {
                        return properties.keySet();
                    }

                    @Override
                    public void setProperty(String name, Object object) {
                        properties.put(name, object);
                    }

                    @Override
                    public void removeProperty(String name) {
                        properties.remove(name);
                    }
                });

        // request entity handling.
        if ((req.headers().contains(HttpHeaderNames.CONTENT_LENGTH) && HttpUtil.getContentLength(req) > 0)
                || HttpUtil.isTransferEncodingChunked(req)) {

            ctx.channel().closeFuture().addListener(new GenericFutureListener<Future<? super Void>>() {
                @Override
                public void operationComplete(Future<? super Void> future) throws Exception {
                    isList.add(NettyInputStream.END_OF_INPUT_ERROR);
                }
            });

            requestContext.setEntityStream(new NettyInputStream(isList));
        } else {
            requestContext.setEntityStream(new InputStream() {
                @Override
                public int read() throws IOException {
                    return -1;
                }
            });
        }

        // copying headers from netty request to jersey container request context.
        for (String name : req.headers().names()) {
            requestContext.headers(name, req.headers().getAll(name));
        }

        return requestContext;
    }

    private SecurityContext getSecurityContext() {
        return new SecurityContext() {

            @Override
            public boolean isUserInRole(final String role) {
                return false;
            }

            @Override
            public boolean isSecure() {
                return false;
            }

            @Override
            public Principal getUserPrincipal() {
                return null;
            }

            @Override
            public String getAuthenticationScheme() {
                return null;
            }
        };
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
    }

}
