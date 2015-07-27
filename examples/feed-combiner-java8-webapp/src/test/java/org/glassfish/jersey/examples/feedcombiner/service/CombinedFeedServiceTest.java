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

package org.glassfish.jersey.examples.feedcombiner.service;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import org.glassfish.jersey.examples.feedcombiner.generator.IdGenerator;
import org.glassfish.jersey.examples.feedcombiner.model.CombinedFeed;
import org.glassfish.jersey.examples.feedcombiner.store.ReadWriteLockDataStore;

import org.easymock.EasyMockRule;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

/**
 * @author Petr Bouda (petr.bouda at oracle.com)
 */
public class CombinedFeedServiceTest extends EasyMockSupport {

    private static final String DEFAULT_REFRESH_PERIOD = "10";
    @Rule
    public EasyMockRule mocks = new EasyMockRule(this);
    @Mock
    private ReadWriteLockDataStore datastore;
    @Mock
    private IdGenerator idGenerator;
    private CombinedFeedService testedClass;

    @Before
    public void init() {
        testedClass = new CombinedFeedService(datastore, idGenerator, DEFAULT_REFRESH_PERIOD);
    }

    @Test
    public void testSaveWithRefreshPeriod() {
        String id = "1";

        CombinedFeed feed = new CombinedFeed.CombinedFeedBuilder(id, "http://localhost")
                .title("title").description("description").refreshPeriod(10).build();

        expect(idGenerator.getId()).andReturn(id);
        expect(datastore.put(eq(id), eq(feed))).andReturn(null);
        replayAll();

        CombinedFeed actual = testedClass.save(feed);

        verifyAll();
        assertEquals(feed, actual);
    }

    @Test
    public void testSaveWithoutRefreshPeriod() {
        String id = "1";

        CombinedFeed feed = new CombinedFeed.CombinedFeedBuilder(id, "http://localhost")
                .title("title").description("description").build();

        CombinedFeed expected = CombinedFeed.CombinedFeedBuilder.of(feed)
                .refreshPeriod(Long.parseLong(DEFAULT_REFRESH_PERIOD)).build();

        expect(idGenerator.getId()).andReturn(id);
        expect(datastore.put(eq(id), eq(expected))).andReturn(null);
        replayAll();

        CombinedFeed actual = testedClass.save(feed);

        verifyAll();
        assertEquals(expected, actual);
    }

    @Test
    public void testDelete() {
        String feedId = "1";
        expect(datastore.put(feedId, null)).andReturn(null);
        replayAll();
        testedClass.delete(feedId);
        verifyAll();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUpdate() {
        testedClass.update(null);
    }

    @Test
    public void testGet() {
        String feedId = "1";
        expect(datastore.get(feedId, CombinedFeed.class)).andReturn(null);
        replayAll();
        testedClass.get(feedId);
        verifyAll();
    }

    @Test
    public void testGetAll() {
        ReadWriteLockDataStore datastoreMock =
                createMock("datastore", ReadWriteLockDataStore.class);

        testedClass = new CombinedFeedService(datastoreMock, idGenerator, DEFAULT_REFRESH_PERIOD);

        CombinedFeed feed = new CombinedFeed.CombinedFeedBuilder("1", "http://localhost")
                .title("title").description("description").build();

        List<Serializable> entities = Arrays.asList(feed, "fakeObject");

        expect(datastoreMock.getAll()).andReturn(entities);
        replay(datastoreMock);
        List<CombinedFeed> actual = testedClass.getAll();
        verify(datastoreMock);
        assertEquals(1, actual.size());
        assertEquals(feed, actual.get(0));
    }
}
