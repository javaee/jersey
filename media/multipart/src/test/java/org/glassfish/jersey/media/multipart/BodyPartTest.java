/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.media.multipart;

import java.util.List;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test case for {@link BodyPart}.
 *
 * @author Craig McClanahan
 * @author Imran M Yousuf (imran at smartitengineering.com)
 * @author Paul Sandoz
 * @author Michal Gajdos
 */
public class BodyPartTest {

    @Before
    public void setUp() throws Exception {
        bodyPart = new BodyPart();
    }

    @After
    public void tearDown() throws Exception {
        bodyPart = null;
    }

    protected BodyPart bodyPart = null;

    @Test
    public void testCreate() {
        assertEquals("text/plain", bodyPart.getMediaType().toString());
        bodyPart.setMediaType(new MediaType("application", "json"));
        assertEquals("application/json", bodyPart.getMediaType().toString());
    }

    @Test
    public void testEntity() {
        bodyPart.setEntity("foo bar baz");
        assertEquals("foo bar baz", bodyPart.getEntity());
    }

    @Test
    public void testHeaders() {
        MultivaluedMap<String, String> headers = bodyPart.getHeaders();
        assertNotNull(headers);
        assertNull(headers.get(HttpHeaders.ACCEPT));
        headers.add(HttpHeaders.ACCEPT, "application/xml");
        assertEquals("application/xml", headers.getFirst(HttpHeaders.ACCEPT));
        headers.add(HttpHeaders.ACCEPT, "application/json");
        assertEquals("application/xml", headers.getFirst(HttpHeaders.ACCEPT));
        List values = headers.get(HttpHeaders.ACCEPT);
        assertTrue(values.contains("application/xml"));
        assertTrue(values.contains("application/json"));
        assertNotNull(headers.get("accept"));
        assertNotNull(headers.get("ACCEPT"));
    }

}
