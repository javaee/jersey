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

package org.glassfish.jersey.examples.jackson;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.message.internal.MediaTypes;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.glassfish.jersey.test.util.runner.ConcurrentRunner;

import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.runner.RunWith;

/**
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
@RunWith(ConcurrentRunner.class)
public class JacksonTest extends JerseyTest {

    @Override
    protected ResourceConfig configure() {
        enable(TestProperties.LOG_TRAFFIC);
        enable(TestProperties.DUMP_ENTITY);

        return App.createApp();
    }

    @Override
    protected void configureClient(ClientConfig config) {
        config.register(new JacksonFeature()).register(MyObjectMapperProvider.class);
    }

    @Test
    public void testEmptyArrayPresent() {
        WebTarget target = target();
        String responseMsg = target.path("emptyArrayResource").request(MediaType.APPLICATION_JSON).get(String.class);
        assertTrue(responseMsg.replaceAll("[ \t]*", "").contains("[]"));
    }

    @Test
    public void testJSONPPresent() {
        WebTarget target = target();
        String responseMsg = target.path("nonJaxbResource").request("application/javascript").get(String.class);
        assertTrue(responseMsg.startsWith("callback("));
    }

    @Test
    public void testJSONDoesNotReflectJSONPWrapper() {
        WebTarget target = target();
        String responseMsg = target.path("nonJaxbResource").request("application/json").get(String.class);
        assertTrue(!responseMsg.contains("jsonSource"));
    }

    @Test
    public void testCombinedAnnotationResource() {
        WebTarget target = target();
        String responseMsg = target.path("combinedAnnotations").request("application/json").get(String.class);
        assertTrue(responseMsg.contains("account") && responseMsg.contains("value"));
    }

    @Test
    public void testEmptyArrayBean() {
        WebTarget target = target();
        EmptyArrayBean responseMsg = target.path("emptyArrayResource").request(MediaType.APPLICATION_JSON)
                .get(EmptyArrayBean.class);
        assertNotNull(responseMsg);
    }

    @Test
    public void testCombinedAnnotationBean() {
        WebTarget target = target();
        CombinedAnnotationBean responseMsg = target.path("combinedAnnotations").request("application/json")
                .get(CombinedAnnotationBean.class);
        assertNotNull(responseMsg);
    }

    @Test
    @Ignore
    // TODO un-ignore once a JSON reader for "application/javascript" is supported
    public void testJSONPBean() {
        WebTarget target = target();
        NonJaxbBean responseMsg = target.path("nonJaxbResource").request("application/javascript").get(NonJaxbBean.class);
        assertNotNull(responseMsg);
    }

    /**
     * Test if a WADL document is available at the relative path
     * "application.wadl".
     * <p/>
     */
    @Test
    public void testApplicationWadl() {
        WebTarget target = target();
        String serviceWadl = target.path("application.wadl").request(MediaTypes.WADL_TYPE).get(String.class);

        assertTrue(serviceWadl.length() > 0);
    }

    /**
     * Test, that in case of malformed JSON, the jackson exception mappers will be used and the response will be
     * 400 - bad request instead of 500 - server error
     */
    @Test
    public void testExceptionMapping() {
        enable(TestProperties.LOG_TRAFFIC);
        // create a request with invalid json string to cause an exception in Jackson
        Response response = target().path("parseExceptionTest").request("application/json")
                .put(Entity.entity("Malformed json string.", MediaType.valueOf("application/json")));

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }
}
