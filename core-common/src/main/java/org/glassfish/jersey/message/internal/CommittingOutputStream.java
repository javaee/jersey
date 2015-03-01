/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.message.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.jersey.internal.LocalizationMessages;

import jersey.repackaged.com.google.common.base.Preconditions;

/**
 * A committing output stream with optional serialized entity buffering functionality
 * which allows measuring of the entity size.
 * <p>
 * When buffering functionality is enabled the output stream buffers
 * the written bytes into an internal buffer of a configurable size. After the last
 * written byte the {@link #commit()} method is expected to be called to notify
 * a {@link org.glassfish.jersey.message.internal.OutboundMessageContext.StreamProvider#getOutputStream(int) callback}
 * with an actual measured entity size. If the entity is too large to
 * fit into the internal buffer and the buffer exceeds before the {@link #commit()}
 * is called then the stream is automatically committed and the callback is called
 * with parameter {@code size} value of {@code -1}.
 * </p>
 * <p>
 * Callback method also returns the output stream in which the output will be written. The committing output stream
 * must be initialized with the callback using
 * {@link #setStreamProvider(org.glassfish.jersey.message.internal.OutboundMessageContext.StreamProvider)}
 * before first byte is written.
 * </p>
 * The buffering is by default disabled and can be enabled by calling {@link #enableBuffering()}
 * or {@link #enableBuffering(int)} before writing the first byte into this output stream. The former
 * method enables buffering with the default size
 * <tt>{@value CommittingOutputStream#DEFAULT_BUFFER_SIZE}</tt> bytes specified in {@link #DEFAULT_BUFFER_SIZE}.
 * </p>
 *
 * @author Paul Sandoz
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @author Miroslav Fuksa
 */
final class CommittingOutputStream extends OutputStream {

    private static final Logger LOGGER = Logger.getLogger(CommittingOutputStream.class.getName());
    /**
     * Null stream provider.
     */
    private static final OutboundMessageContext.StreamProvider NULL_STREAM_PROVIDER =
            new OutboundMessageContext.StreamProvider() {
                @Override
                public OutputStream getOutputStream(int contentLength) throws IOException {
                    return new NullOutputStream();
                }
            };
    /**
     * Default size of the buffer which will be used if no user defined size is specified.
     */
    static final int DEFAULT_BUFFER_SIZE = 8192;
    /**
     * Adapted output stream.
     */
    private OutputStream adaptedOutput;
    /**
     * Buffering stream provider.
     */
    private OutboundMessageContext.StreamProvider streamProvider;
    /**
     * Internal buffer size.
     */
    private int bufferSize = 0;
    /**
     * Entity buffer.
     */
    private ByteArrayOutputStream buffer;
    /**
     * When {@code true}, the data are written directly to output stream and not to the buffer.
     */
    private boolean directWrite = true;
    /**
     * When {@code true}, the stream is already committed (redirected to adaptedOutput).
     */
    private boolean isCommitted;
    /**
     * When {@code true}, the stream is already closed.
     */
    private boolean isClosed;

    private static final String STREAM_PROVIDER_NULL = LocalizationMessages.STREAM_PROVIDER_NULL();
    private static final String COMMITTING_STREAM_BUFFERING_ILLEGAL_STATE = LocalizationMessages
            .COMMITTING_STREAM_BUFFERING_ILLEGAL_STATE();

    /**
     * Creates new committing output stream. The returned stream instance still needs to be initialized before
     * writing first bytes.
     */
    public CommittingOutputStream() {
    }

    /**
     * Set the buffering output stream provider. If the committing output stream works in buffering mode
     * this method must be called before first bytes are written into this stream.
     *
     * @param streamProvider non-null stream provider callback.
     */
    public void setStreamProvider(OutboundMessageContext.StreamProvider streamProvider) {
        if (isClosed) {
            throw new IllegalStateException(LocalizationMessages.OUTPUT_STREAM_CLOSED());
        }
        Preconditions.checkNotNull(streamProvider);

        if (this.streamProvider != null) {
            LOGGER.log(Level.WARNING, LocalizationMessages.COMMITTING_STREAM_ALREADY_INITIALIZED());
        }
        this.streamProvider = streamProvider;
    }

