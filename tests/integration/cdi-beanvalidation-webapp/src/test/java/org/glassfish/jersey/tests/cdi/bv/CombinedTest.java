/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.cdi.bv;

import java.io.IOException;
import java.net.URI;
import java.util.Properties;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpContainer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpContainerProvider;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.inject.hk2.Hk2InjectionManagerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.TestProperties;

import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;

import org.hamcrest.CoreMatchers;
import org.jboss.weld.environment.se.Weld;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

/**
 * Test both Jersey apps running simultaneously within a single Grizzly HTTP server
 * to make sure injection managers do not interfere. The test is not executed
 * if other than the default (Grizzly) test container has been set.
 * For Servlet based container testing, the other two tests, {@link RawCdiTest} and {@link RawHk2Test},
 * do the same job, because the WAR application contains both Jersey apps already.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public class CombinedTest {

    public static final String CDI_URI = "/cdi";
    public static final String HK2_URI = "/hk2";

    public static final String PORT_NUMBER = getSystemProperty(TestProperties.CONTAINER_PORT,
                                                    Integer.toString(TestProperties.DEFAULT_CONTAINER_PORT));

    private static final URI BASE_HK2_URI = URI.create("http://localhost:" + PORT_NUMBER + HK2_URI);
    private static final URI BASE_CDI_URI = URI.create("http://localhost:" + PORT_NUMBER + CDI_URI);

    private static final boolean isDefaultTestContainerFactorySet = isDefaultTestContainerFactorySet();

    Weld weld;
    HttpServer cdiServer;

    Client client;
    WebTarget cdiTarget, hk2Target;

    @Before
    public void before() throws IOException {
        if (isDefaultTestContainerFactorySet && Hk2InjectionManagerFactory.isImmediateStrategy()) {
            initializeWeld();
            startGrizzlyContainer();
            initializeClient();
        }
    }

    @Before
    public void beforeIsImmediate() {
        Assume.assumeTrue(Hk2InjectionManagerFactory.isImmediateStrategy());
    }

    @After
    public void after() {
        if (isDefaultTestContainerFactorySet && Hk2InjectionManagerFactory.isImmediateStrategy()) {
            cdiServer.shutdownNow();
            weld.shutdown();
            client.close();
        }
    }

    @Test
    public void testParamValidatedResourceNoParam() throws Exception {
        Assume.assumeThat(isDefaultTestContainerFactorySet, CoreMatchers.is(true));
        BaseValidationTest._testParamValidatedResourceNoParam(cdiTarget);
        BaseValidationTest._testParamValidatedResourceNoParam(hk2Target);
    }

    @Test
    public void testParamValidatedResourceParamProvided() throws Exception {
        Assume.assumeThat(isDefaultTestContainerFactorySet, CoreMatchers.is(true));
        BaseValidationTest._testParamValidatedResourceParamProvided(cdiTarget);
        BaseValidationTest._testParamValidatedResourceParamProvided(hk2Target);
    }

    @Test
    public void testFieldValidatedResourceNoParam() throws Exception {
        Assume.assumeThat(isDefaultTestContainerFactorySet, CoreMatchers.is(true));
        BaseValidationTest._testFieldValidatedResourceNoParam(cdiTarget);
        BaseValidationTest._testFieldValidatedResourceNoParam(hk2Target);
    }

    @Test
    public void testFieldValidatedResourceParamProvided() throws Exception {
        Assume.assumeThat(isDefaultTestContainerFactorySet, CoreMatchers.is(true));
        BaseValidationTest._testFieldValidatedResourceParamProvided(cdiTarget);
        BaseValidationTest._testFieldValidatedResourceParamProvided(hk2Target);
    }

    @Test
    public void testPropertyValidatedResourceNoParam() throws Exception {
        Assume.assumeThat(isDefaultTestContainerFactorySet, CoreMatchers.is(true));
        BaseValidationTest._testPropertyValidatedResourceNoParam(cdiTarget);
        BaseValidationTest._testPropertyValidatedResourceNoParam(hk2Target);
    }

    @Test
    public void testPropertyValidatedResourceParamProvided() throws Exception {
        Assume.assumeThat(isDefaultTestContainerFactorySet, CoreMatchers.is(true));
        BaseValidationTest._testPropertyValidatedResourceParamProvided(cdiTarget);
        BaseValidationTest._testPropertyValidatedResourceParamProvided(hk2Target);
    }

    @Test
    public void testOldFashionedResourceNoParam() {
        Assume.assumeThat(isDefaultTestContainerFactorySet, CoreMatchers.is(true));
        BaseValidationTest._testOldFashionedResourceNoParam(cdiTarget);
        BaseValidationTest._testOldFashionedResourceNoParam(hk2Target);
    }

    @Test
    public void testOldFashionedResourceParamProvided() throws Exception {
        Assume.assumeThat(isDefaultTestContainerFactorySet, CoreMatchers.is(true));
        BaseValidationTest._testOldFashionedResourceParamProvided(cdiTarget);
        BaseValidationTest._testOldFashionedResourceParamProvided(hk2Target);
    }

    @Test
    public void testNonJaxRsValidationFieldValidatedResourceNoParam() {
        Assume.assumeThat(isDefaultTestContainerFactorySet, CoreMatchers.is(true));
        BaseValidationTest._testNonJaxRsValidationFieldValidatedResourceNoParam(cdiTarget);
    }

    @Test
    public void testNonJaxRsValidationFieldValidatedResourceParamProvided() {
        Assume.assumeThat(isDefaultTestContainerFactorySet, CoreMatchers.is(true));
        BaseValidationTest._testNonJaxRsValidationFieldValidatedResourceParamProvided(cdiTarget);
    }

    private void initializeWeld() {
        weld = new Weld();
        weld.initialize();
    }

    private void startGrizzlyContainer() throws IOException {
        final ResourceConfig cdiConfig = ResourceConfig.forApplicationClass(CdiApplication.class);
        final ResourceConfig hk2Config = ResourceConfig.forApplicationClass(Hk2Application.class);

        cdiServer = GrizzlyHttpServerFactory.createHttpServer(BASE_CDI_URI, cdiConfig, false);
        final HttpHandler hk2Handler = createGrizzlyContainer(hk2Config);
        cdiServer.getServerConfiguration().addHttpHandler(hk2Handler, HK2_URI);
        cdiServer.start();
    }

    private void initializeClient() {
        client = ClientBuilder.newClient();
        cdiTarget = client.target(BASE_CDI_URI);
        hk2Target = client.target(BASE_HK2_URI);
    }

    private GrizzlyHttpContainer createGrizzlyContainer(ResourceConfig resourceConfig) {
        return (new GrizzlyHttpContainerProvider()).createContainer(GrizzlyHttpContainer.class, resourceConfig);
    }

    private static boolean isDefaultTestContainerFactorySet() {
        final String testContainerFactory = getSystemProperty(TestProperties.CONTAINER_FACTORY, null);
        return testContainerFactory == null || TestProperties.DEFAULT_CONTAINER_FACTORY.equals(testContainerFactory);
    }

    private static String getSystemProperty(final String propertyName, final String defaultValue) {
        final Properties systemProperties = System.getProperties();
        return systemProperties.getProperty(propertyName, defaultValue);
    }
}