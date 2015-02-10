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

package org.glassfish.jersey.spi;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import javax.ws.rs.Priorities;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import javax.annotation.Priority;

import jersey.repackaged.com.google.common.collect.Sets;

/**
 * Standard contract for plugging in content encoding support. Provides a standard way of implementing encoding
 * {@link WriterInterceptor} and decoding {@link ReaderInterceptor}. Implementing this class ensures the encoding
 * supported by the implementation will be considered during the content negotiation phase when deciding which encoding
 * should be used based on the accepted encodings (and the associated quality parameters) in the request headers.
 *
 * @author Martin Matula
 */
@Priority(Priorities.ENTITY_CODER)
@Contract
public abstract class ContentEncoder implements ReaderInterceptor, WriterInterceptor {
    private final Set<String> supportedEncodings;

    /**
     * Initializes this encoder implementation with the list of supported content encodings.
     *
     * @param supportedEncodings Values of Content-Encoding header supported by this encoding provider.
     */
    protected ContentEncoder(String... supportedEncodings) {
        if (supportedEncodings.length == 0) {
            throw new IllegalArgumentException();
        }
        this.supportedEncodings = Collections.unmodifiableSet(Sets.newHashSet(Arrays.asList(supportedEncodings)));
    }

    /**
     * Returns values of Content-Encoding header supported by this encoder.
     * @return Set of supported Content-Encoding values.
     */
    public final Set<String> getSupportedEncodings() {
        return supportedEncodings;
    }

    /**
     * Implementations of this method should take the encoded stream, wrap it and return a stream that can be used
     * to read the decoded entity.
     *
     *
     * @param contentEncoding Encoding to be used to decode the stream - guaranteed to be one of the supported encoding
     *                        values.
     * @param encodedStream Encoded input stream.
     * @return Decoded entity stream.
     * @throws java.io.IOException if an IO error arises.
     */
    public abstract InputStream decode(String contentEncoding, InputStream encodedStream) throws IOException;

    /**
     * Implementations of this method should take the entity stream, wrap it and return a stream that is encoded
     * using the specified encoding.
     *
     *
     * @param contentEncoding Encoding to be used to encode the entity - guaranteed to be one of the supported encoding
     *                        values.
     * @param entityStream Entity stream to be encoded.
     * @return Encoded stream.
     * @throws java.io.IOException if an IO error arises.
     */
    public abstract OutputStream encode(String contentEncoding, OutputStream entityStream) throws IOException;

    @Override
    public final Object aroundReadFrom(ReaderInterceptorContext context) throws IOException, WebApplicationException {
        String contentEncoding = context.getHeaders().getFirst(HttpHeaders.CONTENT_ENCODING);
        if (contentEncoding != null && getSupportedEncodings().contains(contentEncoding)) {
            context.setInputStream(decode(contentEncoding, context.getInputStream()));
        }
        return context.proceed();
    }

    @Override
    public final void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
        // must remove Content-Length header since the encoded message will have a different length

        String contentEncoding = (String) context.getHeaders().getFirst(HttpHeaders.CONTENT_ENCODING);
        if (contentEncoding != null && getSupportedEncodings().contains(contentEncoding)) {
            context.setOutputStream(encode(contentEncoding, context.getOutputStream()));
        }
        context.proceed();
    }
}
