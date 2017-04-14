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

package org.glassfish.jersey.netty.connector.internal;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import javax.inject.Provider;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.stream.ChunkedInput;

/**
 * Netty {@link ChunkedInput} implementation which also serves as an output
 * stream to Jersey {@link javax.ws.rs.container.ContainerResponseContext}.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class JerseyChunkedInput extends OutputStream implements ChunkedInput<ByteBuf>, ChannelFutureListener {

    private static final ByteBuffer VOID = ByteBuffer.allocate(0);
    private static final int CAPACITY = 8;
    // TODO this needs to be configurable, see JERSEY-3228
    private static final int WRITE_TIMEOUT = 10000;
    private static final int READ_TIMEOUT = 10000;

    private final LinkedBlockingDeque<ByteBuffer> queue = new LinkedBlockingDeque<>(CAPACITY);
    private final Channel ctx;
    private final ChannelFuture future;

    private volatile boolean open = true;
    private volatile long offset = 0;

    public JerseyChunkedInput(Channel ctx) {
        this.ctx = ctx;
        this.future = ctx.closeFuture();
        this.future.addListener(this);
    }

    @Override
    public boolean isEndOfInput() throws Exception {
        if (!open) {
            return true;
        }

        ByteBuffer peek = queue.peek();

        if ((peek != null && peek == VOID)) {
            queue.remove(); // VOID from the top.
            open = false;
            removeCloseListener();
            return true;
        }

        return false;
    }

    @Override
    public void operationComplete(ChannelFuture f) throws Exception {
        // forcibly closed connection.
        open = false;
        queue.clear();

        close();
        removeCloseListener();
    }

    private void removeCloseListener() {
        if (future != null) {
            future.removeListener(this);
        }
    }

    @Override
    @Deprecated
    public ByteBuf readChunk(ChannelHandlerContext ctx) throws Exception {
        return readChunk(ctx.alloc());
    }

    @Override
    public ByteBuf readChunk(ByteBufAllocator allocator) throws Exception {

        if (!open) {
            return null;
        }

        ByteBuffer top = queue.poll(READ_TIMEOUT, TimeUnit.MILLISECONDS);

        if (top == null) {
            // returning empty buffer instead of null causes flush (which is needed for BroadcasterTest and others..).
            return Unpooled.EMPTY_BUFFER;
        }

        if (top == VOID) {
            open = false;
            return null;
        }

        int topRemaining = top.remaining();
        ByteBuf buffer = allocator.buffer(topRemaining);

        buffer.setBytes(0, top);
        buffer.setIndex(0, topRemaining);

        if (top.remaining() > 0) {
            queue.addFirst(top);
        }

        offset += topRemaining;

        return buffer;
    }

    @Override
    public long length() {
        return -1;
    }

    @Override
    public long progress() {
        return offset;
    }

    @Override
    public void close() throws IOException {

        if (queue.size() == CAPACITY) {
            boolean offer = false;

            try {
                offer = queue.offer(VOID, WRITE_TIMEOUT, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                // ignore.
            }

            if (!offer) {
                queue.removeLast();
                queue.add(VOID);
            }
        } else {
            queue.add(VOID);
        }

        ctx.flush();
    }

    @Override
    public void write(final int b) throws IOException {

        write(new Provider<ByteBuffer>() {
            @Override
            public ByteBuffer get() {
                return ByteBuffer.wrap(new byte[]{(byte) b});
            }
        });
    }

    @Override
    public void write(final byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {

        final byte[] bytes = new byte[len];
        System.arraycopy(b, off, bytes, 0, len);

        write(new Provider<ByteBuffer>() {
            @Override
            public ByteBuffer get() {
                return ByteBuffer.wrap(bytes);
            }
        });
    }

    @Override
    public void flush() throws IOException {
        ctx.flush();
    }

    private void write(Provider<ByteBuffer> bufferSupplier) throws IOException {

        checkClosed();

        try {
            boolean queued = queue.offer(bufferSupplier.get(), WRITE_TIMEOUT, TimeUnit.MILLISECONDS);
            if (!queued) {
                throw new IOException("Buffer overflow.");
            }

        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }

    private void checkClosed() throws IOException {
        if (!open) {
            throw new IOException("Stream already closed.");
        }
    }
}
