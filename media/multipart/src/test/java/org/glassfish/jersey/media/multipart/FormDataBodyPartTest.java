/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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

import javax.ws.rs.core.MediaType;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test case for {@link FormDataBodyPart}.
 *
 * @author Craig McClanahan
 * @author Imran M Yousuf (imran at smartitengineering.com)
 * @author Paul Sandoz (paul.sandoz at oracle.com)
 * @author Michal Gajdos (michal.gajdos at oracle.com)
 */
public class FormDataBodyPartTest extends BodyPartTest {

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        bodyPart = new FormDataBodyPart();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        bodyPart = null;
        super.tearDown();
    }

    @Test
    public void testCreateFDBP() throws Exception {
        FormDataBodyPart fdbp = (FormDataBodyPart) bodyPart;
        assertNull(fdbp.getFormDataContentDisposition());
        assertNull(fdbp.getName());
        assertNull(fdbp.getValue());
        assertTrue(fdbp.isSimple());

        fdbp = new FormDataBodyPart("<foo>bar</foo>", MediaType.APPLICATION_XML_TYPE);
        assertNull(fdbp.getFormDataContentDisposition());
        assertNull(fdbp.getName());

        try {
            fdbp.getValue();
            fail("Should have thrown IllegalStateException");
        } catch (IllegalStateException e) {
            // Expected result.
        }

        assertEquals("<foo>bar</foo>", fdbp.getEntity());
        assertTrue(!fdbp.isSimple());

        fdbp = new FormDataBodyPart("name", "value");
        assertNotNull(fdbp.getFormDataContentDisposition());
        assertEquals("name", fdbp.getName());
        assertEquals("value", fdbp.getValue());
        assertTrue(fdbp.isSimple());

        fdbp = new FormDataBodyPart("name", "<foo>bar</foo>", MediaType.APPLICATION_XML_TYPE);
        assertNotNull(fdbp.getFormDataContentDisposition());
        assertEquals("name", fdbp.getName());

        try {
            fdbp.getValue();
            fail("Should have thrown IllegalStateException");
        } catch (IllegalStateException e) {
            // Expected result.
        }

        assertEquals("<foo>bar</foo>", fdbp.getEntity());
        assertTrue(!fdbp.isSimple());

        fdbp = new FormDataBodyPart(
                FormDataContentDisposition.name("name").fileName("filename").build(),
                "<foo>bar</foo>",
                MediaType.APPLICATION_XML_TYPE);
        assertNotNull(fdbp.getFormDataContentDisposition());
        assertEquals("name", fdbp.getName());
        assertEquals("filename", fdbp.getFormDataContentDisposition().getFileName());

        try {
            fdbp.getValue();
            fail("Should have thrown IllegalStateException");
        } catch (IllegalStateException e) {
            // Expected result.
        }

        assertEquals("<foo>bar</foo>", fdbp.getEntity());
        assertTrue(!fdbp.isSimple());

        try {
            fdbp = new FormDataBodyPart();
            fdbp.setName(null);
            fail("Name should be null settable!");
        } catch(IllegalArgumentException argumentException) {
            // Expected result.
        }
    }

}
