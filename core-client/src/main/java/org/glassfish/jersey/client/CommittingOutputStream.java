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
package org.glassfish.jersey.client;

import java.io.IOException;
import java.io.OutputStream;

/**
 * A committing output stream that commits before
 * the first byte is written to the adapted {@link OutputStream}.
 * <p>
 * This class may be overridden to provide the commit functionality.
 *
 * @author Paul Sandoz
 */
public abstract class CommittingOutputStream extends OutputStream {

    private OutputStream o;

    private boolean isCommitted;

    /**
     * Construct a new instance.
     * <p>
     * The method {@link #getOutputStream() } MUST be overridden
     * to return an output stream.
     */
    public CommittingOutputStream() {
    }

    /**
     * Construct a new instance with an output stream to adapt.
     *
     * @param o the adapted output stream.
     * @throws IllegalArgumentException if <code>o</code> is null.
     */
    public CommittingOutputStream(OutputStream o) {
        if (o == null)
            throw new IllegalArgumentException();

        this.o = o;
    }

    @Override
    public void write(byte b[]) throws IOException {
        commitWrite();
        o.write(b);
    }

    @Override
    public void write(byte b[], int off, int len) throws IOException {
        commitWrite();
        o.write(b, off, len);
    }

    @Override
    public void write(int b) throws IOException {
        commitWrite();
        o.write(b);
    }

    @Override
    public void flush() throws IOException {
        commitWrite();
        o.flush();
    }

    @Override
    public void close() throws IOException {
        commitWrite();
        o.close();
    }

    private void commitWrite() throws IOException {
        if (!isCommitted) {
            isCommitted = true;

            commit();

            if (o == null)
                o = getOutputStream();
        }
    }

    /**
     * Get the adapted output stream.
     * <p>
     * This method MUST be overriden if the empty constructor is
     * utilized to construct an instance of this class.
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