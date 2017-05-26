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

package org.glassfish.jersey.jdk.connector.internal;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.List;

import javax.ws.rs.core.HttpHeaders;

/**
 * @author Alexey Stashok
 * @author Petr Janouch (petr.janouch at oracle.com)
 */
class HttpParser {

    private static final String ENCODING = "ISO-8859-1";

    private static final int BUFFER_STEP_SIZE = 256;
    // this is package private because of the test
    static final int INIT_BUFFER_SIZE = 1024;

    private final HttpParserUtils.HeaderParsingState headerParsingState;
    private final int bufferMaxSize;
    private final int maxHeaderSize;

    private volatile ByteBuffer buffer = ByteBuffer.allocate(INIT_BUFFER_SIZE);
    private volatile boolean headerParsed;
    private volatile boolean expectContent;
    private volatile String protocolVersion;
    private volatile int code;

    private volatile HttpResponse httpResponse;
    private volatile TransferEncodingParser transferEncodingParser;
    private volatile boolean complete;

    HttpParser(int maxHeaderSize, int bufferMaxSize) {
        headerParsingState = new HttpParserUtils.HeaderParsingState(maxHeaderSize);
        this.bufferMaxSize = bufferMaxSize;
        this.maxHeaderSize = maxHeaderSize;
    }

    void reset(boolean expectContent) {
        this.expectContent = expectContent;
        headerParsed = false;
        buffer.clear();
        buffer.flip();
        complete = false;
        headerParsingState.recycle();
    }

    boolean isHeaderParsed() {
        return headerParsed;
    }

    boolean isComplete() {
        return complete;
    }

    HttpResponse getHttpResponse() {
        return httpResponse;
    }

    void parse(ByteBuffer input) throws ParseException {
        if (buffer.remaining() > 0) {
            input = Utils.appendBuffers(buffer, input, bufferMaxSize, BUFFER_STEP_SIZE);
        }

        if (!headerParsed && !parseHeader(input)) {
            saveRemaining(input);
            return;
        }

        httpResponse.setHasContent(expectContent);
        if (expectContent) {
            if (transferEncodingParser.parse(input)) {
                complete = true;
            } else {
                saveRemaining(input);
            }
        } else {
            // We don't expect content
            complete = true;
        }

        if (complete && input.hasRemaining()) {
            throw new ParseException(LocalizationMessages.UNEXPECTED_DATA_IN_BUFFER());
        }

        if (complete) {
            httpResponse.getBodyStream().notifyAllDataRead();
        }
    }

    private void saveRemaining(ByteBuffer input) {

        // some of the fields use 0 nad -1 as a special state -> if the field is <= 0, just let it be
        headerParsingState.start =
                headerParsingState.start > 0 ? headerParsingState.start - input.position() : headerParsingState.start;
        headerParsingState.offset =
                headerParsingState.offset > 0 ? headerParsingState.offset - input.position() : headerParsingState.offset;
        headerParsingState.packetLimit = headerParsingState.packetLimit > 0 ? headerParsingState.packetLimit - input.position()
                : headerParsingState.packetLimit;
        headerParsingState.checkpoint = headerParsingState.checkpoint > 0 ? headerParsingState.checkpoint - input.position()
                : headerParsingState.checkpoint;
        headerParsingState.checkpoint2 = headerParsingState.checkpoint2 > 0 ? headerParsingState.checkpoint2 - input.position()
                : headerParsingState.checkpoint2;

        if (input.hasRemaining()) {
            if (input != buffer) {
                buffer.clear();
                buffer.flip();
                buffer = Utils.appendBuffers(buffer, input, bufferMaxSize, BUFFER_STEP_SIZE);
            } else {
                buffer.compact();
                buffer.flip();
            }
        }
    }

    // Taken with small modifications from Grizzly HttpCodecFilter.parseHeaderFromBuffer
    // (change: operations in phase 2 are translated to fit this parser)
    private boolean parseHeader(ByteBuffer input) throws ParseException {

        while (true) {
            switch (headerParsingState.state) {
                case 0: { // parsing initial line
                    if (!decodeInitialLineFromBuffer(input)) {
                        headerParsingState.checkOverflow(LocalizationMessages.HTTP_INITIAL_LINE_OVERFLOW());
                        return false;
                    }

                    headerParsingState.state++;
                    break;
                }

                case 1: { // parsing headers
                    if (!parseHeadersFromBuffer(input, false)) {
                        headerParsingState.checkOverflow(LocalizationMessages.HTTP_PACKET_HEADER_OVERFLOW());
                        return false;
                    }

                    headerParsingState.state++;
                    break;
                }

                case 2: { // Headers are ready
                    input.position(headerParsingState.offset);
                    // if headers get parsed - set the flag
                    headerParsed = true;
                    decideTransferEncoding();

                    // recycle header parsing state
                    headerParsingState.recycle();
                    return true;
                }

                default:
                    throw new IllegalStateException();
            }
        }
    }

