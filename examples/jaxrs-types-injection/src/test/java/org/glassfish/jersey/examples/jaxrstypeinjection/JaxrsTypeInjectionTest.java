/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.examples.jaxrstypeinjection;

import javax.ws.rs.client.WebTarget;

import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

public class JaxrsTypeInjectionTest extends JerseyTest {

    @Override
    protected ResourceConfig configure() {
        return App.create();
    }

    private String[] expectedFragmentsProgrammatic = new String[]{
            // UriInfo
            "Absolute path : " + this.getBaseUri() + "programmatic/v1/v2",
            "Base URI : " + this.getBaseUri(),
            "Path : programmatic/v1/v2",
            "Path segments : [programmatic, v1, v2]",
            "p1 : v1", "p2 : v2", // path params
            "q1 : 1", "q2 : v2, v3", // query params
            "Request URI : " + this.getBaseUri() + "programmatic/v1/v2?q1=1&q2=v2&q2=v3",
            // RequestHeaders/HttpHeaders
            "Accept : text/plain",
            // Injected Parameters
            "String path param p1=v1",
            "PathSegment path param p2=v2",
            "int query param q1=1",
            "List<String> query param q2=[v2, v3]"
    };
    private String[] expectedFragmentsAnnotatedInstance = new String[]{
            // UriInfo
            "Absolute path : " + this.getBaseUri() + "annotated/instance/v1/v2",
            "Base URI : " + this.getBaseUri(),
            "Path : annotated/instance/v1/v2",
            "Path segments : [annotated, instance, v1, v2]",
            "p1 : v1", "p2 : v2", // path params
            "q1 : 1", "q2 : v2, v3", // query params
            "Request URI : " + this.getBaseUri() + "annotated/instance/v1/v2?q1=1&q2=v2&q2=v3",
            // RequestHeaders/HttpHeaders
            "Accept : text/plain",
            // Injected Parameters
            "String path param p1=v1",
            "PathSegment path param p2=v2",
            "int query param q1=1",
            "List<String> query param q2=[v2, v3]"
    };
    private String[] expectedFragmentsAnnotatedMethod = new String[]{
            // UriInfo
            "Absolute path : " + this.getBaseUri() + "annotated/method/v1/v2",
            "Base URI : " + this.getBaseUri(),
            "Path : annotated/method/v1/v2",
            "Path segments : [annotated, method, v1, v2]",
            "p1 : v1", "p2 : v2", // path params
            "q1 : 1", "q2 : v2, v3", // query params
            "Request URI : " + this.getBaseUri() + "annotated/method/v1/v2?q1=1&q2=v2&q2=v3",
            // RequestHeaders/HttpHeaders
            "Accept : text/plain",
            // Injected Parameters
            "String path param p1=v1",
            "PathSegment path param p2=v2",
            "int query param q1=1",
            "List<String> query param q2=[v2, v3]"
    };

    private WebTarget prepareTarget(String path) {
        final WebTarget target = target();
        target.register(LoggingFeature.class);
        return target.path(path).resolveTemplate("p1", "v1").resolveTemplate("p2",
                "v2").queryParam("q1", 1).queryParam("q2", "v2").queryParam("q2", "v3");
    }

    @Test
    public void testProgrammaticApp() throws Exception {
        String responseEntity = prepareTarget(App.ROOT_PATH_PROGRAMMATIC).request("text/plain").get(String.class)
                .toLowerCase();

        for (String expectedFragment : expectedFragmentsProgrammatic) {
            assertTrue("Expected fragment '" + expectedFragment + "' not found in response:\n" + responseEntity,
                    // http header field names are case insensitive
                    responseEntity.contains(expectedFragment.toLowerCase()));
        }
    }

    @Test
    public void testAnnotatedInstanceApp() throws Exception {
        String responseEntity = prepareTarget(App.ROOT_PATH_ANNOTATED_INSTANCE).request("text/plain").get(String.class)
                .toLowerCase();

        for (String expectedFragment : expectedFragmentsAnnotatedInstance) {
            assertTrue("Expected fragment '" + expectedFragment + "' not found in response:\n" + responseEntity,
                    // http header field names are case insensitive
                    responseEntity.contains(expectedFragment.toLowerCase()));
        }
    }

    @Test
    public void testAnnotatedMethodApp() throws Exception {
        String responseEntity = prepareTarget(App.ROOT_PATH_ANNOTATED_METHOD).request("text/plain").get(String.class)
                .toLowerCase();

        for (String expectedFragment : expectedFragmentsAnnotatedMethod) {
            assertTrue("Expected fragment '" + expectedFragment + "' not found in response:\n" + responseEntity,
                    // http header field names are case insensitive
                    responseEntity.contains(expectedFragment.toLowerCase()));
        }
    }
}
