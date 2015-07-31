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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.glassfish.jersey.examples.feedcombiner.generator.IdGenerator;
import org.glassfish.jersey.examples.feedcombiner.model.CombinedFeed;
import org.glassfish.jersey.examples.feedcombiner.store.InMemoryDataStore;
import org.glassfish.jersey.examples.feedcombiner.store.ReadWriteLockDataStore;

import static org.glassfish.jersey.examples.feedcombiner.ApplicationProperties.DEFAULT_REFRESH_PERIOD;
import static org.glassfish.jersey.examples.feedcombiner.model.CombinedFeed.CombinedFeedBuilder.of;

/**
 * @author Petr Bouda (petr.bouda at oracle.com)
 */
public class CombinedFeedService implements CrudService<CombinedFeed> {

    private InMemoryDataStore datastore;

    private IdGenerator idGenerator;

    private String defaultRefreshPeriod;

    @Inject
    public CombinedFeedService(InMemoryDataStore datastore, IdGenerator idGenerator,
                               @Named(DEFAULT_REFRESH_PERIOD) String defaultRefreshPeriod) {
        this.datastore = datastore;
        this.idGenerator = idGenerator;
        this.defaultRefreshPeriod = defaultRefreshPeriod;
    }

    @Override
    public CombinedFeed save(CombinedFeed insertedFeed) {
        String entityId = idGenerator.getId();

        CombinedFeed.CombinedFeedBuilder feedBuilder = of(insertedFeed).id(entityId).feedEntries(null);
        if (insertedFeed.getRefreshPeriod() == 0) {
            feedBuilder.refreshPeriod(Long.parseLong(defaultRefreshPeriod));
        }

        CombinedFeed combinedFeed = feedBuilder.build();
        datastore.put(entityId, combinedFeed);
        return combinedFeed;
    }

    @Override
    public Serializable delete(String feedId) {
        return datastore.put(feedId, null);
    }

    @Override
    public CombinedFeed update(CombinedFeed feed) {
        throw new UnsupportedOperationException("This operation is not implemented yet.");
    }

    @Override
    public CombinedFeed get(String feedId) {
        return datastore.get(feedId, CombinedFeed.class);
    }

    @Override
    public List<CombinedFeed> getAll() {
        //TODO ugly, for purposes of CRUD controller
        if (datastore instanceof ReadWriteLockDataStore) {
            ReadWriteLockDataStore rwDatastore = (ReadWriteLockDataStore) datastore;
            Collection<Serializable> entities = rwDatastore.getAll();
            return entities.parallelStream()
                    .filter(entity -> entity instanceof CombinedFeed)
                    .map(CombinedFeed.class::cast)
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}
