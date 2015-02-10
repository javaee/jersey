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

package org.glassfish.jersey.message;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * Super class for encoding tests - contains convenient way of defining the test by simply providing the encoding and
 * decoding streams.
 *
 * @author Martin Matula
 */
public class AbstractEncodingTest {
    /**
     * Main testing method that runs the test based on the passed test spec.
     *
     * @param testSpec Test-specific routines (providing encoding/decoding streams).
     * @throws IOException I/O exception.
     */
    protected void test(TestSpec testSpec) throws IOException {
        byte[] entity = "Hello world!".getBytes();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        OutputStream encoded = testSpec.getEncoded(baos);
        encoded.write(entity);
        encoded.close();
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        byte[] result = new byte[entity.length];
        InputStream decoded = testSpec.getDecoded(bais);
        int len = decoded.read(result);
        assertEquals(-1, decoded.read());
        decoded.close();
        assertEquals(entity.length, len);
        assertArrayEquals(entity, result);
    }

    /**
     * Interface that a test typically implements using an anonymous class to provide the test-specific functionality.
     */
    protected static interface TestSpec {
        /**
         * Returns encoded stream.
         *
         * @param stream Original stream.
         * @return Encoded stream.
         * @throws IOException I/O exception.
         */
        OutputStream getEncoded(OutputStream stream) throws IOException;

        /**
         * Returns decoded stream.
         *
         * @param stream Original stream.
         * @return Decoded stream.
         * @throws IOException I/O exception.
         */
        InputStream getDecoded(InputStream stream) throws IOException;
    }
}
