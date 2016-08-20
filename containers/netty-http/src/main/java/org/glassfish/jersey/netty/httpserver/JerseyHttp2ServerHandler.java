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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;

import javax.ws.rs.core.SecurityContext;

import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.glassfish.jersey.internal.PropertiesDelegate;
import org.glassfish.jersey.netty.connector.internal.NettyInputStream;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.internal.ContainerUtils;

/**
 * Jersey Netty HTTP/2 handler.
 * <p>
 * Note that this implementation cannot be more experimental. Any contributions / feedback is welcomed.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
@ChannelHandler.Sharable
class JerseyHttp2ServerHandler extends ChannelDuplexHandler {

    private final URI baseUri;
    private final LinkedBlockingDeque<InputStream> isList = new LinkedBlockingDeque<>();
    private final NettyHttpContainer container;

    /**
     * Constructor.
     *
     * @param baseUri   base {@link URI} of the container (includes context path, if any).
     * @param container Netty container implementation.
     */
    JerseyHttp2ServerHandler(URI baseUri, NettyHttpContainer container) {
        this.baseUri = baseUri;
        this.container = container;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Http2HeadersFrame) {
            onHeadersRead(ctx, (Http2HeadersFrame) msg);
        } else if (msg instanceof Http2DataFrame) {
            onDataRead(ctx, (Http2DataFrame) msg);
        } else {
            super.channelRead(ctx, msg);
        }
    }

    /**
     * Process incoming data.
     */
    private void onDataRead(ChannelHandlerContext ctx, Http2DataFrame data) throws Exception {
        isList.add(new ByteBufInputStream(data.content()));
        if (data.isEndStream()) {
            isList.add(NettyInputStream.END_OF_INPUT);
        }
    }

    /**
     * Process incoming request (just a headers in this case, entity is processed separately).
     */
    private void onHeadersRead(ChannelHandlerContext ctx, Http2HeadersFrame headers) throws Exception {

        final ContainerRequest requestContext = createContainerRequest(ctx, headers);

        requestContext.setWriter(new NettyHttp2ResponseWriter(ctx, headers, container));

        // must be like this, since there is a blocking read from Jersey
        container.getExecutorService().execute(new Runnable() {
            @Override
            public void run() {
                container.getApplicationHandler().handle(requestContext);
            }
        });
    }

    /**
     * Create Jersey {@link ContainerRequest} based on Netty {@link HttpRequest}.
     *
     * @param ctx          Netty channel context.
     * @param http2Headers Netty Http/2 headers.
     * @return created Jersey Container Request.
     */
    private ContainerRequest createContainerRequest(ChannelHandlerContext ctx, Http2HeadersFrame http2Headers) {

        String path = http2Headers.headers().path().toString();

        String s = path.startsWith("/") ? path.substring(1) : path;
        URI requestUri = URI.create(baseUri + ContainerUtils.encodeUnsafeCharacters(s));

        ContainerRequest requestContext = new ContainerRequest(
                baseUri, requestUri, http2Headers.headers().method().toString(), getSecurityContext(),
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
        if (!http2Headers.isEndStream()) {

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
        for (CharSequence name : http2Headers.headers().names()) {
            requestContext.headers(name.toString(), mapToString(http2Headers.headers().getAll(name)));
        }

        return requestContext;
    }

    private List<String> mapToString(List<CharSequence> list) {
        ArrayList<String> result = new ArrayList<>(list.size());

        for (CharSequence sequence : list) {
            result.add(sequence.toString());
        }

        return result;
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
}
