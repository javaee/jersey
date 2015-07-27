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
package org.glassfish.jersey.examples.feedcombiner.store;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.glassfish.jersey.examples.feedcombiner.model.CombinedFeed;
import org.glassfish.jersey.examples.feedcombiner.model.FeedEntry;

import org.apache.commons.lang3.SerializationUtils;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.EasyMockRule;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.easymock.MockType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import static org.easymock.EasyMock.capture;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import jersey.repackaged.com.google.common.base.Objects;

/**
 * @author Petr Bouda (petr.bouda at oracle.com)
 */
public class ReadWriteLockDataStoreTest extends EasyMockSupport {

    @Rule
    public EasyMockRule mocks = new EasyMockRule(this);

    @Mock(type = MockType.STRICT)
    private DataStoreObserver observer;

    private ReadWriteLockDataStore testedClass;

    @Before
    public void init() {
        testedClass = new ReadWriteLockDataStore();
        testedClass.addObserver(observer);
    }

    @Test
    public void testSaveEntity() {
        String id = "1";
        CombinedFeed feed = new CombinedFeed.CombinedFeedBuilder(id, "http://localhost")
                .title("title")
                .description("description")
                .refreshPeriod(5L).build();

        observer.save(feed);
        replayAll();

        Serializable previousEntity = testedClass.put(id, feed);

        verifyAll();
        assertNull(previousEntity);
    }

    @Test
    public void testUpdateEntity() {
        String id = "1";
        CombinedFeed feed = getCombinedFeed(id);

        observer.save(feed);
        replayAll();

        // save entity
        testedClass.put(id, feed);

        CombinedFeed updatedFeed = CombinedFeed.CombinedFeedBuilder.of(feed)
                .title("updated_title")
                .description("updated_description")
                .build();
        // update entity
        Serializable previousEntity = testedClass.put(id, updatedFeed);
        verifyAll();

        if (!(previousEntity instanceof CombinedFeed)) {
            fail("The previous entity is not an instance of CombinedFeed");
        }

        assertEquals(feed, previousEntity);
    }

    @Test
    public void testRemoveEntity() {
        String id = "1";
        CombinedFeed feed = getCombinedFeed(id);

        observer.save(feed);
        observer.remove(id);
        replayAll();

        // save entity
        testedClass.put(id, feed);

        Serializable removedEntity = testedClass.put(id, null);
        verifyAll();

        if (!(removedEntity instanceof CombinedFeed)) {
            fail("The previous entity is not an instance of CombinedFeed");
        }

        assertEquals(feed, removedEntity);
    }

    @Test(expected = NullPointerException.class)
    public void testSaveEntityWithNullKey() {
        String id = null;
        CombinedFeed feed = getCombinedFeed(id);

        observer.save(feed);
        replayAll();

        // save entity
        testedClass.put(id, feed);
        verifyAll();
    }

    @Test
    public void testGetEntity() {
        String id = "1";
        CombinedFeed feed = getCombinedFeed(id);
        observer.save(feed);
        replayAll();

        // save entity
        testedClass.put(id, feed);

        CombinedFeed fetchedFeed = testedClass.get(id, CombinedFeed.class);

        verifyAll();
        assertEquals(feed, fetchedFeed);
    }

    @Test(expected = NullPointerException.class)
    public void testGetEntityNullKey() {
        testedClass.get(null, CombinedFeed.class);
    }

    @Test(expected = NullPointerException.class)
    public void testGetEntityNullClass() {
        testedClass.get("1", null);
    }

    @Test
    public void testGetNullEntity() {
        CombinedFeed combinedFeed = testedClass.get("1", CombinedFeed.class);
        assertNull(combinedFeed);
    }

    @Test(expected = ClassCastException.class)
    public void testGetEntityWrongCast() {
        String id = "1";
        CombinedFeed feed = getCombinedFeed(id);
        observer.save(feed);
        replayAll();

        // save entity
        testedClass.put(id, feed);

        testedClass.get(id, FeedEntry.class);
        verifyAll();
    }

    @Test
    public void saveDatastoreIsNotClosed() {
        try {
            testedClass.save(new OutputStreamStub());
        } catch (UnsupportedOperationException e) {
            fail("It is not allowed to call a method close on OutputStream");
        } catch (IOException e) {
            fail("Unexpected Error");
        }
    }

