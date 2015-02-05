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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.core.CacheControl;

import javax.inject.Singleton;

import org.glassfish.jersey.internal.LocalizationMessages;
import org.glassfish.jersey.spi.HeaderDelegateProvider;
import static org.glassfish.jersey.message.internal.Utils.throwIllegalArgumentExceptionIfNull;

/**
 * {@code Cache-Control} {@link HeaderDelegateProvider header delegate provider}.
 *
 * @author Paul Sandoz
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @author hubick@java.net
 */
@Singleton
public final class CacheControlProvider implements HeaderDelegateProvider<CacheControl> {

    private static final Pattern WHITESPACE = Pattern.compile("\\s");
    private static final Pattern COMMA_SEPARATED_LIST = Pattern.compile("[\\s]*,[\\s]*");

    @Override
    public boolean supports(Class<?> type) {
        return type == CacheControl.class;
    }

    @Override
    public String toString(CacheControl header) {

        throwIllegalArgumentExceptionIfNull(header, LocalizationMessages.CACHE_CONTROL_IS_NULL());

        StringBuilder b = new StringBuilder();
        if (header.isPrivate()) {
            appendQuotedWithSeparator(b, "private", buildListValue(header.getPrivateFields()));
        }
        if (header.isNoCache()) {
            appendQuotedWithSeparator(b, "no-cache", buildListValue(header.getNoCacheFields()));
        }
        if (header.isNoStore()) {
            appendWithSeparator(b, "no-store");
        }
        if (header.isNoTransform()) {
            appendWithSeparator(b, "no-transform");
        }
        if (header.isMustRevalidate()) {
            appendWithSeparator(b, "must-revalidate");
        }
        if (header.isProxyRevalidate()) {
            appendWithSeparator(b, "proxy-revalidate");
        }
        if (header.getMaxAge() != -1) {
            appendWithSeparator(b, "max-age", header.getMaxAge());
        }
        if (header.getSMaxAge() != -1) {
            appendWithSeparator(b, "s-maxage", header.getSMaxAge());
        }

        for (Map.Entry<String, String> e : header.getCacheExtension().entrySet()) {
            appendWithSeparator(b, e.getKey(), quoteIfWhitespace(e.getValue()));
        }

        return b.toString();
    }

    private void readFieldNames(List<String> fieldNames, HttpHeaderReader reader)
            throws ParseException {
        if (!reader.hasNextSeparator('=', false)) {
            return;
        }
        reader.nextSeparator('=');
        fieldNames.addAll(Arrays.asList(COMMA_SEPARATED_LIST.split(reader.nextQuotedString())));
    }

    private int readIntValue(HttpHeaderReader reader, String directiveName)
            throws ParseException {
        reader.nextSeparator('=');
        int index = reader.getIndex();
        try {
            return Integer.parseInt(reader.nextToken().toString());
        } catch (NumberFormatException nfe) {
            ParseException pe = new ParseException(
                    "Error parsing integer value for " + directiveName + " directive", index);
            pe.initCause(nfe);
            throw pe;
        }
    }

    private void readDirective(CacheControl cacheControl,
                               HttpHeaderReader reader) throws ParseException {

        final String directiveName = reader.nextToken().toString().toLowerCase();
        if ("private".equals(directiveName)) {
            cacheControl.setPrivate(true);
            readFieldNames(cacheControl.getPrivateFields(), reader);
        } else if ("public".equals(directiveName)) {
            // CacheControl API doesn't support 'public' for some reason.
            cacheControl.getCacheExtension().put(directiveName, null);
        } else if ("no-cache".equals(directiveName)) {
            cacheControl.setNoCache(true);
            readFieldNames(cacheControl.getNoCacheFields(), reader);
        } else if ("no-store".equals(directiveName)) {
            cacheControl.setNoStore(true);
        } else if ("no-transform".equals(directiveName)) {
            cacheControl.setNoTransform(true);
        } else if ("must-revalidate".equals(directiveName)) {
            cacheControl.setMustRevalidate(true);
        } else if ("proxy-revalidate".equals(directiveName)) {
            cacheControl.setProxyRevalidate(true);
        } else if ("max-age".equals(directiveName)) {
            cacheControl.setMaxAge(readIntValue(reader, directiveName));
        } else if ("s-maxage".equals(directiveName)) {
            cacheControl.setSMaxAge(readIntValue(reader, directiveName));
        } else {
            String value = null;
            if (reader.hasNextSeparator('=', false)) {
                reader.nextSeparator('=');
                value = reader.nextTokenOrQuotedString().toString();
            }
            cacheControl.getCacheExtension().put(directiveName, value);
        }
    }

    @Override
    public CacheControl fromString(String header) {

        throwIllegalArgumentExceptionIfNull(header, LocalizationMessages.CACHE_CONTROL_IS_NULL());

        try {
            HttpHeaderReader reader = HttpHeaderReader.newInstance(header);
            CacheControl cacheControl = new CacheControl();
            cacheControl.setNoTransform(false); // defaults to true
            while (reader.hasNext()) {
                readDirective(cacheControl, reader);
                if (reader.hasNextSeparator(',', true)) {
                    reader.nextSeparator(',');
                }
            }
            return cacheControl;
        } catch (ParseException pe) {
            throw new IllegalArgumentException(
                    "Error parsing cache control '" + header + "'", pe);
        }
    }

    private void appendWithSeparator(StringBuilder b, String field) {
        if (b.length() > 0) {
            b.append(", ");
        }
        b.append(field);
    }

    private void appendQuotedWithSeparator(StringBuilder b, String field, String value) {
        appendWithSeparator(b, field);
        if (value != null && !value.isEmpty()) {
            b.append("=\"");
            b.append(value);
            b.append("\"");
        }
    }

    private void appendWithSeparator(StringBuilder b, String field, String value) {
        appendWithSeparator(b, field);
        if (value != null && !value.isEmpty()) {
            b.append("=");
            b.append(value);
        }
    }

    private void appendWithSeparator(StringBuilder b, String field, int value) {
        appendWithSeparator(b, field);
        b.append("=");
        b.append(value);
    }

    private String buildListValue(List<String> values) {
        StringBuilder b = new StringBuilder();
        for (String value : values) {
            appendWithSeparator(b, value);
        }
        return b.toString();
    }

    private String quoteIfWhitespace(String value) {
        if (value == null) {
            return null;
        }
        Matcher m = WHITESPACE.matcher(value);
        if (m.find()) {
            return "\"" + value + "\"";
        }
        return value;
    }
}
