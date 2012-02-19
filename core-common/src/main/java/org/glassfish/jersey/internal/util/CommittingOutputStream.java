/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.internal.util;

import java.io.IOException;
import java.io.OutputStream;

/**
 * An abstract committing output stream adapter that performs a {@link #commit()
 * commit} before the first byte is written to the adapted {@link OutputStream}.
 *
 * Concrete implementations of the class typically override the commit operation
 * to perform any initialization on the adapted output stream.
 *
 * @author Paul Sandoz
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public abstract class CommittingOutputStream extends OutputStream {

    /**
     * Adapted output stream.
     */
    private OutputStream adaptedOutput;
    /**
     * Determines whether the stream was already committed or not.
     */
    private boolean isCommitted = false;

    /**
     * Construct a new committing output stream using a deferred initialization
     * of the adapted output stream.
     * <p />
     * When this constructor is utilized to construct a committing output stream
     * instance, the method {@link #getOutputStream()} MUST be overridden to return
     * the adapted output stream.
     *
     * @see #CommittingOutputStream(OutputStream) adapting constructor
     */
    public CommittingOutputStream() {
    }

    /**
     * Construct a new committing output stream using an eager initialization of
     * the adapted output stream.
     * <p />
     * When this constructor is utilized to construct a committing output stream
     * instance, the method {@link #getOutputStream()} will be ignored and never
     * invoked to retrieve the adapted output stream.
     *
     * @param out the adapted output stream.
     * @throws IllegalArgumentException if supplied output stream is {@code null}.
     *
     * @see #CommittingOutputStream(OutputStream) deferred initialization constructor
     */
    public CommittingOutputStream(OutputStream out) {
        if (out == null) {
            throw new IllegalArgumentException();
        }

        this.adaptedOutput = out;
    }

    @Override
    public void write(byte b[]) throws IOException {
        if (b.length > 0) {
            commitWrite();
            adaptedOutput.write(b);
        }
    }

    @Override
    public void write(byte b[], int off, int len) throws IOException {
        if (len > 0) {
            commitWrite();
            adaptedOutput.write(b, off, len);
        }
    }

    @Override
    public void write(int b) throws IOException {
        commitWrite();
        adaptedOutput.write(b);
    }

    @Override
    public void flush() throws IOException {
        commitWrite();
        adaptedOutput.flush();
    }

    @Override
    public void close() throws IOException {
        commitWrite();
        adaptedOutput.close();
    }

    private void commitWrite() throws IOException {
        if (!isCommitted) {
            isCommitted = true;

            commit();

            if (adaptedOutput == null) {
                adaptedOutput = getOutputStream();
            }
        }
    }

    /**
     * Get the adapted output stream.
     *
     * The method is called at most once (in case the internal adapted output stream
     * has not been initialized via {@link #CommittingOutputStream(java.io.OutputStream)
     * adapting constructor}) as part of the commit operation immediately after
     * the {@link #commit()} method has been invoked.
     * <p>
     * This method MUST be overridden if the empty {@link #CommittingOutputStream()
     * deferred initialization constructor} is utilized to construct an instance
     * of this class.
     *
     * @return the adapted output stream.
     * @throws java.io.IOException
     */
    protected OutputStream getOutputStream() throws IOException {
        throw new IllegalStateException();
    }

    /**
     * Perform the commit functionality.
     *
     * @throws java.io.IOException
     */
    protected abstract void commit() throws IOException;
};