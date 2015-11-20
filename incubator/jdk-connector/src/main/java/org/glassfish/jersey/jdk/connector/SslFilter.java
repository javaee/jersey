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

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLParameters;

/**
 * @author Petr Janouch (petr.janouch at oracle.com)
 */
class SslFilter extends Filter<ByteBuffer, ByteBuffer, ByteBuffer, ByteBuffer> {

/* SYNCHRONIZATION NOTE: SSLEngine#wrap and SSLEngine#unwrap can be done concurrently (one thread doing wrap
    and another doing unwrap). The same operation cannot be done concurrently (2 threads doing wrap).

    Method doHandshakeStep must be synchronized, because it might be entered both by writing and reading thread
    during re-handshake. Write, close and re-handshake cannot be done concurrently, because all those operations might
    do SSLEngine#wrap. Read can be be done concurrently with any other operation, because even thought re-handshake
    can do SSLEngine#unwrap, it won't do so if it was entered from write operation.

    Operations upstreamFilter#onRead cannot be done while holding a lock of this class. Doing so might lead to a deadlock. An
    example of deadlock would be if a thread holding a lock in upstreamFilter#onRead writes a response synchronously (blocks
    and waits for write completion handler). The write completion handler might be executed by another thread which will not be
    able to obtain a lock for this class.*/

    /* Some operations on SSL engine require a buffer as a parameter even if they don't need any data.
    This buffer is for that purpose. */
    private static final ByteBuffer emptyBuffer = ByteBuffer.allocate(0);

    // buffer for passing data to the upper filter
    private final ByteBuffer applicationInputBuffer;
    // buffer for passing data to the transport filter
    private final ByteBuffer networkOutputBuffer;
    private final SSLEngine sslEngine;
    private final HostnameVerifier customHostnameVerifier;
    private final String serverHost;
    private final WriteQueue writeQueue = new WriteQueue();

    private volatile State state = State.NOT_STARTED;
    /*
     * Pending write operation stored when writing data was not possible. It will be resumed when write operation is
     * available again. Only one write operation can be in progress at a time. Trying to store more than one pending
     * application write indicates that an upper stack called write without waiting for the completion handler
     * of the previous write.
     * Currently this is used only during re-handshake.
     */
    private Runnable pendingApplicationWrite = null;

    /**
     * SSL Filter constructor, takes upstream filter as a parameter.
     *
     * @param downstreamFilter       a filter that is positioned under the SSL filter.
     * @param sslContext             configuration of SSL engine.
     * @param serverHost             server host (hostname or IP address), which will be used to verify authenticity of
     *                               the server (the provided host will be compared against the host in the certificate
     *                               provided by the server). IP address and hostname cannot be used interchangeably -
     *                               if a certificate contains hostname and an IP address of the server is provided here,
     *                               the verification will fail.
     * @param customHostnameVerifier hostname verifier that will be used instead of the default one.
     */
    SslFilter(Filter<ByteBuffer, ByteBuffer, ?, ?> downstreamFilter,
              SSLContext sslContext,
              String serverHost,
              HostnameVerifier customHostnameVerifier) {
        super(downstreamFilter);
        this.serverHost = serverHost;
        sslEngine = sslContext.createSSLEngine(serverHost, -1);
        sslEngine.setUseClientMode(true);
        this.customHostnameVerifier = customHostnameVerifier;

        /**
         * Enable server host verification.
         * This can be moved to {@link SslEngineConfigurator} with the rest of {@link SSLEngine} configuration
         * when {@link SslEngineConfigurator} supports Java 7.
         */
        if (customHostnameVerifier == null) {
            SSLParameters sslParameters = sslEngine.getSSLParameters();
            sslParameters.setEndpointIdentificationAlgorithm("HTTPS");
            sslEngine.setSSLParameters(sslParameters);
        }

        applicationInputBuffer = ByteBuffer.allocate(sslEngine.getSession().getApplicationBufferSize());
        networkOutputBuffer = ByteBuffer.allocate(sslEngine.getSession().getPacketBufferSize());
    }

