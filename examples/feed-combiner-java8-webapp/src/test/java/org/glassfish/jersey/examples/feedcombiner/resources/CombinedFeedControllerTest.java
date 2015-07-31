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
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.StringJoiner;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.examples.feedcombiner.ApplicationProperties;
import org.glassfish.jersey.examples.feedcombiner.MyApplication;
import org.glassfish.jersey.examples.feedcombiner.binder.ApplicationBinder;
import org.glassfish.jersey.examples.feedcombiner.model.CombinedFeed;
import org.glassfish.jersey.examples.feedcombiner.model.FeedEntry;
import org.glassfish.jersey.examples.feedcombiner.store.InMemoryDataStore;
import org.glassfish.jersey.examples.feedcombiner.store.ReadWriteLockDataStore;
import org.glassfish.jersey.moxy.json.MoxyJsonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.mvc.freemarker.FreemarkerMvcFeature;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.Before;
import org.junit.Test;
import static org.glassfish.jersey.examples.feedcombiner.resources.CombinedFeedTestHelper.feedEntries;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import static javax.ws.rs.core.MediaType.TEXT_HTML_TYPE;

/**
 * @author Petr Bouda (petr.bouda at oracle.com)
 */
public class CombinedFeedControllerTest extends JerseyTest {

    // Prepare entities for testing
    private static final String[] params =
            {"My Title", "My Description", "http://localhost/1", "http://localhost/2", "123456"};

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
    }

    private InputStream serializedDatastore() {
        HashMap<String, Serializable> datastore = new HashMap<>();
        byte[] serialized = SerializationUtils.serialize(datastore);
        return new ByteArrayInputStream(serialized);
    }

    @Test
    public void testCreate() {
        Form form = new Form();
        form.param("title", params[0]);
        form.param("description", params[1]);
        form.param("urls", new StringJoiner(",").add(params[2]).add(params[3]).toString());
        form.param("refreshPeriod", params[4]);

        Response response = target().request().post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String html = response.readEntity(String.class);

        // HTML page contains all information about the created entity
        assertTrue(html.contains(params[0]) && html.contains(params[1])
                && html.contains(params[2]) && html.contains(params[3])
                && html.contains(params[4]));
    }

    @Test
    public void testDelete() {
        CombinedFeed feed = combinedFeed("1");
        datastore.put(feed.getId(), feed);

        Response response = target("delete").path(feed.getId()).request().post(null);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String html = response.readEntity(String.class);

        // HTML page does not contain a title of the deleted entity
        assertTrue(!html.contains(params[0]));
    }

    @Test
    public void testGetAll() {
        datastore.put("1", combinedFeed("1"));
        datastore.put("2", combinedFeed("2"));

        Response response = target().request().get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String html = response.readEntity(String.class);

        // HTML page contains IDs of created entities above
        assertTrue(html.contains("<th>1</th>") && html.contains("<th>2</th>"));
    }

    @Test
    public void testGetEntriesHTML() {
        String entityID = "1";
        CombinedFeed feedWithEntries = CombinedFeed.CombinedFeedBuilder
                .of(combinedFeed(entityID))
                .feedEntries(feedEntries())
                .build();
        datastore.put(entityID, feedWithEntries);

        Response response = target().path(entityID).request(TEXT_HTML_TYPE).get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String html = response.readEntity(String.class);
        assertTrue(html.contains("<td>title1</td>") && html.contains("<td>title2</td>"));
        assertTrue(html.contains("<td><a href=\"link1\">link1</a></td>") && html.contains("<td><a href=\"link2\">link2</a></td>"));
    }

    private CombinedFeed combinedFeed(String entityID) {
        return new CombinedFeed.CombinedFeedBuilder(entityID, "http://localhost")
                .title(params[0]).description("description").refreshPeriod(10).build();
    }

}
