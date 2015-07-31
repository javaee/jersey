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
package org.glassfish.jersey.media.multipart.file;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.media.multipart.BodyPartTest;
import org.glassfish.jersey.media.multipart.ContentDisposition;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * Tests for the {@link StreamDataBodyPart} class which checks the class' main
 * contract and new functionality.
 *
 * @author Pedro Kowalski (pallipp at gmail.com)
 * @author Michal Gajdos
 *
 * @see StreamDataBodyPart
 * @see FileDataBodyPartTest
 */
public class StreamDataBodyPartTest extends BodyPartTest {

    /**
     * Class under test.
     */
    private StreamDataBodyPart cut;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        cut = new StreamDataBodyPart();

        // Needed for inherited tests.
        bodyPart = cut;
    }

    @Override
    @After
    public void tearDown() throws Exception {
        bodyPart = null;
        super.tearDown();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Not supported methods
    ///////////////////////////////////////////////////////////////////////////

    @Override
    @Test
    public void testEntity() {
        try {
            bodyPart.setEntity("foo");
            fail();
        } catch (UnsupportedOperationException ex) {
            // expected exception.
        }
    }

    @Test
    public void testSetValueString() {
        try {
            cut.setValue("foo");
            fail();
        } catch (UnsupportedOperationException ex) {
            // expected exception.
        }
    }

    @Test
    public void testSetValueMediaTypeObject() {
        try {
            // Use any MediaType and value - they doesn't matter.
            cut.setValue(MediaType.APPLICATION_OCTET_STREAM_TYPE, new Object());
            fail();
        } catch (UnsupportedOperationException ex) {
            // expected exception.
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Constructor tests
    ///////////////////////////////////////////////////////////////////////////

    @Test
    public void testCreateStreamEntityNameStream() {
        String expectedName = "foo";
        MediaType expectedMediaType = StreamDataBodyPart.getDefaultMediaType();

        cut = new StreamDataBodyPart(expectedName, new ByteArrayInputStream(new byte[] {}));

        boolean actualIsSimpleBodyPart = cut.isSimple();
        String actualName = cut.getName();

        // Filename and mediaType are to be the default ones.
        String actualFilename = cut.getFilename();
        MediaType actualMediaType = cut.getMediaType();

        assertEquals(expectedName, actualName);
        assertEquals(expectedMediaType, actualMediaType);
        assertNull(actualFilename);
        assertFalse(actualIsSimpleBodyPart);
    }

    @Test
    public void testCreateStreamEntityNameStreamFilename() {
        String expectedName = "foo";
        String expectedFilename = "bar.txt";
        MediaType expectedMediaType = StreamDataBodyPart.getDefaultMediaType();

        cut = new StreamDataBodyPart(expectedName, new ByteArrayInputStream(new byte[] {}), expectedFilename);

        boolean actualIsSimpleBodyPart = cut.isSimple();
        String actualName = cut.getName();
        String actualFilename = cut.getFilename();

        // MediaType is to be the default one.
        MediaType actualMediaType = cut.getMediaType();

        assertEquals(expectedName, actualName);
        assertEquals(expectedMediaType, actualMediaType);
        assertEquals(expectedFilename, actualFilename);
        assertFalse(actualIsSimpleBodyPart);
    }

    @Test
    public void testCreateStreamEntity() {
        String expectedName = "foo";
        String expectedFilename = "bar.txt";
        MediaType expectedMediaType = MediaType.TEXT_HTML_TYPE;

        cut = new StreamDataBodyPart(expectedName, new ByteArrayInputStream(new byte[] {}), expectedFilename, expectedMediaType);

        // All parameters must be set as the user requested. No defaults.
        boolean actualIsSimpleBodyPart = cut.isSimple();
        String actualName = cut.getName();
        String actualFilename = cut.getFilename();
        MediaType actualMediaType = cut.getMediaType();

        assertEquals(expectedName, actualName);
        assertEquals(expectedMediaType, actualMediaType);
        assertEquals(expectedFilename, actualFilename);
        assertFalse(actualIsSimpleBodyPart);
    }

    @Test
    public void testCreateStreamEntityNullName() {
        try {
            new StreamDataBodyPart(null, new ByteArrayInputStream(new byte[] {}));
            fail();
        } catch (IllegalArgumentException ex) {
            // expected exception.
        }
    }

    @Test
    public void testCreateStreamEntityNullStream() {
        try {
            new StreamDataBodyPart("foo", null);
            fail();
        } catch (IllegalArgumentException ex) {
            // expected exception.
        }
    }

    @Test
    public void testCreateStreamEntityNullMediaType() {
        MediaType expectedMediaType = StreamDataBodyPart.getDefaultMediaType();

        // MediaType is nullable - it takes the default value in such situation.
        cut = new StreamDataBodyPart("foo", new ByteArrayInputStream(new byte[] {}), "bar.txt", null);

        MediaType actualMediaType = cut.getMediaType();

        assertEquals(expectedMediaType, actualMediaType);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Content disposition building tests
    ///////////////////////////////////////////////////////////////////////////

    @Test
    public void testBuildContentDisposition() {
        String name = "foo";
        String expectedType = "form-data";
        String expectedFilename = "bar.txt";

        cut.setName(name);
        cut.setFilename(expectedFilename);

        ContentDisposition actual = cut.buildContentDisposition();

        assertEquals(expectedType, actual.getType());
        assertEquals(expectedFilename, actual.getFileName());
    }

    @Test
    public void testBuildContentDispositionWithoutFilename() {
        String name = "foo";
        String expectedType = "form-data";
        String expectedFilename = "foo";

        cut.setName(name);

        ContentDisposition actual = cut.buildContentDisposition();

        assertEquals(expectedType, actual.getType());
        assertEquals(expectedFilename, actual.getFileName());
    }

    ///////////////////////////////////////////////////////////////////////////
    // Stream entity setter tests
    ///////////////////////////////////////////////////////////////////////////

    @Test
    public void testStreamEntityStreamMediaType() {
        MediaType expectedMediaType = MediaType.APPLICATION_SVG_XML_TYPE;
        InputStream expectedInputStream = new ByteArrayInputStream(new byte[] {0x002, 0x003});

        assertSetEntityStream(expectedMediaType, expectedInputStream);
    }

    @Test
    public void testStreamEntityStream() {
        MediaType expectedMediaType = StreamDataBodyPart.getDefaultMediaType();
        InputStream expectedInputStream = new ByteArrayInputStream(new byte[] {0x002, 0x003});

        assertSetEntityStream(expectedMediaType, expectedInputStream);
    }

    @Test
    public void testStreamEntityStreamNullStream() {
        try {
            cut.setStreamEntity(null);
            fail();
        } catch (IllegalArgumentException ex) {
            // expected exception.
        }
    }

    @Test
    public void testStreamEntityStreamNullMediaType() {
        MediaType expectedMediaType = StreamDataBodyPart.getDefaultMediaType();

        // Required to set the entity.
        cut.setName("foo");

        // No exception is to be observed - MediaType should be the default one.
        cut.setStreamEntity(new ByteArrayInputStream(new byte[] {}), null);

        MediaType actualMediaType = cut.getMediaType();

        assertEquals(expectedMediaType, actualMediaType);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Misc tests
    ///////////////////////////////////////////////////////////////////////////

    @Test
    public void testGetDefaultMediaType() {
        MediaType expected = MediaType.APPLICATION_OCTET_STREAM_TYPE;
        MediaType actual = StreamDataBodyPart.getDefaultMediaType();

        assertEquals(expected, actual);
    }

    /**
     * Helper class for checking if the stream entity has been properly set.
     *
     * @param expectedMediaType
     *            after the stream entity has been set.
     * @param expectedStreamEntity
     *            after the stream entity has been set.
     */
    private void assertSetEntityStream(MediaType expectedMediaType,
                                       InputStream expectedStreamEntity) {

        // Required to set the entity.
        cut.setName("foo");

        cut.setStreamEntity(expectedStreamEntity, expectedMediaType);

        MediaType actualMediaType = cut.getMediaType();
        InputStream actualInputStream = cut.getStreamEntity();

        assertEquals(expectedStreamEntity, actualInputStream);
        assertEquals(expectedMediaType, actualMediaType);
    }

}