    @Override
    synchronized void write(final ByteBuffer applicationData, final CompletionHandler<ByteBuffer> completionHandler) {
        switch (state) {
            // before SSL is started, write just passes through
            case NOT_STARTED: {
                writeQueue.write(applicationData, completionHandler);
                return;
            }

            /* TODO:
             The current model does not permit calling write before SSL handshake has completed, if we allow this
             we could easily get rid of the onSslHandshakeCompleted event. The SSL filter can simply store the write until
             the handshake has completed like during re-handshake. With such a change HANDSHAKING and REHANDSHAKING could
             be collapsed into one state. */
            case HANDSHAKING: {
                completionHandler.failed(new IllegalStateException("Cannot write until SSL handshake has been completed"));
                break;
            }

            /* Suspend all writes until the re-handshaking is done. Data are permitted during re-handshake in SSL, but this
             would only complicate things */
            case REHANDSHAKING: {
                storePendingApplicationWrite(applicationData, completionHandler);
                break;
            }

            case DATA: {
                handleWrite(applicationData, completionHandler);
                break;
            }

            case CLOSED: {
                // the engine is closed just abort with failure
                completionHandler.failed(new IllegalStateException(LocalizationMessages.SSL_SESSION_CLOSED()));
                break;
            }
        }
    }

    private void handleWrite(final ByteBuffer applicationData, final CompletionHandler<ByteBuffer> completionHandler) {
        try {
            // transport buffer always writes all data, so there are not leftovers in the networkOutputBuffer
            networkOutputBuffer.clear();
            SSLEngineResult result = sslEngine.wrap(applicationData, networkOutputBuffer);

            switch (result.getStatus()) {
                case BUFFER_OVERFLOW: {
                    /* this means that the content of the ssl packet (max 16kB) did not fit into
                       networkOutputBuffer, we make sure to set networkOutputBuffer > max 16kB + SSL headers
                       when initializing this filter. This indicates a bug. */
                    throw new IllegalStateException("SSL packet does not fit into the network buffer: "
                            + networkOutputBuffer + "\n" + getDebugState());
                }

                case BUFFER_UNDERFLOW: {
                    /* This basically says that there is not enough data to create an SSL packet. Javadoc suggests that
                    BUFFER_UNDERFLOW can occur only after unwrap(), but to be 100% sure we handle all possible error states: */
                    throw new IllegalStateException("SSL engine underflow with the following application input: "
                            + applicationData + "\n" + getDebugState());
                }

                case CLOSED: {
                    state = State.CLOSED;
                    break;
                }

                case OK: {
                    // check if we started re-handshaking
                    if (result.getHandshakeStatus() != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
                        state = State.REHANDSHAKING;
                    }

                    networkOutputBuffer.flip();
                    // write only if something was written to the output buffer
                    if (networkOutputBuffer.hasRemaining()) {
                        writeQueue.write(networkOutputBuffer, new CompletionHandler<ByteBuffer>() {
                            @Override
                            public void completed(ByteBuffer result) {
                                handlePostWrite(applicationData, completionHandler);
                            }

                            @Override
                            public void failed(Throwable throwable) {
                                completionHandler.failed(throwable);
                            }
                        });
                    } else {
                        handlePostWrite(applicationData, completionHandler);
                    }
                    break;
                }
            }

        } catch (SSLException e) {
            handleSslError(e);
        }
    }

    private synchronized void handlePostWrite(final ByteBuffer applicationData,
                                              final CompletionHandler<ByteBuffer> completionHandler) {
        if (state == State.REHANDSHAKING) {
            if (applicationData.hasRemaining()) {
                // the remaining data will be sent after re-handshake
                storePendingApplicationWrite(applicationData, completionHandler);
                // start re-handshaking
                doHandshakeStep(emptyBuffer);
            }
        } else {
            if (applicationData.hasRemaining()) {
                // make sure to empty the application output buffer
                handleWrite(applicationData, completionHandler);
            } else {
                completionHandler.completed(applicationData);
            }
        }
    }

    private void storePendingApplicationWrite(final ByteBuffer applicationData,
                                              final CompletionHandler<ByteBuffer> completionHandler) {
        // store the write until re-handshaking is completed
        if (pendingApplicationWrite != null) {
            /* If this happens it means a bug in this class or upper layer called another write() without waiting
             for a completion handler of the previous one. */
            throw new IllegalStateException("Only one write operation can be in progress\n" + getDebugState());
        }

        pendingApplicationWrite = () -> {
            // go again through the entire write procedure like this data came directly from the application
            write(applicationData, completionHandler);
        };
    }

