/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.osgi.test.basic;

import java.net.URI;
import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.osgi.test.util.Helper;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.validation.ValidationFeature;

import org.glassfish.grizzly.http.server.HttpServer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import static org.junit.Assert.assertEquals;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;

/**
 * Basic test of Bean Validation.
 *
 * @author Michal Gajdos
 * @author Libor Kramolis (libor.kramolis at oracle.com)
 */
@RunWith(PaxExam.class)
public class BeanValidationTest {

    private static final String CONTEXT = "/jersey";

    private static final URI baseUri = UriBuilder.fromUri("http://localhost")
            .port(Helper.getPort())
            .path(CONTEXT).build();

    @Configuration
    public static Option[] configuration() {
        List<Option> options = Helper.getCommonOsgiOptions();

        options.addAll(Helper.expandedList(
                // for debug purposes
                // PaxRunnerOptions.vmOption("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"),

                // validation
                mavenBundle().groupId("org.glassfish.jersey.ext").artifactId("jersey-bean-validation").versionAsInProject(),
                mavenBundle().groupId("org.hibernate").artifactId("hibernate-validator").versionAsInProject(),
                mavenBundle().groupId("org.jboss.logging").artifactId("jboss-logging").versionAsInProject(),
                mavenBundle().groupId("com.fasterxml").artifactId("classmate").versionAsInProject(),
                mavenBundle().groupId("javax.el").artifactId("javax.el-api").versionAsInProject()
        ));

        options = Helper.addPaxExamMavenLocalRepositoryProperty(options);
        return Helper.asArray(options);
    }

    @Test
    public void testBeanValidationResourceFeature() throws Exception {
        _test(400, true, false);
    }

    @Test
    public void testBeanValidationResourceAutoDiscovery() throws Exception {
        _test(400, false, false);
    }

    @Test
    public void testBeanValidationResourceManualRegistration() throws Exception {
        _test(400, true, true);
    }

    @Test
    public void testBeanValidationResourceNoValidationFeature() throws Exception {
        // Even though properties are disabled BV is registered.
        _test(400, false, true);
    }

    protected void _test(final int expectedResponseCode,
                         final boolean registerFeature,
                         final boolean disableMetainfServicesLookup) {
        final ResourceConfig resourceConfig = new ResourceConfig(BeanValidationResource.class);
        if (registerFeature) {
            resourceConfig.register(ValidationFeature.class);
        }
        if (disableMetainfServicesLookup) {
            resourceConfig.property(ServerProperties.METAINF_SERVICES_LOOKUP_DISABLE, Boolean.TRUE);

            resourceConfig.register(org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpContainerProvider.class);
        }

        final HttpServer server = GrizzlyHttpServerFactory.createHttpServer(baseUri, resourceConfig);

        final Form form = new Form();
        final String formValue = "formValue";
        form.asMap().add("formParam", formValue);

        final Client client = ClientBuilder.newClient();
        final String entity = client.target(baseUri)
                .path("/bean-validation")
                .request()
                .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE), String.class);

        assertEquals(formValue, entity);

        final Response response = client.target(baseUri)
                .path("/bean-validation")
                .request()
                .post(Entity.entity(new Form(), MediaType.APPLICATION_FORM_URLENCODED_TYPE));

        assertEquals(expectedResponseCode, response.getStatus());

        server.shutdownNow();
    }

}