    /**
     * Enable buffering of the serialized entity.
     *
     * @param bufferSize size of the buffer. When the value is less or equal to zero the buffering will be disabled and {@code -1}
     *                   will be passed to the
     *                   {@link org.glassfish.jersey.message.internal.OutboundMessageContext.StreamProvider#getOutputStream(int) callback}.
     */
    public void enableBuffering(int bufferSize) {
        Preconditions.checkState(!isCommitted && (this.buffer == null || this.buffer.size() == 0),
                COMMITTING_STREAM_BUFFERING_ILLEGAL_STATE);
        this.bufferSize = bufferSize;
        if (bufferSize <= 0) {
            this.directWrite = true;
            this.buffer = null;
        } else {
            directWrite = false;
            buffer = new ByteArrayOutputStream(bufferSize);
        }
    }

    /**
     * Enable buffering of the serialized entity with the {@link #DEFAULT_BUFFER_SIZE default buffer size }.
     */
    public void enableBuffering() {
        enableBuffering(DEFAULT_BUFFER_SIZE);
    }

    /**
     * Determine whether the stream was already committed or not.
     *
     * @return {@code true} if this stream was already committed, {@code false} otherwise.
     */
    public boolean isCommitted() {
        return isCommitted;
    }

    private void commitStream() throws IOException {
        commitStream(-1);
    }

    private void commitStream(int currentSize) throws IOException {
        if (!isCommitted) {
            Preconditions.checkState(streamProvider != null, STREAM_PROVIDER_NULL);
            adaptedOutput = streamProvider.getOutputStream(currentSize);
            if (adaptedOutput == null) {
                adaptedOutput = new NullOutputStream();
            }

            directWrite = true;
            isCommitted = true;
        }
    }

    @Override
    public void write(byte b[]) throws IOException {
        if (directWrite) {
            commitStream();
            adaptedOutput.write(b);
        } else {
            if (b.length + buffer.size() > bufferSize) {
                flushBuffer(false);
                adaptedOutput.write(b);
            } else {
                buffer.write(b);
            }
        }
    }

    @Override
    public void write(byte b[], int off, int len) throws IOException {
        if (directWrite) {
            commitStream();
            adaptedOutput.write(b, off, len);
        } else {
            if (len + buffer.size() > bufferSize) {
                flushBuffer(false);
                adaptedOutput.write(b, off, len);
            } else {
                buffer.write(b, off, len);
            }
        }
    }

    @Override
    public void write(int b) throws IOException {
        if (directWrite) {
            commitStream();
            adaptedOutput.write(b);
        } else {
            if (buffer.size() + 1 > bufferSize) {
                flushBuffer(false);
                adaptedOutput.write(b);
            } else {
                buffer.write(b);
            }
        }
    }

    /**
     * Commit the output stream.
     *
     * @throws IOException when underlying stream returned from the callback method throws the io exception.
     */
    void commit() throws IOException {
        flushBuffer(true);
        commitStream();
    }

    @Override
    public void close() throws IOException {
        if (isClosed) {
            return;
        }

        isClosed = true;

        if (streamProvider == null) {
            streamProvider = NULL_STREAM_PROVIDER;
        }
        commit();
        adaptedOutput.close();
    }

    /**
     * Check if the committing output stream has been closed already.
     *
     * @return {@code true} if the stream has been closed, {@code false} otherwise.
     */
    public boolean isClosed() {
        return isClosed;
    }

    @Override
    public void flush() throws IOException {
        if (isCommitted()) {
            adaptedOutput.flush();
        }
    }

    private void flushBuffer(boolean endOfStream) throws IOException {
        if (!directWrite) {
            int currentSize;
            if (endOfStream) {
                currentSize = buffer == null ? 0 : buffer.size();
            } else {
                currentSize = -1;
            }

            commitStream(currentSize);
            if (buffer != null) {
                buffer.writeTo(adaptedOutput);
            }
        }
    }

}
