/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.jdk.connector;

import java.nio.ByteBuffer;

import static org.glassfish.jersey.jdk.connector.HttpParserUtils.isSpaceOrTab;
import static org.glassfish.jersey.jdk.connector.HttpParserUtils.skipSpaces;

/**
 * @author Alexey Stashok
 * @author Petr Janouch (petr.janouch at oracle.com)
 */
abstract class TransferEncodingParser {

    abstract boolean parse(ByteBuffer input) throws ParseException;

    static TransferEncodingParser createFixedLengthParser(AsynchronousBodyInputStream responseBody, int expectedLength) {
        return new FixedLengthEncodingParser(responseBody, expectedLength);
    }

    static TransferEncodingParser createChunkParser(AsynchronousBodyInputStream responseBody,
                                                    HttpParser httpParser, int maxHeadersSize) {
        return new ChunkedEncodingParser(responseBody, httpParser, maxHeadersSize);
    }

    private static class FixedLengthEncodingParser extends TransferEncodingParser {

        private final int expectedLength;
        private final AsynchronousBodyInputStream responseBody;
        private volatile int consumedLength = 0;

        FixedLengthEncodingParser(AsynchronousBodyInputStream responseBody, int expectedLength) {
            this.expectedLength = expectedLength;
            this.responseBody = responseBody;
        }

        @Override
        boolean parse(ByteBuffer input) throws ParseException {
            if (input.remaining() + consumedLength > expectedLength) {
                throw new ParseException(LocalizationMessages.HTTP_BODY_SIZE_OVERFLOW());
            }

            byte[] data = new byte[input.remaining()];
            input.get(data);
            ByteBuffer parsed = ByteBuffer.wrap(data);
            responseBody.notifyDataAvailable(parsed);
            consumedLength += data.length;

            return consumedLength == expectedLength;
        }
    }

    private static class ChunkedEncodingParser extends TransferEncodingParser {

        private static final int MAX_HTTP_CHUNK_SIZE_LENGTH = 16;
        private static final long CHUNK_SIZE_OVERFLOW = Long.MAX_VALUE >> 4;

        private static final int CHUNK_LENGTH_PARSED_STATE = 3;

        private static final int[] DEC = {
                -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
                -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
                -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
                00, 01, 02, 03, 04, 05, 06, 07, 8, 9, -1, -1, -1, -1, -1, -1,
                -1, 10, 11, 12, 13, 14, 15, -1, -1, -1, -1, -1, -1, -1, -1, -1,
                -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
                -1, 10, 11, 12, 13, 14, 15, -1, -1, -1, -1, -1, -1, -1, -1, -1,
                -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
                -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
                -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
                -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
                -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
                -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
                -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
                -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
                -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        };

        private final HttpParserUtils.ContentParsingState contentParsingState = new HttpParserUtils
                .ContentParsingState();
        private final HttpParserUtils.HeaderParsingState headerParsingState;
        private final AsynchronousBodyInputStream responseBody;
        private final HttpParser httpParser;
        private final int maxHeadersSize;

        ChunkedEncodingParser(AsynchronousBodyInputStream responseBody, HttpParser httpParser, int maxHeadersSize) {
            this.responseBody = responseBody;
            this.httpParser = httpParser;
            this.headerParsingState = httpParser.getHeaderParsingState();
            this.maxHeadersSize = maxHeadersSize;
        }

        // Taken with small modifications from Grizzly ChunkedTransferEncoding.parsePacket
        @Override
        boolean parse(ByteBuffer input) throws ParseException {

            while (input.hasRemaining()) {

                boolean isLastChunk = contentParsingState.isLastChunk;
                // Check if HTTP chunk length was parsed
                if (!isLastChunk && contentParsingState.chunkRemainder <= 0) {
                    if (!parseTrailerCRLF(input)) {
                        return false;
                    }

                    if (!parseHttpChunkLength(input)) {
                        // if not a HEAD request and we don't have enough data to
                        // parse chunk length - shutdownNow execution
                        return false;
                    }
                } else {
                    // HTTP content starts from position 0 in the input Buffer (HTTP chunk header is not part of the input Buffer)
                    //contentParsingState.chunkContentStart = 0;
                    contentParsingState.chunkContentStart = input.position();
                }

                // Get the position in the input Buffer, where actual HTTP content starts
                int chunkContentStart = contentParsingState.chunkContentStart;

                if (contentParsingState.chunkLength == 0) {
                    // if it's the last HTTP chunk
                    if (!isLastChunk) {
                        // set it's the last chunk
                        contentParsingState.isLastChunk = true;
                        isLastChunk = true;
                        // start trailer parsing
                        initTrailerParsing();
                    }

                    // Check if trailer is present
                    if (!parseLastChunkTrailer(input)) {
                        // if yes - and there is not enough input data - shutdownNow the
                        // filterchain processing
                        return false;
                    }

                    // move the content start position after trailer parsing
                    chunkContentStart = headerParsingState.offset;
                }

                if (isLastChunk) {
                    input.position(chunkContentStart);
                    return true;
                }

                // Get the number of bytes remaining in the current chunk
                final long thisPacketRemaining = contentParsingState.chunkRemainder;
                // Get the number of content bytes available in the current input Buffer
                final int contentAvailable = input.limit() - chunkContentStart;

                input.position(chunkContentStart);
                ByteBuffer data;
                if (contentAvailable > thisPacketRemaining) {
                    // If input Buffer has part of the next message - slice it
                    data = Utils.split(input, (int) (chunkContentStart + thisPacketRemaining));

                } else {
                    data = Utils.split(input, chunkContentStart + input.remaining());
                }

                contentParsingState.chunkRemainder -= data.remaining();
                responseBody.notifyDataAvailable(data);
            }

            return false;
        }

