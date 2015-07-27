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

package org.glassfish.jersey.examples.feedcombiner.model;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Petr Bouda (petr.bouda at oracle.com)
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public final class CombinedFeed implements Serializable {

    private static final long serialVersionUID = 2176919801739913480L;

    private static Logger LOG = LoggerFactory.getLogger(CombinedFeed.class.getName());

    private String id;

    // UnmodifiableList
    @NotEmpty(message = "At least one valid URL must be in the request.")
    private List<URL> urls;

    @Min(value = 0, message = "Refresh period must be equal or higher than 0.")
    private long refreshPeriod;

    // UnmodifiableList
    private List<FeedEntry> feedEntries;

    @NotNull(message = "Title may not be null.")
    private String title;

    @NotNull(message = "Description may not be null.")
    private String description;

    // for purposes of JAXB
    private CombinedFeed() {
    }

    private CombinedFeed(String id, List<URL> urls, long refreshPeriod,
                         List<FeedEntry> feedEntries, String title, String description) {
        this.id = id;
        this.urls = urls;
        this.refreshPeriod = refreshPeriod;
        this.feedEntries = feedEntries;
        this.title = title;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public long getRefreshPeriod() {
        return refreshPeriod;
    }

    public List<URL> getUrls() {
        if (urls == null) {
            return Collections.unmodifiableList(Collections.emptyList());
        }
        return urls;
    }

    public List<FeedEntry> getFeedEntries() {
        if (feedEntries == null) {
            return Collections.unmodifiableList(Collections.emptyList());
        }
        return feedEntries;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CombinedFeed{");
        sb.append("id='").append(id).append('\'');
        sb.append(", urls=").append(urls);
        sb.append(", refreshPeriod=").append(refreshPeriod);
        sb.append(", feedEntries=").append(feedEntries);
        sb.append(", title='").append(title).append('\'');
        sb.append(", description='").append(description).append('\'');
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CombinedFeed feed = (CombinedFeed) obj;

        return Objects.equals(this.id, feed.id)
                && Objects.equals(this.title, feed.title)
                && Objects.equals(this.description, feed.description)
                && Objects.equals(this.refreshPeriod, feed.refreshPeriod)
                && Objects.deepEquals(this.urls, feed.urls)
                && Objects.deepEquals(this.feedEntries, feed.feedEntries);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, description, refreshPeriod, urls, feedEntries);
    }

    public static class CombinedFeedBuilder {

        private static final Function<String, URL> mapper = new UrlMapper();

        private String id;
        private List<URL> urls = Collections.unmodifiableList(new ArrayList<>());
        private String title;
        private String description;
        private long refreshPeriod;
        private List<FeedEntry> feedEntries = Collections.unmodifiableList(new ArrayList<>());

        public CombinedFeedBuilder(String id, String... urls) {
            this(id, createUrlList(urls));
        }

        public CombinedFeedBuilder(String id, List<URL> urls) {
            this.id = id;
            this.urls = Collections.unmodifiableList(urls);
        }

        public static CombinedFeedBuilder of(CombinedFeed feed) {
            CombinedFeedBuilder builder = new CombinedFeedBuilder(feed.getId(), feed.getUrls());
            builder.title = feed.getTitle();
            builder.description = feed.getDescription();
            builder.refreshPeriod = feed.getRefreshPeriod();
            builder.feedEntries = feed.getFeedEntries();
            return builder;
        }

        private static List<URL> createUrlList(String... urls) {
            return Arrays.stream(urls)
                    .map(mapper)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        public CombinedFeedBuilder id(String id) {
            this.id = id;
            return this;
        }

        public CombinedFeedBuilder urls(List<URL> urls) {
            if (urls == null) {
                this.urls = Collections.unmodifiableList(new ArrayList<>());
            } else {
                this.urls = Collections.unmodifiableList(urls);
            }
            return this;

        }

        public CombinedFeedBuilder title(String title) {
            this.title = title;
            return this;
        }

        public CombinedFeedBuilder description(String description) {
            this.description = description;
            return this;
        }

        public CombinedFeedBuilder refreshPeriod(long refreshPeriod) {
            this.refreshPeriod = refreshPeriod;
            return this;
        }

        public CombinedFeedBuilder feedEntries(List<FeedEntry> feedEntries) {
            if (feedEntries == null) {
                this.feedEntries = Collections.unmodifiableList(new ArrayList<>());
            } else {
                this.feedEntries = Collections.unmodifiableList(feedEntries);
            }
            return this;
        }

        public CombinedFeed build() {
            return new CombinedFeed(id, urls, refreshPeriod, feedEntries, title, description);
        }
    }

    private static final class UrlMapper implements Function<String, URL> {

        @Override
        public URL apply(String url) {
            try {
                return new URL(url);
            } catch (MalformedURLException mue) {
                LOG.warn("It is not possible to create URL object from:" + url, mue);
            }
            return null;
        }
    }
}
