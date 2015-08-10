/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2015 Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;

import javax.ws.rs.ProcessingException;

import org.glassfish.jersey.internal.LocalizationMessages;

/**
 * Entity input stream customized for entity message processing:
 * <ul>
 * <li>contains {@link #isEmpty()} method.</li>
 * <li>{@link #close()} method throws Jersey-specific runtime exception in case of an IO error.</li>
 * </ul>
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class EntityInputStream extends InputStream {

    private InputStream input;
    private boolean closed = false;

    /**
     * Create an entity input stream instance wrapping the original input stream.
     * <p/>
     * In case the original entity stream is already of type {@code EntityInputStream},
     * the stream is returned without wrapping.
     *
     * @param inputStream input stream.
     * @return entity input stream.
     */
    public static EntityInputStream create(InputStream inputStream) {
        if (inputStream instanceof EntityInputStream) {
            return (EntityInputStream) inputStream;
        }

        return new EntityInputStream(inputStream);
    }

    /**
     * Extension constructor.
     *
     * @param input underlying wrapped input stream.
     */
    public EntityInputStream(InputStream input) {
        this.input = input;
    }

    @Override
    public int read() throws IOException {
        return input.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return input.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return input.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        return input.skip(n);
    }

    @Override
    public int available() throws IOException {
        return input.available();
    }

    @Override
    public void mark(int readLimit) {
        input.mark(readLimit);
    }

    @Override
    public boolean markSupported() {
        return input.markSupported();
    }

    /**
     * {@inheritDoc}
     * <p>
     * The method is customized to not throw an {@link IOException} if the reset operation fails. Instead,
     * a runtime {@link javax.ws.rs.ProcessingException} is thrown.
     * </p>
     *
     * @throws javax.ws.rs.ProcessingException in case the reset operation on the underlying entity input stream failed.
     */
    @Override
    public void reset() {
        try {
            input.reset();
        } catch (IOException ex) {
            throw new ProcessingException(LocalizationMessages.MESSAGE_CONTENT_BUFFER_RESET_FAILED(), ex);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * The method is customized to not throw an {@link IOException} if the close operation fails. Instead,
     * a warning message is logged.
     * </p>
     */
    @Override
    public void close() throws ProcessingException {
        final InputStream in = input;
        if (in == null) {
            return;
        }
        if (!closed) {
            try {
                in.close();
            } catch (IOException ex) {
                // This e.g. means that the underlying socket stream got closed by other thread somehow...
                throw new ProcessingException(LocalizationMessages.MESSAGE_CONTENT_INPUT_STREAM_CLOSE_FAILED(), ex);
            } finally {
                closed = true;
            }
        }
    }

    /**
     * Check if the underlying entity stream is empty.
     * <p>
     * Note that the operation may need to block until a first byte (or EOF) is available in the stream.
     * </p>
     *
     * @return {@code true} if the entity stream is empty, {@code false} otherwise.
     */
    public boolean isEmpty() {
        ensureNotClosed();

        final InputStream in = input;
        if (in == null) {
            return true;
        }

        try {
            // Try #markSupported first - #available on WLS waits until socked timeout is reached when chunked encoding is used.
            if (in.markSupported()) {
                in.mark(1);
                int i = in.read();
                in.reset();
                return i == -1;
            } else {
                try {
                    if (in.available() > 0) {
                        return false;
                    }
                } catch (IOException ioe) {
                    // NOOP. Try other approaches as this can fail on WLS.
                }

                int b = in.read();
                if (b == -1) {
                    return true;
                }

                PushbackInputStream pbis;
                if (in instanceof PushbackInputStream) {
                    pbis = (PushbackInputStream) in;
                } else {
                    pbis = new PushbackInputStream(in, 1);
                    input = pbis;
                }
                pbis.unread(b);

                return false;
            }
        } catch (IOException ex) {
            throw new ProcessingException(ex);
        }
    }

    /**
     * Check that the entity input stream has not been closed yet.
     *
     * @throws IllegalStateException in case the entity input stream has been closed.
     */
    public void ensureNotClosed() throws IllegalStateException {
        if (closed) {
            throw new IllegalStateException(LocalizationMessages.ERROR_ENTITY_STREAM_CLOSED());
        }
    }

    /**
     * Get the closed status of this input stream.
     *
     * @return {@code true} if the stream has been closed, {@code false} otherwise.
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * Get the wrapped input stream instance.
     *
     * @return wrapped input stream instance.
     */
    public final InputStream getWrappedStream() {
        return input;
    }

    /**
     * Set the wrapped input stream instance.
     *
     * @param wrapped new input stream instance to be wrapped.
     */
    public final void setWrappedStream(InputStream wrapped) {
        input = wrapped;
    }
}
