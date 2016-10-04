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

package org.glassfish.jersey.netty.connector.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Input stream which servers as Request entity input.
 * <p>
 * Converts Netty NIO buffers to an input streams and stores them in the queue,
 * waiting for Jersey to process it.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class NettyInputStream extends InputStream {

    private volatile boolean end = false;

    /**
     * End of input.
     */
    public static final InputStream END_OF_INPUT = new InputStream() {
        @Override
        public int read() throws IOException {
            return 0;
        }

        @Override
        public String toString() {
            return "END_OF_INPUT " + super.toString();
        }
    };

    /**
     * Unexpected end of input.
     */
    public static final InputStream END_OF_INPUT_ERROR = new InputStream() {
        @Override
        public int read() throws IOException {
            return 0;
        }

        @Override
        public String toString() {
            return "END_OF_INPUT_ERROR " + super.toString();
        }
    };

    private final LinkedBlockingDeque<InputStream> isList;

    public NettyInputStream(LinkedBlockingDeque<InputStream> isList) {
        this.isList = isList;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {

        if (end) {
            return -1;
        }

        InputStream take;
        try {
            take = isList.take();

            if (checkEndOfInput(take)) {
                return -1;
            }

            int read = take.read(b, off, len);

            if (take.available() > 0) {
                isList.addFirst(take);
            }

            return read;
        } catch (InterruptedException e) {
            throw new IOException("Interrupted.", e);
        }
    }

    @Override
    public int read() throws IOException {

        if (end) {
            return -1;
        }

        try {
            InputStream take = isList.take();

            if (checkEndOfInput(take)) {
                return -1;
            }

            int read = take.read();

            if (take.available() > 0) {
                isList.addFirst(take);
            }

            return read;
        } catch (InterruptedException e) {
            throw new IOException("Interrupted.", e);
        }
    }

    @Override
    public int available() throws IOException {
        InputStream peek = isList.peek();
        if (peek != null) {
            return peek.available();
        }

        return 0;
    }

    private boolean checkEndOfInput(InputStream take) throws IOException {
        if (take == END_OF_INPUT) {
            end = true;
            return true;
        } else if (take == END_OF_INPUT_ERROR) {
            end = true;
            throw new IOException("Connection was closed prematurely.");
        }
        return false;
    }
}