    @Override
    synchronized void close() {
        if (state == State.NOT_STARTED) {
            downstreamFilter.close();
            return;
        }

        sslEngine.closeOutbound();
        try {
            LazyBuffer lazyBuffer = new LazyBuffer();

            while (sslEngine.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_WRAP) {
                ByteBuffer buffer = lazyBuffer.get();
                SSLEngineResult result = sslEngine.wrap(emptyBuffer, buffer);

                switch (result.getStatus()) {
                    case BUFFER_OVERFLOW: {
                        lazyBuffer.resize();
                        break;
                    }

                    case BUFFER_UNDERFLOW: {
                        /* This basically says that there is not enough data to create an SSL packet. Javadoc suggests that
                        BUFFER_UNDERFLOW can occur only after unwrap(), but to be 100% sure we handle all possible error
                        states: */
                        throw new IllegalStateException("SSL engine underflow while close operation \n" + getDebugState());
                    }

                    // CLOSE or OK are expected outcomes
                }

            }

            if (lazyBuffer.isAllocated()) {
                ByteBuffer buffer = lazyBuffer.get();
                buffer.flip();
                writeQueue.write(buffer, new CompletionHandler<ByteBuffer>() {

                    @Override
                    public void completed(ByteBuffer result) {
                        downstreamFilter.close();
                    }

                    @Override
                    public void failed(Throwable throwable) {
                        downstreamFilter.close();
                    }
                });
            } else {
                // make sure we close even if SSL had nothing to send
                downstreamFilter.close();
            }
        } catch (Exception e) {
            handleSslError(e);
        }
    }

    @Override
    boolean processRead(ByteBuffer networkData) {
        /* A flag indicating if we should keep reading from the network buffer.
        If false, the buffer contains an uncompleted packet -> stop reading, SSL engine accepts only whole packets */
        boolean readMore = true;

        while (networkData.hasRemaining() && readMore) {
            switch (state) {
                // before SSL is started write just passes through
                case NOT_STARTED: {
                    return true;
                }

                case HANDSHAKING:
                case REHANDSHAKING: {
                    readMore = doHandshakeStep(networkData);
                    break;
                }

                case DATA: {
                    readMore = handleRead(networkData);
                    break;
                }

                case CLOSED: {
                    // drop any data that arrive after the SSL has been closed
                    networkData.clear();
                    readMore = false;
                }
            }
        }

        return false;
    }

    private boolean handleRead(ByteBuffer networkData) {
        try {
            applicationInputBuffer.clear();
            SSLEngineResult result = sslEngine.unwrap(networkData, applicationInputBuffer);

            switch (result.getStatus()) {
                case BUFFER_OVERFLOW: {
                    /* This means that the content of the ssl packet (max 16kB) did not fit into
                       applicationInputBuffer, but we make sure to set applicationInputBuffer > max 16kB
                       when initializing this filter. This indicates a bug.*/
                    throw new IllegalStateException("Contents of a SSL packet did not fit into buffer: "
                            + applicationInputBuffer + "\n" + getDebugState());
                }

                case BUFFER_UNDERFLOW: {
                    // the ssl packet is not full, return and indicate that we won't get more from this buffer
                    return false;
                }

                case CLOSED:
                case OK: {
                    if (result.bytesProduced() > 0) {
                        applicationInputBuffer.flip();
                        upstreamFilter.onRead(applicationInputBuffer);
                        applicationInputBuffer.compact();
                    }

                    if (sslEngine.isInboundDone()) {
                        /* we have just received a close alert from our peer, so we are done. If there is something
                        remaining in the input buffer, just drop it. */

                        // signal that there is nothing useful left in this buffer
                        return false;
                    }

                    // we started re-handshaking
                    if (result.getHandshakeStatus() != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING
                            // make sure we don't confuse re-handshake with closing handshake
                            && !sslEngine.isOutboundDone()) {
                        state = State.REHANDSHAKING;
                        return doHandshakeStep(networkData);
                    }

                    break;
                }
            }
        } catch (SSLException e) {
            handleSslError(e);
        }

        return true;
    }

