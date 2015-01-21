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
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Helper class for HTTP specified date formats.
 *
 * @author Paul Sandoz
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public final class HttpDateFormat {

    private HttpDateFormat() {
    }
    /**
     * The date format pattern for RFC 1123.
     */
    private static final String RFC1123_DATE_FORMAT_PATTERN = "EEE, dd MMM yyyy HH:mm:ss zzz";
    /**
     * The date format pattern for RFC 1036.
     */
    private static final String RFC1036_DATE_FORMAT_PATTERN = "EEEE, dd-MMM-yy HH:mm:ss zzz";
    /**
     * The date format pattern for ANSI C asctime().
     */
    private static final String ANSI_C_ASCTIME_DATE_FORMAT_PATTERN = "EEE MMM d HH:mm:ss yyyy";

    private static final TimeZone GMT_TIME_ZONE = TimeZone.getTimeZone("GMT");

    private static final ThreadLocal<List<SimpleDateFormat>> dateFormats = new ThreadLocal<List<SimpleDateFormat>>() {

        @Override
        protected synchronized List<SimpleDateFormat> initialValue() {
            return createDateFormats();
        }
    };

    private static List<SimpleDateFormat> createDateFormats() {
        final SimpleDateFormat[] formats = new SimpleDateFormat[]{
            new SimpleDateFormat(RFC1123_DATE_FORMAT_PATTERN, Locale.US),
            new SimpleDateFormat(RFC1036_DATE_FORMAT_PATTERN, Locale.US),
            new SimpleDateFormat(ANSI_C_ASCTIME_DATE_FORMAT_PATTERN, Locale.US)
        };
        formats[0].setTimeZone(GMT_TIME_ZONE);
        formats[1].setTimeZone(GMT_TIME_ZONE);
        formats[2].setTimeZone(GMT_TIME_ZONE);

        return Collections.unmodifiableList(Arrays.asList(formats));
    }

    /**
     * Return an unmodifiable list of HTTP specified date formats to use for
     * parsing or formatting {@link Date}.
     * <p>
     * The list of date formats are scoped to the current thread and may be
     * used without requiring to synchronize access to the instances when
     * parsing or formatting.
     *
     * @return the list of data formats.
     */
    private static List<SimpleDateFormat> getDateFormats() {
        return dateFormats.get();
    }

    /**
     * Get the preferred HTTP specified date format (RFC 1123).
     * <p>
     * The date format is scoped to the current thread and may be
     * used without requiring to synchronize access to the instance when
     * parsing or formatting.
     *
     * @return the preferred of data format.
     */
    public static SimpleDateFormat getPreferredDateFormat() {
        // returns clone because calling SDF.parse(...) can change time zone
        return (SimpleDateFormat) dateFormats.get().get(0).clone();
    }

    /**
     * Read a date.
     *
     * @param date the date as a string.
     *
     * @return the date
     * @throws java.text.ParseException in case the date string cannot be parsed.
     */
    public static Date readDate(final String date) throws ParseException {
        ParseException pe = null;
        for (final SimpleDateFormat f : HttpDateFormat.getDateFormats()) {
            try {
                Date result = f.parse(date);
                // parse can change time zone -> set it back to GMT
                f.setTimeZone(GMT_TIME_ZONE);
                return result;
            } catch (final ParseException e) {
                pe = (pe == null) ? e : pe;
            }
        }

        throw pe;
    }
}
