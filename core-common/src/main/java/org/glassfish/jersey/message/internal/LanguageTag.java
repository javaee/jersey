/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

    protected String tag;
    protected String primaryTag;
    protected String subTags;

    protected LanguageTag() {
    }

    public static LanguageTag valueOf(String s) throws IllegalArgumentException {
        LanguageTag lt = new LanguageTag();

        try {
            lt.parse(s);
        } catch (ParseException pe) {
            throw new IllegalArgumentException(pe);
        }

        return lt;
    }

    public LanguageTag(String primaryTag, String subTags) {
        if (subTags != null && subTags.length() > 0) {
            this.tag = primaryTag + "-" + subTags;
        } else {
            this.tag = primaryTag;
        }

        this.primaryTag = primaryTag;

        this.subTags = subTags;
    }

    public LanguageTag(String header) throws ParseException {
        this(HttpHeaderReader.newInstance(header));
    }

    public LanguageTag(HttpHeaderReader reader) throws ParseException {
        // Skip any white space
        reader.hasNext();

        tag = reader.nextToken();

        if (reader.hasNext()) {
            throw new ParseException("Invalid Language tag", reader.getIndex());
        }

        parse(tag);
    }

    public final boolean isCompatible(Locale tag) {
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

    protected final void parse(String languageTag) throws ParseException {
        if (!isValid(languageTag)) {
            throw new ParseException("String, " + languageTag + ", is not a valid language tag", 0);
        }

        int index = languageTag.indexOf('-');
        if (index == -1) {
            primaryTag = languageTag;
            subTags = null;
        } else {
            primaryTag = languageTag.substring(0, index);
            subTags = languageTag.substring(index + 1, languageTag.length());
        }
    }

    private boolean isValid(String tag) {
        int alphaCount = 0;
        for (int i = 0; i < tag.length(); i++) {
            final char c = tag.charAt(i);
            if (c == '-') {
                if (alphaCount == 0) {
                    return false;
                }
                alphaCount = 0;
            } else if (('A' <= c && c <= 'Z') || ('a' <= c && c <= 'z')) {
                alphaCount++;
                if (alphaCount > 8) {
                    return false;
                }
            } else {
                return false;
            }
        }
        return (alphaCount != 0);
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
    public boolean equals(Object object) {
        if (object instanceof LanguageTag) {
            LanguageTag lt = (LanguageTag) object;

            if (this.tag != null) {
                if (!this.tag.equals(lt.getTag())) {
                    return false;
                } else if (lt.getTag() != null) {
                    return false;
                }
            }

            if (this.primaryTag != null) {
                if (!this.primaryTag.equals(lt.getPrimaryTag())) {
                    return false;
                } else if (lt.getPrimaryTag() != null) {
                    return false;
                }
            }

            if (this.subTags != null) {
                if (!this.subTags.equals(lt.getSubTags())) {
                    return false;
                } else if (lt.getSubTags() != null) {
                    return false;
                }
            }

            return true;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return (tag == null ? 0 : tag.hashCode())
                + (primaryTag == null ? 0 : primaryTag.hashCode())
                + (subTags == null ? 0 : primaryTag.hashCode());
    }

    @Override
    public String toString() {
        return primaryTag + (subTags == null ? "" : subTags);
    }
}
