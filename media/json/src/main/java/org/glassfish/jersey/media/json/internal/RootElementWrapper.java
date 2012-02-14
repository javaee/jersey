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
package org.glassfish.jersey.media.json.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.io.UnsupportedEncodingException;

/**
 * Collection of root element wrapping/unwrapping utility methods.
 *
 * @author Jakub Podlesak
 */
public class RootElementWrapper {

    /**
     * Prevents instantiation.
     */
    private RootElementWrapper() {
    }

    /**
     * Wrap input into a root element with a given name.
     *
     * @param inputStream input stream.
     * @param rootName root element name.
     * @return wrapping input stream.
     * @throws UnsupportedEncodingException in case the input stream encoding is not supported.
     */
    public static InputStream wrapInput(InputStream inputStream, String rootName) throws UnsupportedEncodingException {
        SequenceInputStream sis = new SequenceInputStream(new ByteArrayInputStream(String.format("{\"%s\":", rootName).getBytes("UTF-8")), inputStream);
        return new SequenceInputStream(sis, new ByteArrayInputStream("}".getBytes("UTF-8")));
    }

    /**
     * Un-wrap input by discarding the wrapping root element.
     *
     * @param inputStream wrapped input stream.
     * @return un-wrapped input stream.
     * @throws IOException in case of I/O error.
     */
    public static InputStream unwrapInput(InputStream inputStream) throws IOException {
        return new JsonRootEatingInputStreamFilter(inputStream);
    }

    /**
     * Un-wrap output by discarding the wrapping root element.
     *
     * @param outputStream wrapped output stream.
     * @return un-wrapped output stream.
     * @throws IOException in case of I/O error.
     */
    public static OutputStream unwrapOutput(OutputStream outputStream) throws IOException {
        throw new UnsupportedOperationException("to be implemented yet");
    }

    /**
     * Wrap output into a root element with a given name.
     *
     * @param outputStream output stream.
     * @param rootName root element name.
     * @return wrapping output stream.
     * @throws IOException in case of I/O error.
     */
    public static OutputStream wrapOutput(OutputStream outputStream, String rootName) throws IOException {
        throw new UnsupportedOperationException("to be implemented yet");
    }
}
