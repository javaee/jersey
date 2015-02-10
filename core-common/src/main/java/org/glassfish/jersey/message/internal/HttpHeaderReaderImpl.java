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

import java.text.ParseException;

import org.glassfish.jersey.internal.LocalizationMessages;

import static org.glassfish.jersey.message.internal.GrammarUtil.COMMENT;
import static org.glassfish.jersey.message.internal.GrammarUtil.CONTROL;
import static org.glassfish.jersey.message.internal.GrammarUtil.QUOTED_STRING;
import static org.glassfish.jersey.message.internal.GrammarUtil.SEPARATOR;
import static org.glassfish.jersey.message.internal.GrammarUtil.TOKEN;
import static org.glassfish.jersey.message.internal.GrammarUtil.filterToken;
import static org.glassfish.jersey.message.internal.GrammarUtil.getType;
import static org.glassfish.jersey.message.internal.GrammarUtil.isSeparator;
import static org.glassfish.jersey.message.internal.GrammarUtil.isToken;
import static org.glassfish.jersey.message.internal.GrammarUtil.isWhiteSpace;

/**
 * Concrete internal implementation of pull-based HTTP reader.
 *
 * @author Paul Sandoz
 * @author Martin Matula
 */
/* package */ final class HttpHeaderReaderImpl extends HttpHeaderReader {

    private final CharSequence header;
    private final boolean processComments;
    private final int length;

    private int index;
    private Event event;
    private CharSequence value;

    HttpHeaderReaderImpl(String header, boolean processComments) {
        this.header = (header == null) ? "" : header;
        this.processComments = processComments;
        this.index = 0;
        this.length = this.header.length();
    }

    HttpHeaderReaderImpl(String header) {
        this(header, false);
    }

    @Override
    public boolean hasNext() {
        return skipWhiteSpace();
    }

    @Override
    public boolean hasNextSeparator(char separator, boolean skipWhiteSpace) {
        if (skipWhiteSpace) {
            skipWhiteSpace();
        }

        if (index >= length) {
            return false;
        }

        char c = header.charAt(index);
        return isSeparator(c) && c == separator;
    }

    @Override
    public String nextSeparatedString(char startSeparator, char endSeparator) throws ParseException {
        nextSeparator(startSeparator);
        final int start = index;
        for (; index < length; index++) {
            if (header.charAt(index) == endSeparator) {
                break;
            }
        }

        if (start == index) {
            // no token between separators
            throw new ParseException(LocalizationMessages.HTTP_HEADER_NO_CHARS_BETWEEN_SEPARATORS(startSeparator, endSeparator),
                    index);
        } else if (index == length) {
            // no end separator
            throw new ParseException(LocalizationMessages.HTTP_HEADER_NO_END_SEPARATOR(endSeparator), index);
        }

        event = Event.Token;
        value = header.subSequence(start, index++);
        return value.toString();
    }

    @Override
    public Event next() throws ParseException {
        return next(true);
    }

    @Override
    public Event next(boolean skipWhiteSpace) throws ParseException {
        return next(skipWhiteSpace, false);
    }

    @Override
    public Event next(boolean skipWhiteSpace, boolean preserveBackslash) throws ParseException {
        return event = process(getNextCharacter(skipWhiteSpace), preserveBackslash);
    }

    @Override
    public Event getEvent() {
        return event;
    }

    @Override
    public CharSequence getEventValue() {
        return value;
    }

    @Override
    public CharSequence getRemainder() {
        return (index < length) ? header.subSequence(index, header.length()) : null;
    }

    @Override
    public int getIndex() {
        return index;
    }

    private boolean skipWhiteSpace() {
        for (; index < length; index++) {
            if (!isWhiteSpace(header.charAt(index))) {
                return true;
            }
        }

        return false;
    }

    private char getNextCharacter(boolean skipWhiteSpace) throws ParseException {
        if (skipWhiteSpace) {
            skipWhiteSpace();
        }

        if (index >= length) {
            throw new ParseException(LocalizationMessages.HTTP_HEADER_END_OF_HEADER(), index);
        }

        return header.charAt(index);
    }

    private Event process(char c, boolean preserveBackslash) throws ParseException {
        if (c > Byte.MAX_VALUE) {
            index++;
            return Event.Control;
        }

        switch (getType(c)) {
            case TOKEN: {
                final int start = index;
                for (index++; index < length; index++) {
                    if (!isToken(header.charAt(index))) {
                        break;
                    }
                }
                value = header.subSequence(start, index);
                return Event.Token;
            }
            case QUOTED_STRING:
                processQuotedString(preserveBackslash);
                return Event.QuotedString;
            case COMMENT:
                if (!processComments) {
                    throw new ParseException(LocalizationMessages.HTTP_HEADER_COMMENTS_NOT_ALLOWED(), index);
                }

                processComment();
                return Event.Comment;
            case SEPARATOR:
                index++;
                value = String.valueOf(c);
                return Event.Separator;
            case CONTROL:
                index++;
                value = String.valueOf(c);
                return Event.Control;
            default:
                // White space
                throw new ParseException(LocalizationMessages.HTTP_HEADER_WHITESPACE_NOT_ALLOWED(), index);
        }
    }

    private void processComment() throws ParseException {
        boolean filter = false;
        int nesting;
        int start;
        for (start = ++index, nesting = 1; nesting > 0 && index < length; index++) {
            char c = header.charAt(index);
            if (c == '\\') {
                index++;
                filter = true;
            } else if (c == '\r') {
                filter = true;
            } else if (c == '(') {
                nesting++;
            } else if (c == ')') {
                nesting--;
            }
        }
        if (nesting != 0) {
            throw new ParseException(LocalizationMessages.HTTP_HEADER_UNBALANCED_COMMENTS(), index);
        }

        value = (filter) ? filterToken(header, start, index - 1) : header.subSequence(start, index - 1);
    }

    private void processQuotedString(boolean preserveBackslash) throws ParseException {
        boolean filter = false;
        for (int start = ++index; index < length; index++) {
            char c = this.header.charAt(index);
            if (!preserveBackslash && c == '\\') {
                index++;
                filter = true;
            } else if (c == '\r') {
                filter = true;
            } else if (c == '"') {
                value = (filter) ? filterToken(header, start, index, preserveBackslash) : header.subSequence(start, index);

                index++;
                return;
            }
        }

        throw new ParseException(LocalizationMessages.HTTP_HEADER_UNBALANCED_QUOTED(), index);
    }
}