    private boolean doHandshakeStep(ByteBuffer networkData) {
        /* Buffer used to store application data read during this handshake step.
        Application data can be interleaved with handshake messages only during re-handshake.
        We don't use applicationInputBuffer, because we might want to store more than one packet */
        LazyBuffer inputBuffer = new LazyBuffer();
        boolean handshakeFinished = false;

        synchronized (this) {
            if (SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING.equals(sslEngine.getHandshakeStatus())) {
                // we stopped handshaking while waiting for the lock
                return true;
            }

            try {
                /* we don't use networkOutputBuffer, because there might be a write operation still in progress ->
                we don't want to corrupt the buffer it is using */
                LazyBuffer outputBuffer = new LazyBuffer();
                boolean stepFinished = false;
                while (!stepFinished) {
                    SSLEngineResult.HandshakeStatus hs = sslEngine.getHandshakeStatus();

                    switch (hs) {
                        case NOT_HANDSHAKING: {
                            /* This should never happen. If we are here and not handshaking, it means a bug
                            in the state machine of this class, because we stopped handshaking and did not exit this while loop.
                            The could be caused either by overlooking FINISHED state or incorrectly treating an error. */

                            throw new IllegalStateException("Trying to handshake, but SSL engine not in HANDSHAKING state."
                                    + "SSL filter state: \n" + getDebugState());
                        }

                        case FINISHED: {
                            /* According to SSLEngine javadoc FINISHED status can be returned only in SSLEngineResult,
                            but just to make sure we don't end up in an infinite loop when presented with an SSLEngine
                            implementation that does not respect this:*/
                            stepFinished = true;
                            handshakeFinished = true;
                            break;
                        }
                        // needs to write data to the network
                        case NEED_WRAP: {
                            ByteBuffer byteBuffer = outputBuffer.get();
                            SSLEngineResult result = sslEngine.wrap(emptyBuffer, byteBuffer);

                            if (result.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.FINISHED) {
                                stepFinished = true;
                                handshakeFinished = true;
                            }

                            switch (result.getStatus()) {
                                case BUFFER_OVERFLOW: {
                                    outputBuffer.resize();
                                    break;
                                }

                                case BUFFER_UNDERFLOW: {
                                    /* This basically says that there is not enough data to create an SSL packet. Javadoc suggests
                                    that BUFFER_UNDERFLOW can occur only after unwrap(), but to be 100% sure we handle all
                                    possible error states: */
                                    throw new IllegalStateException("SSL engine underflow with the following SSL filter "
                                            + "state: \n" + getDebugState());
                                }

                                case CLOSED: {
                                    stepFinished = true;
                                    state = State.CLOSED;
                                    break;
                                }
                            }

                            break;
                        }

                        case NEED_UNWRAP: {

                            SSLEngineResult result = sslEngine.unwrap(networkData, applicationInputBuffer);

                            applicationInputBuffer.flip();
                            if (applicationInputBuffer.hasRemaining()) {
                                // data can flow during re-handshake
                                inputBuffer.append(applicationInputBuffer);
                            }
                            applicationInputBuffer.compact();

                            if (result.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.FINISHED) {
                                stepFinished = true;
                                handshakeFinished = true;
                            }

                            switch (result.getStatus()) {
                                case BUFFER_OVERFLOW: {
                                    /* This means that the content of the ssl packet (max 16kB) did not fit into
                                    applicationInputBuffer, but we make sure to set applicationInputBuffer > max 16kB
                                    when initializing this filter. This indicates a bug. */
                                    throw new IllegalStateException("SSL packet does not fit into the network buffer: "
                                            + getDebugState());
                                }

                                case BUFFER_UNDERFLOW: {
                                    // indicate that we won't get more from this buffer
                                    stepFinished = true;
                                    break;
                                }

                                case CLOSED: {
                                    stepFinished = true;
                                    state = State.CLOSED;
                                    break;
                                }
                            }

                            break;
                        }
                        // needs to execute long running task (for instance validating certificates)
                        case NEED_TASK: {
                            Runnable delegatedTask;
                            while ((delegatedTask = sslEngine.getDelegatedTask()) != null) {
                                delegatedTask.run();
                            }
                            break;
                        }
                    }
                }

                // now write the stored wrap() results
                if (outputBuffer.isAllocated()) {
                    ByteBuffer buffer = outputBuffer.get();
                    buffer.flip();
                    writeQueue.write(buffer, null);
                }

            } catch (Exception e) {
                handleSslError(e);
            }
        }

        /* Handle any read data.
        We have to execute upstreamFilter.onRead after releasing the lock. See the synchronization note on top.
        Only one read operation can be in progress at a time, so even though we have released the lock, no other
        SSlEngine#unwrap can be performed until this method returns. So there is no chance of the read data
        being mixed up */
        if (inputBuffer.isAllocated()) {
            ByteBuffer buffer = inputBuffer.get();
            upstreamFilter.onRead(buffer);
        }

        if (handshakeFinished) {
            handleHandshakeFinished();
            // indicate that there still might be usable data in the input buffer
            return true;
        }

        /* if we are here, it means that we are waiting for more data -> indicate that there is nothing usable in the
        input buffer left */
        return false;
    }

