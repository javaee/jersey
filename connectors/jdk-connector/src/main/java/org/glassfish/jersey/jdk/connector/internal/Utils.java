/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package org.glassfish.jersey.jdk.connector.internal;

import java.net.URI;
import java.nio.ByteBuffer;

/**
 * @author Petr Janouch (petr.janouch at oracle.com)
 */
public class Utils {

    /**
     * Concatenates two buffers into one. If buffer given as first argument has enough space for putting
     * the other one, it will be done and the original buffer will be returned. Otherwise new buffer will
     * be created.
     *
     * @param buffer  first buffer.
     * @param buffer1 second buffer.
     * @return concatenation.
     */
    static ByteBuffer appendBuffers(ByteBuffer buffer, ByteBuffer buffer1, int incomingBufferSize, int BUFFER_STEP_SIZE) {

        final int limit = buffer.limit();
        final int capacity = buffer.capacity();
        final int remaining = buffer.remaining();
        final int len = buffer1.remaining();

        // buffer1 will be appended to buffer
        if (len < (capacity - limit)) {

            buffer.mark();
            buffer.position(limit);
            buffer.limit(capacity);
            buffer.put(buffer1);
            buffer.limit(limit + len);
            buffer.reset();
            return buffer;
            // Remaining data is moved to left. Then new data is appended
        } else if (remaining + len < capacity) {
            buffer.compact();
            buffer.put(buffer1);
            buffer.flip();
            return buffer;
            // create new buffer
        } else {
            int newSize = remaining + len;
            if (newSize > incomingBufferSize) {
                throw new IllegalArgumentException("Buffer overflow");
            } else {
                final int roundedSize =
                        (newSize % BUFFER_STEP_SIZE) > 0 ? ((newSize / BUFFER_STEP_SIZE) + 1) * BUFFER_STEP_SIZE : newSize;
                final ByteBuffer result = ByteBuffer.allocate(roundedSize > incomingBufferSize ? newSize : roundedSize);
                result.put(buffer);
                result.put(buffer1);
                result.flip();
                return result;
            }
        }
    }

    static ByteBuffer split(ByteBuffer buffer, int position) {
        int bytesLength = position - buffer.position();
        byte[] bytes = new byte[bytesLength];
        buffer.get(bytes);
        return ByteBuffer.wrap(bytes);
    }

    static int getPort(URI uri) {
        if (uri.getPort() != -1) {
            return uri.getPort();
        }

        if ("https".equals(uri.getScheme())) {
            return 443;
        }

        return 80;
    }
}
