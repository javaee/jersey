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

package org.glassfish.jersey.examples.feedcombiner.resources;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.examples.feedcombiner.ApplicationProperties;
import org.glassfish.jersey.examples.feedcombiner.MyApplication;
import org.glassfish.jersey.examples.feedcombiner.binder.ApplicationBinder.PropertiesBinder;
import org.glassfish.jersey.examples.feedcombiner.binder.ApplicationBinder.ResourcePartBinder;
import org.glassfish.jersey.examples.feedcombiner.model.CombinedFeed;
import org.glassfish.jersey.examples.feedcombiner.model.FeedEntry;
import org.glassfish.jersey.examples.feedcombiner.store.InMemoryDataStore;
import org.glassfish.jersey.examples.feedcombiner.store.ReadWriteLockDataStore;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.Before;
import org.junit.Test;
import static org.glassfish.jersey.examples.feedcombiner.resources.CombinedFeedTestHelper.combinedFeed;
import static org.glassfish.jersey.examples.feedcombiner.resources.CombinedFeedTestHelper.feedEntries;
import static org.junit.Assert.assertEquals;

import static javax.ws.rs.core.MediaType.APPLICATION_ATOM_XML_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

/**
 * @author Petr Bouda (petr.bouda at oracle.com)
 */
public class CombinedFeedResourceTest extends JerseyTest {

    private static final String RESOURCE_URI = "feeds";

    private WebTarget target;
    private ReadWriteLockDataStore datastore;

    @Override
    protected Application configure() {
        enable(TestProperties.LOG_TRAFFIC);
        enable(TestProperties.DUMP_ENTITY);

        datastore = new ReadWriteLockDataStore();
        return new MyApplication(datastore, false);
    }

    @Before
    public void init() throws IOException {
        datastore.load(serializedDatastore());
        target = client().target(UriBuilder.fromUri(getBaseUri()).path(RESOURCE_URI).build());
    }

    private InputStream serializedDatastore() {
        HashMap<String, Serializable> datastore = new HashMap<>();
        byte[] serialized = SerializationUtils.serialize(datastore);
        return new ByteArrayInputStream(serialized);
    }

    @Test
    public void testCreate() {
        CombinedFeed feed = combinedFeed("1");

        Entity<CombinedFeed> entity = Entity.entity(feed, APPLICATION_JSON_TYPE);
        Response response = target.request(MediaType.APPLICATION_JSON).post(entity);

        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        assertEquals(feed, response.readEntity(CombinedFeed.class));
    }

    @Test
    public void testDelete() {
        // Saving entity
        CombinedFeed feed = combinedFeed("1");
        datastore.put(feed.getId(), feed);

        // Deleting entity
        Response deleteResp = target.path(feed.getId()).request().delete();
        assertEquals(Status.NO_CONTENT.getStatusCode(), deleteResp.getStatus());
    }

    @Test
    public void testDeleteNotFound() {
        // Saving entity
        CombinedFeed feed = combinedFeed("1");
        datastore.put(feed.getId(), feed);

        // Deleting entity
        Response deleteResp = target.path("wrong_id").request().delete();
        assertEquals(Status.NOT_FOUND.getStatusCode(), deleteResp.getStatus());
    }

    @Test
    public void testGetEntriesJSON() {
        Response response = callGetEntries(APPLICATION_JSON_TYPE);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        List<FeedEntry> restoredEntities = response.readEntity(new GenericType<List<FeedEntry>>() {});
        assertEquals(feedEntries(), restoredEntities);
    }

    @Test
    public void testGetEntriesATOM() {
        Response response = callGetEntries(APPLICATION_ATOM_XML_TYPE);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        assertEquals(APPLICATION_ATOM_XML_TYPE, response.getMediaType());
    }

    @Test
    public void testGetEntriesNotFound() {
        Response response = target.path("1").path("entries").request(APPLICATION_JSON_TYPE).get();
        assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    private Response callGetEntries(MediaType mediaType) {
        String entityID = "1";
        CombinedFeed feedWithEntries = CombinedFeed.CombinedFeedBuilder
                .of(combinedFeed(entityID))
                .feedEntries(feedEntries())
                .build();

        datastore.put(entityID, feedWithEntries);
        return target.path(entityID).path("entries").request(mediaType).get();
    }
}
