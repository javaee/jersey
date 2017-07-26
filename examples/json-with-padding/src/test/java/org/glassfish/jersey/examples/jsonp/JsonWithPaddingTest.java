/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package org.glassfish.jersey.examples.jsonp;

import java.util.List;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public class JsonWithPaddingTest extends JerseyTest {

    @Override
    protected ResourceConfig configure() {
        enable(TestProperties.LOG_TRAFFIC);
        enable(TestProperties.DUMP_ENTITY);

        return App.createApp();
    }

    /**
     * Test checks that the application.wadl is reachable.
     * <p/>
     */
    @Test
    public void testApplicationWadl() {
        WebTarget target = target();
        String applicationWadl = target.path("application.wadl").request().get(String.class);
        assertTrue("Something wrong. Returned wadl length is not > 0", applicationWadl.length() > 0);
    }

    /**
     * Test check GET on the "changes" resource in "application/json" format.
     */
    @Test
    public void testGetOnChangesJSONFormat() {
        WebTarget target = target();
        GenericType<List<ChangeRecordBean>> genericType = new GenericType<List<ChangeRecordBean>>() {};
        // get the initial representation
        List<ChangeRecordBean> changes = target.path("changes").request("application/json").get(genericType);
        // check that there are two changes entries
        assertEquals("Expected number of initial changes not found", 5, changes.size());
    }

    /**
     * Test check GET on the "changes" resource in "application/xml" format.
     */
    @Test
    public void testGetOnLatestChangeXMLFormat() {
        WebTarget target = target();
        ChangeRecordBean lastChange = target.path("changes/latest").request("application/xml").get(ChangeRecordBean.class);
        assertEquals(1, lastChange.linesChanged);
    }

    /**
     * Test check GET on the "changes" resource in "application/javascript" format.
     */
    @Test
    public void testGetOnLatestChangeJavascriptFormat() {
        WebTarget target = target();
        String js = target.path("changes").request("application/x-javascript").get(String.class);
        assertTrue(js.startsWith("callback"));
    }

    @Test
    public void testGetOnLatestChangeJavascriptFormatDifferentCallback() {
        WebTarget target = target();
        String js = target.path("changes").queryParam("__callback", "parse").request("application/x-javascript")
                .get(String.class);
        assertTrue(js.startsWith("parse"));
    }
}