    // Taken unmodified from Grizzly HttpClientFilter.decodeInitialLineFromBuffer
    private boolean decodeInitialLineFromBuffer(final ByteBuffer input) throws ParseException {

        final int packetLimit = headerParsingState.packetLimit;

        //noinspection LoopStatementThatDoesntLoop
        while (true) {
            int subState = headerParsingState.subState;

            switch (subState) {
                case 0: { // HTTP protocol
                    final int spaceIdx =
                            findSpace(input, headerParsingState.offset, packetLimit);
                    if (spaceIdx == -1) {
                        headerParsingState.offset = input.limit();
                        return false;
                    }

                    protocolVersion = parseString(input, headerParsingState.start, spaceIdx);

                    headerParsingState.start = -1;
                    headerParsingState.offset = spaceIdx;

                    headerParsingState.subState++;
                    break;
                }

                case 1: { // skip spaces after the HTTP protocol
                    final int nonSpaceIdx =
                            HttpParserUtils.skipSpaces(input, headerParsingState.offset, packetLimit);
                    if (nonSpaceIdx == -1) {
                        headerParsingState.offset = input.limit();
                        return false;
                    }

                    headerParsingState.start = nonSpaceIdx;
                    headerParsingState.offset = nonSpaceIdx + 1;
                    headerParsingState.subState++;
                    break;
                }

                case 2: { // parse the status code
                    if (headerParsingState.offset + 3 > input.limit()) {
                        return false;
                    }

                    code = parseInt(input, headerParsingState.start, headerParsingState.start + 3);

                    headerParsingState.start = -1;
                    headerParsingState.offset += 3;
                    headerParsingState.subState++;
                    break;
                }

                case 3: { // skip spaces after the status code
                    final int nonSpaceIdx =
                            HttpParserUtils.skipSpaces(input, headerParsingState.offset, packetLimit);
                    if (nonSpaceIdx == -1) {
                        headerParsingState.offset = input.limit();
                        return false;
                    }

                    headerParsingState.start = nonSpaceIdx;
                    headerParsingState.offset = nonSpaceIdx;
                    headerParsingState.subState++;
                    break;
                }

                case 4: { // HTTP response reason-phrase
                    if (!findEOL(input)) {
                        headerParsingState.offset = input.limit();
                        return false;
                    }

                    String reasonPhrase = parseString(input, headerParsingState.start, headerParsingState.checkpoint);

                    headerParsingState.subState = 0;
                    headerParsingState.start = -1;
                    headerParsingState.checkpoint = -1;
                    httpResponse = new HttpResponse(protocolVersion, code, reasonPhrase);

                    if (httpResponse.getStatusCode() == 100) {
                        // reset the parsing state in preparation to parse
                        // another initial line which represents the final
                        // response from the server after it has sent a
                        // 100-Continue.
                        headerParsingState.offset += 2;
                        headerParsingState.start = 0;
                        input.position(headerParsingState.offset);
                        input.compact();
                        headerParsingState.offset = 0;
                        return false;
                    }

                    return true;
                }

                default:
                    throw new IllegalStateException();
            }
        }
    }

    // Taken unmodified from Grizzly from HttpCodecFilter.parseHeadersFromBuffer
    boolean parseHeadersFromBuffer(final ByteBuffer input, boolean parsingTrailerHeaders) throws ParseException {
        do {
            if (headerParsingState.subState == 0) {
                final int eol = checkEOL(input);
                if (eol == 0) { // EOL
                    return true;
                } else if (eol == -2) { // not enough data
                    return false;
                }
            }

            if (!parseHeaderFromBuffer(input, parsingTrailerHeaders)) {
                return false;
            }

        } while (true);
    }

