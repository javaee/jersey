/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.server;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 * Used for sending messages in "typed" chunks.
 *
 * Useful for long running processes, which are able to produce partial responses.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class ChunkedResponse<T> {

    private final BlockingDeque<T> queue = new LinkedBlockingDeque<T>();
    private final Class<T> clazz;
    private final long interval;
    private final TimeUnit timeUnit;

    private boolean closed = false;

    /**
     * Create {@link ChunkedResponse} with specified type.
     *
     * TODO: make EventChannel use this class as impl base
     *
     * @param clazz chunk type
     */
    public ChunkedResponse(Class<T> clazz) {
        this(clazz, null, null);
    }

    /**
     * Create {@link ChunkedResponse} with specified type and polling interval. Polling interval is used when retrieving
     * data - basically it specifies how often will be connection checked if it is closed from the client side.
     *
     * @param interval polling interval. Default value is {@code 5}.
     * @param timeUnit polling interval {@link TimeUnit}. Default value is {@code TimeUnit.SECONDS}.
     * @param clazz chunk type
     */
    public ChunkedResponse(Class<T> clazz, Long interval, TimeUnit timeUnit) {
        this.clazz = clazz;
        this.interval = (interval == null ? 5 : interval);
        this.timeUnit = (timeUnit == null ? TimeUnit.SECONDS : timeUnit);
    }


    /**
     * Write chunk.
     *
     * @param t chunk instance to be written.
     * @throws IllegalStateException when {@link ChunkedResponse} is closed.
     */
    public void write(T t) {
        if(closed) {
            throw new IllegalStateException();
        }

        if(t != null) {
            queue.add(t);
        }
    }

    /**
     * Close this response - it will be finalized and underlying connections will be closed
     * or made available for another response.
     *
     */
    public void close() {
        closed = true;
    }

    /**
     * Get state information.
     *
     * Please note that {@link ChunkedResponse} can be closed by the client side - client can close connection
     * from its side.
     *
     * @return true when closed, false otherwise.
     */
    boolean isClosed() {
        return closed;
    }


    protected Class<T> getChunkType() {
        return clazz;
    }

    protected T getChunk() throws InterruptedException {
        if(closed) {
            return null;
        }

        return queue.poll(interval, timeUnit);
    }

}
