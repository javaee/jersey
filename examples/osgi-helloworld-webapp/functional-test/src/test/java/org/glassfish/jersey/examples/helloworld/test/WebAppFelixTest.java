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

package org.glassfish.jersey.examples.helloworld.test;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;
import static org.junit.Assert.assertEquals;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
public class WebAppFelixTest extends AbstractWebAppTest {

    private static final Logger LOGGER = Logger.getLogger(WebAppFelixTest.class.getName());

    @Override
    public List<Option> osgiRuntimeOptions() {
        return Arrays.asList(CoreOptions.options(
                mavenBundle()
                        .groupId("org.apache.felix").artifactId("org.apache.felix.eventadmin")
                        .versionAsInProject()
        )
        );
    }

    @Before
    public void before() throws Exception {
        defaultMandatoryBeforeMethod();
    }

    @Test
    public void testWebResources() throws Exception {
        final WebTarget target = webAppTestTarget("/webresources");

        // send request and check response - helloworld resource
        final String helloResult = target.path("/helloworld").request().build("GET").invoke().readEntity(
                String.class);
        LOGGER.info("HELLO RESULT = " + helloResult);
        assertEquals("Hello World", helloResult);

        // send request and check response - another resource
        final String anotherResult = target.path("/another").request().build("GET").invoke()
                .readEntity(String.class);

        LOGGER.info("ANOTHER RESULT = " + anotherResult);
        assertEquals("Another", anotherResult);

        // send request and check response for the additional bundle - should fail now
        final String additionalResult = target.path("/additional").request().build("GET").invoke()
                .readEntity(String.class);

        LOGGER.info("ADDITIONAL RESULT = " + additionalResult);
        assertEquals("Additional Bundle!", additionalResult);

        // send request and check response for the sub-packaged additional bundle
        final String subAdditionalResult = target.path("/subadditional").request().build("GET").invoke()
                .readEntity(String.class);

        LOGGER.info("SUB-PACKAGED ADDITIONAL RESULT = " + subAdditionalResult);
        assertEquals("Sub-packaged Additional Bundle!", subAdditionalResult);

        // send request and check response for the WEB-INF classes located resource
        final String webInfClassesResourceResult = target.path("/webinf").request().build("GET").invoke()
                .readEntity(String.class);

        LOGGER.info("WEB-INF CLASSES RESOURCE RESULT = " + webInfClassesResourceResult);
        assertEquals("WebInfClassesResource", webInfClassesResourceResult);

        // send request and check response for the WEB-INF classes located resource
        final String webInfClassesSubPackagedResourceResult = target.path("/subwebinf").request().build("GET")
                .invoke().readEntity(String.class);

        LOGGER.info("WEB-INF CLASSES SUB-PACKAGED RESOURCE RESULT = " + webInfClassesSubPackagedResourceResult);
        assertEquals("WebInfClassesSubPackagedResource", webInfClassesSubPackagedResourceResult);
    }

    @Test
    public void testNonRecursiveWebResources() throws Exception {
        final WebTarget target = webAppTestTarget("/n-webresources");

        // send request and check response - helloworld resource
        final String helloResult = target.path("/helloworld").request().build("GET").invoke().readEntity(
                String.class);
        LOGGER.info("HELLO RESULT = " + helloResult);
        assertEquals("Hello World", helloResult);

        // send request and check response - another resource
        final String anotherResult = target.path("/another").request().build("GET").invoke()
                .readEntity(String.class);

        LOGGER.info("ANOTHER RESULT = " + anotherResult);
        assertEquals("Another", anotherResult);

        // send request and check response for the additional bundle - should fail now
        final String additionalResult = target.path("/additional").request().build("GET").invoke()
                .readEntity(String.class);

        LOGGER.info("ADDITIONAL RESULT = " + additionalResult);
        assertEquals("Additional Bundle!", additionalResult);

        // send request and check response for the sub-packaged additional bundle
        final Response subAdditionalResponse = target.path("/subadditional").request().build("GET").invoke();

        LOGGER.info("SUB-PACKAGED ADDITIONAL http status = " + subAdditionalResponse.getStatus());
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), subAdditionalResponse.getStatus());

        // send request and check response for the WEB-INF classes located resource
        final String webInfClassesResourceResult = target.path("/webinf").request().build("GET").invoke()
                .readEntity(String.class);

        LOGGER.info("WEB-INF CLASSES RESOURCE RESULT = " + webInfClassesResourceResult);
        assertEquals("WebInfClassesResource", webInfClassesResourceResult);

        // send request and check response for the WEB-INF classes located resource
        final Response webInfClassesSubPackagedResourceResponse = target.path("/subwebinf").request().build("GET")
                .invoke();

        LOGGER.info("WEB-INF CLASSES SUB-PACKAGED http status = " + webInfClassesSubPackagedResourceResponse.getStatus());
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), webInfClassesSubPackagedResourceResponse.getStatus());
    }
}
