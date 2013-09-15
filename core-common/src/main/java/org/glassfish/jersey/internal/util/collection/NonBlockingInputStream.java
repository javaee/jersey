/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.internal.util.collection;

import java.io.IOException;
import java.io.InputStream;

/**
 * An abstract {@link InputStream} extension that defines contract for non-blocking
 * streaming {@code read} operations.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public abstract class NonBlockingInputStream extends InputStream {
    /**
     * Constant used as a return value from {@link #tryRead()} method, to indicate that nothing
     * has been read.
     */
    public static final int NOTHING = Integer.MIN_VALUE;

    /**
     * Returns an estimate of the number of bytes that can be read (or
     * skipped over) from this input stream without blocking by the next
     * invocation of a method for this input stream. The next invocation
     * might be the same thread or another thread.  A single read or skip of this
     * many bytes will not block, but may read or skip fewer bytes.
     * <p>
     * Note that while some implementations of {@code InputStream} will return
     * the total number of bytes in the stream, many will not. It is
     * never correct to use the return value of this method to allocate
     * a buffer intended to hold all data in this stream.
     * </p>
     * <p>
     * A subclass' implementation of this method may choose to throw an
     * {@link java.io.IOException} if this input stream has been closed by
     * invoking the {@link #close()} method.
     * </p>
     * <p>
     * The default implementation of this method in {@code NonBlockingInputStream}
     * throws an {@link UnsupportedOperationException}. This method must be overridden
     * by subclasses. The overriding implementations must guarantee non-blocking behavior
     * of the method. The overriding implementation must also guarantee that a non-empty
     * stream does not return zero from the method. IOW, it must be possible to use the
     * method for empty check: {@code stream.available() == 0}
     * </p>
     *
     * @return an estimate of the number of bytes that can be read (or skipped
     *         over) from this input stream without blocking or {@code 0} when
     *         it reaches the end of the input stream or the stream is empty.
     * @throws java.io.IOException if an I/O error occurs.
     */
    @Override
    public int available() throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Behaves mostly like {@link java.io.InputStream#read()}.
     *
     * The main difference is that this method is non-blocking. In case there are no
     * data available to be read, the method returns {@link #NOTHING} immediately.
     *
     * @return next byte of data, {@code -1} if end of the stream has been reached or
     *         {@link #NOTHING} in case no data are available to be read at the moment.
     * @throws java.io.IOException if an I/O error occurs.
     */
    public abstract int tryRead() throws IOException;

    /**
     * Behaves mostly like {@link java.io.InputStream#read(byte[])}.
     *
     * The main difference is that this method is non-blocking. In case there are no
     * data available to be read, the method returns zero immediately.
     *
     * @param b the buffer into which the data is read.
     * @return the total number of bytes read into the buffer or {@code -1} if end of the
     *         stream has been reached or {@code 0} in case no data are available to be
     *         read at the moment.
     * @throws java.io.IOException if an I/O error occurs.
     */
    public abstract int tryRead(byte b[]) throws IOException;

    /**
     * Behaves mostly like {@link java.io.InputStream#read(byte[], int, int)}.
     *
     * The main difference is that this method is non-blocking. In case there are no
     * data available to be read, the method returns zero immediately.
     *
     * @param b   the buffer into which the data is read.
     * @param off the start offset in array {@code b}
     *            at which the data is written.
     * @param len the maximum number of bytes to read.
     * @return the total number of bytes read into the buffer or {@code -1} if end of the
     *         stream has been reached or {@code 0} in case no data are available to be
     *         read at the moment.
     * @throws java.io.IOException if an I/O error occurs.
     */
    public abstract int tryRead(byte b[], int off, int len) throws IOException;
}
