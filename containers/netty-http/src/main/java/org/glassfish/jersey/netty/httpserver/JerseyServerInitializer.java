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

import java.net.URI;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerUpgradeHandler;
import io.netty.handler.codec.http2.Http2Codec;
import io.netty.handler.codec.http2.Http2CodecUtil;
import io.netty.handler.codec.http2.Http2ServerUpgradeCodec;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.AsciiString;

/**
 * Jersey {@link ChannelInitializer}.
 * <p>
 * Adds {@link HttpServerCodec}, {@link ChunkedWriteHandler} and {@link JerseyServerHandler} to the channels pipeline.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
class JerseyServerInitializer extends ChannelInitializer<SocketChannel> {

    private final URI baseUri;
    private final SslContext sslCtx;
    private final NettyHttpContainer container;
    private final boolean http2;

    /**
     * Constructor.
     *
     * @param baseUri   base {@link URI} of the container (includes context path, if any).
     * @param sslCtx    SSL context.
     * @param container Netty container implementation.
     */
    public JerseyServerInitializer(URI baseUri, SslContext sslCtx, NettyHttpContainer container) {
        this(baseUri, sslCtx, container, false);
    }

    /**
     * Constructor.
     *
     * @param baseUri   base {@link URI} of the container (includes context path, if any).
     * @param sslCtx    SSL context.
     * @param container Netty container implementation.
     * @param http2     Http/2 protocol support.
     */
    public JerseyServerInitializer(URI baseUri, SslContext sslCtx, NettyHttpContainer container, boolean http2) {
        this.baseUri = baseUri;
        this.sslCtx = sslCtx;
        this.container = container;
        this.http2 = http2;
    }

    @Override
    public void initChannel(SocketChannel ch) {
        if (http2) {

            if (sslCtx != null) {
                configureSsl(ch);
            } else {
                configureClearText(ch);
            }

        } else {
            ChannelPipeline p = ch.pipeline();
            if (sslCtx != null) {
                p.addLast(sslCtx.newHandler(ch.alloc()));
            }
            p.addLast(new HttpServerCodec());
            p.addLast(new ChunkedWriteHandler());
            p.addLast(new JerseyServerHandler(baseUri, container));
        }
    }

    /**
     * Configure the pipeline for TLS NPN negotiation to HTTP/2.
     */
    private void configureSsl(SocketChannel ch) {
        ch.pipeline().addLast(sslCtx.newHandler(ch.alloc()), new HttpVersionChooser(baseUri, container));
    }

    /**
     * Configure the pipeline for a cleartext upgrade from HTTP to HTTP/2.
     */
    private void configureClearText(SocketChannel ch) {
        final ChannelPipeline p = ch.pipeline();
        final HttpServerCodec sourceCodec = new HttpServerCodec();

        p.addLast(sourceCodec);
        p.addLast(new HttpServerUpgradeHandler(sourceCodec, new HttpServerUpgradeHandler.UpgradeCodecFactory() {
            @Override
            public HttpServerUpgradeHandler.UpgradeCodec newUpgradeCodec(CharSequence protocol) {
                if (AsciiString.contentEquals(Http2CodecUtil.HTTP_UPGRADE_PROTOCOL_NAME, protocol)) {
                    return new Http2ServerUpgradeCodec(new Http2Codec(true, new JerseyHttp2ServerHandler(baseUri, container)));
                } else {
                    return null;
                }
            }
        }));
        p.addLast(new SimpleChannelInboundHandler<HttpMessage>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, HttpMessage msg) throws Exception {
                // If this handler is hit then no upgrade has been attempted and the client is just talking HTTP.
                // "Directly talking: " + msg.protocolVersion() + " (no upgrade was attempted)");

                ChannelPipeline pipeline = ctx.pipeline();
                ChannelHandlerContext thisCtx = pipeline.context(this);
                pipeline.addAfter(thisCtx.name(), null, new JerseyServerHandler(baseUri, container));
                pipeline.replace(this, null, new ChunkedWriteHandler());
                ctx.fireChannelRead(msg);
            }
        });
    }
}
