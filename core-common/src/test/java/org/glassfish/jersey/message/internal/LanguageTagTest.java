/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2016 Oracle and/or its affiliates. All rights reserved.
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

import java.util.Locale;

import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link LanguageTag} class.
 *
 * @author Michal Gajdos
 */
public class LanguageTagTest {

    @Test
    public void testLanguageCountry() throws Exception {
        _test("en", "gb");
        _test("sk", "SK");
        _test("CZ", "cs");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLanguageCountryInvalid() throws Exception {
        _test("en", "gbgbgbgbgb");
    }

    @Test
    public void testLanguageRegion() throws Exception {
        _test("es", "419");
    }

    @Test
    public void testEquals() {
        LanguageTag lt1 = new LanguageTag("en", "us");
        LanguageTag lt2 = new LanguageTag("en", "us");

        assertTrue(lt1.equals(lt2));
    }

    @Test
    public void testNonEquals() {
        LanguageTag lt1 = new LanguageTag("en", "us");
        LanguageTag lt2 = new LanguageTag("en", "gb");

        assertFalse(lt1.equals(lt2));
    }

    private void _test(final String primary, final String sub) throws Exception {
        final LanguageTag tag = LanguageTag.valueOf(primary + "-" + sub);

        assertThat(tag.getPrimaryTag(), equalToIgnoringCase(primary));
        assertThat(tag.getSubTags(), equalToIgnoringCase(sub));

        final Locale locale = tag.getAsLocale();

        assertThat(locale.getLanguage(), equalToIgnoringCase(primary));
        assertThat(locale.getCountry(), equalToIgnoringCase(sub));
    }
}
