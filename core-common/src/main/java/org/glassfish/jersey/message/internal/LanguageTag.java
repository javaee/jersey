/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2016 Oracle and/or its affiliates. All rights reserved.
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
import java.util.Locale;

/**
 * A language tag.
 *
 * @author Paul Sandoz
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class LanguageTag {

    String tag;
    String primaryTag;
    String subTags;

    protected LanguageTag() {
    }

    public static LanguageTag valueOf(final String s) throws IllegalArgumentException {
        final LanguageTag lt = new LanguageTag();

        try {
            lt.parse(s);
        } catch (final ParseException pe) {
            throw new IllegalArgumentException(pe);
        }

        return lt;
    }

    public LanguageTag(final String primaryTag, final String subTags) {
        if (subTags != null && subTags.length() > 0) {
            this.tag = primaryTag + "-" + subTags;
        } else {
            this.tag = primaryTag;
        }

        this.primaryTag = primaryTag;

        this.subTags = subTags;
    }

    public LanguageTag(final String header) throws ParseException {
        this(HttpHeaderReader.newInstance(header));
    }

    public LanguageTag(final HttpHeaderReader reader) throws ParseException {
        // Skip any white space
        reader.hasNext();

        tag = reader.nextToken().toString();

        if (reader.hasNext()) {
            throw new ParseException("Invalid Language tag", reader.getIndex());
        }

        parse(tag);
    }

    public final boolean isCompatible(final Locale tag) {
        if (this.tag.equals("*")) {
            return true;
        }

        if (subTags == null) {
            return primaryTag.equalsIgnoreCase(tag.getLanguage());
        } else {
            return primaryTag.equalsIgnoreCase(tag.getLanguage())
                    && subTags.equalsIgnoreCase(tag.getCountry());
        }
    }

    public final Locale getAsLocale() {
        return (subTags == null)
                ? new Locale(primaryTag)
                : new Locale(primaryTag, subTags);
    }

    protected final void parse(final String languageTag) throws ParseException {
        if (!isValid(languageTag)) {
            throw new ParseException("String, " + languageTag + ", is not a valid language tag", 0);
        }

        final int index = languageTag.indexOf('-');
        if (index == -1) {
            primaryTag = languageTag;
            subTags = null;
        } else {
            primaryTag = languageTag.substring(0, index);
            subTags = languageTag.substring(index + 1, languageTag.length());
        }
    }

    /**
     * Validate input tag (header value) according to HTTP 1.1 spec + allow region code (numeric) instead of country code.
     *
     * @param tag accept-language header value.
     * @return {@code true} if the given value is valid language tag, {@code false} instead.
     */
    private boolean isValid(final String tag) {
        int alphanumCount = 0;
        int dash = 0;
        for (int i = 0; i < tag.length(); i++) {
            final char c = tag.charAt(i);
            if (c == '-') {
                if (alphanumCount == 0) {
                    return false;
                }
                alphanumCount = 0;
                dash++;
            } else if (('A' <= c && c <= 'Z') || ('a' <= c && c <= 'z') || (dash > 0 && '0' <= c && c <= '9')) {
                alphanumCount++;
                if (alphanumCount > 8) {
                    return false;
                }
            } else {
                return false;
            }
        }
        return (alphanumCount != 0);
    }

    public final String getTag() {
        return tag;
    }

    public final String getPrimaryTag() {
        return primaryTag;
    }

    public final String getSubTags() {
        return subTags;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LanguageTag) || o.getClass() != this.getClass()) {
            return false;
        }

        final LanguageTag that = (LanguageTag) o;

        if (primaryTag != null ? !primaryTag.equals(that.primaryTag) : that.primaryTag != null) {
            return false;
        }
        if (subTags != null ? !subTags.equals(that.subTags) : that.subTags != null) {
            return false;
        }
        return !(tag != null ? !tag.equals(that.tag) : that.tag != null);

    }

    @Override
    public int hashCode() {
        int result = tag != null ? tag.hashCode() : 0;
        result = 31 * result + (primaryTag != null ? primaryTag.hashCode() : 0);
        result = 31 * result + (subTags != null ? subTags.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return primaryTag + (subTags == null ? "" : subTags);
    }
}
