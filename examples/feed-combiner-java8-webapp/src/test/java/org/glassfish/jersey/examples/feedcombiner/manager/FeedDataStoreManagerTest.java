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

package org.glassfish.jersey.examples.feedcombiner.manager;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.glassfish.jersey.examples.feedcombiner.model.CombinedFeed;

import org.easymock.EasyMockRule;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;

/**
 * @author Petr Bouda (petr.bouda at oracle.com)
 */
public class FeedDataStoreManagerTest extends EasyMockSupport {

    @Rule
    public EasyMockRule mocks = new EasyMockRule(this);

    @Mock
    private ScheduledExecutorService scheduler;

    @Mock
    private FeedTaskFactory taskFactory;

    @Mock
    private ScheduledFuture scheduledFuture;

    private FeedDataStoreManager testedClass;

    @Before
    public void init() {
        testedClass = new FeedDataStoreManager(taskFactory, scheduler);
    }

    @Test
    @SuppressWarnings({"unchecked", "ConstantConditions"})
    public void testSaveSuccess() {
        long refreshPeriod = 60;

        CombinedFeed feed = new CombinedFeed.CombinedFeedBuilder("1", "http://localhost")
                .refreshPeriod(refreshPeriod).build();

        expect(scheduler.scheduleAtFixedRate(
                anyObject(Runnable.class), eq(0L), eq(60L), eq(TimeUnit.SECONDS))).andReturn(scheduledFuture);

        expect(taskFactory.get(feed)).andReturn(new FeedDownloadTask(null, null, null));

        replayAll();
        testedClass.save(feed);
        verifyAll();
    }

    @Test
    public void testSaveNoCombinedFeedEntity() {
        String combinedFeedId = "1";
        replayAll();
        testedClass.save(combinedFeedId);
        verifyAll();
    }

    @Test
    public void testSaveNull() {
        replayAll();
        testedClass.save(null);
        verifyAll();
    }

    @Test
    public void testRemoveSuccess() {
        replayAll();
        String combinedFeedId = "1";
        testedClass.save(combinedFeedId);
        testedClass.remove(combinedFeedId);
        verifyAll();
    }

}
