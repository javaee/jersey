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

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.message.internal.HttpDateFormat;
import org.glassfish.jersey.message.internal.HttpHeaderReader;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Imran@SmartITEngineering.Com
 */
public class FormDataContentDispositionTest extends ContentDispositionTest {

    public FormDataContentDispositionTest() {
        contentDispositionType = "form-data";
    }

    @Test
    @Override
    public void testCreate() {
        final Date date = new Date();
        FormDataContentDisposition contentDisposition;
        contentDisposition = FormDataContentDisposition.name("testData").fileName("test.file").creationDate(date)
                .modificationDate(date).readDate(date).size(1222).build();
        assertFormDataContentDisposition(contentDisposition, date);
        try {
            final String dateString = HttpDateFormat.getPreferredDateFormat().format(date);
            final String header = contentDispositionType + ";filename=\"test.file\";creation-date=\"" + dateString
                    + "\";modification-date=\"" + dateString + "\";read-date=\"" + dateString + "\";size=1222"
                    + ";name=\"testData\"";

            contentDisposition = new FormDataContentDisposition(contentDisposition.toString());
            assertFormDataContentDisposition(contentDisposition, date);
            contentDisposition = new FormDataContentDisposition(header);
            assertFormDataContentDisposition(contentDisposition, date);
            contentDisposition = new FormDataContentDisposition(HttpHeaderReader.newInstance(header), true);
            assertFormDataContentDisposition(contentDisposition, date);
        } catch (final ParseException ex) {
            fail(ex.getMessage());
        }
        try {
            new FormDataContentDisposition((HttpHeaderReader) null, true);
            fail("NullPointerException was expected to be thrown.");
        } catch (final ParseException exception) {
            fail(exception.getMessage());
        } catch (final NullPointerException exception) {
            //expected
        }
        try {
            new FormDataContentDisposition("form-data;filename=\"test.file\"");
            fail("IllegalArgumentException was expected to be thrown.");
        } catch (final ParseException exception) {
            fail(exception.getMessage());
        } catch (final IllegalArgumentException exception) {
            //expected
        }
        try {
            FormDataContentDisposition.name(null).build();
            fail("IllegalArgumentException was expected to be thrown.");
        } catch (final IllegalArgumentException exception) {
            //expected
        } catch (final Exception exception) {
            fail(exception.getMessage());
        }
    }

    @Test
    @Override
    public void testToString() {
        final Date date = new Date();
        final FormDataContentDisposition contentDisposition = FormDataContentDisposition.name("testData")
                .fileName("test.file").creationDate(date).modificationDate(date)
                        .readDate(date).size(1222).build();
        final String dateString = HttpDateFormat.getPreferredDateFormat().format(date);
        final String header = contentDispositionType + "; filename=\"test.file\"; creation-date=\"" + dateString
                + "\"; modification-date=\"" + dateString + "\"; read-date=\"" + dateString + "\"; size=1222"
                + "; name=\"testData\"";

        assertEquals(header, contentDisposition.toString());
    }

    protected void assertFormDataContentDisposition(final FormDataContentDisposition contentDisposition, final Date date) {
        assertContentDisposition(contentDisposition, date);
        assertEquals("testData", contentDisposition.getName());
    }

}
