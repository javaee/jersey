/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.api;

import java.text.ParseException;
import java.util.Date;
import java.util.List;

import javax.ws.rs.core.MediaType;
import org.glassfish.jersey.message.internal.AcceptableLanguageTag;
import org.glassfish.jersey.message.internal.AcceptableToken;
import org.glassfish.jersey.message.internal.HttpDateFormat;
import org.glassfish.jersey.message.internal.HttpHeaderReader;
import org.glassfish.jersey.message.internal.LanguageTag;
import org.glassfish.jersey.message.internal.ParameterizedHeader;
import org.glassfish.jersey.message.internal.Token;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Paul Sandoz
 */
public class HttpHeaderTest {

    @Test
    public void testTokens() throws ParseException {
        final String header = "type  /  content; a = \"asdsd\"";

        final HttpHeaderReader r = HttpHeaderReader.newInstance(header);
        while (r.hasNext()) {
            r.next();
        }
    }

    @Test
    public void testMediaType() throws ParseException {
        final String mimeType = "application/xml;charset=UTF-8";
        MediaType.valueOf(mimeType);
    }

    @Test
    public void testLanguageTag() throws ParseException {
        final String languageTag = "en-US";
        new LanguageTag(languageTag);
    }

    @Test
    public void testAcceptableLanguageTag() throws ParseException {
        final String languageTag = "en-US;q=0.123";
        new AcceptableLanguageTag(languageTag);
    }

    @Test
    public void testAcceptableLanguageTagList() throws Exception {
        final String languageTags = "en-US;q=0.123, fr;q=0.2, en;q=0.3, *;q=0.01";
        final List<AcceptableLanguageTag> l = HttpHeaderReader.readAcceptLanguage(languageTags);
        assertEquals("en", l.get(0).getTag());
        assertEquals("fr", l.get(1).getTag());
        assertEquals("en-US", l.get(2).getTag());
        assertEquals("*", l.get(3).getTag());
    }

    @Test
    public void testToken() throws ParseException {
        final String token = "gzip";
        new Token(token);
    }

    @Test
    public void testAcceptableToken() throws ParseException {
        final String token = "gzip;q=0.123";
        new AcceptableToken(token);
    }

    @Test
    public void testAcceptableTokenList() throws Exception {
        final String tokens = "gzip;q=0.123, compress;q=0.2, zlib;q=0.3, *;q=0.01";
        final List<AcceptableToken> l = HttpHeaderReader.readAcceptToken(tokens);
        assertEquals("zlib", l.get(0).getToken());
        assertEquals("compress", l.get(1).getToken());
        assertEquals("gzip", l.get(2).getToken());
        assertEquals("*", l.get(3).getToken());
    }

    @Test
    public void testDateParsing() throws ParseException {
        final String date_RFC1123 = "Sun, 06 Nov 1994 08:49:37 GMT";
        final String date_RFC1036 = "Sunday, 06-Nov-94 08:49:37 GMT";
        final String date_ANSI_C = "Sun Nov  6 08:49:37 1994";

        HttpHeaderReader.readDate(date_RFC1123);
        HttpHeaderReader.readDate(date_RFC1036);
        HttpHeaderReader.readDate(date_ANSI_C);
    }

    @Test
    public void testDateFormatting() throws ParseException {
        final String date_RFC1123 = "Sun, 06 Nov 1994 08:49:37 GMT";
        final Date date = HttpHeaderReader.readDate(date_RFC1123);

        final String date_formatted = HttpDateFormat.getPreferredDateFormat().format(date);
        assertEquals(date_RFC1123, date_formatted);
    }

    @Test
    public void testParameterizedHeader() throws ParseException {
        ParameterizedHeader ph = new ParameterizedHeader("a");
        assertEquals("a", ph.getValue());

        ph = new ParameterizedHeader("a/b");
        assertEquals("a/b", ph.getValue());

        ph = new ParameterizedHeader("  a  /  b  ");
        assertEquals("a/b", ph.getValue());

        ph = new ParameterizedHeader("");
        assertEquals("", ph.getValue());

        ph = new ParameterizedHeader(";");
        assertEquals("", ph.getValue());
        assertEquals(0, ph.getParameters().size());

        ph = new ParameterizedHeader(";;;");
        assertEquals("", ph.getValue());
        assertEquals(0, ph.getParameters().size());

        ph = new ParameterizedHeader("  ;  ;  ;  ");
        assertEquals("", ph.getValue());
        assertEquals(0, ph.getParameters().size());

        ph = new ParameterizedHeader("a;x=1;y=2");
        assertEquals("a", ph.getValue());
        assertEquals(2, ph.getParameters().size());
        assertEquals("1", ph.getParameters().get("x"));
        assertEquals("2", ph.getParameters().get("y"));

        ph = new ParameterizedHeader("a ;  x=1  ;  y=2  ");
        assertEquals("a", ph.getValue());
        assertEquals(2, ph.getParameters().size());
        assertEquals("1", ph.getParameters().get("x"));
        assertEquals("2", ph.getParameters().get("y"));
    }
}
