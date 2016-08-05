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

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Application;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.glassfish.jersey.Beta;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.spi.ContainerProvider;

/**
 * Netty implementation of {@link ContainerProvider}.
 * <p>
 * There is also a few "factory" methods for creating Netty server.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 * @since 2.24
 */
@Beta
public class NettyHttpContainerProvider implements ContainerProvider {

    @Override
    public <T> T createContainer(Class<T> type, Application application) throws ProcessingException {
        if (NettyHttpContainer.class == type) {
            return type.cast(new NettyHttpContainer(application));
        }

        return null;
    }

    /**
     * Create and start Netty server.
     *
     * @param baseUri       base uri.
     * @param configuration Jersey configuration.
     * @param sslContext    Netty SSL context (can be null).
     * @param block         when {@code true}, this method will block until the server is stopped. When {@code false}, the
     *                      execution will
     *                      end immediately after the server is started.
     * @return Netty channel instance.
     * @throws ProcessingException when there is an issue with creating new container.
     */
    public static Channel createServer(final URI baseUri, final ResourceConfig configuration, SslContext sslContext,
                                       final boolean block)
            throws ProcessingException {

        // Configure the server.
        final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        final EventLoopGroup workerGroup = new NioEventLoopGroup();
        final NettyHttpContainer container = new NettyHttpContainer(configuration);

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.option(ChannelOption.SO_BACKLOG, 1024);
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .childHandler(new JerseyServerInitializer(baseUri, sslContext, container));

            int port = getPort(baseUri);

            Channel ch = b.bind(port).sync().channel();

            ch.closeFuture().addListener(new GenericFutureListener<Future<? super Void>>() {
                @Override
                public void operationComplete(Future<? super Void> future) throws Exception {
                    container.getApplicationHandler().onShutdown(container);

                    bossGroup.shutdownGracefully();
                    workerGroup.shutdownGracefully();
                }
            });

            if (block) {
                ch.closeFuture().sync();
                return ch;
            } else {
                return ch;
            }
        } catch (InterruptedException e) {
            throw new ProcessingException(e);
        }
    }

    /**
     * Create and start Netty server.
     *
     * @param baseUri       base uri.
     * @param configuration Jersey configuration.
     * @param block         when {@code true}, this method will block until the server is stopped. When {@code false}, the
     *                      execution will
     *                      end immediately after the server is started.
     * @return Netty channel instance.
     * @throws ProcessingException when there is an issue with creating new container.
     */
    public static Channel createServer(final URI baseUri, final ResourceConfig configuration, final boolean block)
            throws ProcessingException {

        return createServer(baseUri, configuration, null, block);
    }

    /**
     * Create and start Netty HTTP/2 server.
     * <p>
     * The server is capable of connection upgrade to HTTP/2. HTTP/1.x request will be server as they were used to.
     * <p>
     * Note that this implementation cannot be more experimental. Any contributions / feedback is welcomed.
     *
     * @param baseUri       base uri.
     * @param configuration Jersey configuration.
     * @param sslContext    Netty {@link SslContext}.
     * @return Netty channel instance.
     * @throws ProcessingException when there is an issue with creating new container.
     */
    public static Channel createHttp2Server(final URI baseUri, final ResourceConfig configuration, SslContext sslContext) throws
            ProcessingException {

        final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        final EventLoopGroup workerGroup = new NioEventLoopGroup();
        final NettyHttpContainer container = new NettyHttpContainer(configuration);

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.option(ChannelOption.SO_BACKLOG, 1024);
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .childHandler(new JerseyServerInitializer(baseUri, sslContext, container, true));

            int port = getPort(baseUri);

            Channel ch = b.bind(port).sync().channel();

            ch.closeFuture().addListener(new GenericFutureListener<Future<? super Void>>() {
                @Override
                public void operationComplete(Future<? super Void> future) throws Exception {
                    container.getApplicationHandler().onShutdown(container);

                    bossGroup.shutdownGracefully();
                    workerGroup.shutdownGracefully();
                }
            });

            return ch;

        } catch (InterruptedException e) {
            throw new ProcessingException(e);
        }
    }

    private static int getPort(URI uri) {
        if (uri.getPort() == -1) {
            if ("http".equalsIgnoreCase(uri.getScheme())) {
                return 80;
            } else if ("https".equalsIgnoreCase(uri.getScheme())) {
                return 443;
            }

            throw new IllegalArgumentException("URI scheme must be 'http' or 'https'.");
        }

        return uri.getPort();
    }
}