        // Taken with small modifications from Grizzly ChunkedTransferEncoding.parseHttpChunkLength
        private boolean parseHttpChunkLength(final ByteBuffer input) throws ParseException {
            while (true) {
                switch (headerParsingState.state) {
                    case 0: {// Initialize chunk parsing
                        final int pos = input.position();
                        headerParsingState.start = pos;
                        headerParsingState.offset = pos;
                        headerParsingState.packetLimit = pos + MAX_HTTP_CHUNK_SIZE_LENGTH;
                        headerParsingState.state = 1;
                        break;
                    }

                    case 1: { // Skip heading spaces (it's not allowed by the spec, but some servers put it there)
                        final int nonSpaceIdx = skipSpaces(input,
                                headerParsingState.offset, headerParsingState.packetLimit);
                        if (nonSpaceIdx == -1) {
                            headerParsingState.offset = input.limit();
                            headerParsingState.state = 1;

                            headerParsingState.checkOverflow(LocalizationMessages.HTTP_CHUNK_ENCODING_PREFIX_OVERFLOW());
                            return false;
                        }

                        headerParsingState.offset = nonSpaceIdx;
                        headerParsingState.state = 2;
                        break;
                    }

                    case 2: { // Scan chunk size
                        int offset = headerParsingState.offset;
                        int limit = Math.min(headerParsingState.packetLimit, input.limit());
                        long value = headerParsingState.parsingNumericValue;

                        while (offset < limit) {
                            final byte b = input.get(offset);
                            if (isSpaceOrTab(b) || /*trailing spaces are not allowed by the spec, but some server put it there*/
                                    b == HttpParserUtils.CR || b == HttpParserUtils.SEMI_COLON) {
                                headerParsingState.checkpoint = offset;
                            } else if (b == HttpParserUtils.LF) {
                                contentParsingState.chunkContentStart = offset + 1;
                                contentParsingState.chunkLength = value;
                                contentParsingState.chunkRemainder = value;

                                headerParsingState.state = CHUNK_LENGTH_PARSED_STATE;

                                return true;
                            } else if (headerParsingState.checkpoint == -1) {
                                if (DEC[b & 0xFF] != -1 && checkOverflow(value)) {
                                    value = (value << 4) + (DEC[b & 0xFF]);
                                } else {
                                    throw new ParseException(
                                            LocalizationMessages.HTTP_INVALID_CHUNK_SIZE_HEX_VALUE(b));
                                }
                            } else {
                                throw new ParseException(LocalizationMessages.HTTP_UNEXPECTED_CHUNK_HEADER());
                            }

                            offset++;
                        }

                        headerParsingState.parsingNumericValue = value;
                        headerParsingState.offset = offset;
                        headerParsingState.checkOverflow(LocalizationMessages.HTTP_CHUNK_ENCODING_PREFIX_OVERFLOW());
                        return false;
                    }
                }
            }
        }

        // Taken with small modifications from Grizzly ChunkedTransferEncoding.parseTrailerCRLF
        private boolean parseTrailerCRLF(ByteBuffer input) {
            if (headerParsingState.state == CHUNK_LENGTH_PARSED_STATE) {
                while (input.hasRemaining()) {
                    if (input.get() == HttpParserUtils.LF) {
                        headerParsingState.recycle();
                        return input.hasRemaining();
                    }
                }

                return false;
            }

            return true;
        }

        /**
         * @return <tt>false</tt> if next left bit-shift by 4 bits will cause overflow,
         * or <tt>true</tt> otherwise
         */
        private boolean checkOverflow(final long chunkLength) {
            return chunkLength <= CHUNK_SIZE_OVERFLOW;
        }

        private void initTrailerParsing() {
            headerParsingState.subState = 0;
            final int start = contentParsingState.chunkContentStart;
            headerParsingState.start = start;
            headerParsingState.offset = start;
            headerParsingState.packetLimit = start + maxHeadersSize;
        }

        // Taken with small modifications from Grizzly ChunkedTransferEncoding.parseLastChunkTrailer
        private boolean parseLastChunkTrailer(final ByteBuffer input) throws ParseException {
            boolean result = httpParser.parseHeadersFromBuffer(input, true);
            if (!result) {
                headerParsingState.checkOverflow(LocalizationMessages.HTTP_TRAILER_HEADER_OVERFLOW());
            }

            return result;
        }
    }
}
