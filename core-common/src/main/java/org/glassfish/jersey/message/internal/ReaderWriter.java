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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.security.AccessController;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.internal.LocalizationMessages;
import org.glassfish.jersey.internal.util.PropertiesHelper;
import org.glassfish.jersey.message.MessageProperties;

/**
 * A utility class for reading and writing using byte and character streams.
 * <p>
 * If a byte or character array is utilized then the size of the array
 * is by default the value of {@value org.glassfish.jersey.message.MessageProperties#IO_DEFAULT_BUFFER_SIZE}.
 * This value can be set using the system property
 * {@value org.glassfish.jersey.message.MessageProperties#IO_BUFFER_SIZE}.
 *
 * @author Paul Sandoz
 */
public final class ReaderWriter {

    private static final Logger LOGGER = Logger.getLogger(ReaderWriter.class.getName());
    /**
     * The UTF-8 Charset.
     */
    public static final Charset UTF8 = Charset.forName("UTF-8");
    /**
     * The buffer size for arrays of byte and character.
     */
    public static final int BUFFER_SIZE = getBufferSize();

    private static int getBufferSize() {
        // TODO should we unify this buffer size and CommittingOutputStream buffer size (controlled by CommonProperties.OUTBOUND_CONTENT_LENGTH_BUFFER)?
        final String value = AccessController.doPrivileged(PropertiesHelper.getSystemProperty(MessageProperties.IO_BUFFER_SIZE));
        if (value != null) {
            try {
                final int i = Integer.parseInt(value);
                if (i <= 0) {
                    throw new NumberFormatException("Value not positive.");
                }
                return i;
            } catch (NumberFormatException e) {
                LOGGER.log(Level.CONFIG,
                        "Value of " + MessageProperties.IO_BUFFER_SIZE
                                + " property is not a valid positive integer [" + value + "]."
                                + " Reverting to default [" + MessageProperties.IO_DEFAULT_BUFFER_SIZE + "].",
                        e);
            }
        }
        return MessageProperties.IO_DEFAULT_BUFFER_SIZE;
    }

    /**
     * Read bytes from an input stream and write them to an output stream.
     *
     * @param in  the input stream to read from.
     * @param out the output stream to write to.
     * @throws IOException if there is an error reading or writing bytes.
     */
    public static void writeTo(InputStream in, OutputStream out) throws IOException {
        int read;
        final byte[] data = new byte[BUFFER_SIZE];
        while ((read = in.read(data)) != -1) {
            out.write(data, 0, read);
        }
    }

    /**
     * Read characters from an input stream and write them to an output stream.
     *
     * @param in  the reader to read from.
     * @param out the writer to write to.
     * @throws IOException if there is an error reading or writing characters.
     */
    public static void writeTo(Reader in, Writer out) throws IOException {
        int read;
        final char[] data = new char[BUFFER_SIZE];
        while ((read = in.read(data)) != -1) {
            out.write(data, 0, read);
        }
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
        String name = (m == null) ? null : m.getParameters().get(MediaType.CHARSET_PARAMETER);
        return (name == null) ? UTF8 : Charset.forName(name);
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
        return readFromAsString(new InputStreamReader(in, getCharset(type)));
    }

    /**
     * Read the characters of a reader and convert to a string.
     *
     * @param reader the reader
     * @return the string
     *
     * @throws IOException if there is an error reading from the reader.
     */
    public static String readFromAsString(Reader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        char[] c = new char[BUFFER_SIZE];
        int l;
        while ((l = reader.read(c)) != -1) {
            sb.append(c, 0, l);
        }
        return sb.toString();
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
        Writer osw = new OutputStreamWriter(out, getCharset(type));
        osw.write(s, 0, s.length());
        osw.flush();
    }

    /**
     * Safely close a closeable, without throwing an exception.
     *
     * @param closeable object to be closed.
     */
    public static void safelyClose(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException ioe) {
            LOGGER.log(Level.FINE, LocalizationMessages.MESSAGE_CONTENT_INPUT_STREAM_CLOSE_FAILED(), ioe);
        } catch (ProcessingException pe) {
            LOGGER.log(Level.FINE, LocalizationMessages.MESSAGE_CONTENT_INPUT_STREAM_CLOSE_FAILED(), pe);
        }
    }

    /**
     * Prevents instantiation.
     */
    private ReaderWriter() {
    }
}
