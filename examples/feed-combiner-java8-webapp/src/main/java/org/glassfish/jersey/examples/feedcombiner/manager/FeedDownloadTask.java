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

import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.glassfish.jersey.examples.feedcombiner.model.CombinedFeed;
import org.glassfish.jersey.examples.feedcombiner.model.FeedEntry;
import org.glassfish.jersey.examples.feedcombiner.store.InMemoryDataStore;

import org.slf4j.LoggerFactory;
import static org.glassfish.jersey.examples.feedcombiner.model.CombinedFeed.CombinedFeedBuilder.of;

import com.sun.syndication.feed.synd.SyndEntry;

/**
 * An instance of this class implements the {@link Runnable#run() run method}
 * with functionality of downloading all entries from feed which are available.
 * These entries are added into the combined feed and stored in the database again
 * (storing means rewrite previous combined feed by a new version with added entries).
 *
 * @author Petr Bouda (petr.bouda at oracle.com)
 */
public class FeedDownloadTask implements Runnable {

    private static org.slf4j.Logger LOG = LoggerFactory.getLogger(FeedDownloadTask.class.getName());

    private final FeedEntryMapper entryMapper = new FeedEntryMapper();

    private final Function<URL, List<SyndEntry>> feedDownloader;
    private final InMemoryDataStore datastore;
    private final String combinedFeedID;

    FeedDownloadTask(Function<URL, List<SyndEntry>> feedDownloader, InMemoryDataStore datastore, String feedId) {
        this.combinedFeedID = feedId;
        this.datastore = datastore;
        this.feedDownloader = feedDownloader;
    }

    @Override
    public void run() {
        CombinedFeed fetchedCombinedFeed = datastore.get(combinedFeedID, CombinedFeed.class);
        if (fetchedCombinedFeed == null) {
            LOG.warn("There is no CombinedFeed for the ID: " + combinedFeedID);
            return;
        }

        List<FeedEntry> entries = fetchedCombinedFeed.getUrls().stream()
                .map(feedDownloader)
                .flatMap(Collection::stream)
                .map(entryMapper)
                .filter(Objects::nonNull)
                .sorted((e1, e2) -> e2.getPublishDate().compareTo(e1.getPublishDate()))
                .collect(Collectors.toList());

        CombinedFeed combinedFeed = of(fetchedCombinedFeed).feedEntries(entries).build();
        datastore.put(combinedFeedID, combinedFeed);

        LOG.debug("New entries for the CombinedFeed were downloaded [combined-feed={}, entries-count={}]",
                combinedFeed.getId(), entries.size());
    }

    /**
     * The mapper which is used for transform {@link SyndEntry synd entry} to
     * this application-friendly version {@link FeedEntry feed entry}.
     *
     * @author Petr Bouda (petr.bouda at oracle.com)
     **/
    private static class FeedEntryMapper implements Function<SyndEntry, FeedEntry> {

        @Override
        public FeedEntry apply(SyndEntry entry) {
            if (entry.getDescription() == null) {
                return null;
            }

            String title = entry.getTitle();
            String link = entry.getLink();
            String description = entry.getDescription().getValue();
            Date publishedDate = entry.getPublishedDate();
            return new FeedEntry(title, link, description, publishedDate);
        }
    }
}
