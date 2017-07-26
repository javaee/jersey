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

package org.glassfish.jersey.examples.console;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.internal.util.collection.MultivaluedStringMap;
import org.glassfish.jersey.jettison.JettisonFeature;
import org.glassfish.jersey.message.internal.MediaTypes;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;

import org.codehaus.jettison.json.JSONArray;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Naresh (srinivas.bhimisetty at oracle.com)
 */
public class MainTest extends JerseyTest {

    @Override
    protected ResourceConfig configure() {
        enable(TestProperties.LOG_TRAFFIC);

        return App.createApp();
    }

    @Override
    protected void configureClient(ClientConfig config) {
        config.register(new JettisonFeature());
    }

    /**
     * Test if a WADL document is available at the relative path
     * "application.wadl".
     */
    @Test
    public void testApplicationWadl() {
        String serviceWadl = target().path("application.wadl").request(MediaTypes.WADL_TYPE).get(String.class);
        assertTrue(!serviceWadl.isEmpty());
    }

    /**
     * Test if GET on the resource "/form" gives response with status code 200.
     */
    @Test
    public void testGetOnForm() {
        Response response = target().path("form").request(MediaType.TEXT_HTML).get();
        assertEquals("GET on the 'form' resource doesn't give expected response", Response.Status.OK.getStatusCode(),
                response.getStatusInfo().getStatusCode());
    }

    /**
     * Test checks that POST on the '/form' resource gives a response page
     * with the entered data.
     */
    @Test
    public void testPostOnForm() {
        MultivaluedMap<String, String> formData = new MultivaluedStringMap();
        formData.add("name", "testName");
        formData.add("colour", "red");
        formData.add("hint", "re");

        Response response = target().path("form").request().post(Entity.entity(formData, MediaType.APPLICATION_FORM_URLENCODED));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusInfo().getStatusCode());

        // check that the generated response is the expected one
        InputStream responseInputStream = response.readEntity(InputStream.class);
        try {
            byte[] responseData = new byte[responseInputStream.available()];
            final int read = responseInputStream.read(responseData);

            assertTrue(read > 0);
            assertTrue(new String(responseData).contains("Hello, you entered"));
        } catch (IOException ex) {
            Logger.getLogger(MainTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Test checks that a GET on the resource "/form/colours" with mime-type "text/html"
     * shows the appropriate colours based on the query param "match".
     */
    @Test
    public void testGetColoursAsPlainText() {
        // without the query param "match"
        Response response = target().path("form").path("colours").request(MediaType.TEXT_PLAIN).get();
        assertEquals("GET on path '/form/colours' with mime type 'text/html' doesn't give expected response",
                Response.Status.OK.getStatusCode(), response.getStatusInfo().getStatusCode());

        String responseMsg = target().path("form").path("colours").request(MediaType.TEXT_PLAIN).get(String.class);
        assertEquals("Response content doesn't match the expected value", "red\norange\nyellow\ngreen\nblue\nindigo\nviolet\n",
                responseMsg);

        // with the query param "match" value "re"
        responseMsg = target("form/colours").queryParam("match", "re").request(MediaType.TEXT_PLAIN).get(String.class);
        assertEquals("Response content doesn't match the expected value with the query param 'match=re'", "red\ngreen\n",
                responseMsg);
    }

    /**
     * Test checks that a GET on the resource "/form/colours" with mime-type "application/json"
     * shows the appropriate colours based on the query param "match".
     */
    @Test
    public void testGetColoursAsJson() {
        Response response = target().path("form").path("colours").request(MediaType.APPLICATION_JSON).get();
        assertEquals("GET on path '/form/colours' with mime type 'application/json' doesn't give expected response",
                Response.Status.OK.getStatusCode(), response.getStatusInfo().getStatusCode());

        JSONArray jsonArray = target().path("form").path("colours").request(MediaType.APPLICATION_JSON).get(JSONArray.class);
        assertEquals("Returned JSONArray doesn't have expected number of entries", 7, jsonArray.length());

        // with the query param "match" value "re"
        jsonArray = target("form/colours").queryParam("match", "re").request(MediaType.APPLICATION_JSON).get(JSONArray.class);
        assertEquals("Returned JSONArray doesn't have expected number of entries with the query param 'match=re'", 2,
                jsonArray.length());
    }

}
