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

import java.nio.ByteBuffer;

/**
 * @author Alexey Stashok
 * @author Petr Janouch (petr.janouch at oracle.com)
 */
class HttpParserUtils {

    static final byte CR = (byte) '\r';
    static final byte LF = (byte) '\n';
    static final byte SP = (byte) ' ';
    static final byte HT = (byte) '\t';
    static final byte COMMA = (byte) ',';
    static final byte COLON = (byte) ':';
    static final byte SEMI_COLON = (byte) ';';
    static final byte A = (byte) 'A';
    static final byte Z = (byte) 'Z';
    static final byte a = (byte) 'a';
    static final byte LC_OFFSET = A - a;

    static int skipSpaces(ByteBuffer input, int offset, int packetLimit) {
        final int limit = Math.min(input.limit(), packetLimit);
        while (offset < limit) {
            final byte b = input.get(offset);
            if (isNotSpaceAndTab(b)) {
                return offset;
            }

            offset++;
        }

        return -1;
    }

    static boolean isNotSpaceAndTab(byte b) {
        return !isSpaceOrTab(b);
    }

    static boolean isSpaceOrTab(byte b) {
        return (b == HttpParserUtils.SP || b == HttpParserUtils.HT);
    }

    static class HeaderParsingState {

        final int maxHeaderSize;
        int packetLimit;

        int state;
        int subState;

        int start;
        int offset;
        int checkpoint = -1; // extra parsing state field
        int checkpoint2 = -1; // extra parsing state field

        String headerName;

        long parsingNumericValue;

        int contentLengthHeadersCount;   // number of Content-Length headers in the HTTP header
        boolean contentLengthsDiffer;

        HeaderParsingState(int maxHeaderSize) {
            this.maxHeaderSize = maxHeaderSize;
        }

        void recycle() {
            state = 0;
            subState = 0;
            start = 0;
            offset = 0;
            checkpoint = -1;
            checkpoint2 = -1;
            parsingNumericValue = 0;
            contentLengthHeadersCount = 0;
            contentLengthsDiffer = false;
            headerName = null;
            packetLimit = maxHeaderSize;
        }

        void checkOverflow(String errorDescriptionIfOverflow) throws ParseException {
            if (offset < packetLimit) {
                return;
            }

            throw new ParseException(errorDescriptionIfOverflow);
        }
    }

    static class ContentParsingState {

        boolean isLastChunk;
        int chunkContentStart = -1;
        long chunkLength = -1;
        long chunkRemainder = -1;
    }
}