    // Taken unmodified from Grizzly HttpCodecFilter.parseHeaderFromBuffer
    private boolean parseHeaderFromBuffer(final ByteBuffer input, boolean parsingTrailerHeaders) throws ParseException {

        while (true) {
            final int subState = headerParsingState.subState;

            switch (subState) {
                case 0: { // start to parse the header
                    headerParsingState.start = headerParsingState.offset;
                    headerParsingState.subState++;
                    break;
                }
                case 1: { // parse header name
                    if (!parseHeaderName(input)) {
                        return false;
                    }

                    headerParsingState.subState++;
                    headerParsingState.start = -1;
                    break;
                }

                case 2: { // skip value preceding spaces
                    final int nonSpaceIdx = HttpParserUtils
                            .skipSpaces(input, headerParsingState.offset, headerParsingState.packetLimit);
                    if (nonSpaceIdx == -1) {
                        headerParsingState.offset = input.limit();
                        return false;
                    }

                    headerParsingState.subState++;
                    headerParsingState.offset = nonSpaceIdx;

                    if (headerParsingState.start == -1) {
                        // Starting to parse header (will be called only for the first line of the multi line header)
                        headerParsingState.start = nonSpaceIdx;
                        headerParsingState.checkpoint = nonSpaceIdx;
                        headerParsingState.checkpoint2 = nonSpaceIdx;
                    }
                    break;
                }

                case 3: { // parse header value
                    final int result = parseHeaderValue(input, parsingTrailerHeaders);
                    if (result == -1) {
                        return false;
                    } else if (result == -2) {
                        // Multiline header detected. Skip preceding spaces
                        headerParsingState.subState = 2;
                        break;
                    }

                    headerParsingState.subState = 0;
                    headerParsingState.start = -1;

                    return true;
                }

                default:
                    throw new IllegalStateException();
            }
        }
    }

    // Taken with small modifications from Grizzly HttpCodecFilter.parseHeaderName
    // (change: Grizzly also initializes value store)
    private boolean parseHeaderName(final ByteBuffer input) throws ParseException {
        final int limit = Math.min(input.limit(), headerParsingState.packetLimit);
        final int start = headerParsingState.start;
        int offset = headerParsingState.offset;

        while (offset < limit) {
            byte b = input.get(offset);
            if (b == HttpParserUtils.COLON) {

                headerParsingState.headerName = parseString(input, start, offset);
                headerParsingState.offset = offset + 1;

                return true;
            } else if ((b >= HttpParserUtils.A) && (b <= HttpParserUtils.Z)) {
                b -= HttpParserUtils.LC_OFFSET;
                input.put(offset, b);
            }

            offset++;
        }

        headerParsingState.offset = offset;
        return false;
    }

    // Taken with small modifications from Grizzly HttpCodecFilter.parseHeaderValue
    // (change: Grizzly saves teh value as a buffer, we split it and add to response)
    private int parseHeaderValue(ByteBuffer input, boolean parsingTrailerHeaders) throws ParseException {

        final int limit = Math.min(input.limit(), headerParsingState.packetLimit);

        int offset = headerParsingState.offset;

        final boolean hasShift = (offset != headerParsingState.checkpoint);

        while (offset < limit) {
            final byte b = input.get(offset);
            /* This if is not in Grizzly, it is used for parsing comma separated values.
             Grizzly separates the header in Header class. */
            if (b == HttpParserUtils.COMMA && !isInseparableHeader()) {
                headerParsingState.offset = offset + 1;
                String value = parseString(input,
                        headerParsingState.start, headerParsingState.checkpoint2);
                httpResponse.addHeader(headerParsingState.headerName, value);
                headerParsingState.start = headerParsingState.checkpoint2;
                return -2;
            }

            if (b == HttpParserUtils.CR) {
                // do nothing
            } else if (b == HttpParserUtils.LF) {
                // Check if it's not multi line header
                if (offset + 1 < limit) {
                    final byte b2 = input.get(offset + 1);
                    if (b2 == HttpParserUtils.SP || b2 == HttpParserUtils.HT) {
                        input.put(headerParsingState.checkpoint++, b2);
                        headerParsingState.offset = offset + 2;
                        return -2;
                    } else {
                        headerParsingState.offset = offset + 1;
                        String value = parseString(input,
                                headerParsingState.start, headerParsingState.checkpoint2);
                        if (parsingTrailerHeaders) {
                            httpResponse.addTrailerHeader(headerParsingState.headerName, value);
                        } else {
                            httpResponse.addHeader(headerParsingState.headerName, value);
                        }
                        return 0;
                    }
                }

                headerParsingState.offset = offset;
                return -1;
            } else if (b == HttpParserUtils.SP) {
                if (hasShift) {
                    input.put(headerParsingState.checkpoint++, b);
                } else {
                    headerParsingState.checkpoint++;
                }
            } else {
                if (hasShift) {
                    input.put(headerParsingState.checkpoint++, b);
                } else {
                    headerParsingState.checkpoint++;
                }
                headerParsingState.checkpoint2 = headerParsingState.checkpoint;
            }

            offset++;
        }

        headerParsingState.offset = offset;
        return -1;
    }

    private boolean isInseparableHeader() {
        /* Authenticate headers contain comma separated list of properties, which would be normally treated as separate header
         values */
        return Constants.WWW_AUTHENTICATE.equalsIgnoreCase(headerParsingState.headerName)
                || Constants.PROXY_AUTHENTICATE.equalsIgnoreCase(headerParsingState.headerName);

    }

