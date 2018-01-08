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

package org.glassfish.jersey.message.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

/**
 * {@link FormProvider} unit tests
 *
 * @author Petr Bouda
 */
public class FormMultivaluedMapProviderTest {

    private static final FormMultivaluedMapProvider PROVIDER = new FormMultivaluedMapProvider();

    @Test
    public void testReadFormParam() {
        MultivaluedMap<String, String> map = readFrom("name&age=26");
        assertEquals(2, map.size());

        List<String> nameEntry = map.get("name");
        assertEquals(1, nameEntry.size());
        assertNull(nameEntry.get(0));

        List<String> ageEntry = map.get("age");
        assertEquals(1, ageEntry.size());
        assertEquals("26", ageEntry.get(0));
    }

    @Test
    public void testReadMultipleSameFormParam() {
        MultivaluedMap<String, String> map = readFrom("name&name=George");
        assertEquals(1, map.size());

        List<String> nameEntry = map.get("name");
        assertEquals(2, nameEntry.size());
        assertNull(nameEntry.get(0));
        assertEquals("George", nameEntry.get(1));
    }

    @SuppressWarnings("unchecked")
    private static MultivaluedMap<String, String> readFrom(String body) {
        try {
            InputStream stream = new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
            Class<MultivaluedMap<String, String>> clazz =
                    (Class<MultivaluedMap<String, String>>) (Class<?>) MultivaluedHashMap.class;
            return PROVIDER.readFrom(clazz, clazz, new Annotation[] {}, MediaType.APPLICATION_FORM_URLENCODED_TYPE, null, stream);
        } catch (IOException e) {
            throw new RuntimeException("Unexpected exception", e);
        }
    }
}
