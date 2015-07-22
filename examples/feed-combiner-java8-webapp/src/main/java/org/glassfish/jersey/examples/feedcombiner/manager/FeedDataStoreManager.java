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

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.glassfish.jersey.examples.feedcombiner.model.CombinedFeed;
import org.glassfish.jersey.examples.feedcombiner.store.DataStoreObserver;
import org.glassfish.jersey.examples.feedcombiner.store.InMemoryDataStore;

/**
 * This class is used for purposes of processing and listening a new saved or
 * removed entities in {@link InMemoryDataStore} and according to
 * its {@link CombinedFeed#getRefreshPeriod()} property is able to give an entity
 * into the queue for another processing which is dedicated to consumers.
 *
 * @author Petr Bouda (petr.bouda at oracle.com)
 */
public class FeedDataStoreManager implements DataStoreObserver {

    private static final long START_DELAY = 0L;

    private static final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.SECONDS;

    private final ScheduledExecutorService scheduler;

    private final FeedTaskFactory taskFactory;

    // Concurrent Map used for purposes of cancellation tasks in the given executor
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    public FeedDataStoreManager(FeedTaskFactory taskFactory) {
        this(taskFactory, Executors.newScheduledThreadPool(1));
    }

    public FeedDataStoreManager(FeedTaskFactory taskFactory, ScheduledExecutorService scheduler) {
        this.taskFactory = taskFactory;
        this.scheduler = scheduler;
    }

    @Override
    public void save(Serializable entity) {
        createTask(entity);
    }

    @Override
    public void saveAll(Collection<Serializable> entities) {
        entities.forEach(this::createTask);
    }

    private void createTask(Serializable serializableEntity) {
        if (serializableEntity instanceof CombinedFeed) {
            CombinedFeed feed = (CombinedFeed) serializableEntity;
            ScheduledFuture<?> scheduledTask = scheduler.scheduleAtFixedRate(
                    taskFactory.get(feed), START_DELAY, feed.getRefreshPeriod(), DEFAULT_TIME_UNIT);
            scheduledTasks.put(feed.getId(), scheduledTask);
        }
    }

    @Override
    public void remove(String key) {
        removeTask(key);
    }

    @Override
    public void removeAll(Collection<String> keys) {
        keys.forEach(this::removeTask);
    }

    private void removeTask(String key) {
        ScheduledFuture<?> scheduledTask = scheduledTasks.get(key);
        if (scheduledTask != null) {
            scheduledTask.cancel(true);
            scheduledTasks.remove(key);
        }
    }

}
