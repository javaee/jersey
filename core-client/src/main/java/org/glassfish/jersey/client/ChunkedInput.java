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
package org.glassfish.jersey.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.ReaderInterceptor;

import org.glassfish.jersey.client.internal.LocalizationMessages;
import org.glassfish.jersey.internal.PropertiesDelegate;
import org.glassfish.jersey.message.MessageBodyWorkers;

/**
 * Response entity type used for receiving messages in "typed" chunks.
 * <p/>
 * This data type is useful for consuming partial responses from large or continuous data
 * input streams.
 *
 * @param <T> chunk type.
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
@SuppressWarnings("UnusedDeclaration")
public class ChunkedInput<T> extends GenericType<T> implements Closeable {

    private static final Logger LOGGER = Logger.getLogger(ChunkedInput.class.getName());

    private final AtomicBoolean closed = new AtomicBoolean(false);
    private ChunkParser parser = createParser("\r\n");
    private MediaType mediaType;

    private final InputStream inputStream;
    private final Annotation[] annotations;
    private final MultivaluedMap<String, String> headers;
    private final MessageBodyWorkers messageBodyWorkers;
    private final PropertiesDelegate propertiesDelegate;

    /**
     * Create new chunk parser that will split the response entity input stream
     * based on a fixed boundary string.
     *
     * @param boundary chunk boundary.
     * @return new fixed boundary string-based chunk parser.
     */
    public static ChunkParser createParser(final String boundary) {
        return new FixedBoundaryParser(boundary.getBytes());
    }

    /**
     * Create new chunk parser that will split the response entity input stream
     * based on a fixed boundary sequence of bytes.
     *
     * @param boundary chunk boundary.
     * @return new fixed boundary sequence-based chunk parser.
     */
    public static ChunkParser createParser(final byte[] boundary) {
        return new FixedBoundaryParser(boundary);
    }

    /**
     * Create a new chunk multi-parser that will split the response entity input stream
     * based on multiple fixed boundary strings.
     *
     * @param boundaries chunk boundaries.
     * @return new fixed boundary string-based chunk parser.
     */
    public static ChunkParser createMultiParser(final String... boundaries) {
        return new FixedMultiBoundaryParser(boundaries);
    }

    private abstract static class AbstractBoundaryParser implements ChunkParser {

        @Override
        public byte[] readChunk(final InputStream in) throws IOException {
            final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] delimiterBuffer = new byte[getDelimiterBufferSize()];

            int data;
            int dPos;
            do {
                dPos = 0;
                while ((data = in.read()) != -1) {
                    final byte b = (byte) data;
                    byte[] delimiter = getDelimiter(b, dPos, delimiterBuffer);

                    // last read byte is part of the chunk delimiter
                    if (delimiter != null && b == delimiter[dPos]) {
                        delimiterBuffer[dPos++] = b;
                        if (dPos == delimiter.length) {
                            // found chunk delimiter
                            break;
                        }
                    } else if (dPos > 0) {
                        delimiter = getDelimiter(dPos - 1, delimiterBuffer);
                        delimiterBuffer[dPos] = b;

                        int matched = matchTail(delimiterBuffer, 1, dPos, delimiter);
                        if (matched == 0) {
                            // flush delimiter buffer
                            buffer.write(delimiterBuffer, 0, dPos);
                            buffer.write(b);
                            dPos = 0;
                        } else if (matched == delimiter.length) {
                            // found chunk delimiter
                            break;
                        } else {
                            // one or more elements of a previous buffered delimiter
                            // are parts of a current buffered delimiter
                            buffer.write(delimiterBuffer, 0, dPos + 1 - matched);
                            dPos = matched;
                        }
                    } else {
                        buffer.write(b);
                    }
                }

            } while (data != -1 && buffer.size() == 0); // skip an empty chunk

            if (dPos > 0 && dPos != getDelimiter(dPos - 1, delimiterBuffer).length) {
                // flush the delimiter buffer, if not empty - parsing finished in the middle of a potential delimiter sequence
                buffer.write(delimiterBuffer, 0, dPos);
            }

            return (buffer.size() > 0) ? buffer.toByteArray() : null;
        }

        /**
         * Selects a delimiter which corresponds to delimiter buffer. Method automatically appends {@code b} param on the
         * {@code pos} position of {@code delimiterBuffer} array and then starts the selection process with a newly created array.
         *
         * @param b               byte which will be added on the {@code pos} position of {@code delimiterBuffer} array
         * @param pos             number of bytes from the delimiter buffer which will be used in processing
         * @param delimiterBuffer current content of the delimiter buffer
         * @return delimiter which corresponds to delimiterBuffer
         */
        abstract byte[] getDelimiter(byte b, int pos, byte[] delimiterBuffer);

        /**
         * Selects a delimiter which corresponds to delimiter buffer.
         *
         * @param pos             position of the last read byte
         * @param delimiterBuffer number of bytes from the delimiter buffer which will be used in processing
         * @return delimiter which corresponds to delimiterBuffer
         */
        abstract byte[] getDelimiter(int pos, byte[] delimiterBuffer);

        /**
         * Returns a delimiter buffer size depending on the selected strategy.
         * <p>
         * If a strategy has multiple registered delimiters, then the delimiter buffer should be a length of the longest
         * delimiter.
         *
         * @return length of the delimiter buffer
         */
        abstract int getDelimiterBufferSize();

        /**
         * Tries to find an element intersection between two arrays in a way that intersecting elements must be
         * at the tail of the first array and at the beginning of the second array.
         * <p>
         * For example, consider the following two arrays:
         * <pre>
         * a1: {a, b, c, d, e}
         * a2: {d, e, f, g}
         * </pre>
         * In this example, the intersection of tail of {@code a1} with head of {@code a2} is <tt>{d, e}</tt>
         * and consists of 2 overlapping elements.
         * </p>
         * The method takes the first array represented as a sub-array in buffer demarcated by an offset and length.
         * The second array is a fixed pattern to be matched. The method then compares the tail of the
         * array in the buffer with the head of the pattern and returns the number of intersecting elements,
         * or zero in case the two arrays do not intersect tail to head.
         *
         * @param buffer  byte buffer containing the array whose tail to intersect.
         * @param offset  start of the array to be tail-matched in the {@code buffer}.
         * @param length  length of the array to be tail-matched.
         * @param pattern pattern to be head-matched.
         * @return {@code 0} if any part of the tail of the array in the buffer does not match
         * any part of the head of the pattern, otherwise returns number of overlapping elements.
         */
        private static int matchTail(byte[] buffer, int offset, int length, byte[] pattern) {
            if (pattern == null) {
                return 0;
            }

            outer:
            for (int i = 0; i < length; i++) {
                final int tailLength = length - i;
                for (int j = 0; j < tailLength; j++) {
                    if (buffer[offset + i + j] != pattern[j]) {
                        // mismatch - continue with shorter tail
                        continue outer;
                    }
                }

                // found the longest matching tail
                return tailLength;
            }
            return 0;
        }
    }

    private static class FixedBoundaryParser extends AbstractBoundaryParser {

        private final byte[] delimiter;

        public FixedBoundaryParser(final byte[] boundary) {
            delimiter = Arrays.copyOf(boundary, boundary.length);
        }

        @Override
        byte[] getDelimiter(byte b, int pos, byte[] delimiterBuffer) {
            return delimiter;
        }

        @Override
        byte[] getDelimiter(int pos, byte[] delimiterBuffer) {
            return delimiter;
        }

        @Override
        int getDelimiterBufferSize() {
            return delimiter.length;
        }
    }

    private static class FixedMultiBoundaryParser extends AbstractBoundaryParser {

        private final List<byte[]> delimiters = new ArrayList<byte[]>();

        private final int longestDelimiterLength;

        public FixedMultiBoundaryParser(String... boundaries) {
            for (String boundary: boundaries) {
                byte[] boundaryBytes = boundary.getBytes();
                delimiters.add(Arrays.copyOf(boundaryBytes, boundaryBytes.length));
            }

            Collections.sort(delimiters, new Comparator<byte[]>() {
                @Override
                public int compare(byte[] o1, byte[] o2) {
                    return Integer.compare(o1.length, o2.length);
                }
            });

            byte[] longestDelimiter = delimiters.get(delimiters.size() - 1);
            this.longestDelimiterLength = longestDelimiter.length;
        }

        @Override
        byte[] getDelimiter(byte b, int pos, byte[] delimiterBuffer) {
            byte[] buffer = Arrays.copyOf(delimiterBuffer, delimiterBuffer.length);
            buffer[pos] = b;

            return getDelimiter(pos, buffer);
        }

        @Override
        byte[] getDelimiter(int pos, byte[] delimiterBuffer) {
            outer:
            for (byte[] delimiter: delimiters) {
                if (pos > delimiter.length) {
                    continue;
                }

                for (int i = 0; i <= pos && i < delimiter.length; i++) {
                    if (delimiter[i] != delimiterBuffer[i]) {
                        continue outer;
                    } else if (pos == i) {
                        return delimiter;
                    }
                }
            }

            return null;
        }

        @Override
        int getDelimiterBufferSize() {
            return this.longestDelimiterLength;
        }
    }

    /**
     * Package-private constructor used by the {@link ChunkedInputReader}.
     *
     * @param chunkType          chunk type.
     * @param inputStream        response input stream.
     * @param annotations        annotations associated with response entity.
     * @param mediaType          response entity media type.
     * @param headers            response headers.
     * @param messageBodyWorkers message body workers.
     * @param propertiesDelegate properties delegate for this request/response.
     */
    protected ChunkedInput(
            final Type chunkType,
            final InputStream inputStream,
            final Annotation[] annotations,
            final MediaType mediaType,
            final MultivaluedMap<String, String> headers,
            final MessageBodyWorkers messageBodyWorkers,
            final PropertiesDelegate propertiesDelegate) {
        super(chunkType);

        this.inputStream = inputStream;
        this.annotations = annotations;
        this.mediaType = mediaType;
        this.headers = headers;
        this.messageBodyWorkers = messageBodyWorkers;
        this.propertiesDelegate = propertiesDelegate;
    }

    /**
     * Get the underlying chunk parser.
     * <p>
     * Note: Access to internal chunk parser is not a thread-safe operation and has to be explicitly synchronized
     * in case the chunked input is used from multiple threads.
     * </p>
     *
     * @return underlying chunk parser.
     */
    public ChunkParser getParser() {
        return parser;
    }

    /**
     * Set new chunk parser.
     * <p>
     * Note: Access to internal chunk parser is not a thread-safe operation and has to be explicitly synchronized
     * in case the chunked input is used from multiple threads.
     * </p>
     *
     * @param parser new chunk parser.
     */
    public void setParser(final ChunkParser parser) {
        this.parser = parser;
    }

    /**
     * Get chunk data media type.
     * <p/>
     * Default chunk data media type is derived from the value of the response
     * <tt>{@value javax.ws.rs.core.HttpHeaders#CONTENT_TYPE}</tt> header field.
     * This default value may be manually overridden by {@link #setChunkType(javax.ws.rs.core.MediaType) setting}
     * a custom non-{@code null} chunk media type value.
     * <p>
     * Note: Access to internal chunk media type is not a thread-safe operation and has to
     * be explicitly synchronized in case the chunked input is used from multiple threads.
     * </p>
     *
     * @return media type specific to each chunk of data.
     */
    public MediaType getChunkType() {
        return mediaType;
    }

    /**
     * Set custom chunk data media type.
     * <p/>
     * By default, chunk data media type is derived from the value of the response
     * <tt>{@value javax.ws.rs.core.HttpHeaders#CONTENT_TYPE}</tt> header field.
     * Using this methods will override the default chunk media type value and set it
     * to a custom non-{@code null} chunk media type. Once this method is invoked,
     * all subsequent {@link #read chunk reads} will use the newly set chunk media
     * type when selecting the proper {@link javax.ws.rs.ext.MessageBodyReader} for
     * chunk de-serialization.
     * <p>
     * Note: Access to internal chunk media type is not a thread-safe operation and has to
     * be explicitly synchronized in case the chunked input is used from multiple threads.
     * </p>
     *
     * @param mediaType custom chunk data media type. Must not be {@code null}.
     * @throws IllegalArgumentException in case the {@code mediaType} is {@code null}.
     */
    public void setChunkType(final MediaType mediaType) throws IllegalArgumentException {
        if (mediaType == null) {
            throw new IllegalArgumentException(LocalizationMessages.CHUNKED_INPUT_MEDIA_TYPE_NULL());
        }
        this.mediaType = mediaType;
    }

    /**
     * Set custom chunk data media type from a string value.
     * <p>
     * Note: Access to internal chunk media type is not a thread-safe operation and has to
     * be explicitly synchronized in case the chunked input is used from multiple threads.
     * </p>
     *
     * @param mediaType custom chunk data media type. Must not be {@code null}.
     * @throws IllegalArgumentException in case the {@code mediaType} cannot be parsed into
     *                                  a valid {@link MediaType} instance or is {@code null}.
     * @see #setChunkType(javax.ws.rs.core.MediaType)
     */
    public void setChunkType(final String mediaType) throws IllegalArgumentException {
        this.mediaType = MediaType.valueOf(mediaType);
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (final IOException e) {
                    LOGGER.log(Level.FINE, LocalizationMessages.CHUNKED_INPUT_STREAM_CLOSING_ERROR(), e);
                }
            }
        }
    }

    /**
     * Check if the chunked input has been closed.
     *
     * @return {@code true} if this chunked input has been closed, {@code false} otherwise.
     */
    public boolean isClosed() {
        return closed.get();
    }

    /**
     * Read next chunk from the response stream and convert it to a Java instance
     * using the {@link #getChunkType() chunk media type}. The method returns {@code null}
     * if the underlying entity input stream has been closed (either implicitly or explicitly
     * by calling the {@link #close()} method).
     * <p>
     * Note: Access to internal chunk parser is not a thread-safe operation and has to be explicitly
     * synchronized in case the chunked input is used from multiple threads.
     * </p>
     *
     * @return next streamed chunk or {@code null} if the underlying entity input stream
     * has been closed while reading next chunk data.
     * @throws IllegalStateException in case this chunked input has been closed.
     */
    @SuppressWarnings("unchecked")
    public T read() throws IllegalStateException {
        if (closed.get()) {
            throw new IllegalStateException(LocalizationMessages.CHUNKED_INPUT_CLOSED());
        }

        try {
            final byte[] chunk = parser.readChunk(inputStream);
            if (chunk == null) {
                close();
            } else {
                final ByteArrayInputStream chunkStream = new ByteArrayInputStream(chunk);
                // TODO: add interceptors: interceptors are used in ChunkedOutput, so the stream should
                // be intercepted in the ChunkedInput too. Interceptors cannot be easily added to the readFrom
                // method as they should wrap the stream before it is processed by ChunkParser. Also please check todo
                // in ChunkedInput (this should be fixed together with this todo)
                // issue: JERSEY-1809
                return (T) messageBodyWorkers.readFrom(
                        getRawType(),
                        getType(),
                        annotations,
                        mediaType,
                        headers,
                        propertiesDelegate,
                        chunkStream,
                        Collections.<ReaderInterceptor>emptyList(),
                        false);
            }
        } catch (final IOException e) {
            Logger.getLogger(this.getClass().getName()).log(Level.FINE, e.getMessage(), e);
            close();
        }
        return null;
    }
}
