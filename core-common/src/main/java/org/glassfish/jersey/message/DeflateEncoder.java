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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.core.Configuration;

import javax.inject.Inject;

import org.glassfish.jersey.spi.ContentEncoder;

/**
 * Deflate encoding support. Interceptor that encodes the output or decodes the input if
 * {@link javax.ws.rs.core.HttpHeaders#CONTENT_ENCODING Content-Encoding header} value equals to {@code deflate}.
 * The default behavior of this interceptor can be tweaked using {@link MessageProperties#DEFLATE_WITHOUT_ZLIB}
 * property.
 *
 * @author Martin Matula
 */
@Priority(Priorities.ENTITY_CODER)
public class DeflateEncoder extends ContentEncoder {

    // TODO This provider should be registered and configured via a feature.
    private final Configuration config;

    /**
     * Initialize DeflateEncoder.
     *
     * @param config Jersey configuration properties.
     */
    @Inject
    public DeflateEncoder(final Configuration config) {
        super("deflate");
        this.config = config;
    }

    @Override
    public InputStream decode(String contentEncoding, InputStream encodedStream)
            throws IOException {
        // correct impl. should wrap deflate in zlib, but some don't do it - have to identify, which one we got
        InputStream markSupportingStream = encodedStream.markSupported() ? encodedStream
                : new BufferedInputStream(encodedStream);

        markSupportingStream.mark(1);
        // read the first byte
        int firstByte = markSupportingStream.read();
        markSupportingStream.reset();

        // if using zlib, first 3 bits should be 0, 4th should be 1
        // that should never be the case if no zlib wrapper
        if ((firstByte & 15) == 8) {
            // ok, zlib wrapped stream
            return new InflaterInputStream(markSupportingStream);
        } else {
            // no zlib wrapper
            return new InflaterInputStream(markSupportingStream, new Inflater(true));
        }
    }

    @Override
    public OutputStream encode(String contentEncoding, OutputStream entityStream)
            throws IOException {
        // some implementations don't support the correct deflate
        // so we have a property to configure the incorrect deflate (no zlib wrapper) should be used
        // let's check that
        Object value = config.getProperty(MessageProperties.DEFLATE_WITHOUT_ZLIB);
        boolean deflateWithoutZLib;
        if (value instanceof String) {
            deflateWithoutZLib = Boolean.valueOf((String) value);
        } else if (value instanceof Boolean) {
            deflateWithoutZLib = (Boolean) value;
        } else {
            deflateWithoutZLib = false;
        }

        return deflateWithoutZLib
                ? new DeflaterOutputStream(entityStream, new Deflater(Deflater.DEFAULT_COMPRESSION, true))
                : new DeflaterOutputStream(entityStream);
    }
}
