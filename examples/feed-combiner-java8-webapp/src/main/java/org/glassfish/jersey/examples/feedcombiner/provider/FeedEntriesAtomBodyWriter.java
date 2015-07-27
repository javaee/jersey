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

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.glassfish.jersey.examples.feedcombiner.model.FeedEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedOutput;

/**
 * Provides functionality to convert java object to ATOM representation.
 *
 * @author Petr Bouda (petr.bouda at oracle.com)
 */
@Provider
@Produces(MediaType.APPLICATION_ATOM_XML)
public class FeedEntriesAtomBodyWriter implements MessageBodyWriter<List<FeedEntry>> {

    private static Logger LOG = LoggerFactory.getLogger(FeedEntriesAtomBodyWriter.class.getName());

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return MediaType.APPLICATION_ATOM_XML_TYPE.isCompatible(mediaType)
                && Collection.class.isAssignableFrom(type)
                && (((ParameterizedType) genericType).getActualTypeArguments()[0]).equals(FeedEntry.class);
    }

    @Override
    public void writeTo(List<FeedEntry> entries, Class<?> type, Type genericType, Annotation[] annotations,
                        MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
            throws IOException, WebApplicationException {

        List<SyndEntry> syndEntries = entries.parallelStream().map(entry -> {
            SyndContent description = new SyndContentImpl();
            description.setType(MediaType.TEXT_PLAIN);
            description.setValue(entry.getDescription());

            SyndEntry syndEntry = new SyndEntryImpl();
            syndEntry.setTitle(entry.getTitle());
            syndEntry.setLink(entry.getLink());
            syndEntry.setPublishedDate(entry.getPublishDate());
            syndEntry.setDescription(description);
            return syndEntry;
        }).collect(Collectors.toList());

        SyndFeed feed = new SyndFeedImpl();
        feed.setFeedType("atom_1.0");
        feed.setTitle("Combined Feed");
        feed.setDescription("Combined Feed created by a feed-combiner application");
        feed.setPublishedDate(new Date());
        feed.setEntries(syndEntries);

        writeSyndFeed(entityStream, feed);
    }

    @Override
    public long getSize(List<FeedEntry> feeds, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        // deprecated by JAX-RS 2.0 and ignored by Jersey runtime
        return 0;
    }

    private void writeSyndFeed(OutputStream entityStream, SyndFeed feed) {
        try {
            OutputStreamWriter writer = new OutputStreamWriter(entityStream);
            SyndFeedOutput output = new SyndFeedOutput();
            output.output(feed, writer);
            writer.close();
        } catch (FeedException | IOException e) {
            LOG.warn("An error occurred during writing a synd feed.", e);
        }
    }

}
