/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2014 Oracle and/or its affiliates. All rights reserved.
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
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;

/**
 * Helper class for HTTP specified date formats.
 *
 * @author Paul Sandoz
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @author Derek P. Moore
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
    private static final String RFC1036_DATE_FORMAT_PATTERN_ALT1 = "EEEE, dd MMM yy HH:mm:ss zzz";
    private static final String RFC1036_DATE_FORMAT_PATTERN_ALT2 = "dd MMM yy HH:mm:ss zzz";
    private static final String RFC1036_DATE_FORMAT_PATTERN_OLD = "EEEE, dd MMM HH:mm:ss yyyy";
    /**
     * The date format pattern for ANSI C asctime().
     */
    private static final String ANSI_C_ASCTIME_DATE_FORMAT_PATTERN = "EEE MMM d HH:mm:ss yyyy";
    /**
     * The date format pattern for RFC 2822 section 3.3 excluding obsolete date and time.
     */
    private static final String RFC2822_DATE_FORMAT_PATTERN = "EEE, d MMM yyyy HH:mm:ss Z";
    private static final String RFC2822_DATE_FORMAT_PATTERN_ALT1 = "d MMM yyyy HH:mm:ss Z";
    private static final String RFC2822_DATE_FORMAT_PATTERN_ALT2 = "EEE, d MMM yyyy HH:mm Z";
    private static final String RFC2822_DATE_FORMAT_PATTERN_ALT3 = "d MMM yyyy HH:mm Z";
    /**
     * The date format pattern for RFC 3339,
     * JAXB's DatatypeFactory.newXMLGregorianCalendar(),
     * ECMAScript simplified ISO 8601, and
     * XML Schema Part 2: Datatypes' dateTime.
     */
    private static final String ECMASCRIPT_ISO_DATE_FORMAT_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
    private static final String RFC3339_DATE_FORMAT_PATTERN_FULL = "yyyy-MM-dd'T'HH:mm:ss.SSSXX";
    private static final String RFC3339_DATE_FORMAT_PATTERN_ALT1 = "yyyy-MM-dd'T'HH:mm:ssXXX";
    private static final String RFC3339_DATE_FORMAT_PATTERN_ALT2 = "yyyy-MM-dd'T'HH:mm:ssXX";
    private static final String XSD_DATE_FORMAT_PATTERN_MIN1 = "yyyy-MM-dd'T'HH:mm:ss";
    private static final String XSD_DATE_FORMAT_PATTERN_MIN2 = "yyyy-MM-dd'T'HH:mm:ss.SSS";
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
            new SimpleDateFormat(RFC1036_DATE_FORMAT_PATTERN_ALT1, Locale.US),
            new SimpleDateFormat(RFC1036_DATE_FORMAT_PATTERN_ALT2, Locale.US),
            new SimpleDateFormat(RFC1036_DATE_FORMAT_PATTERN_OLD, Locale.US),
            new SimpleDateFormat(ANSI_C_ASCTIME_DATE_FORMAT_PATTERN, Locale.US),
            new SimpleDateFormat(ECMASCRIPT_ISO_DATE_FORMAT_PATTERN, Locale.US),
            new SimpleDateFormat(RFC3339_DATE_FORMAT_PATTERN_FULL, Locale.US),
            new SimpleDateFormat(RFC3339_DATE_FORMAT_PATTERN_ALT1, Locale.US),
            new SimpleDateFormat(RFC3339_DATE_FORMAT_PATTERN_ALT2, Locale.US),
            new SimpleDateFormat(XSD_DATE_FORMAT_PATTERN_MIN1, Locale.US)
        };

        final TimeZone tz = TimeZone.getTimeZone("GMT");
        for (final SimpleDateFormat f : formats) {
            f.setTimeZone(tz);
        }

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
    public static List<SimpleDateFormat> getDateFormats() {
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
        return dateFormats.get().get(0);
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
        final Map<Date, Map<SimpleDateFormat, ParsePosition>> valid = new HashMap<Date, Map<SimpleDateFormat, ParsePosition>>();
        for (final SimpleDateFormat f : HttpDateFormat.getDateFormats()) {
            final ParsePosition pp = new ParsePosition(0);
            final Date d = f.parse(date, pp);
            if (pp.getErrorIndex() == -1) {
                final Map<SimpleDateFormat, ParsePosition> data = new HashMap<SimpleDateFormat, ParsePosition>();
                data.put(f, pp);
                valid.put(d, data);
            }
        }

        System.out.println("----\n" + date);
        Date latest = null;
        int last_pos = 0;
        for (final Entry<Date, Map<SimpleDateFormat, ParsePosition>> e : valid.entrySet()) {
            final Date d = e.getKey();
            final Map<SimpleDateFormat, ParsePosition> v = e.getValue();
            final ParsePosition pp = v.values().iterator().next();
            System.out.println(v.keySet().iterator().next().format(d));
            if (latest == null)
                latest = d;
            if ((d.after(latest) && pp.getIndex() >= last_pos)
                || pp.getIndex() > last_pos) {
                latest = d;
                last_pos = pp.getIndex();
            }
        }
        if (latest != null)
            return latest;

        throw new ParseException("Unparseable date: \"" + date + "\"", -1);
    }
}
