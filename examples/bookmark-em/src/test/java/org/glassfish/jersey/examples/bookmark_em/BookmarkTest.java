/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.examples.bookmark_em;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;

import org.glassfish.jersey.jettison.JettisonFeature;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.external.ExternalTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainer;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class BookmarkTest extends JerseyTest {

    @Override
    protected Application configure() {
        return new MyApplication();
    }

    @Override
    protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
        return new ExternalTestContainerFactory();
    }

    @Override
    protected Client getClient(final TestContainer tc, final ApplicationHandler applicationHandler) {
        final Client client = super.getClient(tc, applicationHandler);
        client.configuration().register(new JettisonFeature());
        return client;
    }

    @Test
    public void testGetUsers() {
        JSONArray users = target().path("resources/users/").request("application/json").get(JSONArray.class);
        assertTrue(users != null);
    }

    @Test
    public void testCreateUser() {
        boolean thrown = false;
        JSONObject user = new JSONObject();

        try {
            user.put("userid", "testuid").put("password", "test").put("email", "test@test.net").put("username", "Test User");
            target().path("resources/users/testuid").request().put(Entity.entity(user, "application/json"));
        } catch (Exception e) {
            e.printStackTrace();
            thrown = true;
        }

        assertFalse(thrown);
    }

    @Test
    public void testGetUsers2() {
        JSONArray users = target().path("resources/users/").request("application/json").get(JSONArray.class);
        assertTrue(users != null);
        assertTrue(users.length() == 1);
    }

    @Test
    public void updateUser() {
        boolean thrown = false;

        try {
            JSONObject user = target().path("resources/users/testuid").request("application/json").get(JSONObject.class);

            user.put("password", "NEW PASSWORD").put("email", "NEW@EMAIL.NET").put("username", "UPDATED TEST USER");
            target().path("resources/users/testuid").request().put(Entity.entity(user, "application/json"));

            user = target().path("resources/users/testuid").request("application/json").get(JSONObject.class);

            assertEquals(user.get("username"), "UPDATED TEST USER");
            assertEquals(user.get("email"), "NEW@EMAIL.NET");
            assertEquals(user.get("password"), "NEW PASSWORD");

        } catch (Exception e) {
            e.printStackTrace();
            thrown = true;
        }

        assertFalse(thrown);
    }

    // this is ugly but it would be probably uglier when divided into separate
    // test cases
    @Test
    public void getUserBookmarkList() {
        boolean thrown = false;

        try {
            JSONObject user = target().path("resources/users/testuid").request("application/json").get(JSONObject.class);
            assertTrue(user != null);

            final WebTarget webTarget = target(user.getString("bookmarks"));

            JSONObject bookmark = new JSONObject();
            bookmark.put("uri", "http://java.sun.com").put("sdesc", "test desc").put("ldesc", "long test description");
            webTarget.request().post(Entity.entity(bookmark, "application/json"));

            JSONArray bookmarks = webTarget.request("application/json").get(JSONArray.class);
            assertTrue(bookmarks != null);
            int bookmarksSize = bookmarks.length();

            String testBookmarkUrl = bookmarks.getString(0);

            final WebTarget bookmarkResource = target().path(testBookmarkUrl);
            bookmark = bookmarkResource.request("application/json").get(JSONObject.class);
            assertTrue(bookmark != null);

            bookmarkResource.request().delete();

            bookmarks = target().path("resources/users/testuid/bookmarks").request("application/json").get(JSONArray.class);
            assertTrue(bookmarks != null);
            assertTrue(bookmarks.length() == (bookmarksSize - 1));

        } catch (Exception e) {
            e.printStackTrace();
            thrown = true;
        }

        assertFalse(thrown);
    }

    @Test
    public void deleteUser() {
        boolean thrown = false;

        try {
            target().path("resources/users/testuid").request().delete();
        } catch (Exception e) {
            e.printStackTrace();
            thrown = true;
        }

        assertFalse(thrown);
    }
}