    @Test
    public void loadDatastoreIsNotClosed() {
        try {
            byte[] serializedDatastore = SerializationUtils.serialize(new HashMap<String, Serializable>());

            testedClass.load(new InputStreamStub(serializedDatastore));
        } catch (UnsupportedOperationException e) {
            fail("It is not allowed to call a method close on InputStream");
        } catch (IOException e) {
            fail("Unexpected Error");
        }
    }

    @Test
    public void testSaveDatastore() {
        // Insert new combined Feed
        String id = "1";
        CombinedFeed feed = new CombinedFeed.CombinedFeedBuilder(id, "http://localhost")
                .title("title")
                .description("description")
                .refreshPeriod(5L).build();

        observer.save(feed);
        replayAll();

        testedClass.put(id, feed);

        // Copy the datastore into the stream
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            testedClass.save(output);

            // Deserialize the datastore
            Map<String, Serializable> entities =
                    SerializationUtils.<Map<String, Serializable>>deserialize(output.toByteArray());

            // Check whether is the saved entity was deserialized successfully or not
            Serializable serializableEntity = entities.get(id);
            if (serializableEntity instanceof CombinedFeed) {
                CombinedFeed fetchedFeed = (CombinedFeed) serializableEntity;
                assertEquals(feed, fetchedFeed);
            } else {
                fail("Deserialized entity is not CombinedFeed Class");
            }
        } catch (IOException e) {
            fail(e.getMessage());
        }
        verifyAll();
    }

    @Test
    public void testLoadDatastore() {
        // Insert new combined Feed
        String id = "1";
        CombinedFeed feed = new CombinedFeed.CombinedFeedBuilder(id, "http://localhost")
                .title("title")
                .description("description")
                .refreshPeriod(5L).build();

        // call save mock
        observer.save(feed);

        // Create a datastore with one Combined Feed and serialize it
        String id2 = "2";
        CombinedFeed deserializedFeed = new CombinedFeed.CombinedFeedBuilder(id2, "http://localhost")
                .title("deserialized_title")
                .description("deserialized_description")
                .refreshPeriod(5L).build();

        HashMap<String, Serializable> datastore = new HashMap<>();
        datastore.put(id2, deserializedFeed);

        // call mocks after a load of the datastore
        Capture<Set<String>> captureRemoveAll = EasyMock.newCapture();
        Capture<Collection<Serializable>> captureSaveAll = EasyMock.newCapture();
        observer.removeAll(capture(captureRemoveAll));
        observer.saveAll(capture(captureSaveAll));

        replayAll();

        // Insert feed into the old datastore
        testedClass.put(id, feed);

        byte[] serializedDatastore = SerializationUtils.serialize(datastore);

        // Load the serialized datastore
        ByteArrayInputStream input = new ByteArrayInputStream(serializedDatastore);
        try {
            testedClass.load(input);

            // Test that the new datastore does not contain old Combined Feed
            CombinedFeed previousCombinedFeed = testedClass.get(id, CombinedFeed.class);
            if (previousCombinedFeed != null) {
                fail("The previous combined feed should be deleted.");
            }

            // Test that the new datastore contains new Combined Feed
            CombinedFeed newCombinedFeed = testedClass.get(id2, CombinedFeed.class);
            assertEquals(deserializedFeed, newCombinedFeed);
        } catch (IOException e) {
            fail(e.getMessage());
        }

        verifyAll();

        // Verify captured values
        // Check whether the registered observer was called because of removing entities
        Set<String> previousKeySet = captureRemoveAll.getValue();
        if (!previousKeySet.contains(id)) {
            fail("The previous keys should be deleted.");
        }

        // Check whether the registered observer was called because of saving entities
        Collection<Serializable> newlySavedEntities = captureSaveAll.getValue();
        assertEquals(1, newlySavedEntities.size());
        boolean exists = newlySavedEntities.stream()
                .map(CombinedFeed.class::cast)
                .anyMatch(entity -> Objects.equal(entity.getId(), id2));
        if (!exists) {
            fail("The new stored CombinedFeed was not found.");
        }
    }

    @Test
    public void testLoadCastException() {
        byte[] serializedObject = SerializationUtils.serialize("Wrong object");
        ByteArrayInputStream input = new ByteArrayInputStream(serializedObject);

        try {
            testedClass.load(input);
        } catch (IOException e) {
            fail("Any IOException should not occur.");
        }
    }

    private CombinedFeed getCombinedFeed(String id) {
        return new CombinedFeed.CombinedFeedBuilder(id, "http://localhost")
                .title("title")
                .description("description")
                .refreshPeriod(5L).build();
    }
}
