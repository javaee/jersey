/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.message.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

/**
 * Abstract entity provider (reader and writer) base class.
 *
 * @param <T> Java type supported by the provider
 * @author Paul Sandoz
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public abstract class AbstractMessageReaderWriterProvider<T> implements MessageBodyReader<T>, MessageBodyWriter<T> {

    // TODO: refactor away all constants & static wrappers of ReaderWriter methods and constants - those can be used directly.

    /**
     * The UTF-8 Charset.
     */
    public static final Charset UTF8 = ReaderWriter.UTF8;

    /**
     * Reader bytes from an input stream and write then to an output stream.
     *
     * @param in  the input stream to read from.
     * @param out the output stream to write to.
     * @throws IOException if there is an error reading or writing bytes.
     */
    public static void writeTo(InputStream in, OutputStream out) throws IOException {
        ReaderWriter.writeTo(in, out);
    }

    /**
     * Reader characters from an input stream and write then to an output stream.
     *
     * @param in  the reader to read from.
     * @param out the writer to write to.
     * @throws IOException if there is an error reading or writing characters.
     */
    public static void writeTo(Reader in, Writer out) throws IOException {
        ReaderWriter.writeTo(in, out);
    }

    /**
     * Get the character set from a media type.
     * <p>
     * The character set is obtained from the media type parameter "charset".
     * If the parameter is not present the {@link #UTF8} charset is utilized.
     *
     * @param m the media type.
     * @return the character set.
     */
    public static Charset getCharset(MediaType m) {
        return ReaderWriter.getCharset(m);
    }

    /**
     * Read the bytes of an input stream and convert to a string.
     *
     * @param in   the input stream to read from.
     * @param type the media type that determines the character set defining
     *             how to decode bytes to characters.
     * @return the string.
     *
     * @throws IOException if there is an error reading from the input stream.
     */
    public static String readFromAsString(InputStream in, MediaType type) throws IOException {
        return ReaderWriter.readFromAsString(in, type);
    }

    /**
     * Convert a string to bytes and write those bytes to an output stream.
     *
     * @param s    the string to convert to bytes.
     * @param out  the output stream to write to.
     * @param type the media type that determines the character set defining
     *             how to decode bytes to characters.
     * @throws IOException in case of a write failure.
     */
    public static void writeToAsString(String s, OutputStream out, MediaType type) throws IOException {
        ReaderWriter.writeToAsString(s, out, type);
    }

    // MessageBodyWriter
    @Override
    public long getSize(T t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }
}