    private void decideTransferEncoding() throws ParseException {

        int statusCode = httpResponse.getStatusCode();
        if (statusCode == 204 || statusCode == 205 || statusCode == 304) {
            expectContent = false;
        }

        if (httpResponse.getHeaders().size() == 0) {
            expectContent = false;
        }

        List<String> transferEncodings = httpResponse.getHeader(Constants.TRANSFER_ENCODING_HEADER);

        if (transferEncodings != null) {
            String transferEncoding = transferEncodings.get(0);
            if (Constants.TRANSFER_ENCODING_CHUNKED.equalsIgnoreCase(transferEncoding)) {
                transferEncodingParser = TransferEncodingParser
                        .createChunkParser(httpResponse.getBodyStream(), this, maxHeaderSize);
            }

            return;
        }

        List<String> contentLengths = httpResponse.getHeader(HttpHeaders.CONTENT_LENGTH);

        if (contentLengths != null) {
            try {
                int bodyLength = Integer.parseInt(contentLengths.get(0));
                if (bodyLength == 0) {
                    expectContent = false;
                    return;
                }

                if (bodyLength <= 0) {
                    throw new ParseException(LocalizationMessages.HTTP_NEGATIVE_CONTENT_LENGTH());
                }

                transferEncodingParser = TransferEncodingParser
                        .createFixedLengthParser(httpResponse.getBodyStream(), bodyLength);

            } catch (NumberFormatException e) {
                throw new ParseException(LocalizationMessages.HTTP_INVALID_CONTENT_LENGTH());
            }

            return;
        }

        // TODO what now? Expect no content or fail loudly?
    }

    // Taken unmodified from Grizzly HttpCodecUtils.findSpace
    private int findSpace(final ByteBuffer input, int offset, final int packetLimit) {
        final int limit = Math.min(input.limit(), packetLimit);
        while (offset < limit) {
            final byte b = input.get(offset);
            if (HttpParserUtils.isSpaceOrTab(b)) {
                return offset;
            }

            offset++;
        }

        return -1;
    }

    // Taken unmodified from Grizzly HttpCodecUtils.findEOL
    private boolean findEOL(final ByteBuffer input) {
        int offset = headerParsingState.offset;
        final int limit = Math.min(input.limit(), headerParsingState.packetLimit);

        while (offset < limit) {
            final byte b = input.get(offset);
            if (b == HttpParserUtils.CR) {
                headerParsingState.checkpoint = offset;
            } else if (b == HttpParserUtils.LF) {
                if (headerParsingState.checkpoint == -1) {
                    headerParsingState.checkpoint = offset;
                }

                headerParsingState.offset = offset + 1;
                return true;
            }

            offset++;
        }

        headerParsingState.offset = offset;

        return false;
    }

    // Taken unmodified from Grizzly HttpCodecUtils.checkEOL
    private int checkEOL(final ByteBuffer input) {
        final int offset = headerParsingState.offset;
        final int avail = input.limit() - offset;

        final byte b1;
        final byte b2;

        if (avail >= 2) { // if more than 2 bytes available
            final short s = input.getShort(offset);
            b1 = (byte) (s >>> 8);
            b2 = (byte) (s & 0xFF);
        } else if (avail == 1) {  // if one byte available
            b1 = input.get(offset);
            b2 = -1;
        } else {
            return -2;
        }

        return checkCRLF(b1, b2);
    }

    // Taken unmodified from Grizzly HttpCodecUtils.checkCRLF
    private int checkCRLF(byte b1, byte b2) {
        if (b1 == HttpParserUtils.CR) {
            if (b2 == HttpParserUtils.LF) {
                headerParsingState.offset += 2;
                return 0;
            } else if (b2 == -1) {
                return -2;
            }
        } else if (b1 == HttpParserUtils.LF) {
            headerParsingState.offset++;
            return 0;
        }

        return -1;
    }

    HttpParserUtils.HeaderParsingState getHeaderParsingState() {
        return headerParsingState;
    }

    private String parseString(ByteBuffer input, int startIdx, int endIdx) throws ParseException {
        byte[] bytes = new byte[endIdx - startIdx];
        input.position(startIdx);
        input.get(bytes, 0, endIdx - startIdx);
        try {
            return new String(bytes, ENCODING);
        } catch (UnsupportedEncodingException e) {
            throw new ParseException("Unsupported encoding: " + ENCODING, e);
        }

    }

    private int parseInt(ByteBuffer input, int startIdx, int endIdx) throws ParseException {
        String value = parseString(input, startIdx, endIdx);
        return Integer.valueOf(value);
    }
}