    private void handleHandshakeFinished() {
        // Apply a custom host verifier if present. Do it for both handshaking and re-handshaking.
        if (customHostnameVerifier != null && !customHostnameVerifier.verify(serverHost, sslEngine.getSession())) {
            handleSslError(new SSLException("Server host name verification using " + customHostnameVerifier
                    .getClass() + " has failed"));
            return;
        }

        if (state == State.HANDSHAKING) {
            state = State.DATA;
            upstreamFilter.onSslHandshakeCompleted();
        } else if (state == State.REHANDSHAKING) {
            state = State.DATA;
            if (pendingApplicationWrite != null) {
                Runnable write = pendingApplicationWrite;
                // set pending write to null to cover the extremely improbable case that we start re-handshaking again
                pendingApplicationWrite = null;

                write.run();
            }
        }
    }

    private void handleSslError(Throwable t) {
        onError(t);
    }

    @Override
    void startSsl() {
        try {
            state = State.HANDSHAKING;
            sslEngine.beginHandshake();
            doHandshakeStep(emptyBuffer);
        } catch (SSLException e) {
            handleSslError(e);
        }
    }

    /**
     * Only for test.
     */
    void rehandshake() {
        try {
            sslEngine.beginHandshake();
        } catch (SSLException e) {
            handleSslError(e);
        }
    }

    /**
     * Returns a printed current state of the SslFilter that could be helpful for troubleshooting.
     */
    private String getDebugState() {
        return "SslFilter{"
                + "\napplicationInputBuffer=" + applicationInputBuffer
                + ",\nnetworkOutputBuffer=" + networkOutputBuffer
                + ",\nsslEngineStatus=" + sslEngine.getHandshakeStatus()
                + ",\nsslSession=" + sslEngine.getSession()
                + ",\nstate=" + state
                + ",\npendingApplicationWrite=" + pendingApplicationWrite
                + ",\npendingWritesSize=" + writeQueue
                + '}';
    }

    private enum State {
        NOT_STARTED,
        HANDSHAKING,
        REHANDSHAKING,
        DATA,
        CLOSED
    }

    private class LazyBuffer {

        private ByteBuffer buffer = null;

        ByteBuffer get() {
            if (buffer == null) {
                buffer = ByteBuffer.allocate(sslEngine.getSession().getPacketBufferSize());
            }

            return buffer;
        }

        boolean isAllocated() {
            return buffer != null;
        }

        void resize() {
            int increment = sslEngine.getSession().getPacketBufferSize();
            int newSize = buffer.position() + increment;
            ByteBuffer newBuffer = ByteBuffer.allocate(newSize);
            buffer.flip();
            newBuffer.flip();
            buffer = Utils.appendBuffers(newBuffer, buffer, newBuffer.limit(), 50);
            buffer.compact();
        }

        void append(ByteBuffer b) {
            if (buffer == null) {
                buffer = ByteBuffer.allocate(b.remaining());
                buffer.flip();
            }
            int newSize = buffer.limit() + b.remaining();
            buffer = Utils.appendBuffers(buffer, b, newSize, 50);
        }
    }

    // synchronized on the outer class, because there is a danger of deadlock if this has its own lock
    private class WriteQueue {

        private final Queue<Runnable> pendingWrites = new LinkedList<>();

        void write(final ByteBuffer data, final CompletionHandler<ByteBuffer> completionHandler) {
            synchronized (SslFilter.this) {
                Runnable r = () -> downstreamFilter.write(data, new CompletionHandler<ByteBuffer>() {

                    @Override
                    public void completed(ByteBuffer result) {
                        if (completionHandler != null) {
                            completionHandler.completed(result);
                        }

                        onWriteCompleted();
                    }

                    @Override
                    public void failed(Throwable throwable) {
                        if (completionHandler != null) {
                            completionHandler.failed(throwable);
                        }

                        onWriteCompleted();
                    }
                });

                pendingWrites.offer(r);
                // if our task is the first one in the queue, there is no other write task in progress -> process it
                if (pendingWrites.peek() == r) {
                    r.run();
                }
            }
        }

        private void onWriteCompleted() {
            synchronized (SslFilter.this) {
                // task in progress is at the head of the queue -> remove it
                pendingWrites.poll();
                Runnable next = pendingWrites.peek();

                if (next != null) {
                    next.run();
                }
            }
        }

        @Override
        public String toString() {
            synchronized (SslFilter.this) {
                return "WriteQueue{"
                        + "pendingWrites="
                        + pendingWrites.size()
                        + '}';
            }
        }
    }
}
