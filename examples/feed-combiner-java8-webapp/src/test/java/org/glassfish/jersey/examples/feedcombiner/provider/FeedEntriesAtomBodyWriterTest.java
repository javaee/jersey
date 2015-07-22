/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.examples.feedcombiner.provider;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.examples.feedcombiner.model.FeedEntry;

import org.glassfish.hk2.utilities.reflection.ParameterizedTypeImpl;

import org.easymock.EasyMockRule;
import org.easymock.TestSubject;
import org.junit.Rule;
import org.junit.Test;
import org.xml.sax.InputSource;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;

/**
 * @author Petr Bouda (petr.bouda at oracle.com)
 */
public class FeedEntriesAtomBodyWriterTest {

    private static final Date DATE = new Date();

    private static final String[] TITLES = {"title1", "title2"};
    private static final String[] LINKS = {"link1", "link2"};
    private static final String[] DESCS = {"description1", "description2"};

    @Rule
    public EasyMockRule mocks = new EasyMockRule(this);

    @TestSubject
    private FeedEntriesAtomBodyWriter testedClass = new FeedEntriesAtomBodyWriter();

    @Test
    public void testWriteTo() throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        testedClass.writeTo(feedEntries(), null, null, null, null, null, outputStream);

        SyndFeedInput input = new SyndFeedInput();
        InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        SyndFeed feed = input.build(new InputSource(inputStream));

        feed.setFeedType("atom_1.0");
        feed.setTitle("Combined Feed");
        feed.setDescription("Combined Feed created by a feed-combiner application");

        assertEquals("atom_1.0", feed.getFeedType());
        assertEquals("Combined Feed", feed.getTitle());
        assertEquals("Combined Feed created by a feed-combiner application", feed.getDescription());

        @SuppressWarnings("unchecked")
        List<SyndEntry> entries = feed.getEntries();
        assertEquals(2, entries.size());
        for (SyndEntry entry : entries) {
            if (TITLES[0].equals(entry.getTitle())) {
                assertEquals(entry.getLink(), LINKS[0]);
                assertEquals(entry.getTitle(), TITLES[0]);
                assertEquals(entry.getDescription().getValue(), DESCS[0]);
                assertEquals(entry.getPublishedDate().toString(), DATE.toString());
            } else {
                assertEquals(entry.getLink(), LINKS[1]);
                assertEquals(entry.getTitle(), TITLES[1]);
                assertEquals(entry.getDescription().getValue(), DESCS[1]);
                assertEquals(entry.getPublishedDate().toString(), DATE.toString());
            }
        }
    }

    private List<FeedEntry> feedEntries() {
        return Arrays.asList(new FeedEntry(TITLES[0], LINKS[0], DESCS[0], DATE),
                new FeedEntry(TITLES[1], LINKS[1], DESCS[1], DATE));
    }

    @Test
    public void testIsWriteableSuccess() {
        boolean writeable = testedClass.isWriteable(List.class, new ParameterizedTypeImpl(FeedEntry.class,
                        FeedEntry.class),
                null, MediaType.APPLICATION_ATOM_XML_TYPE);
        assertTrue(writeable);
    }

    @Test
    public void testIsWriteableWrongClass() {
        boolean writeable = testedClass.isWriteable(String.class, new ParameterizedTypeImpl(FeedEntry.class,
                        FeedEntry.class),
                null, MediaType.APPLICATION_ATOM_XML_TYPE);
        assertFalse(writeable);
    }

    @Test
    public void testIsWriteableWrongGeneric() {
        boolean writeable = testedClass.isWriteable(List.class, new ParameterizedTypeImpl(String.class, String.class),
                null, MediaType.APPLICATION_ATOM_XML_TYPE);
        assertFalse(writeable);
    }

    @Test
    public void testIsWriteableWrongMediaType() {
        boolean writeable = testedClass.isWriteable(List.class, new ParameterizedTypeImpl(FeedEntry.class,
                        FeedEntry.class),
                null, MediaType.APPLICATION_JSON_TYPE);
        assertFalse(writeable);
    }
}
